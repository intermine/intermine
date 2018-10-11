package org.intermine.api.userprofile;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.NotXmlParser;
import org.intermine.objectstore.intermine.NotXmlRenderer;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.model.StringConstructor;
import org.intermine.metadata.TypeUtil;
import org.intermine.util.DynamicUtil;

public class UserProfile implements org.intermine.model.InterMineObject
{
    // Attr: org.intermine.api.userprofile.UserProfile.username
    protected java.lang.String username;
    public java.lang.String getUsername() { return username; }
    public void setUsername(final java.lang.String username) { this.username = username; }

    // Attr: org.intermine.api.userprofile.UserProfile.password
    protected java.lang.String password;
    public java.lang.String getPassword() { return password; }
    public void setPassword(final java.lang.String password) { this.password = password; }

    // Attr: org.intermine.api.userprofile.UserProfile.apiKey
    protected java.lang.String apiKey;
    public java.lang.String getApiKey() { return apiKey; }
    public void setApiKey(final java.lang.String apiKey) { this.apiKey = apiKey; }

    // Attr: org.intermine.api.userprofile.UserProfile.localAccount
    protected boolean localAccount;
    public boolean getLocalAccount() { return localAccount; }
    public void setLocalAccount(final boolean localAccount) { this.localAccount = localAccount; }

    // Attr: org.intermine.api.userprofile.UserProfile.superuser
    protected boolean superuser;
    public boolean getSuperuser() { return superuser; }
    public void setSuperuser(final boolean superuser) { this.superuser = superuser; }

    // Col: org.intermine.api.userprofile.UserProfile.savedBags
    protected java.util.Set<org.intermine.api.userprofile.SavedBag> savedBags = new java.util.HashSet<org.intermine.api.userprofile.SavedBag>();
    public java.util.Set<org.intermine.api.userprofile.SavedBag> getSavedBags() { return savedBags; }
    public void setSavedBags(final java.util.Set<org.intermine.api.userprofile.SavedBag> savedBags) { this.savedBags = savedBags; }
    public void addSavedBags(final org.intermine.api.userprofile.SavedBag arg) { savedBags.add(arg); }

    // Col: org.intermine.api.userprofile.UserProfile.savedQuerys
    protected java.util.Set<org.intermine.api.userprofile.SavedQuery> savedQuerys = new java.util.HashSet<org.intermine.api.userprofile.SavedQuery>();
    public java.util.Set<org.intermine.api.userprofile.SavedQuery> getSavedQuerys() { return savedQuerys; }
    public void setSavedQuerys(final java.util.Set<org.intermine.api.userprofile.SavedQuery> savedQuerys) { this.savedQuerys = savedQuerys; }
    public void addSavedQuerys(final org.intermine.api.userprofile.SavedQuery arg) { savedQuerys.add(arg); }

    // Col: org.intermine.api.userprofile.UserProfile.savedTemplateQuerys
    protected java.util.Set<org.intermine.api.userprofile.SavedTemplateQuery> savedTemplateQuerys = new java.util.HashSet<org.intermine.api.userprofile.SavedTemplateQuery>();
    public java.util.Set<org.intermine.api.userprofile.SavedTemplateQuery> getSavedTemplateQuerys() { return savedTemplateQuerys; }
    public void setSavedTemplateQuerys(final java.util.Set<org.intermine.api.userprofile.SavedTemplateQuery> savedTemplateQuerys) { this.savedTemplateQuerys = savedTemplateQuerys; }
    public void addSavedTemplateQuerys(final org.intermine.api.userprofile.SavedTemplateQuery arg) { savedTemplateQuerys.add(arg); }

    // Col: org.intermine.api.userprofile.UserProfile.tags
    protected java.util.Set<org.intermine.api.userprofile.Tag> tags = new java.util.HashSet<org.intermine.api.userprofile.Tag>();
    public java.util.Set<org.intermine.api.userprofile.Tag> getTags() { return tags; }
    public void setTags(final java.util.Set<org.intermine.api.userprofile.Tag> tags) { this.tags = tags; }
    public void addTags(final org.intermine.api.userprofile.Tag arg) { tags.add(arg); }

    // Col: org.intermine.api.userprofile.UserProfile.permanentTokens
    protected java.util.Set<org.intermine.api.userprofile.PermanentToken> permanentTokens = new java.util.HashSet<org.intermine.api.userprofile.PermanentToken>();
    public java.util.Set<org.intermine.api.userprofile.PermanentToken> getPermanentTokens() { return permanentTokens; }
    public void setPermanentTokens(final java.util.Set<org.intermine.api.userprofile.PermanentToken> permanentTokens) { this.permanentTokens = permanentTokens; }
    public void addPermanentTokens(final org.intermine.api.userprofile.PermanentToken arg) { permanentTokens.add(arg); }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(final java.lang.Integer id) { this.id = id; }

