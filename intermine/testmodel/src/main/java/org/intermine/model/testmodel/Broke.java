package org.intermine.model.testmodel;

public interface Broke extends org.intermine.model.InterMineObject
{
    public int getDebt();
    public void setDebt(final int debt);

    public double getInterestRate();
    public void setInterestRate(final double interestRate);

    public org.intermine.model.testmodel.Bank getBank();
    public void setBank(final org.intermine.model.testmodel.Bank bank);
    public void proxyBank(final org.intermine.objectstore.proxy.ProxyReference bank);
    public org.intermine.model.InterMineObject proxGetBank();

    public org.intermine.model.testmodel.Employable getOwedBy();
    public void setOwedBy(final org.intermine.model.testmodel.Employable owedBy);
    public void proxyOwedBy(final org.intermine.objectstore.proxy.ProxyReference owedBy);
    public org.intermine.model.InterMineObject proxGetOwedBy();

}
