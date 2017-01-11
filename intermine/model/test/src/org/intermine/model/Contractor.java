package org.intermine.model;

import org.intermine.metadata.TypeUtil;

public class Contractor implements org.intermine.model.Employable
{
    // Ref: org.intermine.model.testmodel.Contractor.personalAddress
    protected org.intermine.model.Address personalAddress;
    public org.intermine.model.Address getPersonalAddress() { return personalAddress; }
    public void setPersonalAddress(final org.intermine.model.Address personalAddress) { this.personalAddress = personalAddress; }

    // Ref: org.intermine.model.testmodel.Contractor.businessAddress
    protected org.intermine.model.Address businessAddress;
    public org.intermine.model.Address getBusinessAddress() { return businessAddress; }
    public void setBusinessAddress(final org.intermine.model.Address businessAddress) { this.businessAddress = businessAddress; }

    // Attr: org.intermine.model.testmodel.Employable.name
    protected java.lang.String name;
    public java.lang.String getName() { return name; }
    public void setName(final java.lang.String name) { this.name = name; }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(final java.lang.Integer id) { this.id = id; }


    @Override public boolean equals(Object o) { return (o instanceof Contractor && id != null) ? id.equals(((Contractor)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "Contractor [businessAddress=" + (businessAddress == null ? "null" : (businessAddress.getId() == null ? "no id" : businessAddress.getId().toString())) + ", id=" + id + ", name=" + (name == null ? "null" : "\"" + name + "\"") + ", personalAddress=" + (personalAddress == null ? "null" : (personalAddress.getId() == null ? "no id" : personalAddress.getId().toString())) + "]"; }
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("personalAddress".equals(fieldName)) {
            return personalAddress;
        }
        if ("businessAddress".equals(fieldName)) {
            return businessAddress;
        }
        if ("name".equals(fieldName)) {
            return name;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        if ("personalAddress".equals(fieldName)) {
            return personalAddress;
        }
        if ("businessAddress".equals(fieldName)) {
            return businessAddress;
        }
        if ("name".equals(fieldName)) {
            return name;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.model.Contractor.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("personalAddress".equals(fieldName)) {
            personalAddress = (org.intermine.model.Address) value;
        } else if ("businessAddress".equals(fieldName)) {
            businessAddress = (org.intermine.model.Address) value;
        } else if ("name".equals(fieldName)) {
            name = (java.lang.String) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else {
            /*if (!org.intermine.model.testmodel.Contractor.class.equals(getClass())) {
                DynamicUtil.setFieldValue(this, fieldName, value);
                return;
            }*/
            throw new IllegalArgumentException("Unknown field " + fieldName);
        }
    }
    public Class<?> getFieldType(final String fieldName) {
        if ("personalAddress".equals(fieldName)) {
            return org.intermine.model.Address.class;
        }
        if ("businessAddress".equals(fieldName)) {
            return org.intermine.model.Address.class;
        }
        if ("name".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("id".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if (!org.intermine.model.Contractor.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.model.Contractor.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
