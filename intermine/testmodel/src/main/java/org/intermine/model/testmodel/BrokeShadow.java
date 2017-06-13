package org.intermine.model.testmodel;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.NotXmlParser;
import org.intermine.objectstore.intermine.NotXmlRenderer;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.model.StringConstructor;
import org.intermine.metadata.TypeUtil;
import org.intermine.util.DynamicUtil;
import org.intermine.model.ShadowClass;

public class BrokeShadow implements Broke, ShadowClass
{
    public static final Class<Broke> shadowOf = Broke.class;
    // Attr: org.intermine.model.testmodel.Broke.debt
    protected int debt;
    public int getDebt() { return debt; }
    public void setDebt(final int debt) { this.debt = debt; }

    // Attr: org.intermine.model.testmodel.Broke.interestRate
    protected double interestRate;
    public double getInterestRate() { return interestRate; }
    public void setInterestRate(final double interestRate) { this.interestRate = interestRate; }

    // Ref: org.intermine.model.testmodel.Broke.bank
    protected org.intermine.model.InterMineObject bank;
    public org.intermine.model.testmodel.Bank getBank() { if (bank instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((org.intermine.model.testmodel.Bank) ((org.intermine.objectstore.proxy.ProxyReference) bank).getObject()); }; return (org.intermine.model.testmodel.Bank) bank; }
    public void setBank(final org.intermine.model.testmodel.Bank bank) { this.bank = bank; }
    public void proxyBank(final org.intermine.objectstore.proxy.ProxyReference bank) { this.bank = bank; }
    public org.intermine.model.InterMineObject proxGetBank() { return bank; }

    // Ref: org.intermine.model.testmodel.Broke.owedBy
    protected org.intermine.model.InterMineObject owedBy;
    public org.intermine.model.testmodel.Employable getOwedBy() { if (owedBy instanceof org.intermine.objectstore.proxy.ProxyReference) { return ((org.intermine.model.testmodel.Employable) ((org.intermine.objectstore.proxy.ProxyReference) owedBy).getObject()); }; return (org.intermine.model.testmodel.Employable) owedBy; }
    public void setOwedBy(final org.intermine.model.testmodel.Employable owedBy) { this.owedBy = owedBy; }
    public void proxyOwedBy(final org.intermine.objectstore.proxy.ProxyReference owedBy) { this.owedBy = owedBy; }
    public org.intermine.model.InterMineObject proxGetOwedBy() { return owedBy; }

    // Attr: org.intermine.model.InterMineObject.id
    protected java.lang.Integer id;
    public java.lang.Integer getId() { return id; }
    public void setId(final java.lang.Integer id) { this.id = id; }

