package org.intermine.model.fulldata;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.NotXmlParser;
import org.intermine.objectstore.intermine.NotXmlRenderer;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.model.StringConstructor;
import org.intermine.metadata.TypeUtil;
import org.intermine.util.DynamicUtil;

public class Reference implements org.intermine.model.FastPathObject
{
    // Attr: org.intermine.model.fulldata.Reference.name
    protected java.lang.String name;
    public java.lang.String getName() { return name; }
    public void setName(final java.lang.String name) { this.name = name; }

    // Attr: org.intermine.model.fulldata.Reference.refId
    protected java.lang.String refId;
    public java.lang.String getRefId() { return refId; }
    public void setRefId(final java.lang.String refId) { this.refId = refId; }

    // Ref: org.intermine.model.fulldata.Reference.item
    protected org.intermine.model.InterMineObject item;
    public org.intermine.model.fulldata.Item getItem() { if (item instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((org.intermine.model.fulldata.Item) ((org.intermine.objectstore.proxy.ProxyReference) item).getObject()); }; return (org.intermine.model.fulldata.Item) item; }
    public void setItem(final org.intermine.model.fulldata.Item item) { this.item = item; }
    public void proxyItem(final org.intermine.objectstore.proxy.ProxyReference item) { this.item = item; }
    public org.intermine.model.InterMineObject proxGetItem() { return item; }

    @Override public String toString() { return "Reference [item=" + (item == null ? "null" : (item.getId() == null ? "no id" : item.getId().toString())) + ", name=" + (name == null ? "null" : "\"" + name + "\"") + ", refId=" + (refId == null ? "null" : "\"" + refId + "\"") + "]"; }
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("name".equals(fieldName)) {
            return name;
        }
        if ("refId".equals(fieldName)) {
            return refId;
        }
        if ("item".equals(fieldName)) {
            if (item instanceof ProxyReference) {
                return ((ProxyReference) item).getObject();
            } else {
                return item;
            }
        }
        if (!org.intermine.model.fulldata.Reference.class.equals(getClass())) {
            return TypeUtil.getFieldValue(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        if ("name".equals(fieldName)) {
            return name;
        }
        if ("refId".equals(fieldName)) {
            return refId;
        }
        if ("item".equals(fieldName)) {
            return item;
        }
        if (!org.intermine.model.fulldata.Reference.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("name".equals(fieldName)) {
            name = (java.lang.String) value;
        } else if ("refId".equals(fieldName)) {
            refId = (java.lang.String) value;
        } else if ("item".equals(fieldName)) {
            item = (org.intermine.model.InterMineObject) value;
        } else {
            if (!org.intermine.model.fulldata.Reference.class.equals(getClass())) {
                DynamicUtil.setFieldValue(this, fieldName, value);
                return;
            }
            throw new IllegalArgumentException("Unknown field " + fieldName);
        }
    }
    public Class<?> getFieldType(final String fieldName) {
        if ("name".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("refId".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("item".equals(fieldName)) {
            return org.intermine.model.fulldata.Item.class;
        }
        if (!org.intermine.model.fulldata.Reference.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.model.fulldata.Reference.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
