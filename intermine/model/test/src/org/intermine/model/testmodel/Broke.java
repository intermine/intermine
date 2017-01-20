package org.intermine.model.testmodel;

public interface Broke extends org.intermine.model.FastPathObject
{
    public int getDebt();
    public void setDebt(final int debt);

    public double getInterestRate();
    public void setInterestRate(final double interestRate);

    public org.intermine.model.testmodel.Employable getOwedBy();
    public void setOwedBy(final org.intermine.model.testmodel.Employable owedBy);
}
