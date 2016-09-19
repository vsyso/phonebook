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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.syso.phonebook.controllers.helpers.JAXBMapper;
import org.syso.phonebook.domain.Contact;
import org.syso.phonebook.domain.Contacts;
import org.syso.phonebook.domain.PhoneNumber;
import org.syso.phonebook.domain.PhoneType;

/**
 * PhonebookServiceTest performs unit tests for Servlet and Jersey
 * implementations of the Phonebook REST API
 * 
 * @author Vladimir Syso
 */
@RunWith(value = Parameterized.class)
public class PhonebookServiceTest {
    
    // Test two following serives via Parameters
    public static final String JERSEY_REST_SERVICE_URL = "http://localhost:8080/Phonebook/jersey";
    public static final String SERVLET_REST_SERVICE_URL = "http://localhost:8080/Phonebook/servlet";
    
    @Parameter
    public String baseURL;
    
    CloseableHttpClient httpClient;
    
    private final String testFirstName;
    private final String testLastName;    
    private final String testPhoneNumber;
    private final String testPhoneType;
    
    private String testContactLocation;
    private boolean deleteContactOnTearDown;

    public PhonebookServiceTest() {
        this.testFirstName = UUID.randomUUID().toString();
        this.testLastName = UUID.randomUUID().toString();
        this.testPhoneType = "TestPhoneType";
        this.testPhoneNumber = "+09(876)543-21-00";
    }
    
    /**
     *
     * @return an array of service URL
     */
    @Parameters
    public static Object[] data() {
        return new Object[]{
                JERSEY_REST_SERVICE_URL,
                SERVLET_REST_SERVICE_URL
        };
    }

    @Before
    public void setUp() throws IOException {
        
        // Create test contact for all tests       
        Contact contact = new Contact();
        contact.setFirstName(testFirstName);
        contact.setLastName(testLastName);
        
        JAXBMapper<Contact> adapter = new JAXBMapper<>(Contact.class);
        
        StringWriter out = new StringWriter();
        
        adapter.marshal(contact, out, JAXBMapper.MEDIA_TYPE_XML);
        HttpResponse response = Request.Post(baseURL + "/contact")
                .bodyString(out.toString(), ContentType.APPLICATION_XML)
                .execute().returnResponse();
        
        Header[] headers = response.getHeaders("Location");        
        testContactLocation = headers[0].getValue();
        
        deleteContactOnTearDown = true;
    }

    @After
    public void tearDown() throws IOException {        
       if(deleteContactOnTearDown){
           Request.Delete(testContactLocation).execute();
       }
    }

