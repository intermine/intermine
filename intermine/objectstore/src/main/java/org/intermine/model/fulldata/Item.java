package org.intermine.model.fulldata;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.NotXmlParser;
import org.intermine.objectstore.intermine.NotXmlRenderer;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.model.StringConstructor;
import org.intermine.metadata.TypeUtil;
import org.intermine.util.DynamicUtil;

public class Item implements org.intermine.model.InterMineObject
{
    // Attr: org.intermine.model.fulldata.Item.className
    protected java.lang.String className;
    public java.lang.String getClassName() { return className; }
    public void setClassName(final java.lang.String className) { this.className = className; }

    // Attr: org.intermine.model.fulldata.Item.implementations
    protected java.lang.String implementations;
    public java.lang.String getImplementations() { return implementations; }
    public void setImplementations(final java.lang.String implementations) { this.implementations = implementations; }

    // Attr: org.intermine.model.fulldata.Item.identifier
    protected java.lang.String identifier;
    public java.lang.String getIdentifier() { return identifier; }
    public void setIdentifier(final java.lang.String identifier) { this.identifier = identifier; }

    // Col: org.intermine.model.fulldata.Item.attributes
    protected java.util.Set<org.intermine.model.fulldata.Attribute> attributes = new java.util.HashSet<org.intermine.model.fulldata.Attribute>();
    public java.util.Set<org.intermine.model.fulldata.Attribute> getAttributes() { return attributes; }
    public void setAttributes(final java.util.Set<org.intermine.model.fulldata.Attribute> attributes) { this.attributes = attributes; }
    public void addAttributes(final org.intermine.model.fulldata.Attribute arg) { attributes.add(arg); }

    // Col: org.intermine.model.fulldata.Item.collections
    protected java.util.Set<org.intermine.model.fulldata.ReferenceList> collections = new java.util.HashSet<org.intermine.model.fulldata.ReferenceList>();
    public java.util.Set<org.intermine.model.fulldata.ReferenceList> getCollections() { return collections; }
    public void setCollections(final java.util.Set<org.intermine.model.fulldata.ReferenceList> collections) { this.collections = collections; }
    public void addCollections(final org.intermine.model.fulldata.ReferenceList arg) { collections.add(arg); }

    // Col: org.intermine.model.fulldata.Item.references
    protected java.util.Set<org.intermine.model.fulldata.Reference> references = new java.util.HashSet<org.intermine.model.fulldata.Reference>();
    public java.util.Set<org.intermine.model.fulldata.Reference> getReferences() { return references; }
    public void setReferences(final java.util.Set<org.intermine.model.fulldata.Reference> references) { this.references = references; }
    public void addReferences(final org.intermine.model.fulldata.Reference arg) { references.add(arg); }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(final java.lang.Integer id) { this.id = id; }

