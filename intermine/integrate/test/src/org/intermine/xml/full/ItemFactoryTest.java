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

import org.intermine.metadata.Model;

/**
 * ItemFactoryTest class
 *
 * @author Kim Rutherford
 */

public class ItemFactoryTest extends TestCase
{
    Model model;

    public ItemFactoryTest(String arg) throws Exception {
        super(arg);

        model = Model.getInstanceByName("testmodel");
    }

    public void testMakeItem1() throws Exception {
        ItemFactory itemFactory1 = new ItemFactory();
        
        Item item1FromItemFactory1 = itemFactory1.makeItem();
        Item item2FromItemFactory1 = itemFactory1.makeItem();
        Item item3FromItemFactory1 = itemFactory1.makeItem("some_id_from_itemFactory1");
        Item item4FromItemFactory1 = itemFactory1.makeItem();
        
        assertEquals("1", item1FromItemFactory1.getIdentifier());
        assertEquals("2", item2FromItemFactory1.getIdentifier());
        assertEquals("some_id_from_itemFactory1", item3FromItemFactory1.getIdentifier());
        assertEquals("3", item4FromItemFactory1.getIdentifier());

        ItemFactory itemFactory2 = new ItemFactory(model);

        Item item1FromItemFactory2 = itemFactory2.makeItem();
        Item item2FromItemFactory2 = itemFactory2.makeItem();
        Item item3FromItemFactory2 = itemFactory2.makeItem("some_id_from_itemFactory2");
        Item item4FromItemFactory2 = itemFactory2.makeItem();
        
        assertEquals("1", item1FromItemFactory2.getIdentifier());
        assertEquals("2", item2FromItemFactory2.getIdentifier());
        assertEquals("some_id_from_itemFactory2", item3FromItemFactory2.getIdentifier());
        assertEquals("3", item4FromItemFactory2.getIdentifier());

        ItemFactory itemFactory3 = new ItemFactory(model, "prefix_");

        Item item1FromItemFactory3 = itemFactory3.makeItem();
        Item item2FromItemFactory3 = itemFactory3.makeItem();
        Item item3FromItemFactory3 = itemFactory3.makeItem("some_id_from_itemFactory3");
        Item item4FromItemFactory3 = itemFactory3.makeItem();
        
        assertEquals("prefix_1", item1FromItemFactory3.getIdentifier());
        assertEquals("prefix_2", item2FromItemFactory3.getIdentifier());
        assertEquals("some_id_from_itemFactory3", item3FromItemFactory3.getIdentifier());
        assertEquals("prefix_3", item4FromItemFactory3.getIdentifier());

        ItemFactory itemFactory4 = new ItemFactory(null, "prefix_");

        Item item1FromItemFactory4 = itemFactory4.makeItem();
        Item item2FromItemFactory4 = itemFactory4.makeItem();
        Item item3FromItemFactory4 = itemFactory4.makeItem("some_id_from_itemFactory4");
        Item item4FromItemFactory4 = itemFactory4.makeItem();
        
        assertEquals("prefix_1", item1FromItemFactory4.getIdentifier());
        assertEquals("prefix_2", item2FromItemFactory4.getIdentifier());
        assertEquals("some_id_from_itemFactory4", item3FromItemFactory4.getIdentifier());
        assertEquals("prefix_3", item4FromItemFactory4.getIdentifier());
    }

    public void testMakeItem2() throws Exception {
        ItemFactory itemFactory = new ItemFactory(model);

        try {
            Item item = itemFactory.makeItem("my_id1",
                                             "http://www.intermine.org/model/testmodel#Foo", "");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
        
        try {
            Item item = itemFactory.makeItemForClass("http://www.intermine.org/model/testmodel#Foo");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

    }
}
