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

import junit.framework.TestCase;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Arrays;
import java.util.Collection;

import org.flymine.model.testmodel.*;
import org.flymine.metadata.Model;

public class FullParserTest extends TestCase
{
    private List exampleItems;

    public FullParserTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
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

        exampleItems = Arrays.asList(new Object[] {item1, item2, item3, item4});
    }

    public void testParse() throws Exception{
        InputStream is = getClass().getClassLoader().getResourceAsStream("test/FullParserTest.xml");
        List items = FullParser.parse(is);

        assertEquals(exampleItems, items);
    }


    public void testParseNull() throws Exception{
        try {
            FullParser.parse(null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }

    }

    public void testRealiseObjects() throws Exception {
        Collection objects = FullParser.realiseObjects(exampleItems, Model.getInstanceByName("testmodel"));

        Company c1 = (Company) objects.iterator().next();
        assertEquals("Company1", c1.getName());
        assertNull(c1.getId());
        Address a1 = c1.getAddress();
        assertEquals("Address1", a1.getAddress());
        assertNull(a1.getId());
        Department d1 = (Department) c1.getDepartments().get(0);
        assertEquals("Department1", d1.getName());
        assertNull(d1.getId());
        Department d2 = (Department) c1.getDepartments().get(1);
        assertEquals("Department2", d2.getName());
        assertNull(d1.getId());
    }
}
