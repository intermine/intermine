package org.intermine.api.bag.operations;

import java.util.Arrays;

import org.intermine.api.bag.operations.BagOperation;
import org.intermine.api.bag.operations.IncompatibleTypes;
import org.intermine.api.bag.operations.Union;
import org.intermine.api.profile.InterMineBag;

public class UnionTest extends AbstractBagOperationTestCase {

    public UnionTest(String arg) {
        super(arg);
    }

    public void testTwoOfSameType_NoOverlap() throws Exception {
        BagOperation operation = new Union(os.getModel(), testUser, Arrays.asList(bagA, bagB));
        InterMineBag union = operation.operate();
        assertEquals("Employee", union.getType());
        assertEquals(19, union.getSize());
    }

    public void testUniversalUnion() throws Exception {
        BagOperation operation = new Union(os.getModel(), testUser, Arrays.asList(bagA, bagB, bagC, bagD, bagE, bagF));
        InterMineBag union = operation.operate();
        assertEquals("Employable", union.getType());
        assertEquals(50, union.getSize());
    }

    public void testDisjointUnion() throws Exception {
        BagOperation operation = new Union(os.getModel(), testUser, Arrays.asList(bagD, bagE));
        InterMineBag union = operation.operate();
        assertEquals("Employable", union.getType());
        assertEquals(20, union.getSize());
    }

    public void testLineage() throws Exception {
        BagOperation operation = new Union(os.getModel(), testUser, Arrays.asList(bagB, bagC, bagD));
        InterMineBag union = operation.operate();
        assertEquals("Employee", union.getType());
        assertEquals(25, union.getSize());
    }

    public void testSingleIsCopy() throws Exception {
        BagOperation operation = new Union(os.getModel(), testUser, Arrays.asList(bagE));
        InterMineBag union = operation.operate();
        assertEquals("Contractor", union.getType());
        assertEquals(10, union.getSize());
    }

    public void testIncompatibleTypes() throws Exception {
        BagOperation operation = new Union(os.getModel(), testUser, Arrays.asList(bagA, bagG));
        try {
            InterMineBag bag = operation.operate();
            fail("Expected an exception. Got " + bag.toString());
        } catch (IncompatibleTypes e) {
            // Expected behaviour.
        }
    }
}
