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

import java.util.List;

import org.flymine.model.testmodel.*;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryTestCase;
import org.flymine.objectstore.query.fql.FqlQuery;
import org.flymine.objectstore.ObjectStoreException;

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
        FqlQuery q = new FqlQuery("select a1_ from Company as a1_", "org.flymine.model.testmodel");

        assertEquals(1, server.registerQuery(q));
        assertEquals(2, server.registerQuery(q));
    }

    public void testRegisterNullQueryObject() throws Exception {
        FqlQuery q = null;

        try {
            server.registerQuery(q);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testValidLookup() throws Exception {
        FqlQuery query = new FqlQuery("SELECT a1_ FROM Company AS a1_", "org.flymine.model.testmodel");
        int queryId = server.registerQuery(query);
        Query ret = server.lookupQuery(queryId);
        assertEquals("SELECT a1_ FROM org.flymine.model.testmodel.Company AS a1_", ret.toString());
    }

    public void testInvalidLookup() throws Exception {
        try {
            server.lookupQuery(Integer.MAX_VALUE);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

}
