package org.intermine.model.testmodel;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.NotXmlParser;
import org.intermine.objectstore.intermine.NotXmlRenderer;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.model.StringConstructor;
import org.intermine.metadata.TypeUtil;
import org.intermine.util.DynamicUtil;
import org.intermine.model.ShadowClass;

public class HasAddressShadow implements HasAddress, ShadowClass
{
    public static final Class<HasAddress> shadowOf = HasAddress.class;
    // Ref: org.intermine.model.testmodel.HasAddress.address
    protected org.intermine.model.InterMineObject address;
    public org.intermine.model.testmodel.Address getAddress() { if (address instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((org.intermine.model.testmodel.Address) ((org.intermine.objectstore.proxy.ProxyReference) address).getObject()); }; return (org.intermine.model.testmodel.Address) address; }
    public void setAddress(final org.intermine.model.testmodel.Address address) { this.address = address; }
    public void proxyAddress(final org.intermine.objectstore.proxy.ProxyReference address) { this.address = address; }
    public org.intermine.model.InterMineObject proxGetAddress() { return address; }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(final java.lang.Integer id) { this.id = id; }

    @Override public boolean equals(Object o) { return (o instanceof HasAddress && id != null) ? id.equals(((HasAddress)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "HasAddress [address=" + (address == null ? "null" : (address.getId() == null ? "no id" : address.getId().toString())) + ", id=" + id + "]"; }
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("address".equals(fieldName)) {
            if (address instanceof ProxyReference) {
                return ((ProxyReference) address).getObject();
            } else {
                return address;
            }
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.model.testmodel.HasAddress.class.equals(getClass())) {
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
        if (!org.intermine.model.testmodel.HasAddress.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("address".equals(fieldName)) {
            address = (org.intermine.model.InterMineObject) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else {
            if (!org.intermine.model.testmodel.HasAddress.class.equals(getClass())) {
                DynamicUtil.setFieldValue(this, fieldName, value);
                return;
            }
            throw new IllegalArgumentException("Unknown field " + fieldName);
        }
    }
    public Class<?> getFieldType(final String fieldName) {
        if ("address".equals(fieldName)) {
            return org.intermine.model.testmodel.Address.class;
        }
        if ("id".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if (!org.intermine.model.testmodel.HasAddress.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.model.testmodel.HasAddress.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public StringConstructor getoBJECT() {
        if (!org.intermine.model.testmodel.HasAddressShadow.class.equals(getClass())) {
            return NotXmlRenderer.render(this);
        }
        StringConstructor sb = new StringConstructor();
        sb.append("$_^org.intermine.model.testmodel.HasAddress");
        if (address != null) {
            sb.append("$_^raddress$_^").append(address.getId());
        }
        if (id != null) {
            sb.append("$_^aid$_^").append(id);
        }
        return sb;
    }
    public void setoBJECT(String notXml, ObjectStore os) {
        setoBJECT(NotXmlParser.SPLITTER.split(notXml), os);
    }
    public void setoBJECT(final String[] notXml, final ObjectStore os) {
        if (!org.intermine.model.testmodel.HasAddressShadow.class.equals(getClass())) {
            throw new IllegalStateException("Class " + getClass().getName() + " does not match code (org.intermine.model.testmodel.HasAddress)");
        }
        for (int i = 2; i < notXml.length;) {
            int startI = i;
            if ((i < notXml.length) &&"raddress".equals(notXml[i])) {
                i++;
                address = new ProxyReference(os, Integer.valueOf(notXml[i]), org.intermine.model.testmodel.Address.class);
                i++;
            };
            if ((i < notXml.length) && "aid".equals(notXml[i])) {
                i++;
                id = Integer.valueOf(notXml[i]);
                i++;
            }
            if (startI == i) {
                throw new IllegalArgumentException("Unknown field " + notXml[i]);
            }
        }
    }
    public void addCollectionElement(final String fieldName, final org.intermine.model.InterMineObject element) {
        {
            if (!org.intermine.model.testmodel.HasAddress.class.equals(getClass())) {
                TypeUtil.addCollectionElement(this, fieldName, element);
                return;
            }
            throw new IllegalArgumentException("Unknown collection " + fieldName);
        }
    }
    public Class<?> getElementType(final String fieldName) {
        if (!org.intermine.model.testmodel.HasAddress.class.equals(getClass())) {
            return TypeUtil.getElementType(org.intermine.model.testmodel.HasAddress.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