    @Override public boolean equals(Object o) { return (o instanceof Item && id != null) ? id.equals(((Item)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "Item [className=" + (className == null ? "null" : "\"" + className + "\"") + ", id=" + id + ", identifier=" + (identifier == null ? "null" : "\"" + identifier + "\"") + ", implementations=" + (implementations == null ? "null" : "\"" + implementations + "\"") + "]"; }
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("className".equals(fieldName)) {
            return className;
        }
        if ("implementations".equals(fieldName)) {
            return implementations;
        }
        if ("identifier".equals(fieldName)) {
            return identifier;
        }
        if ("attributes".equals(fieldName)) {
            return attributes;
        }
        if ("collections".equals(fieldName)) {
            return collections;
        }
        if ("references".equals(fieldName)) {
            return references;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.model.fulldata.Item.class.equals(getClass())) {
            return TypeUtil.getFieldValue(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        if ("className".equals(fieldName)) {
            return className;
        }
        if ("implementations".equals(fieldName)) {
            return implementations;
        }
        if ("identifier".equals(fieldName)) {
            return identifier;
        }
        if ("attributes".equals(fieldName)) {
            return attributes;
        }
        if ("collections".equals(fieldName)) {
            return collections;
        }
        if ("references".equals(fieldName)) {
            return references;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.model.fulldata.Item.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("className".equals(fieldName)) {
            className = (java.lang.String) value;
        } else if ("implementations".equals(fieldName)) {
            implementations = (java.lang.String) value;
        } else if ("identifier".equals(fieldName)) {
            identifier = (java.lang.String) value;
        } else if ("attributes".equals(fieldName)) {
            attributes = (java.util.Set) value;
        } else if ("collections".equals(fieldName)) {
            collections = (java.util.Set) value;
        } else if ("references".equals(fieldName)) {
            references = (java.util.Set) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else {
            if (!org.intermine.model.fulldata.Item.class.equals(getClass())) {
                DynamicUtil.setFieldValue(this, fieldName, value);
                return;
            }
            throw new IllegalArgumentException("Unknown field " + fieldName);
        }
    }
    public Class<?> getFieldType(final String fieldName) {
        if ("className".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("implementations".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("identifier".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("attributes".equals(fieldName)) {
            return java.util.Set.class;
        }
        if ("collections".equals(fieldName)) {
            return java.util.Set.class;
        }
        if ("references".equals(fieldName)) {
            return java.util.Set.class;
        }
        if ("id".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if (!org.intermine.model.fulldata.Item.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.model.fulldata.Item.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public StringConstructor getoBJECT() {
        if (!org.intermine.model.fulldata.Item.class.equals(getClass())) {
            return NotXmlRenderer.render(this);
        }
        StringConstructor sb = new StringConstructor();
        sb.append("$_^org.intermine.model.fulldata.Item");
        if (className != null) {
            sb.append("$_^aclassName$_^");
            String string = className;
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
        if (implementations != null) {
            sb.append("$_^aimplementations$_^");
            String string = implementations;
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
        if (identifier != null) {
            sb.append("$_^aidentifier$_^");
            String string = identifier;
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
        if (id != null) {
            sb.append("$_^aid$_^").append(id);
        }
        return sb;
    }
    public void setoBJECT(String notXml, ObjectStore os) {
        setoBJECT(NotXmlParser.SPLITTER.split(notXml), os);
    }
    public void setoBJECT(final String[] notXml, final ObjectStore os) {
        if (!org.intermine.model.fulldata.Item.class.equals(getClass())) {
            throw new IllegalStateException("Class " + getClass().getName() + " does not match code (org.intermine.model.fulldata.Item)");
        }
        for (int i = 2; i < notXml.length;) {
            int startI = i;
            if ((i < notXml.length) && "aclassName".equals(notXml[i])) {
                i++;
                StringBuilder string = null;
                while ((i + 1 < notXml.length) && (notXml[i + 1].charAt(0) == 'd')) {
                    if (string == null) string = new StringBuilder(notXml[i]);
                    i++;
                    string.append("$_^").append(notXml[i].substring(1));
                }
                className = string == null ? notXml[i] : string.toString();
                i++;
            }
            if ((i < notXml.length) && "aimplementations".equals(notXml[i])) {
                i++;
                StringBuilder string = null;
                while ((i + 1 < notXml.length) && (notXml[i + 1].charAt(0) == 'd')) {
                    if (string == null) string = new StringBuilder(notXml[i]);
                    i++;
                    string.append("$_^").append(notXml[i].substring(1));
                }
                implementations = string == null ? notXml[i] : string.toString();
                i++;
            }
            if ((i < notXml.length) && "aidentifier".equals(notXml[i])) {
                i++;
                StringBuilder string = null;
                while ((i + 1 < notXml.length) && (notXml[i + 1].charAt(0) == 'd')) {
                    if (string == null) string = new StringBuilder(notXml[i]);
                    i++;
                    string.append("$_^").append(notXml[i].substring(1));
                }
                identifier = string == null ? notXml[i] : string.toString();
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
        attributes = new ProxyCollection<org.intermine.model.fulldata.Attribute>(os, this, "attributes", org.intermine.model.fulldata.Attribute.class);
        collections = new ProxyCollection<org.intermine.model.fulldata.ReferenceList>(os, this, "collections", org.intermine.model.fulldata.ReferenceList.class);
        references = new ProxyCollection<org.intermine.model.fulldata.Reference>(os, this, "references", org.intermine.model.fulldata.Reference.class);
    }
    public void addCollectionElement(final String fieldName, final org.intermine.model.InterMineObject element) {
        if ("attributes".equals(fieldName)) {
            attributes.add((org.intermine.model.fulldata.Attribute) element);
        } else if ("collections".equals(fieldName)) {
            collections.add((org.intermine.model.fulldata.ReferenceList) element);
        } else if ("references".equals(fieldName)) {
            references.add((org.intermine.model.fulldata.Reference) element);
        } else {
            if (!org.intermine.model.fulldata.Item.class.equals(getClass())) {
                TypeUtil.addCollectionElement(this, fieldName, element);
                return;
            }
            throw new IllegalArgumentException("Unknown collection " + fieldName);
        }
    }
    public Class<?> getElementType(final String fieldName) {
        if ("attributes".equals(fieldName)) {
            return org.intermine.model.fulldata.Attribute.class;
        }
        if ("collections".equals(fieldName)) {
            return org.intermine.model.fulldata.ReferenceList.class;
        }
        if ("references".equals(fieldName)) {
            return org.intermine.model.fulldata.Reference.class;
        }
        if (!org.intermine.model.fulldata.Item.class.equals(getClass())) {
            return TypeUtil.getElementType(org.intermine.model.fulldata.Item.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
