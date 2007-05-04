package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.Query;

public class ObjectStoreAbstractImplTestCase extends ObjectStoreTestCase
{
    protected static ObjectStoreAbstractImpl osai;
    
    public ObjectStoreAbstractImplTestCase(String arg) {
        super(arg);
    }

    public static void oneTimeSetUp() throws Exception {
        ObjectStoreTestCase.oneTimeSetUp();
        if (os instanceof ObjectStoreAbstractImpl) {
            osai = (ObjectStoreAbstractImpl) os;
        }
    }

    public void testCheckStartLimit() throws Exception {
        int oldOffset = osai.maxOffset;
        osai.maxOffset = 10;
        try {
            osai.checkStartLimit(11,0,(Query) queries.get("SelectSimpleObject"));
            fail("Expected ObjectStoreLimitReachedException");
        } catch (ObjectStoreLimitReachedException e) {
        }
        osai.maxOffset = oldOffset;

        int oldLimit = osai.maxLimit;
        osai.maxLimit = 10;
        try {
            osai.checkStartLimit(0,11,(Query) queries.get("SelectSimpleObject"));
            fail("Expected ObjectStoreLimitReachedException");
        } catch (ObjectStoreLimitReachedException e) {
        }
        osai.maxLimit = oldLimit;
    }

    public void testLimitTooHigh() throws Exception {
        // try to run query with limit higher than imposed maximum
        int before = osai.maxLimit;
        osai.maxLimit = 99;
        try{
            osai.execute((Query) queries.get("SelectSimpleObject"), 10, 100, true, true, ObjectStore.SEQUENCE_IGNORE);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreLimitReachedException e) {
        } finally {
            osai.maxLimit = before;
        }
    }

    public void testOffsetTooHigh() throws Exception {
        // try to run query with offset higher than imposed maximum
        int before = osai.maxOffset;
        osai.maxOffset = 99;
        try {
            osai.execute((Query) queries.get("SelectSimpleObject"), 100, 50, true, true, ObjectStore.SEQUENCE_IGNORE);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreLimitReachedException e) {
        } finally {
            osai.maxOffset = before;
        }
    }

    public void testTooMuchTime()  throws Exception {
        // try to run a query that takes longer than max amount of time
        long before = osai.maxTime;
        osai.maxTime = -1;
        try {
            osai.execute((Query) queries.get("SelectSimpleObject"), 0, 1, true, true, ObjectStore.SEQUENCE_IGNORE);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
        } finally {
            osai.maxTime = before;
        }
    }
}
