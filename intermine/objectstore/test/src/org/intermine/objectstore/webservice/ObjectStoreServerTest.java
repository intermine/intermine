package org.intermine.objectstore.webservice;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.iql.IqlQuery;

public class ObjectStoreServerTest extends TestCase
{

    protected static ObjectStoreServer server;

    public ObjectStoreServerTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        server = new ObjectStoreServer();
    }

    public void testRegisterQueryObject() throws Exception {
        IqlQuery q = new IqlQuery("select a1_ from Company as a1_", "org.intermine.model.testmodel");

        assertEquals(1, server.registerQuery(q));
        assertEquals(2, server.registerQuery(q));
    }

    public void testRegisterNullQueryObject() throws Exception {
        IqlQuery q = null;

        try {
            server.registerQuery(q);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testValidLookup() throws Exception {
        IqlQuery query = new IqlQuery("SELECT a1_ FROM Company AS a1_", "org.intermine.model.testmodel");
        int queryId = server.registerQuery(query);
        Query ret = server.lookupResults(queryId).getQuery();
        assertEquals("SELECT a1_ FROM org.intermine.model.testmodel.Company AS a1_", ret.toString());
    }

    public void testInvalidLookup() throws Exception {
        try {
            server.lookupResults(Integer.MAX_VALUE);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

}
