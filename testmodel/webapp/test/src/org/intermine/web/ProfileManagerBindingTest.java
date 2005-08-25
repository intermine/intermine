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
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.XmlBinding;
import org.intermine.web.bag.InterMineBag;
import org.intermine.web.bag.InterMineIdBag;
import org.intermine.web.bag.InterMinePrimitiveBag;
import org.intermine.web.bag.PkQueryIdUpgrader;

/**
 * Tests for the Profile class.
 */

public class ProfileManagerBindingTest extends XMLTestCase
{
    private Profile bobProfile;
    private Profile sallyProfile;
    private ObjectStoreWriter osw;
    private ObjectStore os;
    private ObjectStoreWriter userProfileOS;

    public ProfileManagerBindingTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        os = osw.getObjectStore();
        userProfileOS = ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");


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

        bobProfile = new Profile(null, "bob", "pass",
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


        sallyProfile = new Profile(null, "sally", "sally_pass",
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
    }

    public void testXMLWrite() throws Exception {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            writer.writeStartElement("userprofiles");
            ProfileBinding.marshal(bobProfile, Model.getInstanceByName("userprofile"),
                                    os, writer);
            ProfileBinding.marshal(sallyProfile, Model.getInstanceByName("userprofile"), os,
                                   writer);
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

        assertXMLEqual("XML doesn't match", expectedXml, actualXml);
    }

    public void testXMLRead() throws Exception {
        ProfileManager pm = new ProfileManager(os, userProfileOS);

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
    }
}
