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

import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;

public class DynamicBeanTest extends TestCase
{
    public DynamicBeanTest(String arg) {
        super(arg);
    }

    // Employee is a class
    public void testCreateObjectClass() {
        Object obj = DynamicBean.create(Employee.class);
        assertTrue(obj instanceof Employee);

        Employee e = (Employee) obj;
        e.setName("Employee1");

        Department d = new Department();
        e.setDepartment(d);

        assertEquals("Employee1", e.getName());
        assertTrue(e.getDepartment() == d);
    }

    // Company is an interface
    public void testCreateObjectInterface() {
        Company company = DynamicBean.create(Company.class);
        assertTrue(company instanceof Company);

        company.setName("Company1");

        CEO ceo = new CEO();
        company.setcEO(ceo);

        assertEquals("Company1", company.getName());
        assertTrue(company.getcEO() == ceo);
    }

    public void testCreateObjectNull() {
        try {
            DynamicBean.create(null);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }
}
