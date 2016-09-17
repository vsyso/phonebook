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
package org.syso.phonebook.controller;

import org.syso.phonebook.domain.Contact;
import org.syso.phonebook.helpers.ContactsWrapper;
import org.syso.phonebook.domain.PhoneNumber;
import org.syso.phonebook.service.PhonebookService;

import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Contact Controller
 * 
 * @author Vladimir Syso
 */
@RestController
@RequestMapping("/")
public class ContactController {

    @Autowired
    private PhonebookService phonebookService;
   
    /**
     * Display All contact entities
     * 
     * @return ContactsWrapper object
     */
    @RequestMapping(value = "/contacts",
            method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ContactsWrapper getContacts() {

        ContactsWrapper contacts = new ContactsWrapper();
        contacts.setContacts(phonebookService.getAllContacts());
        return contacts;
    }
    
    /**
     * Display Contact entities By phone number
     * 
     * @param number un-filtered number to search
     * @param match the whole number using digits only or search for a sequence of numbers
     * @return ResponseEntity object
     */
    @RequestMapping(value = "/contacts/find_by_number",
            method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<ContactsWrapper> findContactsByNumber(@RequestParam("phone_number") String number,
            @RequestParam(value="match", required = false) boolean match) {

        List<Contact> contactList = phonebookService.findContactByNumber(number, match);
        if(contactList.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        ContactsWrapper contacts = new ContactsWrapper();
        contacts.setContacts(contactList);
        
        return new ResponseEntity<>(contacts, HttpStatus.OK);
    }    
            
    /**
     * Find Contact entity by id
     * 
     * @param contactId to find entity
     * @return ResponseEntity object
     */
    @RequestMapping(value = "/contact/{id}",
            method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Contact> getContact(@PathVariable("id") Integer contactId) {
        
        Contact contact = phonebookService.findContactById(contactId);
        if (contact == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(contact, HttpStatus.OK);
    }

    /**
     * Create new contact entity with first name and last name
     * 
     * @param contact de-serialized object
     * @param ucBuilder path builder
     * @return ResponseEntity object
     */
    @RequestMapping(value = "/contact",
            method = RequestMethod.POST)
    public ResponseEntity<Void> createContact(@RequestBody Contact contact,
            UriComponentsBuilder ucBuilder) {

        if (contact.getFirstName() == null && contact.getLastName() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if(!phonebookService.addContact(contact)){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/contact/{id}").buildAndExpand(contact.getContactId()).toUri());
        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    /**
     * Update Contact by id
     * 
     * @param contactId to edit
     * @param contact object from the POST body
     * @param ucBuilder path builder
     * @return ResponseEntity object
     */
    @RequestMapping(value = "/contact/{id}",
            method = RequestMethod.PUT)
    public ResponseEntity<Void> updateContact(@PathVariable("id") Integer contactId,
            @RequestBody Contact contact,
            UriComponentsBuilder ucBuilder) {

        if (contact.getFirstName() == null && contact.getLastName() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Contact existentContact = phonebookService.findContactById(contactId);
        if (existentContact == null) {

            phonebookService.addContact(contact);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(ucBuilder.path("/contact/{id}").buildAndExpand(contact.getContactId()).toUri().normalize());
            return new ResponseEntity<>(headers, HttpStatus.CREATED);
        }

        existentContact.setFirstName(contact.getFirstName());
        existentContact.setLastName(contact.getLastName());

        phonebookService.updateContact(existentContact);
        return new ResponseEntity<>(HttpStatus.OK);       
    }
    
    /**
     * Remove Contact entity by id
     * 
     * @param contactId to remove an entity
     * @return ResponseEntity object
     */
    @RequestMapping(value = "/contact/{id}",
            method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteContact(@PathVariable("id") Integer contactId) {

        Contact existentContact = phonebookService.findContactById(contactId);
        if (existentContact == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        phonebookService.deleteContact(contactId);
        return new ResponseEntity<>(HttpStatus.OK);       
    }
    
    
    /**
     * Add phone number to specific Contact by id
     * 
     * @param contactId to update entity
     * @param phoneNumber over PhoneNumber entity
     * @return ResponseEntity object
     */
    @RequestMapping(value = "/contact/{id}/add_number",
            method = RequestMethod.POST,
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Void> addPhoneNumber(@PathVariable("id") Integer contactId, 
            @RequestBody PhoneNumber phoneNumber) {

        PhoneNumber newNumber = phonebookService.addNumber(contactId, phoneNumber);
        System.out.println(contactId);
        if (newNumber == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }else if(!Objects.equals(newNumber.getContact().getContactId(), contactId)) {            
            return new ResponseEntity<>(HttpStatus.CONFLICT);  
        }
        
        return new ResponseEntity<>(HttpStatus.CREATED);  
    }
    
    /**
     * Remove a phone number from contact
     * 
     * @param contactId of the owner
     * @param number un-filtered phone number to remove
     * @return ResponseEntity object
     */
    @RequestMapping(value = "/contact/{id}/{phone_number}",
            method = RequestMethod.DELETE)
    public ResponseEntity<Void> deletePhoneNumber(@PathVariable("id") Integer contactId, 
            @PathVariable("phone_number") String number) {
        
        if(phonebookService.deletePhoneNumber(contactId, number) == true) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);  
        }else {        
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);  
        }
    }

}
