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
import org.syso.phonebook.domain.PhoneNumber;
import org.syso.phonebook.helpers.ContactsWrapper;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for Contact Controller
 * 
 * @author Vladimir Syso
 */
public class ContactControllerTest {
    
    public static final String REST_SERVICE_URI = "http://localhost:8080/Phonebook";
    
    private RestTemplate restTemplate;
    private ResponseEntity<Void> testContactResponse;
    private String testContactLocation;
    private boolean deleteContactOnTearDown;    
    
    private final String testFirstName;
    private final String testLastName;    
    private final String testPhoneNumber;
    private final String testPhoneType;
    
    public ContactControllerTest() {        
        this.testFirstName = UUID.randomUUID().toString();
        this.testLastName = UUID.randomUUID().toString();
        this.testPhoneType = "TestPhoneType";
        this.testPhoneNumber = "+09(876)543-21-00";
    }
    
    @BeforeClass
    public static void setUpClass() {
         System.out.println("setUpClass");
    }
    
    @AfterClass
    public static void tearDownClass() {
        System.out.println("tearDownClass");
    }
    
    @Before
    public void setUp() {       
        
        restTemplate = new RestTemplate();
        
        // Create test contact for all tests      
        
        Contact contact = new Contact();
        contact.setFirstName(testFirstName);
        contact.setLastName(testLastName);       

        testContactResponse =  restTemplate.postForEntity(REST_SERVICE_URI + "/contact/", contact, Void.class);
        testContactLocation = testContactResponse.getHeaders().getLocation().toString();
        deleteContactOnTearDown = true;
    }
    
    @After
    public void tearDown() {
        
        // Cleanup test Contact
        if(deleteContactOnTearDown){
            restTemplate.delete(testContactLocation);
        }
    }

    /**
     * Test of getContacts method, of class ContactController.
     */
    @Test
    public void testGetContacts() {
        
        System.out.println("getContacts");          

        Collection<Contact> contacts;         
        contacts = restTemplate.getForObject(REST_SERVICE_URI + "/contacts/", ContactsWrapper.class).getContacts();
        
        assertNotNull(contacts);
        assertFalse(contacts.isEmpty());
        
        Integer contactId = Integer.valueOf(testContactLocation.substring(testContactLocation.lastIndexOf('/') + 1));
        boolean contactFound = false;
        for (Contact contact : contacts) {

            if (Objects.equals(contact.getContactId(), contactId)) {
                assertEquals(contact.getFirstName(), testFirstName);
                assertEquals(contact.getLastName(), testLastName); 
                contactFound = true;
                break;
            }
        }
        
        assertTrue("Test Contact is not enlisted in /contacts", contactFound);
    }

    /**
     * Test of findContactsByNumber method, of class ContactController.
     */
    @Test
    public void testFindContactsByNumber() {
        System.out.println("findContactsByNumber");
       
         // Add a test number
        String jsonRequest = String.format("{\"number\":\"%s\", \"type\":\"%s\"}", testPhoneNumber, testPhoneType);                
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(jsonRequest, headers);
        
        ResponseEntity<String> result = restTemplate.exchange(testContactLocation + "/add_number", HttpMethod.POST, request, String.class);
        assertEquals(result.getStatusCode(), HttpStatus.CREATED);
        
        
        // Test the number can be found       
        Collection<Contact> contacts;        
        Map<String, String> vars = new HashMap<>();
        vars.put("match", "true");
        String findByNumberURI = REST_SERVICE_URI + "/contacts/find_by_number?phone_number=" + testPhoneNumber;
        contacts = restTemplate.getForObject(findByNumberURI, ContactsWrapper.class, vars).getContacts();
        
        assertNotNull(contacts);
        assertEquals(contacts.size(), 1);
        
        Contact contact = contacts.iterator().next();        
        assertNotNull(contact);
        
        Collection<PhoneNumber> phoneNumbers = contact.getPhoneNumberCollection();
        assertNotNull(phoneNumbers);
        assertEquals(phoneNumbers.size(), 1);
        
        PhoneNumber phoneNumber = phoneNumbers.iterator().next();
        assertEquals(phoneNumber.getPhoneNumber(),testPhoneNumber);
        assertNotNull(phoneNumber.getPhoneType());
        assertEquals(phoneNumber.getPhoneType().getPhoneTypeName(),testPhoneType);        
    }

    /**
     * Test of getContact method, of class ContactController.
     */
    @Test
    public void testGetContact() {
        
        System.out.println("getContact");
               
        // Retreive test contact
        Contact contact = restTemplate.getForObject(testContactLocation, Contact.class);
        
        assertEquals(contact.getFirstName(), testFirstName);
        assertEquals(contact.getLastName(), testLastName);        
    }

