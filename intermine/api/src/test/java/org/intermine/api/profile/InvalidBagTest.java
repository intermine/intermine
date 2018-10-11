package org.intermine.api.profile;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.api.InterMineAPITestCase;
import org.intermine.api.bag.UnknownBagTypeException;
import org.intermine.objectstore.ObjectStoreException;

public class InvalidBagTest extends InterMineAPITestCase {

    private Profile underTest;
    private List<BagValue> values;

    private static final String NAME = "deadBag";
    private static final String API_KEY = "abcdef012345";
    private static final String PASSWORD = "12345";
    private static final Integer USER_ID = null;
    private static final String DESCRIPTION = "Places were stuff is actually made";
    private static final String TYPE = "ManufacturingPlant";
    private static final Date CREATED_AT = new Date(123456789l);


    public InvalidBagTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();

        Map<String, InvalidBag> invalidBags = new HashMap<String, InvalidBag>();
        Map<String, InterMineBag> validBags = new HashMap<String, InterMineBag>();

        InvalidBag bag = new InvalidBag(NAME, TYPE, DESCRIPTION, CREATED_AT, os, uosw);
        invalidBags.put(NAME, bag);

        ProfileManager pm = im.getProfileManager();

        Integer userId = null;
        underTest = new Profile(pm, "UNDER_TEST", userId, PASSWORD,
                               new HashMap(), new BagSet(validBags, invalidBags),
                               new HashMap(), API_KEY, true, false);
        pm.createProfile(underTest);

        values = new LinkedList<BagValue>();
        for (String p: new String[] {"Tires", "Ceramics", "Sugar", "Cotton", "Steel"}) {
            values.add(new BagValue(p, null));
        }
        bag.saveWithBagValues(underTest.getUserId(), values);
    }

    public void testGetInvalidBag() {
        StorableBag bag = underTest.getAllBags().get(NAME);

        assertEquals(NAME, bag.getName());
        assertEquals(BagState.NOT_CURRENT.toString(), bag.getState());
        assertEquals(TYPE, bag.getType());
    }

    public void testSetBagValues() throws ObjectStoreException {
        StorableBag bag = underTest.getAllBags().get(NAME);

        assertEquals(values.size(), bag.getSize());
        assertTrue("The bag values should be the same as the ones we put in",
                values.containsAll(bag.getContents()));
    }

    public void testAmending() throws ObjectStoreException, UnknownBagTypeException {
        assertTrue(underTest.getInvalidBags().containsKey(NAME));
        assertFalse(underTest.getSavedBags().containsKey(NAME));
        underTest.fixInvalidBag(NAME, "Employee");
        assertFalse(underTest.getInvalidBags().containsKey(NAME));
        assertTrue(underTest.getSavedBags().containsKey(NAME));
        assertEquals(0, underTest.getSavedBags().get(NAME).getSize());
    }

    public void testDelete() throws Exception {
        assertTrue(underTest.getAllBags().containsKey(NAME));
        underTest.deleteBag(NAME);
        assertFalse(underTest.getAllBags().containsKey(NAME));
    }

}
