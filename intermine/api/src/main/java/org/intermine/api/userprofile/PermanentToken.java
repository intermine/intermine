package org.intermine.api.userprofile;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.NotXmlParser;
import org.intermine.objectstore.intermine.NotXmlRenderer;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.model.StringConstructor;
import org.intermine.metadata.TypeUtil;
import org.intermine.util.DynamicUtil;

public class PermanentToken implements org.intermine.model.InterMineObject
{
    // Attr: org.intermine.api.userprofile.PermanentToken.token
    protected java.lang.String token;
    public java.lang.String getToken() { return token; }
    public void setToken(final java.lang.String token) { this.token = token; }

    // Attr: org.intermine.api.userprofile.PermanentToken.level
    protected java.lang.String level;
    public java.lang.String getLevel() { return level; }
    public void setLevel(final java.lang.String level) { this.level = level; }

    // Attr: org.intermine.api.userprofile.PermanentToken.message
    protected java.lang.String message;
    public java.lang.String getMessage() { return message; }
    public void setMessage(final java.lang.String message) { this.message = message; }

    // Attr: org.intermine.api.userprofile.PermanentToken.dateCreated
    protected java.util.Date dateCreated;
    public java.util.Date getDateCreated() { return dateCreated; }
    public void setDateCreated(final java.util.Date dateCreated) { this.dateCreated = dateCreated; }

    // Ref: org.intermine.api.userprofile.PermanentToken.userProfile
    protected org.intermine.model.InterMineObject userProfile;
    public org.intermine.api.userprofile.UserProfile getUserProfile() { if (userProfile instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((org.intermine.api.userprofile.UserProfile) ((org.intermine.objectstore.proxy.ProxyReference) userProfile).getObject()); }; return (org.intermine.api.userprofile.UserProfile) userProfile; }
    public void setUserProfile(final org.intermine.api.userprofile.UserProfile userProfile) { this.userProfile = userProfile; }
    public void proxyUserProfile(final org.intermine.objectstore.proxy.ProxyReference userProfile) { this.userProfile = userProfile; }
    public org.intermine.model.InterMineObject proxGetUserProfile() { return userProfile; }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(final java.lang.Integer id) { this.id = id; }

    @Override public boolean equals(Object o) { return (o instanceof PermanentToken && id != null) ? id.equals(((PermanentToken)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "PermanentToken [dateCreated=" + (dateCreated == null ? "null" : "\"" + dateCreated + "\"") + ", id=" + id + ", level=" + (level == null ? "null" : "\"" + level + "\"") + ", message=" + (message == null ? "null" : "\"" + message + "\"") + ", token=" + (token == null ? "null" : "\"" + token + "\"") + ", userProfile=" + (userProfile == null ? "null" : (userProfile.getId() == null ? "no id" : userProfile.getId().toString())) + "]"; }
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("token".equals(fieldName)) {
            return token;
        }
        if ("level".equals(fieldName)) {
            return level;
        }
        if ("message".equals(fieldName)) {
            return message;
        }
        if ("dateCreated".equals(fieldName)) {
            return dateCreated;
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
        if (!org.intermine.api.userprofile.PermanentToken.class.equals(getClass())) {
            return TypeUtil.getFieldValue(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        if ("token".equals(fieldName)) {
            return token;
        }
        if ("level".equals(fieldName)) {
            return level;
        }
        if ("message".equals(fieldName)) {
            return message;
        }
        if ("dateCreated".equals(fieldName)) {
            return dateCreated;
        }
        if ("userProfile".equals(fieldName)) {
            return userProfile;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.api.userprofile.PermanentToken.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("token".equals(fieldName)) {
            token = (java.lang.String) value;
        } else if ("level".equals(fieldName)) {
            level = (java.lang.String) value;
        } else if ("message".equals(fieldName)) {
            message = (java.lang.String) value;
        } else if ("dateCreated".equals(fieldName)) {
            dateCreated = (java.util.Date) value;
        } else if ("userProfile".equals(fieldName)) {
            userProfile = (org.intermine.model.InterMineObject) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else {
            if (!org.intermine.api.userprofile.PermanentToken.class.equals(getClass())) {
                DynamicUtil.setFieldValue(this, fieldName, value);
                return;
            }
            throw new IllegalArgumentException("Unknown field " + fieldName);
        }
    }
    public Class<?> getFieldType(final String fieldName) {
        if ("token".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("level".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("message".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("dateCreated".equals(fieldName)) {
            return java.util.Date.class;
        }
        if ("userProfile".equals(fieldName)) {
            return org.intermine.api.userprofile.UserProfile.class;
        }
        if ("id".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if (!org.intermine.api.userprofile.PermanentToken.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.api.userprofile.PermanentToken.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public StringConstructor getoBJECT() {
        if (!org.intermine.api.userprofile.PermanentToken.class.equals(getClass())) {
            return NotXmlRenderer.render(this);
        }
        StringConstructor sb = new StringConstructor();
        sb.append("$_^org.intermine.api.userprofile.PermanentToken");
        if (token != null) {
            sb.append("$_^atoken$_^");
            String string = token;
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
        if (level != null) {
            sb.append("$_^alevel$_^");
            String string = level;
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
        if (message != null) {
            sb.append("$_^amessage$_^");
            String string = message;
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
        if (dateCreated != null) {
            sb.append("$_^adateCreated$_^").append(dateCreated.getTime());
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
        if (!org.intermine.api.userprofile.PermanentToken.class.equals(getClass())) {
            throw new IllegalStateException("Class " + getClass().getName() + " does not match code (org.intermine.api.userprofile.PermanentToken)");
        }
        for (int i = 2; i < notXml.length;) {
            int startI = i;
            if ((i < notXml.length) && "atoken".equals(notXml[i])) {
                i++;
                StringBuilder string = null;
                while ((i + 1 < notXml.length) && (notXml[i + 1].charAt(0) == 'd')) {
                    if (string == null) string = new StringBuilder(notXml[i]);
                    i++;
                    string.append("$_^").append(notXml[i].substring(1));
                }
                token = string == null ? notXml[i] : string.toString();
                i++;
            }
            if ((i < notXml.length) && "alevel".equals(notXml[i])) {
                i++;
                StringBuilder string = null;
                while ((i + 1 < notXml.length) && (notXml[i + 1].charAt(0) == 'd')) {
                    if (string == null) string = new StringBuilder(notXml[i]);
                    i++;
                    string.append("$_^").append(notXml[i].substring(1));
                }
                level = string == null ? notXml[i] : string.toString();
                i++;
            }
            if ((i < notXml.length) && "amessage".equals(notXml[i])) {
                i++;
                StringBuilder string = null;
                while ((i + 1 < notXml.length) && (notXml[i + 1].charAt(0) == 'd')) {
                    if (string == null) string = new StringBuilder(notXml[i]);
                    i++;
                    string.append("$_^").append(notXml[i].substring(1));
                }
                message = string == null ? notXml[i] : string.toString();
                i++;
            }
            if ((i < notXml.length) && "adateCreated".equals(notXml[i])) {
                i++;
                dateCreated = new java.util.Date(Long.parseLong(notXml[i]));
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
            if (!org.intermine.api.userprofile.PermanentToken.class.equals(getClass())) {
                TypeUtil.addCollectionElement(this, fieldName, element);
                return;
            }
            throw new IllegalArgumentException("Unknown collection " + fieldName);
        }
    }
    public Class<?> getElementType(final String fieldName) {
        if (!org.intermine.api.userprofile.PermanentToken.class.equals(getClass())) {
            return TypeUtil.getElementType(org.intermine.api.userprofile.PermanentToken.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
