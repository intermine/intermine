package org.intermine.api.userprofile;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.NotXmlParser;
import org.intermine.objectstore.intermine.NotXmlRenderer;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.model.StringConstructor;
import org.intermine.metadata.TypeUtil;
import org.intermine.util.DynamicUtil;

public class SavedQuery implements org.intermine.model.InterMineObject
{
    // Attr: org.intermine.api.userprofile.SavedQuery.query
    protected java.lang.String query;
    public java.lang.String getQuery() { return query; }
    public void setQuery(final java.lang.String query) { this.query = query; }

    // Ref: org.intermine.api.userprofile.SavedQuery.userProfile
    protected org.intermine.model.InterMineObject userProfile;
    public org.intermine.api.userprofile.UserProfile getUserProfile() { if (userProfile instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((org.intermine.api.userprofile.UserProfile) ((org.intermine.objectstore.proxy.ProxyReference) userProfile).getObject()); }; return (org.intermine.api.userprofile.UserProfile) userProfile; }
    public void setUserProfile(final org.intermine.api.userprofile.UserProfile userProfile) { this.userProfile = userProfile; }
    public void proxyUserProfile(final org.intermine.objectstore.proxy.ProxyReference userProfile) { this.userProfile = userProfile; }
    public org.intermine.model.InterMineObject proxGetUserProfile() { return userProfile; }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(final java.lang.Integer id) { this.id = id; }

    @Override public boolean equals(Object o) { return (o instanceof SavedQuery && id != null) ? id.equals(((SavedQuery)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "SavedQuery [id=" + id + ", query=" + (query == null ? "null" : "\"" + query + "\"") + ", userProfile=" + (userProfile == null ? "null" : (userProfile.getId() == null ? "no id" : userProfile.getId().toString())) + "]"; }
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("query".equals(fieldName)) {
            return query;
        }
        if ("userProfile".equals(fieldName)) {
            if (userProfile instanceof ProxyReference) {
                return ((ProxyReference) userProfile).getObject();
            } else {
                return userProfile;
            }
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.api.userprofile.SavedQuery.class.equals(getClass())) {
            return TypeUtil.getFieldValue(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        if ("query".equals(fieldName)) {
            return query;
        }
        if ("userProfile".equals(fieldName)) {
            return userProfile;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.api.userprofile.SavedQuery.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("query".equals(fieldName)) {
            query = (java.lang.String) value;
        } else if ("userProfile".equals(fieldName)) {
            userProfile = (org.intermine.model.InterMineObject) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else {
            if (!org.intermine.api.userprofile.SavedQuery.class.equals(getClass())) {
                DynamicUtil.setFieldValue(this, fieldName, value);
                return;
            }
            throw new IllegalArgumentException("Unknown field " + fieldName);
        }
    }
    public Class<?> getFieldType(final String fieldName) {
        if ("query".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("userProfile".equals(fieldName)) {
            return org.intermine.api.userprofile.UserProfile.class;
        }
        if ("id".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if (!org.intermine.api.userprofile.SavedQuery.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.api.userprofile.SavedQuery.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public StringConstructor getoBJECT() {
        if (!org.intermine.api.userprofile.SavedQuery.class.equals(getClass())) {
            return NotXmlRenderer.render(this);
        }
        StringConstructor sb = new StringConstructor();
        sb.append("$_^org.intermine.api.userprofile.SavedQuery");
        if (query != null) {
            sb.append("$_^aquery$_^");
            String string = query;
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
        if (userProfile != null) {
            sb.append("$_^ruserProfile$_^").append(userProfile.getId());
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
        if (!org.intermine.api.userprofile.SavedQuery.class.equals(getClass())) {
            throw new IllegalStateException("Class " + getClass().getName() + " does not match code (org.intermine.api.userprofile.SavedQuery)");
        }
        for (int i = 2; i < notXml.length;) {
            int startI = i;
            if ((i < notXml.length) && "aquery".equals(notXml[i])) {
                i++;
                StringBuilder string = null;
                while ((i + 1 < notXml.length) && (notXml[i + 1].charAt(0) == 'd')) {
                    if (string == null) string = new StringBuilder(notXml[i]);
                    i++;
                    string.append("$_^").append(notXml[i].substring(1));
                }
                query = string == null ? notXml[i] : string.toString();
                i++;
            }
            if ((i < notXml.length) &&"ruserProfile".equals(notXml[i])) {
                i++;
                userProfile = new ProxyReference(os, Integer.valueOf(notXml[i]), org.intermine.api.userprofile.UserProfile.class);
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
            if (!org.intermine.api.userprofile.SavedQuery.class.equals(getClass())) {
                TypeUtil.addCollectionElement(this, fieldName, element);
                return;
            }
            throw new IllegalArgumentException("Unknown collection " + fieldName);
        }
    }
    public Class<?> getElementType(final String fieldName) {
        if (!org.intermine.api.userprofile.SavedQuery.class.equals(getClass())) {
            return TypeUtil.getElementType(org.intermine.api.userprofile.SavedQuery.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
