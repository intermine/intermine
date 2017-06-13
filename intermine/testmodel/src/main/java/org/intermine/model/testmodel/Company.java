package org.intermine.model.testmodel;

public interface Company extends org.intermine.model.testmodel.RandomInterface, org.intermine.model.testmodel.HasAddress, org.intermine.model.testmodel.HasSecretarys
{
    public java.lang.String getName();
    public void setName(final java.lang.String name);

    public int getVatNumber();
    public void setVatNumber(final int vatNumber);

    public org.intermine.model.testmodel.CEO getcEO();
    public void setcEO(final org.intermine.model.testmodel.CEO CEO);
    public void proxycEO(final org.intermine.objectstore.proxy.ProxyReference CEO);
    public org.intermine.model.InterMineObject proxGetcEO();

    public org.intermine.model.testmodel.Bank getBank();
    public void setBank(final org.intermine.model.testmodel.Bank bank);
    public void proxyBank(final org.intermine.objectstore.proxy.ProxyReference bank);
    public org.intermine.model.InterMineObject proxGetBank();

    public java.util.Set<org.intermine.model.testmodel.Department> getDepartments();
    public void setDepartments(final java.util.Set<org.intermine.model.testmodel.Department> departments);
    public void addDepartments(final org.intermine.model.testmodel.Department arg);

    public java.util.Set<org.intermine.model.testmodel.Contractor> getContractors();
    public void setContractors(final java.util.Set<org.intermine.model.testmodel.Contractor> contractors);
    public void addContractors(final org.intermine.model.testmodel.Contractor arg);

    public java.util.Set<org.intermine.model.testmodel.Contractor> getOldContracts();
    public void setOldContracts(final java.util.Set<org.intermine.model.testmodel.Contractor> oldContracts);
    public void addOldContracts(final org.intermine.model.testmodel.Contractor arg);

}
