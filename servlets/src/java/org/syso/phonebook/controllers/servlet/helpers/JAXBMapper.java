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

package org.syso.phonebook.controllers.servlet.helpers;

import java.io.StringReader;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
/**
 *
 * @author Vladimir Syso
 * @param <T>
 */
public final class JAXBMapper<T> {

    public static final String MEDIA_TYPE_XML = "application/xml";
    public static final String MEDIA_TYPE_JSON = "application/json";
    
    private final Class<T> entityClass;


    public JAXBMapper(Class<T> entityClass){        
        this.entityClass = entityClass;      
    }
    
    public T unmarshal(String in, String contentType){
       
        try {
            JAXBContext jc = JAXBContextFactory.createContext(new Class<?>[] {entityClass}, null);
            Unmarshaller jaxbUnmarshaller = jc.createUnmarshaller();

            jaxbUnmarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, getMediaType(contentType));
            jaxbUnmarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, true);
            
            StringReader reader = new StringReader(in);
            JAXBElement<T> root = jaxbUnmarshaller.unmarshal(new StreamSource(reader), entityClass);
            return root.getValue();

        } catch (JAXBException ex) {
            Logger.getLogger(JAXBMapper.class.getName()).log(Level.SEVERE, null, ex);
        }        
        return null;
    }

    /**
     *
     * @param object
     * @param out
     * @param acceptType
     * @return response media type, JSON or XML
     */
    public String marshal(Object object, Writer out, String acceptType){
        
        String mediaType = getMediaType(acceptType);
        
        try {
            JAXBContext jc = JAXBContextFactory.createContext(new Class<?>[] {entityClass}, null);
            Marshaller jaxbMarshaller = jc.createMarshaller();

            jaxbMarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, mediaType);
            jaxbMarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, true);
            jaxbMarshaller.marshal(object, out);
            return mediaType;
        } catch (JAXBException ex) {
            Logger.getLogger(JAXBMapper.class.getName()).log(Level.SEVERE, null, ex);
        }        
        
        return null;
    }

    /**
     * @param mediaType to recognize
     * @return recognized media type or XML by default
     */
    protected String getMediaType(String mediaType) {        
        
        if(mediaType != null && mediaType.contains(MEDIA_TYPE_JSON)) {
            return MEDIA_TYPE_JSON;
        }else{
            return MEDIA_TYPE_XML;
        }
    }
    
}
