package org.intermine.api.userprofile;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.NotXmlParser;
import org.intermine.objectstore.intermine.NotXmlRenderer;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.model.StringConstructor;
import org.intermine.metadata.TypeUtil;
import org.intermine.util.DynamicUtil;

public class SavedTemplateQuery implements org.intermine.model.InterMineObject
{
    // Attr: org.intermine.api.userprofile.SavedTemplateQuery.templateQuery
    protected java.lang.String templateQuery;
    public java.lang.String getTemplateQuery() { return templateQuery; }
    public void setTemplateQuery(final java.lang.String templateQuery) { this.templateQuery = templateQuery; }

    // Ref: org.intermine.api.userprofile.SavedTemplateQuery.userProfile
    protected org.intermine.model.InterMineObject userProfile;
    public org.intermine.api.userprofile.UserProfile getUserProfile() { if (userProfile instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((org.intermine.api.userprofile.UserProfile) ((org.intermine.objectstore.proxy.ProxyReference) userProfile).getObject()); }; return (org.intermine.api.userprofile.UserProfile) userProfile; }
    public void setUserProfile(final org.intermine.api.userprofile.UserProfile userProfile) { this.userProfile = userProfile; }
    public void proxyUserProfile(final org.intermine.objectstore.proxy.ProxyReference userProfile) { this.userProfile = userProfile; }
    public org.intermine.model.InterMineObject proxGetUserProfile() { return userProfile; }

    // Col: org.intermine.api.userprofile.SavedTemplateQuery.summaries
    protected java.util.Set<org.intermine.api.userprofile.TemplateSummary> summaries = new java.util.HashSet<org.intermine.api.userprofile.TemplateSummary>();
    public java.util.Set<org.intermine.api.userprofile.TemplateSummary> getSummaries() { return summaries; }
    public void setSummaries(final java.util.Set<org.intermine.api.userprofile.TemplateSummary> summaries) { this.summaries = summaries; }
    public void addSummaries(final org.intermine.api.userprofile.TemplateSummary arg) { summaries.add(arg); }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(final java.lang.Integer id) { this.id = id; }

    @Override public boolean equals(Object o) { return (o instanceof SavedTemplateQuery && id != null) ? id.equals(((SavedTemplateQuery)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "SavedTemplateQuery [id=" + id + ", templateQuery=" + (templateQuery == null ? "null" : "\"" + templateQuery + "\"") + ", userProfile=" + (userProfile == null ? "null" : (userProfile.getId() == null ? "no id" : userProfile.getId().toString())) + "]"; }
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("templateQuery".equals(fieldName)) {
            return templateQuery;
        }
        if ("userProfile".equals(fieldName)) {
            if (userProfile instanceof ProxyReference) {
                return ((ProxyReference) userProfile).getObject();
            } else {
                return userProfile;
            }
        }
        if ("summaries".equals(fieldName)) {
            return summaries;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.api.userprofile.SavedTemplateQuery.class.equals(getClass())) {
            return TypeUtil.getFieldValue(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        if ("templateQuery".equals(fieldName)) {
            return templateQuery;
        }
        if ("userProfile".equals(fieldName)) {
            return userProfile;
        }
        if ("summaries".equals(fieldName)) {
            return summaries;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.api.userprofile.SavedTemplateQuery.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("templateQuery".equals(fieldName)) {
            templateQuery = (java.lang.String) value;
        } else if ("userProfile".equals(fieldName)) {
            userProfile = (org.intermine.model.InterMineObject) value;
        } else if ("summaries".equals(fieldName)) {
            summaries = (java.util.Set) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else {
            if (!org.intermine.api.userprofile.SavedTemplateQuery.class.equals(getClass())) {
                DynamicUtil.setFieldValue(this, fieldName, value);
                return;
            }
            throw new IllegalArgumentException("Unknown field " + fieldName);
        }
    }
    public Class<?> getFieldType(final String fieldName) {
        if ("templateQuery".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("userProfile".equals(fieldName)) {
            return org.intermine.api.userprofile.UserProfile.class;
        }
        if ("summaries".equals(fieldName)) {
            return java.util.Set.class;
        }
        if ("id".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if (!org.intermine.api.userprofile.SavedTemplateQuery.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.api.userprofile.SavedTemplateQuery.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public StringConstructor getoBJECT() {
        if (!org.intermine.api.userprofile.SavedTemplateQuery.class.equals(getClass())) {
            return NotXmlRenderer.render(this);
        }
        StringConstructor sb = new StringConstructor();
        sb.append("$_^org.intermine.api.userprofile.SavedTemplateQuery");
        if (templateQuery != null) {
            sb.append("$_^atemplateQuery$_^");
            String string = templateQuery;
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
        if (!org.intermine.api.userprofile.SavedTemplateQuery.class.equals(getClass())) {
            throw new IllegalStateException("Class " + getClass().getName() + " does not match code (org.intermine.api.userprofile.SavedTemplateQuery)");
        }
        for (int i = 2; i < notXml.length;) {
            int startI = i;
            if ((i < notXml.length) && "atemplateQuery".equals(notXml[i])) {
                i++;
                StringBuilder string = null;
                while ((i + 1 < notXml.length) && (notXml[i + 1].charAt(0) == 'd')) {
                    if (string == null) string = new StringBuilder(notXml[i]);
                    i++;
                    string.append("$_^").append(notXml[i].substring(1));
                }
                templateQuery = string == null ? notXml[i] : string.toString();
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
        summaries = new ProxyCollection<org.intermine.api.userprofile.TemplateSummary>(os, this, "summaries", org.intermine.api.userprofile.TemplateSummary.class);
    }
    public void addCollectionElement(final String fieldName, final org.intermine.model.InterMineObject element) {
        if ("summaries".equals(fieldName)) {
            summaries.add((org.intermine.api.userprofile.TemplateSummary) element);
        } else {
            if (!org.intermine.api.userprofile.SavedTemplateQuery.class.equals(getClass())) {
                TypeUtil.addCollectionElement(this, fieldName, element);
                return;
            }
            throw new IllegalArgumentException("Unknown collection " + fieldName);
        }
    }
    public Class<?> getElementType(final String fieldName) {
        if ("summaries".equals(fieldName)) {
            return org.intermine.api.userprofile.TemplateSummary.class;
        }
        if (!org.intermine.api.userprofile.SavedTemplateQuery.class.equals(getClass())) {
            return TypeUtil.getElementType(org.intermine.api.userprofile.SavedTemplateQuery.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
