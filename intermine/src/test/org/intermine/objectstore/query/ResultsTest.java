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

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.lang.reflect.Field;

import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.ObjectStoreLimitReachedException;
import org.flymine.objectstore.dummy.ObjectStoreDummyImpl;
import org.flymine.objectstore.proxy.LazyCollection;
import org.flymine.objectstore.proxy.LazyReference;
import org.flymine.objectstore.proxy.LazyInitializer;
import org.flymine.model.testmodel.Department;
import org.flymine.model.testmodel.Employee;
import org.flymine.model.testmodel.Company;

public class ResultsTest extends TestCase
{
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
            Results res = new Results(null, new ObjectStoreDummyImpl());
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testConstructNullObjectStore() throws Exception {
        try {
            Results res = new Results(new Query(), null);
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
        Results res = os.execute(q);
        res.setBatchSize(5);
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
        Results res = os.execute(q);
        res.setBatchSize(5);
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
        Results res = os.execute(q);
        res.setBatchSize(10);
        // Don't let res call the ObjectStore
        res.os = null;
        try {
            ResultsRow row = (ResultsRow) res.range(5, 3);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetAllRowsInOneRange() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results res = os.execute(q);
        res.setBatchSize(10);
        List rows = res.range(0,9);
        assertEquals(10, rows.size());
        for (int i = 0; i < 10; i++) {
            assertEquals("" + i, (String) ((ResultsRow) rows.get(i)).get(0));
        }
    }

    public void testGetPartialRowsFromTwoBatches() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results res = os.execute(q);
        res.setBatchSize(5);
        List rows = res.range(4,7);
        assertEquals(4, rows.size());
        for (int i = 4; i <= 7; i++) {
            assertEquals("" + i, (String) ((ResultsRow) rows.get(i - 4)).get(0));
        }
    }

    public void testGetAllRowsInTwoRanges() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results res = os.execute(q);
        res.setBatchSize(5);
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
        Results res = os.execute(q);
        res.setBatchSize(5);
        List rows = res.range(0,9);
        // Call this a second time - the rows should be in the cache
        // Invalidate the os - to check that no further calls can be made to it
        res.os = null;
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
        Results res = os.execute(q);
        res.setBatchSize(10);
        assertEquals(10, res.batchSize);
    }

    public void testGetBatchNoForRow() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results res = os.execute(q);
        res.setBatchSize(10);
        assertEquals(0, res.getBatchNoForRow(6));
        assertEquals(1, res.getBatchNoForRow(14));

