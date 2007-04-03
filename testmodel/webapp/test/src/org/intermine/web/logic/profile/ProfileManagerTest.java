package org.intermine.web.logic.profile;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.userprofile.Tag;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
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
import org.intermine.web.ProfileBinding;
import org.intermine.web.ProfileManagerBinding;
import org.intermine.web.bag.PkQueryIdUpgrader;
import org.intermine.web.logic.ClassKeyHelper;
import org.intermine.web.logic.bag.BagElement;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.query.SavedQuery;
import org.intermine.web.logic.template.TemplateQuery;

/**
 * Tests for the Profile class.
 */

public class ProfileManagerTest extends XMLTestCase
{
    private Profile bobProfile, sallyProfile;
    private ProfileManager pm;
    private ObjectStore os, userProfileOS;
    private ObjectStoreWriter osw, userProfileOSW;
    private Integer bobId = new Integer(101);
    private Integer sallyId = new Integer(102);
    private String bobPass = "bob_pass";
    private String sallyPass = "sally_pass";
    private Map classKeys;
    
    public ProfileManagerTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        os = osw.getObjectStore();

        userProfileOSW =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        userProfileOS = userProfileOSW.getObjectStore();

        Properties classKeyProps = new Properties();
        classKeyProps.load(getClass().getClassLoader()
                           .getResourceAsStream("class_keys.properties"));
        classKeys = ClassKeyHelper.readKeys(os.getModel(), classKeyProps);
        pm = new ProfileManager(os, userProfileOSW, classKeys);
    }

    private void setUpUserProfiles() throws Exception {

        PathQuery query = new PathQuery(Model.getInstanceByName("testmodel"));
        Date date = null;
        SavedQuery sq = new SavedQuery("query1", date, query);

        // bob's details
        String bobName = "bob";

        Set contents = new HashSet();
        InterMineBag bag = new InterMineBag(bobId, "bag1", "org.intermine.model.testmodel.Department", userProfileOS,os, contents);
        bag.setDescription("This is some description");

        Department deptEx = new Department();
        deptEx.setName("DepartmentA1");
        Set fieldNames = new HashSet();
        fieldNames.add("name");
        Department departmentA1 = (Department) os.getObjectByExample(deptEx, fieldNames);
        bag.add(new BagElement(departmentA1.getId(), "org.intermine.model.testmodel.Department"));

        Department deptEx2 = new Department();
        deptEx2.setName("DepartmentB1");
        Department departmentB1 = (Department) os.getObjectByExample(deptEx2, fieldNames);
        bag.add(new BagElement(departmentB1.getId(), "org.intermine.model.testmodel.Department"));
       
        TemplateQuery template =
            new TemplateQuery("template", "ttitle", "tdesc", "tcomment",
                              new PathQuery(Model.getInstanceByName("testmodel")),
                              false, "");
        
        bobProfile = new Profile(pm, bobName, null, bobPass,
                                 new HashMap(), new HashMap(), new HashMap());
        pm.createProfile(bobProfile);
        UserProfile realBobProfile = new UserProfile();
        realBobProfile.setUsername(bobName);
        fieldNames = new HashSet();
        fieldNames.add("username");
        UserProfile bobProfile4ID = (UserProfile) userProfileOS.getObjectByExample(realBobProfile, fieldNames);
        bag.setUserId(bobProfile4ID.getId());
        bobProfile.setUserId(bobProfile4ID.getId());
        bobProfile.saveQuery("query1", sq);
        bobProfile.saveBag("bag1", bag);
        bobProfile.saveTemplate("template", template);

        query = new PathQuery(Model.getInstanceByName("testmodel"));
        contents = new HashSet();
        sq = new SavedQuery("query1", date, query);

        // sally details
        String sallyName = "sally";

        Set objectContents = new HashSet();

        // employees and managers
//        <bag name="sally_bag2" type="org.intermine.model.CEO">
//        <bagElement type="org.intermine.model.CEO" id="1011"/>
//    </bag>
        
        CEO ceoEx = new CEO();
        ceoEx.setName("EmployeeB1");
        fieldNames = new HashSet();
        fieldNames.add("name");
        CEO ceoB1 = (CEO) os.getObjectByExample(ceoEx, fieldNames);
        objectContents.add(ceoB1);

        InterMineBag objectBag = new InterMineBag(sallyId, "bag2", "org.intermine.model.testmodel.Employee", userProfileOS, os, contents);

        template = new TemplateQuery("template", "ttitle", "some desc", "tcomment",
                                     new PathQuery(Model.getInstanceByName("testmodel")), true,
                                     "some_keyword");

        sallyProfile = new Profile(pm, sallyName, sallyId, sallyPass,
                                   new HashMap(), new HashMap(), new HashMap());
        pm.createProfile(sallyProfile);
        UserProfile realSallyProfile = new UserProfile();
        realSallyProfile.setUsername(sallyName);
        fieldNames = new HashSet();
        fieldNames.add("username");
        UserProfile sallyProfile4ID = (UserProfile) userProfileOS.getObjectByExample(realSallyProfile, fieldNames);
        objectBag.setUserId(sallyProfile4ID.getId());
        bobProfile.setUserId(sallyProfile4ID.getId());
        sallyProfile.saveQuery("query1", sq);
        sallyProfile.saveBag("sally_bag1", objectBag);

        sallyProfile.saveTemplate("template", template);
    }


    public void tearDown() throws Exception {
        cleanUserProfile();
    }

    private void cleanUserProfile() throws ObjectStoreException {
        if (userProfileOSW.isInTransaction()) {
            userProfileOSW.abortTransaction();
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(Tag.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new SimpleConstraint(new QueryField(qc, "tagName"), ConstraintOp.MATCHES, new QueryValue("test%")));
        SingletonResults res = new SingletonResults(q, userProfileOS,
                                                    userProfileOS.getSequence());
        Iterator resIter = res.iterator();
        userProfileOSW.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            userProfileOSW.delete(o);
        }

        removeUserProfile("bob");
        removeUserProfile("sally");

        userProfileOSW.commitTransaction();
        userProfileOSW.close();
    }

    private void removeUserProfile(String username) throws ObjectStoreException {
        Query q = new Query();
        QueryClass qc = new QueryClass(UserProfile.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryField qf = new QueryField(qc, "username");
        SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue(username));
        q.setConstraint(sc);
        SingletonResults res = new SingletonResults(q, userProfileOS,
                                                    userProfileOS.getSequence());
        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            userProfileOSW.delete(o);
        }
    }

    public void testXMLWrite() throws Exception {
        setUpUserProfiles();
        XMLUnit.setIgnoreWhitespace(true);
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        pm.addTag("test-tag", "Department.company", "reference", "bob");
        pm.addTag("test-tag2", "Department.name", "attribute", "bob");
        pm.addTag("test-tag2", "Department.company", "reference", "bob");
        pm.addTag("test-tag2", "Department.employees", "collection", "bob");

        pm.addTag("test-tag", "Department.company", "reference", "sally");

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            writer.writeStartElement("userprofiles");
            ProfileBinding.marshal(bobProfile, os, writer);
            ProfileBinding.marshal(sallyProfile, os, writer);
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

        InputStream is =
            getClass().getClassLoader().getResourceAsStream("ProfileManagerBindingTest.xml");
        BufferedReader bis = new BufferedReader(new InputStreamReader(is));

        StringBuffer sb = new StringBuffer();

        String line = null;
        while ((line = bis.readLine()) != null) {
            sb.append(line.trim());
        }
        String expectedXml = sb.toString();
        String actualXml = sw.toString().trim();
        //System.out.println("expected: " + expectedXml);
        //System.out.println("actual: " + actualXml);
        // TODO this doesn't work because the ids don't match in the bag (as they are retrieved from
        // the database.
//        assertXMLEqual("XML doesn't match", expectedXml, actualXml);
    }

    public void testXMLRead() throws Exception {
        InputStream is =
            getClass().getClassLoader().getResourceAsStream("ProfileManagerBindingTestNewIDs.xml");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        ProfileManagerBinding.unmarshal(reader, pm, os, new PkQueryIdUpgrader(), classKeys);

        assertEquals(3, pm.getProfileUserNames().size());

        assertTrue(pm.getProfileUserNames().contains("bob"));

        Profile sallyProfile = pm.getProfile("sally", sallyPass);

        Employee employeeEx = new Employee();
        employeeEx.setName("EmployeeA3");
        Set fieldNames = new HashSet();
        fieldNames.add("name");
        Employee employeeA3 = (Employee) os.getObjectByExample(employeeEx, fieldNames);
        Employee employeeEx2 = new Employee();
        employeeEx2.setName("EmployeeB2");
        Employee employeeB2 = (Employee) os.getObjectByExample(employeeEx2, fieldNames);
        Set expectedBagContents = new HashSet();

        expectedBagContents.add(new BagElement(employeeA3.getId(), "org.intermine.model.testmodel.Employee"));
        expectedBagContents.add(new BagElement(employeeB2.getId(), "org.intermine.model.testmodel.Employee"));
        
        assertEquals(expectedBagContents, sallyProfile.getSavedBags().get("sally_bag3"));

        assertEquals(1, sallyProfile.getSavedQueries().size());
        assertEquals(1, sallyProfile.getSavedTemplates().size());

        Set expectedTags = new HashSet();
        Tag tag1 = (Tag) DynamicUtil.createObject(Collections.singleton(Tag.class));

        tag1.setTagName("test-tag");
        tag1.setObjectIdentifier("Department.company");
        tag1.setType("reference");
        tag1.setUserProfile(pm.getUserProfile("bob"));

        Tag tag2 = (Tag) DynamicUtil.createObject(Collections.singleton(Tag.class));
        tag2.setTagName("test-tag2");
        tag2.setObjectIdentifier("Department.name");
        tag2.setType("attribute");
        tag2.setUserProfile(pm.getUserProfile("bob"));

        Tag tag3 = (Tag) DynamicUtil.createObject(Collections.singleton(Tag.class));
        tag3.setTagName("test-tag2");
        tag3.setObjectIdentifier("Department.company");
        tag3.setType("reference");
        tag3.setUserProfile(pm.getUserProfile("bob"));

        Tag tag4 = (Tag) DynamicUtil.createObject(Collections.singleton(Tag.class));
        tag4.setTagName("test-tag2");
        tag4.setObjectIdentifier("Department.employees");
        tag4.setType("collection");
        tag4.setUserProfile(pm.getUserProfile("bob"));

        expectedTags.add(tag1);
        expectedTags.add(tag2);
        expectedTags.add(tag3);
        expectedTags.add(tag4);

        Set actualTags = new HashSet(pm.getTags(null, null, null, "bob"));

        assertEquals(expectedTags.size(), actualTags.size());

        Iterator actualTagsIter = actualTags.iterator();

      ACTUAL:
        while (actualTagsIter.hasNext()) {
            Tag actualTag = (Tag) actualTagsIter.next();

            Iterator expectedTagIter = expectedTags.iterator();

            while (expectedTagIter.hasNext()) {
                Tag expectedTag = (Tag) expectedTagIter.next();
                if (actualTag.getTagName().equals(expectedTag.getTagName())
                    && actualTag.getObjectIdentifier().equals(expectedTag.getObjectIdentifier())
                    && actualTag.getType().equals(expectedTag.getType())
                    && actualTag.getUserProfile().getUsername().equals("bob")) {
                    continue ACTUAL;
                }
            }

            fail("can't find tag " + actualTag.getTagName() + ", "
                 + actualTag.getObjectIdentifier() + ", "
                 + actualTag.getType());
        }
    }

    public void testAddTag() throws Exception {
        InputStream is =
            getClass().getClassLoader().getResourceAsStream("ProfileManagerBindingTestNewIDs.xml");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        ProfileManagerBinding.unmarshal(reader, pm, os, new PkQueryIdUpgrader(), classKeys);

        pm.addTag("test-tag", "Department.name", "attribute", "bob");
        pm.addTag("test-tag", "Department.company", "reference", "bob");
        pm.addTag("test-tag", "Department.employees", "collection", "bob");

        try {
            pm.addTag("test-tag", "Department.name", "error_tag_type", "bob");
            fail("expected runtime exception");
        } catch (RuntimeException e) {
            // expected
        }
        try {
            pm.addTag("test-tag", "Department.error_field", "collection", "bob");
            fail("expected runtime exception");
        } catch (RuntimeException e) {
            // expected
        }
        try {
            pm.addTag("test-tag", "Department.name", "collection", "bob");
            fail("expected runtime exception");
        } catch (RuntimeException e) {
            // expected
        }
        try {
            pm.addTag("test-tag", "Department.name", "reference", "bob");
            fail("expected runtime exception");
        } catch (RuntimeException e) {
            // expected
        }
        try {
            pm.addTag("test-tag", "Department.company", "attribute", "bob");
            fail("expected runtime exception");
        } catch (RuntimeException e) {
            // expected
        }
        try {
            pm.addTag("test-tag", "Department.company", "collection", "bob");
            fail("expected runtime exception");
        } catch (RuntimeException e) {
            // expected
        }
        try {
            pm.addTag("test-tag", "Department.employees", "attribute", "bob");
            fail("expected runtime exception");
        } catch (RuntimeException e) {
            // expected
        }
        try {
            pm.addTag("test-tag", "Department.employees", "reference", "bob");
            fail("expected runtime exception");
        } catch (RuntimeException e) {
            // expected
        }
        try {
            pm.addTag(null, "Department.name", "attribute", "bob");
            fail("expected runtime exception because of null parameter");
        } catch (RuntimeException e) {
            // expected
        }

        try {
            pm.addTag("test-tag", null, "attribute", "bob");
            fail("expected runtime exception because of null parameter");
        } catch (RuntimeException e) {
            // expected
        }
        try {
            pm.addTag("test-tag", "Department.name", null, "bob");
            fail("expected runtime exception because of null parameter");
        } catch (RuntimeException e) {
            // expected
        }
        try {
            pm.addTag("test-tag", "Department.name", "attribute", null);
            fail("expected runtime exception because of null parameter");
        } catch (RuntimeException e) {
            // expected
        }

        pm.addTag("test-tag", "org.intermine.model.testmodel.Department", "class", "bob");
    }

    public void testGetTags() throws Exception {
        InputStream is =
            getClass().getClassLoader().getResourceAsStream("ProfileManagerBindingTestNewIDs.xml");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        ProfileManagerBinding.unmarshal(reader, pm, os, new PkQueryIdUpgrader(), classKeys);

        pm.addTag("test_tag1", "Department.name", "attribute", "bob");
        pm.addTag("test_tag1", "Department.company", "reference", "bob");
        pm.addTag("test_tag1", "Department.employees", "collection", "bob");

        pm.addTag("test_tag2", "Department.name", "attribute", "bob");
        pm.addTag("test_tag2", "Department.company", "reference", "bob");
        pm.addTag("test_tag2", "Department.employees", "collection", "bob");

        pm.addTag("test_tag1", "Department.name", "attribute", "sally");
        pm.addTag("test_tag1", "Department.company", "reference", "sally");
        pm.addTag("test_tag1", "Department.employees", "collection", "sally");

        pm.addTag("test_tag3", "Department.name", "attribute", "sally");
        pm.addTag("test_tag3", "Department.company", "reference", "sally");
        pm.addTag("test_tag3", "Department.employees", "collection", "sally");

        pm.addTag("test_tag3.1", "org.intermine.model.testmodel.Department", "class", "sally");

        List allTags = pm.getTags(null, null, null, null);
        
        List aspectTags = pm.getTags("aspect:%", null, null, null);
        List imTags = pm .getTags("im:%", null, null, null);
        int excludeSize = aspectTags.size() + imTags.size();
        
        // 18 tags because ProfileManagerBindingTestNewIDs.xml has 5
        assertEquals(18 + excludeSize, allTags.size());

        List nameTags = pm.getTags("test_tag%", "Department.name", "attribute", "bob");
        assertEquals(3, nameTags.size());

        List bobAttributeTag1 = pm.getTags("test_tag1", null, "attribute", "bob");
        assertEquals(1, bobAttributeTag1.size());

        List bobTag1DeptName = pm.getTags("test_tag1", "Department.name", null, "bob");
        assertEquals(1, bobTag1DeptName.size());

        List allNameAttributeTag1s = pm.getTags("test_tag1", "Department.name", "attribute", null);
        assertEquals(2, allNameAttributeTag1s.size());

        List nameTagsPattern = pm.getTags("%tag3%", null, null, null);
        assertEquals(4, nameTagsPattern.size());

        List sallyNameTagsPattern = pm.getTags("test_tag%", null, null, "sal%");
        assertEquals(8, sallyNameTagsPattern.size());

        // "_" is a wildcard - add 3 from xml file
        List bobDeptPattern = pm.getTags("test_tag_", "Department.%", null, "bob");
        assertEquals(9, bobDeptPattern.size());

        //add 2 from XML
        List typeTestPattern = pm.getTags("test_tag_", "Department.%", "%e", "___");
        assertEquals(6, typeTestPattern.size());

        List allClassTags = pm.getTags(null, "org.intermine.model.testmodel.Department",
                                       "class", null);
        assertEquals(2, allClassTags.size());
    }
}
