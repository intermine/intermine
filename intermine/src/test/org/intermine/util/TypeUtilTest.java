package org.flymine.util;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.flymine.model.testmodel.*;

public class TypeUtilTest extends TestCase
{
    public TypeUtilTest(String arg) {
        super(arg);
    }

    public void testGetFieldValue() throws Exception {
        assertNotNull(TypeUtil.getFieldValue(new Company(), "departments"));
    }

    public void testSetFieldValue() throws Exception {
        Manager m = new Manager();
        String fieldValue = "Accountant";
        TypeUtil.setFieldValue(m, "title", fieldValue);
        assertEquals(fieldValue, m.getTitle());
    }

    public void testGetFieldInfos() throws Exception {
        Class c = Address.class;

        Map got = TypeUtil.getFieldInfos(c);
        assertEquals(new HashSet(Arrays.asList(new String[] {"id", "address"})), got.keySet());

        TypeUtil.FieldInfo idInfo = (TypeUtil.FieldInfo) got.get("id");
        assertEquals("id", idInfo.getName());
        assertEquals(c.getMethod("getId", new Class[] {}), idInfo.getGetter());
        assertEquals(c.getMethod("setId", new Class[] {Integer.class}), idInfo.getSetter());

        TypeUtil.FieldInfo addressInfo = (TypeUtil.FieldInfo) got.get("address");
        assertEquals("address", addressInfo.getName());
        assertEquals(c.getMethod("getAddress", new Class[] {}), addressInfo.getGetter());
        assertEquals(c.getMethod("setAddress", new Class[] {String.class}), addressInfo.getSetter());
    }

    public void testGetFieldInfosNoGetters() throws Exception {
        Map expected = new HashMap();
        Class c = NoGetSet.class;  // random class with no getters
        try {
            assertEquals(Collections.EMPTY_MAP, TypeUtil.getFieldInfos(c));
        } catch (NullPointerException e) {
            fail("Should not have thrown a NullPointerException");
        }
    }

    public void testPackageName() throws Exception {
        assertEquals("", TypeUtil.packageName("test"));
        assertEquals("package", TypeUtil.packageName("package.test"));
    }

    public void testUnqualifiedName() throws Exception {
        assertEquals("test", TypeUtil.unqualifiedName("test"));
        assertEquals("test", TypeUtil.unqualifiedName("package.test"));
    }

    public void testGetGetter() throws Exception {
        assertEquals(Company.class.getMethod("getName", new Class[] {}), TypeUtil.getGetter(Company.class, "name"));
    }
    
    //===========================

    private class NoGetSet {
    }
}
