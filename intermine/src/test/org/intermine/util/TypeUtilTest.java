package org.flymine.util;

import junit.framework.TestCase;

import java.lang.reflect.Field;

import org.flymine.model.testmodel.*;

public class TypeUtilTest extends TestCase
{
    public TypeUtilTest(String arg) {
        super(arg);
    }

    public void testInvalidGetField() {
        Field f = TypeUtil.getField(Manager.class, "FullTime");
        assertNull("Field should be null", f);
    }

    public void testValidNonInheritedGetField() {
        Field f = TypeUtil.getField(Manager.class, "title");
        assertNotNull("Field should not be null", f);
    }

    public void testValidInheritedGetField() {
        Field f = TypeUtil.getField(Manager.class, "fullTime");
        assertNotNull("Field should not be null", f);
    }
}
