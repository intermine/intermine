package org.intermine.model.testmodel;

public class CEO extends org.intermine.model.testmodel.Manager
{
    // Attr: org.intermine.model.CEO.salary
    protected int salary;
    public int getSalary() { return salary; }
    public void setSalary(final int salary) { this.salary = salary; }

    // Ref: org.intermine.model.CEO.company
    protected org.intermine.model.testmodel.Company company;
    public org.intermine.model.testmodel.Company getCompany() { return company; }
    public void setCompany(final org.intermine.model.testmodel.Company company) { this.company = company; }

}
