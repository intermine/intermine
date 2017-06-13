package org.intermine.model.testmodel;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.NotXmlParser;
import org.intermine.objectstore.intermine.NotXmlRenderer;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.model.StringConstructor;
import org.intermine.metadata.TypeUtil;
import org.intermine.util.DynamicUtil;

public class CEO extends org.intermine.model.testmodel.Manager implements org.intermine.model.testmodel.HasSecretarys
{
    // Attr: org.intermine.model.testmodel.CEO.salary
    protected int salary;
    public int getSalary() { return salary; }
    public void setSalary(final int salary) { this.salary = salary; }

    // Ref: org.intermine.model.testmodel.CEO.company
    protected org.intermine.model.InterMineObject company;
    public org.intermine.model.testmodel.Company getCompany() { if (company instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((org.intermine.model.testmodel.Company) ((org.intermine.objectstore.proxy.ProxyReference) company).getObject()); }; return (org.intermine.model.testmodel.Company) company; }
    public void setCompany(final org.intermine.model.testmodel.Company company) { this.company = company; }
    public void proxyCompany(final org.intermine.objectstore.proxy.ProxyReference company) { this.company = company; }
    public org.intermine.model.InterMineObject proxGetCompany() { return company; }

    // Col: org.intermine.model.testmodel.HasSecretarys.secretarys
    protected java.util.Set<org.intermine.model.testmodel.Secretary> secretarys = new java.util.HashSet<org.intermine.model.testmodel.Secretary>();
    public java.util.Set<org.intermine.model.testmodel.Secretary> getSecretarys() { return secretarys; }
    public void setSecretarys(final java.util.Set<org.intermine.model.testmodel.Secretary> secretarys) { this.secretarys = secretarys; }
    public void addSecretarys(final org.intermine.model.testmodel.Secretary arg) { secretarys.add(arg); }

