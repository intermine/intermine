package org.intermine.xml.full;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.intermine.model.testmodel.*;
import org.intermine.metadata.Model;

public class FullParserTest extends TestCase
{
    private List exampleItems;
    private Item departmentItem;

    public FullParserTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        Item item1 = new Item();
        item1.setImplementations("http://www.intermine.org/model/testmodel#Company");
        item1.setIdentifier("1");
        Attribute attr1 = new Attribute();
        attr1.setName("name");
        attr1.setValue("Company1");
        item1.addAttribute(attr1);
        Attribute attr2 = new Attribute();
        attr2.setName("vatNumber");
        attr2.setValue("10");
        item1.addAttribute(attr2);
        Reference ref1 = new Reference();
        ref1.setName("address");
        ref1.setRefId("2");
        item1.addReference(ref1);
        ReferenceList col1 = new ReferenceList();
        col1.setName("departments");
        col1.addRefId("3");
        col1.addRefId("4");
        item1.addCollection(col1);

        Item item2 = new Item();
        item2.setClassName("http://www.intermine.org/model/testmodel#Address");
        item2.setImplementations("http://www.intermine.org/model/testmodel#Thing");
        item2.setIdentifier("2");
        Attribute field2 = new Attribute();
        field2.setName("address");
        field2.setValue("\"Company's\" street");
        item2.addAttribute(field2);

        Item item3 = new Item();
        item3.setClassName("http://www.intermine.org/model/testmodel#Department");
        item3.setImplementations("http://www.intermine.org/model/testmodel#RandomInterface");
        item3.setIdentifier("3");
        Attribute field3 = new Attribute();
        field3.setName("name");
        field3.setValue("Department1");
        item3.addAttribute(field3);

        departmentItem = new Item();
        departmentItem.setClassName("http://www.intermine.org/model/testmodel#Department");
        departmentItem.setImplementations("http://www.intermine.org/model/testmodel#RandomInterface");
        departmentItem.setIdentifier("4");
        Attribute field4 = new Attribute();
        field4.setName("name");
        field4.setValue("Department2");
        departmentItem.addAttribute(field4);

        exampleItems = Arrays.asList(new Object[] {item1, item2, item3, departmentItem});
    }

    public void testParse() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("FullParserTest.xml");
        assertEquals(exampleItems, FullParser.parse(is));
    }

    public void testParseNull() throws Exception {
        try {
            FullParser.parse(null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testRealiseObjects() throws Exception {
        Collection objects =
            FullParser.realiseObjects(exampleItems, Model.getInstanceByName("testmodel"), false);
        Company c1 = (Company) objects.iterator().next();
        assertEquals("Company1", c1.getName());
        assertNull(c1.getId());
        Address a1 = c1.getAddress();
        assertEquals("\"Company's\" street", a1.getAddress());
        assertNull(a1.getId());
        List departments = new ArrayList(c1.getDepartments());
        Collections.sort(departments, new DepartmentComparator());
        Department d1 = (Department) departments.get(0);
        assertEquals("Department1", d1.getName());
        assertNull(d1.getId());
        Department d2 = (Department) departments.get(1);
        assertEquals("Department2", d2.getName());
        assertNull(d2.getId());
    }

    public void testRealiseObjectsWithID() throws Exception {
        Collection objects =
            FullParser.realiseObjects(exampleItems, Model.getInstanceByName("testmodel"), true);
        Company c1 = (Company) objects.iterator().next();
        assertEquals("Company1", c1.getName());
        assertEquals(new Integer(1), c1.getId());
        Address a1 = c1.getAddress();
        assertEquals("\"Company's\" street", a1.getAddress());
        assertEquals(new Integer(2), a1.getId());
        List departments = new ArrayList(c1.getDepartments());
        Collections.sort(departments, new DepartmentComparator());
        Department d1 = (Department) departments.get(0);
        assertEquals("Department1", d1.getName());
        assertEquals(new Integer(3), d1.getId());
        Department d2 = (Department) departments.get(1);
        assertEquals("Department2", d2.getName());
        assertEquals(new Integer(4), d2.getId());
    }
    
    public void testRealiseObjectsWithUnderscoreID() throws Exception {
        departmentItem.setIdentifier("1_4");
        try {
            FullParser.realiseObjects(exampleItems, Model.getInstanceByName("testmodel"), true);
            fail("Expected: NumberFormatException");
        } catch (NumberFormatException e) {
            // expected
        }
    }

    class DepartmentComparator implements Comparator
    {
        public int compare(Object a, Object b) {
            return ((Department) a).getName().compareTo(((Department) b).getName());
        }
    }
}
