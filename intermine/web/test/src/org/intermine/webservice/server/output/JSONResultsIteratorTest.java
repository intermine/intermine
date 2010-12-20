package org.intermine.webservice.server.output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.api.query.MainHelper;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.dummy.DummyResults;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.util.IteratorIterable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import junit.framework.TestCase;

/**
 * Tests for the JSONResultsIterator class
 *
 * @author Alexis Kalderimis
 */

public class JSONResultsIteratorTest extends TestCase {
	
	/**
	 * Compare two JSONObjects for equality
	 * @param left
	 * @param right
	 * @return a String with messages explaining the problems if there are any, otherwise null (equal)
	 * @throws JSONException
	 */
	private static String getProblemsComparing(JSONObject left, Object right) throws JSONException {
		List<String> problems = new ArrayList<String>();
		
		if (left == null ) {
			if (right != null) {
				return "Expected null, but got " + right;
			} else {
				return null;
			}
		}
		
		if (! (right instanceof JSONObject)) {
			if (right == null) {
				return "Didn't expect null, but got null";
			} else {
				return "Expected a JSONObject, is got a " + right.getClass();
			}
		}
		JSONObject rjo = (JSONObject) right;
		Set<String> leftNames = new HashSet<String>(Arrays.asList(JSONObject.getNames(left)));
		Set<String> rightNames = new HashSet<String>(Arrays.asList(JSONObject.getNames(rjo)));
		if (! leftNames.equals(rightNames)) {
			problems.add("Expected the keys " + 
					leftNames + ", but got these: " + rightNames);
		}
		
		for (String name : leftNames) {
			Object leftValue = left.get(name);
			String problem = null;
			if (leftValue == null) {
				if ( rjo.get(name) != null ) {
					problem = "Expected null, but got " + rjo.get(name);
				}
			} else if (leftValue instanceof JSONObject) {
				problem = getProblemsComparing((JSONObject) leftValue, rjo.get(name));
			} else if (leftValue instanceof JSONArray) {
				problem = getProblemsComparing((JSONArray) leftValue, rjo.get(name));
			} else {
				if (! leftValue.toString().equals(rjo.get(name).toString())) {
					problem = "Expected " + leftValue + " but got " + rjo.get(name); 
				}
			}
			if (problem != null) {
				problems.add("Problem with " + name + ": " + problem);
			}
		}
		
		if (problems.isEmpty()) {
			return null;
		} 
		return problems.toString();
	}
	
	/**
	 * Compare two JSONArrays for equality
	 * @param left
	 * @param right
	 * @return a String with messages explaining the problems if there are any, otherwise null (equal)
	 * @throws JSONException
	 */
	private static String getProblemsComparing(JSONArray left, Object right) throws JSONException {
		List<String> problems = new ArrayList<String>();
		
		if (left == null ) {
			if (right != null) {
				return "Expected null, but got " + right;
			} else {
				return null;
			}
		}
		
		if (! (right instanceof JSONArray)) {
			return "Expected a JSONArray, but got a " + right.getClass();
		}
		
		JSONArray rja = (JSONArray) right;
		if (left.length() != rja.length()) {
			problems.add("Expected the size of this array to be " + left.length() + 
					" but got " + rja.length());
		}
		for (int index = 0; index < left.length(); index++) {
			Object leftMember = left.get(index);
			String problem = null;
			if (leftMember== null) {
				if ( rja.get(index) != null ) {
					problem = "Expected null, but got " + rja.get(index);
				}
			} else if (leftMember instanceof JSONObject) {
				problem = getProblemsComparing((JSONObject) leftMember, rja.get(index));
			} else if (leftMember instanceof JSONArray) {
				problem = getProblemsComparing((JSONArray) leftMember, rja.get(index));
			} else {
				if (! leftMember.toString().equals(rja.get(index).toString())) {
					problem = "Expected " + leftMember + 
						" but got " + rja.get(index);
				}
			}
			if (problem != null) {
				problems.add("Problem with index " + index + ": " + problem);
			}
		}
		
		if (problems.isEmpty()) {
			return null;
		} 
		return problems.toString();
		
	}

	private final Model model = Model.getInstanceByName("testmodel");
	
    public JSONResultsIteratorTest(String arg) {
        super(arg);
    }
    
