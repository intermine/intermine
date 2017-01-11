package org.intermine.model;

public class CEO extends org.intermine.model.Manager
{
    // Attr: org.intermine.model.CEO.salary
    protected int salary;
    public int getSalary() { return salary; }
    public void setSalary(final int salary) { this.salary = salary; }

    // Ref: org.intermine.model.CEO.company
    protected org.intermine.model.Company company;
    public org.intermine.model.Company getCompany() { return company; }
    public void setCompany(final org.intermine.model.Company company) { this.company = company; }

}
