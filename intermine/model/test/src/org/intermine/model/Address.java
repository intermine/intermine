package org.intermine.model;

import javax.swing.JTree.DynamicUtilTreeNode;

import org.intermine.metadata.TypeUtil;

public class Address implements org.intermine.model.Thing
{
    // Attr: org.intermine.model.testmodel.Address.address
    protected java.lang.String address;
    public java.lang.String getAddress() { return address; }
    public void setAddress(final java.lang.String address) { this.address = address; }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(final java.lang.Integer id) { this.id = id; }

    @Override public boolean equals(Object o) { return (o instanceof Address && id != null) ? id.equals(((Address)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "Address [address=" + (address == null ? "null" : "\"" + address + "\"") + ", id=" + id + "]";}

    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("address".equals(fieldName)) {
            return address;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.model.Address.class.equals(getClass())) {
            return TypeUtil.getFieldValue(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        if ("address".equals(fieldName)) {
            return address;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.model.Address.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("address".equals(fieldName)) {
            address = (java.lang.String) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else {
            /*if (!org.intermine.model.Address.class.equals(getClass())) {
                DynamicUtil.setFieldValue(this, fieldName, value);
                return;
            }*/
            throw new IllegalArgumentException("Unknown field " + fieldName);
        }
    }
    public Class<?> getFieldType(final String fieldName) {
        if ("address".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("id".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if (!org.intermine.model.Address.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.model.Address.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
