package org.flymine.objectstore.query;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.util.Set;
import java.util.HashSet;

import org.flymine.model.testmodel.*;
import org.flymine.testing.OneTimeTestCase;
import org.flymine.metadata.Model;

public class QueryHelperTest extends QueryTestCase
{
    Model model;

    public QueryHelperTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        super.setUp();
        model = Model.getInstanceByName("testmodel");
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(QueryHelperTest.class);
    }
    
    public void testQueryForExampleSetEmptySet() {
        assertNull(QueryHelper.createQueryForExampleSet(new HashSet(), model));
    }

    public void testQueryForExampleSetDifferentElementTypes() {
        Set set = new HashSet();
        Address a = new Address();
        a.setAddress("1 The Street");
        Department d = new Department();
        d.setName("Department1");
        set.add(a);
        set.add(d);

        try {
            QueryHelper.createQueryForExampleSet(set, model);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

    }

    public void testQueryForExampleSetOneObjectKeyAttributes() throws Exception {
        // Address's key is "address" field
        Set set = new HashSet();
        Address a = new Address();
        a.setAddress("1 The Street");
        set.add(a);
        Query q = QueryHelper.createQueryForExampleSet(set, model);

        Query expected = new Query();
        QueryClass qc = new QueryClass(Address.class);
        expected.addToSelect(qc);
        expected.addFrom(qc);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        ConstraintSet cs2 = new ConstraintSet(ConstraintSet.OR);
        ConstraintSet cs3 = new ConstraintSet(ConstraintSet.AND);
        QueryField qf = new QueryField(qc, "address");
        cs3.addConstraint(new SimpleConstraint(qf, SimpleConstraint.EQUALS, new QueryValue("1 The Street")));
        cs2.addConstraint(cs3);
        cs1.addConstraint(cs2);
        expected.setConstraint(cs1);

        assertEquals(expected, q);
    }

    public void testQueryForExampleSetTwoObjectsKeyAttributes() throws Exception {
        Set set = new HashSet();
        Address a1 = new Address();
        a1.setAddress("1 The Street");
        set.add(a1);
        Address a2 = new Address();
        a2.setAddress("2 The Street");
        set.add(a2);
        Query q = QueryHelper.createQueryForExampleSet(set, model);

        Query expected = new Query();
        QueryClass qc = new QueryClass(Address.class);
        expected.addToSelect(qc);
        expected.addFrom(qc);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        ConstraintSet cs2 = new ConstraintSet(ConstraintSet.OR);
        ConstraintSet cs3 = new ConstraintSet(ConstraintSet.AND);
        ConstraintSet cs4 = new ConstraintSet(ConstraintSet.AND);

        QueryField qf = new QueryField(qc, "address");
        cs3.addConstraint(new SimpleConstraint(qf, SimpleConstraint.EQUALS, new QueryValue("1 The Street")));
        cs2.addConstraint(cs3);

        cs4.addConstraint(new SimpleConstraint(qf, SimpleConstraint.EQUALS, new QueryValue("2 The Street")));
        cs2.addConstraint(cs4);

        cs1.addConstraint(cs2);
        expected.setConstraint(cs1);

    }

    public void testQueryForExampleSetOneObjectKeyAttributesReferences() throws Exception {
        // Employee's key is "name", "address", "age" fields
        Set set = new HashSet();
        Address a = new Address();
        a.setAddress("1 The Street");

        Employee e = new Employee();
        e.setAddress(a);
        e.setName("Employee 1");
        e.setAge(20);
        set.add(e);

        Query q = QueryHelper.createQueryForExampleSet(set, model);

        Query expected = new Query();
        QueryClass qcEmployee = new QueryClass(Employee.class);
        QueryClass qcAddress = new QueryClass(Address.class);
        expected.addToSelect(qcEmployee);
        expected.addFrom(qcEmployee);
        expected.addFrom(qcAddress);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        ConstraintSet cs2 = new ConstraintSet(ConstraintSet.OR);
        ConstraintSet cs3 = new ConstraintSet(ConstraintSet.AND);
        QueryReference qr1 = new QueryObjectReference(qcEmployee, "address");
        cs1.addConstraint(new ContainsConstraint(qr1, ContainsConstraint.CONTAINS, qcAddress));

        QueryField qf1 = new QueryField(qcEmployee, "name");
        cs3.addConstraint(new SimpleConstraint(qf1, SimpleConstraint.EQUALS, new QueryValue("Employee 1")));

        QueryField qf2 = new QueryField(qcEmployee, "age");
        cs3.addConstraint(new SimpleConstraint(qf2, SimpleConstraint.EQUALS, new QueryValue(new Integer(20))));

        cs3.addConstraint(new ClassConstraint(qcAddress, ClassConstraint.EQUALS, a));

        cs2.addConstraint(cs3);
        cs1.addConstraint(cs2);
        expected.setConstraint(cs1);

        assertEquals(expected, q);
    }

    public void testQueryForExampleSetTwoObjectsKeyAttributesReferences() throws Exception {
        // Employee's key is "name", "address", "age" fields
        Set set = new HashSet();
        Address a1 = new Address();
        a1.setAddress("1 The Street");
        Address a2 = new Address();
        a2.setAddress("2 The Street");

        Employee e1 = new Employee();
        e1.setAddress(a1);
        e1.setName("Employee 1");
        e1.setAge(20);
        set.add(e1);

        Employee e2 = new Employee();
        e2.setAddress(a2);
        e2.setName("Employee 2");
        e2.setAge(30);
        set.add(e2);

        Query q = QueryHelper.createQueryForExampleSet(set, model);

        Query expected = new Query();
        QueryClass qcEmployee = new QueryClass(Employee.class);
        QueryClass qcAddress = new QueryClass(Address.class);
        expected.addToSelect(qcEmployee);
        expected.addFrom(qcEmployee);
        expected.addFrom(qcAddress);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        ConstraintSet cs2 = new ConstraintSet(ConstraintSet.OR);
        ConstraintSet cs3 = new ConstraintSet(ConstraintSet.AND);
        ConstraintSet cs4 = new ConstraintSet(ConstraintSet.AND);
        QueryReference qr1 = new QueryObjectReference(qcEmployee, "address");
        cs1.addConstraint(new ContainsConstraint(qr1, ContainsConstraint.CONTAINS, qcAddress));

        QueryField qf1 = new QueryField(qcEmployee, "name");
        QueryField qf2 = new QueryField(qcEmployee, "age");

        // First Employee
        cs3.addConstraint(new SimpleConstraint(qf1, SimpleConstraint.EQUALS, new QueryValue("Employee 1")));
        cs3.addConstraint(new SimpleConstraint(qf2, SimpleConstraint.EQUALS, new QueryValue(new Integer(20))));
        cs3.addConstraint(new ClassConstraint(qcAddress, ClassConstraint.EQUALS, a1));
        cs2.addConstraint(cs3);

        // Second Employee
        cs4.addConstraint(new SimpleConstraint(qf1, SimpleConstraint.EQUALS, new QueryValue("Employee 2")));
        cs4.addConstraint(new SimpleConstraint(qf2, SimpleConstraint.EQUALS, new QueryValue(new Integer(30))));
        cs4.addConstraint(new ClassConstraint(qcAddress, ClassConstraint.EQUALS, a2));
        cs2.addConstraint(cs4);

        cs1.addConstraint(cs2);
        expected.setConstraint(cs1);

        assertEquals(expected, q);
    }

    public void testQueryForExampleObjectNullObject() throws Exception {
        try {
            QueryHelper.createQueryForExampleObject(null, model);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testQueryForExampleObjectKeyAttributes() throws Exception {
        // Address's key is "address" field
        Address a = new Address();
        a.setAddress("1 The Street");
        Query q = QueryHelper.createQueryForExampleObject(a, model);

        Query expected = new Query();
        QueryClass qc = new QueryClass(Address.class);
        expected.addToSelect(qc);
        expected.addFrom(qc);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        QueryField qf = new QueryField(qc, "address");
        cs1.addConstraint(new SimpleConstraint(qf, SimpleConstraint.EQUALS, new QueryValue("1 The Street")));
        expected.setConstraint(cs1);

        assertEquals(expected, q);

    }

    public void testQueryForExampleObjectKeyAttributesReferences() throws Exception {
        // Employee's key is "name", "address", "fullTime" fields
        Address a = new Address();
        a.setAddress("1 The Street");

        Employee e = new Employee();
        e.setAddress(a);
        e.setName("Employee 1");
        e.setAge(20);

        Query q = QueryHelper.createQueryForExampleObject(e, model);

        Query expected = new Query();
        QueryClass qcEmployee = new QueryClass(Employee.class);
        QueryClass qcAddress = new QueryClass(Address.class);
        expected.addToSelect(qcEmployee);
        expected.addFrom(qcEmployee);
        expected.addFrom(qcAddress);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);

        QueryField qf1 = new QueryField(qcEmployee, "name");
        cs1.addConstraint(new SimpleConstraint(qf1, SimpleConstraint.EQUALS, new QueryValue("Employee 1")));

        QueryField qf2 = new QueryField(qcEmployee, "age");
        cs1.addConstraint(new SimpleConstraint(qf2, SimpleConstraint.EQUALS, new QueryValue(new Integer(20))));

        QueryReference qr1 = new QueryObjectReference(qcEmployee, "address");
        cs1.addConstraint(new ContainsConstraint(qr1, ContainsConstraint.CONTAINS, qcAddress));

        QueryField qf3 = new QueryField(qcAddress, "address");
        cs1.addConstraint(new SimpleConstraint(qf3, SimpleConstraint.EQUALS, new QueryValue("1 The Street")));

        expected.setConstraint(cs1);

        assertEquals(expected, q);
    }

    public void testQueryForExampleObjectKeyAttributes2ReferencesToSameClass() throws Exception {
        // Contractor's key is "name", "personalAddress", "businessAddress" fields
        Address a1 = new Address();
        a1.setAddress("1 The Street");
        Address a2 = new Address();
        a2.setAddress("2 The Street");

        Contractor c = new Contractor();
        c.setPersonalAddress(a1);
        c.setBusinessAddress(a2);
        c.setName("Contractor 1");

        Query q = QueryHelper.createQueryForExampleObject(c, model);

        Query expected = new Query();
        QueryClass qcContractor = new QueryClass(Contractor.class);
        QueryClass qcAddress1 = new QueryClass(Address.class);
        QueryClass qcAddress2 = new QueryClass(Address.class);
        expected.addToSelect(qcContractor);
        expected.addFrom(qcContractor);
        expected.addFrom(qcAddress1);
        expected.addFrom(qcAddress2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);

        QueryField qf1 = new QueryField(qcContractor, "name");
        cs1.addConstraint(new SimpleConstraint(qf1, SimpleConstraint.EQUALS, new QueryValue("Contractor 1")));

        QueryReference qr1 = new QueryObjectReference(qcContractor, "personalAddress");
        cs1.addConstraint(new ContainsConstraint(qr1, ContainsConstraint.CONTAINS, qcAddress1));

        QueryField qf2 = new QueryField(qcAddress1, "address");
        cs1.addConstraint(new SimpleConstraint(qf2, SimpleConstraint.EQUALS, new QueryValue("1 The Street")));

        QueryReference qr2 = new QueryObjectReference(qcContractor, "businessAddress");
        cs1.addConstraint(new ContainsConstraint(qr2, ContainsConstraint.CONTAINS, qcAddress2));

        QueryField qf3 = new QueryField(qcAddress2, "address");
        cs1.addConstraint(new SimpleConstraint(qf3, SimpleConstraint.EQUALS, new QueryValue("2 The Street")));

        expected.setConstraint(cs1);

        assertEquals(expected, q);
    }

    public void testQueryForExampleObjectSubclass() throws Exception {
        // Employee's key is "name", "address", "fullTime" fields
        Address a = new Address();
        a.setAddress("1 The Street");

        CEO e = new CEO();
        e.setAddress(a);
        e.setName("Employee 1");
        e.setAge(20);
        e.setSalary(45000);

        Query q = QueryHelper.createQueryForExampleObject(e, model);

        Query expected = new Query();
        QueryClass qcEmployee = new QueryClass(CEO.class);
        QueryClass qcAddress = new QueryClass(Address.class);
        expected.addToSelect(qcEmployee);
        expected.addFrom(qcEmployee);
        expected.addFrom(qcAddress);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);

        QueryField qf1 = new QueryField(qcEmployee, "name");
        cs1.addConstraint(new SimpleConstraint(qf1, SimpleConstraint.EQUALS, new QueryValue("Employee 1")));

        QueryField qf2 = new QueryField(qcEmployee, "age");
        cs1.addConstraint(new SimpleConstraint(qf2, SimpleConstraint.EQUALS, new QueryValue(new Integer(20))));

        QueryReference qr1 = new QueryObjectReference(qcEmployee, "address");
        cs1.addConstraint(new ContainsConstraint(qr1, ContainsConstraint.CONTAINS, qcAddress));

        QueryField qf3 = new QueryField(qcAddress, "address");
        cs1.addConstraint(new SimpleConstraint(qf3, SimpleConstraint.EQUALS, new QueryValue("1 The Street")));

        expected.setConstraint(cs1);

        assertEquals(expected, q);
    }

    public void testQueryForExampleObjectAttributeMissing() throws Exception {
        // Employee's key is "name", "address", "fullTime" fields

        Address a = new Address();
        a.setAddress("1 The Street");

        Employee employee = new Employee();
        employee.setAddress(a);
        employee.setAge(20);

        // Name not set

        try {
            Query q = QueryHelper.createQueryForExampleObject(employee, model);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testQueryForExampleObjectReferenceMissing() throws Exception {
        // Employee's key is "name", "address", "fullTime" fields

        Employee employee = new Employee();
        employee.setName("Employee 1");
        employee.setAge(20);

        // Address not set

        try {
            Query q = QueryHelper.createQueryForExampleObject(employee, model);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

}
