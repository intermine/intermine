package org.intermine.webservice.client.results;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class RowResultSetTest extends TestCase {

    private List<String> views = Arrays.asList(
        "Employee.age",
        "Employee.fullTime",
        "Employee.name",
        "Employee.end",
        "Employee.manager.end"
        );

    private List<Integer> expectedAges = Arrays.asList(
            10, 20, 25, 25, 26,
            27, 27, 28, 28, 28,
            28, 29, 29, 29, 29
            );

    private RowResultSet resultset = null;

    public RowResultSetTest() {
    }

    public RowResultSetTest(String name) {
        super(name);
    }

    @Override
    public void setUp() {
        InputStream is = getClass().getResourceAsStream("resultrowset.json");
        resultset = new RowResultSet(is, views);
    }

    public void testGetRowsAsLists() {
        List<List<Object>> rows = resultset.getRowsAsLists();
        assertEquals(rows.get(0).get(0), 10);
        assertEquals(rows.get(1).get(2), "EmployeeA2");
        assertEquals(rows.get(2).get(1), false);
        assertEquals(rows.get(3).get(4), null);
        assertEquals(rows.get(14).get(2), "Stephane");
        assertEquals(rows.size(), 15);
    }

    public void testGetData() {
        List<List<String>> rows = resultset.getData();
        assertEquals(rows.get(0).get(0), "10");
        assertEquals(rows.get(1).get(2), "EmployeeA2");
        assertEquals(rows.get(2).get(1), "false");
        assertEquals(rows.get(3).get(4), "null");
        assertEquals(rows.get(14).get(2), "Stephane");
        assertEquals(rows.size(), 15);
    }

    public void testGetRowsAsMaps() {
        List<Map<String, Object>> rows = resultset.getRowsAsMaps();
        assertEquals(rows.get(0).get("age"), 10);
        assertEquals(rows.get(1).get("name"), "EmployeeA2");
        assertEquals(rows.get(2).get("fullTime"), false);
        assertEquals(rows.get(3).get("manager.end"), null);
        assertEquals(rows.get(14).get("Employee.name"), "Stephane");
        assertEquals(rows.size(), 15);
    }

    public void testListIterator() {
        int i = 0;
        Iterator<List<Object>> it = resultset.getListIterator();
        while (it.hasNext()) {
            List<Object> row = it.next();
            assertEquals(expectedAges.get(i++), row.get(0));
        }
        assertEquals(i, 15);
    }

    public void testMapIterator() {
        int i = 0;
        Iterator<Map<String, Object>> it = resultset.getMapIterator();
        while (it.hasNext()) {
            Map<String, Object> row = it.next();
            assertEquals(expectedAges.get(i), row.get("Employee.age"));
            assertEquals(expectedAges.get(i++), row.get("age"));
        }
        assertEquals(i, 15);
    }
}





