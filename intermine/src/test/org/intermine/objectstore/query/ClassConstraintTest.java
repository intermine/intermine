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

import junit.framework.TestCase;
import org.flymine.model.testmodel.Company;
import org.flymine.model.testmodel.Manager;
import org.flymine.model.testmodel.Employee;

public class ClassConstraintTest extends TestCase {

    private ClassConstraint constraint;
    private QueryClass company1;
    private QueryClass company2;
    private QueryClass employee;
    private QueryClass manager;
    private Object company1Object;
    private Object company2Object;
    private Object employeeObject;
    private Object managerObject;

    public ClassConstraintTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        company1 = new QueryClass(Company.class);
        company2 = new QueryClass(Company.class);
        employee = new QueryClass(Employee.class);
        manager = new QueryClass(Manager.class);
        company1Object = new Company();
        managerObject = new Manager();
    }

    public void testInvalidTypeQCQC() throws Exception{
        try {
            constraint = new ClassConstraint(company1, 234, company2);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testValidTypeQCQC() throws Exception {
        constraint = new ClassConstraint(company1, ClassConstraint.EQUALS, company2);
        assertEquals(ClassConstraint.EQUALS, constraint.getType());
    }

    public void testNullConstructor1QCQC() throws Exception {
        try {
            constraint = new ClassConstraint(null, ClassConstraint.EQUALS, company2);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testNullConstructor2QCQC() throws Exception {
        try {
            constraint = new ClassConstraint(company1, ClassConstraint.EQUALS, (QueryClass) null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }


    public void testInvalidClassTypesQCQC() throws Exception {
        try {
            constraint = new ClassConstraint(employee, ClassConstraint.EQUALS, company1);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testValidClassTypesSubclassQCQC() throws Exception {
        try {
            constraint = new ClassConstraint(employee, ClassConstraint.EQUALS, manager);
        }
        catch (IllegalArgumentException e) {
            fail("IllegalArgumentException should not have been thrown");
        }
    }

    public void testInvalidTypeQCObj() throws Exception{
        try {
            constraint = new ClassConstraint(company1, 234, company1Object);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testValidTypeQCObj() throws Exception {
        constraint = new ClassConstraint(company1, ClassConstraint.EQUALS, company1Object);
        assertEquals(ClassConstraint.EQUALS, constraint.getType());
    }

    public void testNullConstructor1QCObj() throws Exception {
        try {
            constraint = new ClassConstraint(null, ClassConstraint.EQUALS, company1Object);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testNullConstructor2QCObj() throws Exception {
        try {
            constraint = new ClassConstraint(company1, ClassConstraint.EQUALS, (Object) null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }


    public void testInvalidClassTypesQCObj() throws Exception {
        try {
            constraint = new ClassConstraint(employee, ClassConstraint.EQUALS, company1Object);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testValidClassTypesSubclassQCObj() throws Exception {
        try {
            constraint = new ClassConstraint(employee, ClassConstraint.EQUALS, managerObject);
        }
        catch (IllegalArgumentException e) {
            fail("IllegalArgumentException should not have been thrown");
        }
    }

    public void testEqualsHashCode() throws Exception {
        Constraint c1 = new ClassConstraint(company1, ClassConstraint.EQUALS, company1);
        Constraint c2 = new ClassConstraint(company1, ClassConstraint.EQUALS, company1);
        Constraint c3 = new ClassConstraint(company1, ClassConstraint.EQUALS, company2);
        Constraint c4 = new ClassConstraint(company1, ClassConstraint.EQUALS, company1Object);
        Constraint c5 = new ClassConstraint(company1, ClassConstraint.EQUALS, company1Object);
        Constraint c6 = new ClassConstraint(company1, ClassConstraint.NOT_EQUALS, company1Object);
        Constraint c7 = new ClassConstraint(company1, ClassConstraint.NOT_EQUALS, company1Object, true);

        assertEquals(c1, c1);
        assertEquals(c1, c2);
        assertTrue(!c1.equals(c3));
        assertTrue(!c1.equals(c4));
        assertTrue(!c1.equals(c6));

        assertTrue(!c4.equals(c1));
        assertEquals(c4, c4);
        assertEquals(c4, c5);
        assertTrue(!c4.equals(c6));

        assertTrue(!c6.equals(c7));

        assertEquals(c1.hashCode(), c1.hashCode());
        assertEquals(c1.hashCode(), c2.hashCode());
        assertTrue(c1.hashCode() != c3.hashCode());
        assertTrue(c1.hashCode() != c4.hashCode());
        assertTrue(c1.hashCode() != c6.hashCode());

        assertEquals(c4.hashCode(), c4.hashCode());
        assertEquals(c4.hashCode(), c5.hashCode());
        assertTrue(c4.hashCode() != c6.hashCode());

        assertTrue(c6.hashCode() != c7.hashCode());
    }


}

