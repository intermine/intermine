package org.flymine.util;

import junit.framework.TestCase;

import org.flymine.model.testmodel.*;

public class ModelUtilTest extends TestCase
{
    public ModelUtilTest(String arg) {
        super(arg);
    }

    public void testGetFieldTypeCollection() {
        assertEquals(ModelUtil.COLLECTION, ModelUtil.getFieldType(Company.class, "departments"));
    }

    public void testGetFieldTypeAttribute() {
        assertEquals(ModelUtil.ATTRIBUTE, ModelUtil.getFieldType(Company.class, "name"));
    }

    public void testGetFieldTypeReference() {
        assertEquals(ModelUtil.REFERENCE, ModelUtil.getFieldType(Company.class, "address"));
    }
}
