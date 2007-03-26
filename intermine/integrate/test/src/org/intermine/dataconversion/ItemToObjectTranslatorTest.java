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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.fulldata.Attribute;
import org.intermine.model.fulldata.Item;
import org.intermine.model.fulldata.Reference;
import org.intermine.model.fulldata.ReferenceList;
import org.intermine.model.testmodel.Broke;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryTestCase;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicBean;


public class ItemToObjectTranslatorTest extends QueryTestCase
{
    protected ItemToObjectTranslator translator;

    public static Test suite() {
        return buildSuite(ItemToObjectTranslatorTest.class);
    }

    public ItemToObjectTranslatorTest(String arg1) {
        super(arg1);
    }

    public static void oneTimeSetUp() throws Exception {
        QueryTestCase.oneTimeSetUp();
    }

    public void setUp() throws Exception {
        super.setUp();
        translator = new ItemToObjectTranslator(Model.getInstanceByName("testmodel"), null);
        translator.setObjectStore(ObjectStoreFactory.getObjectStore("os.unittest"));
        translator.idToNamespace.put(new Integer(0), "fish");
        translator.namespaceToId.put("fish", new Integer(0));
    }

    public void testTranslateQueryNoConstraint() throws Exception {
        Query expected = new Query();
        QueryClass qc = new QueryClass(Item.class);
        expected.addFrom(qc);
        expected.addToSelect(qc);

        Query original = new Query();
        QueryClass qc2 = new QueryClass(InterMineObject.class);
        original.addFrom(qc2);
        original.addToSelect(qc2);

        assertEquals(expected, translator.translateQuery(original));
    }

    public void testTranslateQuerySimpleConstraint() throws Exception {
        Query expected = new Query();
        QueryClass qc = new QueryClass(Item.class);
        expected.addFrom(qc);
        expected.addToSelect(qc);
        QueryField qf = new QueryField(qc, "identifier");
        SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue("fish_42"));
        expected.setConstraint(sc);

        Query original = new Query();
        QueryClass qc2 = new QueryClass(InterMineObject.class);
        original.addFrom(qc2);
        original.addToSelect(qc2);
        QueryField qf2 = new QueryField(qc2, "id");
        SimpleConstraint sc2 = new SimpleConstraint(qf2, ConstraintOp.EQUALS, new QueryValue(new Integer(42)));
        original.setConstraint(sc2);