    /**
     * Test of findAllContacts method, of class PhonebookService via HTTP
     * @throws IOException
     */
    @Test
    public void testFindAllContacts() throws IOException {
        
        System.out.println("getContacts");
        
        // Get all contacts
        Content content = Request.Get(baseURL + "/contacts/").execute().returnContent();
        
        JAXBMapper<Contacts> adapter = new JAXBMapper<>(Contacts.class);        
        Contacts contacts = adapter.unmarshal(content.asString(), JAXBMapper.MEDIA_TYPE_XML);
        assertNotNull(contacts);
        
        Collection<Contact> contactList = contacts.getContacts();       
        assertFalse(contactList.isEmpty());
        
        // Check the test contact in the list
        Integer contactId = Integer.valueOf(testContactLocation.substring(testContactLocation.lastIndexOf('/') + 1));
        boolean contactFound = false;
        for (Contact contact : contactList) {

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
     * Test of addPhoneNumber method, of class PhonebookService.
     * @throws java.io.IOException
     */
    @Test
    public void testAddPhoneNumber() throws IOException {
        System.out.println("addPhoneNumber");     
                
        // Add a test number
        String jsonRequest = String.format("{\"number\":\"%s\", \"type\":\"%s\"}", testPhoneNumber, testPhoneType);     
        
        Response response = Request.Post(testContactLocation + "/add_number")
                .bodyString(jsonRequest, ContentType.APPLICATION_JSON)
                .execute();        
        
        assertEquals(response.returnResponse().getStatusLine().getStatusCode(), HttpStatus.SC_CREATED);
        
        // Check number was added to contact
        
        response = Request.Get(testContactLocation).execute();       
        Content content = response.returnContent();
        
        JAXBMapper<Contact> adapter = new JAXBMapper<>(Contact.class);
        Contact contact = adapter.unmarshal(content.asString(), JAXBMapper.MEDIA_TYPE_XML);
        assertNotNull(contact);        
                
        Collection<PhoneNumber> phoneNumbers = contact.getPhoneNumbers();
        
        assertNotNull(phoneNumbers);
        assertEquals(phoneNumbers.size(), 1);       
        
        PhoneNumber phoneNumber = phoneNumbers.iterator().next();
        assertNotNull(phoneNumber.getNumber());
        assertEquals(phoneNumber.getNumber(), testPhoneNumber);  
        assertNotNull(phoneNumber.getType());
        assertEquals(phoneNumber.getType().getPhoneTypeName(), testPhoneType);
    }
    
    /**
     * Test of findContactsByNumber method, of class PhonebookService.
     * @throws java.io.IOException
     */
    @Test
    public void testFindContactsByNumber() throws IOException {
        System.out.println("findContactsByNumber");
       
         // Add a test number
        String jsonRequest = String.format("{\"number\":\"%s\", \"type\":\"%s\"}", testPhoneNumber, testPhoneType);     
        
        Response response = Request.Post(testContactLocation + "/add_number")
                .bodyString(jsonRequest, ContentType.APPLICATION_JSON)
                .execute();       
                
        assertEquals(response.returnResponse().getStatusLine().getStatusCode(), HttpStatus.SC_CREATED);        
        
        // Test the test contact can be found by number
        String findByNumberURL = baseURL + "/contacts/find_by_number?phone_number=" + testPhoneNumber + "&match=true";
        response = Request.Get(findByNumberURL).execute();
        
        Content content = response.returnContent();
        
        JAXBMapper<Contacts> adapter = new JAXBMapper<>(Contacts.class); 
        Contacts contacts = adapter.unmarshal(content.asString(), JAXBMapper.MEDIA_TYPE_XML);
        assertNotNull(contacts);
        
        Collection<Contact> contactList = contacts.getContacts();       
        assertNotNull(contactList);
        assertFalse(contactList.isEmpty());
        assertEquals(contactList.size(), 1);
        
        Contact contact = contactList.iterator().next();        
        assertNotNull(contact);
        
        Collection<PhoneNumber> phoneNumbers = contact.getPhoneNumbers();
        assertNotNull(phoneNumbers);
        assertEquals(phoneNumbers.size(), 1);
        
        PhoneNumber phoneNumber = phoneNumbers.iterator().next();
        assertEquals(phoneNumber.getNumber(),testPhoneNumber);
        assertNotNull(phoneNumber.getType());
        assertEquals(phoneNumber.getType().getPhoneTypeName(),testPhoneType);        
    }
    
    /**
     * Test of findContact method, of class PhonebookService.
     * @throws java.io.IOException
     */
    @Test
    public void testGetContact() throws IOException {
        
        System.out.println("getContact");
               
        // Retreive test contact
        Response response = Request.Get(testContactLocation).execute();       
        Content content = response.returnContent();
        
        JAXBMapper<Contact> adapter = new JAXBMapper<>(Contact.class);
        Contact contact = adapter.unmarshal(content.asString(), JAXBMapper.MEDIA_TYPE_XML);
        assertNotNull(contact);        
        assertEquals(contact.getFirstName(), testFirstName);
        assertEquals(contact.getLastName(), testLastName);        
    }
    
    /**
     * Test of createContact method, of class PhonebookService.
     * @throws java.io.IOException
     */
    @Test
    public void testCreateContact() throws IOException {
       
        System.out.println("CreateContact");
        
        // Test account created in SetUp      
        Response response = Request.Get(testContactLocation).execute();       
        Content content = response.returnContent();
        
        JAXBMapper<Contact> adapter = new JAXBMapper<>(Contact.class);
        Contact testContact = adapter.unmarshal(content.asString(), content.getType().getMimeType());       
        assertEquals(testContact.getFirstName(), testFirstName);
        assertEquals(testContact.getLastName(), testLastName);
        
        // Create another Contact with the same Name with JSON
        String jsonRequest = String.format("{\"firstName\":\"%s\", \"lastName\":\"%s\"}", testFirstName, testLastName); 
        response = Request.Post(baseURL + "/contact")
                .bodyString(jsonRequest, ContentType.APPLICATION_JSON)
                .execute();        
        
        HttpResponse httpResponse = response.returnResponse();
        assertEquals(httpResponse.getStatusLine().getStatusCode(), HttpStatus.SC_CREATED);
        
        Header[] headers = httpResponse.getHeaders("Location"); 
        assertNotNull(headers);
        String newContactLocation = headers[0].getValue();
        assertNotNull(newContactLocation);     
        
        // Test that new contact was created and valid
        response = Request.Get(newContactLocation).execute();       
        content = response.returnContent();
        
        Contact newContact = adapter.unmarshal(content.asString(), content.getType().getMimeType());
        assertNotNull(newContact);
        assertEquals(newContact.getFirstName(), testFirstName);
        assertEquals(newContact.getLastName(), testLastName);
                                         
        // Check the Contact created from the SetUp is not the same one created with JSON and the identical name
        Integer contactId = Integer.valueOf(newContactLocation.substring(newContactLocation.lastIndexOf('/') + 1));
        assertNotEquals(contactId, testContact.getContactId());
        
        // Delete the contact
        response = Request.Delete(testContactLocation).execute();
        assertEquals(response.returnResponse().getStatusLine().getStatusCode(), HttpStatus.SC_NO_CONTENT);        
    }
    
    /**
     * Test of updateContact method, of class PhonebookService.
     * @throws java.io.IOException
     */
    @Test
    public void testUpdateContact() throws IOException {
        System.out.println("updateContact");

        // Generate info to update
        String updateFirstName = "Updated";
        String updateLastName = UUID.randomUUID().toString();
        
        // Check contact not updated yet
        Response response = Request.Get(testContactLocation).execute();       
        Content content = response.returnContent();
        
        JAXBMapper<Contact> adapter = new JAXBMapper<>(Contact.class);
        Contact createdContact = adapter.unmarshal(content.asString(), content.getType().getMimeType());   
        assertNotEquals(updateFirstName, createdContact.getFirstName());
        assertNotEquals(updateLastName, createdContact.getLastName());
        
        // Update the test contact
        String jsonRequest = String.format("{\"firstName\":\"%s\", \"lastName\":\"%s\"}", updateFirstName, updateLastName); 
        HttpResponse httpResponse = Request.Put(testContactLocation)
                .bodyString(jsonRequest, ContentType.APPLICATION_JSON)
                .execute().returnResponse();
        
        assertEquals(httpResponse.getStatusLine().getStatusCode(), HttpStatus.SC_NO_CONTENT);
        
        // Check contact was updated
        response = Request.Get(testContactLocation).execute();       
        content = response.returnContent();
        Contact updatedContact = adapter.unmarshal(content.asString(), content.getType().getMimeType());   
        
        assertEquals(updateFirstName, updatedContact.getFirstName());
        assertEquals(updateLastName, updatedContact.getLastName());
    }
    
    /**
     * Test of deleteContact method, of class PhonebookService.
     * @throws java.io.IOException
     */
    @Test
    public void testDeleteContact() throws IOException {
        
        System.out.println("deleteContact");     
        
        // Test contact deleted
        Response response = Request.Delete(testContactLocation).execute();        
        assertEquals(response.returnResponse().getStatusLine().getStatusCode(), HttpStatus.SC_NO_CONTENT);
        deleteContactOnTearDown = false;
    }
    
    /**
     * Test of deletePhoneNumber method, of class PhonebookService.
     * @throws java.io.IOException
     */
    @Test
    public void testDeletePhoneNumber() throws IOException {
        System.out.println("deletePhoneNumber");       
        
        // Add a test number
        String jsonRequest = String.format("{\"number\":\"%s\", \"type\":\"%s\"}", testPhoneNumber, testPhoneType);     
        
        Response response = Request.Post(testContactLocation + "/add_number")
                .bodyString(jsonRequest, ContentType.APPLICATION_JSON)
                .execute();        
        
        assertEquals(response.returnResponse().getStatusLine().getStatusCode(), HttpStatus.SC_CREATED);
        
        // Check it was added
        response = Request.Get(testContactLocation).execute();       
        Content content = response.returnContent();
        JAXBMapper<Contact> adapter = new JAXBMapper<>(Contact.class);
        Contact contact = adapter.unmarshal(content.asString(), content.getType().getMimeType());
        
        assertNotNull(contact);
        assertNotNull(contact.getPhoneNumbers());
        assertEquals(contact.getPhoneNumbers().size(), 1); 
        
        PhoneNumber phoneNumber = contact.getPhoneNumbers().iterator().next();
        assertNotNull(phoneNumber.getNumber());
        assertEquals(phoneNumber.getNumber(), testPhoneNumber);
        
        PhoneType phoneType = phoneNumber.getType();
        assertNotNull(phoneType);
        assertEquals(phoneType.getPhoneTypeName(), testPhoneType);        
        
        // Delete the number
        response = Request.Delete(testContactLocation + "/" + testPhoneNumber).execute();        
        assertEquals(response.returnResponse().getStatusLine().getStatusCode(), HttpStatus.SC_NO_CONTENT);
        
        // Test it was deleted
        response = Request.Get(testContactLocation).execute();       
        content = response.returnContent();
        contact = adapter.unmarshal(content.asString(), content.getType().getMimeType());
        
        assertNotNull(contact);
        assertNull(contact.getPhoneNumbers());
    }
}
