package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

/**
 * Tests for the path traversing methods in AbstractItemReader.
 * 
 * @author Thomas Riley
 */
public class AbstractItemReaderTest extends TestCase
{
    private String srcNs = "http://www.intermine.org/source#";
    private ItemFactory itemFactory = new ItemFactory();

    public AbstractItemReaderTest(String arg) {
        super(arg);
    }
    

    public void testSingleReferencePath() throws Exception {
        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        Item src2 = itemFactory.makeItem();
        src2.setIdentifier("2");
        src2.setClassName(srcNs + "Address");
        Reference r1 = new Reference();
        r1.setName("address");
        r1.setRefId("2");
        src1.addReference(r1);
        
        Map items = new HashMap();
        items.put(src1.getIdentifier(), ItemHelper.convert(src1));
        items.put(src2.getIdentifier(), ItemHelper.convert(src2));
        
        ItemReader reader = new MockItemReader(items);
        Item address = ItemHelper.convert(reader.getItemByPath(new ItemPath("LtdCompany.address", srcNs),
                ItemHelper.convert(src1)));
        
        assertEquals(src2, address);
    }
    
    public void testSingleNullReferencePath() throws Exception {
        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        Reference r1 = new Reference();
        r1.setName("address");
        r1.setRefId("2");
        src1.addReference(r1);
        
        Map items = new HashMap();
        items.put(src1.getIdentifier(), ItemHelper.convert(src1));
        
        ItemReader reader = new MockItemReader(items);
        Object address = reader.getItemByPath(new ItemPath("LtdCompany.address", srcNs),
                ItemHelper.convert(src1));
        
        assertNull(address);
    }
    
    public void testGetItemByPathWithCollection() throws Exception {
        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        
        ReferenceList r1 = new ReferenceList();
        r1.setName("departments");
        r1.addRefId("2");
        r1.addRefId("3");
        src1.addCollection(r1);
        
        Item src2 = itemFactory.makeItem();
        src2.setIdentifier("2");
        src2.setClassName(srcNs + "Department");
        Item src3 = itemFactory.makeItem();
        src3.setIdentifier("3");
        src3.setClassName(srcNs + "Department");
        
        Map items = new HashMap();
        items.put(src1.getIdentifier(), ItemHelper.convert(src1));
        items.put(src2.getIdentifier(), ItemHelper.convert(src2));
        items.put(src3.getIdentifier(), ItemHelper.convert(src3));
        
        ItemReader reader = new MockItemReader(items);
        
        try {
            reader.getItemByPath(new ItemPath("LtdCompany.departments", srcNs),
                    ItemHelper.convert(src1));
            fail("Expected ObjectStoreException - too many items");
        } catch (ObjectStoreException err) {}
    }
    
    public void testGetItemsByPathWithCollection() throws Exception {
        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        
        Item src4 = itemFactory.makeItem();
        src4.setIdentifier("4");
        src4.setClassName(srcNs + "Address");
        Reference r = new Reference();
        r.setName("address");
        r.setRefId("4");
        src1.addReference(r);
        
        ReferenceList r1 = new ReferenceList();
        r1.setName("departments");
        r1.addRefId("2");
        r1.addRefId("3");
        src1.addCollection(r1);
        
        Item src2 = itemFactory.makeItem();
        src2.setIdentifier("2");
        src2.setClassName(srcNs + "Department");
        Item src3 = itemFactory.makeItem();
        src3.setIdentifier("3");
        src3.setClassName(srcNs + "Department");
        
        Map items = new HashMap();
        items.put(src1.getIdentifier(), ItemHelper.convert(src1));
        items.put(src2.getIdentifier(), ItemHelper.convert(src2));
        items.put(src3.getIdentifier(), ItemHelper.convert(src3));
        
        Set expected = new HashSet();
        expected.add(src2);
        expected.add(src3);
        
        ItemReader reader = new MockItemReader(items);
        
        List itemsFound = reader.getItemsByPath(
                new ItemPath("(Address <- LtdCompany.address).departments", srcNs),
                ItemHelper.convert(src4));
        assertEquals(2, itemsFound.size());
        Set setFound = new HashSet(ItemHelper.convertFromFullDataItems(itemsFound));
        
        assertEquals(expected, setFound);
    }
    
