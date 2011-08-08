package org.intermine.webservice.client.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;

import org.apache.commons.io.IOUtils;

import junit.framework.TestCase;

public class ResultRowMapTest extends TestCase {

	private JSONArray testData;
	private ResultRowMap row;
	private List<String> views = Arrays.asList(
		"Employee.name",
		"Employee.age",
		"Employee.fullTime",
		"Employee.end");
	
	public ResultRowMapTest() throws Exception {
		init();
	}

	public ResultRowMapTest(String name) throws Exception {
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
		row = new ResultRowMap(testData, views); 
	}
	
	public void testGet() {
		assertEquals(row.get("Employee.name"), "Tom");
		assertEquals(row.get("Employee.age"), 33);
		assertEquals(row.get("Employee.fullTime"), true);
		assertEquals(row.get("Employee.end"), null);

		assertEquals(row.get("name"), "Tom");
		assertEquals(row.get("age"), 33);
		assertEquals(row.get("fullTime"), true);
		assertEquals(row.get("end"), null);
	}

    public void testValues() {
        assertEquals(row.values(), Arrays.asList("Tom", 33, true, null));
    }
    
    public void testIteration() {
    	int i = 0;
		List<? extends Object> expected = Arrays.asList("Tom", 33, true, null);
		for (Entry<String, Object> e: row.entrySet()) {
			assertEquals(e.getValue(), expected.get(i));
			assertEquals(e.getKey(), views.get(i++));
		}
    }

    public void testContains() {
        assertTrue(row.containsValue("Tom"));
        assertTrue(row.containsValue(33));
        assertTrue(row.containsValue(true));
        assertTrue(row.containsValue(null));

        assertFalse(row.containsValue(false));
        assertFalse(row.containsValue("Bill"));
    }

    public void testKeys() {
        assertEquals(row.keySet(), new TreeSet<String>(views));
    }

}
