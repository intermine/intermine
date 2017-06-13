package org.intermine.model.testmodel;

public interface HasSecretarys extends org.intermine.model.InterMineObject
{
    public java.util.Set<org.intermine.model.testmodel.Secretary> getSecretarys();
    public void setSecretarys(final java.util.Set<org.intermine.model.testmodel.Secretary> secretarys);
    public void addSecretarys(final org.intermine.model.testmodel.Secretary arg);

}
