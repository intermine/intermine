package org.intermine.model.testmodel;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.NotXmlParser;
import org.intermine.objectstore.intermine.NotXmlRenderer;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.model.StringConstructor;
import org.intermine.metadata.TypeUtil;
import org.intermine.util.DynamicUtil;

public class Contractor implements org.intermine.model.testmodel.Employable, org.intermine.model.testmodel.ImportantPerson
{
    // Ref: org.intermine.model.testmodel.Contractor.personalAddress
    protected org.intermine.model.InterMineObject personalAddress;
    public org.intermine.model.testmodel.Address getPersonalAddress() { if (personalAddress instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((org.intermine.model.testmodel.Address) ((org.intermine.objectstore.proxy.ProxyReference) personalAddress).getObject()); }; return (org.intermine.model.testmodel.Address) personalAddress; }
    public void setPersonalAddress(final org.intermine.model.testmodel.Address personalAddress) { this.personalAddress = personalAddress; }
    public void proxyPersonalAddress(final org.intermine.objectstore.proxy.ProxyReference personalAddress) { this.personalAddress = personalAddress; }
    public org.intermine.model.InterMineObject proxGetPersonalAddress() { return personalAddress; }

    // Ref: org.intermine.model.testmodel.Contractor.businessAddress
    protected org.intermine.model.InterMineObject businessAddress;
    public org.intermine.model.testmodel.Address getBusinessAddress() { if (businessAddress instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((org.intermine.model.testmodel.Address) ((org.intermine.objectstore.proxy.ProxyReference) businessAddress).getObject()); }; return (org.intermine.model.testmodel.Address) businessAddress; }
    public void setBusinessAddress(final org.intermine.model.testmodel.Address businessAddress) { this.businessAddress = businessAddress; }
    public void proxyBusinessAddress(final org.intermine.objectstore.proxy.ProxyReference businessAddress) { this.businessAddress = businessAddress; }
    public org.intermine.model.InterMineObject proxGetBusinessAddress() { return businessAddress; }

    // Col: org.intermine.model.testmodel.Contractor.companys
    protected java.util.Set<org.intermine.model.testmodel.Company> companys = new java.util.HashSet<org.intermine.model.testmodel.Company>();
    public java.util.Set<org.intermine.model.testmodel.Company> getCompanys() { return companys; }
    public void setCompanys(final java.util.Set<org.intermine.model.testmodel.Company> companys) { this.companys = companys; }
    public void addCompanys(final org.intermine.model.testmodel.Company arg) { companys.add(arg); }

    // Col: org.intermine.model.testmodel.Contractor.oldComs
    protected java.util.Set<org.intermine.model.testmodel.Company> oldComs = new java.util.HashSet<org.intermine.model.testmodel.Company>();
    public java.util.Set<org.intermine.model.testmodel.Company> getOldComs() { return oldComs; }
    public void setOldComs(final java.util.Set<org.intermine.model.testmodel.Company> oldComs) { this.oldComs = oldComs; }
    public void addOldComs(final org.intermine.model.testmodel.Company arg) { oldComs.add(arg); }

    // Attr: org.intermine.model.testmodel.Employable.name
    protected java.lang.String name;
    public java.lang.String getName() { return name; }
    public void setName(final java.lang.String name) { this.name = name; }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(final java.lang.Integer id) { this.id = id; }

    // Attr: org.intermine.model.testmodel.ImportantPerson.seniority
    protected java.lang.Integer seniority;
    public java.lang.Integer getSeniority() { return seniority; }
    public void setSeniority(final java.lang.Integer seniority) { this.seniority = seniority; }

