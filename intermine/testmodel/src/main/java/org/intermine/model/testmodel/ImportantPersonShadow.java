package org.intermine.model.testmodel;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.NotXmlParser;
import org.intermine.objectstore.intermine.NotXmlRenderer;
import org.intermine.model.StringConstructor;
import org.intermine.metadata.TypeUtil;
import org.intermine.util.DynamicUtil;
import org.intermine.model.ShadowClass;

public class ImportantPersonShadow implements ImportantPerson, ShadowClass
{
    public static final Class<ImportantPerson> shadowOf = ImportantPerson.class;
    // Attr: org.intermine.model.testmodel.ImportantPerson.seniority
    protected java.lang.Integer seniority;
    public java.lang.Integer getSeniority() { return seniority; }
    public void setSeniority(final java.lang.Integer seniority) { this.seniority = seniority; }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(final java.lang.Integer id) { this.id = id; }

    @Override public boolean equals(Object o) { return (o instanceof ImportantPerson && id != null) ? id.equals(((ImportantPerson)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "ImportantPerson [id=" + id + ", seniority=" + seniority + "]"; }
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("seniority".equals(fieldName)) {
            return seniority;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.model.testmodel.ImportantPerson.class.equals(getClass())) {
            return TypeUtil.getFieldValue(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        if ("seniority".equals(fieldName)) {
            return seniority;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.model.testmodel.ImportantPerson.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("seniority".equals(fieldName)) {
            seniority = (java.lang.Integer) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else {
            if (!org.intermine.model.testmodel.ImportantPerson.class.equals(getClass())) {
                DynamicUtil.setFieldValue(this, fieldName, value);
                return;
            }
            throw new IllegalArgumentException("Unknown field " + fieldName);
        }
    }
    public Class<?> getFieldType(final String fieldName) {
        if ("seniority".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if ("id".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if (!org.intermine.model.testmodel.ImportantPerson.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.model.testmodel.ImportantPerson.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public StringConstructor getoBJECT() {
        if (!org.intermine.model.testmodel.ImportantPersonShadow.class.equals(getClass())) {
            return NotXmlRenderer.render(this);
        }
        StringConstructor sb = new StringConstructor();
        sb.append("$_^org.intermine.model.testmodel.ImportantPerson");
        if (seniority != null) {
            sb.append("$_^aseniority$_^").append(seniority);
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
        if (!org.intermine.model.testmodel.ImportantPersonShadow.class.equals(getClass())) {
            throw new IllegalStateException("Class " + getClass().getName() + " does not match code (org.intermine.model.testmodel.ImportantPerson)");
        }
        for (int i = 2; i < notXml.length;) {
            int startI = i;
            if ((i < notXml.length) && "aseniority".equals(notXml[i])) {
                i++;
                seniority = Integer.valueOf(notXml[i]);
                i++;
            }
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
            if (!org.intermine.model.testmodel.ImportantPerson.class.equals(getClass())) {
                TypeUtil.addCollectionElement(this, fieldName, element);
                return;
            }
            throw new IllegalArgumentException("Unknown collection " + fieldName);
        }
    }
    public Class<?> getElementType(final String fieldName) {
        if (!org.intermine.model.testmodel.ImportantPerson.class.equals(getClass())) {
            return TypeUtil.getElementType(org.intermine.model.testmodel.ImportantPerson.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