	public void testNestedCollection() throws Exception {
	    ObjectStoreDummyImpl os = new ObjectStoreDummyImpl();
	    os.setResultsSize(1);
	
	    // Set up some known objects in the first 3 results rows
	    Company company1 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
	    company1.setName("Company1");
	    company1.setVatNumber(101);
	    company1.setId(new Integer(1));
	
	    Department department1 = new Department();
	    department1.setName("Department1");
	    department1.setId(new Integer(2));
	    Department department2 = new Department();
	    department2.setName("Department2");
	    department2.setId(new Integer(3));
	
	    Employee employee1 = new Employee();
	    employee1.setName("Employee1");
	    employee1.setId(new Integer(4));
	    employee1.setAge(42);
	    Employee employee2 = new Employee();
	    employee2.setName("Employee2");
	    employee2.setId(new Integer(5));
	    employee2.setAge(43);
	    Employee employee3 = new Employee();
	    employee3.setName("Employee3");
	    employee3.setId(new Integer(6));
	    employee3.setAge(44);
	    Employee employee4 = new Employee();
	    employee4.setName("Employee4");
	    employee4.setId(new Integer(7));
	    employee4.setAge(45);
	
	    ResultsRow row = new ResultsRow();
	    row.add(company1);
	    List sub1 = new ArrayList();
	    ResultsRow subRow1 = new ResultsRow();
	    subRow1.add(department1);
	    List sub2 = new ArrayList();
	    ResultsRow subRow2 = new ResultsRow();
	    subRow2.add(employee1);
	    sub2.add(subRow2);
	    subRow2 = new ResultsRow();
	    subRow2.add(employee2);
	    sub2.add(subRow2);
	    subRow1.add(sub2);
	    sub1.add(subRow1);
	    subRow1 = new ResultsRow();
	    subRow1.add(department2);
	    sub2 = new ArrayList();
	    subRow2 = new ResultsRow();
	    subRow2.add(employee3);
	    sub2.add(subRow2);
	    subRow2 = new ResultsRow();
	    subRow2.add(employee4);
	    sub2.add(subRow2);
	    subRow1.add(sub2);
	    sub1.add(subRow1);
	    row.add(sub1);
	    os.addRow(row);
	    
	    PathQuery pq = new PathQuery(model);
        pq.addViews("Company.name", "Company.vatNumber", "Company.departments.name", "Company.departments.employees.name");
        pq.setOuterJoinStatus("Company.departments", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.departments.employees", OuterJoinStatus.OUTER);
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 1, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
                
        JsonResultsIterator jsonIter = new JsonResultsIterator(iter);
        
        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }
        
        assertEquals(got.size(), 1);
        
        String jsonString = "{" +
        	"\"objectId\" : 1," +
        	"\"name\" : \"Company1\"," +
        	"\"class\" : \"Company\"," +
        	"\"departments\" : [" +
        		"{" +
	        		"\"objectId\" : 2," +
	        		"\"name\":\"Department1\"," +
	        		"\"class\":\"Department\"," +
	        		"\"employees\" : [" +
	        			"{" +
	        				"\"objectId\" : 4," +
	        				"\"name\" : \"Employee1\"," +
	        				"\"class\" : \"Employee\"" +
	        			"}," +
	        			"{" +
	        			"	\"objectId\" : 5," +
	        			"	\"name\" : \"Employee2\"," +
	        			"	\"class\":\"Employee\"" +
	        			"}" +
	        		"]" +
	        	"}," +
	        	"{" +
	        	"	\"objectId\" : 3," +
	        	"	\"name\" : \"Department2\", " +
	        	"	\"class\" : \"Department\"," +
	        	"	\"employees\" : [" +
	        	"		{" +
	        	"			\"objectId\" : 6," +
	        	"			\"name\" : \"Employee3\"," +
	        	"			\"class\" : \"Employee\"" +
	        	"		}," +
	        	"		{" +
	        	"			\"objectId\" : 7," +
	        	"			\"name\" : \"Employee4\"," +
	        	"			\"class\" : \"Employee\"" +
	        	"		}" +
	        	"	]" +
	        	"}" +
	        "]," +
	        "\"vatNumber\" : \"101\"" +
	      "}";
        
        
        JSONObject expected = new JSONObject(jsonString);
        assertEquals(null, getProblemsComparing(expected, got.get(0)));
        
    }
   
}
