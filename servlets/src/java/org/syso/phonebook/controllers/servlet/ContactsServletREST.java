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

import org.syso.phonebook.domain.Contacts;
import org.syso.phonebook.controllers.helpers.JAXBMapper;
import org.syso.phonebook.service.PhonebookService;
import org.syso.phonebook.domain.Contact;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

/**
 * Contacts Servlet RESTful Service
 * 
 * @author Vladimir Syso
 */
@WebServlet(name = "ContactsServletREST", urlPatterns = {"/ContactsServletREST"})
public class ContactsServletREST extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_ENCODING = "UTF-8";
     
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
        
        response.setCharacterEncoding(DEFAULT_ENCODING);
        
        try (PrintWriter out = response.getWriter()) {
            
            String pathInfo = request.getPathInfo();
            Pair<Integer, String> responseParam;
            
            String acceptType = request.getHeader("accept");
            if (pathInfo != null && pathInfo.equals("/find_by_number")) {                
                responseParam = displayContactsByPhoneNumber(acceptType, out, request);                
            } else if(pathInfo == null || pathInfo.equals("/")){
                responseParam = displayAllContacts(acceptType, out);
            }else{
                responseParam = new Pair<>(HttpServletResponse.SC_NOT_FOUND, null);
            }
            
            String mediaType = responseParam.getValue();
            if(mediaType != null) {                
                response.setContentType(mediaType);
            }
            
            int statusCode = responseParam.getKey();
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
     * Processes GET /contacts/find_by_number?{phone_number}
     * 
     * @param acceptType JSON or XML
     * @param out Writer
     * @param request servlet request
     * @return a Pair of Response code and Media Type (might be null if no content to return)
     */
    protected Pair<Integer, String> displayContactsByPhoneNumber(String acceptType, PrintWriter out, HttpServletRequest request)
    {
        Boolean match = Boolean.valueOf(request.getParameter("match"));
        String phoneNumber = request.getParameter("phone_number");       
        
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return new Pair<>(HttpServletResponse.SC_BAD_REQUEST, null);
        }      
            
        List<Contact> contactsList = phonebook.findContactsByPhoneNumber(phoneNumber, match);        
        return displayContacts(acceptType, out, contactsList);
    }
    
    /**
     * Processes GET /contacts
     * 
     * @param acceptType JSON or XML
     * @param out Writer
     * @return a Pair of Response code and Media Type (might be null if no content to return)
     */
    protected Pair<Integer, String> displayAllContacts(String acceptType, PrintWriter out)
    {
        List<Contact> contactsList = phonebook.findAllContacts();
        return displayContacts(acceptType, out, contactsList);
    }
    
    /**
     * Serializes a Contact Entities List to XML or JSON format
     * 
     * @param acceptType JSON or XML
     * @param out Writer
     * @param contactsList to display
     * @return a Pair of Response code and Media Type (might be null if no content to return)
     */
    protected Pair<Integer, String> displayContacts(String acceptType, PrintWriter out, List<Contact> contactsList)
    {
        if (contactsList == null || contactsList.isEmpty()) {
            return new Pair<>(HttpServletResponse.SC_NOT_FOUND, null);
        }
        Contacts wrapper = new Contacts();
        wrapper.setContacts(contactsList);        

        JAXBMapper<Contacts> mapper;
        mapper = new JAXBMapper<>(Contacts.class);
        String mediaType = mapper.marshal(wrapper, out, acceptType);

        return new Pair<>(HttpServletResponse.SC_OK, mediaType);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Phonebook Contacts";
    }

}