    @Override public boolean equals(Object o) { return (o instanceof Contractor && id != null) ? id.equals(((Contractor)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "Contractor [businessAddress=" + (businessAddress == null ? "null" : (businessAddress.getId() == null ? "no id" : businessAddress.getId().toString())) + ", id=" + id + ", name=" + (name == null ? "null" : "\"" + name + "\"") + ", personalAddress=" + (personalAddress == null ? "null" : (personalAddress.getId() == null ? "no id" : personalAddress.getId().toString())) + ", seniority=" + seniority + "]"; }
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("personalAddress".equals(fieldName)) {
            if (personalAddress instanceof ProxyReference) {
                return ((ProxyReference) personalAddress).getObject();
            } else {
                return personalAddress;
            }
        }
        if ("businessAddress".equals(fieldName)) {
            if (businessAddress instanceof ProxyReference) {
                return ((ProxyReference) businessAddress).getObject();
            } else {
                return businessAddress;
            }
        }
        if ("companys".equals(fieldName)) {
            return companys;
        }
        if ("oldComs".equals(fieldName)) {
            return oldComs;
        }
        if ("name".equals(fieldName)) {
            return name;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if ("seniority".equals(fieldName)) {
            return seniority;
        }
        if (!org.intermine.model.testmodel.Contractor.class.equals(getClass())) {
            return TypeUtil.getFieldValue(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        if ("personalAddress".equals(fieldName)) {
            return personalAddress;
        }
        if ("businessAddress".equals(fieldName)) {
            return businessAddress;
        }
        if ("companys".equals(fieldName)) {
            return companys;
        }
        if ("oldComs".equals(fieldName)) {
            return oldComs;
        }
        if ("name".equals(fieldName)) {
            return name;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if ("seniority".equals(fieldName)) {
            return seniority;
        }
        if (!org.intermine.model.testmodel.Contractor.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("personalAddress".equals(fieldName)) {
            personalAddress = (org.intermine.model.InterMineObject) value;
        } else if ("businessAddress".equals(fieldName)) {
            businessAddress = (org.intermine.model.InterMineObject) value;
        } else if ("companys".equals(fieldName)) {
            companys = (java.util.Set) value;
        } else if ("oldComs".equals(fieldName)) {
            oldComs = (java.util.Set) value;
        } else if ("name".equals(fieldName)) {
            name = (java.lang.String) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else if ("seniority".equals(fieldName)) {
            seniority = (java.lang.Integer) value;
        } else {
            if (!org.intermine.model.testmodel.Contractor.class.equals(getClass())) {
                DynamicUtil.setFieldValue(this, fieldName, value);
                return;
            }
            throw new IllegalArgumentException("Unknown field " + fieldName);
        }
    }
    public Class<?> getFieldType(final String fieldName) {
        if ("personalAddress".equals(fieldName)) {
            return org.intermine.model.testmodel.Address.class;
        }
        if ("businessAddress".equals(fieldName)) {
            return org.intermine.model.testmodel.Address.class;
        }
        if ("companys".equals(fieldName)) {
            return java.util.Set.class;
        }
        if ("oldComs".equals(fieldName)) {
            return java.util.Set.class;
        }
        if ("name".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("id".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if ("seniority".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if (!org.intermine.model.testmodel.Contractor.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.model.testmodel.Contractor.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public StringConstructor getoBJECT() {
        if (!org.intermine.model.testmodel.Contractor.class.equals(getClass())) {
            return NotXmlRenderer.render(this);
        }
        StringConstructor sb = new StringConstructor();
        sb.append("$_^org.intermine.model.testmodel.Contractor");
        if (personalAddress != null) {
            sb.append("$_^rpersonalAddress$_^").append(personalAddress.getId());
        }
        if (businessAddress != null) {
            sb.append("$_^rbusinessAddress$_^").append(businessAddress.getId());
        }
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
        if (id != null) {
            sb.append("$_^aid$_^").append(id);
        }
        if (seniority != null) {
            sb.append("$_^aseniority$_^").append(seniority);
        }
        return sb;
    }
    public void setoBJECT(String notXml, ObjectStore os) {
        setoBJECT(NotXmlParser.SPLITTER.split(notXml), os);
    }
    public void setoBJECT(final String[] notXml, final ObjectStore os) {
        if (!org.intermine.model.testmodel.Contractor.class.equals(getClass())) {
            throw new IllegalStateException("Class " + getClass().getName() + " does not match code (org.intermine.model.testmodel.Contractor)");
        }
        for (int i = 2; i < notXml.length;) {
            int startI = i;
            if ((i < notXml.length) &&"rpersonalAddress".equals(notXml[i])) {
                i++;
                personalAddress = new ProxyReference(os, Integer.valueOf(notXml[i]), org.intermine.model.testmodel.Address.class);
                i++;
            };
            if ((i < notXml.length) &&"rbusinessAddress".equals(notXml[i])) {
                i++;
                businessAddress = new ProxyReference(os, Integer.valueOf(notXml[i]), org.intermine.model.testmodel.Address.class);
                i++;
            };
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
            if ((i < notXml.length) && "aid".equals(notXml[i])) {
                i++;
                id = Integer.valueOf(notXml[i]);
                i++;
            }
            if ((i < notXml.length) && "aseniority".equals(notXml[i])) {
                i++;
                seniority = Integer.valueOf(notXml[i]);
                i++;
            }
            if (startI == i) {
                throw new IllegalArgumentException("Unknown field " + notXml[i]);
            }
        }
        companys = new ProxyCollection<org.intermine.model.testmodel.Company>(os, this, "companys", org.intermine.model.testmodel.Company.class);
        oldComs = new ProxyCollection<org.intermine.model.testmodel.Company>(os, this, "oldComs", org.intermine.model.testmodel.Company.class);
    }
    public void addCollectionElement(final String fieldName, final org.intermine.model.InterMineObject element) {
        if ("companys".equals(fieldName)) {
            companys.add((org.intermine.model.testmodel.Company) element);
        } else if ("oldComs".equals(fieldName)) {
            oldComs.add((org.intermine.model.testmodel.Company) element);
        } else {
            if (!org.intermine.model.testmodel.Contractor.class.equals(getClass())) {
                TypeUtil.addCollectionElement(this, fieldName, element);
                return;
            }
            throw new IllegalArgumentException("Unknown collection " + fieldName);
        }
    }
    public Class<?> getElementType(final String fieldName) {
        if ("companys".equals(fieldName)) {
            return org.intermine.model.testmodel.Company.class;
        }
        if ("oldComs".equals(fieldName)) {
            return org.intermine.model.testmodel.Company.class;
        }
        if (!org.intermine.model.testmodel.Contractor.class.equals(getClass())) {
            return TypeUtil.getElementType(org.intermine.model.testmodel.Contractor.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
