package org.intermine.model.testmodel;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.NotXmlParser;
import org.intermine.objectstore.intermine.NotXmlRenderer;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.model.StringConstructor;
import org.intermine.metadata.TypeUtil;
import org.intermine.util.DynamicUtil;

public class SimpleObject implements org.intermine.model.FastPathObject
{
    // Attr: org.intermine.model.testmodel.SimpleObject.name
    protected java.lang.String name;
    public java.lang.String getName() { return name; }
    public void setName(final java.lang.String name) { this.name = name; }

    // Ref: org.intermine.model.testmodel.SimpleObject.employee
    protected org.intermine.model.InterMineObject employee;
    public org.intermine.model.testmodel.Employee getEmployee() { if (employee instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((org.intermine.model.testmodel.Employee) ((org.intermine.objectstore.proxy.ProxyReference) employee).getObject()); }; return (org.intermine.model.testmodel.Employee) employee; }
    public void setEmployee(final org.intermine.model.testmodel.Employee employee) { this.employee = employee; }
    public void proxyEmployee(final org.intermine.objectstore.proxy.ProxyReference employee) { this.employee = employee; }
    public org.intermine.model.InterMineObject proxGetEmployee() { return employee; }

    @Override public String toString() { return "SimpleObject [employee=" + (employee == null ? "null" : (employee.getId() == null ? "no id" : employee.getId().toString())) + ", name=" + (name == null ? "null" : "\"" + name + "\"") + "]"; }
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("name".equals(fieldName)) {
            return name;
        }
        if ("employee".equals(fieldName)) {
            if (employee instanceof ProxyReference) {
                return ((ProxyReference) employee).getObject();
            } else {
                return employee;
            }
        }
        if (!org.intermine.model.testmodel.SimpleObject.class.equals(getClass())) {
            return TypeUtil.getFieldValue(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        if ("name".equals(fieldName)) {
            return name;
        }
        if ("employee".equals(fieldName)) {
            return employee;
        }
        if (!org.intermine.model.testmodel.SimpleObject.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("name".equals(fieldName)) {
            name = (java.lang.String) value;
        } else if ("employee".equals(fieldName)) {
            employee = (org.intermine.model.InterMineObject) value;
        } else {
            if (!org.intermine.model.testmodel.SimpleObject.class.equals(getClass())) {
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
        if ("employee".equals(fieldName)) {
            return org.intermine.model.testmodel.Employee.class;
        }
        if (!org.intermine.model.testmodel.SimpleObject.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.model.testmodel.SimpleObject.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
