package org.intermine.model.testmodel;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.NotXmlParser;
import org.intermine.objectstore.intermine.NotXmlRenderer;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.model.StringConstructor;
import org.intermine.metadata.TypeUtil;
import org.intermine.util.DynamicUtil;
import org.intermine.model.ShadowClass;

public class HasSecretarysShadow implements HasSecretarys, ShadowClass
{
    public static final Class<HasSecretarys> shadowOf = HasSecretarys.class;
    // Col: org.intermine.model.testmodel.HasSecretarys.secretarys
    protected java.util.Set<org.intermine.model.testmodel.Secretary> secretarys = new java.util.HashSet<org.intermine.model.testmodel.Secretary>();
    public java.util.Set<org.intermine.model.testmodel.Secretary> getSecretarys() { return secretarys; }
    public void setSecretarys(final java.util.Set<org.intermine.model.testmodel.Secretary> secretarys) { this.secretarys = secretarys; }
    public void addSecretarys(final org.intermine.model.testmodel.Secretary arg) { secretarys.add(arg); }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(final java.lang.Integer id) { this.id = id; }

    @Override public boolean equals(Object o) { return (o instanceof HasSecretarys && id != null) ? id.equals(((HasSecretarys)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "HasSecretarys [id=" + id + "]"; }
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("secretarys".equals(fieldName)) {
            return secretarys;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.model.testmodel.HasSecretarys.class.equals(getClass())) {
            return TypeUtil.getFieldValue(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        if ("secretarys".equals(fieldName)) {
            return secretarys;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.model.testmodel.HasSecretarys.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("secretarys".equals(fieldName)) {
            secretarys = (java.util.Set) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else {
            if (!org.intermine.model.testmodel.HasSecretarys.class.equals(getClass())) {
                DynamicUtil.setFieldValue(this, fieldName, value);
                return;
            }
            throw new IllegalArgumentException("Unknown field " + fieldName);
        }
    }
    public Class<?> getFieldType(final String fieldName) {
        if ("secretarys".equals(fieldName)) {
            return java.util.Set.class;
        }
        if ("id".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if (!org.intermine.model.testmodel.HasSecretarys.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.model.testmodel.HasSecretarys.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public StringConstructor getoBJECT() {
        if (!org.intermine.model.testmodel.HasSecretarysShadow.class.equals(getClass())) {
            return NotXmlRenderer.render(this);
        }
        StringConstructor sb = new StringConstructor();
        sb.append("$_^org.intermine.model.testmodel.HasSecretarys");
        if (id != null) {
            sb.append("$_^aid$_^").append(id);
        }
        return sb;
    }
    public void setoBJECT(String notXml, ObjectStore os) {
        setoBJECT(NotXmlParser.SPLITTER.split(notXml), os);
    }
    public void setoBJECT(final String[] notXml, final ObjectStore os) {
        if (!org.intermine.model.testmodel.HasSecretarysShadow.class.equals(getClass())) {
            throw new IllegalStateException("Class " + getClass().getName() + " does not match code (org.intermine.model.testmodel.HasSecretarys)");
        }
        for (int i = 2; i < notXml.length;) {
            int startI = i;
            if ((i < notXml.length) && "aid".equals(notXml[i])) {
                i++;
                id = Integer.valueOf(notXml[i]);
                i++;
            }
            if (startI == i) {
                throw new IllegalArgumentException("Unknown field " + notXml[i]);
            }
        }
        secretarys = new ProxyCollection<org.intermine.model.testmodel.Secretary>(os, this, "secretarys", org.intermine.model.testmodel.Secretary.class);
    }
    public void addCollectionElement(final String fieldName, final org.intermine.model.InterMineObject element) {
        if ("secretarys".equals(fieldName)) {
            secretarys.add((org.intermine.model.testmodel.Secretary) element);
        } else {
            if (!org.intermine.model.testmodel.HasSecretarys.class.equals(getClass())) {
                TypeUtil.addCollectionElement(this, fieldName, element);
                return;
            }
            throw new IllegalArgumentException("Unknown collection " + fieldName);
        }
    }
    public Class<?> getElementType(final String fieldName) {
        if ("secretarys".equals(fieldName)) {
            return org.intermine.model.testmodel.Secretary.class;
        }
        if (!org.intermine.model.testmodel.HasSecretarys.class.equals(getClass())) {
            return TypeUtil.getElementType(org.intermine.model.testmodel.HasSecretarys.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
