package org.flymine.objectstore.dummy;

import junit.framework.TestCase;

import java.util.List;

import org.flymine.model.testmodel.*;
import org.flymine.objectstore.query.*;
import org.flymine.objectstore.ObjectStoreException;

public class ObjectStoreDummyImplTest extends TestCase
{
    public ObjectStoreDummyImplTest(String arg) {
        super(arg);
    }

    public void testAddRowRetrieveSame() throws Exception {

        ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
        ResultsRow row = new ResultsRow();
        String field1 = "test1";
        String field2 = "test2";
        row.add(field1);
        row.add(field2);

        os.addRow(row);
        os.setResultsSize(1);
        List rows = os.execute(new Query(), 0, 0);

        assertEquals(1, rows.size());
        ResultsRow newRow = (ResultsRow) rows.get(0);
        assertEquals(2, newRow.size());
        assertEquals("test1", (String) newRow.get(0));
        assertEquals("test2", (String) newRow.get(1));
    }

    public void testRetrieveNew() throws Exception {
        ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
        Query q = new Query();
        q.addToSelect(new QueryClass(String.class));
        q.addToSelect(new QueryClass(Department.class));

        os.setResultsSize(1);
        List rows = os.execute(q, 0, 0);

        assertEquals(1, rows.size());
        ResultsRow newRow = (ResultsRow) rows.get(0);
        assertEquals(2, newRow.size());
        assertTrue(newRow.get(0) instanceof String);
        assertTrue(newRow.get(1) instanceof Department);
    }

    public void testRetrieveNewAfterAdd() throws Exception {
        ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
        Query q = new Query();
        q.addToSelect(new QueryClass(String.class));
        q.addToSelect(new QueryClass(Department.class));

        ResultsRow row = new ResultsRow();
        String field1 = "test1";
        String field2 = "test2";
        row.add(field1);
        row.add(field2);

        os.addRow(row);
        os.setResultsSize(2);
        List rows = os.execute(q, 0, 1);

        assertEquals(2, rows.size());
        ResultsRow newRow = (ResultsRow) rows.get(0);
        assertEquals(2, newRow.size());
        assertEquals("test1", (String) newRow.get(0));
        assertEquals("test2", (String) newRow.get(1));
        newRow = (ResultsRow) rows.get(1);
        assertEquals(2, newRow.size());
        assertTrue(newRow.get(0) instanceof String);
        assertTrue(newRow.get(1) instanceof Department);
    }

    public void testRowLimit() throws Exception {
        ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
        Query q = new Query();
        os.setResultsSize(10);
        Results res = os.execute(q);

        assertEquals(10, res.size());

    }

    public void testReachEndOfResults() throws Exception {
        ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
        Query q = new Query();
        os.setResultsSize(10);
        Results res = os.execute(q);

        // Get the first 8 rows in a batch
        List rows = os.execute(q, 0, 7);
        assertEquals(8, rows.size());

        // Try and get the next 7
        rows = os.execute(q, 8, 14);
        assertEquals(2, rows.size());

        // Try and get rows 10 to 19
        rows = os.execute(q, 10, 19);
        assertEquals(0, rows.size());

        // Stupidly try and get beyond the end
        rows = os.execute(q, 15, 21);
        assertEquals(0, rows.size());
    }


    public void testexecuteCalls() throws Exception {
        ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
        Query q = new Query();
        os.setResultsSize(10);
        Results res = os.execute(q);
        os.execute(q, 1, 4);
        assertEquals(1, os.getExecuteCalls());
        os.execute(q, 5, 7);
        assertEquals(2, os.getExecuteCalls());
    }

    public void testPoisonRow() throws Exception {
        ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
        Query q = new Query();
        os.setResultsSize(10);
        Results res = os.execute(q);
        os.setPoisonRowNo(7);
        os.execute(q, 0, 4);
        os.execute(q, 8, 9);
        os.execute(q, 4, 6);
        try {
            os.execute(q, 0, 9);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
        }
        try {
            os.execute(q, 7, 9);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
        }
        try {
            os.execute(q, 7, 7);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
        }
        try {
            os.execute(q, 0, 7);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
        }
    }
}
