package org.flymine.objectstore.query;

import junit.framework.TestCase;

import org.flymine.model.testmodel.Department;
import org.flymine.model.testmodel.Manager;
import org.flymine.model.testmodel.Employee;

public class ContainsConstraintTest extends TestCase {

    private ContainsConstraint constraint;
    private QueryCollectionReference collRef;
    private QueryObjectReference objRef;
    private QueryClass qc1, qc2, qc3;

    public ContainsConstraintTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        qc1 = new QueryClass(Department.class);
        qc2 = new QueryClass(Manager.class);
        qc3 = new QueryClass(Employee.class);
        collRef = new QueryCollectionReference(qc1, "employees");
        objRef = new QueryObjectReference(qc1, "manager");
    }

    public void testNullConstructor1() throws Exception {
        try {
            constraint = new ContainsConstraint(null, ContainsConstraint.CONTAINS, qc3);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testNullConstructor2() throws Exception {
        try {
            constraint = new ContainsConstraint(collRef, ContainsConstraint.CONTAINS, null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testInvalidType() {
        try {
            constraint = new ContainsConstraint(collRef, 234, qc3);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testIncompatibleTypesReference() throws Exception {
        try {
            // objRef has type Manager, qc3 is type Employee
            constraint = new ContainsConstraint(objRef, 1, qc3);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

}

