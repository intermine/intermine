package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;

import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStoreAbstractImplTestCase;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCloner;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;

public class ObjectStoreInterMineImplTest extends ObjectStoreAbstractImplTestCase
{
    public static void oneTimeSetUp() throws Exception {
        os = (ObjectStoreInterMineImpl) ObjectStoreFactory.getObjectStore("os.unittest");
        ObjectStoreAbstractImplTestCase.oneTimeSetUp();
    }

    public ObjectStoreInterMineImplTest(String arg) throws Exception {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(ObjectStoreInterMineImplTest.class);
    }

    public void testLargeOffset() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        Query q2 = QueryCloner.cloneQuery(q);
        SingletonResults r = new SingletonResults(q, os, os.getSequence());
        r.setBatchSize(2);
        InterMineObject o = (InterMineObject) r.get(5);
        SqlGenerator.registerOffset(q2, 6, ((ObjectStoreInterMineImpl) os).getSchema(), ((ObjectStoreInterMineImpl) os).db, o.getId());
        SingletonResults r2 = new SingletonResults(q2, os, os.getSequence());
        r2.setBatchSize(2);

        Query q3 = QueryCloner.cloneQuery(q);
        SqlGenerator.registerOffset(q3, 5, ((ObjectStoreInterMineImpl) os).getSchema(), ((ObjectStoreInterMineImpl) os).db, o.getId());
        SingletonResults r3 = new SingletonResults(q3, os, os.getSequence());
        r3.setBatchSize(2);

        assertEquals(r, r2);
        assertTrue(!r.equals(r3));
    }
}