    /**
     * Test of createContact method, of class ContactController.
     */
    @Test
    public void testCreateContact() {
       
        System.out.println("CreateContact");
        // Test account created in SetUp
        assertEquals(testContactResponse.getStatusCode(), HttpStatus.CREATED);
        
        // Make sure the test contact location provided
        assertNotNull(testContactResponse.getHeaders().getLocation());
        
        Contact testContact = restTemplate.getForObject(testContactLocation, Contact.class);        
        assertEquals(testContact.getFirstName(), testFirstName);
        assertEquals(testContact.getLastName(), testLastName);
        
        // Create another Contact with the same Name with JSON
        String jsonRequest = String.format("{\"firstName\":\"%s\", \"lastName\":\"%s\"}", testFirstName, testLastName);                
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(jsonRequest, headers);
        
        ResponseEntity<String> result = restTemplate.exchange(REST_SERVICE_URI + "/contact", HttpMethod.POST, request, String.class);
        assertEquals(result.getStatusCode(), HttpStatus.CREATED);
        URI location = result.getHeaders().getLocation();
        assertNotNull(location);        
        
        // Test that new contact was created and valid
        String newContactLocation = location.toString();
        Contact newContact = restTemplate.getForObject(newContactLocation, Contact.class);
        assertNotNull(newContact);
        assertEquals(newContact.getFirstName(), testFirstName);
        assertEquals(newContact.getLastName(), testLastName);
                                         
        // Check the Contact created from the SetUp is not the same one created with JSON and the identical name
        Integer contactId = Integer.valueOf(newContactLocation.substring(newContactLocation.lastIndexOf('/') + 1));
        assertNotEquals(contactId, testContact.getContactId());
        
        // Delete the contact
        restTemplate.delete(newContactLocation);
    }

    /**
     * Test of updateContact method, of class ContactController.
     */
    @Test
    public void testUpdateContact() {
        System.out.println("updateContact");

        // Contact to update
        Contact updateContact = new Contact();
        updateContact.setFirstName(UUID.randomUUID().toString());
        updateContact.setLastName(UUID.randomUUID().toString());
        
        // Check contact not updated yet
        Contact createdContact = restTemplate.getForObject(testContactLocation, Contact.class);        
        assertNotEquals(updateContact.getFirstName(), createdContact.getFirstName());
        assertNotEquals(updateContact.getLastName(), createdContact.getLastName());
        
        restTemplate.put(testContactLocation, updateContact);
        
        // Check contact was updated
        Contact updatedContact = restTemplate.getForObject(testContactLocation, Contact.class);        
        assertEquals(updateContact.getFirstName(), updatedContact.getFirstName());
        assertEquals(updateContact.getLastName(), updatedContact.getLastName());
    }

    /**
     * Test of deleteContact method, of class ContactController.
     */
    @Test
    public void testDeleteContact() {
        
        System.out.println("deleteContact");     
        
        // Test contact deleted
        HttpEntity<String> request = new HttpEntity<>("");
        ResponseEntity<String> result;
        result = restTemplate.exchange(testContactLocation, HttpMethod.DELETE, request, String.class);
        assertEquals(result.getStatusCode(), HttpStatus.OK);
        deleteContactOnTearDown = false;
    }

    /**
     * Test of addPhoneNumber method, of class ContactController.
     */
    @Test
    public void testAddPhoneNumber() {
        System.out.println("addPhoneNumber");     
                
        // Add a test number
        String jsonRequest = String.format("{\"number\":\"%s\", \"type\":\"%s\"}", testPhoneNumber, testPhoneType);                
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(jsonRequest, headers);
        
        ResponseEntity<String> result = restTemplate.exchange(testContactLocation + "/add_number", HttpMethod.POST, request, String.class);
        assertEquals(result.getStatusCode(), HttpStatus.CREATED);
        
        // Check number was added to contact
        Contact updatedContact = restTemplate.getForObject(testContactLocation, Contact.class);        
        Collection<PhoneNumber> phoneNumbers = updatedContact.getPhoneNumberCollection();
        
        assertEquals(phoneNumbers.size(), 1);       
        
        PhoneNumber phoneNumber = phoneNumbers.iterator().next();        
        assertEquals(phoneNumber.getPhoneNumber(), testPhoneNumber);  
        assertEquals(phoneNumber.getPhoneType().getPhoneTypeName(), testPhoneType);
    }

    /**
     * Test of deletePhoneNumber method, of class ContactController.
     */
    @Test
    public void testDeletePhoneNumber() {
        System.out.println("deletePhoneNumber");       
        
        // Add a test number
        String jsonRequest = String.format("{\"number\":\"%s\", \"type\":\"%s\"}", testPhoneNumber, testPhoneType);                
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(jsonRequest, headers);
        
        ResponseEntity<String> result = restTemplate.exchange(testContactLocation + "/add_number", HttpMethod.POST, request, String.class);
        assertEquals(result.getStatusCode(), HttpStatus.CREATED);
        
        // Check it was added
        Contact updatedContact = restTemplate.getForObject(testContactLocation, Contact.class);    
        assertEquals(updatedContact.getPhoneNumberCollection().size(), 1); 
        
        // Delete the number
        restTemplate.delete(testContactLocation + "/" + testPhoneNumber);
        
        // Test it was deleted
        updatedContact = restTemplate.getForObject(testContactLocation, Contact.class);     
        assertNull(updatedContact.getPhoneNumberCollection());
    }    
}
