package org.intermine.api.bag;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.TagManagerFactory;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Address;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;

public class BagManagerTest extends TestCase
{
    private ProfileManager pm;
    private ObjectStore os;
    private ObjectStoreWriter uosw;
    private Profile superUser, testUser, emptyUser;
    private BagManager bagManager;
    private TagManager tagManager;
    private InterMineBag globalCompanyBag, globalAddressBag, superPrivateBag, userCompanyBag, userAddressBag;
    private Integer ADDRESS_ID = 1;
    private Integer DUMMY_ID = 2;


    public void setUp() throws Exception {
        super.setUp();
        os = ObjectStoreFactory.getObjectStore("os.unittest");

        uosw =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        pm = new ProfileManager(os, uosw);

        superUser = new Profile(pm, "superUser", null, "password", new HashMap(), new HashMap(), new HashMap());
        pm.createProfile(superUser);
        pm.setSuperuser("superUser");

        testUser = new Profile(pm, "testUser", null, "password", new HashMap(), new HashMap(), new HashMap());
        pm.createProfile(testUser);

        emptyUser = new Profile(pm, "emptyUser", null, "password", new HashMap(), new HashMap(), new HashMap());
        pm.createProfile(emptyUser);

        tagManager = new TagManagerFactory(pm).getTagManager();
        bagManager = new BagManager(superUser, os.getModel());

        setUpBagsAndTags();
    }

    private void setUpBagsAndTags() throws Exception {
        globalCompanyBag = superUser.createBag("companyBag", "Company", "");
        globalAddressBag = superUser.createBag("globalAddressBag", "Address", "");
        superPrivateBag = superUser.createBag("superPrivateBag", "Company", "");

        tagManager.addTag(TagNames.IM_PUBLIC, "companyBag", TagTypes.BAG, "superUser");
        tagManager.addTag(TagNames.IM_PUBLIC, "globalAddressBag", TagTypes.BAG, "superUser");

        tagManager.addTag(TagNames.IM_FAVOURITE, "companyBag", TagTypes.BAG, "superUser");
        tagManager.addTag(TagNames.IM_FAVOURITE, "superPrivateBag", TagTypes.BAG, "superUser");

        userCompanyBag = testUser.createBag("companyBag", "Company", "");
        userAddressBag = testUser.createBag("userAddressBag", "Address", "");
    }


    public void tearDown() throws Exception {
        superUser.deleteBag(globalCompanyBag.getName());
        superUser.deleteBag(globalAddressBag.getName());
        superUser.deleteBag(superPrivateBag.getName());
        testUser.deleteBag(userCompanyBag.getName());
        testUser.deleteBag(userAddressBag.getName());
        removeUserProfile(superUser.getUsername());
        removeUserProfile(testUser.getUsername());
        removeUserProfile(emptyUser.getUsername());

        uosw.close();
    }

    private void removeUserProfile(String username) throws ObjectStoreException {
        Query q = new Query();
        QueryClass qc = new QueryClass(UserProfile.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryField qf = new QueryField(qc, "username");
        SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue(username));
        q.setConstraint(sc);
        SingletonResults res = uosw.getObjectStore().executeSingleton(q);
        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            uosw.delete(o);
        }
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

    public void testGetUserOrGlobalBagsOfType() throws Exception {
        Map<String, InterMineBag> expected = createExpected(globalAddressBag, userAddressBag);
        assertEquals(expected, bagManager.getUserOrGlobalBagsOfType(testUser, "Address"));
    }

    public void testGetUserOrGlobalBagsOfTypeNoUserBags() throws Exception {
        Map<String, InterMineBag> expected = createExpected(globalCompanyBag);
        assertEquals(expected, bagManager.getUserOrGlobalBagsOfType(emptyUser, "Company"));
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

    public void testGetUserOrGlobalBagsContainingId() throws Exception {
        storeAddress();
        globalAddressBag.addIdToBag(ADDRESS_ID, "Address");
        userAddressBag.addIdToBag(ADDRESS_ID, "Address");

        Set<InterMineBag> expected = new HashSet<InterMineBag>(createExpected(globalAddressBag, userAddressBag).values());
        try {
            assertEquals(expected, bagManager.getUserOrGlobalBagsContainingId(testUser, ADDRESS_ID));
        } finally {
            deleteAddress();
        }
    }

    public void testGetUserOrGlobalBagsContainingIdNoBagsWithId() throws Exception {
        Set<InterMineBag> expected = Collections.EMPTY_SET;
        assertEquals(expected, bagManager.getUserOrGlobalBagsContainingId(testUser, DUMMY_ID));
    }

    private Map<String, InterMineBag> createExpected(InterMineBag... bags) {
        Map<String, InterMineBag> expected = new HashMap<String, InterMineBag>();
        for(InterMineBag bag : bags) {
            expected.put(bag.getName(), bag);
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