    public void testReverseReferencePath() throws Exception {
        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        Item src2 = itemFactory.makeItem();
        src2.setIdentifier("2");
        src2.setClassName(srcNs + "Address");
        Reference r1 = new Reference();
        r1.setName("address");
        r1.setRefId("2");
        src1.addReference(r1);
        
        Map items = new HashMap();
        items.put(src1.getIdentifier(), ItemHelper.convert(src1));
        items.put(src2.getIdentifier(), ItemHelper.convert(src2));
        
        ItemReader reader = new MockItemReader(items);
        // start from address and get LtdCompany whose address reference points to starting point
        Item company = ItemHelper.convert(
                reader.getItemByPath(new ItemPath("(Address <- LtdCompany.address)", srcNs),
                        ItemHelper.convert(src2)));
        
        assertEquals(src1, company);
    }
    
    public void testReverseReferenceThenReferencePath() throws Exception {
        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        Item src2 = itemFactory.makeItem();
        src2.setIdentifier("2");
        src2.setClassName(srcNs + "Address");
        Reference r1 = new Reference();
        r1.setName("address");
        r1.setRefId("2");
        src1.addReference(r1);
        
        Map items = new HashMap();
        items.put(src1.getIdentifier(), ItemHelper.convert(src1));
        items.put(src2.getIdentifier(), ItemHelper.convert(src2));
        
        ItemReader reader = new MockItemReader(items);
        // start from address and get LtdCompany whose address reference points to starting point
        Item address = ItemHelper.convert(
                reader.getItemByPath(new ItemPath("(Address <- LtdCompany.address).address", srcNs),
                        ItemHelper.convert(src2)));
        
        assertEquals(src2, address);
    }
    
    public void testNestedReverseReferencePath() throws Exception {
        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        
        Item src2 = itemFactory.makeItem();
        src2.setIdentifier("2");
        src2.setClassName(srcNs + "Address");
        
        Reference r1 = new Reference();
        r1.setName("address");
        r1.setRefId("2");
        src1.addReference(r1);
        
        Item src3 = itemFactory.makeItem();
        src3.setIdentifier("3");
        src3.setClassName(srcNs + "CEO");
        
        Reference r2 = new Reference();
        r2.setName("workAddress");
        r2.setRefId("2");
        src3.addReference(r2);
        
        // Another Address object
        Item src4 = itemFactory.makeItem();
        src4.setIdentifier("4");
        src4.setClassName(srcNs + "Address");
        // CEO.homeAddress
        Reference r3 = new Reference();
        r3.setName("homeAddress");
        r3.setRefId("4");
        src3.addReference(r3);
        
        Map items = new HashMap();
        items.put(src1.getIdentifier(), ItemHelper.convert(src1));
        items.put(src2.getIdentifier(), ItemHelper.convert(src2));
        items.put(src3.getIdentifier(), ItemHelper.convert(src3));
        items.put(src4.getIdentifier(), ItemHelper.convert(src4));
        
        ItemReader reader = new MockItemReader(items);
        // start from address and get LtdCompany whose address reference points to starting point
        Item address = ItemHelper.convert(reader.getItemByPath(
                new ItemPath("((Address <- LtdCompany.address).address <- CEO.workAddress).homeAddress",
                        srcNs), ItemHelper.convert(src2)));
        
        assertEquals(src4, address);
    }
    
    public void testConstrainSingleItemToField() throws Exception {
        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        
        Item src2 = itemFactory.makeItem();
        src2.setIdentifier("2");
        src2.setClassName(srcNs + "CEO");
        
        Item src3 = itemFactory.makeItem();
        src3.setIdentifier("3");
        src3.setClassName(srcNs + "Address");
        
        Reference r1 = new Reference();
        r1.setName("ceo");
        r1.setRefId("2");
        src1.addReference(r1);
        
        Reference r2 = new Reference();
        r2.setName("address");
        r2.setRefId("3");
        src2.addReference(r2);
        
        src3.addAttribute(new Attribute("address", "Somewhere"));
        src3.addAttribute(new Attribute("postcode", "ABC"));
        
        Map items = new HashMap();
        items.put(src1.getIdentifier(), ItemHelper.convert(src1));
        items.put(src2.getIdentifier(), ItemHelper.convert(src2));
        items.put(src3.getIdentifier(), ItemHelper.convert(src3));
        
        ItemReader reader = new MockItemReader(items);
        
        Item address = ItemHelper.convert(reader.getItemByPath(
                new ItemPath("LtdCompany.ceo.address[address='Somewhere']", srcNs),
                ItemHelper.convert(src1)));
        assertEquals(src3, address);
    }
    
