package org.flymine.objectstore.query;

import junit.framework.TestCase;

import org.flymine.model.testmodel.Department;


public class ContainsConstraintTest extends TestCase {

    private ContainsConstraint constraint;
    private QueryCollection collection;
    private QueryClass cls;

    public ContainsConstraintTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        cls = new QueryClass(Department.class);
        collection = new QueryCollection(cls, "employees");
    }

    public void testNullConstructor1() throws Exception {
        try {
            constraint = new ContainsConstraint(null, ContainsConstraint.CONTAINS, cls);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testNullConstructor2() throws Exception {
        try {
            constraint = new ContainsConstraint(collection, ContainsConstraint.CONTAINS, null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testInvalidType() {
        try {
            constraint = new ContainsConstraint(collection, 234, cls);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

}

