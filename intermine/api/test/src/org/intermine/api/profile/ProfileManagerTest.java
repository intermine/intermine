package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.intermine.api.InterMineAPITestCase;
import org.intermine.api.profile.ProfileManager.ApiPermission;
import org.intermine.api.profile.ProfileManager.AuthenticationException;
import org.intermine.api.template.ApiTemplate;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.StoreDataTestCase;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.web.ProfileBinding;
import org.intermine.web.ProfileManagerBinding;

/**
 * Tests for the Profile class.
 */

public class ProfileManagerTest extends InterMineAPITestCase
{
    private Profile bobProfile, sallyProfile;
    private ProfileManager pm;
    private final Integer bobId = new Integer(101);
    private final Integer sallyId = new Integer(102);
    private final String bobPass = "bob_pass";
    private final String sallyPass = "sally_pass";
    private Map<String, List<FieldDescriptor>>  classKeys;

    private final String bobKey = "BOBKEY";

    public ProfileManagerTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        classKeys = im.getClassKeys();
        pm = im.getProfileManager();
        StoreDataTestCase.oneTimeSetUp();
//        StoreDataTestCase.storeData();

    }

    public void tearDown() throws Exception {
        super.tearDown();
        StoreDataTestCase.removeDataFromStore();
    }

    private void setUpUserProfiles() throws Exception {
        PathQuery query = new PathQuery(Model.getInstanceByName("testmodel"));
        Date date = new Date();
        SavedQuery sq = new SavedQuery("query1", date, query);

        // bob's details
        String bobName = "bob";
        List<String> classKeys = new ArrayList<String>();
        classKeys.add("name");
        InterMineBag bag = new InterMineBag("bag1", "Department", "This is some description",
                new Date(), BagState.CURRENT, os, bobId, uosw, classKeys);

        Department deptEx = new Department();
        deptEx.setName("DepartmentA1");
        Set<String> fieldNames = new HashSet<String>();
        fieldNames.add("name");
        Department departmentA1 = (Department) os.getObjectByExample(deptEx, fieldNames);
        bag.addIdToBag(departmentA1.getId(), "Department");

        Department deptEx2 = new Department();
        deptEx2.setName("DepartmentB1");
        Department departmentB1 = (Department) os.getObjectByExample(deptEx2, fieldNames);
        bag.addIdToBag(departmentB1.getId(), "Department");

        ApiTemplate template =
            new ApiTemplate("template", "ttitle", "tcomment",
                              new PathQuery(Model.getInstanceByName("testmodel")));

        bobProfile = new Profile(pm, bobName, bobId, bobPass,
                Collections.EMPTY_MAP, Collections.EMPTY_MAP, Collections.EMPTY_MAP, bobKey,
                true, false);
        pm.createProfile(bobProfile);
        bobProfile.saveQuery("query1", sq);
        bobProfile.saveBag("bag1", bag);
        bobProfile.saveTemplate("template", template);

        query = new PathQuery(Model.getInstanceByName("testmodel"));
        sq = new SavedQuery("query1", date, query);

        // sally details
        String sallyName = "sally";

        // employees and managers
        //    <bag name="sally_bag2" type="org.intermine.model.CEO">
        //        <bagElement type="org.intermine.model.CEO" id="1011"/>
        //    </bag>

        CEO ceoEx = new CEO();
        ceoEx.setName("EmployeeB1");
        fieldNames = new HashSet<String>();
        fieldNames.add("name");
        CEO ceoB1 = (CEO) os.getObjectByExample(ceoEx, fieldNames);

        InterMineBag objectBag = new InterMineBag("bag2", "Employee", "description",
                new Date(), BagState.CURRENT, os, sallyId, uosw, classKeys);
        objectBag.addIdToBag(ceoB1.getId(), "CEO");

        template = new ApiTemplate("template", "ttitle", "tcomment",
                                     new PathQuery(Model.getInstanceByName("testmodel")));
        sallyProfile = new Profile(pm, sallyName, sallyId, sallyPass,
                  Collections.EMPTY_MAP, Collections.EMPTY_MAP, Collections.EMPTY_MAP, true, false);
        pm.createProfile(sallyProfile);
        sallyProfile.saveQuery("query1", sq);
        sallyProfile.saveBag("sally_bag1", objectBag);

        sallyProfile.saveTemplate("template", template);
    }

    public void testXMLWrite() throws Exception {
        setUpUserProfiles();
        XMLUnit.setIgnoreWhitespace(true);
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        TagManager tagManager = im.getTagManager();

        tagManager.addTag("test-tag", "Department.company", "reference", "bob");
        tagManager.addTag("test-tag2", "Department.name", "attribute", "bob");
        tagManager.addTag("test-tag2", "Department.company", "reference", "bob");
        tagManager.addTag("test-tag2", "Department.employees", "collection", "bob");

        tagManager.addTag("test-tag", "Department.company", "reference", "sally");

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            writer.writeStartElement("userprofiles");
            ProfileBinding.marshal(bobProfile, os, writer,
                                   PathQuery.USERPROFILE_VERSION, classKeys);
            ProfileBinding.marshal(sallyProfile, os, writer,
                                   PathQuery.USERPROFILE_VERSION, classKeys);
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }


        InputStream is =
            getClass().getClassLoader().getResourceAsStream("ProfileManagerBindingTest.xml");
        String expectedXml = IOUtils.toString(is);

        String actualXml = sw.toString().trim();

        assertEquals(normalise(expectedXml), normalise(actualXml));
    }

    private static String normalise(String x) {
        return x.replaceAll(">\\s*<", "><") // Ignore whitespace between elements
                .replaceAll("\n", "")       // Remove all new-lines
                .replaceAll("\\s*/>", "/>") // Remove whitespace before />
                .replaceAll("\\s{2,}", " ") // Collapse white space to single space
                .replaceAll("date-created=\"\\d+\"", "date-created=\"XXXX\""); // Ignore all dates
    }

    public void testXMLRead() throws Exception {
        InputStream is =
            getClass().getClassLoader().getResourceAsStream("ProfileManagerBindingTestNewIDs.xml");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        ProfileManagerBinding.unmarshal(reader, pm, os.getNewWriter());

        assertEquals(4, pm.getProfileUserNames().size());

        assertTrue(pm.getProfileUserNames().contains("Unmarshall-1"));

        Profile stored2 = pm.getProfile("Unmarshall-2", "querty");

        assertEquals("token2", stored2.getApiKey());

        Employee employeeEx = new Employee();
        employeeEx.setName("EmployeeA3");
        Set<String> fieldNames = new HashSet<String>();
        fieldNames.add("name");

        assertEquals("Wrong number of bags!", 3, stored2.getSavedBags().size());
        Set<Integer> expectedBagContents = new HashSet<Integer>();
        //when we read xml file, we load data into savedbag and bagvalues table but not in the
        //osbag_int loaded after user login
        assertEquals(expectedBagContents,
                    (stored2.getSavedBags().get("stored_2_3")).getContentsAsIds());

        List<BagValue> contentsAsKey = (stored2.getSavedBags()
                .get("stored_2_1")).getContents();
        assertEquals("DepartmentA1", contentsAsKey.get(0).value);

        List<BagValue> contentsAsKey2 = (stored2.getSavedBags()
                .get("stored_2_3")).getContents();
        assertEquals("EmployeeA3", contentsAsKey2.get(0).value);
        assertEquals("EmployeeB2", contentsAsKey2.get(1).value);

        assertEquals(1, stored2.getSavedQueries().size());
        assertEquals(1, stored2.getSavedTemplates().size());

        Set<Tag> expectedTags = new HashSet<Tag>();
        Tag tag1 = (Tag) DynamicUtil.createObject(Collections.singleton(Tag.class));

        tag1.setTagName("test-tag");
        tag1.setObjectIdentifier("Department.company");
        tag1.setType("reference");
        tag1.setUserProfile(pm.getUserProfile("Unmarsall-1"));

        Tag tag2 = (Tag) DynamicUtil.createObject(Collections.singleton(Tag.class));
        tag2.setTagName("test-tag2");
        tag2.setObjectIdentifier("Department.name");
        tag2.setType("attribute");
        tag2.setUserProfile(pm.getUserProfile("Unmarsall-1"));

        Tag tag3 = (Tag) DynamicUtil.createObject(Collections.singleton(Tag.class));
        tag3.setTagName("test-tag2");
        tag3.setObjectIdentifier("Department.company");
        tag3.setType("reference");
        tag3.setUserProfile(pm.getUserProfile("Unmarsall-1"));

        Tag tag4 = (Tag) DynamicUtil.createObject(Collections.singleton(Tag.class));
        tag4.setTagName("test-tag2");
        tag4.setObjectIdentifier("Department.employees");
        tag4.setType("collection");
        tag4.setUserProfile(pm.getUserProfile("Unmarsall-1"));

        expectedTags.add(tag1);
        expectedTags.add(tag2);
        expectedTags.add(tag3);
        expectedTags.add(tag4);

        Set<Tag> actualTags = new HashSet<Tag>(im.getTagManager()
                .getTags(null, null, null, "Unmarshall-1"));

        assertEquals(expectedTags.size(), actualTags.size());

        Iterator<Tag> actualTagsIter = actualTags.iterator();

      ACTUAL:
        while (actualTagsIter.hasNext()) {
            Tag actualTag = actualTagsIter.next();

            Iterator<Tag> expectedTagIter = expectedTags.iterator();

            while (expectedTagIter.hasNext()) {
                Tag expectedTag = expectedTagIter.next();
                if (actualTag.getTagName().equals(expectedTag.getTagName())
                    && actualTag.getObjectIdentifier().equals(expectedTag.getObjectIdentifier())
                    && actualTag.getType().equals(expectedTag.getType())
                    && "Unmarshall-1".equals(actualTag.getUserProfile().getUsername())) {
                    continue ACTUAL;
                }
            }

            fail("can't find tag " + actualTag.getTagName() + ", "
                 + actualTag.getObjectIdentifier() + ", "
                 + actualTag.getType());
        }
    }

    public void testApiKeys() throws Exception {
        setUpUserProfiles();
        Profile bob = pm.getProfile("bob");
        assertEquals(bob.getApiKey(), bobKey);
        Profile sally = pm.getProfile("sally");
        assertEquals(sally.getApiKey(), null);
        bob.setApiKey("NEW-TOKEN");
        assertEquals(bob.getApiKey(), "NEW-TOKEN");
        sally.setApiKey("ANOTHER-TOKEN");
        assertEquals(sally.getApiKey(), "ANOTHER-TOKEN");
    }

    public void testGetRWPermission() throws Exception {
        setUpUserProfiles();
        ApiPermission permission = null;
        permission = pm.getPermission(bobKey, classKeys);
        assertNotNull(permission);
        assertTrue(permission.isRW());
        assertEquals(permission.getProfile().getUsername(), bobProfile.getUsername());

        permission = pm.getPermission("bob", bobPass, classKeys);
        assertNotNull(permission);
        assertTrue(permission.isRW());
        assertEquals(permission.getProfile().getUsername(), bobProfile.getUsername());

        permission = pm.getPermission("sally", sallyPass, classKeys);
        assertNotNull(permission);
        assertTrue(permission.isRW());
        assertEquals(permission.getProfile().getUsername(), sallyProfile.getUsername());

        String newKey = pm.generateApiKey(bobProfile);
        permission = pm.getPermission(newKey, classKeys);
        assertNotNull(permission);
        assertTrue(permission.isRW());
        assertEquals(permission.getProfile().getUsername(), bobProfile.getUsername());

        try {
            pm.getPermission("foo", classKeys);
            fail("Expected an exception here");
        } catch (AuthenticationException e) {
            //
        }

        int limit = 100;

        String[] keys = new String[limit];
        Set<String> uniqueKeys = new HashSet<String>();
        for (int i = 0; i < limit; i++) {
            keys[i] = pm.generateApiKey(bobProfile);
            uniqueKeys.add(keys[i]);
        }

        assertEquals(uniqueKeys.size(), limit);

        // Only the last key is valid
        permission = pm.getPermission(keys[limit - 1], classKeys);
        assertNotNull(permission);
        assertTrue(permission.isRW());
        assertEquals(permission.getProfile().getUsername(), bobProfile.getUsername());

        // All other keys are invalid
        for (int i = 0; i < (limit - 1); i++) {
            try {
                pm.getPermission(keys[i], classKeys);
                fail("expected authentication exception");
            } catch (AuthenticationException e) {
                // expected
            }
        }

    }

    public void testGetROPermission() throws Exception {
        setUpUserProfiles();
        ApiPermission permission = null;

        String key = pm.generateSingleUseKey(bobProfile);
        permission = pm.getPermission(key, classKeys);
        assertNotNull(permission);
        assertTrue(permission.isRO());
        assertEquals(permission.getProfile().getUsername(), bobProfile.getUsername());

        try {
            pm.getPermission(key, classKeys);
            fail("Expected an exception here");
        } catch (AuthenticationException e) {
            //
        }

        try {
            pm.getPermission("foo", classKeys);
            fail("Expected an exception here");
        } catch (AuthenticationException e) {
            //
        }

        String[] keys = new String[1000];
        Set<String> uniqueKeys = new HashSet<String>();
        for (int i = 0; i < 1000; i++) {
            keys[i] = pm.generateSingleUseKey(bobProfile);
            uniqueKeys.add(keys[i]);
        }

        assertEquals(uniqueKeys.size(), 1000);

        // It's ok to have many single use keys
        for (int j = 0; j < 1000; j++) {
            permission = pm.getPermission(keys[j], classKeys);
            assertNotNull(permission);
            assertTrue(permission.isRO());
            assertEquals(permission.getProfile().getUsername(), bobProfile.getUsername());
        }

    }
}