        res.setBatchSize(5);
        assertEquals(1, res.getBatchNoForRow(6));
        assertEquals(2, res.getBatchNoForRow(14));

    }

    public void testFetchBatchFromObjectStore() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results res = os.execute(q);
        res.setBatchSize(7);

        // Fetch the first batch - will be a full batch
        res.fetchBatchFromObjectStore(0);
        assertEquals(1, os.getExecuteCalls());

        // Fetch the second batch - will be partial, but will now know size
        res.fetchBatchFromObjectStore(1);
        assertEquals(10, res.maxSize);
        assertEquals(2, os.getExecuteCalls());

        List list = res.fetchBatchFromObjectStore(2);
        assertEquals(0, list.size());
    }


    public void testSetBatchSizeWhenInitialised() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results res = os.execute(q);
        res.setBatchSize(10);

        res.get(0);
        try {
            res.setBatchSize(15);
            fail("Expected: IllegalStateException");
        }
        catch (IllegalStateException e) {
        }

    }

    public void testPromoteLazyCollection() throws Exception {
        // Create a Department object with a LazyCollection
        Department dept = getDeptExampleObject();
        assertTrue(dept.getEmployees() instanceof LazyCollection);

        // build a List of ResultsRows to simulate call to promoteProxies
        ResultsRow rr = new ResultsRow();
        rr.add(dept);
        List list = new ArrayList(1);
        list.add(rr);

        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        Results.promoteProxies(list, os);
        Department resDept = (Department) ((List)list.get(0)).get(0);

        // Employees should now have become a Results object
        Collection col = resDept.getEmployees();
        if (!(col instanceof SingletonResults)) {
            fail("LazyCollection was not converted to a Results object");
        }
    }

    public void testPromoteLazyCollectionSet() throws Exception {
        // Create a Department object with a LazyCollection
        Example ex = getExampleObjectWithSet();
        assertTrue(ex.getSet() instanceof LazyCollection);

        // build a List of ResultsRows to simulate call to promoteProxies
        ResultsRow rr = new ResultsRow();
        rr.add(ex);
        List list = new ArrayList(1);
        list.add(rr);

        Query q = new Query();
        q.addFrom(new QueryClass(Example.class));
        Results.promoteProxies(list, os);
        Example resEx = (Example) ((List)list.get(0)).get(0);

        // Employees should now have become a Results object
        Collection col = resEx.getSet();
        if (!(col instanceof SingletonResults)) {
            fail("LazyCollection was not converted to a Results object");
        }
    }

    public void testPromoteLazyReference() throws Exception {
        // Create a Department object with a LazyCollection
        Department dept = getDeptExampleObject();
        assertTrue(dept.getCompany() instanceof LazyReference);

        // build a List of ResultsRows to simulate call to promoteProxies
        ResultsRow rr = new ResultsRow();
        rr.add(dept);
        List list = new ArrayList(1);
        list.add(rr);

        Query q = new Query();
        Results.promoteProxies(list, os);
        Department resDept = (Department) ((List)list.get(0)).get(0);

        // Company should now be materialized
        Object obj = resDept.getCompany();
        if (!(obj instanceof Company)) {
            fail("LazyCollection was not converted to a Results object");
        }
    }

    // set up a Department object with an id and Employees as a LazyCollection
    // and a LazyReference
    private Department getDeptExampleObject() throws Exception {
        Department dept = new Department();
        Class deptClass = dept.getClass();
        Field f = deptClass.getDeclaredField("id");
        f.setAccessible(true);
        f.set(dept, new Integer(1234));

        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Employee.class);
        q1.addToSelect(qc1);
        q1.addFrom(qc1);

        LazyCollection lazyCol = new LazyCollection(q1);
        dept.setEmployees((List)lazyCol);

        Query q2 = new Query();
        QueryClass qc2 = new QueryClass(Company.class);
        q2.addToSelect(qc2);
        q2.addFrom(qc2);

        LazyReference lazyRef = (LazyReference) LazyInitializer.getDynamicProxy(Company.class, q2, new Integer(0));
        dept.setCompany((Company)lazyRef);

        return dept;
    }

    // set up an Example object with field of type Set that is actually a LazyCollection
    private Example getExampleObjectWithSet() throws Exception {
        Example ex = new Example();
        Class exClass = ex.getClass();
        //Field f = deptClass.getDeclaredField("id");
        //f.setAccessible(true);
        //f.set(dept, new Integer(1234));

        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Example.class);
        q1.addToSelect(qc1);
        q1.addFrom(qc1);

        LazyCollection lazyCol = new LazyCollection(q1);
        ex.setSet((Set)lazyCol);

        return ex;
    }

    // example class with a set, for testing promoteProxies()
    private class Example {
        Set set = new HashSet();
        public void setSet(Set set) {
            this.set = set;
        }
        public Set getSet() {
            return this.set;
        }
    }

    public void testGarbageCollectionOnWeakHashMap() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        ObjectStoreDummyImpl os2 = new ObjectStoreDummyImpl();
        os2.setResultsSize(100000);

        Results res = os2.execute(q);
        Object tempHold = null;;
        res.setBatchSize(200);
        int count = 0;
        Iterator iter = res.iterator();
        while (iter.hasNext() && (count < 50000)) {
            Object row = iter.next();
            if (count == 0) {
                tempHold = res.batches.keySet().iterator().next();
            }
            count++;
        }
        int mapSize = res.batches.size();
        int mapCount = 0;
        iter = res.batches.keySet().iterator();
        while (iter.hasNext()) {
            mapCount++;
            Object obj = iter.next();
        }
        Results.LOG.info("testGarbageCollectionOnWeakHashMap - batches map now: " + res.batches.keySet());
        tempHold = null;
        System.gc();
        Results.LOG.info("testGarbageCollectionOnWeakHashMap - batches map now: " + res.batches.keySet());
        int newMapSize = res.batches.size();
        int newMapCount = 0;
        iter = res.batches.keySet().iterator();
        while (iter.hasNext()) {
            newMapCount++;
            Object obj = iter.next();
        }
        Results.LOG.info("testGarbageCollectionOnWeakHashMap - original size: " + mapSize + ", new size: " + newMapSize);
        Results.LOG.info("testGarbageCollectionOnWeakHashMap - original count: " + mapCount + ", new count: " + newMapCount);
        assertTrue("new count " + newMapCount + " is not less than " + mapCount, newMapCount < mapCount);
    }

    public void testWorkingWithRemovals() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        ObjectStoreDummyImpl os2 = new ObjectStoreDummyImpl();
        os2.setResultsSize(5000);

        Results res = os2.execute(q);
        res.setBatchSize(200);
        int count = 0;
        Iterator iter = res.iterator();
        while (iter.hasNext()) {
            count++;
            Object row = iter.next();
            res.batches.clear();
        }
        assertEquals(5000, count);
    }

    public void testSizeUsesCount() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        ObjectStoreDummyImpl os2 = new ObjectStoreDummyImpl();
        os2.setResultsSize(5000);

        Results.LOG.info("testSizeUsesFewBatchFetches starting");
        Results res = os2.execute(q);
        res.setBatchSize(1);
        res.batches = Collections.synchronizedMap(new HashMap());
        assertEquals(5000, res.size());
        assertTrue("Expected size to fetch one batch, but fetched " + res.batches.size() + ".", res.batches.size() == 1);
    }

    public void testSizeUsesFewBatchFetches() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        ObjectStoreDummyImpl os2 = new ObjectStoreDummyImpl();
        os2.setResultsSize(5000);

        Results.LOG.info("testSizeUsesFewBatchFetches starting");
        Results res = os2.execute(q);
        res.setBatchSize(1);
        res.get(3000);
        try {
            res.get(6002);
        } catch (Exception e) {
            // Expected.
        }
        res.batches = Collections.synchronizedMap(new HashMap());
        assertEquals(5000, res.size());
        assertTrue("Expected size to need exactly 12 tries to find size - took " + res.batches.size() + " tries.", res.batches.size() == 12);
    }

    public void testSizeUsesFewBatchFetches2() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        ObjectStoreDummyImpl os2 = new ObjectStoreDummyImpl();
        os2.setResultsSize(5000);

        Results.LOG.info("testSizeUsesFewBatchFetches2 starting");
        Results res = os2.execute(q);
        res.setBatchSize(30);
        res.get(3000);
        try {
            res.get(6031);
        } catch (Exception e) {
            // Expected.
        }
        res.batches = Collections.synchronizedMap(new HashMap());
        assertEquals(5000, res.size());
        assertTrue("Expected size to need exactly 6 tries to find size - took " + res.batches.size() + " tries.", res.batches.size() == 6);
    }

    public void testIteratorPropagatesObjectStoreException() throws Exception {
        Results.LOG.info("testIteratorPropagatesObjectStoreException starting");
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        ObjectStoreDummyImpl os2 = new ObjectStoreDummyImpl();
        os2.setResultsSize(10);
        os2.setPoisonRowNo(7);
        Results res = os2.execute(q);
        res.setBatchSize(1);

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
        Results res = os2.execute(q);
        res.setBatchSize(1);

        int count = 0;
        try {
            Iterator iter = res.iterator();
            while (iter.hasNext()) {
                Object obj = iter.next();
                count++;
            }
            fail("Expected ObjectStoreLimitReachedException - count = " + count);
        } catch (ObjectStoreLimitReachedException e) {
            if (count != 7) {
                fail("Expected to get the exception after 7 rows");
            }
        }
    }

    public void testStrangeIteratorUsage() throws Exception {
        Query q = new Query();
        q.addFrom(new QueryClass(Department.class));
        ObjectStoreDummyImpl os2 = new ObjectStoreDummyImpl();
        os2.setResultsSize(50);

        Results res = os2.execute(q);
        res.setBatchSize(20);
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

        Results res = os2.execute(q);
        res.setBatchSize(30);

        ResultsInfo i = res.getInfo();
        assertEquals(1, i.getRows());
        assertEquals(ResultsInfo.ESTIMATE, i.getStatus());

        i = res.getInfo();
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
}
