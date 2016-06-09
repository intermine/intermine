package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreLimitReachedException;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.model.testmodel.Department;

import org.apache.log4j.Logger;

public class ResultsTest extends TestCase
{
    private static final Logger LOG = Logger.getLogger(ResultsTest.class);

    public ResultsTest(String arg1) {
        super(arg1);
    }

    private ObjectStoreDummyImpl os;

    public void setUp() throws Exception {

        // Set up a dummy ObjectStore with 10 rows to return
        os = new ObjectStoreDummyImpl();
        os.setResultsSize(10);

        for (int i = 0; i < 10; i++) {
            ResultsRow row = new ResultsRow();
            row.add("" + i);
            os.addRow(row);
        }
    }

    public void testConstructNullQuery() throws Exception {
        try {
            Results res = new Results(null, new ObjectStoreDummyImpl(), ObjectStore.SEQUENCE_IGNORE);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testConstructNullObjectStore() throws Exception {
        try {
            Results res = new Results(new Query(), null, ObjectStore.SEQUENCE_IGNORE);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testGetOutOfBounds() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results res = os.execute(q);
        try {
            res.get(10);
            fail("Expected IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException e) {
        }
    }

    public void testRangeOutOfBounds() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results res = os.execute(q, 5, true, true, true);
        try {
            res.range(6,11);
            fail("Expected IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException e) {
        }
    }

    public void testGetFromTwoBatches() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results res = os.execute(q, 5, true, true, true);
        ResultsRow row = (ResultsRow) res.get(6);
        assertEquals(1, os.getExecuteCalls());
        assertEquals("6", (String) row.get(0));
        row = (ResultsRow) res.get(3);
        assertEquals(2, os.getExecuteCalls());
        assertEquals("3", (String) row.get(0));
    }

    public void testInvalidRange() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results res = os.execute(q, 10, true, true, true);
        // Don't let res call the ObjectStore
        res.resultsBatches.os = null;
        try {
            ResultsRow row = (ResultsRow) res.range(5, 3);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetAllRowsInOneRange() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results res = os.execute(q, 10, true, true, true);
        List rows = res.range(0,9);
        assertEquals(10, rows.size());
        for (int i = 0; i < 10; i++) {
            assertEquals("" + i, (String) ((ResultsRow) rows.get(i)).get(0));
        }
    }

    public void testGetPartialRowsFromTwoBatches() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results res = os.execute(q, 5, true, true, true);
        List rows = res.range(4,7);
        assertEquals(4, rows.size());
        for (int i = 4; i <= 7; i++) {
            assertEquals("" + i, (String) ((ResultsRow) rows.get(i - 4)).get(0));
        }
    }

    public void testGetAllRowsInTwoRanges() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results res = os.execute(q, 5, true, true, true);
        Iterator iter = res.iterator();
        int o = 0;
        while (iter.hasNext()) {
            assertEquals("" + o++, (String) ((ResultsRow) iter.next()).get(0));
        }
        assertEquals(10, res.size());
        List rows = res.range(0,9);
        assertEquals(10, rows.size());
        for (int i = 0; i < 10; i++) {
            assertEquals("" + i, (String) ((ResultsRow) rows.get(i)).get(0));
        }
    }

    public void testGetAllRowsInTwoRangesTwice() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results res = os.execute(q, 5, true, true, true);
        List rows = res.range(0,9);
        // Call this a second time - the rows should be in the cache
        // Invalidate the os - to check that no further calls can be made to it
        res.resultsBatches.os = null;
        rows = res.range(0,9);
        assertEquals(10, rows.size());
        for (int i = 0; i < 10; i++) {
            assertEquals("" + i, (String) ((ResultsRow) rows.get(i)).get(0));
        }
    }

    public void testSubList() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results res = os.execute(q);
        List rows = res.subList(4,7);
        assertEquals(3, rows.size());

