package org.flymine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.Writer;
import java.io.FileWriter;
import java.io.Reader;
import java.io.FileReader;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

import org.xml.sax.InputSource;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ojb.ObjectStoreWriterOjbImpl;
import org.flymine.objectstore.ojb.ObjectStoreOjbImpl;
import org.flymine.util.TypeUtil;
import org.flymine.util.ListBean;
import org.flymine.model.testmodel.*;

public class XmlDataLoaderTest extends TestCase
{
    protected ObjectStore os;
    protected ObjectStoreWriter writer;
    protected IntegrationWriter iw;
    protected int fakeId = 0;
    protected File file;
    protected Mapping map;
    protected ArrayList toDelete;

    public XmlDataLoaderTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        os = ObjectStoreFactory.getObjectStore("os.unittest");
        writer = new ObjectStoreWriterOjbImpl((ObjectStoreOjbImpl) os);
        iw = new IntegrationWriterSingleSourceImpl("test", writer);

        URL mapFile = getClass().getClassLoader().getResource("castor_xml_testmodel.xml");
        map = new Mapping();
        map.loadMapping(mapFile);
        toDelete = new ArrayList();
    }

    public void tearDown() throws Exception {
        Iterator deleteIter = toDelete.iterator();
        while (deleteIter.hasNext()) {
            Object o = deleteIter.next();
            writer.delete(o);
        }
        if (file != null) {
            file.delete();
        }
    }


    // marshal an object, set up as an InputSource and store to DB
    public void testSimpleObject() throws Exception {
        Company c1 = new Company();
        c1.setName("c1");
        c1.setVatNumber(101);
        Address a1 = new Address();
        a1.setAddress("a1");
        c1.setAddress(a1);

        List list = new ArrayList();
        list.add(c1);
        file = File.createTempFile("temp", "xml");
        marshalList(list, file);

        Reader reader = new FileReader(file);
        InputSource source = new InputSource(reader);

        XmlDataLoader dl = new XmlDataLoader(iw);
        dl.processXml(source);

        // check address was stored
        Address a2 = (Address) os.getObjectByExample(a1);
        assertNotNull("Expected address to be retieved from DB", a2);
        assertTrue("address id should be set", (a2.getId().intValue() != 0));

        // check company was stored
        Company c2 = (Company) os.getObjectByExample(c1);
        assertNotNull("Expected company to be retieved from DB", c2);
        assertTrue("company id should be set", (c2.getId().intValue() != 0));

        toDelete.add(a2);
        toDelete.add(c2);
    }

    // marshal an object, set up as an InputSource and store to DB
    public void testSubclassedObject() throws Exception {
        Manager m1 = new Manager();
        m1.setName("m1");
        m1.setTitle("Pointy Haired Boss");
        Address a1 = new Address();
        a1.setAddress("a1");
        m1.setAddress(a1);

        List list = new ArrayList();
        list.add(m1);

        file = File.createTempFile("temp", "xml");
        marshalList(list, file);

        Reader reader = new FileReader(file);
        InputSource source = new InputSource(reader);

        XmlDataLoader dl = new XmlDataLoader(iw);
        dl.processXml(source);

        // check address was stored
        Address a2 = (Address) os.getObjectByExample(a1);
        assertNotNull("Expected address to be retieved from DB", a2);
        assertTrue("address id should be set", (a2.getId().intValue() != 0));

        // check company was stored
        Manager m2 = (Manager) os.getObjectByExample(m1);
        assertNotNull("Expected company to be retieved from DB", m2);
        assertTrue("manager id should be set", (m2.getId().intValue() != 0));

        toDelete.add(a2);
        toDelete.add(m2);
    }

    public void testStoreFromFile() throws Exception {
        XmlDataLoader dl = new XmlDataLoader(iw);
        InputStream testData = getClass().getClassLoader().getResourceAsStream("test/testmodel_data.xml");
        dl.processXml(new InputSource(testData));

        // Just test that a specific Company is there
        Address a1 = new Address();
        a1.setAddress("Company Street, BVille");
        Company c1 = new Company();
        c1.setName("CompanyB");
        c1.setAddress(a1);
        Company c2 = (Company) os.getObjectByExample(c1);

        // Could only know the vatNumber if it got it from the database
        assertNotNull(c2);
        assertEquals(5678, c2.getVatNumber());

        // Read in the file again in order to delete the objects in it
        InputStream testData2 = getClass().getClassLoader().getResourceAsStream("test/testmodel_data.xml");
        Unmarshaller unmarshaller = new Unmarshaller(map);
        unmarshaller.setMapping(map);
        List objects = (List) unmarshaller.unmarshal(new InputSource(testData2));

        Iterator iter = objects.iterator();
        while (iter.hasNext()) {
            Object obj = iter.next();
            Object obj2 = os.getObjectByExample(obj);
            toDelete.add(obj2);
        }

    }

    private void marshalList(List list, File file) throws Exception {
        Writer writer = new FileWriter(file);
        Marshaller marshaller = new Marshaller(writer);
        marshaller.setMapping(map);

        List flat = TypeUtil.flatten(list);
        setIds(flat);
        ListBean bean = new ListBean();
        bean.setItems(flat);
        //LOG.warn("testSimpleObject..." + bean.getItems().toString());
        marshaller.marshal(bean);

        stripIds(flat);
    }

       // set fake ids for a collection of business objects
     void setIds(Collection c) throws Exception {
         Iterator iter = c.iterator();
         while (iter.hasNext()) {
             fakeId++;
             Object obj = iter.next();
             Class cls = obj.getClass();
             Field f = getIdField(cls);
             if (f != null) {
                 f.setAccessible(true);
                 f.set(obj, new Integer(fakeId));
             }
         }
     }

    // remove fake ids for a collection of business objects
     void stripIds(Collection c) throws Exception {
         Iterator iter = c.iterator();
         while (iter.hasNext()) {
             fakeId++;
             Object obj = iter.next();
             Class cls = obj.getClass();
             Field f = getIdField(cls);
             if (f != null) {
                 f.setAccessible(true);
                 f.set(obj, null);
             }
         }
     }

    // find the id field of an object, search superclasses if not found
    Field getIdField(Class cls) throws Exception {
        Field[] fields = cls.getDeclaredFields();
        for(int i=0; i<fields.length; i++) {
            if (fields[i].getName() == "id") {
                return fields[i];
            }
        }
        Class sup = cls.getSuperclass();
        if (sup != null && !sup.isInterface()) {
            return getIdField(sup);
        }
        return null;
    }
}
