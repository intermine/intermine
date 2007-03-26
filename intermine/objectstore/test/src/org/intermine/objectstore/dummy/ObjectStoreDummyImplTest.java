package org.intermine.objectstore.dummy;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.util.List;

import org.intermine.model.testmodel.*;
import org.intermine.objectstore.query.*;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.testing.OneTimeTestCase;

public class ObjectStoreDummyImplTest extends OneTimeTestCase
{
    protected static ObjectStoreDummyImpl os;

    public ObjectStoreDummyImplTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(ObjectStoreDummyImplTest.class);
    }

    public static void oneTimeSetUp() throws Exception {
        OneTimeTestCase.oneTimeSetUp();
        os = new ObjectStoreDummyImpl();
        os.setExecuteTime(10);
        os.setMaxTime(20);
    }

    public void testAddRowRetrieveSame() throws Exception {
        os = new ObjectStoreDummyImpl();
        ResultsRow row = new ResultsRow();
        String field1 = "test1";
        String field2 = "test2";
        row.add(field1);
        row.add(field2);

        os.addRow(row);
        os.setResultsSize(1);
        List rows = os.execute(new Query(), 0, 1, true, true, 0);

        assertEquals(1, rows.size());
        ResultsRow newRow = (ResultsRow) rows.get(0);
        assertEquals(2, newRow.size());
        assertEquals("test1", (String) newRow.get(0));
        assertEquals("test2", (String) newRow.get(1));
    }

    public void testRetrieveNew() throws Exception {
        os = new ObjectStoreDummyImpl();
        Query q = new Query();
        q.addToSelect(new QueryClass(String.class));
        q.addToSelect(new QueryClass(Department.class));

        os.setResultsSize(1);
        List rows = os.execute(q, 0, 1, true, true, 0);

        assertEquals(1, rows.size());
        ResultsRow newRow = (ResultsRow) rows.get(0);
        assertEquals(2, newRow.size());
        assertTrue(newRow.get(0) instanceof String);
        assertTrue(newRow.get(1) instanceof Department);
    }

    public void testRetrieveNewAfterAdd() throws Exception {
        os = new ObjectStoreDummyImpl();
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
        List rows = os.execute(q, 0, 2, true, true, 0);

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
        os = new ObjectStoreDummyImpl();
        Query q = new Query();
        os.setResultsSize(10);
        Results res = os.execute(q);

        assertEquals(10, res.size());

    }

    public void testReachEndOfResults() throws Exception {
        os = new ObjectStoreDummyImpl();
        Query q = new Query();
        os.setResultsSize(10);
        Results res = os.execute(q);

        // Get the first 8 rows in a batch
        List rows = os.execute(q, 0, 8, true, true, 0);
        assertEquals(8, rows.size());

        // Try and get the next 7
        rows = os.execute(q, 8, 7, true, true, 0);
        assertEquals(2, rows.size());

        // Try and get rows 10 to 19
        rows = os.execute(q, 10, 10, true, true, 0);
        assertEquals(0, rows.size());

        // Stupidly try and get beyond the end
        rows = os.execute(q, 15, 10, true, true, 0);
        assertEquals(0, rows.size());
    }


    public void testExecuteCalls() throws Exception {
        os = new ObjectStoreDummyImpl();
        Query q = new Query();
        os.setResultsSize(10);
        Results res = os.execute(q);
        os.execute(q, 1, 3, true, true, 0);
        assertEquals(1, os.getExecuteCalls());
        os.execute(q, 5, 2, true, true, 0);
        assertEquals(2, os.getExecuteCalls());
    }

    public void testPoisonRow() throws Exception {
        os = new ObjectStoreDummyImpl();
        Query q = new Query();
        os.setResultsSize(10);
        os.setPoisonRowNo(7);
        os.execute(q, 0, 5, true, true, 0);
        os.execute(q, 8, 2, true, true, 0);
        os.execute(q, 4, 3, true, true, 0);
        try {
            os.execute(q, 0, 10, true, true, 0);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
        }
        try {
            os.execute(q, 7, 3, true, true, 0);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
        }
        try {
            os.execute(q, 7, 1, true, true, 0);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
        }
        try {
            os.execute(q, 0, 8, true, true, 0);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
        }
    }

    public void testCount() throws Exception {
        ObjectStoreDummyImpl objectStore = new ObjectStoreDummyImpl();
        objectStore.setResultsSize(12);
        Query q = new Query();
        assertEquals(objectStore.count(q, 0), 12);
    }
}