        for (int i = 4; i <= 6; i++) {
            assertEquals("" + i, (String) ((ResultsRow) rows.get(i - 4)).get(0));
        }
    }

    public void testSize() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results res = os.execute(q);
        assertEquals(10, res.size());
    }

    public void testSetBatchSize() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results res = os.execute(q, 10, true, true, true);
        assertEquals(10, res.getBatchSize());
    }

    public void testGetBatchNoForRow() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results res = os.execute(q, 10, true, true, true);
        assertEquals(0, res.getBatchNoForRow(6));
        assertEquals(1, res.getBatchNoForRow(14));

        res = os.execute(q, 5, true, true, true);
        assertEquals(1, res.getBatchNoForRow(6));
        assertEquals(2, res.getBatchNoForRow(14));

    }

    public void testFetchBatchFromObjectStore() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results res = os.execute(q, 7, true, true, true);

        // Fetch the first batch - will be a full batch
        res.resultsBatches.fetchBatchFromObjectStore(0, true, true);
        assertEquals(1, os.getExecuteCalls());

        // Fetch the second batch - will be partial, but will now know size
        res.resultsBatches.fetchBatchFromObjectStore(1, true, true);
        assertEquals(10, res.resultsBatches.maxSize);
        assertEquals(2, os.getExecuteCalls());

        List list = res.resultsBatches.fetchBatchFromObjectStore(2, true, true);
        assertEquals(0, list.size());
    }


    public void testSetBatchSizeWhenInitialised() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results res = new Results(q, os, ObjectStore.SEQUENCE_IGNORE);
        res.setBatchSize(10);

        res.get(0);
        try {
            res.setBatchSize(15);
            fail("Expected: IllegalStateException");
        }
        catch (IllegalStateException e) {
        }
    }

    public void testWorkingWithRemovals() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        ObjectStoreDummyImpl os2 = new ObjectStoreDummyImpl();
        os2.setResultsSize(5000);

        Results res = os2.execute(q, 200, true, true, true);
        int count = 0;
        Iterator iter = res.iterator();
        while (iter.hasNext()) {
            count++;
            Object row = iter.next();
            res.resultsBatches.batches.clear();
        }
        assertEquals(5000, count);
    }

    public void testSizeUsesCount() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        ObjectStoreDummyImpl os2 = new ObjectStoreDummyImpl();
        os2.setResultsSize(5000);

        LOG.info("testSizeUsesFewBatchFetches starting");
        Results res = os2.execute(q, 1, true, true, true);
        res.resultsBatches.batches = Collections.synchronizedMap(new HashMap());
        assertEquals(5000, res.size());
        assertTrue("Expected size to fetch one batch, but fetched " + res.resultsBatches.batches.size() + ".", res.resultsBatches.batches.size() == 1);
    }

    public void testIteratorPropagatesObjectStoreException() throws Exception {
        LOG.info("testIteratorPropagatesObjectStoreException starting");
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        ObjectStoreDummyImpl os2 = new ObjectStoreDummyImpl();
        os2.setResultsSize(10);
        os2.setPoisonRowNo(7);
        Results res = os2.execute(q, 1, true, true, true);

        int count = 0;
        try {
            Iterator iter = res.iterator();
            while (iter.hasNext()) {
                Object obj = iter.next();
                count++;
            }
            fail("Expected RuntimeException containing ObjectStoreException - count = " + count);
        } catch (RuntimeException e) {
            if (!(e.getCause() instanceof ObjectStoreException)) {
                fail("Expected RuntimeException to contain an ObjectStoreException");
            }
            if (count != 7) {
                fail("Expected to get the exception after 7 rows");
            }
        }
    }

    public void testIteratorPropagatesObjectStoreLimitReachedException() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        ObjectStoreDummyImpl os2 = new ObjectStoreDummyImpl();
        os2.setResultsSize(10);
        os2.setMaxOffset(6);
        Results res = os2.execute(q, 1, true, true, true);

        int count = 0;
        try {
            Iterator iter = res.iterator();
            while (iter.hasNext()) {
                Object obj = iter.next();
                count++;
            }
            fail("Expected ObjectStoreLimitReachedException - count = " + count);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof ObjectStoreLimitReachedException) {
                if (count != 7) {
                    fail("Expected to get the exception after 7 rows");
                }
            }
        }
    }

    public void testStrangeIteratorUsage() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        ObjectStoreDummyImpl os2 = new ObjectStoreDummyImpl();
        os2.setResultsSize(50);

        Results res = os2.execute(q, 20, true, true, true);
        int count = 0;
        Iterator iter = res.iterator();
        while (iter.hasNext()) {
            count++;
            iter.hasNext();
            Object row = iter.next();
        }
        assertEquals(50, count);
    }

    public void testResultsInfo() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        ObjectStoreDummyImpl os2 = new ObjectStoreDummyImpl();
        os2.setResultsSize(50);
        os2.setEstimatedResultsSize(1);

        Results res = os2.execute(q, 30, true, true, true);

        ResultsInfo i = res.getInfo();
        assertEquals(1, i.getRows());
        assertEquals(ResultsInfo.ESTIMATE, i.getStatus());

        res.get(0);

        i = res.getInfo();
        assertEquals(30, i.getRows());
        assertEquals(ResultsInfo.AT_LEAST, i.getStatus());

        res.get(29);

        i = res.getInfo();
        assertEquals(30, i.getRows());
        assertEquals(ResultsInfo.AT_LEAST, i.getStatus());

        res.get(30);

        i = res.getInfo();
        assertEquals(50, i.getRows());
        assertEquals(ResultsInfo.SIZE, i.getStatus());
    }

    public void testResultsInfo2() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        ObjectStoreDummyImpl os2 = new ObjectStoreDummyImpl();
        os2.setResultsSize(50);
        os2.setEstimatedResultsSize(1);

        Results res = os2.execute(q, 30, true, true, true);

        ResultsInfo i = res.getInfo();
        assertEquals(1, i.getRows());
        assertEquals(ResultsInfo.ESTIMATE, i.getStatus());

        res.get(0);

        i = res.getInfo();
        assertEquals(30, i.getRows());
        assertEquals(ResultsInfo.AT_LEAST, i.getStatus());

        assertEquals(50, res.size());
        i = res.getInfo();
        assertEquals(50, i.getRows());
        assertEquals(ResultsInfo.SIZE, i.getStatus());
    }
}
