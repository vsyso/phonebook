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

package org.syso.phonebook.controllers.servlet;

import org.syso.phonebook.controllers.servlet.helpers.JAXBMapper;
import org.syso.phonebook.service.PhonebookService;
import org.syso.phonebook.domain.Contact;
import org.syso.phonebook.domain.PhoneNumber;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

/**
 *  Contact Servlet RESTful Service
 * 
 * @author Vladimir Syso
 */
public class ContactServletREST extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    @PersistenceContext(unitName = "PhonebookPU")
    private EntityManager em;   
    @Resource
    UserTransaction ut;    
    private PhonebookService phonebook;
    
    @Override
    public void init(ServletConfig config) throws ServletException{        
        super.init(config);        
        phonebook = new PhonebookService(em, ut);
    }

    /**
     * Processes requests for HTTP <code>GET</code>, <code>POST</code>, <code>DELETE</code> and <code>PUT</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo;
        String[] pathParts = null;
        Integer contactId = null;

        pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            pathParts = pathInfo.split("/");
            if (pathParts.length > 0) {
                try {
                    contactId = Integer.valueOf(pathParts[1]);
                } catch (NumberFormatException e) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
        }
        
        String requestMethod = request.getMethod();
        if(!requestMethod.equals("POST") && contactId == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
            
        try (PrintWriter out = response.getWriter()) { 
            
            Pair<Integer, String> responseParam;
            int statusCode;
            
            switch(request.getMethod()) {
                case "GET":
                    
                    responseParam = displayContactById(request.getHeader("accept"), out, contactId);
                    break;
                case "POST":     
                    
                    if(pathParts != null && pathParts.length > 0){
                        // Checking for pattern contact/{id}/add_number
                        if(pathParts.length == 3 && pathParts[2].equals("add_number")) {
                            statusCode = addPhoneNumber(request.getContentType(), out, request, contactId);                            
                        }else{
                            statusCode = HttpServletResponse.SC_NOT_FOUND;
                        }
                    }else{                    
                        statusCode = createContact(request.getContentType(), request, response);
                    }                    
                    
                    responseParam = new Pair<>(statusCode, null);
                    break;
                case "DELETE":                    
                    if(pathParts != null && pathParts.length == 3){
                        statusCode = deletePhoneNumber(contactId, pathParts[2]);
                    }else{
                        statusCode = deleteContact(contactId);
                    }
                    responseParam = new Pair<>(statusCode, null);
                    break;
                case "PUT":
                    statusCode = updateContact(request.getContentType(), contactId, request, response);
                    responseParam = new Pair<>(statusCode, null);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    return;
            }
           
            String mediaType = responseParam.getValue();
            if(mediaType != null) {                
                response.setContentType(mediaType);
            }
            statusCode = responseParam.getKey();
            if (statusCode >= HttpServletResponse.SC_OK && statusCode <= HttpServletResponse.SC_NO_CONTENT) {
                response.setStatus(statusCode);
            } else {
                response.sendError(statusCode);
            }
        }catch (IllegalStateException | IOException ex) {
            Logger.getLogger(ContactsServletREST.class.getName()).log(Level.SEVERE, null, ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } 
    }
    
    /**
     * Processes GET /contact/{id}
     * 
     * @param acceptType JSON or XML
     * @param out Writer
     * @param contactId to display
     * @return a Pair of Response code and Media Type (might be null if no content to return)
     */
    protected Pair<Integer, String> displayContactById(String acceptType, PrintWriter out, Integer contactId)
    {
        Contact contact = phonebook.findContact(contactId);  
        
        if(contact == null) {
            return new Pair<>(HttpServletResponse.SC_NOT_FOUND, null);
        }
        
        JAXBMapper<Contact> adapter = new JAXBMapper<>(Contact.class);
        String mediaType = adapter.marshal(contact, out, acceptType);
        
        return new Pair<>(HttpServletResponse.SC_OK, mediaType);
    }
    
    /**
     * Processes POST /contact/{id}/add_number
     * 
     * @param contentType JSON or XML
     * @param out Writer
     * @param request servlet Request
     * @param contactId  to add the number
     * @return a Status code
     */
    protected int addPhoneNumber(String contentType, PrintWriter out, HttpServletRequest request, Integer contactId) {

        String data = getPostData(request);       
        if(data == null) {
            return HttpServletResponse.SC_BAD_REQUEST;
        }
            
        JAXBMapper<PhoneNumber> adapter = new JAXBMapper<>(PhoneNumber.class);        
        PhoneNumber phoneNumber = adapter.unmarshal(data, contentType);

        if (phoneNumber == null || phoneNumber.getType() == null) {
            return HttpServletResponse.SC_BAD_REQUEST;
        }
        
        if(phonebook.findPhoneNumber(phoneNumber.getNumber()) != null)
        {
            return HttpServletResponse.SC_CONFLICT;
        }
        
        phoneNumber = phonebook.addPhoneNumber(contactId, phoneNumber.getNumber(), phoneNumber.getType().getPhoneTypeName());        
        if(phoneNumber == null) {
           return HttpServletResponse.SC_NOT_FOUND; 
        }        
        
        Contact owner = phoneNumber.getContact();
        if (owner == null) {
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        
        if (!owner.getContactId().equals(contactId)) {
            return HttpServletResponse.SC_CONFLICT;
        }
        return HttpServletResponse.SC_CREATED;
    }
    /**
     * Processes DELETE contact/{id}/{phone_number}
     * 
     * @param contactId to delete the number
     * @param phoneNumber to add
     * @return a Status Code
     */
    protected int deletePhoneNumber(Integer contactId, String phoneNumber)
    { 
        if(phonebook.deletePhoneNumber(contactId, phoneNumber)) {
            return HttpServletResponse.SC_NO_CONTENT;
        }else {        
            return HttpServletResponse.SC_NOT_FOUND;
        }
    }
    
    /**
     * Processes DELETE /contact/{id}
     * 
     * @param contactId to delete
     * @return a Status Code
     */
    protected int deleteContact(Integer contactId)
    {
        if (phonebook.deleteContact(contactId)) {
            return HttpServletResponse.SC_NO_CONTENT;
        } else {
            return HttpServletResponse.SC_NOT_FOUND;
        }
    }
    
    /**
     * Processes PUT /contact/{id}
     * 
     * @param contentType JSON or XML
     * @param contactId to update
     * @param request servlet request
     * @param response servlet response
     * @return a Status Code
     */
    protected int updateContact(String contentType, Integer contactId, HttpServletRequest request, HttpServletResponse response)
    {
        String data = getPostData(request);
        if(data == null) {
            return HttpServletResponse.SC_BAD_REQUEST;
        }
            
        JAXBMapper<Contact> adapter = new JAXBMapper<>(Contact.class);
        Contact contact = adapter.unmarshal(data, contentType);

        if (contact == null || 
                (contact.getFirstName() == null && contact.getLastName() == null) ||
                (contact.getFirstName().isEmpty() && contact.getLastName().isEmpty())) {
            return HttpServletResponse.SC_BAD_REQUEST;
        }

        Contact updatedContact = phonebook.updateContact(contactId, contact.getFirstName(), contact.getLastName());
        
        if(updatedContact == null) {            
            return HttpServletResponse.SC_BAD_REQUEST;            
        }else if(updatedContact.getContactId().equals(contactId)) {            
            return HttpServletResponse.SC_NO_CONTENT;            
        }else{            
            // Replace to new index in the URI
            String requestURI = request.getRequestURI();        
            int slashIndex = requestURI.lastIndexOf("/");
            requestURI = requestURI.substring(0, slashIndex) + "/";
            
            response.setHeader("Location", requestURI + updatedContact.getContactId().toString());
        
            return HttpServletResponse.SC_CREATED;
        }      
    }
    
    /**
     * Processes POST /contact
     * 
     * @param contentType JSON or XML
     * @param request servlet request
     * @param response servlet response
     * @return a Status Code
     */
    protected int createContact(String contentType, HttpServletRequest request, HttpServletResponse response) {
     
        String data = getPostData(request);
        if(data == null) {
            return HttpServletResponse.SC_BAD_REQUEST;
        }
        
        JAXBMapper<Contact> adapter = new JAXBMapper<>(Contact.class);
        Contact contact = adapter.unmarshal(data, contentType);
        if (contact == null) {
            return HttpServletResponse.SC_BAD_REQUEST;
        }
          
        Contact newContact = phonebook.createContact(contact.getFirstName(), contact.getLastName());
        if (newContact == null) {
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        
        String requestURL = request.getRequestURL().toString();        
        if(!requestURL.endsWith("/")) {
            requestURL = requestURL + "/";
        }        
        
        response.setHeader("Location", requestURL + newContact.getContactId().toString());
        return HttpServletResponse.SC_CREATED;
    }
    
    /**
     * Retrieves POST data from request
     * 
     * @param request servlet request
     * @return content of the POST request
     */
    protected String getPostData(HttpServletRequest request)
    {
        StringBuilder buffer  = new StringBuilder();        
        String line;
        
        try {            
            BufferedReader reader = request.getReader();            
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        }catch(IOException ex)
        {
            return null;
        }
        
        return buffer.toString();
    }
    
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {        
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /**
     * Handles the HTTP <code>DELETE</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /**
     * Handles the HTTP <code>PUT</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Phonebook Contact";
    }// </editor-fold>

}
