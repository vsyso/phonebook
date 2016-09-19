/*
 * The MIT License
 *
 * Copyright 2016 Vladimir Syso.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.syso.phonebook.service;

import org.syso.phonebook.domain.Contact;
import org.syso.phonebook.domain.PhoneMask;
import org.syso.phonebook.domain.PhoneNumber;
import org.syso.phonebook.domain.PhoneNumberPK;
import org.syso.phonebook.domain.PhoneType;

import java.util.List;
import javax.annotation.Resource;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phonebook Service uses Hibernate to manipulate with data
 * 
 * @author Vladimir Syso
 */
@Service("phonebookService")
@Transactional
public class PhonebookService{

    @Resource(name = "sessionFactory")
    private SessionFactory sessionFactory;

    public List<Contact> getAllContacts() {
 
        Session session = sessionFactory.getCurrentSession();
        
        Query query = session.createQuery("FROM Contact");
        @SuppressWarnings("unchecked")
        List<Contact> list =  query.list();
        return list;
    }

    /**
     *  Add a Contact to the database
     * 
     * @param contact entity to add
     * @return  operation completed successfully or not
     */
    public boolean addContact(Contact contact) {

        // Prevent to create numbers with Contact entity
        contact.setPhoneNumberCollection(null);
        
        Session session = sessionFactory.getCurrentSession();
        session.save(contact);        

        return contact.getContactId() > 0;
    }

    /**
     * Delete a Contact entity from database
     * 
     * @param contactId of the Contact
     */
    public void deleteContact(Integer contactId) {
        
        Session session = sessionFactory.getCurrentSession();
        
        Contact contact = (Contact) session.get(Contact.class, contactId);
        contact.setPhoneNumberCollection(null);
        
        session.delete(contact);
    }

    public void updateContact(Contact contact) {

        Session session = sessionFactory.getCurrentSession();
        
        Contact existingContact = (Contact) session.get(Contact.class, contact.getContactId());
        existingContact.setFirstName(contact.getFirstName());
        existingContact.setLastName(contact.getLastName());
        
        session.save(existingContact);
    }
    
    /**
     * Add a phone number to specific contact
     * 
     * @param contactId of the new owner
     * @param phoneNumber un-filtered phone number
     * @return created or already existent PhoneNumber object
     */
    public PhoneNumber addNumber(Integer contactId, PhoneNumber phoneNumber)
    {
        String number = phoneNumber.getPhoneNumber();
        String numbersOnly = number.replaceAll("\\D", "");
        if(numbersOnly.isEmpty()) {
            return null;
        }
        
        PhoneNumber exsitentPhoneNumber = findPhoneNumber(numbersOnly);
        if(exsitentPhoneNumber != null){
            return exsitentPhoneNumber;
        }
                
        Session session = sessionFactory.getCurrentSession();
        
        Contact contact = (Contact) session.get(Contact.class, contactId);
        if(contact == null){
            return null;
        }
        
        String maskOnly = number.replace("X", "").replaceAll("\\d", "X");            
        PhoneMask phoneMask = findPhoneMask(maskOnly, true);
        if (phoneMask == null) {
            return null;
        }
        
        PhoneType phoneType = phoneNumber.getPhoneType();
        if(phoneType == null || phoneType.getPhoneTypeName() == null){
            return null;
        }
        
        phoneType = findPhoneType(phoneType.getPhoneTypeName(), true); 
        if (phoneType == null) {
            return null;
        }
        
        PhoneNumber newPhoneNumber = new PhoneNumber();        
        newPhoneNumber.setPhoneMaskId(phoneMask);
        newPhoneNumber.setPhoneType(phoneType);
        newPhoneNumber.setContact(contact);
        newPhoneNumber.setPhoneNumber(numbersOnly);
        newPhoneNumber.setPhoneNumberPK(new PhoneNumberPK(0, contactId));
        newPhoneNumber.setPhoneMaskId(phoneMask);
        
        session.save(newPhoneNumber);
        
        return newPhoneNumber;
    }
    
