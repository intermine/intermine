package org.flymine.objectstore.flymine;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.Test;

import org.flymine.model.FlyMineBusinessObject;
import org.flymine.objectstore.ObjectStoreAbstractImplTestCase;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryCloner;
import org.flymine.objectstore.query.SingletonResults;

public class ObjectStoreFlyMineImplTest extends ObjectStoreAbstractImplTestCase
{
    public static void oneTimeSetUp() throws Exception {
        os = (ObjectStoreFlyMineImpl) ObjectStoreFactory.getObjectStore("os.unittest");
        ObjectStoreAbstractImplTestCase.oneTimeSetUp();
    }

    public ObjectStoreFlyMineImplTest(String arg) throws Exception {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(ObjectStoreFlyMineImplTest.class);
    }

    public void testLargeOffset() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(FlyMineBusinessObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        Query q2 = QueryCloner.cloneQuery(q);
        SingletonResults r = new SingletonResults(q, os, os.getSequence());
        r.setBatchSize(2);
        FlyMineBusinessObject o = (FlyMineBusinessObject) r.get(5);
        SqlGenerator.registerOffset(q2, 6, os.getModel(), ((ObjectStoreFlyMineImpl) os).db, o.getId());
        SingletonResults r2 = new SingletonResults(q2, os, os.getSequence());
        r2.setBatchSize(2);

        Query q3 = QueryCloner.cloneQuery(q);
        SqlGenerator.registerOffset(q3, 5, os.getModel(), ((ObjectStoreFlyMineImpl) os).db, o.getId());
        SingletonResults r3 = new SingletonResults(q3, os, os.getSequence());
        r3.setBatchSize(2);

        assertEquals(r, r2);
        assertTrue(!r.equals(r3));
    }
}

