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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.Reader;
import java.io.InputStream;
import java.io.FileInputStream;
import java.lang.reflect.Field;

import org.flymine.model.FlyMineBusinessObject;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreWriterFactory;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.SingletonResults;
import org.flymine.util.DynamicUtil;
import org.flymine.util.TypeUtil;
import org.flymine.util.ListBean;
import org.flymine.util.XmlBinding;
import org.flymine.model.testmodel.*;

public class XmlDataLoaderTest extends TestCase
{
    protected ObjectStoreWriter writer;
    protected IntegrationWriter iw;
    protected int fakeId = 0;
    protected File file;
    protected XmlBinding binding;
    protected ArrayList toDelete;

    public XmlDataLoaderTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        writer = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        iw = new IntegrationWriterSingleSourceImpl("test", writer);

        binding = new XmlBinding(writer.getModel());
        toDelete = new ArrayList();
    }

    public void tearDown() throws Exception {
        Iterator deleteIter = toDelete.iterator();
        while (deleteIter.hasNext()) {
            FlyMineBusinessObject o = (FlyMineBusinessObject) deleteIter.next();
            writer.delete(o);
        }
        if (file != null) {
            file.delete();
        }
    }


    // marshal an object, set up as an InputSource and store to DB
    public void testSimpleObject() throws Exception {
        Company c1 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        c1.setName("c1");
        c1.setVatNumber(101);
        Address a1 = new Address();
        a1.setAddress("a1");
        c1.setAddress(a1);

        List list = new ArrayList();
        list.add(c1);
        file = File.createTempFile("temp", "xml");
        marshalList(list, file);

        InputStream is = new FileInputStream(file);

        XmlDataLoader dl = new XmlDataLoader(iw);
        dl.processXml(is);

        // check address was stored
        Address a2 = (Address) writer.getObjectByExample(a1, Collections.singletonList("address"));
        assertNotNull("Expected address to be retieved from DB", a2);
        assertTrue("address id should be set", (a2.getId().intValue() != 0));

        // check company was stored
        Company c2 = (Company) writer.getObjectByExample(c1, Collections.singletonList("name"));
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

        InputStream is = new FileInputStream(file);

        XmlDataLoader dl = new XmlDataLoader(iw);
        dl.processXml(is);

        // check address was stored
        Address a2 = (Address) writer.getObjectByExample(a1, Collections.singletonList("address"));
        assertNotNull("Expected address to be retieved from DB", a2);
        assertTrue("address id should be set", (a2.getId().intValue() != 0));

        // check company was stored
        Manager m2 = (Manager) writer.getObjectByExample(m1, Collections.singletonList("name"));
        assertNotNull("Expected company to be retieved from DB", m2);
        assertTrue("manager id should be set", (m2.getId().intValue() != 0));

        toDelete.add(a2);
        toDelete.add(m2);
    }

    public void testStoreFromFile() throws Exception {
        XmlDataLoader dl = new XmlDataLoader(iw);
        InputStream testData = getClass().getClassLoader().getResourceAsStream("test/testmodel_data.xml");
        dl.processXml(testData);

        // Just test that a specific Company is there
        Address a1 = new Address();
        a1.setAddress("Company Street, BVille");
        Company c1 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        c1.setName("CompanyB");
        c1.setAddress(a1);
        Company c2 = (Company) writer.getObjectByExample(c1, Collections.singletonList("name"));

        // Could only know the vatNumber if it got it from the database
        assertNotNull(c2);
        assertEquals(5678, c2.getVatNumber());

        Query q = new Query();
        QueryClass qc = new QueryClass(FlyMineBusinessObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        List objects = new SingletonResults(q, writer);
        // TODO: Should the above be ObjectStore.singletonExecute() or something?
        Iterator iter = objects.iterator();
        while (iter.hasNext()) {
            Object obj = iter.next();
            toDelete.add(obj);
        }
    }

    private void marshalList(List list, File file) throws Exception {
        List flat = TypeUtil.flatten(list);
        setIds(flat);
        binding.marshal(flat, new BufferedWriter(new FileWriter(file)));
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
