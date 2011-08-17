package org.intermine.api.bag;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.api.InterMineAPITestCase;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.BagState;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.testmodel.Address;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.util.DynamicUtil;

public class BagManagerTest extends InterMineAPITestCase
{

    //private Profile superUser, testUser, emptyUser;
    private Profile superUser, emptyUser;
    private BagManager bagManager;
    private TagManager tagManager;
    private InterMineBag globalCompanyBag, globalAddressBag, superPrivateBag, userCompanyBag, userAddressBag;
    private Integer ADDRESS_ID = 1;
    private Integer DUMMY_ID = 2;

    public BagManagerTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        superUser = im.getProfileManager().getSuperuserProfile();
        emptyUser = im.getProfileManager().getProfile("emptyUser");
        tagManager = im.getProfileManager().getTagManager();
        bagManager = im.getBagManager();
        setUpBagsAndTags();
    }

    private void setUpBagsAndTags() throws Exception {
        Map<String, List<FieldDescriptor>>  classKeys = im.getClassKeys();

        globalCompanyBag = superUser.createBag("companyBag", "Company", "", classKeys);
        globalAddressBag = superUser.createBag("globalAddressBag", "Address", "", classKeys);
        superPrivateBag = superUser.createBag("superPrivateBag", "Company", "", classKeys);

        if (tagManager == null) {
            throw new NullPointerException("oops");
        }

        tagManager.addTag(TagNames.IM_PUBLIC, "companyBag", TagTypes.BAG, "superUser");
        tagManager.addTag(TagNames.IM_PUBLIC, "globalAddressBag", TagTypes.BAG, "superUser");
        tagManager.addTag(TagNames.IM_FAVOURITE, "companyBag", TagTypes.BAG, "superUser");
        tagManager.addTag(TagNames.IM_FAVOURITE, "superPrivateBag", TagTypes.BAG, "superUser");

        userCompanyBag = testUser.createBag("companyBag", "Company", "", classKeys);
        userAddressBag = testUser.createBag("userAddressBag", "Address", "", classKeys);
    }


    public void testGetBagsWithTag() throws Exception {
        Map<String, InterMineBag> expected = createExpected(globalCompanyBag, globalAddressBag);
        assertEquals(expected, bagManager.getBagsWithTag(superUser, TagNames.IM_PUBLIC));

        expected = createExpected(globalCompanyBag, superPrivateBag);
        assertEquals(expected, bagManager.getBagsWithTag(superUser, TagNames.IM_FAVOURITE));

        expected = Collections.EMPTY_MAP;
        assertEquals(expected, bagManager.getBagsWithTag(superUser, TagNames.IM_HIDDEN));
    }


    public void testGetGlobalBags() throws Exception {
        Map<String, InterMineBag> expected = createExpected(globalCompanyBag, globalAddressBag);
        assertEquals(expected, bagManager.getGlobalBags());
    }

    public void testGetUserBags() throws Exception {
        Map<String, InterMineBag> expected = createExpected(userCompanyBag, userAddressBag);
        assertEquals(expected, bagManager.getUserBags(testUser));
    }

    // user bags with same name as global bag take precedence
    public void testGetUserAndGlobalBags() throws Exception {
        Map<String, InterMineBag> expected = createExpected(globalAddressBag, userCompanyBag, userAddressBag);
        assertEquals(expected, bagManager.getUserAndGlobalBags(testUser));
    }

    public void testGetUserAndGlobalBagsNoUserBags() throws Exception {
        Map<String, InterMineBag> expected = createExpected(globalCompanyBag, globalAddressBag);
        assertEquals(expected, bagManager.getUserAndGlobalBags(emptyUser));
    }

    public void testGetGlobalBag() throws Exception {
        String testBagName = globalCompanyBag.getName();
        assertEquals(globalCompanyBag, bagManager.getGlobalBag(testBagName));
    }

    public void testGetGlobalBagNoBagWithName()  throws Exception {
        assertNull(bagManager.getGlobalBag("dummyName"));
    }

    public void testGetUserBag() throws Exception {
        String bagName = userCompanyBag.getName();
        assertEquals(userCompanyBag, bagManager.getUserBag(testUser, bagName));
    }

    public void testGetUserBagNoBagWithName()  throws Exception {
        assertNull(bagManager.getUserBag(testUser, "dummyName"));
    }

    public void testGetUserOrGlobalBagUser() throws Exception {
        String bagName = userCompanyBag.getName();
        assertEquals(userCompanyBag, bagManager.getUserOrGlobalBag(testUser, bagName));
    }

    public void testGetUserOrGlobalBagGlobal() throws Exception {
        String bagName = globalAddressBag.getName();
        assertEquals(globalAddressBag, bagManager.getUserOrGlobalBag(testUser, bagName));
    }

    public void testGetUserOrGlobalBagGlobalNoBagWithName() throws Exception {
        assertNull(bagManager.getUserOrGlobalBag(testUser, "dummyName"));
    }

    // user bags with same name as global bag take precedence
    public void testGetUserOrGlobalBagGlobalUserPrecedence() throws Exception {
        String bagName = userCompanyBag.getName();
        assertEquals(userCompanyBag, bagManager.getUserOrGlobalBag(testUser, bagName));
    }

    public void testGetCurrentUserOrGlobalBagsOfType() throws Exception {
        Map<String, InterMineBag> expected = createExpected(globalAddressBag, userAddressBag);
        assertEquals(expected, bagManager.getCurrentUserOrGlobalBagsOfType(testUser, "Address"));
        globalAddressBag.setState(BagState.NOT_CURRENT);
        expected = createExpectedCurrent(globalAddressBag, userAddressBag);
        assertEquals(expected, bagManager.getCurrentUserOrGlobalBagsOfType(testUser, "Address"));
    }

    public void testGetUserOrGlobalBagsOfType() throws Exception {
        Map<String, InterMineBag> expected = createExpected(globalAddressBag, userAddressBag);
        assertEquals(expected, bagManager.getUserOrGlobalBagsOfType(testUser, "Address"));
    }

    public void testGetUserBagsOfType() throws Exception {
        Map<String, InterMineBag> expected = createExpected(userAddressBag);
        assertEquals(expected, bagManager.getCurrentUserBagsOfType(testUser, "Address"));
    }

    public void testGetUserOrGlobalBagsOfTypeNoUserBags() throws Exception {
        Map<String, InterMineBag> expected = createExpected(globalCompanyBag);
        assertEquals(expected, bagManager.getCurrentUserOrGlobalBagsOfType(emptyUser, "Company"));
    }

    // user bags with same name as global bag take precedence
    public void testGetUserOrGlobalBagsOfTypeUserPrecedence() throws Exception {
        Map<String, InterMineBag> expected = createExpected(userCompanyBag);
        assertEquals(expected, bagManager.getUserOrGlobalBagsOfType(testUser, "Company"));
    }

    public void testGetUserOrGlobalBagsOfTypeNoneOfType() throws Exception {
        Map<String, InterMineBag> expected = Collections.EMPTY_MAP;
        assertEquals(expected, bagManager.getUserOrGlobalBagsOfType(testUser, "Employee"));
    }

    public void testGetCurrentUserOrGlobalBagsContainingId() throws Exception {
        storeAddress();
        globalAddressBag.addIdToBag(ADDRESS_ID, "Address");
        userAddressBag.addIdToBag(ADDRESS_ID, "Address");
        userAddressBag.setState(BagState.NOT_CURRENT);

        Set<InterMineBag> expected = new HashSet<InterMineBag>(Arrays.asList(globalAddressBag));
        try {
        	for (int i = 0; i < 1000; i++) { // try and provoke the intermittent exception
        		assertEquals(expected, bagManager.getCurrentUserOrGlobalBagsContainingId(testUser, ADDRESS_ID));
        	}
        } finally {
            deleteAddress();
        }
    }

    public void testGetUserOrGlobalBagsContainingIdNoBagsWithId() throws Exception {
        Set<InterMineBag> expected = Collections.EMPTY_SET;
        assertEquals(expected, bagManager.getCurrentUserOrGlobalBagsContainingId(testUser, DUMMY_ID));
    }

    private Map<String, InterMineBag> createExpected(InterMineBag... bags) {
        Map<String, InterMineBag> expected = new HashMap<String, InterMineBag>();
        for(InterMineBag bag : bags) {
            expected.put(bag.getName(), bag);
        }
        return expected;
    }

    private Map<String, InterMineBag> createExpectedCurrent(InterMineBag... bags) {
        Map<String, InterMineBag> expected = new HashMap<String, InterMineBag>();
        for(InterMineBag bag : bags) {
            if (bag.isCurrent()) {
                expected.put(bag.getName(), bag);
            }
        }
        return expected;
    }

    private void storeAddress() throws ObjectStoreException {
        ObjectStoreWriter osw = os.getNewWriter();
        Address address1 = DynamicUtil.createObject(Address.class);
        address1.setId(ADDRESS_ID);
        osw.store(address1);
        osw.close();
    }

    private void deleteAddress() throws ObjectStoreException {
        ObjectStoreWriter osw = os.getNewWriter();
        osw.delete(os.getObjectById(ADDRESS_ID));
        osw.close();
    }
}