    /**
     * Delete phone number for specific Contact Id
     * 
     * @param contactId owner of specific phone number
     * @param number un-filtered phone number
     * @return operation completed successfully or not
     */
    public boolean deletePhoneNumber(Integer contactId, String number) {
        
        String numbersOnly = number.replaceAll("\\D", "");
        
        Session session = sessionFactory.getCurrentSession();        
        return session.getNamedQuery("PhoneNumber.deletePhoneNumber")
                .setInteger("contactId", contactId)
                .setString("phoneNumber", numbersOnly).executeUpdate() > 0;
        
    }
    
    /**
     * Find a Contact by Id
     * 
     * @param contactId an id of a contact
     * @return a Contact found by the id
     */
    public Contact findContactById(Integer contactId) {

        
        Session session = sessionFactory.getCurrentSession();
        Contact contact = (Contact) session.get(Contact.class, contactId);
        
        Hibernate.initialize(contact);
        
        return contact;
    }

    
    /**
     * Find a PhoneNumber object by numbers
     * 
     * @param number sequence of digits and format symbols to match
     * @return PhoneNumber object if exists, otherwise returns null 
     */
    public PhoneNumber findPhoneNumber(String number){
        
        String numbersOnly = number.replaceAll("\\D", "");
        
        Session session = sessionFactory.getCurrentSession();    
        Query query = session.getNamedQuery("PhoneNumber.findByPhoneNumber")
                .setString("phoneNumber", numbersOnly);
        
        @SuppressWarnings("unchecked")
        List<PhoneNumber> list = query.list();
        if(!list.isEmpty()){
            return list.get(0);
        }
        
        return null;
    } 
    
    /**
     * Find a PhoneType object by type name
     * 
     * @param typeName name to match the PhoneType
     * @param createIfNotExists create a new object if nothing was found
     * @return PhoneType object if exists or parameter createIfNotExists set to true, otherwise returns null 
     */
    public PhoneType findPhoneType(String typeName, boolean createIfNotExists) {
        
        
        Session session = sessionFactory.getCurrentSession();        
        Query query = session.getNamedQuery("PhoneType.findByPhoneTypeName")
                .setString("phoneTypeName", typeName);
        
        @SuppressWarnings("unchecked")
        List<PhoneType> typeList = query.list();
        if(!typeList.isEmpty()){
            return typeList.get(0);
        }
        
        if(createIfNotExists == false){
            return null;
        }
        
        PhoneType phoneType = new PhoneType();
        phoneType.setPhoneTypeName(typeName);
        
        session.save(phoneType);
        
        return phoneType;        
    }
    
    /** Find Phone Mask by String
     * 
     * @param mask Phone number mask to match
     * @param createIfNotExists create a new object if nothing was found
     * @return PhoneMask object if exists or parameter createIfNotExists set to true, otherwise returns null 
     */
    public PhoneMask findPhoneMask(String mask, boolean createIfNotExists){
        
        Session session = sessionFactory.getCurrentSession();        
        Query query = session.getNamedQuery("PhoneMask.findByPhoneMaskView")
                .setString("phoneMaskView", mask);
        
        @SuppressWarnings("unchecked")
        List<PhoneMask> maskList = query.list();
        if(!maskList.isEmpty()){
            return maskList.get(0);
        }
        
        if(createIfNotExists == false){
            return null;
        }
        
        PhoneMask phoneMask = new PhoneMask();
        phoneMask.setPhoneMaskView(mask);
        
        session.save(phoneMask);       
        
        return phoneMask;
    }
    
    /**
     * Find all Contacts matching the phone number
     * 
     * @param number un-filtered number to match
     * @param match the whole number of just a part of it
     * @return a List of matched Contacts
     */
    public List<Contact> findContactByNumber(String number, boolean match) {

        String numbersOnly = number.replaceAll("\\D", "");
        String numberToMatch;
        
        if(match) {
            numberToMatch = numbersOnly;
        }else {
            numberToMatch = '%' + numbersOnly + '%';
        }
        
        Session session = sessionFactory.getCurrentSession();    
        Query query = session.getNamedQuery("Contact.findByPhoneNumber").setParameter("phoneNumber", numberToMatch);
        
        @SuppressWarnings("unchecked")
        List<Contact> list =  query.list();
        return list;
    }

}
