package org.intermine.webservice.client.results;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;

public class ResultRowListTest extends TestCase {

    private JSONArray testData;
    private ResultRowList row;

    public ResultRowListTest() throws Exception {
        init();
    }

    public ResultRowListTest(String name) throws Exception {
        super(name);
        init();
    }

    private void init() throws Exception {
    InputStream is = getClass().getResourceAsStream("resultrow.json");
    String input = IOUtils.toString(is);
        try {
            testData = new JSONArray(input);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setUp() {
        row = new ResultRowList(testData);
    }

    public void testGet() {
        assertEquals(row.get(0), "Tom");
        assertEquals(row.get(1), 33);
        assertEquals(row.get(2), true);
        assertEquals(row.get(3), null);

        try {
            row.get(-1);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
        try {
            row.get(4);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
    }

    public void testIteration() {
        int i = 0;
        @SuppressWarnings("unchecked")
        List<? extends Object> expected = Arrays.asList("Tom", 33, true, null);
        for (Object o: row) {
            assertEquals(o, expected.get(i++));
        }
    }

    @SuppressWarnings("unchecked")
    public void testSubList() {
        assertEquals(row.subList(1, 3), Arrays.asList(33, true));
    }

    public void testContains() {
        assertTrue(row.contains("Tom"));
        assertTrue(row.contains(33));
        assertTrue(row.contains(true));
        assertTrue(row.contains(null));

        assertFalse(row.contains(false));
        assertFalse(row.contains("Bill"));
    }

}
