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

import javax.annotation.PostConstruct;
import org.syso.phonebook.service.PhonebookService;
import org.syso.phonebook.domain.Contact;
import org.syso.phonebook.domain.PhoneNumber;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Contact Facade REST
 * 
 * @author Vladimir Syso
 */
@Stateless
@Path("contact")
public class ContactFacadeREST{

    @PersistenceContext(unitName = "PhonebookPU")
    private EntityManager em;
    private PhonebookService phonebook;
    
    public ContactFacadeREST() {
    }
    
    @PostConstruct
    public void init() {
        phonebook = new PhonebookService(em);
    }
    
    /**
     * Create new contact entity with first name and last name
     * 
     * @param uriInfo Context
     * @param contact de-serialized object
     * @return Response object
     */
    @POST
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response createContact(@Context UriInfo uriInfo, Contact contact) {
        
        if (contact == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        
        Contact newContact = phonebook.createContact(contact.getFirstName(), contact.getLastName());

        if(newContact == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        
        String requestURI = uriInfo.getPath();        
        if(!requestURI.endsWith("/")) {
            requestURI = requestURI + "/";
        }
        return Response.status(Response.Status.CREATED).header("Location", requestURI + newContact.getContactId().toString()).build();
    }
    
    /**
     * Add phone number to specific Contact by id
     * 
     * @param contactId to update entity
     * @param phoneNumber over PhoneNumber entity
     * @return Response object
     */
    @POST
    @Path("{id}/add_number")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response addPhoneNumber(@PathParam("id") Integer contactId, PhoneNumber phoneNumber) {
        
        if(phoneNumber == null || phoneNumber.getType() == null){
            return Response.status(Response.Status.BAD_REQUEST).build(); 
        }
        
        if(phonebook.findPhoneNumber(phoneNumber.getNumber()) != null) {            
             return Response.status(Response.Status.CONFLICT).build(); 
        }        
        
        phoneNumber = phonebook.addPhoneNumber(contactId, phoneNumber.getNumber(), phoneNumber.getType().getPhoneTypeName());        
        if(phoneNumber == null) {
           return Response.status(Response.Status.NOT_FOUND).build(); 
        }        
        
        Contact owner = phoneNumber.getContact();
        if (owner == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        
        if (!owner.getContactId().equals(contactId)) {
            return Response.status(Response.Status.CONFLICT).build();
        }
        
        return Response.status(Response.Status.CREATED).build();
    }

    /**
     * Edit Contact entity by id
     * 
     * @param uriInfo context to get URI path
     * @param contactId to edit an entity
     * @param contact de-serialized object
     * @return Response object
     */
    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response editContact(@Context UriInfo uriInfo, @PathParam("id") Integer contactId, Contact contact) {          

        if(contact == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Contact updatedContact = phonebook.updateContact(contactId, contact.getFirstName(), contact.getLastName());
        
        if(updatedContact == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();            
        }            
        Integer updatedId = updatedContact.getContactId();
        
        if(updatedId.equals(contactId)) {            
            return Response.status(Response.Status.NO_CONTENT).build();            
        }else{
            
            // Replace to new index in the URI    
            String requestURI = uriInfo.getPath();
            int slashIndex = requestURI.lastIndexOf("/");
            requestURI = requestURI.substring(0, slashIndex) + "/";                  
            return Response.status(Response.Status.CREATED).header("Location", requestURI + updatedId.toString()).build();
        } 
    }

    /**
     * Remove Contact entity by id
     * 
     * @param contactId to remove an entity
     * @return Response object
     */
    @DELETE
    @Path("{id}")
    public Response removeContact(@PathParam("id") Integer contactId) {

        if (phonebook.deleteContact(contactId)) {
            return Response.status(Response.Status.NO_CONTENT).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
    
    /**
     * Remove phone number from contact
     * 
     * @param contactId of the owner of number
     * @param phoneNumber un-filtered number to remove
     * @return Response object
     */
    @DELETE
    @Path("/{id}/{phone_number}")
    public Response removePhoneNumber(@PathParam("id") Integer contactId,
                                  @PathParam("phone_number") String phoneNumber) {

        if(phonebook.deletePhoneNumber(contactId, phoneNumber)) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }else {        
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * Find Contact entity by id
     * 
     * @param contactId to find entity
     * @return Response object
     */
    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response findContact(@PathParam("id") Integer contactId) {

        Contact contact = phonebook.findContact(contactId);  
        
        if(contact == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }       
        return Response.ok().entity(contact).build();       
    }    
}