    public void testConstraintSingleItemFromCollection() throws Exception {
        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        
        ReferenceList r1 = new ReferenceList();
        r1.setName("departments");
        r1.addRefId("2");
        r1.addRefId("3");
        src1.addCollection(r1);
        
        Item src2 = itemFactory.makeItem();
        src2.setIdentifier("2");
        src2.setClassName(srcNs + "Department");
        src2.addAttribute(new Attribute("name", "A"));
        Item src3 = itemFactory.makeItem();
        src3.setIdentifier("3");
        src3.setClassName(srcNs + "Department");
        src3.addAttribute(new Attribute("name", "B"));
        
        Map items = new HashMap();
        items.put(src1.getIdentifier(), ItemHelper.convert(src1));
        items.put(src2.getIdentifier(), ItemHelper.convert(src2));
        items.put(src3.getIdentifier(), ItemHelper.convert(src3));
        
        ItemReader reader = new MockItemReader(items);
        
        Item address = ItemHelper.convert(reader.getItemByPath(
                new ItemPath("LtdCompany.departments[name='B']", srcNs),
                ItemHelper.convert(src1)));
    }
    
    public void test2ConstraintsSingleItemFromCollection() throws Exception {
        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        
        ReferenceList r1 = new ReferenceList();
        r1.setName("departments");
        r1.addRefId("2");
        r1.addRefId("3");
        src1.addCollection(r1);
        
        Item src2 = itemFactory.makeItem();
        src2.setIdentifier("2");
        src2.setClassName(srcNs + "Department");
        src2.addAttribute(new Attribute("name", "A"));
        src2.addAttribute(new Attribute("thing", "1"));
        Item src3 = itemFactory.makeItem();
        src3.setIdentifier("3");
        src3.setClassName(srcNs + "Department");
        src3.addAttribute(new Attribute("name", "B"));
        src3.addAttribute(new Attribute("thing", "2"));
        Item src4 = itemFactory.makeItem();
        src4.setIdentifier("4");
        src4.setClassName(srcNs + "Manager");
        src4.addAttribute(new Attribute("name", "Bob"));
        
        Reference r = new Reference();
        r.setName("manager");
        r.setRefId("4");
        src2.addReference(r);
        r = new Reference();
        r.setName("manager");
        r.setRefId("4");
        src3.addReference(r);
        
        Map items = new HashMap();
        items.put(src1.getIdentifier(), ItemHelper.convert(src1));
        items.put(src2.getIdentifier(), ItemHelper.convert(src2));
        items.put(src3.getIdentifier(), ItemHelper.convert(src3));
        items.put(src4.getIdentifier(), ItemHelper.convert(src4));
        
        ItemReader reader = new MockItemReader(items);
        
        Item item = ItemHelper.convert(reader.getItemByPath(
                new ItemPath("LtdCompany.departments[name='B' && thing='2']", srcNs),
                ItemHelper.convert(src1)));
        assertEquals(src3, item);
        item = ItemHelper.convert(reader.getItemByPath(
                new ItemPath("LtdCompany.departments[name='B' && thing='2' && manager.name='Bob']", srcNs),
                ItemHelper.convert(src1)));
        assertEquals(src3, item);
        
        assertNull(reader.getItemByPath(
                new ItemPath("LtdCompany.departments[name='B' && thing='2' && manager.name='asdf']", srcNs),
                ItemHelper.convert(src1)));
    }
    

