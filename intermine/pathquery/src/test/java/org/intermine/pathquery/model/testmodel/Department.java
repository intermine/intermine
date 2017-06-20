package org.intermine.pathquery.model.testmodel;

import org.intermine.model.FastPathObject;

public class Department implements FastPathObject
{
    public Object getFieldValue(final String fieldName) throws IllegalAccessException {
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public Object getFieldProxy(final String fieldName) throws IllegalAccessException {
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
    public void setFieldValue(final String fieldName, final Object value) {
    }
    public Class<?> getFieldType(final String fieldName) {
        throw new IllegalArgumentException("Unknown field " + fieldName);
    }
}
