package org.flymine.objectstore.query.fql;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;
import junit.framework.Test;

import java.util.HashMap;
import java.util.Map;

import org.flymine.objectstore.query.Query;
import org.flymine.testing.OneTimeTestCase;

public class FqlQueryTest extends FqlQueryTestCase
{
    public FqlQueryTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(FqlQueryTest.class);
    }

    public void executeTest(String type) throws Exception {
        Query orig = ((Query) queries.get(type));
        FqlQuery fq = (FqlQuery) results.get(type);

        if (fq == null) {
            fail("No results set up for test " + type);
        }

        // This is testing whether new FqlQuery(Query) gives the FqlQueries above
        FqlQuery fqGenerated = new FqlQuery(orig);
        assertEquals(type + " has failed", fq.getQueryString(), fqGenerated.getQueryString());;
        assertEquals(type + " has failed", fq.getParameters(), fqGenerated.getParameters());;

        // This is testing whether the FqlQueries above are parsed into the correct query
        // This really duplicates FqlQueryParserTest but here we call the FqlQuery.toQuery() method
        Query parsed = fq.toQuery();
        assertEquals(type + " has failed", orig, parsed);
    }

    public void testConstructNullQuery() throws Exception {
        try {
            FqlQuery fq = new FqlQuery(null, "org.flymine.model.testmodel");
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testConstructEmptyQuery() throws Exception {
        try {
            FqlQuery fq = new FqlQuery("", "org.flymine.model.testmodel");
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testConstructEmptyPackageName() throws Exception {
        try {
            FqlQuery fq = new FqlQuery("select a1_ from Company as a1_", "");
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

}
