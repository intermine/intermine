package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.*;

import org.intermine.objectstore.query.*;
import org.junit.Assert;

/**
 * TestCase for testing InterMine Queries
 * To check results:
 * add results to the results mapItemsToNames
 * override executeTest to run query and assert that the result is what is expected
 */

public abstract class ObjectStoreImplQueryTestCase extends ObjectStoreQueryTestCase {

    /**
     * Execute a test for a query. This should run the query and
     * contain an assert call to assert that the returned results are
     * those expected.
     *
     * @param type the type of query we are testing (ie. the key in the queries Map)
     * @throws Exception if type does not appear in the queries map
     */
    public void executeTest(String type) throws Exception {
        if (results.get(type) instanceof Failure) {
            try {
                Results res = os.execute(queries.get(type), 2, true, true, true);
                Iterator iter = res.iterator();
                while (iter.hasNext()) {
                    iter.next();
                }
                Assert.fail(type + " was expected to fail");
            } catch (Exception e) {
                Assert.assertEquals(type + " was expected to produce a particular exception", results.get(type), new Failure(e));
            }
        } else {
            Results res = os.execute(queries.get(type), 2, true, true, true);
            List expected = (List) results.get(type);
            if ((expected != null) && (!expected.equals(res))) {
                Set a = new HashSet(expected);
                Set b = new HashSet(res);
                List la = ObjectStoreTestUtils.queryResultsToNames(expected);
                List lb = ObjectStoreTestUtils.queryResultsToNames(res);
                if (a.equals(b)) {
                    Assert.assertEquals(type + " has failed - wrong order", la, lb);
                }
                Assert.fail(type + " has failed. Expected " + la + " but was " + lb);
            }
        }
    }
}
