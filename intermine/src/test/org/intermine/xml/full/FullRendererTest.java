package org.flymine.xml.full;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.util.Date;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import org.flymine.util.TypeUtil;
import org.flymine.util.DynamicUtil;
import org.flymine.model.testmodel.*;
import org.flymine.metadata.Model;

import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;

public class FullRendererTest extends XMLTestCase
{
    private Model model;
    private final String ENDL = System.getProperty("line.separator");

    public void setUp() throws Exception {
        model = Model.getInstanceByName("testmodel");
    }

    public void testRenderBusinessObjects() throws Exception {
        Department d1 = new Department();
        d1.setId(new Integer(5678));
        Department d2 = new Department();
        d2.setId(new Integer(6789));

        List list = Arrays.asList(new Object[] {d1, d2});

        String expected = "<items>" + ENDL
            + "<object xml_id=\"5678\" class=\"http://www.flymine.org/model/testmodel#Department\" implements=\"http://www.flymine.org/model/testmodel#RandomInterface\">" + ENDL
            + "</object>" + ENDL
            + "<object xml_id=\"6789\" class=\"http://www.flymine.org/model/testmodel#Department\" implements=\"http://www.flymine.org/model/testmodel#RandomInterface\">" + ENDL
            + "</object>" + ENDL
            + "</items>" + ENDL;

        assertEquals(expected, FullRenderer.render(list, model));
    }

    public void testRenderItems() throws Exception {
        Item item1 = new Item();
        item1.setImplementations("http://www.flymine.org/testmodel#Company");
        item1.setIdentifier("1");
        Field field1 = new Field();
        field1.setName("name");
        field1.setValue("Company1");
        item1.addField(field1);
        Field ref1 = new Field();
        ref1.setName("address");
        ref1.setValue("2");
        item1.addReference(ref1);
        ReferenceList col1 = new ReferenceList();
        col1.setName("departments");
        col1.addValue("3");
        col1.addValue("4");
        item1.addCollection(col1);

        Item item2 = new Item();
        item2.setClassName("http://www.flymine.org/testmodel#Address");
        item2.setIdentifier("2");
        Field field2 = new Field();
        field2.setName("address");
        field2.setValue("Address1");
        item2.addField(field2);

        Item item3 = new Item();
        item3.setClassName("http://www.flymine.org/testmodel#Department");
        item3.setIdentifier("3");
        Field field3 = new Field();
        field3.setName("name");
        field3.setValue("Department1");
        item3.addField(field3);

        Item item4 = new Item();
        item4.setClassName("http://www.flymine.org/testmodel#Department");
        item4.setIdentifier("4");
        Field field4 = new Field();
        field4.setName("name");
        field4.setValue("Department2");
        item4.addField(field4);

        List exampleItems = Arrays.asList(new Object[] {item1, item2, item3, item4});
        String generated = FullRenderer.render(exampleItems);

        InputStream expected = getClass().getClassLoader().getResourceAsStream("test/FullParserTest.xml");

        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(new InputStreamReader(expected), new StringReader(generated));
    }

    public void testRenderObjectMaterial() throws Exception {
        Employee e = new Employee();
        Department d = new Department();
        e.setId(new Integer(1234));
        e.setName("Employee1");
        d.setId(new Integer(5678));
        e.setDepartment(d);

        String expected = "<object xml_id=\"1234\" class=\"http://www.flymine.org/model/testmodel#Employee\" implements=\"http://www.flymine.org/model/testmodel#Employable http://www.flymine.org/model/testmodel#HasAddress\">" + ENDL
            + "<field name=\"age\" value=\"0\"/>" + ENDL
            + "<field name=\"fullTime\" value=\"false\"/>" + ENDL
            + "<field name=\"name\" value=\"Employee1\"/>" + ENDL
            + "<reference name=\"department\" ref_id=\"5678\"/>" + ENDL
            + "</object>" + ENDL;

        assertEquals(expected, FullRenderer.render(e, model));
    }

    public void testRenderObjectNoId() throws Exception {
        Employee e = new Employee();
        e.setName("Employee1");

        try {
            FullRenderer.render(e, model);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }
    }

    public void testRenderObjectDynamic() throws Exception {
        Department d1 = new Department();
        d1.setId(new Integer(5678));
        Department d2 = new Department();
        d2.setId(new Integer(6789));

        Object o = DynamicUtil.createObject(new HashSet(Arrays.asList(new Class[] {Company.class, Broke.class})));
        Company c = (Company) o;
        c.setId(new Integer(1234));
        c.setName("BrokeCompany1");
        c.setDepartments(Arrays.asList(new Object[] {d1, d2}));

        Broke b = (Broke) o;
        b.setDebt(10);

        String expected = "<object xml_id=\"1234\" class=\"\" implements=\"http://www.flymine.org/model/testmodel#Broke http://www.flymine.org/model/testmodel#Company\">" + ENDL
            + "<field name=\"vatNumber\" value=\"0\"/>" + ENDL
            + "<field name=\"debt\" value=\"10\"/>" + ENDL
            + "<collection name=\"departments\">" + ENDL
            + "<reference ref_id=\"5678\"/>" + ENDL
            + "<reference ref_id=\"6789\"/>" + ENDL
            + "</collection>" + ENDL
            + "<field name=\"name\" value=\"BrokeCompany1\"/>" + ENDL
            + "</object>" + ENDL;

        assertEquals(expected, FullRenderer.render(b, model));
    }

    public void testRenderTypes() throws Exception {
        Types t = new Types();
        t.setId(new Integer(1234));
        t.setName("Types1");
        t.setFloatType(1.2f);
        t.setDoubleType(1.3d);
        t.setIntType(2);
        t.setBooleanType(true);
        t.setBooleanObjType(Boolean.TRUE);
        t.setIntObjType(new Integer(4));
        t.setFloatObjType(new Float(2.2f));
        t.setDoubleObjType(new Double(2.3d));
        t.setDateObjType(new Date(7777777777l));
        t.setStringObjType("A String");

        String expected = "<object xml_id=\"1234\" class=\"http://www.flymine.org/model/testmodel#Types\" implements=\"http://www.flymine.org/model/testmodel#FlyMineBusinessObject\">" + ENDL
            + "<field name=\"intObjType\" value=\"4\"/>" + ENDL
            + "<field name=\"booleanObjType\" value=\"true\"/>" + ENDL
            + "<field name=\"doubleType\" value=\"1.3\"/>" + ENDL
            + "<field name=\"floatType\" value=\"1.2\"/>" + ENDL
            + "<field name=\"floatObjType\" value=\"2.2\"/>" + ENDL
            + "<field name=\"booleanType\" value=\"true\"/>" + ENDL
            + "<field name=\"stringObjType\" value=\"A String\"/>" + ENDL
            + "<field name=\"doubleObjType\" value=\"2.3\"/>" + ENDL
            + "<field name=\"intType\" value=\"2\"/>" + ENDL
            + "<field name=\"name\" value=\"Types1\"/>" + ENDL
            + "<field name=\"dateObjType\" value=\"7777777777\"/>" + ENDL
            + "</object>" + ENDL;

        assertEquals(expected, FullRenderer.render(t, model));
    }
}
