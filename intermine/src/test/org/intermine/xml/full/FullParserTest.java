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

import org.flymine.model.testmodel.*;

public class FullParserTest extends TestCase
{

    public FullParserTest(String arg) {
        super(arg);
    }

    public void testParse1() throws Exception{
        InputStream is = getClass().getClassLoader().getResourceAsStream("test/FullParserTest.xml");
        List items = FullParser.parse(is);

        Item item1 = new Item();
        item1.setImplementations("http://www.flymine.org/testmodel#Company");
        item1.setIdentifier("1");
        Field field1 = new Field();
        field1.setName("http://www.flymine.org/testmodel#Company.name");
        field1.setValue("Company1");
        item1.addField(field1);
        Field ref1 = new Field();
        ref1.setName("http://www.flymine.org/testmodel#Company.address");
        ref1.setValue("2");
        item1.addReference(ref1);
        ReferenceList col1 = new ReferenceList();
        col1.setName("http://www.flymine.org/testmodel#Company.departments");
        col1.addValue("3");
        col1.addValue("4");
        item1.addCollection(col1);

        Item item2 = new Item();
        item2.setClassName("http://www.flymine.org/testmodel#Address");
        item2.setIdentifier("2");
        Field field2 = new Field();
        field2.setName("http://www.flymine.org/testmodel#Address.address");
        field2.setValue("Address1");
        item2.addField(field2);

        Item item3 = new Item();
        item3.setClassName("http://www.flymine.org/testmodel#Department");
        item3.setIdentifier("3");
        Field field3 = new Field();
        field3.setName("http://www.flymine.org/testmodel#Department.name");
        field3.setValue("Department1");
        item3.addField(field3);

        Item item4 = new Item();
        item4.setClassName("http://www.flymine.org/testmodel#Department");
        item4.setIdentifier("4");
        Field field4 = new Field();
        field4.setName("http://www.flymine.org/testmodel#Department.name");
        field4.setValue("Department2");
        item4.addField(field4);

        List expected = Arrays.asList(new Object[] {item1, item2, item3, item4});
        assertEquals(expected, items);
    }


    public void testParseNull() throws Exception{
        try {
            FullParser.parse(null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }

    }

}
