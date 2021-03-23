package org.intermine.objectstore.fastcollections;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.*;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Contractor;
import org.intermine.objectstore.*;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ObjectStoreFastCollectionsImplTest extends ObjectStoreAbstractImplTestCase
{
    private static ObjectStoreFastCollectionsImpl osFastCollections;

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        ObjectStoreAbstractImpl os = (ObjectStoreAbstractImpl)ObjectStoreFactory.getObjectStore("os.unittest");
        oneTimeSetUp(os, "osw.unittest", "testmodel", "testmodel_data.xml");
        osFastCollections = new ObjectStoreFastCollectionsImpl(os);
    }

    @Test
    public void testLazyCollectionMtoN() throws Exception {
        osFastCollections.setFetchFields(true, Collections.EMPTY_SET);
        // query for company and check contractors
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        //QueryField f1 = new QueryField(c1, "name");
        //QueryValue v1 = new QueryValue("CompanyA");
        //SimpleConstraint sc1 = new SimpleConstraint(f1, ConstraintOp.EQUALS, v1);
        //q1.setConstraint(sc1);
        Results r = osFastCollections.execute(q1);
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

    @Test
    public void testLazyCollectionMtoN2() throws Exception {
        osFastCollections.flushObjectById();
        FieldDescriptor fd
                = osFastCollections.getModel().getClassDescriptorByName(
                        "org.intermine.model.testmodel.Company").getCollectionDescriptorByName("contractors");
        Assert.assertNotNull(fd);
        osFastCollections.setFetchFields(true, Collections.singleton(fd));
        // query for company and check contractors
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        Results r = osFastCollections.execute(q1);
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
