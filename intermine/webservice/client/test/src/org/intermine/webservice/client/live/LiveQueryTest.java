package org.intermine.webservice.client.live;

import static org.junit.Assert.assertEquals;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.results.Page;
import org.intermine.webservice.client.services.QueryService;
import org.intermine.webservice.client.services.QueryService.NumericSummary;
import org.intermine.webservice.client.util.TestUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

public class LiveQueryTest {

    private static final String EMP_NAME_2 = "Madge Madsen";
    private static final String EMP_NAME_1 = "EmployeeA1";
    static Map<String, PathQuery> queries;
    private static final String authToken = "test-user-token";
    private static final QueryService authorised =
            new ServiceFactory(TestUtil.getRootUrl(), authToken).getQueryService();
    private static final QueryService unauthorised =
            new ServiceFactory(TestUtil.getRootUrl()).getQueryService();
    private static final Page middle = new Page(5, 5);
    private static final int EXP_COUNT_1 = 9;
    private static final int EXP_COUNT_2 = 4;
    private static final int EXP_AGE = 26;

    @BeforeClass
    public static void oneTimeSetup() {
        Reader reader = new InputStreamReader(LiveQueryTest.class.getResourceAsStream("queries.xml"));
        queries = PathQueryBinding.unmarshalPathQueries(reader, PathQuery.USERPROFILE_VERSION);
    }

    @Test
    public void count() {
        PathQuery test1 = queries.get("test1");
        assertEquals(EXP_COUNT_1, unauthorised.getCount(test1.toXml()));

        assertEquals(EXP_COUNT_1, unauthorised.getCount(test1));
    }

    @Test
    public void count2() {
        PathQuery test2 = queries.get("test2");
        assertEquals(EXP_COUNT_2, authorised.getCount(test2.toXml()));

        assertEquals(EXP_COUNT_2, authorised.getCount(test2));
    }

    @Test(expected=ServiceException.class)
    public void count3() {
        PathQuery test2 = queries.get("test2");
        assertEquals(EXP_COUNT_2, unauthorised.getCount(test2));
    }

    @Test
    public void testJSONObjectCoherence() throws Exception {
        PathQuery test3 = queries.get("test3");
        PathQuery test4 = queries.get("test4");
        List<JSONObject> resultsA = unauthorised.getAllJSONResults(test3);
        List<JSONObject> resultsB = unauthorised.getAllJSONResults(test4);
        assertEquals(resultsA.size(), resultsB.size());
        int empCount = unauthorised.getCount(queries.get("emps-with-deps"));
        int nestedEmpCount = 0;
        for (JSONObject dep: resultsB) {
            JSONArray emps = dep.getJSONArray("employees");
            nestedEmpCount += emps.length();
        }
        assertEquals(empCount, nestedEmpCount);
    }

    @Test
    public void allJSON() throws JSONException {
        PathQuery test1 = queries.get("test1");
        List<JSONObject> results = unauthorised.getAllJSONResults(test1);
        assertEquals(EXP_COUNT_1, results.size());
        assertEquals(EMP_NAME_1, results.get(0).getString("name"));
        assertEquals(EXP_AGE, results.get(EXP_COUNT_1 - 1).getInt("age"));
    }

    @Test
    public void allJSON2() throws JSONException {
        PathQuery test1 = queries.get("test1");
        List<JSONObject> results = unauthorised.getAllJSONResults(test1.toXml());
        assertEquals(EXP_COUNT_1, results.size());
        assertEquals(EMP_NAME_1, results.get(0).getString("name"));
        assertEquals(EXP_AGE, results.get(EXP_COUNT_1 - 1).getInt("age"));
    }

    @Test
    public void someJSON() throws JSONException {
        PathQuery test1 = queries.get("test1");
        List<JSONObject> results = unauthorised.getJSONResults(test1, middle);
        assertEquals(EXP_COUNT_2, results.size());
        assertEquals(EMP_NAME_2, results.get(0).getString("name"));
        assertEquals(EXP_AGE, results.get(EXP_COUNT_2 - 1).getInt("age"));
    }

    @Test
    public void someJSON2() throws JSONException {
        PathQuery test1 = queries.get("test1");
        List<JSONObject> results = unauthorised.getJSONResults(test1.toXml(), middle);
        assertEquals(EXP_COUNT_2, results.size());
        assertEquals(EMP_NAME_2, results.get(0).getString("name"));
        assertEquals(EXP_AGE, results.get(EXP_COUNT_2 - 1).getInt("age"));
    }

    @Test
    public void allStrings() throws JSONException {
        PathQuery test1 = queries.get("test1");
        List<List<String>> results = unauthorised.getAllResults(test1);
        assertEquals(EXP_COUNT_1, results.size());
        assertEquals(EMP_NAME_1, results.get(0).get(1));
        assertEquals("" + EXP_AGE, results.get(EXP_COUNT_1 - 1).get(2));
    }

    @Test
    public void allStrings2() throws JSONException {
        PathQuery test1 = queries.get("test1");
        List<List<String>> results = unauthorised.getAllResults(test1.toXml());
        assertEquals(EXP_COUNT_1, results.size());
        assertEquals(EMP_NAME_1, results.get(0).get(1));
        assertEquals("" + EXP_AGE, results.get(EXP_COUNT_1 - 1).get(2));
    }

