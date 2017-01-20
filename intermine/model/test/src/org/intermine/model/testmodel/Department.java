package org.intermine.model.testmodel;

import org.intermine.metadata.TypeUtil;

public class Department implements org.intermine.model.testmodel.Thing
{
    // Attr: org.intermine.model.testmodel.Department.name
    protected java.lang.String name;
    public java.lang.String getName() { return name; }
    public void setName(final java.lang.String name) { this.name = name; }

    // Ref: org.intermine.model.testmodel.Department.company
    protected org.intermine.model.testmodel.Company company;
    public org.intermine.model.testmodel.Company getCompany() { return company; }
    public void setCompany(final org.intermine.model.testmodel.Company company) { this.company = company; }

    // Ref: org.intermine.model.testmodel.Department.manager
    protected org.intermine.model.testmodel.Manager manager;
    public org.intermine.model.testmodel.Manager getManager() { return manager; }
    public void setManager(final org.intermine.model.testmodel.Manager manager) { this.manager = manager; }

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
    @Override public String toString() { return "Department [ id=" + id + ", manager=" + (manager == null ? "null" : (manager.getId() == null ? "no id" : manager.getId().toString())) + ", name=" + (name == null ? "null" : "\"" + name + "\"") + "]"; }
    
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
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
            company = (org.intermine.model.testmodel.Company) value;
        } else if ("manager".equals(fieldName)) {
            manager = (org.intermine.model.testmodel.Manager) value;
        } else if ("employees".equals(fieldName)) {
            employees = (java.util.Set) value;
        } else if ("rejectedEmployee".equals(fieldName)) {
            rejectedEmployee = (java.util.Set) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else {
/*            if (!org.intermine.model.testmodel.Department.class.equals(getClass())) {
                DynamicUtil.setFieldValue(this, fieldName, value);
                return;
            }*/
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
}
