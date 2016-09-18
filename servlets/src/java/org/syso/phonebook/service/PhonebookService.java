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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TransactionRequiredException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transactional;
import javax.transaction.UserTransaction;
/**
 *
 * @author Vladimir Syso
 */
public class PhonebookService {
    
    EntityManager em;
    UserTransaction ut;
    
    /**
     * Constructs the service wit Entity Manager only
     * 
     * @param em EntityManager object
     */
    public PhonebookService(EntityManager em){
        this.em = em;
        this.ut = null;
    }
    
    /**
     * Constructs the service wit Entity Manager and User Transaction objects
     * 
     * @param em EntityManager object
     * @param ut UserTransaction object
     */
    public PhonebookService(EntityManager em, UserTransaction ut){
        this.em = em;
        this.ut = ut;
    }
    
    /**
     * Find a Contact by Id
     * 
     * @param contactId an id of a contact
     * @return a Contact found by the id
     */
    public Contact findContact(Integer contactId) {        
        return em.find(Contact.class, contactId);
    }
    
    /**
     * Delete a Contact by Id
     * 
     * @param contactId an id of a contact
     * @return operation completed successfully or not
     */
    public boolean deleteContact(Integer contactId) {        
        
        Contact contact = em.find(Contact.class, contactId);
        if (contact == null) {
            return false;
        }
        
        try { 
            if(ut != null){
                ut.begin();
            }
            
            Contact merged = em.merge(contact);
            em.remove(merged);
            em.joinTransaction();
            
            if(ut != null){
                ut.commit();
            }
            
            em.close();
            
        } catch(SecurityException | IllegalStateException | NotSupportedException | SystemException |
                RollbackException | HeuristicMixedException | HeuristicRollbackException ex) {
            
            Logger.getLogger(PhonebookService.class.getName()).log(Level.SEVERE, null, ex);            
            return false;
        }
        
        return true;
    } 
    
    /**
     * Delete phone number for specific Contact Id
     * @param contactId owner of specific phone number
     * @param phoneNumber un-filtered phone number
     * @return operation completed successfully or not
     */
    @Transactional
    public boolean deletePhoneNumber(Integer contactId, String phoneNumber) {        
        
        String numbersOnly = phoneNumber.replaceAll("\\D", "");        
        int entitiesCount = 0;
        try{
            if(ut != null){
                ut.begin();
            }
            entitiesCount = em.createNamedQuery("Contact.deletePhoneNumber", Contact.class)
                    .setParameter("phoneNumber", numbersOnly)
                    .setParameter("contactId", contactId)
                    .executeUpdate();
            
            if(ut != null){
                ut.commit();
            }
            
        }catch(SecurityException | IllegalStateException | NotSupportedException | SystemException |
                RollbackException | HeuristicMixedException | HeuristicRollbackException ex) {            
            Logger.getLogger(PhonebookService.class.getName()).log(Level.SEVERE, null, ex);
        }       
        
        return entitiesCount > 0;
    }
    
    /**
     * Find all contacts
     * 
     * @return a List of all contacts in database
     */
    public List<Contact> findAllContacts() {            
        List<Contact> contactsList;
        
        try{
            contactsList = em.createNamedQuery("Contact.findAll", Contact.class).getResultList();
        }catch(Exception ex)
        {
            Logger.getLogger(PhonebookService.class.getName()).log(Level.SEVERE, null, ex);
            contactsList = new ArrayList<>();  
        }
        return contactsList;
    }
    