    @Test
    public void someStrings() throws JSONException {
        PathQuery test1 = queries.get("test1");
        List<List<String>> results = unauthorised.getResults(test1, middle);
        assertEquals(EXP_COUNT_2, results.size());
        assertEquals(EMP_NAME_2, results.get(0).get(1));
        assertEquals("" + EXP_AGE, results.get(EXP_COUNT_2 - 1).get(2));
    }

    @Test
    public void someStrings2() throws JSONException {
        PathQuery test1 = queries.get("test1");
        List<List<String>> results = unauthorised.getResults(test1.toXml(), middle);
        assertEquals(EXP_COUNT_2, results.size());
        assertEquals(EMP_NAME_2, results.get(0).get(1));
        assertEquals("" + EXP_AGE, results.get(EXP_COUNT_2 - 1).get(2));
    }

    @Test
    public void allObjects() throws JSONException {
        PathQuery test1 = queries.get("test1");
        List<List<Object>> results = unauthorised.getRowsAsLists(test1);
        assertEquals(EXP_COUNT_1, results.size());
        assertEquals(EMP_NAME_1, (String) results.get(0).get(1));
        assertEquals(new Integer(EXP_AGE), (Integer) results.get(EXP_COUNT_1 - 1).get(2));
    }

    @Test
    public void allObjects2() throws JSONException {
        PathQuery test1 = queries.get("test1");
        List<List<Object>> results = unauthorised.getRowsAsLists(test1.toXml());
        assertEquals(EXP_COUNT_1, results.size());
        assertEquals(EMP_NAME_1, (String) results.get(0).get(1));
        assertEquals(new Integer(EXP_AGE), (Integer) results.get(EXP_COUNT_1 - 1).get(2));
    }

    @Test
    public void someObjects() throws JSONException {
        PathQuery test1 = queries.get("test1");
        List<List<Object>> results = unauthorised.getRowsAsLists(test1, middle);
        assertEquals(EXP_COUNT_2, results.size());
        assertEquals(EMP_NAME_2, (String) results.get(0).get(1));
        assertEquals(new Integer(EXP_AGE), (Integer) results.get(EXP_COUNT_2 - 1).get(2));
    }

    @Test
    public void someObjects2() throws JSONException {
        PathQuery test1 = queries.get("test1");
        List<List<Object>> results = unauthorised.getRowsAsLists(test1.toXml(), middle);
        assertEquals(EXP_COUNT_2, results.size());
        assertEquals(EMP_NAME_2, (String) results.get(0).get(1));
        assertEquals(new Integer(EXP_AGE), (Integer) results.get(EXP_COUNT_2 - 1).get(2));
    }

    @Test
    public void allMaps() {
        PathQuery test1 = queries.get("test1");
        List<Map<String, Object>> results = unauthorised.getRowsAsMaps(test1);
        assertEquals(EXP_COUNT_1, results.size());
        assertEquals(EMP_NAME_1, (String) results.get(0).get("name"));
        assertEquals(new Integer(EXP_AGE), (Integer) results.get(EXP_COUNT_1 - 1).get("age"));
    }

    @Test
    public void allMaps2() {
        PathQuery test1 = queries.get("test1");
        List<Map<String, Object>> results = unauthorised.getRowsAsMaps(test1.toXml());
        assertEquals(EXP_COUNT_1, results.size());
        assertEquals(EMP_NAME_1, (String) results.get(0).get("name"));
        assertEquals(new Integer(EXP_AGE), (Integer) results.get(EXP_COUNT_1 - 1).get("age"));
    }

    @Test
    public void someMaps() {
        PathQuery test1 = queries.get("test1");
        List<Map<String, Object>> results = unauthorised.getRowsAsMaps(test1, middle);
        assertEquals(EXP_COUNT_2, results.size());
        assertEquals(EMP_NAME_2, (String) results.get(0).get("name"));
        assertEquals(new Integer(EXP_AGE), (Integer) results.get(EXP_COUNT_2 - 1).get("age"));
    }

    @Test
    public void someMaps2() {
        PathQuery test1 = queries.get("test1");
        List<Map<String, Object>> results = unauthorised.getRowsAsMaps(test1.toXml(), middle);
        assertEquals(EXP_COUNT_2, results.size());
        assertEquals(EMP_NAME_2, (String) results.get(0).get("name"));
        assertEquals(new Integer(EXP_AGE), (Integer) results.get(EXP_COUNT_2 - 1).get("age"));
    }

    @Test
    public void numericSummary() {
        PathQuery test1 = queries.get("test1");
        NumericSummary summary = unauthorised.getNumericSummary(test1, "age");
        assertEquals("Employee.age", summary.getColumn());
        assertEquals(29, summary.getMax(), 0.0001);
        assertEquals(10, summary.getMin(), 0.0001);
        assertEquals(24.55555555, summary.getAverage(), 0.0001);
        assertEquals(6.1259919, summary.getStandardDeviation(), 0.0001);
    }

    @Test
    public void nonNumericSummary() {
        PathQuery test1 = queries.get("test1");
        Map<String, Integer> summary = unauthorised.getSummary(test1, "department.name");
        assertEquals(new Integer(3), summary.get("Warehouse"));
        int sum = 0;
        for (Integer i: summary.values()) {
            sum += i;
        }
        assertEquals(sum, unauthorised.getCount(test1));
    }

    /// Bug tests

    @Test
    public void ticket2446() {
        PathQuery test1 = queries.get("lookup-quotes");
        PathQuery test2 = queries.get("multi-values");

        assertEquals("These queries should be equivalent",
                unauthorised.getCount(test2),
                unauthorised.getCount(test1)
                );
    }
}