        assertEquals(expected, translator.translateQuery(original));
    }

    public void testTranslateQueryBagConstraint() throws Exception {
        Query expected = new Query();
        QueryClass qc = new QueryClass(Item.class);
        expected.addFrom(qc);
        expected.addToSelect(qc);
        QueryField qf = new QueryField(qc, "identifier");
        BagConstraint bc = new BagConstraint(qf, ConstraintOp.IN, Arrays.asList(new Object[] {"fish_12", "fish_15", "fish_19"}));
        expected.setConstraint(bc);

        Query original = new Query();
        QueryClass qc2 = new QueryClass(InterMineObject.class);
        original.addFrom(qc2);
        original.addToSelect(qc2);
        QueryField qf2 = new QueryField(qc2, "id");
        BagConstraint bc2 = new BagConstraint(qf2, ConstraintOp.IN, Arrays.asList(new Object[] {new Integer(12), new Integer(15), new Integer(19)}));
        original.setConstraint(bc2);

        assertEquals(expected, translator.translateQuery(original));
    }

    public void testTranslateQuerySpecificClass() throws Exception {
        Query expected = new Query();
        QueryClass qc = new QueryClass(Item.class);
        expected.addFrom(qc);
        expected.addToSelect(qc);
        QueryField qf = new QueryField(qc, "className");
        SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.EQUALS,
                       new QueryValue("http://www.intermine.org/model/testmodel#Department"));
        expected.setConstraint(sc);

        Query original = new Query();
        QueryClass qc2 = new QueryClass(Department.class);
        original.addFrom(qc2);
        original.addToSelect(qc2);

        assertEquals(expected, translator.translateQuery(original));
    }

    public void testTranslateQuerySpecificClassPlus() throws Exception {
        Query expected = new Query();
        QueryClass qc = new QueryClass(Item.class);
        expected.addFrom(qc);
        expected.addToSelect(qc);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryField qf2 = new QueryField(qc, "identifier");
        SimpleConstraint sc2 = new SimpleConstraint(qf2, ConstraintOp.EQUALS, new QueryValue("fish_42"));
        cs.addConstraint(sc2);
        QueryField qf1 = new QueryField(qc, "className");
        SimpleConstraint sc1 = new SimpleConstraint(qf1, ConstraintOp.EQUALS,
                       new QueryValue("http://www.intermine.org/model/testmodel#Department"));
        cs.addConstraint(sc1);
        expected.setConstraint(cs);

        Query original = new Query();
        QueryClass qc2 = new QueryClass(Department.class);
        original.addFrom(qc2);
        original.addToSelect(qc2);
        QueryField qf3 = new QueryField(qc2, "id");
        SimpleConstraint sc3 = new SimpleConstraint(qf3, ConstraintOp.EQUALS, new QueryValue(new Integer(42)));
        original.setConstraint(sc3);

        assertEquals(expected, translator.translateQuery(original));
    }

    public void testTranslateQueryInvalid() throws Exception {
        Query original = new Query();
        QueryClass qc2 = new QueryClass(Company.class);
        original.addFrom(qc2);
        original.addToSelect(qc2);

        try {
            translator.translateQuery(original);
            fail("Expected ObjectStoreException");
        } catch (ObjectStoreException e) {
        }
    }

    public void testTranslateFromDbObject() throws Exception {
        Item dbItem = new Item();
        dbItem.setClassName("http://www.intermine.org/model/testmodel#Department");
        dbItem.setImplementations("http://www.intermine.org/model/testmodel#Broke");
        dbItem.setIdentifier("fish_1");
        Attribute dbAttr1 = new  Attribute();
        dbAttr1.setName("name");
        dbAttr1.setValue("Department1");
        dbAttr1.setItem(dbItem);
        dbItem.addAttributes(dbAttr1);
        Attribute dbAttr2 = new  Attribute();
        dbAttr2.setName("debt");
        dbAttr2.setValue("10");
        dbAttr2.setItem(dbItem);
        dbItem.addAttributes(dbAttr2);
        Reference dbRef1 = new  Reference();
        dbRef1.setName("company");
        dbRef1.setRefId("2");
        dbRef1.setItem(dbItem);
        dbItem.addReferences(dbRef1);
        ReferenceList dbCol1 = new  ReferenceList();
        dbCol1.setName("employees");
        dbCol1.setRefIds("3 4");
        dbCol1.setItem(dbItem);
        dbItem.addCollections(dbCol1);

        InterMineObject result = translator.translateFromDbObject(dbItem);
        assertTrue(result instanceof Department);
        assertTrue(result instanceof Broke);

        assertEquals("Department1", ((Department) result).getName());
        assertEquals(10, ((Broke) result).getDebt());

        Map fieldMap = ((DynamicBean) ((net.sf.cglib.proxy.Factory) result).getCallback(0)).getMap();
        ProxyReference o = (ProxyReference) fieldMap.get("Company");
        assertNotNull(o);
        assertEquals(new Integer(2), o.getId());

        Collection c = ((Department) result).getEmployees();
        assertTrue(c instanceof SingletonResults);
        Query expected = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        expected.addFrom(qc);
        expected.addToSelect(qc);
        QueryField qf = new QueryField(qc, "id");
        Set ids = new HashSet();
        ids.add(new Integer(3));
        ids.add(new Integer(4));
        BagConstraint bc = new BagConstraint(qf, ConstraintOp.IN, ids);
        expected.setConstraint(bc);
        assertEquals(expected, ((SingletonResults) c).getQuery());
    }
}
