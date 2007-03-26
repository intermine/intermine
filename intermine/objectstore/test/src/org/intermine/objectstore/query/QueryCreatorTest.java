package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.Test;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.testing.OneTimeTestCase;

public class QueryCreatorTest extends QueryTestCase
{
    Model model;

    public QueryCreatorTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        super.setUp();
        model = Model.getInstanceByName("testmodel");
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(QueryCreatorTest.class);
    }

    public void testCreateQueryForId() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new SimpleConstraint(new QueryField(qc, "id"), ConstraintOp.EQUALS,
                    new QueryValue(new Integer(5))));

        assertEquals(q, QueryCreator.createQueryForId(new Integer(5), InterMineObject.class));
    }


    /*
    public void testQueryForExampleObjectNullObject() throws Exception {
        try {
            QueryCreator.createQueryForExampleObject(null, model);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    
    public void testQueryForExampleObjectKeyAttributes() throws Exception {
        // Address's key is "address" field
        Address a = new Address();
        a.setAddress("1 The Street");
        Query q = QueryCreator.createQueryForExampleObject(a, model);

        Query expected = new Query();
        QueryClass qc = new QueryClass(Address.class);
        expected.addToSelect(qc);
        expected.addFrom(qc);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        QueryField qf = new QueryField(qc, "address");
        cs1.addConstraint(new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue("1 The Street")));
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

        Query q = QueryCreator.createQueryForExampleObject(e, model);

        Query expected = new Query();
        QueryClass qcEmployee = new QueryClass(Employee.class);
        QueryClass qcAddress = new QueryClass(Address.class);
        expected.addToSelect(qcEmployee);
        expected.addFrom(qcEmployee);
        expected.addFrom(qcAddress);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);

        QueryField qf1 = new QueryField(qcEmployee, "name");
        cs1.addConstraint(new SimpleConstraint(qf1, ConstraintOp.EQUALS, new QueryValue("Employee 1")));

        QueryField qf2 = new QueryField(qcEmployee, "age");
        cs1.addConstraint(new SimpleConstraint(qf2, ConstraintOp.EQUALS, new QueryValue(new Integer(20))));

        QueryReference qr1 = new QueryObjectReference(qcEmployee, "address");
        cs1.addConstraint(new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcAddress));

        QueryField qf3 = new QueryField(qcAddress, "address");
        cs1.addConstraint(new SimpleConstraint(qf3, ConstraintOp.EQUALS, new QueryValue("1 The Street")));

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

        Query q = QueryCreator.createQueryForExampleObject(c, model);

        Query expected = new Query();
        QueryClass qcContractor = new QueryClass(Contractor.class);
        QueryClass qcAddress1 = new QueryClass(Address.class);
        QueryClass qcAddress2 = new QueryClass(Address.class);
        expected.addToSelect(qcContractor);
        expected.addFrom(qcContractor);
        expected.addFrom(qcAddress1);
        expected.addFrom(qcAddress2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);

        QueryField qf1 = new QueryField(qcContractor, "name");
        cs1.addConstraint(new SimpleConstraint(qf1, ConstraintOp.EQUALS, new QueryValue("Contractor 1")));

        QueryReference qr1 = new QueryObjectReference(qcContractor, "personalAddress");
        cs1.addConstraint(new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcAddress1));

        QueryField qf2 = new QueryField(qcAddress1, "address");
        cs1.addConstraint(new SimpleConstraint(qf2, ConstraintOp.EQUALS, new QueryValue("1 The Street")));

        QueryReference qr2 = new QueryObjectReference(qcContractor, "businessAddress");
        cs1.addConstraint(new ContainsConstraint(qr2, ConstraintOp.CONTAINS, qcAddress2));

        QueryField qf3 = new QueryField(qcAddress2, "address");
        cs1.addConstraint(new SimpleConstraint(qf3, ConstraintOp.EQUALS, new QueryValue("2 The Street")));

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

        Query q = QueryCreator.createQueryForExampleObject(e, model);

        Query expected = new Query();
        QueryClass qcEmployee = new QueryClass(CEO.class);
        QueryClass qcAddress = new QueryClass(Address.class);
        expected.addToSelect(qcEmployee);
        expected.addFrom(qcEmployee);
        expected.addFrom(qcAddress);
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);

        QueryField qf1 = new QueryField(qcEmployee, "name");
        cs1.addConstraint(new SimpleConstraint(qf1, ConstraintOp.EQUALS, new QueryValue("Employee 1")));

        QueryField qf2 = new QueryField(qcEmployee, "age");
        cs1.addConstraint(new SimpleConstraint(qf2, ConstraintOp.EQUALS, new QueryValue(new Integer(20))));

        QueryReference qr1 = new QueryObjectReference(qcEmployee, "address");
        cs1.addConstraint(new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcAddress));

        QueryField qf3 = new QueryField(qcAddress, "address");
        cs1.addConstraint(new SimpleConstraint(qf3, ConstraintOp.EQUALS, new QueryValue("1 The Street")));

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
            Query q = QueryCreator.createQueryForExampleObject(employee, model);
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
            Query q = QueryCreator.createQueryForExampleObject(employee, model);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }
*/

    public void testCreateQueryForQueryNodeValues1() throws Exception {
        QueryClass qcCompany = new QueryClass(Company.class);
        QueryClass qcDepartment = new QueryClass(Department.class);

        QueryField qf1 = new QueryField(qcCompany, "name");
        QueryReference qr1 = new QueryCollectionReference(qcCompany, "departments");

        Constraint c1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcDepartment);
        Constraint c2 = new SimpleConstraint(qf1, ConstraintOp.EQUALS, new QueryValue("Company1"));

        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        cs1.addConstraint(c1);
        cs1.addConstraint(c2);

        Query orig = new Query();
        orig.addFrom(qcCompany);
        orig.addFrom(qcDepartment);
        orig.addToSelect(qcCompany);
        orig.addToSelect(qcDepartment);
        orig.setConstraint(cs1);
        orig.setDistinct(false);
        orig.addToOrderBy(qcCompany);

        QueryClass qcCompanyExpected = new QueryClass(Company.class);
        QueryClass qcDepartmentExpected = new QueryClass(Department.class);

        QueryField qf1Expected = new QueryField(qcCompanyExpected, "name");
        QueryReference qr1Expected = new QueryCollectionReference(qcCompanyExpected, "departments");

        Constraint c1Expected = new ContainsConstraint(qr1Expected, ConstraintOp.CONTAINS, qcDepartmentExpected);
        Constraint c2Expected = new SimpleConstraint(qf1Expected, ConstraintOp.EQUALS, new QueryValue("Company1"));

        ConstraintSet cs1Expected = new ConstraintSet(ConstraintOp.AND);
        cs1Expected.addConstraint(c1Expected);
        cs1Expected.addConstraint(c2Expected);

        QueryField qfWantedExpected = new QueryField(qcDepartmentExpected, "name");

        Query expected = new Query();

        expected.addFrom(qcCompanyExpected);
        expected.addFrom(qcDepartmentExpected);
        expected.addToSelect(qfWantedExpected);
        expected.setConstraint(cs1Expected);
        expected.setDistinct(true);
        expected.addToOrderBy(qfWantedExpected);

        QueryField qfWanted = new QueryField(qcDepartment, "name");
        Query ret = QueryCreator.createQueryForQueryNodeValues(orig, qfWanted);

        // Make sure it has been cloned
        assertTrue(ret != orig);
        // Test that the expected query is produced
        assertEquals("Wanted " + expected.toString() + ", got " + ret.toString(), expected, ret);


    }

    public void testCreateQueryForQueryNodeValues2() throws Exception {
        QueryClass qcCompany = new QueryClass(Company.class);
        QueryClass qcDepartment = new QueryClass(Department.class);

        QueryField qf1 = new QueryField(qcCompany, "name");
        QueryReference qr1 = new QueryCollectionReference(qcCompany, "departments");

        Constraint c1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, qcDepartment);
        Constraint c2 = new SimpleConstraint(qf1, ConstraintOp.EQUALS, new QueryValue("Company1"));

        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        cs1.addConstraint(c1);
        cs1.addConstraint(c2);

        Query orig = new Query();
        orig.addFrom(qcCompany);
        orig.addFrom(qcDepartment);
        orig.addToSelect(qcCompany);
        orig.addToSelect(qcDepartment);
        orig.setConstraint(cs1);
        orig.setDistinct(false);
        orig.addToOrderBy(qcCompany);

        QueryClass qcCompanyExpected = new QueryClass(Company.class);
        QueryClass qcDepartmentExpected = new QueryClass(Department.class);

        QueryField qf1Expected = new QueryField(qcCompanyExpected, "name");
        QueryReference qr1Expected = new QueryCollectionReference(qcCompanyExpected, "departments");

        Constraint c1Expected = new ContainsConstraint(qr1Expected, ConstraintOp.CONTAINS, qcDepartmentExpected);
        Constraint c2Expected = new SimpleConstraint(qf1Expected, ConstraintOp.EQUALS, new QueryValue("Company1"));

        ConstraintSet cs1Expected = new ConstraintSet(ConstraintOp.AND);
        cs1Expected.addConstraint(c1Expected);
        cs1Expected.addConstraint(c2Expected);

        QueryClass qcWantedExpected = qcDepartmentExpected;

        Query expected = new Query();

        expected.addFrom(qcCompanyExpected);
        expected.addFrom(qcDepartmentExpected);
        expected.addToSelect(qcWantedExpected);
        expected.setConstraint(cs1Expected);
        expected.setDistinct(true);
        expected.addToOrderBy(qcWantedExpected);

        QueryClass qcWanted = qcDepartment;
        Query ret = QueryCreator.createQueryForQueryNodeValues(orig, qcWanted);

        // Make sure it has been cloned
        assertTrue(ret != orig);
        // Test that the expected query is produced
        assertEquals("Wanted " + expected.toString() + ", got " + ret.toString(), expected, ret);
    }
}
