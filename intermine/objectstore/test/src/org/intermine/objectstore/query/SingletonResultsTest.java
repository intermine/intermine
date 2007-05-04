package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.List;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;

public class SingletonResultsTest extends TestCase
{
    public SingletonResultsTest(String arg1) {
        super(arg1);
    }

    private ObjectStoreDummyImpl os;
    private Query q;

    public void setUp() throws Exception {

        // Set up a dummy ObjectStore with 10 rows to return
        os = new ObjectStoreDummyImpl();
        os.setResultsSize(10);

        for (int i = 0; i < 10; i++) {
            ResultsRow row = new ResultsRow();
            Employee e = new Employee();
            e.setName("" + i);
            row.add(e);
            os.addRow(row);
        }

        q = new Query();
        QueryClass qc1 = new QueryClass(Department.class);

        q.addToSelect(qc1);
        q.addFrom(qc1);

    }

    public void testConstructMultiColumnQuery() throws Exception {
        q = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Employee.class);

        q.addToSelect(qc1);
        q.addToSelect(qc2);
        q.addFrom(qc1);

        try {
            Results res = os.executeSingleton(q);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

    }

    public void testConstructNullQuery() throws Exception {
        try {
            Results res = new SingletonResults(null, new ObjectStoreDummyImpl(), ObjectStore.SEQUENCE_IGNORE);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testConstructNullObjectStore() throws Exception {
        try {
            Results res = new SingletonResults(new Query(), null, ObjectStore.SEQUENCE_IGNORE);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testGet() throws Exception {
        Results res = os.executeSingleton(q);

        assertTrue(res.get(0) instanceof Employee);
        assertEquals("0", ((Employee) res.get(0)).getName());

    }

    public void testRange() throws Exception {
        Results res = os.executeSingleton(q);
        List objs = res.range(0,5);

        for (int i = 0; i <= 5; i++) {
            assertTrue(objs.get(i) instanceof Employee);
            assertEquals("" + i, ((Employee) objs.get(i)).getName());
        }
    }


    public void testGetOutOfBounds() throws Exception {
        Results res = os.executeSingleton(q);
        try {
            res.get(10);
            fail("Expected IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException e) {
        }
    }

    public void testRangeOutOfBounds() throws Exception {
        Results res = os.executeSingleton(q);
        res.setBatchSize(5);
        try {
            res.range(6,11);
            fail("Expected IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException e) {
        }
    }

}