    public void testConstraintSubPath() throws Exception {
        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        src1.setClassName(srcNs + "LtdCompany");
        
        Item src2 = itemFactory.makeItem();
        src2.setIdentifier("2");
        src2.setClassName(srcNs + "CEO");
        
        Item src3 = itemFactory.makeItem();
        src3.setIdentifier("3");
        src3.setClassName(srcNs + "Address");
        
        Reference r1 = new Reference();
        r1.setName("ceo");
        r1.setRefId("2");
        src1.addReference(r1);
        
        Reference r2 = new Reference();
        r2.setName("address");
        r2.setRefId("3");
        src2.addReference(r2);
        
        src3.addAttribute(new Attribute("address", "Somewhere"));
        src3.addAttribute(new Attribute("postcode", "ABC"));
        src2.addAttribute(new Attribute("name", "Bob"));
        
        Map items = new HashMap();
        items.put(src1.getIdentifier(), ItemHelper.convert(src1));
        items.put(src2.getIdentifier(), ItemHelper.convert(src2));
        items.put(src3.getIdentifier(), ItemHelper.convert(src3));
        
        ItemReader reader = new MockItemReader(items);
        
        // test single path constraint
        Item ceo = ItemHelper.convert(reader.getItemByPath(
                new ItemPath("LtdCompany.ceo[address.address='Somewhere']", srcNs),
                ItemHelper.convert(src1)));
        assertEquals(src2, ceo);
        
        // test path constraint that won't work out
        assertNull(reader.getItemByPath(
                new ItemPath("LtdCompany.ceo[address.address='Nowhere']", srcNs),
                ItemHelper.convert(src1)));
        
        // test two path constraints
        ceo = ItemHelper.convert(reader.getItemByPath(
                new ItemPath("LtdCompany.ceo[address.address='Somewhere' && address.postcode='ABC']", srcNs),
                ItemHelper.convert(src1)));
        assertEquals(src2, ceo);
        
//      test two path constraints
        Item address = ItemHelper.convert(reader.getItemByPath(
                new ItemPath("LtdCompany.ceo[address.address='Somewhere' && address.postcode='ABC'].address", srcNs),
                ItemHelper.convert(src1)));
        assertEquals(src3, address);
        
        // add a field attribute
        address = ItemHelper.convert(reader.getItemByPath(
                new ItemPath("LtdCompany.ceo[address.address='Somewhere' && address.postcode='ABC' && name='Bob'].address", srcNs),
                ItemHelper.convert(src1)));
        assertEquals(src3, address);
        
        
        // test fail
        assertNull(reader.getItemByPath(
                new ItemPath("LtdCompany.ceo[address.address='Somewhere' && address.postcode='XYZ']", srcNs),
                ItemHelper.convert(src1)));
    }
    
    public void testVariableConstraint() throws Exception {
        Item src1 = itemFactory.makeItem();
        src1.setIdentifier("1");
        
        src1.setClassName(srcNs + "LtdCompany");
        
        Item src2 = itemFactory.makeItem();
        src2.setIdentifier("2");
        src2.setClassName(srcNs + "CEO");
        
        Item src3 = itemFactory.makeItem();
        src3.setIdentifier("3");
        src3.setClassName(srcNs + "Address");
        
        Reference r1 = new Reference();
        r1.setName("ceo");
        r1.setRefId("2");
        src1.addReference(r1);
        
        Reference r2 = new Reference();
        r2.setName("address");
        r2.setRefId("3");
        src2.addReference(r2);
        
        src3.addAttribute(new Attribute("address", "Somewhere"));
        src3.addAttribute(new Attribute("postcode", "ABC"));
        src3.addAttribute(new Attribute("postcode2", "ABC"));
        src2.addAttribute(new Attribute("name", "Bob"));
        
        Map items = new HashMap();
        items.put(src1.getIdentifier(), ItemHelper.convert(src1));
        items.put(src2.getIdentifier(), ItemHelper.convert(src2));
        items.put(src3.getIdentifier(), ItemHelper.convert(src3));
        
        ItemReader reader = new MockItemReader(items);
        
        // single variable
        ItemPath path = new ItemPath("LtdCompany.ceo[address.postcode=$0]", srcNs);
        Item item = ItemHelper.convert(
                reader.getItemByPath(path, ItemHelper.convert(src1), new Object[]{"ABC"}));
        assertEquals(src2, item);
        
        // 2 placements of a single variable
        path = new ItemPath("LtdCompany.ceo[address.postcode=$0 && address.postcode2=$0]", srcNs);
        item = ItemHelper.convert(
                reader.getItemByPath(path, ItemHelper.convert(src1), new Object[]{"ABC"}));
        assertEquals(src2, item);
        
        assertNull(reader.getItemByPath(path, ItemHelper.convert(src1), new Object[]{"ZXV"}));
        
        // 2 variables
        path = new ItemPath("LtdCompany.ceo[address.postcode=$0 && address.postcode2=$1]", srcNs);
        item = ItemHelper.convert(
                reader.getItemByPath(path, ItemHelper.convert(src1), new Object[]{"ABC", "ABC"}));
        assertEquals(src2, item);
        
        assertNull(reader.getItemByPath(path, ItemHelper.convert(src1), new Object[]{"ABC", "asdf"}));
        assertNull(reader.getItemByPath(path, ItemHelper.convert(src1), new Object[]{"asdf", "ABC"}));
        
        // test exceptions
        try {
            reader.getItemByPath(path, ItemHelper.convert(src1), new Object[]{"ABC"});
            fail("Expected IllegalArgumentException with too few variable values");
        } catch (IllegalArgumentException err) {
        }
        
        try {
            reader.getItemByPath(path, ItemHelper.convert(src1));
            fail("Expected IllegalArgumentException with too few variable values");
        } catch (IllegalArgumentException err) {
        }
    }
}
