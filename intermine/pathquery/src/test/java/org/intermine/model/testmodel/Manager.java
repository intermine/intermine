package org.intermine.model.testmodel;

public class Manager extends org.intermine.model.testmodel.Employee
{
    // Attr: org.intermine.model.testmodel.Manager.title
    protected java.lang.String title;
    public java.lang.String getTitle() { return title; }
    public void setTitle(final java.lang.String title) { this.title = title; }


    @Override public boolean equals(Object o) { return (o instanceof Manager && id != null) ? id.equals(((Manager)o).getId()) : this == o; }
    @Override public int hashCode() { return (id != null) ? id.hashCode() : super.hashCode(); }
    @Override public String toString() { return "Manager [address=" + (address == null ? "null" : (address.getId() == null ? "no id" : address.getId().toString())) + ", age=" + age + ", fullTime=" + fullTime + ", id=" + id + ", name=" + (name == null ? "null" : "\"" + name + "\"") + ", title=" + (title == null ? "null" : "\"" + title + "\"") + "]"; }
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        if ("title".equals(fieldName)) {
            return title;
        }
        if ("fullTime".equals(fieldName)) {
            return Boolean.valueOf(fullTime);
        }
        if ("age".equals(fieldName)) {
            return Integer.valueOf(age);
        }
        if ("end".equals(fieldName)) {
            return end;
        }
        if ("simpleObjects".equals(fieldName)) {
            return simpleObjects;
        }
        if ("name".equals(fieldName)) {
            return name;
        }
        if ("id".equals(fieldName)) {
            return id;
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
        if ("title".equals(fieldName)) {
            title = (java.lang.String) value;
        } else if ("fullTime".equals(fieldName)) {
            fullTime = ((Boolean) value).booleanValue();
        } else if ("age".equals(fieldName)) {
            age = ((Integer) value).intValue();
        } else if ("end".equals(fieldName)) {
            end = (java.lang.String) value;
        } else if ("id".equals(fieldName)) {
            id = (java.lang.Integer) value;
        } else {
            throw new IllegalArgumentException("Unknown field " + fieldName);
        }
    }
    public Class<?> getFieldType(final String fieldName) {
        if ("title".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("fullTime".equals(fieldName)) {
            return Boolean.TYPE;
        }
        if ("age".equals(fieldName)) {
            return Integer.TYPE;
        }
        if ("end".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("simpleObjects".equals(fieldName)) {
            return java.util.Set.class;
        }
        if ("name".equals(fieldName)) {
            return java.lang.String.class;
        }
        if ("id".equals(fieldName)) {
            return java.lang.Integer.class;
        }
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
