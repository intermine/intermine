package org.intermine.objectstore.fastcollections;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.*;

import org.intermine.objectstore.ObjectStoreAbstractImpl;
import org.intermine.objectstore.ObjectStoreAbstractImplTestCase;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.proxy.Lazy;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.objectstore.query.SimpleConstraint;

import org.intermine.model.testmodel.*;

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
        assertTrue("Expected " + c.getContractors().getClass() + " to be an ArrayList object", c.getContractors() instanceof ArrayList);
        Set contractors = new HashSet(c.getContractors());
        Set expected1 = new HashSet();
        expected1.add(data.get("ContractorA"));
        expected1.add(data.get("ContractorB"));
        assertEquals(expected1, contractors);

        Contractor contractor1 = (Contractor) contractors.iterator().next();
        assertTrue("Expected " + contractor1.getCompanys().getClass() + " to be a Lazy object", contractor1.getCompanys() instanceof Lazy);
        Set expected2 = new HashSet();
        expected2.add(data.get("CompanyA"));
        expected2.add(data.get("CompanyB"));
        assertEquals(expected2, new HashSet(contractor1.getCompanys()));
    }

}