    @Override public boolean equals(Object o) { return (o instanceof UserProfile && id != null) ? id.equals(((UserProfile)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "UserProfile [apiKey=" + (apiKey == null ? "null" : "\"" + apiKey + "\"") + ", id=" + id + ", localAccount=" + localAccount + ", password=" + (password == null ? "null" : "\"" + password + "\"") + ", superuser=" + superuser + ", username=" + (username == null ? "null" : "\"" + username + "\"") + "]"; }
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("username".equals(fieldName)) {
            return username;
        }
        if ("password".equals(fieldName)) {
            return password;
        }
        if ("apiKey".equals(fieldName)) {
            return apiKey;
        }
        if ("localAccount".equals(fieldName)) {
            return Boolean.valueOf(localAccount);
        }
        if ("superuser".equals(fieldName)) {
            return Boolean.valueOf(superuser);
        }
        if ("savedBags".equals(fieldName)) {
            return savedBags;
        }
        if ("savedQuerys".equals(fieldName)) {
            return savedQuerys;
        }
        if ("savedTemplateQuerys".equals(fieldName)) {
            return savedTemplateQuerys;
        }
        if ("tags".equals(fieldName)) {
            return tags;
        }
        if ("permanentTokens".equals(fieldName)) {
            return permanentTokens;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.api.userprofile.UserProfile.class.equals(getClass())) {
            return TypeUtil.getFieldValue(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        if ("username".equals(fieldName)) {
            return username;
        }
        if ("password".equals(fieldName)) {
            return password;
        }
        if ("apiKey".equals(fieldName)) {
            return apiKey;
        }
        if ("localAccount".equals(fieldName)) {
            return Boolean.valueOf(localAccount);
        }
        if ("superuser".equals(fieldName)) {
            return Boolean.valueOf(superuser);
        }
        if ("savedBags".equals(fieldName)) {
            return savedBags;
        }
        if ("savedQuerys".equals(fieldName)) {
            return savedQuerys;
        }
        if ("savedTemplateQuerys".equals(fieldName)) {
            return savedTemplateQuerys;
        }
        if ("tags".equals(fieldName)) {
            return tags;
        }
        if ("permanentTokens".equals(fieldName)) {
            return permanentTokens;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.api.userprofile.UserProfile.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("username".equals(fieldName)) {
            username = (java.lang.String) value;
        } else if ("password".equals(fieldName)) {
            password = (java.lang.String) value;
        } else if ("apiKey".equals(fieldName)) {
            apiKey = (java.lang.String) value;
        } else if ("localAccount".equals(fieldName)) {
            localAccount = ((Boolean) value).booleanValue();
        } else if ("superuser".equals(fieldName)) {
            superuser = ((Boolean) value).booleanValue();
        } else if ("savedBags".equals(fieldName)) {
            savedBags = (java.util.Set) value;
        } else if ("savedQuerys".equals(fieldName)) {
            savedQuerys = (java.util.Set) value;
        } else if ("savedTemplateQuerys".equals(fieldName)) {
            savedTemplateQuerys = (java.util.Set) value;
        } else if ("tags".equals(fieldName)) {
            tags = (java.util.Set) value;
        } else if ("permanentTokens".equals(fieldName)) {
            permanentTokens = (java.util.Set) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else {
            if (!org.intermine.api.userprofile.UserProfile.class.equals(getClass())) {
                DynamicUtil.setFieldValue(this, fieldName, value);
                return;
            }
            throw new IllegalArgumentException("Unknown field " + fieldName);
        }
    }
    public Class<?> getFieldType(final String fieldName) {
        if ("username".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("password".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("apiKey".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("localAccount".equals(fieldName)) {
            return Boolean.TYPE;
        }
        if ("superuser".equals(fieldName)) {
            return Boolean.TYPE;
        }
        if ("savedBags".equals(fieldName)) {
            return java.util.Set.class;
        }
        if ("savedQuerys".equals(fieldName)) {
            return java.util.Set.class;
        }
        if ("savedTemplateQuerys".equals(fieldName)) {
            return java.util.Set.class;
        }
        if ("tags".equals(fieldName)) {
            return java.util.Set.class;
        }
        if ("permanentTokens".equals(fieldName)) {
            return java.util.Set.class;
        }
        if ("id".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if (!org.intermine.api.userprofile.UserProfile.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.api.userprofile.UserProfile.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public StringConstructor getoBJECT() {
        if (!org.intermine.api.userprofile.UserProfile.class.equals(getClass())) {
            return NotXmlRenderer.render(this);
        }
        StringConstructor sb = new StringConstructor();
        sb.append("$_^org.intermine.api.userprofile.UserProfile");
        if (username != null) {
            sb.append("$_^ausername$_^");
            String string = username;
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
        if (password != null) {
            sb.append("$_^apassword$_^");
            String string = password;
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
        if (apiKey != null) {
            sb.append("$_^aapiKey$_^");
            String string = apiKey;
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
        sb.append("$_^alocalAccount$_^").append(localAccount);
        sb.append("$_^asuperuser$_^").append(superuser);
        if (id != null) {
            sb.append("$_^aid$_^").append(id);
        }
        return sb;
    }
    public void setoBJECT(String notXml, ObjectStore os) {
        setoBJECT(NotXmlParser.SPLITTER.split(notXml), os);
    }
    public void setoBJECT(final String[] notXml, final ObjectStore os) {
        if (!org.intermine.api.userprofile.UserProfile.class.equals(getClass())) {
            throw new IllegalStateException("Class " + getClass().getName() + " does not match code (org.intermine.api.userprofile.UserProfile)");
        }
        for (int i = 2; i < notXml.length;) {
            int startI = i;
            if ((i < notXml.length) && "ausername".equals(notXml[i])) {
                i++;
                StringBuilder string = null;
                while ((i + 1 < notXml.length) && (notXml[i + 1].charAt(0) == 'd')) {
                    if (string == null) string = new StringBuilder(notXml[i]);
                    i++;
                    string.append("$_^").append(notXml[i].substring(1));
                }
                username = string == null ? notXml[i] : string.toString();
                i++;
            }
            if ((i < notXml.length) && "apassword".equals(notXml[i])) {
                i++;
                StringBuilder string = null;
                while ((i + 1 < notXml.length) && (notXml[i + 1].charAt(0) == 'd')) {
                    if (string == null) string = new StringBuilder(notXml[i]);
                    i++;
                    string.append("$_^").append(notXml[i].substring(1));
                }
                password = string == null ? notXml[i] : string.toString();
                i++;
            }
            if ((i < notXml.length) && "aapiKey".equals(notXml[i])) {
                i++;
                StringBuilder string = null;
                while ((i + 1 < notXml.length) && (notXml[i + 1].charAt(0) == 'd')) {
                    if (string == null) string = new StringBuilder(notXml[i]);
                    i++;
                    string.append("$_^").append(notXml[i].substring(1));
                }
                apiKey = string == null ? notXml[i] : string.toString();
                i++;
            }
            if ((i < notXml.length) && "alocalAccount".equals(notXml[i])) {
                i++;
                localAccount = Boolean.parseBoolean(notXml[i]);
                i++;
            }
            if ((i < notXml.length) && "asuperuser".equals(notXml[i])) {
                i++;
                superuser = Boolean.parseBoolean(notXml[i]);
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
        savedBags = new ProxyCollection<org.intermine.api.userprofile.SavedBag>(os, this, "savedBags", org.intermine.api.userprofile.SavedBag.class);
        savedQuerys = new ProxyCollection<org.intermine.api.userprofile.SavedQuery>(os, this, "savedQuerys", org.intermine.api.userprofile.SavedQuery.class);
        savedTemplateQuerys = new ProxyCollection<org.intermine.api.userprofile.SavedTemplateQuery>(os, this, "savedTemplateQuerys", org.intermine.api.userprofile.SavedTemplateQuery.class);
        tags = new ProxyCollection<org.intermine.api.userprofile.Tag>(os, this, "tags", org.intermine.api.userprofile.Tag.class);
        permanentTokens = new ProxyCollection<org.intermine.api.userprofile.PermanentToken>(os, this, "permanentTokens", org.intermine.api.userprofile.PermanentToken.class);
    }
    public void addCollectionElement(final String fieldName, final org.intermine.model.InterMineObject element) {
        if ("savedBags".equals(fieldName)) {
            savedBags.add((org.intermine.api.userprofile.SavedBag) element);
        } else if ("savedQuerys".equals(fieldName)) {
            savedQuerys.add((org.intermine.api.userprofile.SavedQuery) element);
        } else if ("savedTemplateQuerys".equals(fieldName)) {
            savedTemplateQuerys.add((org.intermine.api.userprofile.SavedTemplateQuery) element);
        } else if ("tags".equals(fieldName)) {
            tags.add((org.intermine.api.userprofile.Tag) element);
        } else if ("permanentTokens".equals(fieldName)) {
            permanentTokens.add((org.intermine.api.userprofile.PermanentToken) element);
        } else {
            if (!org.intermine.api.userprofile.UserProfile.class.equals(getClass())) {
                TypeUtil.addCollectionElement(this, fieldName, element);
                return;
            }
            throw new IllegalArgumentException("Unknown collection " + fieldName);
        }
    }
    public Class<?> getElementType(final String fieldName) {
        if ("savedBags".equals(fieldName)) {
            return org.intermine.api.userprofile.SavedBag.class;
        }
        if ("savedQuerys".equals(fieldName)) {
            return org.intermine.api.userprofile.SavedQuery.class;
        }
        if ("savedTemplateQuerys".equals(fieldName)) {
            return org.intermine.api.userprofile.SavedTemplateQuery.class;
        }
        if ("tags".equals(fieldName)) {
            return org.intermine.api.userprofile.Tag.class;
        }
        if ("permanentTokens".equals(fieldName)) {
            return org.intermine.api.userprofile.PermanentToken.class;
        }
        if (!org.intermine.api.userprofile.UserProfile.class.equals(getClass())) {
            return TypeUtil.getElementType(org.intermine.api.userprofile.UserProfile.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
