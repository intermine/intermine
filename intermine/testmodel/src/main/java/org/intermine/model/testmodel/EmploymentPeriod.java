package org.intermine.model.testmodel;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.NotXmlParser;
import org.intermine.objectstore.intermine.NotXmlRenderer;
import org.intermine.model.StringConstructor;
import org.intermine.metadata.TypeUtil;
import org.intermine.util.DynamicUtil;

public class EmploymentPeriod implements org.intermine.model.InterMineObject
{
    // Attr: org.intermine.model.testmodel.EmploymentPeriod.startDate
    protected java.util.Date startDate;
    public java.util.Date getStartDate() { return startDate; }
    public void setStartDate(final java.util.Date startDate) { this.startDate = startDate; }

    // Attr: org.intermine.model.testmodel.EmploymentPeriod.endDate
    protected java.util.Date endDate;
    public java.util.Date getEndDate() { return endDate; }
    public void setEndDate(final java.util.Date endDate) { this.endDate = endDate; }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(final java.lang.Integer id) { this.id = id; }

    @Override public boolean equals(Object o) { return (o instanceof EmploymentPeriod && id != null) ? id.equals(((EmploymentPeriod)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "EmploymentPeriod [endDate=" + (endDate == null ? "null" : "\"" + endDate + "\"") + ", id=" + id + ", startDate=" + (startDate == null ? "null" : "\"" + startDate + "\"") + "]"; }
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("startDate".equals(fieldName)) {
            return startDate;
        }
        if ("endDate".equals(fieldName)) {
            return endDate;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.model.testmodel.EmploymentPeriod.class.equals(getClass())) {
            return TypeUtil.getFieldValue(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        if ("startDate".equals(fieldName)) {
            return startDate;
        }
        if ("endDate".equals(fieldName)) {
            return endDate;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.model.testmodel.EmploymentPeriod.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("startDate".equals(fieldName)) {
            startDate = (java.util.Date) value;
        } else if ("endDate".equals(fieldName)) {
            endDate = (java.util.Date) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else {
            if (!org.intermine.model.testmodel.EmploymentPeriod.class.equals(getClass())) {
                DynamicUtil.setFieldValue(this, fieldName, value);
                return;
            }
            throw new IllegalArgumentException("Unknown field " + fieldName);
        }
    }
    public Class<?> getFieldType(final String fieldName) {
        if ("startDate".equals(fieldName)) {
            return java.util.Date.class;
        }
        if ("endDate".equals(fieldName)) {
            return java.util.Date.class;
        }
        if ("id".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if (!org.intermine.model.testmodel.EmploymentPeriod.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.model.testmodel.EmploymentPeriod.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public StringConstructor getoBJECT() {
        if (!org.intermine.model.testmodel.EmploymentPeriod.class.equals(getClass())) {
            return NotXmlRenderer.render(this);
        }
        StringConstructor sb = new StringConstructor();
        sb.append("$_^org.intermine.model.testmodel.EmploymentPeriod");
        if (startDate != null) {
            sb.append("$_^astartDate$_^").append(startDate.getTime());
        }
        if (endDate != null) {
            sb.append("$_^aendDate$_^").append(endDate.getTime());
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
        if (!org.intermine.model.testmodel.EmploymentPeriod.class.equals(getClass())) {
            throw new IllegalStateException("Class " + getClass().getName() + " does not match code (org.intermine.model.testmodel.EmploymentPeriod)");
        }
        for (int i = 2; i < notXml.length;) {
            int startI = i;
            if ((i < notXml.length) && "astartDate".equals(notXml[i])) {
                i++;
                startDate = new java.util.Date(Long.parseLong(notXml[i]));
                i++;
            }
            if ((i < notXml.length) && "aendDate".equals(notXml[i])) {
                i++;
                endDate = new java.util.Date(Long.parseLong(notXml[i]));
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
            if (!org.intermine.model.testmodel.EmploymentPeriod.class.equals(getClass())) {
                TypeUtil.addCollectionElement(this, fieldName, element);
                return;
            }
            throw new IllegalArgumentException("Unknown collection " + fieldName);
        }
    }
    public Class<?> getElementType(final String fieldName) {
        if (!org.intermine.model.testmodel.EmploymentPeriod.class.equals(getClass())) {
            return TypeUtil.getElementType(org.intermine.model.testmodel.EmploymentPeriod.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
