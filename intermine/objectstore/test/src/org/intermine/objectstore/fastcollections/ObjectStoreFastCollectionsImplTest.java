package org.intermine.objectstore.fastcollections;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Contractor;
import org.intermine.objectstore.ObjectStoreAbstractImplTestCase;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.proxy.Lazy;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

public class ObjectStoreFastCollectionsImplTest extends ObjectStoreAbstractImplTestCase
{
    public static void oneTimeSetUp() throws Exception {
        osai = (ObjectStoreInterMineImpl) ObjectStoreFactory.getObjectStore("os.unittest");
        os = new ObjectStoreFastCollectionsImpl(osai);
        ObjectStoreAbstractImplTestCase.oneTimeSetUp();
    }

    public ObjectStoreFastCollectionsImplTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(ObjectStoreFastCollectionsImplTest.class);
    }

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
        assertTrue("Expected " + coll.getClass() + " to be a ProxyCollection object", coll instanceof ProxyCollection);
        assertNotNull("Expected collection to be materialised", ((ProxyCollection) coll).getMaterialisedCollection());
        assertTrue("Expected materialised collection to be a HashSet, but was " + ((ProxyCollection) coll).getMaterialisedCollection().getClass(), ((ProxyCollection) coll).getMaterialisedCollection() instanceof HashSet);
        Set contractors = new HashSet(coll);
        Set expected1 = new HashSet();
        expected1.add(data.get("ContractorA"));
        expected1.add(data.get("ContractorB"));
        assertEquals(expected1, contractors);

        Contractor contractor1 = (Contractor) contractors.iterator().next();
        Collection subColl = contractor1.getCompanys();
        assertTrue("Expected " + subColl.getClass() + " to be a ProxyCollection object", subColl instanceof ProxyCollection);
        Collection matColl = ((ProxyCollection) subColl).getMaterialisedCollection();
        assertNull("Expected collection to not be materialised, but was: " + (matColl == null ? "" : "" + matColl.getClass()), matColl);
        Set expected2 = new HashSet();
        expected2.add(data.get("CompanyA"));
        expected2.add(data.get("CompanyB"));
        assertEquals(expected2, new HashSet(contractor1.getCompanys()));
    }

    public void testLazyCollectionMtoN2() throws Exception {
        os.flushObjectById();
        FieldDescriptor fd = os.getModel().getClassDescriptorByName("org.intermine.model.testmodel.Company").getCollectionDescriptorByName("contractors");
        assertNotNull(fd);
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
        assertTrue("Expected " + coll.getClass() + " to be a ProxyCollection object", coll instanceof ProxyCollection);
        Collection matColl = ((ProxyCollection) coll).getMaterialisedCollection();
        assertNull("Expected collection to not be materialised, but was: " + (matColl == null ? "" : "" + matColl.getClass()), matColl);
        coll = c.getDepartments();
        assertTrue("Expected " + coll.getClass() + " to be a ProxyCollection object", coll instanceof ProxyCollection);
        matColl = ((ProxyCollection) coll).getMaterialisedCollection();
        assertNotNull("Expected collection to be materialised", matColl);
    }
}
