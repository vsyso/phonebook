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
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

/**
 *
 * @author Vladimir Syso
 */
@Embeddable
public class PhoneNumberPK implements Serializable {

    @Basic(optional = false)
    @Column(name = "phone_number_id")
    private int phoneNumberId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "contact_id")
    private int contactId;

    public PhoneNumberPK() {
    }

    public PhoneNumberPK(int phoneNumberId, int contactId) {
        this.phoneNumberId = phoneNumberId;
        this.contactId = contactId;
    }

    public int getPhoneNumberId() {
        return phoneNumberId;
    }

    public void setPhoneNumberId(int phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }

    public int getContactId() {
        return contactId;
    }

    public void setContactId(int contactId) {
        this.contactId = contactId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) phoneNumberId;
        hash += (int) contactId;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PhoneNumberPK)) {
            return false;
        }
        PhoneNumberPK other = (PhoneNumberPK) object;
        if (this.phoneNumberId != other.phoneNumberId) {
            return false;
        }
        if (this.contactId != other.contactId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.syso.phonebook.domain.PhoneNumberPK[ phoneNumberId=" + phoneNumberId + ", contactId=" + contactId + " ]";
    }
    
}
