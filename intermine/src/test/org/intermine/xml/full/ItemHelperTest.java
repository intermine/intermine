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

public class ItemHelperTest extends TestCase
{
    Item item;
    org.flymine.model.fulldata.Item dbItem;

    public void setUp() throws Exception {
        item = new Item();
        item.setClassName("http://www.flymine.org/model/testmodel#Department");
        item.setImplementations("http://www.flymine.org/model/testmodel#Broke");
        item.setIdentifier("1");
        Attribute attr1 = new Attribute();
        attr1.setName("name");
        attr1.setValue("Department1");
        item.addAttribute(attr1);
        Attribute attr2 = new Attribute();
        attr2.setName("debt");
        attr2.setValue("10");
        item.addAttribute(attr2);
        Reference ref1 = new Reference();
        ref1.setName("address");
        ref1.setRefId("2");
        item.addReference(ref1);
        ReferenceList col1 = new ReferenceList();
        col1.setName("employees");
        col1.addRefId("3");
        col1.addRefId("4");
        item.addCollection(col1);

        dbItem = new org.flymine.model.fulldata.Item();
        dbItem.setClassName("http://www.flymine.org/model/testmodel#Department");
        dbItem.setImplementations("http://www.flymine.org/model/testmodel#Broke");
        dbItem.setIdentifier("1");
        org.flymine.model.fulldata.Attribute dbAttr1 = new  org.flymine.model.fulldata.Attribute();
        dbAttr1.setName("name");
        dbAttr1.setValue("Department1");
        dbItem.addAttributes(dbAttr1);
        org.flymine.model.fulldata.Attribute dbAttr2 = new  org.flymine.model.fulldata.Attribute();
        dbAttr2.setName("debt");
        dbAttr2.setValue("10");
        dbItem.addAttributes(dbAttr2);
        org.flymine.model.fulldata.Reference dbRef1 = new  org.flymine.model.fulldata.Reference();
        dbRef1.setName("address");
        dbRef1.setRefId("2");
        dbItem.addReferences(dbRef1);
        org.flymine.model.fulldata.ReferenceList dbCol1 = new  org.flymine.model.fulldata.ReferenceList();
        dbCol1.setName("employees");
        dbCol1.setRefIds("3 4");
        dbItem.addCollections(dbCol1);
    }

    public void testConvertFromDbItem() throws Exception {
        assertEquals(item, ItemHelper.convert(dbItem));
    } 

    public void testConvertToDbItem() throws Exception {
        assertEquals(item, ItemHelper.convert(ItemHelper.convert(item)));
    }
}
