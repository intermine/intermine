package org.flymine.objectstore.query;

import junit.framework.TestCase;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Field;

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
        Results res = os.execute(new Query());
        try {
            res.get(10);
            fail("Expected IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException e) {
        }
    }

    public void testRangeOutOfBounds() throws Exception {
        Results res = os.execute(new Query());
        res.setBatchSize(5);
        try {
            res.range(6,11);
            fail("Expected IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException e) {
        }
    }

    public void testGetFromTwoBatches() throws Exception {
        Results res = os.execute(new Query());
        res.setBatchSize(5);
        ResultsRow row = (ResultsRow) res.get(6);
        assertEquals(1, os.getExecuteCalls());
        assertEquals("6", (String) row.get(0));
        row = (ResultsRow) res.get(3);
        assertEquals(2, os.getExecuteCalls());
        assertEquals("3", (String) row.get(0));
    }

    public void testInvalidRange() throws Exception {
        Results res = os.execute(new Query());
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
        Results res = os.execute(new Query());
        res.setBatchSize(10);
        List rows = res.range(0,9);
        assertEquals(10, rows.size());
        for (int i = 0; i < 10; i++) {
            assertEquals("" + i, (String) ((ResultsRow) rows.get(i)).get(0));
        }
    }

    public void testGetPartialRowsFromTwoBatches() throws Exception {
        Results res = os.execute(new Query());
        res.setBatchSize(5);
        List rows = res.range(4,7);
        assertEquals(4, rows.size());
        for (int i = 4; i <= 7; i++) {
            assertEquals("" + i, (String) ((ResultsRow) rows.get(i - 4)).get(0));
        }
    }

    public void testGetAllRowsInTwoRanges() throws Exception {
        Results res = os.execute(new Query());
        res.setBatchSize(5);
        List rows = res.range(0,9);
        assertEquals(10, rows.size());
        for (int i = 0; i < 10; i++) {
            assertEquals("" + i, (String) ((ResultsRow) rows.get(i)).get(0));
        }
    }

    public void testGetAllRowsInTwoRangesTwice() throws Exception {
        Results res = os.execute(new Query());
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
        Results res = os.execute(new Query());
        List rows = res.subList(4,7);
        assertEquals(3, rows.size());

        for (int i = 4; i <= 6; i++) {
            assertEquals("" + i, (String) ((ResultsRow) rows.get(i - 4)).get(0));
        }
    }

    public void testSize() throws Exception {
        Results res = os.execute(new Query());
        assertEquals(10, res.size());
    }

    public void testSetBatchSize() throws Exception {
        Results res = os.execute(new Query());
        res.setBatchSize(10);
        assertEquals(10, res.batchSize);
    }

    public void testGetBatchNoForRow() throws Exception {
        Results res = os.execute(new Query());
        res.setBatchSize(10);
        assertEquals(0, res.getBatchNoForRow(6));
        assertEquals(1, res.getBatchNoForRow(14));

        res.setBatchSize(5);
        assertEquals(1, res.getBatchNoForRow(6));
        assertEquals(2, res.getBatchNoForRow(14));

    }

    public void testFetchBatchFromObjectStore() throws Exception {
        Results res = os.execute(new Query());
        res.setBatchSize(7);

        // Fetch the first batch - will be a full batch
        res.fetchBatchFromObjectStore(0);
        assertEquals(1, os.getExecuteCalls());
        assertTrue(res.batches.containsKey(new Integer(0)));

        // Fetch the second batch - will be partial, but will now know size
        res.fetchBatchFromObjectStore(1);
        assertEquals(10, res.size);
        assertEquals(2, os.getExecuteCalls());
        assertTrue(res.batches.containsKey(new Integer(1)));

        try {
            res.fetchBatchFromObjectStore(2);
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
    }


    public void testSetBatchSizeWhenInitialised() throws Exception {
        Results res = os.execute(new Query());
        res.setBatchSize(10);

        res.get(0);
        try {
            res.setBatchSize(15);
            fail("Expected: IllegalStateException");
        }
        catch (IllegalStateException e) {
        }

    }

    public void testLazyCollectionPromotion() throws Exception {
        // Create a Department object with a LazyCollection
        Department dept = getDeptExampleObject();
        assertTrue(dept.getEmployees() instanceof LazyCollection);

        // build a List of ResultsRows to simulate call to promoteProxies
        ResultsRow rr = new ResultsRow();
        rr.add(dept);
        List list = new ArrayList(1);
        list.add(rr);

        Query q = new Query();
        Results r = os.execute(q);
        r.promoteProxies(list);
        Department resDept = (Department) ((List)list.get(0)).get(0);

        // Employees should now have become a Results object
        Collection col = resDept.getEmployees();
        if (!(col instanceof Results)) {
            fail("LazyCollection was not converted to a Results object");
        }
    }

    public void testLazyReferencePromotion() throws Exception {
        // Create a Department object with a LazyCollection
        Department dept = getDeptExampleObject();
        assertTrue(dept.getCompany() instanceof LazyReference);

        // build a List of ResultsRows to simulate call to promoteProxies
        ResultsRow rr = new ResultsRow();
        rr.add(dept);
        List list = new ArrayList(1);
        list.add(rr);

        Query q = new Query();
        Results r = os.execute(q);
        r.promoteProxies(list);
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

        LazyReference lazyRef = (LazyReference) LazyInitializer.getDynamicProxy(Company.class, q2);
        dept.setCompany((Company)lazyRef);

        return dept;
    }
}
