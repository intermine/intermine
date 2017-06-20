package org.intermine.model.testmodel;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.NotXmlParser;
import org.intermine.objectstore.intermine.NotXmlRenderer;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.model.StringConstructor;
import org.intermine.metadata.TypeUtil;
import org.intermine.util.DynamicUtil;

public class Department implements org.intermine.model.testmodel.RandomInterface
{
    // Attr: org.intermine.model.testmodel.Department.name
    protected java.lang.String name;
    public java.lang.String getName() { return name; }
    public void setName(final java.lang.String name) { this.name = name; }

    // Ref: org.intermine.model.testmodel.Department.company
    protected org.intermine.model.InterMineObject company;
    public org.intermine.model.testmodel.Company getCompany() { if (company instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((org.intermine.model.testmodel.Company) ((org.intermine.objectstore.proxy.ProxyReference) company).getObject()); }; return (org.intermine.model.testmodel.Company) company; }
    public void setCompany(final org.intermine.model.testmodel.Company company) { this.company = company; }
    public void proxyCompany(final org.intermine.objectstore.proxy.ProxyReference company) { this.company = company; }
    public org.intermine.model.InterMineObject proxGetCompany() { return company; }

    // Ref: org.intermine.model.testmodel.Department.manager
    protected org.intermine.model.InterMineObject manager;
    public org.intermine.model.testmodel.Manager getManager() { if (manager instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((org.intermine.model.testmodel.Manager) ((org.intermine.objectstore.proxy.ProxyReference) manager).getObject()); }; return (org.intermine.model.testmodel.Manager) manager; }
    public void setManager(final org.intermine.model.testmodel.Manager manager) { this.manager = manager; }
    public void proxyManager(final org.intermine.objectstore.proxy.ProxyReference manager) { this.manager = manager; }
    public org.intermine.model.InterMineObject proxGetManager() { return manager; }

    // Col: org.intermine.model.testmodel.Department.employees
    protected java.util.Set<org.intermine.model.testmodel.Employee> employees = new java.util.HashSet<org.intermine.model.testmodel.Employee>();
    public java.util.Set<org.intermine.model.testmodel.Employee> getEmployees() { return employees; }
    public void setEmployees(final java.util.Set<org.intermine.model.testmodel.Employee> employees) { this.employees = employees; }
    public void addEmployees(final org.intermine.model.testmodel.Employee arg) { employees.add(arg); }

    // Col: org.intermine.model.testmodel.Department.rejectedEmployee
    protected java.util.Set<org.intermine.model.testmodel.Employee> rejectedEmployee = new java.util.HashSet<org.intermine.model.testmodel.Employee>();
    public java.util.Set<org.intermine.model.testmodel.Employee> getRejectedEmployee() { return rejectedEmployee; }
    public void setRejectedEmployee(final java.util.Set<org.intermine.model.testmodel.Employee> rejectedEmployee) { this.rejectedEmployee = rejectedEmployee; }
    public void addRejectedEmployee(final org.intermine.model.testmodel.Employee arg) { rejectedEmployee.add(arg); }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(final java.lang.Integer id) { this.id = id; }

