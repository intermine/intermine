package org.intermine.api.bag.operations;

import java.util.Arrays;

import org.intermine.api.bag.operations.BagOperation;
import org.intermine.api.bag.operations.RelativeComplement;
import org.intermine.api.profile.InterMineBag;
import org.intermine.model.testmodel.Company;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.util.DynamicUtil;

public class RelativeComplementTest extends AbstractBagOperationTestCase {

    public RelativeComplementTest(String arg) {
        super(arg);
    }

    InterMineBag hasAddresses;
    InterMineBag things;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Long start = System.currentTimeMillis();
        hasAddresses = testUser.createBag("has-addresses", "HasAddress", "Has-Addresses", im.getClassKeys());
        Company c = DynamicUtil.simpleCreateObject(Company.class);
        ObjectStoreWriter osw = os.getNewWriter();
        osw.store(c);
        osw.close();
        hasAddresses.addIdToBag(c.getId(), "Company");
        hasAddresses.addIdsToBag(managers, "Manager");
        System.out.printf("Finished extra set-up. Took %d ms.\n", System.currentTimeMillis() - start);
    }

    public void testManyFromOne() throws Exception {
        BagOperation operation = new RelativeComplement(os.getModel(), testUser,
                Arrays.asList(bagF),
                Arrays.asList(bagA, bagB, bagC, bagD, bagE));
        InterMineBag winnowed = operation.operate();
        assertEquals("Employable", winnowed.getType());
        assertEquals(10, winnowed.getSize());
    }
    
    public void testOneFromOne() throws Exception {
        BagOperation operation = new RelativeComplement(im.getModel(), testUser,
                Arrays.asList(bagA),
                Arrays.asList(bagC));
        operation.setClassKeys(im.getClassKeys());
        InterMineBag winnowed = operation.operate();
   
        assertEquals("Employee", winnowed.getType());
        assertEquals(8, winnowed.getSize());
    }

    public void testOneFromMany() throws Exception {
        BagOperation operation = new RelativeComplement(
            im.getModel(), testUser,
            Arrays.asList(bagA, bagE),
            Arrays.asList(bagC));
        operation.setClassKeys(im.getClassKeys());
        InterMineBag winnowed = operation.operate();
   
        assertEquals("Employable", winnowed.getType());
        assertEquals(18, winnowed.getSize());
    }

    public void testManyFromMany() throws Exception {
        BagOperation operation = new RelativeComplement(
            im.getModel(), testUser,
            Arrays.asList(bagC, bagF),
            Arrays.asList(bagE, bagD));
        InterMineBag winnowed = operation.operate();
        assertEquals("Employable", winnowed.getType());
        assertEquals(24, winnowed.getSize());
    }

    public void testRemovingMutuallyIncompatibleSuperTypes() throws Exception {
        BagOperation operation = new RelativeComplement(
            im.getModel(), testUser,
            Arrays.asList(bagA),
            Arrays.asList(bagF, hasAddresses));
        InterMineBag winnowed = operation.operate();
        assertEquals("Employee", winnowed.getType());
        assertEquals(6, winnowed.getSize());
    }

    public void testIncompatibleTypes() throws Exception {
        BagOperation operation = new RelativeComplement(im.getModel(), testUser,
                Arrays.asList(bagA, bagB, bagC, bagD),
                Arrays.asList(bagE));

        try {
            InterMineBag bag = operation.operate();
            fail("Expected an exception. Got " + bag);
        } catch (IncompatibleTypes e) {
            // Expected behaviour.
        }
    }


}
