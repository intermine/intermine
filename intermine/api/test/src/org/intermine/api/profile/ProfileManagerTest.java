package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2011 FlyMine
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
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import junit.framework.Test;

import org.custommonkey.xmlunit.XMLUnit;
import org.intermine.api.template.TemplateQuery;
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
import org.intermine.objectstore.StoreDataTestCase;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.web.ProfileBinding;
import org.intermine.web.ProfileManagerBinding;
import org.intermine.web.bag.PkQueryIdUpgrader;

/**
 * Tests for the Profile class.
 */

public class ProfileManagerTest extends StoreDataTestCase
{
    private Profile bobProfile, sallyProfile;
    private ProfileManager pm;
    private ObjectStore os, uos;
    private ObjectStoreWriter osw, uosw;
    private Integer bobId = new Integer(101);
    private Integer sallyId = new Integer(102);
    private String bobPass = "bob_pass";
    private String sallyPass = "sally_pass";

    public ProfileManagerTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        os = osw.getObjectStore();
        uosw =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        uos = uosw.getObjectStore();
        pm = new ProfileManager(os, uosw);
    }

    public void executeTest(String type) {
    }

    public void testQueries() throws Throwable {
    }

    public static void oneTimeSetUp() throws Exception {
        StoreDataTestCase.oneTimeSetUp();
    }

    public static Test suite() {
        return buildSuite(ProfileManagerTest.class);
    }

    private void setUpUserProfiles() throws Exception {

        PathQuery query = new PathQuery(Model.getInstanceByName("testmodel"));
        Date date = new Date();
        SavedQuery sq = new SavedQuery("query1", date, query);

        // bob's details
        String bobName = "bob";

        InterMineBag bag = new InterMineBag("bag1", "Department", "This is some description",
                new Date(), os, bobId, uosw);

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

        TemplateQuery template =
            new TemplateQuery("template", "ttitle", "tcomment",
                              new PathQuery(Model.getInstanceByName("testmodel")));

        bobProfile = new Profile(pm, bobName, bobId, bobPass,
                                 new HashMap(), new HashMap(), new HashMap());
        pm.createProfile(bobProfile);
        bobProfile.saveQuery("query1", sq);
        bobProfile.saveBag("bag1", bag);
        bobProfile.saveTemplate("template", template);

        query = new PathQuery(Model.getInstanceByName("testmodel"));
        sq = new SavedQuery("query1", date, query);

        // sally details
        String sallyName = "sally";

        // employees and managers
//        <bag name="sally_bag2" type="org.intermine.model.CEO">
//        <bagElement type="org.intermine.model.CEO" id="1011"/>
//    </bag>

        CEO ceoEx = new CEO();
        ceoEx.setName("EmployeeB1");
        fieldNames = new HashSet<String>();
        fieldNames.add("name");
        CEO ceoB1 = (CEO) os.getObjectByExample(ceoEx, fieldNames);

        InterMineBag objectBag = new InterMineBag("bag2", "Employee", "description",
                new Date(), os, sallyId, uosw);
        objectBag.addIdToBag(ceoB1.getId(), "CEO");

        template = new TemplateQuery("template", "ttitle", "tcomment",
                                     new PathQuery(Model.getInstanceByName("testmodel")));
        sallyProfile = new Profile(pm, sallyName, sallyId, sallyPass,
                                   new HashMap(), new HashMap(), new HashMap());
        pm.createProfile(sallyProfile);
        sallyProfile.saveQuery("query1", sq);
        sallyProfile.saveBag("sally_bag1", objectBag);

        sallyProfile.saveTemplate("template", template);
    }


    public void tearDown() throws Exception {
        if (bobProfile != null) {
            for (String name : bobProfile.getSavedQueries().keySet()) {
                bobProfile.deleteQuery(name);
            }
            for (String name : bobProfile.getSavedTemplates().keySet()) {
                bobProfile.deleteTemplate(name, null);
            }
            for (String name : bobProfile.getSavedBags().keySet()) {
                bobProfile.deleteBag(name);
            }
        }
        if (sallyProfile != null) {
            for (String name : sallyProfile.getSavedQueries().keySet()) {
                sallyProfile.deleteQuery(name);
            }
            for (String name : sallyProfile.getSavedTemplates().keySet()) {
                sallyProfile.deleteTemplate(name, null);
            }
            for (String name : sallyProfile.getSavedBags().keySet()) {
                sallyProfile.deleteBag(name);
            }
        }
        cleanUserProfile();
    }

    private void cleanUserProfile() throws ObjectStoreException {
        if (uosw.isInTransaction()) {
            uosw.abortTransaction();
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(Tag.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new SimpleConstraint(new QueryField(qc, "tagName"), ConstraintOp.MATCHES, new QueryValue("test%")));
        SingletonResults res = uos.executeSingleton(q);
        Iterator resIter = res.iterator();
        uosw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            uosw.delete(o);
        }

        removeUserProfile("bob");
        removeUserProfile("sally");

        uosw.commitTransaction();
        uosw.close();
        osw.close();
    }

    private void removeUserProfile(String username) throws ObjectStoreException {
        Query q = new Query();
        QueryClass qc = new QueryClass(UserProfile.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryField qf = new QueryField(qc, "username");
        SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue(username));
        q.setConstraint(sc);
        SingletonResults res = uos.executeSingleton(q);
        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            uosw.delete(o);
        }
    }

    public void testXMLWrite() throws Exception {
        setUpUserProfiles();
        XMLUnit.setIgnoreWhitespace(true);
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        TagManager tagManager = getTagManager();

        tagManager.addTag("test-tag", "Department.company", "reference", "bob");
        tagManager.addTag("test-tag2", "Department.name", "attribute", "bob");
        tagManager.addTag("test-tag2", "Department.company", "reference", "bob");
        tagManager.addTag("test-tag2", "Department.employees", "collection", "bob");

        tagManager.addTag("test-tag", "Department.company", "reference", "sally");

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            writer.writeStartElement("userprofiles");
            ProfileBinding.marshal(bobProfile, os, writer, PathQuery.USERPROFILE_VERSION);
            ProfileBinding.marshal(sallyProfile, os, writer, PathQuery.USERPROFILE_VERSION);
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
        //String expectedXml = sb.toString();
        //String actualXml = sw.toString().trim();
        //System.out.println("expected: " + expectedXml);
        //System.out.println("actual: " + actualXml);
        // TODO this doesn't work because the ids don't match in the bag (as they are retrieved from
        // the database.
//        assertXMLEqual("XML doesn't match", expectedXml, actualXml);
    }

    private TagManager getTagManager() {
        return new TagManagerFactory(uosw).getTagManager();
    }

    public void testXMLRead() throws Exception {
        InputStream is =
            getClass().getClassLoader().getResourceAsStream("ProfileManagerBindingTestNewIDs.xml");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        ProfileManagerBinding.unmarshal(reader, pm, osw, new PkQueryIdUpgrader(os));

        // only profiles from file, not from setUpUserprofiles()
        assertEquals(2, pm.getProfileUserNames().size());

        assertTrue(pm.getProfileUserNames().contains("bob"));

        Profile sallyProfile = pm.getProfile("sally", sallyPass);

        Employee employeeEx = new Employee();
        employeeEx.setName("EmployeeA3");
        Set<String> fieldNames = new HashSet<String>();
        fieldNames.add("name");
        Employee employeeA3 = (Employee) os.getObjectByExample(employeeEx, fieldNames);
        Employee employeeEx2 = new Employee();
        employeeEx2.setName("EmployeeB2");
        Employee employeeB2 = (Employee) os.getObjectByExample(employeeEx2, fieldNames);
        Set<Integer> expectedBagContents = new HashSet<Integer>();

        expectedBagContents.add(employeeA3.getId());
        expectedBagContents.add(employeeB2.getId());

        System.out.println("Testing profile with hashCode " + System.identityHashCode(sallyProfile));
        assertEquals(3, sallyProfile.getSavedBags().size());
        assertEquals(expectedBagContents, ((InterMineBag) sallyProfile.getSavedBags().get("sally_bag3")).getContentsAsIds());

        assertEquals(1, sallyProfile.getSavedQueries().size());
        assertEquals(1, sallyProfile.getSavedTemplates().size());

        Set<Tag> expectedTags = new HashSet<Tag>();
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

        Set<Tag> actualTags = new HashSet<Tag>(getTagManager().getTags(null, null, null, "bob"));

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
                    && "bob".equals(actualTag.getUserProfile().getUsername())) {
                    continue ACTUAL;
                }
            }

            fail("can't find tag " + actualTag.getTagName() + ", "
                 + actualTag.getObjectIdentifier() + ", "
                 + actualTag.getType());
        }
    }
}
