package org.flymine.dataloader;

import junit.framework.TestCase;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.URL;

import org.exolab.castor.mapping.*;
import org.exolab.castor.xml.*;

import org.xml.sax.InputSource;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ojb.ObjectStoreWriterOjbImpl;
import org.flymine.objectstore.ojb.ObjectStoreOjbImpl;
import org.flymine.util.*;
import org.flymine.model.testmodel.*;

public class XmlDataLoaderTest extends TestCase {

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
        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
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
        file.delete();
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
        file = new File("temp.xml");
        marshalList(list, file);

        Reader reader = new FileReader(file);
        InputSource source = new InputSource(reader);

        XmlDataLoader.processXml("testmodel", iw, source);

        // check address was stored
        Address a2 = (Address) writer.getObjectByExample(a1);
        assertNotNull("Expected address to be retieved from DB", a2);
        assertTrue("address id should be set", (a2.getId().intValue() != 0));

        // check company was stored
        Company c2 = (Company) writer.getObjectByExample(c1);
        assertNotNull("Expected company to be retieved from DB", c2);
        assertTrue("company id should be set", (c2.getId().intValue() != 0));

        toDelete.add(a2);
        toDelete.add(c2);
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
