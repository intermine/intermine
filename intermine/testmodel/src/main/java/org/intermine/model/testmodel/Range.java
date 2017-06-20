package org.intermine.model.testmodel;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.NotXmlParser;
import org.intermine.objectstore.intermine.NotXmlRenderer;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.model.StringConstructor;
import org.intermine.metadata.TypeUtil;
import org.intermine.util.DynamicUtil;

public class Range implements org.intermine.model.InterMineObject
{
    // Attr: org.intermine.model.testmodel.Range.rangeStart
    protected int rangeStart;
    public int getRangeStart() { return rangeStart; }
    public void setRangeStart(final int rangeStart) { this.rangeStart = rangeStart; }

    // Attr: org.intermine.model.testmodel.Range.rangeEnd
    protected int rangeEnd;
    public int getRangeEnd() { return rangeEnd; }
    public void setRangeEnd(final int rangeEnd) { this.rangeEnd = rangeEnd; }

    // Attr: org.intermine.model.testmodel.Range.name
    protected java.lang.String name;
    public java.lang.String getName() { return name; }
    public void setName(final java.lang.String name) { this.name = name; }

    // Ref: org.intermine.model.testmodel.Range.parent
    protected org.intermine.model.InterMineObject parent;
    public org.intermine.model.testmodel.Company getParent() { if (parent instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((org.intermine.model.testmodel.Company) ((org.intermine.objectstore.proxy.ProxyReference) parent).getObject()); }; return (org.intermine.model.testmodel.Company) parent; }
    public void setParent(final org.intermine.model.testmodel.Company parent) { this.parent = parent; }
    public void proxyParent(final org.intermine.objectstore.proxy.ProxyReference parent) { this.parent = parent; }
    public org.intermine.model.InterMineObject proxGetParent() { return parent; }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(final java.lang.Integer id) { this.id = id; }

    @Override public boolean equals(Object o) { return (o instanceof Range && id != null) ? id.equals(((Range)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "Range [id=" + id + ", name=" + (name == null ? "null" : "\"" + name + "\"") + ", parent=" + (parent == null ? "null" : (parent.getId() == null ? "no id" : parent.getId().toString())) + ", rangeEnd=" + rangeEnd + ", rangeStart=" + rangeStart + "]"; }
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("rangeStart".equals(fieldName)) {
            return Integer.valueOf(rangeStart);
        }
        if ("rangeEnd".equals(fieldName)) {
            return Integer.valueOf(rangeEnd);
        }
        if ("name".equals(fieldName)) {
            return name;
        }
        if ("parent".equals(fieldName)) {
            if (parent instanceof ProxyReference) {
                return ((ProxyReference) parent).getObject();
            } else {
                return parent;
            }
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.model.testmodel.Range.class.equals(getClass())) {
            return TypeUtil.getFieldValue(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        if ("rangeStart".equals(fieldName)) {
            return Integer.valueOf(rangeStart);
        }
        if ("rangeEnd".equals(fieldName)) {
            return Integer.valueOf(rangeEnd);
        }
        if ("name".equals(fieldName)) {
            return name;
        }
        if ("parent".equals(fieldName)) {
            return parent;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.model.testmodel.Range.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("rangeStart".equals(fieldName)) {
            rangeStart = ((Integer) value).intValue();
        } else if ("rangeEnd".equals(fieldName)) {
            rangeEnd = ((Integer) value).intValue();
        } else if ("name".equals(fieldName)) {
            name = (java.lang.String) value;
        } else if ("parent".equals(fieldName)) {
            parent = (org.intermine.model.InterMineObject) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else {
            if (!org.intermine.model.testmodel.Range.class.equals(getClass())) {
                DynamicUtil.setFieldValue(this, fieldName, value);
                return;
            }
            throw new IllegalArgumentException("Unknown field " + fieldName);
        }
    }
    public Class<?> getFieldType(final String fieldName) {
        if ("rangeStart".equals(fieldName)) {
            return Integer.TYPE;
        }
        if ("rangeEnd".equals(fieldName)) {
            return Integer.TYPE;
        }
        if ("name".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("parent".equals(fieldName)) {
            return org.intermine.model.testmodel.Company.class;
        }
        if ("id".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if (!org.intermine.model.testmodel.Range.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.model.testmodel.Range.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public StringConstructor getoBJECT() {
        if (!org.intermine.model.testmodel.Range.class.equals(getClass())) {
            return NotXmlRenderer.render(this);
        }
        StringConstructor sb = new StringConstructor();
        sb.append("$_^org.intermine.model.testmodel.Range");
        sb.append("$_^arangeStart$_^").append(rangeStart);
        sb.append("$_^arangeEnd$_^").append(rangeEnd);
        if (name != null) {
            sb.append("$_^aname$_^");
            String string = name;
            while (string != null) {
                int delimPosition = string.indexOf("$_^");
                if (delimPosition == -1) {
                    sb.append(string);
                    string = null;
                } else {
                    sb.append(string.substring(0, delimPosition + 3));
                    sb.append("d");
                    string = string.substring(delimPosition + 3);
                }
            }
        }
        if (parent != null) {
            sb.append("$_^rparent$_^").append(parent.getId());
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
        if (!org.intermine.model.testmodel.Range.class.equals(getClass())) {
            throw new IllegalStateException("Class " + getClass().getName() + " does not match code (org.intermine.model.testmodel.Range)");
        }
        for (int i = 2; i < notXml.length;) {
            int startI = i;
            if ((i < notXml.length) && "arangeStart".equals(notXml[i])) {
                i++;
                rangeStart = Integer.parseInt(notXml[i]);
                i++;
            }
            if ((i < notXml.length) && "arangeEnd".equals(notXml[i])) {
                i++;
                rangeEnd = Integer.parseInt(notXml[i]);
                i++;
            }
            if ((i < notXml.length) && "aname".equals(notXml[i])) {
                i++;
                StringBuilder string = null;
                while ((i + 1 < notXml.length) && (notXml[i + 1].charAt(0) == 'd')) {
                    if (string == null) string = new StringBuilder(notXml[i]);
                    i++;
                    string.append("$_^").append(notXml[i].substring(1));
                }
                name = string == null ? notXml[i] : string.toString();
                i++;
            }
            if ((i < notXml.length) &&"rparent".equals(notXml[i])) {
                i++;
                parent = new ProxyReference(os, Integer.valueOf(notXml[i]), org.intermine.model.testmodel.Company.class);
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
            if (!org.intermine.model.testmodel.Range.class.equals(getClass())) {
                TypeUtil.addCollectionElement(this, fieldName, element);
                return;
            }
            throw new IllegalArgumentException("Unknown collection " + fieldName);
        }
    }
    public Class<?> getElementType(final String fieldName) {
        if (!org.intermine.model.testmodel.Range.class.equals(getClass())) {
            return TypeUtil.getElementType(org.intermine.model.testmodel.Range.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
