package org.intermine.model.testmodel;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.NotXmlParser;
import org.intermine.objectstore.intermine.NotXmlRenderer;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.model.StringConstructor;
import org.intermine.metadata.TypeUtil;
import org.intermine.util.DynamicUtil;
import org.intermine.model.ShadowClass;

public class CompanyShadow implements Company, ShadowClass
{
    public static final Class<Company> shadowOf = Company.class;
    // Attr: org.intermine.model.testmodel.Company.name
    protected java.lang.String name;
    public java.lang.String getName() { return name; }
    public void setName(final java.lang.String name) { this.name = name; }

    // Attr: org.intermine.model.testmodel.Company.vatNumber
    protected int vatNumber;
    public int getVatNumber() { return vatNumber; }
    public void setVatNumber(final int vatNumber) { this.vatNumber = vatNumber; }

    // Ref: org.intermine.model.testmodel.Company.CEO
    protected org.intermine.model.InterMineObject CEO;
    public org.intermine.model.testmodel.CEO getcEO() { if (CEO instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((org.intermine.model.testmodel.CEO) ((org.intermine.objectstore.proxy.ProxyReference) CEO).getObject()); }; return (org.intermine.model.testmodel.CEO) CEO; }
    public void setcEO(final org.intermine.model.testmodel.CEO CEO) { this.CEO = CEO; }
    public void proxycEO(final org.intermine.objectstore.proxy.ProxyReference CEO) { this.CEO = CEO; }
    public org.intermine.model.InterMineObject proxGetcEO() { return CEO; }

    // Ref: org.intermine.model.testmodel.Company.bank
    protected org.intermine.model.InterMineObject bank;
    public org.intermine.model.testmodel.Bank getBank() { if (bank instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((org.intermine.model.testmodel.Bank) ((org.intermine.objectstore.proxy.ProxyReference) bank).getObject()); }; return (org.intermine.model.testmodel.Bank) bank; }
    public void setBank(final org.intermine.model.testmodel.Bank bank) { this.bank = bank; }
    public void proxyBank(final org.intermine.objectstore.proxy.ProxyReference bank) { this.bank = bank; }
    public org.intermine.model.InterMineObject proxGetBank() { return bank; }

    // Col: org.intermine.model.testmodel.Company.departments
    protected java.util.Set<org.intermine.model.testmodel.Department> departments = new java.util.HashSet<org.intermine.model.testmodel.Department>();
    public java.util.Set<org.intermine.model.testmodel.Department> getDepartments() { return departments; }
    public void setDepartments(final java.util.Set<org.intermine.model.testmodel.Department> departments) { this.departments = departments; }
    public void addDepartments(final org.intermine.model.testmodel.Department arg) { departments.add(arg); }

    // Col: org.intermine.model.testmodel.Company.contractors
    protected java.util.Set<org.intermine.model.testmodel.Contractor> contractors = new java.util.HashSet<org.intermine.model.testmodel.Contractor>();
    public java.util.Set<org.intermine.model.testmodel.Contractor> getContractors() { return contractors; }
    public void setContractors(final java.util.Set<org.intermine.model.testmodel.Contractor> contractors) { this.contractors = contractors; }
    public void addContractors(final org.intermine.model.testmodel.Contractor arg) { contractors.add(arg); }

    // Col: org.intermine.model.testmodel.Company.oldContracts
    protected java.util.Set<org.intermine.model.testmodel.Contractor> oldContracts = new java.util.HashSet<org.intermine.model.testmodel.Contractor>();
    public java.util.Set<org.intermine.model.testmodel.Contractor> getOldContracts() { return oldContracts; }
    public void setOldContracts(final java.util.Set<org.intermine.model.testmodel.Contractor> oldContracts) { this.oldContracts = oldContracts; }
    public void addOldContracts(final org.intermine.model.testmodel.Contractor arg) { oldContracts.add(arg); }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(final java.lang.Integer id) { this.id = id; }

    // Ref: org.intermine.model.testmodel.HasAddress.address
    protected org.intermine.model.InterMineObject address;
    public org.intermine.model.testmodel.Address getAddress() { if (address instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((org.intermine.model.testmodel.Address) ((org.intermine.objectstore.proxy.ProxyReference) address).getObject()); }; return (org.intermine.model.testmodel.Address) address; }
    public void setAddress(final org.intermine.model.testmodel.Address address) { this.address = address; }
    public void proxyAddress(final org.intermine.objectstore.proxy.ProxyReference address) { this.address = address; }
    public org.intermine.model.InterMineObject proxGetAddress() { return address; }