    @Override public boolean equals(Object o) { return (o instanceof Broke && id != null) ? id.equals(((Broke)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "Broke [bank=" + (bank == null ? "null" : (bank.getId() == null ? "no id" : bank.getId().toString())) + ", debt=" + debt + ", id=" + id + ", interestRate=" + interestRate + ", owedBy=" + (owedBy == null ? "null" : (owedBy.getId() == null ? "no id" : owedBy.getId().toString())) + "]"; }
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("debt".equals(fieldName)) {
            return Integer.valueOf(debt);
        }
        if ("interestRate".equals(fieldName)) {
            return Double.valueOf(interestRate);
        }
        if ("bank".equals(fieldName)) {
            if (bank instanceof ProxyReference) {
                return ((ProxyReference) bank).getObject();
            } else {
                return bank;
            }
        }
        if ("owedBy".equals(fieldName)) {
            if (owedBy instanceof ProxyReference) {
                return ((ProxyReference) owedBy).getObject();
            } else {
                return owedBy;
            }
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.model.testmodel.Broke.class.equals(getClass())) {
            return TypeUtil.getFieldValue(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        if ("debt".equals(fieldName)) {
            return Integer.valueOf(debt);
        }
        if ("interestRate".equals(fieldName)) {
            return Double.valueOf(interestRate);
        }
        if ("bank".equals(fieldName)) {
            return bank;
        }
        if ("owedBy".equals(fieldName)) {
            return owedBy;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        if (!org.intermine.model.testmodel.Broke.class.equals(getClass())) {
            return TypeUtil.getFieldProxy(this, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("debt".equals(fieldName)) {
            debt = ((Integer) value).intValue();
        } else if ("interestRate".equals(fieldName)) {
            interestRate = ((Double) value).doubleValue();
        } else if ("bank".equals(fieldName)) {
            bank = (org.intermine.model.InterMineObject) value;
        } else if ("owedBy".equals(fieldName)) {
            owedBy = (org.intermine.model.InterMineObject) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else {
            if (!org.intermine.model.testmodel.Broke.class.equals(getClass())) {
                DynamicUtil.setFieldValue(this, fieldName, value);
                return;
            }
            throw new IllegalArgumentException("Unknown field " + fieldName);
        }
    }
    public Class<?> getFieldType(final String fieldName) {
        if ("debt".equals(fieldName)) {
            return Integer.TYPE;
        }
        if ("interestRate".equals(fieldName)) {
            return Double.TYPE;
        }
        if ("bank".equals(fieldName)) {
            return org.intermine.model.testmodel.Bank.class;
        }
        if ("owedBy".equals(fieldName)) {
            return org.intermine.model.testmodel.Employable.class;
        }
        if ("id".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        if (!org.intermine.model.testmodel.Broke.class.equals(getClass())) {
            return TypeUtil.getFieldType(org.intermine.model.testmodel.Broke.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public StringConstructor getoBJECT() {
        if (!org.intermine.model.testmodel.BrokeShadow.class.equals(getClass())) {
            return NotXmlRenderer.render(this);
        }
        StringConstructor sb = new StringConstructor();
        sb.append("$_^org.intermine.model.testmodel.Broke");
        sb.append("$_^adebt$_^").append(debt);
        sb.append("$_^ainterestRate$_^").append(interestRate);
        if (bank != null) {
            sb.append("$_^rbank$_^").append(bank.getId());
        }
        if (owedBy != null) {
            sb.append("$_^rowedBy$_^").append(owedBy.getId());
        }
        if (id != null) {
            sb.append("$_^aid$_^").append(id);
        }
        return sb;
    }
    public void setoBJECT(String notXml, ObjectStore os) {
        setoBJECT(NotXmlParser.SPLITTER.split(notXml), os);
    }
    public void setoBJECT(final String[] notXml, final ObjectStore os) {
        if (!org.intermine.model.testmodel.BrokeShadow.class.equals(getClass())) {
            throw new IllegalStateException("Class " + getClass().getName() + " does not match code (org.intermine.model.testmodel.Broke)");
        }
        for (int i = 2; i < notXml.length;) {
            int startI = i;
            if ((i < notXml.length) && "adebt".equals(notXml[i])) {
                i++;
                debt = Integer.parseInt(notXml[i]);
                i++;
            }
            if ((i < notXml.length) && "ainterestRate".equals(notXml[i])) {
                i++;
                interestRate = Double.parseDouble(notXml[i]);
                i++;
            }
            if ((i < notXml.length) &&"rbank".equals(notXml[i])) {
                i++;
                bank = new ProxyReference(os, Integer.valueOf(notXml[i]), org.intermine.model.testmodel.Bank.class);
                i++;
            };
            if ((i < notXml.length) &&"rowedBy".equals(notXml[i])) {
                i++;
                owedBy = new ProxyReference(os, Integer.valueOf(notXml[i]), org.intermine.model.testmodel.Employable.class);
                i++;
            };
            if ((i < notXml.length) && "aid".equals(notXml[i])) {
                i++;
                id = Integer.valueOf(notXml[i]);
                i++;
            }
            if (startI == i) {
                throw new IllegalArgumentException("Unknown field " + notXml[i]);
            }
        }
    }
    public void addCollectionElement(final String fieldName, final org.intermine.model.InterMineObject element) {
        {
            if (!org.intermine.model.testmodel.Broke.class.equals(getClass())) {
                TypeUtil.addCollectionElement(this, fieldName, element);
                return;
            }
            throw new IllegalArgumentException("Unknown collection " + fieldName);
        }
    }
    public Class<?> getElementType(final String fieldName) {
        if (!org.intermine.model.testmodel.Broke.class.equals(getClass())) {
            return TypeUtil.getElementType(org.intermine.model.testmodel.Broke.class, fieldName);
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
