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

import java.util.List;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.intermine.metadata.Model;



/**
 * Tests for the Item class.
 *
 * @author Kim Rutherford
 */

public class ItemTest extends TestCase
{
    Model model;
    
    public ItemTest(String arg) throws Exception {
        super(arg);
        model = Model.getInstanceByName("testmodel");
    }

    public void testCreateWithModel1() throws Exception {
        Item item1 = new Item();
        item1.setModel(model);
        item1.setImplementations("http://www.intermine.org/model/testmodel#Company");
        item1.setClassName("http://www.intermine.org/model/testmodel#Company");
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
    }

    public void testCreateWithWrongModel1() throws Exception {
        Item item1 = new Item();
        item1.setModel(model);
        try {
            item1.setImplementations("http://www.intermine.org/model/nottestmodel#Company");
            fail("expected RuntimeException");
        } catch (RuntimeException _) {
            // expected
        }
        try {
            item1.setClassName("http://www.intermine.org/model/nottestmodel#Company");
            fail("expected RuntimeException");
        } catch (RuntimeException _) {
            // expected
        }
        try {
            item1.setImplementations("http://www.intermine.org/model/testmodel#NotCompany");
            fail("expected RuntimeException");
        } catch (RuntimeException _) {
            // expected
        }
        try {
            item1.setClassName("http://www.intermine.org/model/testmodel#NotCompany");
            fail("expected RuntimeException");
        } catch (RuntimeException _) {
            // expected
        }
    }

    public void testCreateWithWrongModel2() throws Exception {
        Item item1 = new Item();
        item1.setModel(model);
        item1.setImplementations("http://www.intermine.org/model/testmodel#Company");
        item1.setClassName("http://www.intermine.org/model/testmodel#Company");
        item1.setIdentifier("1");
        Attribute attr1 = new Attribute();
        attr1.setName("not_name");
        attr1.setValue("Company1");
        try {
            item1.addAttribute(attr1);
            fail("expected RuntimeException");
        } catch (RuntimeException _) {
            // expected
        }
        Reference ref1 = new Reference();
        ref1.setName("not_address");
        ref1.setRefId("2");
        try {
            item1.addReference(ref1);
            fail("expected RuntimeException");
        } catch (RuntimeException _) {
            // expected
        }
        ReferenceList col1 = new ReferenceList();
        col1.setName("not_departments");
        col1.addRefId("3");
        col1.addRefId("4");
        try {
            item1.addCollection(col1);
            fail("expected RuntimeException");
        } catch (RuntimeException _) {
            // expected
        }
    }

    public void testCreateClassCheck() throws Exception {
        ItemFactory itemFactory = new ItemFactory(model);
        
        Item item1 = itemFactory.makeItem();
        
        item1.setClassName(model.getNameSpace() + "Company");

        try {
            item1.setClassName(model.getNameSpace() + "IllegalClass");
            fail("expected RuntimeException");
        } catch (RuntimeException _) {
            // expected
        }

        assertEquals(model.getNameSpace() + "Company", item1.getClassName());
    }

    public void testAddAttribute() throws Exception {
        ItemFactory itemFactory = new ItemFactory(model);
        
        Item item1 = itemFactory.makeItem();
        item1.setClassName(model.getNameSpace() + "Company");

        item1.addAttribute(new Attribute("vatNumber", "1000"));

        assertEquals("1000", item1.getAttribute("vatNumber").getValue());
        
        try {
            item1.addAttribute(new Attribute("illegalAttribute", "1000"));
            fail("expected RuntimeException");
        } catch (RuntimeException _) {
            // expected
        }

        item1.setAttribute("vatNumber", "2000");

        assertEquals("2000", item1.getAttribute("vatNumber").getValue());

        try {
            item1.setAttribute("illegalAttribute", "1000");
            fail("expected RuntimeException");
        } catch (RuntimeException _) {
            // expected
        }
    }

    public void testAddReference() throws Exception {
        ItemFactory itemFactory = new ItemFactory(model);
        
        Item item1 = itemFactory.makeItem();
        
        item1.setClassName(model.getNameSpace() + "Company");

        item1.addReference(new Reference("address", "address_id_1"));

        assertEquals("address_id_1", item1.getReference("address").getRefId());

        try {
            item1.addReference(new Reference("illegalReference", "illegal_id"));
            fail("expected RuntimeException");
        } catch (RuntimeException _) {
            // expected
        }

        item1.setReference("address", "address_id_2");

        assertEquals("address_id_2", item1.getReference("address").getRefId());

        try {
            item1.setReference("illegalReference", "illegal_id");
            fail("expected RuntimeException");
        } catch (RuntimeException _) {
            // expected
        }
        
        Item item2 = itemFactory.makeItem();
        
        item2.setClassName(model.getNameSpace() + "CEO");
        
        item2.setIdentifier("item_2");
        
        item1.setReference("CEO", item2);
        
        assertEquals(item1.getReference("CEO").getRefId(), "item_2");
        
    }

    public void testAddCollection() throws Exception {
        ItemFactory itemFactory = new ItemFactory(model);
        
        Item item1 = itemFactory.makeItem();
        
        item1.setClassName(model.getNameSpace() + "Company");

        item1.addToCollection("contractors", "contractor_id_1");
        item1.addToCollection("contractors", "contractor_id_2");
        item1.addToCollection("contractors", itemFactory.makeItem("contractor_id_3"));

        List resultContractors = item1.getCollection("contractors").getRefIds();

        assertEquals(3, resultContractors.size());        

        List expected = new ArrayList();

        expected.add("contractor_id_1");
        expected.add("contractor_id_2");
        expected.add("contractor_id_3");

        assertEquals(expected, resultContractors);

        try {
            item1.addToCollection("illegalCollection", "illegal_id");
            fail("expected RuntimeException");
        } catch (RuntimeException _) {
            // expected
        }
    }
    
    public void testAddCollectionShortCut() throws Exception {
        ItemFactory itemFactory = new ItemFactory(model);
        
        Item item1 = itemFactory.makeItem();
        
        item1.setClassName(model.getNameSpace() + "Company");

        List idsToAdd = new ArrayList();
        
        idsToAdd.add("contractor_id_1");
        idsToAdd.add("contractor_id_2");
        idsToAdd.add("contractor_id_3");
        
        item1.setCollection("contractors", idsToAdd);

        List resultContractors = item1.getCollection("contractors").getRefIds();

        assertEquals(3, resultContractors.size());        

        List expected = new ArrayList();

        expected.add("contractor_id_1");
        expected.add("contractor_id_2");
        expected.add("contractor_id_3");

        assertEquals(expected, resultContractors);

        try {
            item1.addToCollection("illegalCollection", "illegal_id");
            fail("expected RuntimeException");
        } catch (RuntimeException _) {
            // expected
        }
    }
    
    public void testAddCollectionShortCutWrongType() throws Exception {
        ItemFactory itemFactory = new ItemFactory(model);
        
        Item item1 = itemFactory.makeItem();
        
        item1.setClassName(model.getNameSpace() + "Company");

        List idsToAdd = new ArrayList();

        idsToAdd.add("contractor_id_1");
        idsToAdd.add(new Integer(10));
        
        try {
            item1.setCollection("contractors", idsToAdd);
            fail("expected RuntimeException");
        } catch (RuntimeException _) {
            // expected
        }
    }
}
