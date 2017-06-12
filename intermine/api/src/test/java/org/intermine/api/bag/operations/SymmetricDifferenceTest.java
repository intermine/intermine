package org.intermine.api.bag.operations;

import java.util.Arrays;

import org.intermine.api.profile.InterMineBag;

public class SymmetricDifferenceTest extends AbstractBagOperationTestCase {

    public SymmetricDifferenceTest(String arg) {
        super(arg);
    }

    public void testTwoOfSameType() throws Exception {
        BagOperation operation = new SymmetricDifference(
            im.getModel(), testUser, Arrays.asList(bagA, bagB));
        InterMineBag diff = operation.operate();
        assertEquals("Employee", diff.getType());
        assertEquals(18, diff.getSize());
    }

    public void testAll() throws Exception {
        BagOperation operation = new SymmetricDifference(
                im.getModel(), testUser, Arrays.asList(bagA, bagB, bagC, bagD, bagE, bagF));
        InterMineBag diff = operation.operate();
        assertEquals("Employable", diff.getType());
        assertEquals(50, diff.getSize());
    }

    public void testMany() throws Exception {
        BagOperation operation = new SymmetricDifference(
                im.getModel(), testUser, Arrays.asList(bagA, bagC, bagD));
        InterMineBag diff = operation.operate();
        assertEquals("Employee", diff.getType());
        assertEquals(24, diff.getSize());
    }

    public void testIncompatibleTypes() throws Exception {
        BagOperation operation = new SymmetricDifference(
                im.getModel(), testUser, Arrays.asList(bagA, bagC, bagD, bagG));
        try {
            InterMineBag diff = operation.operate();
            fail("Expected exception. Got " + diff);
        } catch (IncompatibleTypes e) {
            // Expected behaviour.
        }
    }
}
