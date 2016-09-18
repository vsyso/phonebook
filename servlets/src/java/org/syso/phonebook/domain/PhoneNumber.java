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
package org.syso.phonebook.domain;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.syso.phonebook.helpers.PhoneTypeAdapter;
import javax.persistence.FetchType;

/**
 *
 * @author Vladimir Syso
 */
@Entity
@Table(name = "phone_number")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "PhoneNumber.findAll", query = "SELECT p FROM PhoneNumber p"),
    @NamedQuery(name = "PhoneNumber.findByPhoneNumberId", query = "SELECT p FROM PhoneNumber p WHERE p.phoneNumberPK.phoneNumberId = :phoneNumberId"),
    @NamedQuery(name = "PhoneNumber.findByPhoneNumber", query = "SELECT p FROM PhoneNumber p WHERE p.number = :phoneNumber"),
    @NamedQuery(name = "PhoneNumber.findByContactId", query = "SELECT p FROM PhoneNumber p WHERE p.phoneNumberPK.contactId = :contactId")})
public class PhoneNumber implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected PhoneNumberPK phoneNumberPK;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 45)
    @Column(name = "phone_number")
    private String number;
    @JoinColumn(name = "contact_id", referencedColumnName = "contact_id", insertable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Contact contact;
    @JoinColumn(name = "phone_mask_id", referencedColumnName = "phone_mask_id")
    @ManyToOne(optional = false)
    private PhoneMask phoneMaskId;
    @JoinColumn(name = "phone_type_id", referencedColumnName = "phone_type_id")
    @ManyToOne(optional = false)
    @XmlJavaTypeAdapter(PhoneTypeAdapter.class)
    private PhoneType type;

    public PhoneNumber() {
    }

    public PhoneNumber(PhoneNumberPK phoneNumberPK) {
        this.phoneNumberPK = phoneNumberPK;
    }

    public PhoneNumber(PhoneNumberPK phoneNumberPK, String phoneNumber) {
        this.phoneNumberPK = phoneNumberPK;
        this.number = phoneNumber;
    }

    public PhoneNumber(int phoneNumberId, int contactId) {
        this.phoneNumberPK = new PhoneNumberPK(phoneNumberId, contactId);
    }

    @XmlTransient
    public PhoneNumberPK getPhoneNumberPK() {
        return phoneNumberPK;
    }

    public void setPhoneNumberPK(PhoneNumberPK phoneNumberPK) {
        this.phoneNumberPK = phoneNumberPK;
    }

    public String getNumber() {

        if (phoneMaskId == null) {
            return number;
        }

        String mask = phoneMaskId.getPhoneMaskView();
        if (mask.length() - mask.replace("X", "").length() != number.length()) {
            //Invalid mask
            return number;
        }

        int j = 0;
        char maskArray[] = mask.toCharArray();
        for (int i = 0; i < mask.length(); i++) {
            if (maskArray[i] == 'X') {
                maskArray[i] = number.charAt(j++);
            }
        }
        return new String(maskArray);
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @XmlTransient
    public Contact getContact() {
        return this.contact;
    }
    
    public void setContact(Contact contact) {
        this.contact = contact;
    }

    @XmlTransient
    public PhoneMask getPhoneMaskId() {
        return phoneMaskId;
    }

    public void setPhoneMaskId(PhoneMask phoneMaskId) {
        this.phoneMaskId = phoneMaskId;
    }
    
    @XmlTransient
    public PhoneType getType() {
        return type;
    }

    public void setType(PhoneType type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (phoneNumberPK != null ? phoneNumberPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PhoneNumber)) {
            return false;
        }
        PhoneNumber other = (PhoneNumber) object;
        return !((this.phoneNumberPK == null && other.phoneNumberPK != null) || (this.phoneNumberPK != null && !this.phoneNumberPK.equals(other.phoneNumberPK)));
    }

    @Override
    public String toString() {
        return "entities.PhoneNumber[ phoneNumberPK=" + phoneNumberPK + " ]";
    }

}
