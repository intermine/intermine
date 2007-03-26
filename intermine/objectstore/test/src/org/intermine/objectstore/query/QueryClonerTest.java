package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.Test;

import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.objectstore.SetupDataTestCase;
import org.intermine.testing.OneTimeTestCase;

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

    public static void oneTimeSetUp() throws Exception {
        SetupDataTestCase.oneTimeSetUp();

        results.putAll(queries);
    }

    public void executeTest(String type) throws Exception {
        try {
            Query orig = ((Query) queries.get(type));
            Query cloned = QueryCloner.cloneQuery(orig);

            assertEquals(type + " has failed", orig, cloned);
        } catch (IllegalArgumentException e) {
            fail("IllegalArgumentException thrown for " + type + ": " + e.getMessage() + ": " + queries.get(type));
        }
    }

    public void testAlias() throws Exception {
        Query orig = new Query();
        QueryClass c = new QueryClass(Company.class);
        orig.addFrom(c);
        orig.addToSelect(c);

        Query cloned = QueryCloner.cloneQuery(orig);

        QueryClass c2 = new QueryClass(Department.class);
        cloned.addFrom(c2);
        cloned.addToSelect(c2);

        assertEquals("SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a2_", cloned.toString());
    }
}
    
