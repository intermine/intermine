package org.flymine.objectstore.query;

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

}

