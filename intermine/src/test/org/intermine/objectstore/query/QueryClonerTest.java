package org.flymine.objectstore.query;

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

import org.flymine.objectstore.SetupDataTestCase;
import org.flymine.testing.OneTimeTestCase;

/**
 * Test class for the QueryCloner.
 *
 * @author Matthew Wakeling
 */
public class QueryClonerTest extends SetupDataTestCase
{
    public QueryClonerTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(QueryClonerTest.class);
    }

    public void executeTest(String type) throws Exception {
        Query orig = ((Query) queries.get(type));
        Query cloned = QueryCloner.cloneQuery(orig);

        assertEquals(type + " has failed", orig, cloned);
    }
}
    
