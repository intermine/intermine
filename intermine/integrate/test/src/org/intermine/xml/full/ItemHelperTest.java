package org.intermine.xml.full;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;

import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Department;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.proxy.ProxyReference;

import junit.framework.TestCase;

public class ItemHelperTest extends TestCase
{
    Item item;
    org.intermine.model.fulldata.Item dbItem;

    ReferenceList referenceList;
    org.intermine.model.fulldata.ReferenceList dbReferenceList;
    ProxyReference departmentProxy;

    ObjectStore os;

    public void setUp() throws Exception {
        os = ObjectStoreFactory.getObjectStore("os.unittest");

        item = new Item();
        item.setClassName("Department");
        item.setImplementations("Broke");
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

        dbItem = new org.intermine.model.fulldata.Item();
        dbItem.setClassName("Department");
        dbItem.setImplementations("Broke");
        dbItem.setIdentifier("1");
        dbItem.setId(1001);
        org.intermine.model.fulldata.Attribute dbAttr1 = new  org.intermine.model.fulldata.Attribute();
        dbAttr1.setName("name");
        dbAttr1.setValue("Department1");
        dbAttr1.setItem(dbItem);
        dbItem.addAttributes(dbAttr1);
        org.intermine.model.fulldata.Attribute dbAttr2 = new  org.intermine.model.fulldata.Attribute();
        dbAttr2.setName("debt");
        dbAttr2.setValue("10");
        dbAttr2.setItem(dbItem);
        dbItem.addAttributes(dbAttr2);
        org.intermine.model.fulldata.Reference dbRef1 = new  org.intermine.model.fulldata.Reference();
        dbRef1.setName("address");
        dbRef1.setRefId("2");
        dbRef1.setItem(dbItem);
        dbItem.addReferences(dbRef1);
        org.intermine.model.fulldata.ReferenceList dbCol1 = new  org.intermine.model.fulldata.ReferenceList();
        dbCol1.setName("employees");
        dbCol1.setRefIds("3 4");
        dbCol1.setItem(dbItem);
        dbItem.addCollections(dbCol1);

        referenceList = new ReferenceList();
        referenceList.setName("employees");
        referenceList.setRefIds(Arrays.asList("3", "4"));

        dbReferenceList = new org.intermine.model.fulldata.ReferenceList();
        dbReferenceList.setName("employees");
        dbReferenceList.setRefIds("3 4");
        departmentProxy = new ProxyReference(os, 1, Department.class);
        dbReferenceList.proxyItem(departmentProxy);
        //dbReferenceList.setId(2002);
    }

    public void testConvertFromDbItem() throws Exception {
        assertEquals(item, ItemHelper.convert(dbItem));
    }

    public void testConvertToDbItem() throws Exception {
        assertEquals(item, ItemHelper.convert(ItemHelper.convert(item)));
    }

    public void testConvertReferenceList() throws Exception {
        org.intermine.model.fulldata.ReferenceList resRefList = ItemHelper.convert(referenceList);
        resRefList.proxyItem(departmentProxy);

        // referencelists are simple objects so don't have .equals() on id like InterMineObjects
        // need to check each field
        assertEquals(dbReferenceList.getName(), resRefList.getName());
        assertEquals(dbReferenceList.getFieldProxy("item"), resRefList.getFieldProxy("item"));
        assertEquals(dbReferenceList.getRefIds(), resRefList.getRefIds());
    }

    public void testGenerateClassNamesNull() throws Exception {
        assertNull(ItemHelper.generateClassNames(null, null));
    }

    public void testGenerateClassNamesEmpty() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        assertEquals("", ItemHelper.generateClassNames("", model));
    }

    public void testGenerateClassNamesSingle() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        assertEquals("org.intermine.model.testmodel.Company", ItemHelper.generateClassNames("Company", model));
    }

    public void testGenerateClassNamesMultiple() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        String classNames = " Company Department ";
        String expected = "org.intermine.model.testmodel.Company org.intermine.model.testmodel.Department";
        assertEquals(expected, ItemHelper.generateClassNames(classNames, model));
    }
}
