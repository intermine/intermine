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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import junit.framework.TestCase;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.util.XmlBinding;
import org.intermine.web.bag.InterMineBag;
import org.intermine.web.bag.InterMinePrimitiveBag;

/**
 * Tests for the Profile class.
 */

public class ProfileManagerBindingTest extends TestCase
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
        osw.close();

        PathQuery query = new PathQuery(Model.getInstanceByName("testmodel"));
        InterMineBag bag = new InterMinePrimitiveBag();
        bag.add("foo1");
        bag.add("foo2");
        bag.add("foo3");
        bag.add(new Integer(100));
        bag.add(new Boolean(true));
        bag.add(new Float(1.1));

        TemplateQuery template =
            new TemplateQuery("template", "tdesc", "tcat",
                              new PathQuery(Model.getInstanceByName("testmodel")),
                              false, "");

        bobProfile = new Profile(null, "bob", "pass",
                                 new HashMap(), new HashMap(), new HashMap());
        bobProfile.saveQuery("query1", query);
        bobProfile.saveBag("bag1", bag);
        bobProfile.saveTemplate("template", template);

        query = new PathQuery(Model.getInstanceByName("testmodel"));
        bag = new InterMinePrimitiveBag();
        InterMineBag otherBag = new InterMinePrimitiveBag();

        bag.add("some value");
        otherBag.add(new Integer(123));

        template = new TemplateQuery("template", "some desc", "some category",
                                     new PathQuery(Model.getInstanceByName("testmodel")), true,
                                     "some_keyword");


        sallyProfile = new Profile(null, "sally", "sally_pass",
                                   new HashMap(), new HashMap(), new HashMap());
        sallyProfile.saveQuery("query1", query);
        sallyProfile.saveBag("sally_bag1", bag);
        sallyProfile.saveBag("sally_bag2", otherBag);
        sallyProfile.saveTemplate("template", template);
    }

    public void testXMLWrite() throws Exception {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            writer.writeStartElement("userprofiles");
            ProfileBinding.marshal(bobProfile, Model.getInstanceByName("userprofile"), writer);
            ProfileBinding.marshal(sallyProfile, Model.getInstanceByName("userprofile"), writer);
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

        Diff diff = new Diff(actualXml, actualXml);

        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual("XML doesn't match", diff, true);
    }

    public void testXMLRead() throws Exception {
        ProfileManager pm = new ProfileManager(os, userProfileOS);

        InputStream is =
            getClass().getClassLoader().getResourceAsStream("ProfileManagerBindingTest.xml");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        ProfileManagerBinding.unmarshal(reader, pm, os);

        assertEquals(3, pm.getProfileUserNames().size());

        assertTrue(pm.getProfileUserNames().contains("bob"));

        Profile bobProfile = pm.getProfile("bob", "pass");
        Profile sallyProfile = pm.getProfile("bob", "pass");
    }
}