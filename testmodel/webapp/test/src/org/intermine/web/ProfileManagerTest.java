package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.custommonkey.xmlunit.XMLTestCase;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.objectstore.dummy.ObjectStoreWriterDummyImpl;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;

import org.intermine.util.DynamicUtil;
import org.intermine.util.XmlBinding;
import org.intermine.web.bag.InterMineBag;
import org.intermine.web.bag.InterMineIdBag;
import org.intermine.web.bag.InterMinePrimitiveBag;
import org.intermine.web.bag.PkQueryIdUpgrader;

/**
 * Tests for the Profile class.
 */

public class ProfileManagerTest extends XMLTestCase
{
    private Profile bobProfile;
    private Profile sallyProfile;
    private ProfileManager pm;
    private ObjectStoreWriter osw;
    private ObjectStore os;
    private ObjectStoreWriter userProfileOS;

    public ProfileManagerTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        os = osw.getObjectStore();

        userProfileOS =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");

        pm = new ProfileManager(os, userProfileOS);

        XmlBinding binding = new XmlBinding(osw.getModel());
        InputStream is =
            getClass().getClassLoader().getResourceAsStream("testmodel_data.xml");
        List objects = (List) binding.unmarshal(is);

        osw.beginTransaction();
        Iterator iter = objects.iterator();
        int i = 1;
        while (iter.hasNext()) {
            InterMineObject o = (InterMineObject) iter.next();
            o.setId(new Integer(i++));
            osw.store(o);
        }
        osw.commitTransaction();

        PathQuery query = new PathQuery(Model.getInstanceByName("testmodel"));
        Date date = null;
        SavedQuery sq = new SavedQuery("query1", date, query);
        InterMineBag bag = new InterMinePrimitiveBag();
        bag.add("foo1");
        bag.add("foo2");
        bag.add("foo3");
        bag.add(new Integer(100));
        bag.add(new Boolean(true));
        bag.add(new Float(1.1));

        TemplateQuery template =
            new TemplateQuery("template", "tdesc",
                              new PathQuery(Model.getInstanceByName("testmodel")),
                              false, "");

        bobProfile = new Profile(pm, "bob", "pass",
                                 new HashMap(), new HashMap(), new HashMap());
        bobProfile.saveQuery("query1", sq);
        bobProfile.saveBag("bag1", bag);
        bobProfile.saveTemplate("template", template);

        query = new PathQuery(Model.getInstanceByName("testmodel"));
        bag = new InterMinePrimitiveBag();
        sq = new SavedQuery("query1", date, query);
        InterMineBag otherBag = new InterMinePrimitiveBag();

        bag.add("some value");
        otherBag.add(new Integer(123));

        InterMineIdBag objectBag = new InterMineIdBag();

        // employees and managers
        objectBag.add(10);
        objectBag.add(11);
        objectBag.add(12);

        // a department - this will cause the department with ID 6 to be add to the Item XML output
        // and will also implicitly add the Company of this department to the output because the
        // primary key of the Department includes the company reference
        objectBag.add(6);

        template = new TemplateQuery("template", "some desc",
                                     new PathQuery(Model.getInstanceByName("testmodel")), true,
                                     "some_keyword");


