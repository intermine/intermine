package org.intermine.api.bag;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.api.InterMineAPITestCase;
import org.intermine.api.profile.BagState;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.TagManager;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.testmodel.Address;
import org.intermine.model.userprofile.Tag;
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
    private Profile bobProfile;

    public BagManagerTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        superUser = im.getProfileManager().getSuperuserProfile();
        emptyUser = im.getProfileManager().getProfile("emptyUser");
        tagManager = im.getProfileManager().getTagManager();
        bagManager = im.getBagManager();

        ProfileManager pm = im.getProfileManager();
        bobProfile = new Profile(pm, "bob", 101, "bob_pass", new HashMap(), new HashMap(),
                new HashMap(), true, false);
        pm.createProfile(bobProfile);
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

        tagManager.addTag(TagNames.IM_PUBLIC, "companyBag", TagTypes.BAG, superUser);
        tagManager.addTag(TagNames.IM_PUBLIC, "globalAddressBag", TagTypes.BAG, superUser);
        tagManager.addTag(TagNames.IM_FAVOURITE, "companyBag", TagTypes.BAG, superUser);
        tagManager.addTag(TagNames.IM_FAVOURITE, "superPrivateBag", TagTypes.BAG, superUser);

        userCompanyBag = testUser.createBag("companyBag", "Company", "", classKeys);
        userAddressBag = testUser.createBag("userAddressBag", "Address", "", classKeys);
    }

    public void testIsPublic() throws Exception {
        assertFalse(bagManager.isPublic(superPrivateBag));
        assertTrue(bagManager.isPublic(globalCompanyBag));
    }
    
    public void testGetTagsForBag() throws Exception {
        String notToBeFound = "NOT-FOR-PUBLIC-CONSUMPTION";
        String toBeFound = "BUT-THIS-ONE-IS-MINE";
        tagManager.addTag(notToBeFound, globalCompanyBag, bobProfile);
        tagManager.addTag(toBeFound, globalCompanyBag, superUser);
        
        List<Tag> tags = bagManager.getTagsForBag(globalCompanyBag, superUser);
        boolean hasTagItShouldHave = false;
        boolean hasTagItShouldNotHave = false;
        for (Tag t: tags) {
            if (notToBeFound.equals(t.getTagName())) {
                hasTagItShouldNotHave = true;
            }
            if (toBeFound.equals(t.getTagName())) {
                hasTagItShouldHave = true;
            }
        }
        assertTrue(hasTagItShouldHave && !hasTagItShouldNotHave);
    }

    public void testGetBagsWithTag() throws Exception {
        Map<String, InterMineBag> expected = createExpected(globalCompanyBag, globalAddressBag);
        assertEquals(expected, bagManager.getUserBagsWithTag(superUser, TagNames.IM_PUBLIC));

        expected = createExpected(globalCompanyBag, superPrivateBag);
        assertEquals(expected, bagManager.getUserBagsWithTag(superUser, TagNames.IM_FAVOURITE));

        expected = Collections.EMPTY_MAP;
        assertEquals(expected, bagManager.getUserBagsWithTag(superUser, TagNames.IM_HIDDEN));
    }

    public void testGetBagsWithTagsDashCollisions() throws Exception {
        Map<String, List<FieldDescriptor>>  classKeys = im.getClassKeys();
        InterMineBag listA = superUser.createBag("list-a", "Employee", "", classKeys);
        InterMineBag list_A = superUser.createBag("list_a", "Employee", "", classKeys);

        tagManager.addTag("FOO", listA, superUser);

        Map<String, InterMineBag> expected = createExpected(listA);
        assertEquals(expected, bagManager.getUserBagsWithTag(superUser, "FOO"));

    }

    public void testGetBagsWithTagsUnderscoreCollisions() throws Exception {
        Map<String, List<FieldDescriptor>>  classKeys = im.getClassKeys();
        InterMineBag listA = superUser.createBag("listX", "Employee", "", classKeys);
        InterMineBag list_A = superUser.createBag("list_", "Employee", "", classKeys);

        tagManager.addTag("FOO", listA, superUser);

        Map<String, InterMineBag> expected = createExpected(listA);
        assertEquals(expected, bagManager.getUserBagsWithTag(superUser, "FOO"));
    }

    public void testGetBagsWithTagsCaseCollisions() throws Exception {
        Map<String, List<FieldDescriptor>>  classKeys = im.getClassKeys();
        InterMineBag listb = superUser.createBag("list-b", "Employee", "", classKeys);
        InterMineBag listB = superUser.createBag("list-B", "Employee", "", classKeys);

        tagManager.addTag("BAR", listb, superUser);

        Map<String, InterMineBag> expectedB = createExpected(listb);
        assertEquals(expectedB, bagManager.getUserBagsWithTag(superUser, "BAR"));
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
        assertEquals(expected, bagManager.getBags(testUser));
    }

    public void testGetUserAndGlobalBagsNoUserBags() throws Exception {
        Map<String, InterMineBag> expected = createExpected(globalCompanyBag, globalAddressBag);
        assertEquals(expected, bagManager.getBags(emptyUser));
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
        assertEquals(userCompanyBag, bagManager.getBag(testUser, bagName));
    }

    public void testGetUserOrGlobalBagGlobal() throws Exception {
        String bagName = globalAddressBag.getName();
        assertEquals(globalAddressBag, bagManager.getBag(testUser, bagName));
    }

    public void testGetUserOrGlobalBagGlobalNoBagWithName() throws Exception {
        assertNull(bagManager.getBag(testUser, "dummyName"));
    }

    // user bags with same name as global bag take precedence
    public void testGetUserOrGlobalBagGlobalUserPrecedence() throws Exception {
        String bagName = userCompanyBag.getName();
        assertEquals(userCompanyBag, bagManager.getBag(testUser, bagName));
    }

    public void testGetCurrentUserOrGlobalBagsOfType() throws Exception {
        Map<String, InterMineBag> expected = createExpected(globalAddressBag, userAddressBag);
        assertEquals(expected, bagManager.getCurrentBagsOfType(testUser, "Address"));
        globalAddressBag.setState(BagState.NOT_CURRENT);
        expected = createExpectedCurrent(globalAddressBag, userAddressBag);
        assertEquals(expected, bagManager.getCurrentBagsOfType(testUser, "Address"));
    }

    public void testGetUserOrGlobalBagsOfType() throws Exception {
        Map<String, InterMineBag> expected = createExpected(globalAddressBag, userAddressBag);
        assertEquals(expected, bagManager.getBagsOfType(testUser, "Address"));
    }

    public void testGetUserBagsOfType() throws Exception {
        Map<String, InterMineBag> expected = createExpected(userAddressBag);
        assertEquals(expected, bagManager.getCurrentUserBagsOfType(testUser, "Address"));
    }

    public void testGetUserOrGlobalBagsOfTypeNoUserBags() throws Exception {
        Map<String, InterMineBag> expected = createExpected(globalCompanyBag);
        assertEquals(expected, bagManager.getCurrentBagsOfType(emptyUser, "Company"));
    }

    // user bags with same name as global bag take precedence
    public void testGetUserOrGlobalBagsOfTypeUserPrecedence() throws Exception {
        Map<String, InterMineBag> expected = createExpected(userCompanyBag);
        assertEquals(expected, bagManager.getBagsOfType(testUser, "Company"));
    }

    public void testGetUserOrGlobalBagsOfTypeNoneOfType() throws Exception {
        Map<String, InterMineBag> expected = Collections.EMPTY_MAP;
        assertEquals(expected, bagManager.getBagsOfType(testUser, "Employee"));
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
