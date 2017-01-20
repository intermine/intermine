package org.intermine.model.testmodel;

public class SimpleObject
{
    // Attr: org.intermine.model.testmodel.SimpleObject.name
    protected java.lang.String name;
    public java.lang.String getName() { return name; }
    public void setName(final java.lang.String name) { this.name = name; }

    // Ref: org.intermine.model.testmodel.SimpleObject.employee
    protected org.intermine.model.testmodel.Employee employee;
    public org.intermine.model.testmodel.Employee getEmployee() { return employee; }
    public void setEmployee(final org.intermine.model.testmodel.Employee employee) { this.employee = employee; }

    @Override public String toString() { return "SimpleObject [employee=" + (employee == null ? "null" : (employee.getId() == null ? "no id" : employee.getId().toString())) + ", name=" + (name == null ? "null" : "\"" + name + "\"") + "]"; }
}
