package org.flymine.objectstore.query;

import junit.framework.TestCase;

import java.util.List;
import org.flymine.objectstore.dummy.ObjectStoreDummyImpl;

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

}
