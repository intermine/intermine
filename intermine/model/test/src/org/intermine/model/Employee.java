package org.intermine.model;

public class Employee implements org.intermine.model.Employable, org.intermine.model.HasAddress
{
    // Attr: org.intermine.model.testmodel.Employee.fullTime
    protected boolean fullTime;
    public boolean getFullTime() { return fullTime; }
    public void setFullTime(final boolean fullTime) { this.fullTime = fullTime; }

    // Attr: org.intermine.model.testmodel.Employee.age
    protected int age;
    public int getAge() { return age; }
    public void setAge(final int age) { this.age = age; }

    // Attr: org.intermine.model.testmodel.Employee.end
    protected java.lang.String end;
    public java.lang.String getEnd() { return end; }
    public void setEnd(final java.lang.String end) { this.end = end; }

 
    // Col: org.intermine.model.testmodel.Employee.simpleObjects
    protected java.util.Set<org.intermine.model.SimpleObject> simpleObjects = new java.util.HashSet<org.intermine.model.SimpleObject>();
    public java.util.Set<org.intermine.model.SimpleObject> getSimpleObjects() { return simpleObjects; }
    public void setSimpleObjects(final java.util.Set<org.intermine.model.SimpleObject> simpleObjects) { this.simpleObjects = simpleObjects; }
    public void addSimpleObjects(final org.intermine.model.SimpleObject arg) { simpleObjects.add(arg); }

    // Attr: org.intermine.model.testmodel.Employable.name
    protected java.lang.String name;
    public java.lang.String getName() { return name; }
    public void setName(final java.lang.String name) { this.name = name; }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(final java.lang.Integer id) { this.id = id; }

    // Ref: org.intermine.model.testmodel.HasAddress.address
    protected org.intermine.model.Address address;
    public org.intermine.model.Address getAddress() { return address; }
    public void setAddress(final org.intermine.model.Address address) { this.address = address; }

    @Override public boolean equals(Object o) { return (o instanceof Employee && id != null) ? id.equals(((Employee)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "Employee [address=" + (address == null ? "null" : (address.getId() == null ? "no id" : address.getId().toString())) + ", age=" + age + ", end=" + (end == null ? "null" : "\"" + end + "\"") + ", fullTime=" + fullTime + ", id=" + id + ", name=" + (name == null ? "null" : "\"" + name + "\"") + "]"; }
}