    /**
     * Find all Contacts matching the phone number
     * 
     * @param phoneNumber un-filtered number to match
     * @param match the whole number of just a part of it
     * @return a List of matched Contacts
     */
    public List<Contact> findContactsByPhoneNumber(String phoneNumber, Boolean match)
    {
        if(phoneNumber == null || phoneNumber.isEmpty()){
            return null;            
        }
        List<Contact> contactsList;
        String numbersOnly = phoneNumber.replaceAll("\\D", "");
        String numberToMatch;
        
        if(match) {
            numberToMatch = numbersOnly;
        }else {
            numberToMatch = '%' + numbersOnly + '%';
        }
        
        try {            
            contactsList = em.createNamedQuery("Contact.findByPhoneNumber", Contact.class)
                    .setParameter("phoneNumber", numberToMatch)
                    .getResultList();
            
        }catch(NoResultException ex)
        {
            Logger.getLogger(PhonebookService.class.getName()).log(Level.SEVERE, null, ex);
            contactsList = new ArrayList<>();
        }        
        
        return contactsList;
    }

    /**
     * Update Contact by id
     * 
     * @param contactId of a Contact entity to update
     * @param firstName must not be null or empty if lastName is
     * @param lastName must not be null or empty if firstName is
     * @return Updated or newly created Contact object if it wasn't found by contactId
     */
    public Contact updateContact(Integer contactId, String firstName, String lastName) {   
        
        Contact contact = findContact(contactId);
        if (contact == null) {
            return createContact(firstName, lastName);
        }

        if ((firstName != null && firstName.isEmpty()) && (lastName != null && lastName.isEmpty())) {
            return null;
        }
         
        if (firstName != null) {
            contact.setFirstName(firstName);
        }

        if (lastName != null) {
            contact.setLastName(lastName);
        }
        
        Contact updatedContact = null;
        try {
            
           if(ut != null){
                ut.begin();
            }
           
            updatedContact = em.merge(contact);
            em.flush();
            
            if(ut != null){
                ut.commit();
            }
            
        } catch(SecurityException | IllegalStateException | NotSupportedException | SystemException |
                RollbackException | HeuristicMixedException | HeuristicRollbackException ex) {   
            Logger.getLogger(PhonebookService.class.getName()).log(Level.SEVERE, null, ex);
        }     
        
        return updatedContact;
    }

    /**
     * Create a new Contact object with at least First Name or Last Name
     * 
     * @param firstName must not be null or empty if lastName is
     * @param lastName must not be null or empty if firstName is
     * @return a new Contact object
     */
    @Transactional
    public Contact createContact(String firstName, String lastName) {
        
        if ((firstName != null && firstName.isEmpty()) && (lastName != null && lastName.isEmpty())) {
            return null;
        }
        
        Contact contact = new Contact();
        contact.setContactId(0);
        contact.setFirstName(firstName);
        contact.setLastName(lastName);
        
        try {
            if(ut != null){
                ut.begin();
            }
            em.persist(contact);
            em.flush();
            
            if(ut != null){
                ut.commit();
            }
            
            return contact;
            
        } catch(SecurityException | IllegalStateException | NotSupportedException | SystemException |
                RollbackException | HeuristicMixedException | HeuristicRollbackException ex) {   
            Logger.getLogger(PhonebookService.class.getName()).log(Level.SEVERE, null, ex);
        }     
        
        return null;
    }

