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

import org.intermine.model.testmodel.Company;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ObjectStoreAbstractImplTests {

    private static ObjectStoreAbstractImpl osForOsaiTests;
    protected static Map<String, Query> queries = new HashMap<String, Query>();

    public static void oneTimeSetUp(ObjectStoreAbstractImpl osForOsaiTests) throws Exception {
        ObjectStoreAbstractImplTests.osForOsaiTests = osForOsaiTests;
        queries.put("SelectSimpleObject", generateSelectSimpleObjectQuery());
    }

    /*
      select Alias
      from Company AS Alias
      NOT DISTINCT
    */
    public static Query generateSelectSimpleObjectQuery() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.setDistinct(false);
        q1.alias(c1, "Alias");
        q1.addFrom(c1);
        q1.addToSelect(c1);
        return q1;
    }

    @Test
    public void testCheckStartLimit() throws Exception {
        int oldOffset = osForOsaiTests.maxOffset;
        osForOsaiTests.maxOffset = 10;
        try {
            osForOsaiTests.checkStartLimit(11,0,(Query) queries.get("SelectSimpleObject"));
            Assert.fail("Expected ObjectStoreLimitReachedException");
        } catch (ObjectStoreLimitReachedException e) {
        }
        osForOsaiTests.maxOffset = oldOffset;

        int oldLimit = osForOsaiTests.maxLimit;
        osForOsaiTests.maxLimit = 10;
        try {
            osForOsaiTests.checkStartLimit(0,11,(Query) queries.get("SelectSimpleObject"));
            Assert.fail("Expected ObjectStoreLimitReachedException");
        } catch (ObjectStoreLimitReachedException e) {
        }
        osForOsaiTests.maxLimit = oldLimit;
    }

    @Test
    public void testLimitTooHigh() throws Exception {
        // try to run query with limit higher than imposed maximum
        int before = osForOsaiTests.maxLimit;
        osForOsaiTests.maxLimit = 99;
        try{
            osForOsaiTests.execute((Query) queries.get("SelectSimpleObject"), 10, 100, true, true, ObjectStore.SEQUENCE_IGNORE);
            Assert.fail("Expected: ObjectStoreException");
        } catch (ObjectStoreLimitReachedException e) {
        } finally {
            osForOsaiTests.maxLimit = before;
        }
    }

    @Test
    public void testOffsetTooHigh() throws Exception {
        // try to run query with offset higher than imposed maximum
        int before = osForOsaiTests.maxOffset;
        osForOsaiTests.maxOffset = 99;
        try {
            osForOsaiTests.execute((Query) queries.get("SelectSimpleObject"), 100, 50, true, true, ObjectStore.SEQUENCE_IGNORE);
            Assert.fail("Expected: ObjectStoreException");
        } catch (ObjectStoreLimitReachedException e) {
        } finally {
            osForOsaiTests.maxOffset = before;
        }
    }

    @Test
    public void testTooMuchTime()  throws Exception {
        // try to run a query that takes longer than max amount of time
        long before = osForOsaiTests.maxTime;
        osForOsaiTests.maxTime = -1;
        try {
            osForOsaiTests.execute((Query) queries.get("SelectSimpleObject"), 0, 1, true, true, ObjectStore.SEQUENCE_IGNORE);
            Assert.fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
        } finally {
            osForOsaiTests.maxTime = before;
        }
    }
}
