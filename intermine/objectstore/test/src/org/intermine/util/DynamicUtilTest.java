package org.intermine.util;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import org.intermine.model.FastPathObject;
import org.intermine.model.ShadowClass;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.CompanyShadow;
import org.intermine.model.testmodel.Employee;

public class DynamicUtilTest extends TestCase
{
    public DynamicUtilTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
    }

    // Employee is a class
    public void testCreateObjectClass() {
        Employee e = DynamicUtil.createObject(Employee.class);
        assertTrue(e instanceof Employee);
        e.setName("Employee1");
        assertEquals("Employee1", e.getName());
    }

    // Company is an interface, a Shadow class is available
    public void testCreateObjectInterfaceShadow() {
        Company c = DynamicUtil.createObject(Company.class);
        assertTrue(c.getClass() == CompanyShadow.class);
        assertTrue(c instanceof Company);
        assertTrue(c instanceof ShadowClass);

        c.setName("Company1");
        assertEquals("Company1", c.getName());
    }

    interface NewInterface extends org.intermine.model.InterMineObject
    {
        public String getName();
        public void setName(final String name);
    }

    // Create with an interface that doesn't have a Shadow class
    public void testCreateObjectInterface() {
        NewInterface ni = DynamicUtil.createObject(NewInterface.class);
        assertTrue(ni instanceof NewInterface);
        ni.setName("NewInterface1");
        assertEquals("NewInterface1", ni.getName());
    }

    public void testCreateObjectNull() {
        try {
            Class<? extends FastPathObject> cls = null;
            DynamicUtil.createObject(cls);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    // Employee is a class
    public void testGetClassSimple() {
        Employee e = DynamicUtil.createObject(Employee.class);
        assertTrue(e instanceof Employee);
        assertEquals(Employee.class, DynamicUtil.getClass(e));
        assertEquals(Employee.class, DynamicUtil.getClass(e.getClass()));
    }

    // Company is an interface, a Shadow class is available
    public void testGetClassInterfaceShadow() {
        Company c = DynamicUtil.createObject(Company.class);
        assertTrue(c.getClass() == CompanyShadow.class);
        assertTrue(c instanceof Company);
        assertEquals(Company.class, DynamicUtil.getClass(c));
        assertEquals(Company.class, DynamicUtil.getClass(c.getClass()));
    }

    // Create with an interface that doesn't have a Shadow class
    public void testGetClassInterface() {
        NewInterface ni = DynamicUtil.createObject(NewInterface.class);
        assertTrue(ni instanceof NewInterface);
        assertEquals(NewInterface.class, DynamicUtil.getClass(ni));
        assertEquals(NewInterface.class, DynamicUtil.getClass(ni.getClass()));
    }
}