    /**
     * Add a phone number and link to Contact entity
     * 
     * @param contactId of Contact to place a PhoneNumber
     * @param number phone number to add
     * @param type phone type name
     * @return New PhoneNumber entity
     */
    public PhoneNumber addPhoneNumber(Integer contactId, String number, String type) {
                
        String numbersOnly = number.replaceAll("\\D", "");
        if(numbersOnly.isEmpty()) {
            return null;
        }

        Contact contact = findContact(contactId);
        if(contact == null){
            return null;
        }
        
        String maskOnly = number.replace("X", "").replaceAll("\\d", "X");            
        PhoneMask phoneMask = findPhoneMask(maskOnly, true);
        if (phoneMask == null) {
            return null;
        }

        PhoneType phoneType = findPhoneType(type, true); 
        if (phoneType == null) {
            return null;
        }
        
        PhoneNumber phoneNumber = new PhoneNumber();        
        phoneNumber.setPhoneMaskId(phoneMask);
        phoneNumber.setType(phoneType);
        phoneNumber.setContact(contact);
        phoneNumber.setNumber(numbersOnly);
        phoneNumber.setPhoneNumberPK(new PhoneNumberPK(0, contactId));
        phoneNumber.setPhoneMaskId(phoneMask);
                
        try {
            if(ut != null){
                ut.begin();
            }
            em.persist(phoneNumber);
            em.flush();
            
           if(ut != null){
                ut.commit();
            }
            
        } catch(SecurityException | IllegalStateException | NotSupportedException | SystemException |
                RollbackException | HeuristicMixedException | HeuristicRollbackException ex) {   
            Logger.getLogger(PhonebookService.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }            
        
        return phoneNumber;
    }
    
     /**
     * Find a PhoneNumber object by numbers
     * 
     * @param number sequence of digits and format symbols to match
     * @return PhoneNumber object if exists, otherwise returns null 
     */
    public PhoneNumber findPhoneNumber(String number){
        
        String numbersOnly = number.replaceAll("\\D", "");
        PhoneNumber phoneNumber;
        
        try {
            phoneNumber = em.createNamedQuery("PhoneNumber.findByPhoneNumber", PhoneNumber.class)
                    .setParameter("phoneNumber", numbersOnly)
                    .getSingleResult();
            
            return phoneNumber;            

        } catch (NoResultException e) {
            return null;
        }
    }    
    
    /**
     * Find a PhoneType object by type name
     * 
     * @param typeName name to match the PhoneType
     * @param createIfNotExists create a new object if nothing was found
     * @return PhoneType object if exists or parameter createIfNotExists set to true, otherwise returns null 
     */
    public PhoneType findPhoneType(String typeName, boolean createIfNotExists) {
        
        PhoneType phoneType = null;
        // Get or store Phone Type
        try {
            phoneType = em.createNamedQuery("PhoneType.findByPhoneTypeName", PhoneType.class)
                    .setParameter("phoneTypeName", typeName)
                    .getSingleResult();

        } catch (NoResultException e) {            
            // Create new            
        }
        
        if (phoneType == null && createIfNotExists) {
            
            phoneType = new PhoneType();
            phoneType.setPhoneTypeName(typeName);

            try {
                
                if(ut != null){
                    ut.begin();
                }
                em.persist(phoneType);
                em.flush();
                
                if(ut != null){
                    ut.commit();
                }
            } catch (SecurityException | IllegalStateException | NotSupportedException | SystemException |
                RollbackException | HeuristicMixedException | HeuristicRollbackException ex) {
                Logger.getLogger(PhonebookService.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
        
        return phoneType;
    }
    
    /** Find Phone Mask by String
     * 
     * @param mask Phone number mask to match
     * @param createIfNotExists create a new object if nothing was found
     * @return PhoneMask object if exists or parameter createIfNotExists set to true, otherwise returns null 
     */
    public PhoneMask findPhoneMask(String mask, boolean createIfNotExists){
        
        PhoneMask phoneMask = null;
        try{
            
            phoneMask = em.createNamedQuery("PhoneMask.findByPhoneMaskView", PhoneMask.class)
                    .setParameter("phoneMaskView", mask)
                    .getSingleResult();
            
        }catch(NoResultException e) {
            // Create new
        } 
        
        if (phoneMask == null && createIfNotExists) {
            phoneMask = new PhoneMask();
            phoneMask.setPhoneMaskView(mask);
            
            try {
                if(ut != null){
                    ut.begin();
                }
                em.persist(phoneMask);
                em.flush();
                
                if(ut != null){
                    ut.commit();
                }
                
            } catch ( SecurityException | IllegalStateException | NotSupportedException | SystemException | RollbackException |
                    HeuristicMixedException | HeuristicRollbackException | TransactionRequiredException | EntityExistsException ex) {
                Logger.getLogger(PhonebookService.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
        return phoneMask;
    }
}
