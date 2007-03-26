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

import java.util.Collections;
import junit.framework.TestCase;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Contractor;
import org.intermine.model.testmodel.Manager;
import org.intermine.model.testmodel.Employee;
import org.intermine.util.DynamicUtil;

public class ClassConstraintTest extends TestCase {

    private ClassConstraint constraint;
    private QueryClass company1;
    private QueryClass company2;
    private QueryClass employee;
    private QueryClass manager;
    private QueryClass contractor;
    private Company company1Object;
    private Manager managerObject;

    public ClassConstraintTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        company1 = new QueryClass(Company.class);
        company2 = new QueryClass(Company.class);
        employee = new QueryClass(Employee.class);
        manager = new QueryClass(Manager.class);
        contractor = new QueryClass(Contractor.class);
        company1Object = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company1Object.setId(new Integer(8762134));
        managerObject = new Manager();
        managerObject.setId(new Integer(2687634));
    }

    public void testInvalidTypeQCQC() throws Exception{
        try {
            new ClassConstraint(company1, ConstraintOp.CONTAINS, company2);
            fail("An IllegalArgumentException  should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testValidTypeQCQC() throws Exception {
        constraint = new ClassConstraint(company1, ConstraintOp.EQUALS, company2);
        assertEquals(ConstraintOp.EQUALS, constraint.getOp());
    }

    public void testNullConstructor1QCQC() throws Exception {
        try {
            new ClassConstraint(null, ConstraintOp.EQUALS, company2);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testNullConstructor2QCQC() throws Exception {
        try {
            new ClassConstraint(company1, null, company2);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testNullConstructor3QCQC() throws Exception {
        try {
            new ClassConstraint(company1, ConstraintOp.EQUALS, (QueryClass) null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testInvalidClassTypesQCQC() throws Exception {
        try {
            new ClassConstraint(employee, ConstraintOp.EQUALS, contractor);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testValidClassTypesDynamicQCQC() throws Exception {
        try {
            new ClassConstraint(employee, ConstraintOp.EQUALS, company1);
        } catch (IllegalArgumentException e) {
            fail("IllegalArgumentException should not have been thrown");
        }
    }

    public void testValidClassTypesSubclassQCQC() throws Exception {
        try {
            new ClassConstraint(employee, ConstraintOp.EQUALS, manager);
        }
        catch (IllegalArgumentException e) {
            fail("IllegalArgumentException should not have been thrown");
        }
    }

    public void testInvalidTypeQCObj() throws Exception{
        try {
            constraint = new ClassConstraint(company1, null, company1Object);
            fail("An NullPointerException should have been thrown");
        } catch (NullPointerException e) {
        }
    }

    public void testValidTypeQCObj() throws Exception {
        constraint = new ClassConstraint(company1, ConstraintOp.EQUALS, company1Object);
        assertEquals(ConstraintOp.EQUALS, constraint.getOp());
    }

    public void testNullConstructor1QCObj() throws Exception {
        try {
            constraint = new ClassConstraint(null, ConstraintOp.EQUALS, company1Object);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testNullConstructor2QCObj() throws Exception {
        try {
            constraint = new ClassConstraint(company1, ConstraintOp.EQUALS, (InterMineObject) null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testInvalidClassTypesQCObj() throws Exception {
        try {
            constraint = new ClassConstraint(employee, ConstraintOp.EQUALS, company1Object);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testValidClassTypesSubclassQCObj() throws Exception {
        try {
            constraint = new ClassConstraint(employee, ConstraintOp.EQUALS, managerObject);
        }
        catch (IllegalArgumentException e) {
            fail("IllegalArgumentException should not have been thrown");
        }
    }

    public void testEqualsHashCode() throws Exception {
        Constraint c1 = new ClassConstraint(company1, ConstraintOp.EQUALS, company1);
        Constraint c2 = new ClassConstraint(company1, ConstraintOp.EQUALS, company1);
        Constraint c3 = new ClassConstraint(company1, ConstraintOp.EQUALS, company2);
        Constraint c4 = new ClassConstraint(company1, ConstraintOp.EQUALS, company1Object);
        Constraint c5 = new ClassConstraint(company1, ConstraintOp.EQUALS, company1Object);
        Constraint c6 = new ClassConstraint(company1, ConstraintOp.NOT_EQUALS, company1Object);

        assertEquals(c1, c1);
        assertEquals(c1, c2);
        assertTrue(!c1.equals(c3));
        assertTrue(!c1.equals(c4));
        assertTrue(!c1.equals(c6));

        assertTrue(!c4.equals(c1));
        assertEquals(c4, c4);
        assertEquals(c4, c5);
        assertTrue(!c4.equals(c6));

        assertEquals(c1.hashCode(), c1.hashCode());
        assertEquals(c1.hashCode(), c2.hashCode());
        assertTrue(c1.hashCode() != c3.hashCode());
        assertTrue(c1.hashCode() != c4.hashCode());
        assertTrue(c1.hashCode() != c6.hashCode());

        assertEquals(c4.hashCode(), c4.hashCode());
        assertEquals(c4.hashCode(), c5.hashCode());
        assertTrue(c4.hashCode() != c6.hashCode());
    }
}

