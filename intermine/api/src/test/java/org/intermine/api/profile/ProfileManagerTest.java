package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
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
import org.intermine.api.xml.ProfileBinding;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Department;
import org.intermine.objectstore.StoreDataTestCase;
import org.intermine.pathquery.PathQuery;

/**
 * Tests for the Profile class.
 */

public class ProfileManagerTest extends InterMineAPITestCase
{
    private Profile bobProfile, sallyProfile;
    private ProfileManager pm;
    private final String bobName = "bob", sallyName = "sally", bobPass = "bob_pass", sallyPass = "sally_pass";
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
        setUpUserProfiles();
    }

    public void tearDown() throws Exception {
        StoreDataTestCase.removeDataFromStore();
        super.tearDown();
    }

    private void setUpUserProfiles() throws Exception {
        setUpBob();
        setUpSally();
    }

    private void setUpBob() throws Exception {
        bobProfile = pm.createBasicLocalProfile(bobName, bobPass, bobKey);
        Integer bobId = bobProfile.getUserId();

        PathQuery query = new PathQuery(Model.getInstanceByName("testmodel"));
        SavedQuery sq = new SavedQuery("query1", new Date(), query);

        // bob's details
        List<String> keyFields = Arrays.asList("name");
        
        InterMineBag bag = new InterMineBag("bag1", "Department", "This is some description",
                new Date(), BagState.CURRENT, os, bobId, uosw, keyFields);

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

        bobProfile.saveQuery("query1", sq);
        bobProfile.saveBag("bag1", bag);
        bobProfile.saveTemplate("template", template);
    }

    private void setUpSally() throws Exception {

        sallyProfile = pm.createBasicLocalProfile(sallyName, sallyPass, null);
        Integer sallyId = sallyProfile.getUserId();

        PathQuery query = new PathQuery(Model.getInstanceByName("testmodel"));
        SavedQuery sq = new SavedQuery("query1", new Date(), query);

        // sally details
        

        // employees and managers
        //    <bag name="sally_bag2" type="org.intermine.model.CEO">
        //        <bagElement type="org.intermine.model.CEO" id="1011"/>
        //    </bag>

        CEO ceoEx = new CEO();
        ceoEx.setName("EmployeeB1");
        CEO ceoB1 = (CEO) os.getObjectByExample(ceoEx, Collections.singleton("name"));

        InterMineBag objectBag = new InterMineBag("bag2", "Employee", "description",
                new Date(), BagState.CURRENT, os, sallyId, uosw, Arrays.asList("name"));
        objectBag.addIdToBag(ceoB1.getId(), "CEO");

        ApiTemplate template = new ApiTemplate("template", "ttitle", "tcomment",
                                     new PathQuery(Model.getInstanceByName("testmodel")));

        sallyProfile.saveQuery("query1", sq);
        sallyProfile.saveBag("sally_bag1", objectBag);

        sallyProfile.saveTemplate("template", template);
    }

    public void testXMLWrite() throws Exception {
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

    public void testApiKeys() throws Exception {
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
