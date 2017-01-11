package org.intermine.model;

import org.intermine.metadata.TypeUtil;

public class Employee implements org.intermine.model.Employable, org.intermine.model.HasAddress
{
    // Attr: org.intermine.model.Employee.fullTime
    protected boolean fullTime;
    public boolean getFullTime() { return fullTime; }
    public void setFullTime(final boolean fullTime) { this.fullTime = fullTime; }

    // Attr: org.intermine.model.Employee.age
    protected int age;
    public int getAge() { return age; }
    public void setAge(final int age) { this.age = age; }

    // Attr: org.intermine.model.Employee.end
    protected java.lang.String end;
    public java.lang.String getEnd() { return end; }
    public void setEnd(final java.lang.String end) { this.end = end; }

 
    // Col: org.intermine.model.Employee.simpleObjects
    protected java.util.Set<org.intermine.model.SimpleObject> simpleObjects = new java.util.HashSet<org.intermine.model.SimpleObject>();
    public java.util.Set<org.intermine.model.SimpleObject> getSimpleObjects() { return simpleObjects; }
    public void setSimpleObjects(final java.util.Set<org.intermine.model.SimpleObject> simpleObjects) { this.simpleObjects = simpleObjects; }
    public void addSimpleObjects(final org.intermine.model.SimpleObject arg) { simpleObjects.add(arg); }

    // Attr: org.intermine.model.Employable.name
    protected java.lang.String name;
    public java.lang.String getName() { return name; }
    public void setName(final java.lang.String name) { this.name = name; }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(final java.lang.Integer id) { this.id = id; }

    // Ref: org.intermine.model.HasAddress.address
    protected org.intermine.model.Address address;
    public org.intermine.model.Address getAddress() { return address; }
    public void setAddress(final org.intermine.model.Address address) { this.address = address; }

    // Ref: org.intermine.model.Employee.department
    protected org.intermine.model.Department department;
    public org.intermine.model.Department getDepartment() { return department; }
    public void setDepartment(final org.intermine.model.Department department) { this.department = department; }

    @Override public boolean equals(Object o) { return (o instanceof Employee && id != null) ? id.equals(((Employee)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "Employee [address=" + (address == null ? "null" : (address.getId() == null ? "no id" : address.getId().toString())) + ", age=" + age + ", end=" + (end == null ? "null" : "\"" + end + "\"") + ", fullTime=" + fullTime + ", id=" + id + ", name=" + (name == null ? "null" : "\"" + name + "\"") + "]"; }
    
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
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
        if (!org.intermine.model.Employee.class.equals(getClass())) {
            return TypeUtil.getFieldValue(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        if ("age".equals(fieldName)) {
            return Integer.valueOf(age);
        }
        if ("end".equals(fieldName)) {
            return end;
        }
        if ("department".equals(fieldName)) {
            return department;
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
        if (!org.intermine.model.Employee.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("fullTime".equals(fieldName)) {
            fullTime = ((Boolean) value).booleanValue();
        } else if ("age".equals(fieldName)) {
            age = ((Integer) value).intValue();
        } else if ("end".equals(fieldName)) {
            end = (java.lang.String) value;
        } else if ("department".equals(fieldName)) {
            department = (org.intermine.model.Department) value;
        } else if ("simpleObjects".equals(fieldName)) {
            simpleObjects = (java.util.Set) value;
        } else if ("name".equals(fieldName)) {
            name = (java.lang.String) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else if ("address".equals(fieldName)) {
            address = (org.intermine.model.Address) value;
        } else {
/*            if (!org.intermine.model.Employee.class.equals(getClass())) {
                DynamicUtil.setFieldValue(this, fieldName, value);
                return;
            }*/
            throw new IllegalArgumentException("Unknown field " + fieldName);
        }
    }
    public Class<?> getFieldType(final String fieldName) {
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
            return org.intermine.model.Department.class;
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
            return org.intermine.model.Address.class;
        }
        if (!org.intermine.model.Employee.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.model.Employee.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
