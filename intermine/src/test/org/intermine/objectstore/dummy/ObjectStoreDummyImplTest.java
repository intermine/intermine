package org.flymine.objectstore.dummy;

import junit.framework.TestCase;

import java.util.List;

import org.flymine.model.testmodel.*;
import org.flymine.objectstore.query.*;

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

}
