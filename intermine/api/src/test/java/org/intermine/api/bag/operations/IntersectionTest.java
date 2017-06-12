package org.intermine.api.bag.operations;

import java.util.Arrays;

import org.intermine.api.profile.InterMineBag;;

public class IntersectionTest extends AbstractBagOperationTestCase {

    public IntersectionTest(String name) {
        super(name);
    }

    public void testIntersectionOfSameType() throws Exception {
        BagOperation operation = new Intersection(im.getModel(), testUser, Arrays.asList(bagA, bagB));
        InterMineBag intersection = operation.operate();
        assertEquals("Employee", intersection.getType());
        assertEquals(1, intersection.getSize());
    }

    public void testIntersectionOfTypeAndSubType() throws Exception {
        BagOperation operation = new Intersection(im.getModel(), testUser,
                Arrays.asList(bagA, bagD));
        InterMineBag intersection = operation.operate();
        assertEquals("CEO", intersection.getType());
        assertEquals(4, intersection.getSize());
    }

    public void testIntersectionOfLineage() throws Exception {
        BagOperation operation = new Intersection(im.getModel(), testUser,
                Arrays.asList(bagF, bagA, bagC));
        InterMineBag intersection = operation.operate();

        // ManagerB, managerH, and CEOH
        assertEquals("Manager", intersection.getType());
        assertEquals(3, intersection.getSize());
    }

    public void testIncompatibleBags() throws Exception {
        BagOperation operation = new Intersection(im.getModel(), testUser, Arrays.asList(bagA, bagE));
        try {
            operation.operate();
            fail("Expected an exception");
        } catch (IncompatibleTypes e) {
            // Expected behaviour.
        }
    }

}
