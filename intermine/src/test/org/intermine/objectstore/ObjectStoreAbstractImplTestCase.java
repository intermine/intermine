/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.flymine.objectstore;

import org.flymine.objectstore.query.Query;

public class ObjectStoreAbstractImplTestCase extends ObjectStoreTestCase
{
    protected static ObjectStoreAbstractImpl os;

    public ObjectStoreAbstractImplTestCase(String arg) {
        super(arg);
    }

    public static void oneTimeSetUp() throws Exception {
        ObjectStoreTestCase.oneTimeSetUp();
        os = (ObjectStoreAbstractImpl) ObjectStoreTestCase.os;
    }

    public void testCheckStartLimit() throws Exception {
        int oldOffset = os.maxOffset;
        os.maxOffset = 10;
        try {
            os.checkStartLimit(11,0);
            fail("Expected ObjectStoreLimitReachedException");
        } catch (ObjectStoreLimitReachedException e) {
        }
        os.maxOffset = oldOffset;

        int oldLimit = os.maxLimit;
        os.maxLimit = 10;
        try {
            os.checkStartLimit(0,11);
            fail("Expected ObjectStoreLimitReachedException");
        } catch (ObjectStoreLimitReachedException e) {
        }
        os.maxLimit = oldLimit;
    }

    public void testLimitTooHigh() throws Exception {
        // try to run query with limit higher than imposed maximum
        int before = os.maxLimit;
        os.maxLimit = 99;
        try{
            os.execute((Query) queries.get("SelectSimpleObject"), 10, 100);
            fail("Expected: ObjectStoreException");
        } catch (IndexOutOfBoundsException e) {
        } finally {
            os.maxLimit = before;
        }
    }

    public void testOffsetTooHigh() throws Exception {
        // try to run query with offset higher than imposed maximum
        int before = os.maxOffset;
        os.maxOffset = 99;
        try {
            os.execute((Query) queries.get("SelectSimpleObject"), 100, 50);
            fail("Expected: ObjectStoreException");
        } catch (IndexOutOfBoundsException e) {
        } finally {
            os.maxOffset = before;
        }
    }

    public void testTooMuchTime()  throws Exception {
        // try to run a query that takes longer than max amount of time
        long before = os.maxTime;
        os.maxTime = 0;
        try {
            os.execute((Query) queries.get("SelectSimpleObject"), 0, 10);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
        } finally {
            os.maxTime = before;
        }
    }
}
