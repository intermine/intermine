package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.Query;
import org.junit.Assert;
import org.junit.Test;

public class ObjectStoreAbstractImplTestCase extends ObjectStoreTestCase {

    private static ObjectStoreAbstractImpl osForOsaiTests;

    public static void oneTimeSetUp(
            ObjectStoreAbstractImpl osForOsaiTests, String osWriterName, String modelName, String itemsXmlFilename)
            throws Exception {
        ObjectStoreAbstractImplTestCase.osForOsaiTests = osForOsaiTests;
        ObjectStoreTestCase.oneTimeSetUp(osForOsaiTests, osWriterName, modelName, itemsXmlFilename);
    }

    @Test
    public void testCheckStartLimit() throws Exception {
        testCheckStartLimit(osForOsaiTests);
    }

    @Test
    public void testLimitTooHigh() throws Exception {
        testLimitTooHigh(osForOsaiTests);
    }

    @Test
    public void testOffsetTooHigh() throws Exception {
        testOffsetTooHigh(osForOsaiTests);
    }

    @Test
    public void testTooMuchTime() throws Exception {
        testTooMuchTime(osForOsaiTests);
    }

    public static void testCheckStartLimit(ObjectStoreAbstractImpl osai) throws Exception {
        int oldOffset = osai.maxOffset;
        osai.maxOffset = 10;
        try {
            osai.checkStartLimit(11, 0, queries.get("SelectSimpleObject"));
            Assert.fail("Expected ObjectStoreLimitReachedException");
        } catch (ObjectStoreLimitReachedException e) {
        }
        osai.maxOffset = oldOffset;

        int oldLimit = osai.maxLimit;
        osai.maxLimit = 10;
        try {
            osai.checkStartLimit(0,11,(Query) queries.get("SelectSimpleObject"));
            Assert.fail("Expected ObjectStoreLimitReachedException");
        } catch (ObjectStoreLimitReachedException e) {
        }
        osai.maxLimit = oldLimit;
    }

    public static void testLimitTooHigh(ObjectStoreAbstractImpl osai) throws Exception {
        // try to run query with limit higher than imposed maximum
        int before = osai.maxLimit;
        osai.maxLimit = 99;
        try{
            osForOsaiTests.execute(queries.get("SelectSimpleObject"), 10, 100, true, true, ObjectStore.SEQUENCE_IGNORE);
            Assert.fail("Expected: ObjectStoreException");
        } catch (ObjectStoreLimitReachedException e) {
        } finally {
            osai.maxLimit = before;
        }
    }

    public void testOffsetTooHigh(ObjectStoreAbstractImpl osai) throws Exception {
        // try to run query with offset higher than imposed maximum
        int before = osai.maxOffset;
        osai.maxOffset = 99;
        try {
            osai.execute(queries.get("SelectSimpleObject"), 100, 50, true, true, ObjectStore.SEQUENCE_IGNORE);
            Assert.fail("Expected: ObjectStoreException");
        } catch (ObjectStoreLimitReachedException e) {
        } finally {
            osai.maxOffset = before;
        }
    }

    public void testTooMuchTime(ObjectStoreAbstractImpl osai) throws Exception {
        // try to run a query that takes longer than max amount of time
        long before = osai.maxTime;
        osai.maxTime = -1;
        try {
            osai.execute(queries.get("SelectSimpleObject"), 0, 1, true, true, ObjectStore.SEQUENCE_IGNORE);
            Assert.fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
        } finally {
            osai.maxTime = before;
        }
    }
}
