package org.flymine.util;

import junit.framework.TestCase;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

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

    public void testGetFieldValue() throws Exception {
        assertNotNull(TypeUtil.getFieldValue(new Company(), "key"));
    }

    public void testGetElementTypeNull() throws Exception {
        Collection c = null;
        try {
            TypeUtil.getElementType(c);
            fail("Expected NullPointerException");
        } catch (RuntimeException e) {
        }
    }

    public void testGetElementTypeEmpty() throws Exception {
        Collection c = new ArrayList();
        try {
            TypeUtil.getElementType(c);
            fail("Expected NoSuchElementException");
        } catch (RuntimeException e) {
        }
    }

    public void testGetElementType() throws Exception {
        Collection c = new ArrayList();
        c.add(new String());
        assertEquals(String.class, TypeUtil.getElementType(c));
    }
}
