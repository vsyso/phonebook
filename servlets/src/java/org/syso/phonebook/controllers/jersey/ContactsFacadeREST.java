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
package org.syso.phonebook.controllers.jersey;

import org.syso.phonebook.service.PhonebookService;
import org.syso.phonebook.domain.Contact;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Contacts Facade REST
 * 
 * @author Vladimir Syso
 */
@Stateless
@Path("contacts")
public class ContactsFacadeREST {

    @PersistenceContext(unitName = "PhonebookPU")
    private EntityManager em;
    
    public ContactsFacadeREST() {        
    }
    
    /**
     * Display Contact entities By phone number
     * 
     * @param phoneNumber un-filtered number to search
     * @param match the whole number using digits only or search for a sequence of numbers
     * @return Response object
     */
    @GET
    @Path("find_by_number")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response displayContactsByPhoneNumber(@QueryParam("phone_number") String phoneNumber,
                                           @DefaultValue("false") @QueryParam("match") boolean match) {  
        
        PhonebookService phonebook = new PhonebookService(em);        
        List<Contact> contactsList = phonebook.findContactsByPhoneNumber(phoneNumber, match);
        
        if(contactsList == null || contactsList.isEmpty()){
            return Response.status(Response.Status.NOT_FOUND).build();
        }        
        GenericEntity<List<Contact>> entity = new GenericEntity<List<Contact>>(contactsList) {};
        return Response.ok().entity(entity).build();
    }

    /**
     * Display All contact entities
     * 
     * @return Response object
     */
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response displayAllContacts() {
        PhonebookService phonebook = new PhonebookService(em);
        List<Contact> contactsList = phonebook.findAllContacts();
        
        if(contactsList == null || contactsList.isEmpty()){
            return Response.status(Response.Status.NOT_FOUND).build();
        }        
        GenericEntity<List<Contact>> entity = new GenericEntity<List<Contact>>(contactsList) {};
        return Response.ok().entity(entity).build();
    } 
}
