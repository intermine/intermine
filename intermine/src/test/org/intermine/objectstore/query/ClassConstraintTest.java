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

    public ClassConstraintTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        company1 = new QueryClass(Company.class);
        company2 = new QueryClass(Company.class);
        employee = new QueryClass(Employee.class);
        manager = new QueryClass(Manager.class);
    }

    public void testInvalidType() throws Exception{
        try {
            constraint = new ClassConstraint(company1, 234, company2);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testValidType() throws Exception {
        constraint = new ClassConstraint(company1, ClassConstraint.EQUALS, company2);
        assertEquals(ClassConstraint.EQUALS, constraint.getType());
    }

    public void testNullConstructor1() throws Exception {
        try {
            constraint = new ClassConstraint(null, ClassConstraint.EQUALS, company2);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testNullConstructor2() throws Exception {
        try {
            constraint = new ClassConstraint(company1, ClassConstraint.EQUALS, null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }


    public void testInvalidClassTypes() throws Exception {
        try {
            constraint = new ClassConstraint(employee, ClassConstraint.EQUALS, company1);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testValidClassTypesSubclass() throws Exception {
        try {
            constraint = new ClassConstraint(employee, ClassConstraint.EQUALS, manager);
        }
        catch (IllegalArgumentException e) {
            fail("IllegalArgumentException should not have been thrown");
        }
    }

}

