package org.intermine.api.profile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.intermine.api.InterMineAPITestCase;
import org.intermine.api.xml.ProfileManagerBinding;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.StoreDataTestCase;
import org.intermine.util.DynamicUtil;

public class XMLReadTest extends InterMineAPITestCase
{

    private ProfileManager pm;

    public XMLReadTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        pm = im.getProfileManager();
        StoreDataTestCase.oneTimeSetUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        StoreDataTestCase.removeDataFromStore();
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

}
