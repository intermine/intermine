package org.intermine.model.testmodel;

public interface Company extends org.intermine.model.testmodel.HasAddress, org.intermine.model.InterMineFastPathObject 
{
    public java.lang.String getName();
    public void setName(final java.lang.String name);

    public int getVatNumber();
    public void setVatNumber(final int vatNumber);
}
