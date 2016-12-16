package org.intermine.model;

public class Address implements org.intermine.model.Thing
{
    // Attr: org.intermine.model.testmodel.Address.address
    protected java.lang.String address;
    public java.lang.String getAddress() { return address; }
    public void setAddress(final java.lang.String address) { this.address = address; }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(final java.lang.Integer id) { this.id = id; }

    @Override public boolean equals(Object o) { return (o instanceof Address && id != null) ? id.equals(((Address)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "Address [address=" + (address == null ? "null" : "\"" + address + "\"") + ", id=" + id + "]";}
}