    // Col: org.intermine.model.testmodel.HasSecretarys.secretarys
    protected java.util.Set<org.intermine.model.testmodel.Secretary> secretarys = new java.util.HashSet<org.intermine.model.testmodel.Secretary>();
    public java.util.Set<org.intermine.model.testmodel.Secretary> getSecretarys() { return secretarys; }
    public void setSecretarys(final java.util.Set<org.intermine.model.testmodel.Secretary> secretarys) { this.secretarys = secretarys; }
    public void addSecretarys(final org.intermine.model.testmodel.Secretary arg) { secretarys.add(arg); }

    @Override public boolean equals(Object o) { return (o instanceof Company && id != null) ? id.equals(((Company)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "Company [CEO=" + (CEO == null ? "null" : (CEO.getId() == null ? "no id" : CEO.getId().toString())) + ", address=" + (address == null ? "null" : (address.getId() == null ? "no id" : address.getId().toString())) + ", bank=" + (bank == null ? "null" : (bank.getId() == null ? "no id" : bank.getId().toString())) + ", id=" + id + ", name=" + (name == null ? "null" : "\"" + name + "\"") + ", vatNumber=" + vatNumber + "]"; }
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("name".equals(fieldName)) {
            return name;
        }
        if ("vatNumber".equals(fieldName)) {
            return Integer.valueOf(vatNumber);
        }
        if ("CEO".equals(fieldName)) {
            if (CEO instanceof ProxyReference) {
                return ((ProxyReference) CEO).getObject();
            } else {
                return CEO;
            }
        }
        if ("bank".equals(fieldName)) {
            if (bank instanceof ProxyReference) {
                return ((ProxyReference) bank).getObject();
            } else {
                return bank;
            }
        }
        if ("departments".equals(fieldName)) {
            return departments;
        }
        if ("contractors".equals(fieldName)) {
            return contractors;
        }
        if ("oldContracts".equals(fieldName)) {
            return oldContracts;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if ("address".equals(fieldName)) {
            if (address instanceof ProxyReference) {
                return ((ProxyReference) address).getObject();
            } else {
                return address;
            }
        }
        if ("secretarys".equals(fieldName)) {
            return secretarys;
        }
        if (!org.intermine.model.testmodel.Company.class.equals(getClass())) {
            return TypeUtil.getFieldValue(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        if ("name".equals(fieldName)) {
            return name;
        }
        if ("vatNumber".equals(fieldName)) {
            return Integer.valueOf(vatNumber);
        }
        if ("CEO".equals(fieldName)) {
            return CEO;
        }
        if ("bank".equals(fieldName)) {
            return bank;
        }
        if ("departments".equals(fieldName)) {
            return departments;
        }
        if ("contractors".equals(fieldName)) {
            return contractors;
        }
        if ("oldContracts".equals(fieldName)) {
            return oldContracts;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if ("address".equals(fieldName)) {
            return address;
        }
        if ("secretarys".equals(fieldName)) {
            return secretarys;
        }
        if (!org.intermine.model.testmodel.Company.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("name".equals(fieldName)) {
            name = (java.lang.String) value;
        } else if ("vatNumber".equals(fieldName)) {
            vatNumber = ((Integer) value).intValue();
        } else if ("CEO".equals(fieldName)) {
            CEO = (org.intermine.model.InterMineObject) value;
        } else if ("bank".equals(fieldName)) {
            bank = (org.intermine.model.InterMineObject) value;
        } else if ("departments".equals(fieldName)) {
            departments = (java.util.Set) value;
        } else if ("contractors".equals(fieldName)) {
            contractors = (java.util.Set) value;
        } else if ("oldContracts".equals(fieldName)) {
            oldContracts = (java.util.Set) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else if ("address".equals(fieldName)) {
            address = (org.intermine.model.InterMineObject) value;
        } else if ("secretarys".equals(fieldName)) {
            secretarys = (java.util.Set) value;
        } else {
            if (!org.intermine.model.testmodel.Company.class.equals(getClass())) {
                DynamicUtil.setFieldValue(this, fieldName, value);
                return;
            }
            throw new IllegalArgumentException("Unknown field " + fieldName);
        }
    }
    public Class<?> getFieldType(final String fieldName) {
        if ("name".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("vatNumber".equals(fieldName)) {
            return Integer.TYPE;
        }
        if ("CEO".equals(fieldName)) {
            return org.intermine.model.testmodel.CEO.class;
        }
        if ("bank".equals(fieldName)) {
            return org.intermine.model.testmodel.Bank.class;
        }
        if ("departments".equals(fieldName)) {
            return java.util.Set.class;
        }
        if ("contractors".equals(fieldName)) {
            return java.util.Set.class;
        }
        if ("oldContracts".equals(fieldName)) {
            return java.util.Set.class;
        }
        if ("id".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if ("address".equals(fieldName)) {
            return org.intermine.model.testmodel.Address.class;
        }
        if ("secretarys".equals(fieldName)) {
            return java.util.Set.class;
        }
        if (!org.intermine.model.testmodel.Company.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.model.testmodel.Company.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public StringConstructor getoBJECT() {
        if (!org.intermine.model.testmodel.CompanyShadow.class.equals(getClass())) {
            return NotXmlRenderer.render(this);
        }
        StringConstructor sb = new StringConstructor();
        sb.append("$_^org.intermine.model.testmodel.Company");
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
        sb.append("$_^avatNumber$_^").append(vatNumber);
        if (CEO != null) {
            sb.append("$_^rCEO$_^").append(CEO.getId());
        }
        if (bank != null) {
            sb.append("$_^rbank$_^").append(bank.getId());
        }
        if (id != null) {
            sb.append("$_^aid$_^").append(id);
        }
        if (address != null) {
            sb.append("$_^raddress$_^").append(address.getId());
        }
        return sb;
    }
    public void setoBJECT(String notXml, ObjectStore os) {
        setoBJECT(NotXmlParser.SPLITTER.split(notXml), os);
    }
    public void setoBJECT(final String[] notXml, final ObjectStore os) {
        if (!org.intermine.model.testmodel.CompanyShadow.class.equals(getClass())) {
            throw new IllegalStateException("Class " + getClass().getName() + " does not match code (org.intermine.model.testmodel.Company)");
        }
        for (int i = 2; i < notXml.length;) {
            int startI = i;
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
            if ((i < notXml.length) && "avatNumber".equals(notXml[i])) {
                i++;
                vatNumber = Integer.parseInt(notXml[i]);
                i++;
            }
            if ((i < notXml.length) &&"rCEO".equals(notXml[i])) {
                i++;
                CEO = new ProxyReference(os, Integer.valueOf(notXml[i]), org.intermine.model.testmodel.CEO.class);
                i++;
            };
            if ((i < notXml.length) &&"rbank".equals(notXml[i])) {
                i++;
                bank = new ProxyReference(os, Integer.valueOf(notXml[i]), org.intermine.model.testmodel.Bank.class);
                i++;
            };
            if ((i < notXml.length) && "aid".equals(notXml[i])) {
                i++;
                id = Integer.valueOf(notXml[i]);
                i++;
            }
            if ((i < notXml.length) &&"raddress".equals(notXml[i])) {
                i++;
                address = new ProxyReference(os, Integer.valueOf(notXml[i]), org.intermine.model.testmodel.Address.class);
                i++;
            };
            if (startI == i) {
                throw new IllegalArgumentException("Unknown field " + notXml[i]);
            }
        }
        departments = new ProxyCollection<org.intermine.model.testmodel.Department>(os, this, "departments", org.intermine.model.testmodel.Department.class);
        contractors = new ProxyCollection<org.intermine.model.testmodel.Contractor>(os, this, "contractors", org.intermine.model.testmodel.Contractor.class);
        oldContracts = new ProxyCollection<org.intermine.model.testmodel.Contractor>(os, this, "oldContracts", org.intermine.model.testmodel.Contractor.class);
        secretarys = new ProxyCollection<org.intermine.model.testmodel.Secretary>(os, this, "secretarys", org.intermine.model.testmodel.Secretary.class);
    }
    public void addCollectionElement(final String fieldName, final org.intermine.model.InterMineObject element) {
        if ("departments".equals(fieldName)) {
            departments.add((org.intermine.model.testmodel.Department) element);
        } else if ("contractors".equals(fieldName)) {
            contractors.add((org.intermine.model.testmodel.Contractor) element);
        } else if ("oldContracts".equals(fieldName)) {
            oldContracts.add((org.intermine.model.testmodel.Contractor) element);
        } else if ("secretarys".equals(fieldName)) {
            secretarys.add((org.intermine.model.testmodel.Secretary) element);
        } else {
            if (!org.intermine.model.testmodel.Company.class.equals(getClass())) {
                TypeUtil.addCollectionElement(this, fieldName, element);
                return;
            }
            throw new IllegalArgumentException("Unknown collection " + fieldName);
        }
    }
    public Class<?> getElementType(final String fieldName) {
        if ("departments".equals(fieldName)) {
            return org.intermine.model.testmodel.Department.class;
        }
        if ("contractors".equals(fieldName)) {
            return org.intermine.model.testmodel.Contractor.class;
        }
        if ("oldContracts".equals(fieldName)) {
            return org.intermine.model.testmodel.Contractor.class;
        }
        if ("secretarys".equals(fieldName)) {
            return org.intermine.model.testmodel.Secretary.class;
        }
        if (!org.intermine.model.testmodel.Company.class.equals(getClass())) {
            return TypeUtil.getElementType(org.intermine.model.testmodel.Company.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
