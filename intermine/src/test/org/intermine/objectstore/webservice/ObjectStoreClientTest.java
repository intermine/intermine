package org.flymine.objectstore.webservice;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import org.flymine.objectstore.ObjectStoreAbstractImplTestCase;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ObjectStoreTestCase;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.fql.FqlQuery;

public class ObjectStoreClientTest extends ObjectStoreAbstractImplTestCase
{
    public static void oneTimeSetUp() throws Exception {
        ObjectStoreTestCase.os = (ObjectStoreClient) ObjectStoreFactory.getObjectStore("os.unittest-client");
        ObjectStoreAbstractImplTestCase.oneTimeSetUp();
    }

    public ObjectStoreClientTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(ObjectStoreClientTest.class);
    }

    public void testQueryId() throws Exception {
        Query q1 = new FqlQuery("select a1_ from Company as a1_", "org.flymine.model.testmodel").toQuery();
        Query q2 = new FqlQuery("select a1_ from Company as a1_", "org.flymine.model.testmodel").toQuery();

        assertEquals(((ObjectStoreClient) os).getQueryId(q1), ((ObjectStoreClient) os).getQueryId(q1));
        assertEquals(((ObjectStoreClient) os).getQueryId(q2), ((ObjectStoreClient) os).getQueryId(q2));

        assertTrue(((ObjectStoreClient) os).getQueryId(q1) != ((ObjectStoreClient) os).getQueryId(q2));
    }

}
