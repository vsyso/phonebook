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
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author Vladimir Syso
 */
@Entity
@Table(name = "phone_type")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "PhoneType.findAll", query = "SELECT p FROM PhoneType p"),
    @NamedQuery(name = "PhoneType.findByPhoneTypeId", query = "SELECT p FROM PhoneType p WHERE p.phoneTypeId = :phoneTypeId"),
    @NamedQuery(name = "PhoneType.findByPhoneTypeName", query = "SELECT p FROM PhoneType p WHERE p.phoneTypeName = :phoneTypeName")})
public class PhoneType implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "phone_type_id")
    private Integer phoneTypeId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 45)
    @Column(name = "phone_type_name")
    private String phoneTypeName;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "type")
    private Collection<PhoneNumber> phoneNumberCollection;

    public PhoneType() {
    }

    public PhoneType(Integer phoneTypeId) {
        this.phoneTypeId = phoneTypeId;
    }

    public PhoneType(Integer phoneTypeId, String phoneTypeName) {
        this.phoneTypeId = phoneTypeId;
        this.phoneTypeName = phoneTypeName;
    }

    public Integer getPhoneTypeId() {
        return phoneTypeId;
    }

    public void setPhoneTypeId(Integer phoneTypeId) {
        this.phoneTypeId = phoneTypeId;
    }

    public String getPhoneTypeName() {
        return phoneTypeName;
    }

    public void setPhoneTypeName(String phoneTypeName) {
        this.phoneTypeName = phoneTypeName;
    }

    @XmlTransient
    public Collection<PhoneNumber> getPhoneNumberCollection() {
        return phoneNumberCollection;
    }

    public void setPhoneNumberCollection(Collection<PhoneNumber> phoneNumberCollection) {
        this.phoneNumberCollection = phoneNumberCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (phoneTypeId != null ? phoneTypeId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PhoneType)) {
            return false;
        }
        PhoneType other = (PhoneType) object;
        if ((this.phoneTypeId == null && other.phoneTypeId != null) || (this.phoneTypeId != null && !this.phoneTypeId.equals(other.phoneTypeId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entities.PhoneType[ phoneTypeId=" + phoneTypeId + " ]";
    }
    
}
