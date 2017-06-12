package org.intermine.api.userprofile;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.NotXmlParser;
import org.intermine.objectstore.intermine.NotXmlRenderer;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.model.StringConstructor;
import org.intermine.metadata.TypeUtil;
import org.intermine.util.DynamicUtil;

public class TemplateSummary implements org.intermine.model.InterMineObject
{
    // Attr: org.intermine.model.userprofile.TemplateSummary.summary
    protected java.lang.String summary;
    public java.lang.String getSummary() { return summary; }
    public void setSummary(final java.lang.String summary) { this.summary = summary; }

    // Ref: org.intermine.model.userprofile.TemplateSummary.template
    protected org.intermine.model.InterMineObject template;
    public org.intermine.api.userprofile.SavedTemplateQuery getTemplate() { if (template instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((org.intermine.api.userprofile.SavedTemplateQuery) ((org.intermine.objectstore.proxy.ProxyReference) template).getObject()); }; return (org.intermine.api.userprofile.SavedTemplateQuery) template; }
    public void setTemplate(final org.intermine.api.userprofile.SavedTemplateQuery template) { this.template = template; }
    public void proxyTemplate(final org.intermine.objectstore.proxy.ProxyReference template) { this.template = template; }
    public org.intermine.model.InterMineObject proxGetTemplate() { return template; }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(final java.lang.Integer id) { this.id = id; }

    @Override public boolean equals(Object o) { return (o instanceof TemplateSummary && id != null) ? id.equals(((TemplateSummary)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "TemplateSummary [id=" + id + ", summary=" + (summary == null ? "null" : "\"" + summary + "\"") + ", template=" + (template == null ? "null" : (template.getId() == null ? "no id" : template.getId().toString())) + "]"; }
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("summary".equals(fieldName)) {
            return summary;
        }
        if ("template".equals(fieldName)) {
            if (template instanceof ProxyReference) {
                return ((ProxyReference) template).getObject();
            } else {
                return template;
            }
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.api.userprofile.TemplateSummary.class.equals(getClass())) {
            return TypeUtil.getFieldValue(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        if ("summary".equals(fieldName)) {
            return summary;
        }
        if ("template".equals(fieldName)) {
            return template;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.api.userprofile.TemplateSummary.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("summary".equals(fieldName)) {
            summary = (java.lang.String) value;
        } else if ("template".equals(fieldName)) {
            template = (org.intermine.model.InterMineObject) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else {
            if (!org.intermine.api.userprofile.TemplateSummary.class.equals(getClass())) {
                DynamicUtil.setFieldValue(this, fieldName, value);
                return;
            }
            throw new IllegalArgumentException("Unknown field " + fieldName);
        }
    }
    public Class<?> getFieldType(final String fieldName) {
        if ("summary".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("template".equals(fieldName)) {
            return org.intermine.api.userprofile.SavedTemplateQuery.class;
        }
        if ("id".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if (!org.intermine.api.userprofile.TemplateSummary.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.api.userprofile.TemplateSummary.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public StringConstructor getoBJECT() {
        if (!org.intermine.api.userprofile.TemplateSummary.class.equals(getClass())) {
            return NotXmlRenderer.render(this);
        }
        StringConstructor sb = new StringConstructor();
        sb.append("$_^org.intermine.model.userprofile.TemplateSummary");
        if (summary != null) {
            sb.append("$_^asummary$_^");
            String string = summary;
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
        if (template != null) {
            sb.append("$_^rtemplate$_^").append(template.getId());
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
        if (!org.intermine.api.userprofile.TemplateSummary.class.equals(getClass())) {
            throw new IllegalStateException("Class " + getClass().getName() + " does not match code (org.intermine.model.userprofile.TemplateSummary)");
        }
        for (int i = 2; i < notXml.length;) {
            int startI = i;
            if ((i < notXml.length) && "asummary".equals(notXml[i])) {
                i++;
                StringBuilder string = null;
                while ((i + 1 < notXml.length) && (notXml[i + 1].charAt(0) == 'd')) {
                    if (string == null) string = new StringBuilder(notXml[i]);
                    i++;
                    string.append("$_^").append(notXml[i].substring(1));
                }
                summary = string == null ? notXml[i] : string.toString();
                i++;
            }
            if ((i < notXml.length) &&"rtemplate".equals(notXml[i])) {
                i++;
                template = new ProxyReference(os, Integer.valueOf(notXml[i]), org.intermine.api.userprofile.SavedTemplateQuery.class);
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
            if (!org.intermine.api.userprofile.TemplateSummary.class.equals(getClass())) {
                TypeUtil.addCollectionElement(this, fieldName, element);
                return;
            }
            throw new IllegalArgumentException("Unknown collection " + fieldName);
        }
    }
    public Class<?> getElementType(final String fieldName) {
        if (!org.intermine.api.userprofile.TemplateSummary.class.equals(getClass())) {
            return TypeUtil.getElementType(org.intermine.api.userprofile.TemplateSummary.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