    @Override public boolean equals(Object o) { return (o instanceof Department && id != null) ? id.equals(((Department)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "Department [company=" + (company == null ? "null" : (company.getId() == null ? "no id" : company.getId().toString())) + ", id=" + id + ", manager=" + (manager == null ? "null" : (manager.getId() == null ? "no id" : manager.getId().toString())) + ", name=" + (name == null ? "null" : "\"" + name + "\"") + "]"; }
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("name".equals(fieldName)) {
            return name;
        }
        if ("company".equals(fieldName)) {
            if (company instanceof ProxyReference) {
                return ((ProxyReference) company).getObject();
            } else {
                return company;
            }
        }
        if ("manager".equals(fieldName)) {
            if (manager instanceof ProxyReference) {
                return ((ProxyReference) manager).getObject();
            } else {
                return manager;
            }
        }
        if ("employees".equals(fieldName)) {
            return employees;
        }
        if ("rejectedEmployee".equals(fieldName)) {
            return rejectedEmployee;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.model.testmodel.Department.class.equals(getClass())) {
            return TypeUtil.getFieldValue(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        if ("name".equals(fieldName)) {
            return name;
        }
        if ("company".equals(fieldName)) {
            return company;
        }
        if ("manager".equals(fieldName)) {
            return manager;
        }
        if ("employees".equals(fieldName)) {
            return employees;
        }
        if ("rejectedEmployee".equals(fieldName)) {
            return rejectedEmployee;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.model.testmodel.Department.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("name".equals(fieldName)) {
            name = (java.lang.String) value;
        } else if ("company".equals(fieldName)) {
            company = (org.intermine.model.InterMineObject) value;
        } else if ("manager".equals(fieldName)) {
            manager = (org.intermine.model.InterMineObject) value;
        } else if ("employees".equals(fieldName)) {
            employees = (java.util.Set) value;
        } else if ("rejectedEmployee".equals(fieldName)) {
            rejectedEmployee = (java.util.Set) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else {
            if (!org.intermine.model.testmodel.Department.class.equals(getClass())) {
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
        if ("company".equals(fieldName)) {
            return org.intermine.model.testmodel.Company.class;
        }
        if ("manager".equals(fieldName)) {
            return org.intermine.model.testmodel.Manager.class;
        }
        if ("employees".equals(fieldName)) {
            return java.util.Set.class;
        }
        if ("rejectedEmployee".equals(fieldName)) {
            return java.util.Set.class;
        }
        if ("id".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if (!org.intermine.model.testmodel.Department.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.model.testmodel.Department.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public StringConstructor getoBJECT() {
        if (!org.intermine.model.testmodel.Department.class.equals(getClass())) {
            return NotXmlRenderer.render(this);
        }
        StringConstructor sb = new StringConstructor();
        sb.append("$_^org.intermine.model.testmodel.Department");
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
        if (company != null) {
            sb.append("$_^rcompany$_^").append(company.getId());
        }
        if (manager != null) {
            sb.append("$_^rmanager$_^").append(manager.getId());
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
        if (!org.intermine.model.testmodel.Department.class.equals(getClass())) {
            throw new IllegalStateException("Class " + getClass().getName() + " does not match code (org.intermine.model.testmodel.Department)");
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
            if ((i < notXml.length) &&"rcompany".equals(notXml[i])) {
                i++;
                company = new ProxyReference(os, Integer.valueOf(notXml[i]), org.intermine.model.testmodel.Company.class);
                i++;
            };
            if ((i < notXml.length) &&"rmanager".equals(notXml[i])) {
                i++;
                manager = new ProxyReference(os, Integer.valueOf(notXml[i]), org.intermine.model.testmodel.Manager.class);
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
        employees = new ProxyCollection<org.intermine.model.testmodel.Employee>(os, this, "employees", org.intermine.model.testmodel.Employee.class);
        rejectedEmployee = new ProxyCollection<org.intermine.model.testmodel.Employee>(os, this, "rejectedEmployee", org.intermine.model.testmodel.Employee.class);
    }
    public void addCollectionElement(final String fieldName, final org.intermine.model.InterMineObject element) {
        if ("employees".equals(fieldName)) {
            employees.add((org.intermine.model.testmodel.Employee) element);
        } else if ("rejectedEmployee".equals(fieldName)) {
            rejectedEmployee.add((org.intermine.model.testmodel.Employee) element);
        } else {
            if (!org.intermine.model.testmodel.Department.class.equals(getClass())) {
                TypeUtil.addCollectionElement(this, fieldName, element);
                return;
            }
            throw new IllegalArgumentException("Unknown collection " + fieldName);
        }
    }
    public Class<?> getElementType(final String fieldName) {
        if ("employees".equals(fieldName)) {
            return org.intermine.model.testmodel.Employee.class;
        }
        if ("rejectedEmployee".equals(fieldName)) {
            return org.intermine.model.testmodel.Employee.class;
        }
        if (!org.intermine.model.testmodel.Department.class.equals(getClass())) {
            return TypeUtil.getElementType(org.intermine.model.testmodel.Department.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
