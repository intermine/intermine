package org.intermine.objectstore.fastcollections;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Method;
import java.util.*;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Contractor;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.SetupDataTestCase;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.DynamicUtil;
import org.intermine.util.XmlBinding;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ObjectStoreFastCollectionsImplTest
{
    private static Map data = new LinkedHashMap();
    private static Model model;

    private static ObjectStoreInterMineImpl osai;
    private static ObjectStoreFastCollectionsImpl os;

    private static Collection setUpData() throws Exception {
        XmlBinding binding = new XmlBinding(model);
        return binding.unmarshal(SetupDataTestCase.class.getClassLoader().getResourceAsStream("testmodel_data.xml"));
    }

    private static void setIds(Collection c) throws Exception {
        int i=1;
        Iterator iter = c.iterator();
        while (iter.hasNext()) {
            try {
                DynamicUtil.setFieldValue(iter.next(), "id", new Integer(i++));
            } catch (IllegalArgumentException e) {
            }
        }
    }

    private static Map map(Collection c) throws Exception {
        Map returnData = new LinkedHashMap();
        Iterator iter = c.iterator();
        while(iter.hasNext()) {
            Object o = iter.next();
            returnData.put(simpleObjectToName(o), o);
        }
        return returnData;
    }

    private static Object simpleObjectToName(Object o) throws Exception {
        Method name = null;
        try {
            name = o.getClass().getMethod("getName", new Class[] {});
        } catch (Exception e) {
            try {
                name = o.getClass().getMethod("getAddress", new Class[] {});
            } catch (Exception e2) {
            }
        }
        if (name != null) {
            return name.invoke(o, new Object[] {});
        } else if (o instanceof InterMineObject) {
            return new Integer(o.hashCode());
        } else {
            return o;
        }
    }

    private static void storeData() throws Exception {
        ObjectStoreWriter storeDataWriter = null;

        try {
            storeDataWriter = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
            storeDataWithWriter(storeDataWriter);
        } finally {
            if (storeDataWriter != null) {
                storeDataWriter.close();
            }
        }
    }

    private static void storeDataWithWriter(ObjectStoreWriter storeDataWriter) throws Exception {
        //checkIsEmpty();
        System.out.println("Storing data");
        long start = new Date().getTime();
        try {
            //Iterator iter = data.entrySet().iterator();
            //while (iter.hasNext()) {
            //    InterMineObject o = (InterMineObject) ((Map.Entry) iter.next())
            //        .getValue();
            //    o.setId(null);
            //}
            storeDataWriter.beginTransaction();
            Iterator iter = data.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                Object o = entry.getValue();
                storeDataWriter.store(o);
            }
            storeDataWriter.commitTransaction();
        } catch (Exception e) {
            storeDataWriter.abortTransaction();
            throw new Exception(e);
        }

        System.out.println("Took " + (new Date().getTime() - start) + " ms to set up data");
    }

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        osai = (ObjectStoreInterMineImpl) ObjectStoreFactory.getObjectStore("os.unittest");
        os = new ObjectStoreFastCollectionsImpl(osai);

        model = Model.getInstanceByName("testmodel/testmodel");
        Collection col = setUpData();
        setIds(col);
        data = map(col);
        System.out.println(data.size() + " entries in data map");
        storeData();
        //ObjectStoreAbstractImplTestCase.oneTimeSetUp();
    }

    @Test
    public void testLazyCollectionMtoN() throws Exception {
        ((ObjectStoreFastCollectionsImpl) os).setFetchFields(true, Collections.EMPTY_SET);
        // query for company and check contractors
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        //QueryField f1 = new QueryField(c1, "name");
        //QueryValue v1 = new QueryValue("CompanyA");
        //SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.EQUALS, v1);
        //q1.setConstraint(sc1);
        Results r  = os.execute(q1);
        ResultsRow rr = (ResultsRow) r.get(0);
        Company c = (Company) rr.get(0);
        Collection coll = c.getContractors();
        Assert.assertTrue("Expected " + coll.getClass() + " to be a ProxyCollection object", coll instanceof ProxyCollection);
        Assert.assertNotNull("Expected collection to be materialised", ((ProxyCollection) coll).getMaterialisedCollection());
        Assert.assertTrue("Expected materialised collection to be a HashSet, but was " + ((ProxyCollection) coll).getMaterialisedCollection().getClass(), ((ProxyCollection) coll).getMaterialisedCollection() instanceof HashSet);
        Set contractors = new HashSet(coll);
        Set expected1 = new HashSet();
        expected1.add(data.get("ContractorA"));
        expected1.add(data.get("ContractorB"));
        Assert.assertEquals(expected1, contractors);

        Contractor contractor1 = (Contractor) contractors.iterator().next();
        Collection subColl = contractor1.getCompanys();
        Assert.assertTrue("Expected " + subColl.getClass() + " to be a ProxyCollection object", subColl instanceof ProxyCollection);
        Collection matColl = ((ProxyCollection) subColl).getMaterialisedCollection();
        Assert.assertNull("Expected collection to not be materialised, but was: " + (matColl == null ? "" : "" + matColl.getClass()), matColl);
        Set expected2 = new HashSet();
        expected2.add(data.get("CompanyA"));
        expected2.add(data.get("CompanyB"));
        Assert.assertEquals(expected2, new HashSet(contractor1.getCompanys()));
    }

    public void testLazyCollectionMtoN2() throws Exception {
        os.flushObjectById();
        FieldDescriptor fd = os.getModel().getClassDescriptorByName("org.intermine.model.testmodel.Company").getCollectionDescriptorByName("contractors");
        Assert.assertNotNull(fd);
        ((ObjectStoreFastCollectionsImpl) os).setFetchFields(true, Collections.singleton(fd));
        // query for company and check contractors
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        Results r  = os.execute(q1);
        ResultsRow rr = (ResultsRow) r.get(0);
        Company c = (Company) rr.get(0);
        Collection coll = c.getContractors();
        Assert.assertTrue("Expected " + coll.getClass() + " to be a ProxyCollection object", coll instanceof ProxyCollection);
        Collection matColl = ((ProxyCollection) coll).getMaterialisedCollection();
        Assert.assertNull("Expected collection to not be materialised, but was: " + (matColl == null ? "" : "" + matColl.getClass()), matColl);
        coll = c.getDepartments();
        Assert.assertTrue("Expected " + coll.getClass() + " to be a ProxyCollection object", coll instanceof ProxyCollection);
        matColl = ((ProxyCollection) coll).getMaterialisedCollection();
        Assert.assertNotNull("Expected collection to be materialised", matColl);
    }
}