        sallyProfile = new Profile(pm, "sally", "sally_pass",
                                   new HashMap(), new HashMap(), new HashMap());
        sallyProfile.saveQuery("query1", sq);
        sallyProfile.saveBag("sally_bag1", bag);
        sallyProfile.saveBag("sally_bag2", otherBag);
        sallyProfile.saveBag("sally_bag3", objectBag);
        sallyProfile.saveTemplate("template", template);
    }

    public void tearDown() throws Exception {
        if (osw.isInTransaction()) {
            osw.abortTransaction();
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ObjectStore os = osw.getObjectStore();
        SingletonResults res = new SingletonResults(q, osw.getObjectStore(), osw.getObjectStore()
                                                    .getSequence());
        Iterator resIter = res.iterator();
        osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            osw.delete(o);
        }
        osw.commitTransaction();
        osw.close();

        cleanUserProfile();

    }

    private void cleanUserProfile() throws ObjectStoreException {
        if (userProfileOS.isInTransaction()) {
            userProfileOS.abortTransaction();
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(Tag.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ObjectStore os = userProfileOS.getObjectStore();
        SingletonResults res = new SingletonResults(q, userProfileOS.getObjectStore(),
                                                    userProfileOS.getObjectStore()
                                                    .getSequence());
        Iterator resIter = res.iterator();
        userProfileOS.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            userProfileOS.delete(o);
        }
        userProfileOS.commitTransaction();
        userProfileOS.close();
    }

    public void testXMLWrite() throws Exception {
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

        System.err.println ("expectedXml: " + expectedXml);
        System.err.println ("actualXml: " + actualXml);

        assertXMLEqual("XML doesn't match", expectedXml, actualXml);
    }

    public void testXMLRead() throws Exception {
        InputStream is =
            getClass().getClassLoader().getResourceAsStream("ProfileManagerBindingTestNewIDs.xml");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        ProfileManagerBinding.unmarshal(reader, pm, os, new PkQueryIdUpgrader());

        assertEquals(3, pm.getProfileUserNames().size());

        assertTrue(pm.getProfileUserNames().contains("bob"));

        Profile bobProfile = pm.getProfile("bob", "pass");
        Profile sallyProfile = pm.getProfile("sally", "sally_pass");

        Set expectedIDs = new HashSet();

        expectedIDs.add(new Integer(10));
        expectedIDs.add(new Integer(11));
        expectedIDs.add(new Integer(12));
        expectedIDs.add(new Integer(6));
        assertEquals(expectedIDs, sallyProfile.getSavedBags().get("sally_bag3"));

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

        ProfileManagerBinding.unmarshal(reader, pm, os, new PkQueryIdUpgrader());

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
    }

    public void testGetTags() throws Exception {
        InputStream is =
            getClass().getClassLoader().getResourceAsStream("ProfileManagerBindingTestNewIDs.xml");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        ProfileManagerBinding.unmarshal(reader, pm, os, new PkQueryIdUpgrader());

        pm.addTag("test-tag1", "Department.name", "attribute", "bob");
        pm.addTag("test-tag1", "Department.company", "reference", "bob");
        pm.addTag("test-tag1", "Department.employees", "collection", "bob");

        pm.addTag("test-tag2", "Department.name", "attribute", "bob");
        pm.addTag("test-tag2", "Department.company", "reference", "bob");
        pm.addTag("test-tag2", "Department.employees", "collection", "bob");

        pm.addTag("test-tag1", "Department.name", "attribute", "sally");
        pm.addTag("test-tag1", "Department.company", "reference", "sally");
        pm.addTag("test-tag1", "Department.employees", "collection", "sally");

        pm.addTag("test-tag3", "Department.name", "attribute", "sally");
        pm.addTag("test-tag3", "Department.company", "reference", "sally");
        pm.addTag("test-tag3", "Department.employees", "collection", "sally");

        List allTags = pm.getTags(null, null, null, null);
        assertEquals(12, allTags.size());

        List nameTags = pm.getTags(null, "Department.name", "attribute", "bob");
        assertEquals(2, nameTags.size());

        List bobAttributeTag1 = pm.getTags("test-tag1", null, "attribute", "bob");
        assertEquals(1, bobAttributeTag1.size());

        List bobTag1DeptName = pm.getTags("test-tag1", "Department.name", null, "bob");
        assertEquals(1, bobTag1DeptName.size());

        List allNameAttributeTag1s = pm.getTags("test-tag1", "Department.name", "attribute", null);
        assertEquals(2, allNameAttributeTag1s.size());

    }
}
