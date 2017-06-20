package org.intermine.model.testmodel;

public interface HasAddress extends org.intermine.model.InterMineObject
{
    public org.intermine.model.testmodel.Address getAddress();
    public void setAddress(final org.intermine.model.testmodel.Address address);
    public void proxyAddress(final org.intermine.objectstore.proxy.ProxyReference address);
    public org.intermine.model.InterMineObject proxGetAddress();

}
