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
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.util.XmlBinding;

/**
 * Tests for the Profile class.
 */

public class ProfileBindingTest extends TestCase
{
    private PathQuery query;
    private InterMineBag bag;
    private TemplateQuery template;
    private ObjectStoreWriter osw;
    private ObjectStore os;

    public ProfileBindingTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        os = osw.getObjectStore();

        XmlBinding binding = new XmlBinding(osw.getModel());

        InputStream is =
            getClass().getClassLoader().getResourceAsStream("test/testmodel_data.xml");

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

        query = new PathQuery(Model.getInstanceByName("testmodel"));
        bag = new InterMineBag(ObjectStoreFactory.getObjectStore("os.unittest"));
        bag.add("foo1");
        bag.add("foo2");
        bag.add("foo3");
        bag.add(new Integer(100));
        bag.add(new Boolean(true));
        bag.add(new Float(1.1));

        bag.addId(new Integer(9));
        bag.addId(new Integer(10));

        template = new TemplateQuery("template", "tdesc", "tcat",
                                     new PathQuery(Model.getInstanceByName("testmodel")), false,
                                     "");
    }

    public void testXML() throws Exception {
        Profile profile = new Profile(null, "bob", "pass",
                                      new HashMap(), new HashMap(), new HashMap());
        profile.saveQuery("query1", query);
        profile.saveBag("bag1", bag);
        profile.saveTemplate("template", template);

        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            ProfileBinding.marshal(profile, Model.getInstanceByName("userprofile"), writer);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

        InputStream is =
            getClass().getClassLoader().getResourceAsStream("test/ProfileBindingTest.xml");
        BufferedReader bis = new BufferedReader(new InputStreamReader(is));

        String expectedXml = bis.readLine().trim();

        Diff diff = new Diff(expectedXml, sw.toString().trim());

        XMLAssert.assertXMLEqual("XML doesn't match", diff, true);
    }
}