    @Override public boolean equals(Object o) { return (o instanceof CEO && id != null) ? id.equals(((CEO)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "CEO [address=" + (address == null ? "null" : (address.getId() == null ? "no id" : address.getId().toString())) + ", age=" + age + ", company=" + (company == null ? "null" : (company.getId() == null ? "no id" : company.getId().toString())) + ", department=" + (department == null ? "null" : (department.getId() == null ? "no id" : department.getId().toString())) + ", departmentThatRejectedMe=" + (departmentThatRejectedMe == null ? "null" : (departmentThatRejectedMe.getId() == null ? "no id" : departmentThatRejectedMe.getId().toString())) + ", employmentPeriod=" + (employmentPeriod == null ? "null" : (employmentPeriod.getId() == null ? "no id" : employmentPeriod.getId().toString())) + ", end=" + (end == null ? "null" : "\"" + end + "\"") + ", fullTime=" + fullTime + ", id=" + id + ", name=" + (name == null ? "null" : "\"" + name + "\"") + ", salary=" + salary + ", seniority=" + seniority + ", title=" + (title == null ? "null" : "\"" + title + "\"") + "]"; }
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("salary".equals(fieldName)) {
            return Integer.valueOf(salary);
        }
        if ("company".equals(fieldName)) {
            if (company instanceof ProxyReference) {
                return ((ProxyReference) company).getObject();
            } else {
                return company;
            }
        }
        if ("title".equals(fieldName)) {
            return title;
        }
        if ("fullTime".equals(fieldName)) {
            return Boolean.valueOf(fullTime);
        }
        if ("age".equals(fieldName)) {
            return Integer.valueOf(age);
        }
        if ("end".equals(fieldName)) {
            return end;
        }
        if ("department".equals(fieldName)) {
            if (department instanceof ProxyReference) {
                return ((ProxyReference) department).getObject();
            } else {
                return department;
            }
        }
        if ("departmentThatRejectedMe".equals(fieldName)) {
            if (departmentThatRejectedMe instanceof ProxyReference) {
                return ((ProxyReference) departmentThatRejectedMe).getObject();
            } else {
                return departmentThatRejectedMe;
            }
        }
        if ("employmentPeriod".equals(fieldName)) {
            if (employmentPeriod instanceof ProxyReference) {
                return ((ProxyReference) employmentPeriod).getObject();
            } else {
                return employmentPeriod;
            }
        }
        if ("simpleObjects".equals(fieldName)) {
            return simpleObjects;
        }
        if ("name".equals(fieldName)) {
            return name;
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
        if ("seniority".equals(fieldName)) {
            return seniority;
        }
        if ("secretarys".equals(fieldName)) {
            return secretarys;
        }
        if (!org.intermine.model.testmodel.CEO.class.equals(getClass())) {
            return TypeUtil.getFieldValue(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        if ("salary".equals(fieldName)) {
            return Integer.valueOf(salary);
        }
        if ("company".equals(fieldName)) {
            return company;
        }
        if ("title".equals(fieldName)) {
            return title;
        }
        if ("fullTime".equals(fieldName)) {
            return Boolean.valueOf(fullTime);
        }
        if ("age".equals(fieldName)) {
            return Integer.valueOf(age);
        }
        if ("end".equals(fieldName)) {
            return end;
        }
        if ("department".equals(fieldName)) {
            return department;
        }
        if ("departmentThatRejectedMe".equals(fieldName)) {
            return departmentThatRejectedMe;
        }
        if ("employmentPeriod".equals(fieldName)) {
            return employmentPeriod;
        }
        if ("simpleObjects".equals(fieldName)) {
            return simpleObjects;
        }
        if ("name".equals(fieldName)) {
            return name;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if ("address".equals(fieldName)) {
            return address;
        }
        if ("seniority".equals(fieldName)) {
            return seniority;
        }
        if ("secretarys".equals(fieldName)) {
            return secretarys;
        }
        if (!org.intermine.model.testmodel.CEO.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("salary".equals(fieldName)) {
            salary = ((Integer) value).intValue();
        } else if ("company".equals(fieldName)) {
            company = (org.intermine.model.InterMineObject) value;
        } else if ("title".equals(fieldName)) {
            title = (java.lang.String) value;
        } else if ("fullTime".equals(fieldName)) {
            fullTime = ((Boolean) value).booleanValue();
        } else if ("age".equals(fieldName)) {
            age = ((Integer) value).intValue();
        } else if ("end".equals(fieldName)) {
            end = (java.lang.String) value;
        } else if ("department".equals(fieldName)) {
            department = (org.intermine.model.InterMineObject) value;
        } else if ("departmentThatRejectedMe".equals(fieldName)) {
            departmentThatRejectedMe = (org.intermine.model.InterMineObject) value;
        } else if ("employmentPeriod".equals(fieldName)) {
            employmentPeriod = (org.intermine.model.InterMineObject) value;
        } else if ("simpleObjects".equals(fieldName)) {
            simpleObjects = (java.util.Set) value;
        } else if ("name".equals(fieldName)) {
            name = (java.lang.String) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else if ("address".equals(fieldName)) {
            address = (org.intermine.model.InterMineObject) value;
        } else if ("seniority".equals(fieldName)) {
            seniority = (java.lang.Integer) value;
        } else if ("secretarys".equals(fieldName)) {
            secretarys = (java.util.Set) value;
        } else {
            if (!org.intermine.model.testmodel.CEO.class.equals(getClass())) {
                DynamicUtil.setFieldValue(this, fieldName, value);
                return;
            }
            throw new IllegalArgumentException("Unknown field " + fieldName);
        }
    }
    public Class<?> getFieldType(final String fieldName) {
        if ("salary".equals(fieldName)) {
            return Integer.TYPE;
        }
        if ("company".equals(fieldName)) {
            return org.intermine.model.testmodel.Company.class;
        }
        if ("title".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("fullTime".equals(fieldName)) {
            return Boolean.TYPE;
        }
        if ("age".equals(fieldName)) {
            return Integer.TYPE;
        }
        if ("end".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("department".equals(fieldName)) {
            return org.intermine.model.testmodel.Department.class;
        }
        if ("departmentThatRejectedMe".equals(fieldName)) {
            return org.intermine.model.testmodel.Department.class;
        }
        if ("employmentPeriod".equals(fieldName)) {
            return org.intermine.model.testmodel.EmploymentPeriod.class;
        }
        if ("simpleObjects".equals(fieldName)) {
            return java.util.Set.class;
        }
        if ("name".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("id".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if ("address".equals(fieldName)) {
            return org.intermine.model.testmodel.Address.class;
        }
        if ("seniority".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if ("secretarys".equals(fieldName)) {
            return java.util.Set.class;
        }
        if (!org.intermine.model.testmodel.CEO.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.model.testmodel.CEO.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public StringConstructor getoBJECT() {
        if (!org.intermine.model.testmodel.CEO.class.equals(getClass())) {
            return NotXmlRenderer.render(this);
        }
        StringConstructor sb = new StringConstructor();
        sb.append("$_^org.intermine.model.testmodel.CEO");
        sb.append("$_^asalary$_^").append(salary);
        if (company != null) {
            sb.append("$_^rcompany$_^").append(company.getId());
        }
        if (title != null) {
            sb.append("$_^atitle$_^");
            String string = title;
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
        sb.append("$_^afullTime$_^").append(fullTime);
        sb.append("$_^aage$_^").append(age);
        if (end != null) {
            sb.append("$_^aend$_^");
            String string = end;
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
        if (department != null) {
            sb.append("$_^rdepartment$_^").append(department.getId());
        }
        if (departmentThatRejectedMe != null) {
            sb.append("$_^rdepartmentThatRejectedMe$_^").append(departmentThatRejectedMe.getId());
        }
        if (employmentPeriod != null) {
            sb.append("$_^remploymentPeriod$_^").append(employmentPeriod.getId());
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
        if (address != null) {
            sb.append("$_^raddress$_^").append(address.getId());
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
        if (!org.intermine.model.testmodel.CEO.class.equals(getClass())) {
            throw new IllegalStateException("Class " + getClass().getName() + " does not match code (org.intermine.model.testmodel.CEO)");
        }
        for (int i = 2; i < notXml.length;) {
            int startI = i;
            if ((i < notXml.length) && "asalary".equals(notXml[i])) {
                i++;
                salary = Integer.parseInt(notXml[i]);
                i++;
            }
            if ((i < notXml.length) &&"rcompany".equals(notXml[i])) {
                i++;
                company = new ProxyReference(os, Integer.valueOf(notXml[i]), org.intermine.model.testmodel.Company.class);
                i++;
            };
            if ((i < notXml.length) && "atitle".equals(notXml[i])) {
                i++;
                StringBuilder string = null;
                while ((i + 1 < notXml.length) && (notXml[i + 1].charAt(0) == 'd')) {
                    if (string == null) string = new StringBuilder(notXml[i]);
                    i++;
                    string.append("$_^").append(notXml[i].substring(1));
                }
                title = string == null ? notXml[i] : string.toString();
                i++;
            }
            if ((i < notXml.length) && "afullTime".equals(notXml[i])) {
                i++;
                fullTime = Boolean.parseBoolean(notXml[i]);
                i++;
            }
            if ((i < notXml.length) && "aage".equals(notXml[i])) {
                i++;
                age = Integer.parseInt(notXml[i]);
                i++;
            }
            if ((i < notXml.length) && "aend".equals(notXml[i])) {
                i++;
                StringBuilder string = null;
                while ((i + 1 < notXml.length) && (notXml[i + 1].charAt(0) == 'd')) {
                    if (string == null) string = new StringBuilder(notXml[i]);
                    i++;
                    string.append("$_^").append(notXml[i].substring(1));
                }
                end = string == null ? notXml[i] : string.toString();
                i++;
            }
            if ((i < notXml.length) &&"rdepartment".equals(notXml[i])) {
                i++;
                department = new ProxyReference(os, Integer.valueOf(notXml[i]), org.intermine.model.testmodel.Department.class);
                i++;
            };
            if ((i < notXml.length) &&"rdepartmentThatRejectedMe".equals(notXml[i])) {
                i++;
                departmentThatRejectedMe = new ProxyReference(os, Integer.valueOf(notXml[i]), org.intermine.model.testmodel.Department.class);
                i++;
            };
            if ((i < notXml.length) &&"remploymentPeriod".equals(notXml[i])) {
                i++;
                employmentPeriod = new ProxyReference(os, Integer.valueOf(notXml[i]), org.intermine.model.testmodel.EmploymentPeriod.class);
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
            if ((i < notXml.length) &&"raddress".equals(notXml[i])) {
                i++;
                address = new ProxyReference(os, Integer.valueOf(notXml[i]), org.intermine.model.testmodel.Address.class);
                i++;
            };
            if ((i < notXml.length) && "aseniority".equals(notXml[i])) {
                i++;
                seniority = Integer.valueOf(notXml[i]);
                i++;
            }
            if (startI == i) {
                throw new IllegalArgumentException("Unknown field " + notXml[i]);
            }
        }
        simpleObjects = new ProxyCollection<org.intermine.model.testmodel.SimpleObject>(os, this, "simpleObjects", org.intermine.model.testmodel.SimpleObject.class);
        secretarys = new ProxyCollection<org.intermine.model.testmodel.Secretary>(os, this, "secretarys", org.intermine.model.testmodel.Secretary.class);
    }
    public void addCollectionElement(final String fieldName, final org.intermine.model.InterMineObject element) {
        if ("simpleObjects".equals(fieldName)) {
            simpleObjects.add((org.intermine.model.testmodel.SimpleObject) element);
        } else if ("secretarys".equals(fieldName)) {
            secretarys.add((org.intermine.model.testmodel.Secretary) element);
        } else {
            if (!org.intermine.model.testmodel.CEO.class.equals(getClass())) {
                TypeUtil.addCollectionElement(this, fieldName, element);
                return;
            }
            throw new IllegalArgumentException("Unknown collection " + fieldName);
        }
    }
    public Class<?> getElementType(final String fieldName) {
        if ("simpleObjects".equals(fieldName)) {
            return org.intermine.model.testmodel.SimpleObject.class;
        }
        if ("secretarys".equals(fieldName)) {
            return org.intermine.model.testmodel.Secretary.class;
        }
        if (!org.intermine.model.testmodel.CEO.class.equals(getClass())) {
            return TypeUtil.getElementType(org.intermine.model.testmodel.CEO.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
