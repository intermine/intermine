package org.flymine.util;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import org.xml.sax.InputSource;

import org.flymine.model.testmodel.*;

import org.custommonkey.xmlunit.XMLTestCase;

public class XmlBindingTest extends XMLTestCase {
    protected XmlBinding binding;
    protected File tempFile;
    
    public void setUp() throws Exception {
        binding = new XmlBinding("castor_xml_testmodel.xml");
        tempFile = File.createTempFile("temp", "xml");
    }

    public void tearDown() throws Exception {
        tempFile.delete();
    }

    public void testMarshalCollection() throws Exception {
        binding.marshal(list(), new BufferedWriter(new FileWriter(tempFile)));

        Reader original = new BufferedReader(new FileReader(getClass().getClassLoader().getResource("test/XmlBindingTest.xml").getFile()));
        Reader marshalled = new BufferedReader(new FileReader(tempFile));
        assertXMLEqual(original, marshalled);
    }

    public void testUnmarshal() throws Exception {
        List original = list();
        binding.marshal(original, new BufferedWriter(new FileWriter(tempFile)));
        List unmarshalled = (List) binding.unmarshal(new InputSource(new BufferedReader(new FileReader(tempFile))));
        setIds(unmarshalled);
        assertEquals(original, unmarshalled);
    }

    protected List list() throws Exception {
        int id = 0;

        Address a1 = new Address();
        a1.setAddress("a1");

        Address a2 = new Address();
        a2.setAddress("a2");

        Company c1 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        c1.setName("c1");
        c1.setVatNumber(101);
        c1.setAddress(a1);

        Company c2 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        c2.setName("c2");
        c2.setVatNumber(202);
        c2.setAddress(a2);

        Department d1 = new Department();
        d1.setName("d1");
        d1.setCompany(c1);

        Department d2 = new Department();
        d2.setName("d2");
        d2.setCompany(c1);

        c1.setDepartments(Arrays.asList(new Object[] {d1, d2}));

        Department d3 = new Department();
        d3.setName("d3");
        d3.setCompany(c2);

        c2.setDepartments(Arrays.asList(new Object[] {d3}));

        ListBean list = new ListBean();
        list.setItems(TypeUtil.flatten(Arrays.asList(new Object[] {d1, d2, d3})));
        setIds(list);
        return list;
    }

    protected void setIds(Collection c) throws Exception {
        int i=1;
        Iterator iter = c.iterator();
        while (iter.hasNext()) {
            TypeUtil.setFieldValue(iter.next(), "id", new Integer(i++));
        }
    }

    protected void stripIds(Collection c) throws Exception {
        Iterator iter = c.iterator();
        while (iter.hasNext()) {
            TypeUtil.setFieldValue(iter.next(), "id", null);
        }
    }
}
