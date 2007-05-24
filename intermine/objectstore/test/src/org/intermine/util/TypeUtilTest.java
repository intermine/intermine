package org.intermine.util;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Manager;

public class TypeUtilTest extends TestCase
{
    public TypeUtilTest(String arg) {
        super(arg);
    }

    public void testGetFieldValue() throws Exception {
        assertNotNull(TypeUtil.getFieldValue((Company) DynamicUtil.createObject(Collections.singleton(Company.class)), "departments"));
    }

    public void testSetFieldValue() throws Exception {
        Manager m = new Manager();
        String fieldValue = "Accountant";
        TypeUtil.setFieldValue(m, "title", fieldValue);
        assertEquals(fieldValue, m.getTitle());
    }

    public void testSetFieldValueNotExists() throws Exception {
        Manager m = new Manager();
        String fieldValue = "Accountant";
        try {
            TypeUtil.setFieldValue(m, "fieldThatDoesntExists", fieldValue);
            fail("should have throw an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // correct
        }
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

    public void testGetFieldInfosSuper() throws Exception {
        Class c = Company.class;

        Map got = TypeUtil.getFieldInfos(c);
        assertEquals(new HashSet(Arrays.asList(new String[] {"id", "name", "vatNumber", "address", "oldContracts", "contractors", "CEO", "departments", "secretarys"})), got.keySet());

        TypeUtil.FieldInfo idInfo = (TypeUtil.FieldInfo) got.get("id");
        assertEquals("id", idInfo.getName());
        assertEquals(c.getMethod("getId", new Class[] {}), idInfo.getGetter());
        assertEquals(c.getMethod("setId", new Class[] {Integer.class}), idInfo.getSetter());

        TypeUtil.FieldInfo addressInfo = (TypeUtil.FieldInfo) got.get("address");
        assertEquals("address", addressInfo.getName());
        assertEquals(c.getMethod("getAddress", new Class[] {}), addressInfo.getGetter());
        assertEquals(c.getMethod("setAddress", new Class[] {Address.class}), addressInfo.getSetter());

        TypeUtil.FieldInfo nameInfo = (TypeUtil.FieldInfo) got.get("name");
        assertEquals("name", nameInfo.getName());
        assertEquals(c.getMethod("getName", new Class[] {}), nameInfo.getGetter());
        assertEquals(c.getMethod("setName", new Class[] {String.class}), nameInfo.getSetter());

        TypeUtil.FieldInfo vatInfo = (TypeUtil.FieldInfo) got.get("vatNumber");
        assertEquals("vatNumber", vatInfo.getName());
        assertEquals(c.getMethod("getVatNumber", new Class[] {}), vatInfo.getGetter());
        assertEquals(c.getMethod("setVatNumber", new Class[] {Integer.TYPE}), vatInfo.getSetter());


    }


    public void testGetFieldInfosDynamic() throws Exception {
        Class c = DynamicUtil.createObject(Collections.singleton(Company.class)).getClass();

        Map got = TypeUtil.getFieldInfos(c);
        assertEquals(new HashSet(Arrays.asList(new String[] {"id", "name", "vatNumber", "address", "oldContracts", "contractors", "CEO", "departments", "secretarys"})), got.keySet());

        TypeUtil.FieldInfo idInfo = (TypeUtil.FieldInfo) got.get("id");
        assertEquals("id", idInfo.getName());
        assertEquals(c.getMethod("getId", new Class[] {}), idInfo.getGetter());
        assertEquals(c.getMethod("setId", new Class[] {Integer.class}), idInfo.getSetter());

        TypeUtil.FieldInfo addressInfo = (TypeUtil.FieldInfo) got.get("address");
        assertEquals("address", addressInfo.getName());
        assertEquals(c.getMethod("getAddress", new Class[] {}), addressInfo.getGetter());
        assertEquals(c.getMethod("setAddress", new Class[] {Address.class}), addressInfo.getSetter());

        TypeUtil.FieldInfo nameInfo = (TypeUtil.FieldInfo) got.get("name");
        assertEquals("name", nameInfo.getName());
        assertEquals(c.getMethod("getName", new Class[] {}), nameInfo.getGetter());
        assertEquals(c.getMethod("setName", new Class[] {String.class}), nameInfo.getSetter());

        TypeUtil.FieldInfo vatInfo = (TypeUtil.FieldInfo) got.get("vatNumber");
        assertEquals("vatNumber", vatInfo.getName());
        assertEquals(c.getMethod("getVatNumber", new Class[] {}), vatInfo.getGetter());
        assertEquals(c.getMethod("setVatNumber", new Class[] {Integer.TYPE}), vatInfo.getSetter());


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

    public void testStringToObject() throws Exception {
        assertEquals(new Integer(6), TypeUtil.stringToObject(Integer.class, "6"));
        assertEquals(new Integer(6), TypeUtil.stringToObject(Integer.TYPE, "6"));
        assertEquals(Boolean.TRUE, TypeUtil.stringToObject(Boolean.class, "true"));
        assertEquals(Boolean.TRUE, TypeUtil.stringToObject(Boolean.TYPE, "true"));
        assertEquals(new Double(12.0d), TypeUtil.stringToObject(Double.class, "12.0"));
        assertEquals(new Double(12.0d), TypeUtil.stringToObject(Double.TYPE, "12.0"));
        assertEquals(new Float(6.1f), TypeUtil.stringToObject(Float.class, "6.1"));
        assertEquals(new Float(6.1f), TypeUtil.stringToObject(Float.TYPE, "6.1"));
        assertEquals(new Long(6), TypeUtil.stringToObject(Long.class, "6"));
        assertEquals(new Long(6), TypeUtil.stringToObject(Long.TYPE, "6"));
        assertEquals(new Short("6"), TypeUtil.stringToObject(Short.class, "6"));
        assertEquals(new Short("6"), TypeUtil.stringToObject(Short.TYPE, "6"));
        assertEquals(new Byte("3"), TypeUtil.stringToObject(Byte.class, "3"));
        assertEquals(new Byte("3"), TypeUtil.stringToObject(Byte.TYPE, "3"));
        assertEquals(new Character('c'), TypeUtil.stringToObject(Character.class, "c"));
        assertEquals(new Character('c'), TypeUtil.stringToObject(Character.TYPE, "c"));
        assertEquals(new Date(7777777), TypeUtil.stringToObject(Date.class, "7777777"));
    }

    public void testFilter() throws Exception {
        assertEquals("", TypeUtil.javaiseClassName(""));
        assertEquals("OneTwo", TypeUtil.javaiseClassName("one two"));
        assertEquals("OneTwo", TypeUtil.javaiseClassName("one_two"));
        assertEquals("OneTwo", TypeUtil.javaiseClassName("one (two)"));
    }

    public void testIsInstanceOf() throws Exception {
        Manager man = (Manager) DynamicUtil.createObject(Collections.singleton(Manager.class));
        assertTrue(TypeUtil.isInstanceOf(man, "org.intermine.model.testmodel.Manager"));
        assertTrue(TypeUtil.isInstanceOf(man, "org.intermine.model.testmodel.Employee"));
        assertFalse(TypeUtil.isInstanceOf(man, "org.intermine.model.testmodel.Company"));
        try {
            assertTrue(TypeUtil.isInstanceOf(man, "org.intermine.model.testmodel.NoSuchClass"));
            fail("Expected exception");
        } catch (ClassNotFoundException e) {
            // expected
        }
    }
    
    //===========================

    private class NoGetSet {
    }
}
