/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.flymine.dataloader;

import org.custommonkey.xmlunit.*;

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;
import java.util.List;
import java.lang.reflect.*;
import java.net.URL;

import org.flymine.model.testmodel.*;

public class XmlWriterCastorImplTest extends XMLTestCase {

    private XmlWriter xWriter;
    private int fakeId = 0;
    private File file;

    public XmlWriterCastorImplTest(String arg) {
        super(arg);
    }

    public void setUp() {
    }

    public void tearDown() {
        file.delete();
    }

    public void testMarshalObject() throws Exception {
        Department d1 = d1();


        xWriter = new XmlWriterCastorImpl("testmodel");

        file = File.createTempFile("temp", "xml");
        setIds(Arrays.asList(new Object[] {d1}));

        xWriter.writeXml(d1, file);

        URL controlUrl = XmlWriterCastorImplTest.class.getClassLoader()
            .getResource("test/testMarshalObject.xml");
        Reader control = new FileReader(controlUrl.getFile());
        Reader test = new FileReader(file);
        assertXMLEqual(control, test);

    }

    public void testMarshalCollection() throws Exception {
        Department d1 = d1();
        Department d2 = d2();
        Department d3 = d3();

        xWriter = new XmlWriterCastorImpl("testmodel");

        file = File.createTempFile("temp", "xml");
        List list = Arrays.asList(new Object[] {d1, d2, d3});
        setIds(list);

        xWriter.writeXml(list, file);

        URL controlUrl = XmlWriterCastorImplTest.class.getClassLoader()
            .getResource("test/testMarshalCollection.xml");
        Reader control = new FileReader(controlUrl.getFile());
        Reader test = new FileReader(file);
        assertXMLEqual(control, test);
    }


    private Department d1() {
        Address a1 = new Address();
        a1.setAddress("a1");

        Company c1 = new Company();
        c1.setName("c1");
        c1.setVatNumber(101);
        c1.setAddress(a1);

        Department d1 = new Department();
        d1.setName("d1");
        d1.setCompany(c1);
        c1.setDepartments(Arrays.asList(new Object[] {d1}));

        return d1;
    }


    private Department d2() {
        Address a1 = new Address();
        a1.setAddress("a1");

        Company c1 = new Company();
        c1.setName("c1");
        c1.setVatNumber(101);
        c1.setAddress(a1);

        Department d2 = new Department();
        d2.setName("d2");
        d2.setCompany(c1);
        c1.setDepartments(Arrays.asList(new Object[] {d2}));

        return d2;
    }


    private Department d3() {
        Address a2 = new Address();
        a2.setAddress("a2");

        Company c2 = new Company();
        c2.setName("c2");
        c2.setVatNumber(202);
        c2.setAddress(a2);

        Department d3 = new Department();
        d3.setName("d3");
        d3.setCompany(c2);
        c2.setDepartments(Arrays.asList(new Object[] {d3}));

        return d3;
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
