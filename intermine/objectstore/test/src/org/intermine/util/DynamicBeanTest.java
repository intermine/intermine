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

import java.util.List;

import junit.framework.TestCase;

import org.intermine.model.testmodel.*;

public class DynamicBeanTest extends TestCase
{
    public DynamicBeanTest(String arg) {
        super(arg);
    }

    public void testCreateObjectInterfaceAsClass() {
        try {
            DynamicBean.create(List.class, null);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testCreateObjectClassNoInterfaces() {
        Object obj = DynamicBean.create(Employee.class, null);
        assertTrue(obj instanceof Employee);

        Employee e = (Employee) obj;
        e.setName("Employee1");

        Department d = new Department();
        e.setDepartment(d);

        assertEquals("Employee1", e.getName());
        assertTrue(e.getDepartment() == d);
    }

    public void testCreateObjectNoClassOneInterface() {
        Object obj = DynamicBean.create(null, new Class[] { Employable.class });
        assertTrue(obj instanceof Employable);

        Employable e = (Employable) obj;
        e.setName("Employee1");
        assertEquals("Employee1", e.getName());

    }

    public void testCreateObjectNoClassTwoInterfaces() {
        Object obj = DynamicBean.create(null, new Class[] { Employable.class, ImportantPerson.class });
        assertTrue(obj instanceof Employable);
        assertTrue(obj instanceof ImportantPerson);
    }

    public void testCreateObjectClassTwoInterfaces() {
        Object obj = DynamicBean.create(Department.class, new Class[] { Employable.class, ImportantPerson.class });
        assertTrue(obj instanceof Department);
        assertTrue(obj instanceof Employable);
        assertTrue(obj instanceof ImportantPerson);
    }
}
