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
import org.flymine.objectstore.query.*;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.testing.OneTimeTestCase;

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
        Query q = new Query();

        assertEquals(1, server.registerQuery(q));
        assertEquals(2, server.registerQuery(q));
    }


    public void testRegisterQueryString() throws Exception {
        String queryString = "select a1_ from Company as a1_";
        String pkg = "org.flymine.model.testmodel";

        assertEquals(1, server.registerQuery(queryString, pkg));
        assertEquals(2, server.registerQuery(queryString, pkg));
    }

    public void testRegisterNullQueryObject() throws Exception {
        Query q = null;

        try {
            server.registerQuery(q);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testRegisterNullQueryString() throws Exception {
        String queryString = null;

        try {
            server.registerQuery(queryString, "org.flymine.model.testmodel");
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testRegisterEmptyQueryString() throws Exception {
        String queryString = "";

        try {
            server.registerQuery(queryString, "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testValidLookup() throws Exception {
        Query query = new Query();
        int queryId = server.registerQuery(query);
        Query ret = server.lookupQuery(queryId);
        assertTrue(ret == query);
    }

    public void testInvalidLookup() throws Exception {
        try {
            server.lookupQuery(Integer.MAX_VALUE);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

}
