package org.intermine.model;

public interface Company extends org.intermine.model.HasAddress, org.intermine.model.FastPathObject 
{
    public java.lang.String getName();
    public void setName(final java.lang.String name);

    public int getVatNumber();
    public void setVatNumber(final int vatNumber);
}
