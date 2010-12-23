package org.intermine.webservice.server.output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

import org.intermine.api.query.MainHelper;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Contractor;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.model.testmodel.Types;
import org.intermine.objectstore.dummy.DummyResults;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.util.IteratorIterable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Tests for the JSONResultsIterator class
 *
 * @author Alexis Kalderimis
 */

public class JSONResultsIteratorTest extends TestCase {
	
	/**
	 * Compare two JSONObjects for equality
	 * @param left The reference JSONObject (this is referred to as "expected" in any messages)
	 * @param right The candidate object to check.
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
			try {
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
			} catch (Throwable e) {
				problem = e.toString();
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
	 * @param left The reference array (referred to as "expected" in any messages)
	 * @param right The candidate object to check.
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
			try {
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
			} catch (Throwable e) {
				problem = e.toString();
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

	private ObjectStoreDummyImpl os;
	private Company wernhamHogg;
	private CEO jennifer;
	private Manager david;
	private Manager taffy;
	private Employee tim;
	private Employee gareth;
	private Employee dawn;
	private Employee keith;
	private Employee lee;
	private Employee alex;
	private Department sales;
	private Department reception;
	private Department accounts;
	private Department distribution;
	private Address address;
	private Contractor rowan;
	private Contractor ray;
	private Contractor jude;
	private Department swindon;
	private Manager neil;
	private Employee rachel;
	private Employee trudy;
	

    protected void setUp() {
    	os = new ObjectStoreDummyImpl();
    	
        wernhamHogg = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        wernhamHogg.setId(new Integer(1));
        wernhamHogg.setName("Wernham-Hogg");
        wernhamHogg.setVatNumber(101);
        
        jennifer = new CEO();
        jennifer.setId(new Integer(2));
        jennifer.setName("Jennifer Taylor-Clarke");
        jennifer.setAge(42);
        
        david = new Manager();
        david.setId(new Integer(3));
        david.setName("David Brent");
        david.setAge(39);
        
        taffy = new Manager();
        taffy.setId(new Integer(4));
        taffy.setName("Glynn");
        taffy.setAge(38);
        
        tim = new Employee();
        tim.setId(new Integer(5));
        tim.setName("Tim Canterbury");
        tim.setAge(30);
        
        gareth = new Employee();
        gareth.setId(new Integer(6));
        gareth.setName("Gareth Keenan");
        gareth.setAge(32);
        
        dawn = new Employee();
        dawn.setId(new Integer(7));
        dawn.setName("Dawn Tinsley");
        dawn.setAge(26);
        
        keith = new Employee();
        keith.setId(new Integer(8));
        keith.setName("Keith Bishop");
        keith.setAge(41);
        
        lee = new Employee();
        lee.setId(new Integer(9));
        lee.setName("Lee");
        lee.setAge(28);
        
        alex = new Employee();
        alex.setId(new Integer(10));
        alex.setName("Alex");
        alex.setAge(24);
        
        sales = new Department();
        sales.setId(new Integer(11));
        sales.setName("Sales");
        
        accounts = new Department();
        accounts.setId(new Integer(12));
        accounts.setName("Accounts");
        
        distribution = new Department();
        distribution.setId(new Integer(13));
        distribution.setName("Warehouse");
        
        reception = new Department();
        reception.setId(new Integer(14));
        reception.setName("Reception");
        
        address = new Address();
        address.setId(new Integer(15));
        address.setAddress("42 Some St, Slough");
        
        rowan = new Contractor();
        rowan.setId(new Integer(16));
        rowan.setName("Rowan");
        
        ray = new Contractor();
        ray.setId(new Integer(17));
        ray.setName("Ray");
        
        jude = new Contractor();
        jude.setId(new Integer(18));
        jude.setName("Jude");
        
        swindon = new Department();
        swindon.setId(new Integer(19));
        swindon.setName("Swindon");
        
        neil = new Manager();
        neil.setId(new Integer(20));
        neil.setName("Neil Godwin");
        neil.setAge(35);
        
        rachel = new Employee();
        rachel.setId(new Integer(21));
        rachel.setName("Rachel");
        rachel.setAge(34);
        
        trudy = new Employee();
        trudy.setId(new Integer(22));
        trudy.setName("Trudy");
        trudy.setAge(25);
        
        
    }
	
	private final Model model = Model.getInstanceByName("testmodel");
	
    public JSONResultsIteratorTest(String arg) {
        super(arg);
    }
    
    public void testSingleSimpleObject() throws Exception {
    	os.setResultsSize(1);
    	
    	String jsonString = "{ 'class': 'Manager', 'objectId': 3, 'age': 39, 'name': 'David Brent' }";
    	JSONObject expected = new JSONObject(jsonString);
    	
    	ResultsRow row = new ResultsRow();
    	row.add(david);
    	
    	os.addRow(row);
    	
    	PathQuery pq = new PathQuery(model);
        pq.addViews("Manager.name", "Manager.age");

        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 5, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
                
        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
        
        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }
        
        assertEquals(1, got.size());
        
        assertEquals(null, getProblemsComparing(expected, got.get(0)));	
        
    }
    
    public void testMultipleSimpleObjects() throws Exception {
    	os.setResultsSize(5);	
	
	    List<String> jsonStrings = new ArrayList<String>();
        jsonStrings.add("{ 'class':'Employee', 'objectId':5, 'age':30, 'name': 'Tim Canterbury' }");
	    jsonStrings.add("{ 'class':'Employee', 'objectId':6, 'age':32, 'name': 'Gareth Keenan' }");
	    jsonStrings.add("{ 'class':'Employee', 'objectId':7, 'age':26, 'name': 'Dawn Tinsley' }");
	    jsonStrings.add("{ 'class':'Employee', 'objectId':8, 'age':41, 'name': 'Keith Bishop' }");
	    jsonStrings.add("{ 'class':'Employee', 'objectId':9, 'age':28, 'name': 'Lee' }");
	    
	    ResultsRow row1 = new ResultsRow();
	    row1.add(tim);
	    ResultsRow row2 = new ResultsRow();
	    row2.add(gareth);
	    ResultsRow row3 = new ResultsRow();
	    row3.add(dawn);
	    ResultsRow row4 = new ResultsRow();
	    row4.add(keith);
	    ResultsRow row5 = new ResultsRow();
	    row5.add(lee);
	    
	    os.addRow(row1);
	    os.addRow(row2);
	    os.addRow(row3);
	    os.addRow(row4);
	    os.addRow(row5);
	    
	    PathQuery pq = new PathQuery(model);
        pq.addViews("Employee.name", "Employee.age", "Employee.id");
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 5, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
                
        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
        
        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }
        
        assertEquals(5, got.size());
        for (int i = 0; i < jsonStrings.size(); i++) {
        	JSONObject jo = new JSONObject(jsonStrings.get(i));
        	assertEquals(null, getProblemsComparing(jo, got.get(i)));	
        }
        
    }
    
	public void testSingleObjectWithNestedCollections() throws Exception {
	    os.setResultsSize(1);
		
	    ResultsRow row = new ResultsRow();
	    row.add(wernhamHogg);
	    List sub1 = new ArrayList();
	    ResultsRow subRow1 = new ResultsRow();
	    subRow1.add(sales);
	    List sub2 = new ArrayList();
	    ResultsRow subRow2 = new ResultsRow();
	    subRow2.add(tim);
	    sub2.add(subRow2);
	    subRow2 = new ResultsRow();
	    subRow2.add(gareth);
	    sub2.add(subRow2);
	    subRow1.add(sub2);
	    sub1.add(subRow1);
	    subRow1 = new ResultsRow();
	    subRow1.add(distribution);
	    sub2 = new ArrayList();
	    subRow2 = new ResultsRow();
	    subRow2.add(lee);
	    sub2.add(subRow2);
	    subRow2 = new ResultsRow();
	    subRow2.add(alex);
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
                
        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
        
        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }
        
        assertEquals(got.size(), 1);
        
        String jsonString = "{" +
        "	'objectId'    : 1," +
        "	'class'       : 'Company'," +
        "	'name' 	      : 'Wernham-Hogg'," +
        "	'vatNumber'   : 101," +
        "	'departments' : [" +
        "		{" +
	    "			'objectId'  : 11," +
	    "			'class'     : 'Department'," +
	    "			'name'      : 'Sales'," +
	    "			'employees' : [" +
	    "				{ " +
	    "					'objectId' : 5," +
	    "					'class'    : 'Employee'," +
	    "					'name'     : 'Tim Canterbury'" +
	    "				}, " +
	    "				{ " +
	    "					'objectId' : 6," +
	    "					'class'	   : 'Employee'," +
	    "					'name' 	   : 'Gareth Keenan'" +
	    "				}" +
	    "			]" +
	    "		}," +
	    "		{" +
	    "			'objectId'  : 13," +
	    "			'class'     : 'Department'," +
	    "			'name'      : 'Warehouse', " +	        	
	    "			'employees' : [" +
    	"				{" +
    	"					'objectId' : 9," +
    	"					'class'    : 'Employee'," +
    	"					'name'     : 'Lee'" +
    	"				}," +
    	"				{" +
    	"					'objectId' : 10," +
    	"					'class'    : 'Employee'," +
    	"					'name'     : 'Alex'" +
    	"				}" +
    	"			]" +
    	"		}" +
	    "	]" +   
	    "}";
        
        
        JSONObject expected = new JSONObject(jsonString);
        assertEquals(null, getProblemsComparing(expected, got.get(0)));
        
    }
	public void testSingleObjectWithNestedCollectionsAndMultipleAttributes() throws Exception {
	    os.setResultsSize(1);
	    
	    ResultsRow row = new ResultsRow();
	    row.add(wernhamHogg);
	    List sub1 = new ArrayList();
	    ResultsRow subRow1 = new ResultsRow();
	    subRow1.add(sales);
	    List sub2 = new ArrayList();
	    ResultsRow subRow2 = new ResultsRow();
	    subRow2.add(tim);
	    sub2.add(subRow2);
	    subRow2 = new ResultsRow();
	    subRow2.add(gareth);
	    sub2.add(subRow2);
	    subRow1.add(sub2);
	    sub1.add(subRow1);
	    subRow1 = new ResultsRow();
	    subRow1.add(distribution);
	    sub2 = new ArrayList();
	    subRow2 = new ResultsRow();
	    subRow2.add(lee);
	    sub2.add(subRow2);
	    subRow2 = new ResultsRow();
	    subRow2.add(alex);
	    sub2.add(subRow2);
	    subRow1.add(sub2);
	    sub1.add(subRow1);
	    row.add(sub1);
	    os.addRow(row);
	    
	    PathQuery pq = new PathQuery(model);
        pq.addViews("Company.name", "Company.vatNumber", 
        				"Company.departments.name", 
        					"Company.departments.employees.name", "Company.departments.employees.age");
        pq.setOuterJoinStatus("Company.departments", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.departments.employees", OuterJoinStatus.OUTER);
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 1, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
                
        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
        
        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }
        
        assertEquals(got.size(), 1);

        String jsonString = "{" +
        "	'objectId'    : 1," +
        "	'class'       : 'Company'," +
        "	'name' 	      : 'Wernham-Hogg'," +
        "	'vatNumber'   : 101," +
        "	'departments' : [" +
        "		{" +
	    "			'objectId'  : 11," +
	    "			'class'     : 'Department'," +
	    "			'name'      : 'Sales'," +
	    "			'employees' : [" +
	    "				{ " +
	    "					'objectId' : 5," +
	    "					'class'    : 'Employee'," +
	    "					'name'     : 'Tim Canterbury'," +
	    "					'age'	   : 30" +
	    "				}, " +
	    "				{ " +
	    "					'objectId' : 6," +
	    "					'class'	   : 'Employee'," +
	    "					'name' 	   : 'Gareth Keenan'," +
	    "					'age'      : 32" +
	    "				}" +
	    "			]" +
	    "		}," +
	    "		{" +
	    "			'objectId'  : 13," +
	    "			'class'     : 'Department'," +
	    "			'name'      : 'Warehouse', " +	        	
	    "			'employees' : [" +
    	"				{" +
    	"					'objectId' : 9," +
    	"					'class'    : 'Employee'," +
    	"					'name'     : 'Lee'," +
    	"					'age'      : 28" +
    	"				}," +
    	"				{" +
    	"					'objectId' : 10," +
    	"					'class'    : 'Employee'," +
    	"					'name'     : 'Alex'," +
    	"					'age'      : 24" +
    	"				}" +
    	"			]" +
    	"		}" +
	    "	]" +   
	    "}";
        
        
        JSONObject expected = new JSONObject(jsonString);
        assertEquals(null, getProblemsComparing(expected, got.get(0)));
        
    }
	

	// Attributes on references should precede references on references
	// The dummy attribute "id" can be used without populating the object with
	// unwanted stuff.
	public void testSingleObjectWithTrailOfReferences() throws Exception {
	    os.setResultsSize(1);
	
	    PathQuery pq = new PathQuery(model);
        pq.addViews("Department.name", "Department.company.id", "Department.company.CEO.name", "Department.company.CEO.address.address");
        
        String jsonString = "{" +
        		"				'class'    : 'Department'," +
        		"				'objectId' : 11," +
        		"				'name'     : 'Sales'," +
        		"				'company'  : {" +
        		"					'class'    : 'Company'," +
        		"					'objectId' : 1," +
        		"					'CEO'      : {" +
        		"						'class'    : 'CEO'," +
        		"						'objectId' : 2," +
        		"						'name'     : 'Jennifer Taylor-Clarke'," +
        		"						'address'  : {" +
        		"							'class'    : 'Address'," +
        		"							'objectId' : 15," +
        		"							'address'  : '42 Some St, Slough'" +
        		"						}" +
        		"					}" +
        		"				}" +
        		"			}";
        JSONObject expected = new JSONObject(jsonString);
        
        ResultsRow row = new ResultsRow();
        row.add(sales);
        row.add(wernhamHogg);
        row.add(jennifer);
        row.add(address);
        os.addRow(row);
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 1, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
                
        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
        
        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }
        
        assertEquals(got.size(), 1);
        
        assertEquals(null, getProblemsComparing(expected, got.get(0)));
        
	}
	
	// Attributes on references should precede references on references
	// Not doing so causes an error
	public void testBadReferenceTrail() throws Exception {
	    os.setResultsSize(1);
	    
        ResultsRow row = new ResultsRow();
        row.add(sales);
        row.add(jennifer);
        row.add(address);
        os.addRow(row);
        
        PathQuery pq = new PathQuery(model);
        pq.addViews("Department.name", "Department.company.CEO.name", "Department.company.CEO.address.address");
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 1, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
                
        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
        
        try {
	        List<JSONObject> got = new ArrayList<JSONObject>();
	        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
	            got.add(gotRow);
	        }
	        fail("No exception was thrown dealing with bad query - got this list: " + got);
        } catch (AssertionFailedError e) {
        	// rethrow the fail from within the try
        	throw e; 
        } catch (JSONFormattingException e) {
        	// Test that this is what we thought would happen.
	    	assertEquals("This node is not fully initialised: it has no objectId", e.getMessage());
	    } catch (Throwable e){
	    	// All other exceptions are failures
	    	fail("Got unexpected error: " + e); 
	    }
                
	}
	
	// Attributes on references should precede references on references
	public void testSingleObjectWithACollection() throws Exception {
	    os.setResultsSize(2);
	
        ResultsRow row = new ResultsRow();
        row.add(david);
        row.add(sales);
        row.add(tim);
        os.addRow(row);
        ResultsRow row2 = new ResultsRow();
        row2.add(david);
        row2.add(sales);
        row2.add(gareth);
        os.addRow(row2);
        
        String jsonString =
        "{" +
        "	'objectId'    : 3," +
        "	'class'       : 'Manager'," +
        "	'name' 	      : 'David Brent'," +
        "	'age'         : 39," +
        "	'department' : {" +
	    "			'objectId'  : 11," +
	    "			'class'     : 'Department'," +
	    "			'name'      : 'Sales'," +
	    "			'employees' : [" +
	    "				{ " +
	    "					'objectId' : 5," +
	    "					'class'    : 'Employee'," +
	    "					'name'     : 'Tim Canterbury'," +
	    "					'age'	   : 30" +
	    "				}, " +
	    "				{ " +
	    "					'objectId' : 6," +
	    "					'class'	   : 'Employee'," +
	    "					'name' 	   : 'Gareth Keenan'," +
	    "					'age'      : 32" +
	    "				}" +
	    "			]" +
	    "	}" +
	    "}";
        
        PathQuery pq = new PathQuery(model);
        pq.addViews("Manager.name", "Manager.age", "Manager.department.name", "Manager.department.employees.name", "Manager.department.employees.age"); 
        
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 2, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
                
        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
        
        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }
        
        assertEquals(got.size(), 1);

        JSONObject expected = new JSONObject(jsonString);
        assertEquals(null, getProblemsComparing(expected, got.get(0)));
                    
	}
	
	// Attributes on references should precede references on references
	// Not doing so causes an error
	public void testBadCollectionTrail() throws Exception {
	    os.setResultsSize(2);
	
        ResultsRow row = new ResultsRow();
        row.add(david);
        row.add(tim);
        os.addRow(row);
        ResultsRow row2 = new ResultsRow();
        row2.add(david);
        row2.add(gareth);
        os.addRow(row2);
        
        PathQuery pq = new PathQuery(model);
        pq.addViews("Manager.name", "Manager.department.employees.name"); 
        
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 2, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
                
        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
        
        try {
	        List<JSONObject> got = new ArrayList<JSONObject>();
	        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
	            got.add(gotRow);
	        }
	        fail("No exception was thrown dealing with bad query - got this list: " + got);
        } catch (AssertionFailedError e) {
        	// rethrow the fail from within the try
        	throw e; 
        } catch (JSONFormattingException e) {
        	// Test that this is what we thought would happen.
	    	assertEquals("This node is not properly initialised (it doesn't have an objectId) - is the view in the right order?", e.getMessage());
	    } catch (Throwable e){
	    	// All other exceptions are failures
	    	fail("Got unexpected error: " + e); 
	    }
                
	}
	
	public void testHeadlessQuery() throws Exception {
	    os.setResultsSize(2);
	    
        ResultsRow row = new ResultsRow();
        row.add(tim);
        os.addRow(row);
        ResultsRow row2 = new ResultsRow();
        row2.add(gareth);
        os.addRow(row2);
        
        PathQuery pq = new PathQuery(model);
        pq.addViews("Manager.department.employees.name", "Manager.department.employees.age"); 
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 2, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
                
        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
        
        try {
	        List<JSONObject> got = new ArrayList<JSONObject>();
	        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
	            got.add(gotRow);
	        }
	        fail("No exception was thrown dealing with bad query - got this list: " + got);
        } catch (AssertionFailedError e) {
        	// rethrow the fail from within the try
        	throw e; 
        } catch (JSONFormattingException e) {
        	// Test that this is what we thought would happen.
	    	assertEquals("Head of the object is missing", e.getMessage());
	    } catch (Throwable e){
	    	// All other exceptions are failures
	    	fail("Got unexpected error: " + e); 
	    }
                
	}
	
	public void testParallelCollection() throws Exception {
        os.setResultsSize(1);

	    String jsonString = 
	    		"{" +
	    		"	'class' : 'Company'," +
	    		"	'objectId' : 1," +
	    		"	'name'     : 'Wernham-Hogg'," +
	    		"	'vatNumber' : 101," +
	    		"	departments : [" +
	    		"		{"	+
	    		"   		'class' : 'Department'," +
	    		"			'objectId' : 11," +
	    		"			'name'     : 'Sales'" +
	    		"		}," +
	    		"		{" + 		
	    		"   		'class' : 'Department'," +
	    		"			'objectId' : 12," +
	    		"			'name' : 'Accounts'" +
	    		"		}" +
	    		"	]," +
	    		"	contractors : [" +
	    		"		{"	+
	    		"   		'class' : 'Contractor'," +
	    		"			'objectId' : 16," +
	    		"			'name'     : 'Rowan'" +
	    		"		}," +
	    		"		{" + 		
	    		"   		'class' : 'Contractor'," +
	    		"			'objectId' : 17," +
	    		"			'name' : 'Ray'" +
	    		"		}," +
	    		"		{" + 		
	    		"   		'class' : 'Contractor'," +
	    		"			'objectId' : 18," +
	    		"			'name' : 'Jude'" +
	    		"		}" +
	    		"	]" +
	    		"}";
	    
        ResultsRow row = new ResultsRow();
        row.add(wernhamHogg);
        List sub1 = new ArrayList();
        ResultsRow subRow1 = new ResultsRow();
        subRow1.add(sales);
        sub1.add(subRow1);
        subRow1 = new ResultsRow();
        subRow1.add(accounts);
        sub1.add(subRow1);
        row.add(sub1);
        sub1 = new ArrayList();
        subRow1 = new ResultsRow();
        subRow1.add(rowan);
        sub1.add(subRow1);
        subRow1 = new ResultsRow();
        subRow1.add(ray);
        sub1.add(subRow1);
        subRow1 = new ResultsRow();
        subRow1.add(jude);
        sub1.add(subRow1);
        row.add(sub1);
        os.addRow(row);
        
        PathQuery pq = new PathQuery(model);
        pq.addViews("Company.name", "Company.vatNumber", 
        				"Company.departments.name", 
        				"Company.contractors.name");
        pq.setOuterJoinStatus("Company.departments", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.contractors", OuterJoinStatus.OUTER);
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 1, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);

        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
        
       
	    List<JSONObject> got = new ArrayList<JSONObject>();
	    for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
	        got.add(gotRow);
	    }
	    
	    JSONObject expected = new JSONObject(jsonString);
	    
	    assertEquals(got.size(), 1);
        
        assertEquals(null, getProblemsComparing(expected, got.get(0)));

    }	
	
	public void testHeadInWrongPlace() throws Exception {
		os.setResultsSize(2);
	
        ResultsRow row1 = new ResultsRow();
        row1.add(tim);
        row1.add(sales);
        os.addRow(row1);
        ResultsRow row2 = new ResultsRow();
        row2.add(gareth);
        row2.add(sales);
        os.addRow(row2);
        
        PathQuery pq = new PathQuery(model);
        pq.addViews("Department.employees.name", "Department.name"); 
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 2, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
                
        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
        
        try {
	        List<JSONObject> got = new ArrayList<JSONObject>();
	        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
	            got.add(gotRow);
	        }
	        fail("No exception was thrown dealing with bad query - got this list: " + got);
        } catch (AssertionFailedError e) {
        	// rethrow the fail from within the try
        	throw e; 
        } catch (JSONFormattingException e) {
        	// Test that this is what we thought would happen.
	    	assertEquals("Head of the object is missing", e.getMessage());
	    } catch (Throwable e){
	    	// All other exceptions are failures
	    	fail("Got unexpected error: " + e); 
	    }
	    
	}
	public void testRefBeforeItsParentCollectionOrder() throws Exception {
		os.setResultsSize(2);
	
        ResultsRow row1 = new ResultsRow();
        row1.add(wernhamHogg);
        row1.add(tim);
        row1.add(sales);
        os.addRow(row1);
        ResultsRow row2 = new ResultsRow();
        row2.add(wernhamHogg);
        row2.add(gareth);
        row2.add(sales);
        os.addRow(row2);
        
        PathQuery pq = new PathQuery(model);
        pq.addViews("Company.name", "Company.departments.employees.name", "Company.departments.name"); 
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 2, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
                
        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
        
        try {
	        List<JSONObject> got = new ArrayList<JSONObject>();
	        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
	            got.add(gotRow);
	        }
	        fail("No exception was thrown dealing with bad query - got this list: " + got);
        } catch (AssertionFailedError e) {
        	// rethrow the fail from within the try
        	throw e; 
        } catch (JSONFormattingException e) {
        	// Test that this is what we thought would happen.
	    	assertEquals("This array is empty - is the view in the wrong order?", e.getMessage());
	    } catch (Throwable e){
	    	// All other exceptions are failures
	    	fail("Got unexpected error: " + e); 
	    }
	    
	}
	public void testColBeforeItsParentRefOrder() throws Exception {
		os.setResultsSize(2);
		
        ResultsRow row1 = new ResultsRow();
        row1.add(david);
        row1.add(tim);
        row1.add(sales);
        os.addRow(row1);
        ResultsRow row2 = new ResultsRow();
        row2.add(david);
        row2.add(gareth);
        row2.add(sales);
        os.addRow(row2);
        
        PathQuery pq = new PathQuery(model);
        pq.addViews("Manager.name", "Manager.department.employees.name", "Manager.department.name"); 
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 2, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
                
        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
        
        try {
	        List<JSONObject> got = new ArrayList<JSONObject>();
	        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
	            got.add(gotRow);
	        }
	        fail("No exception was thrown dealing with bad query - got this list: " + got);
        } catch (AssertionFailedError e) {
        	// rethrow the fail from within the try
        	throw e; 
        } catch (JSONFormattingException e) {
        	// Test that this is what we thought would happen.
	    	assertEquals("This node is not properly initialised (it doesn't have an objectId) - is the view in the right order?", e.getMessage());
	    } catch (Throwable e){
	    	// All other exceptions are failures
	    	fail("Got unexpected error: " + e); 
	    }
	    
	}
	
	public void testMultipleObjectsWithColls() throws Exception {
		os.setResultsSize(6);
		
		List<String> jsonStrings = new ArrayList<String>();
		
		jsonStrings.add(
					"	{" +
				    "			'objectId'  : 11," +
				    "			'class'     : 'Department'," +
				    "			'name'      : 'Sales'," +
				    "			'employees' : [" +
				    "				{ " +
				    "					'objectId' : 5," +
				    "					'class'    : 'Employee'," +
				    "					'name'     : 'Tim Canterbury'," +
				    "					'age'	   : 30" +
				    "				}, " +
				    "				{ " +
				    "					'objectId' : 6," +
				    "					'class'	   : 'Employee'," +
				    "					'name' 	   : 'Gareth Keenan'," +
				    "					'age'      : 32" +
				    "				}" +
				    "			]" +
				    "	}");
		jsonStrings.add(
				"	{" +
			    "			'objectId'  : 12," +
			    "			'class'     : 'Department'," +
			    "			'name'      : 'Accounts'," +
			    "			'employees' : [" +
			    "				{ " +
			    "					'objectId' : 8," +
			    "					'class'    : 'Employee'," +
			    "					'name'     : 'Keith Bishop'," +
			    "					'age'	   : 41" +
			    "				} " +
			    "			]" +
			    "	}");
		
		jsonStrings.add(
				 	"		{" +
				    "			'objectId'  : 13," +
				    "			'class'     : 'Department'," +
				    "			'name'      : 'Warehouse', " +	        	
				    "			'employees' : [" +
			    	"				{" +
			    	"					'objectId' : 9," +
			    	"					'class'    : 'Employee'," +
			    	"					'name'     : 'Lee'," +
			    	"					'age'      : 28" +
			    	"				}," +
			    	"				{" +
			    	"					'objectId' : 10," +
			    	"					'class'    : 'Employee'," +
			    	"					'name'     : 'Alex'," +
			    	"					'age'      : 24" +
			    	"				}" +
			    	"			]" +
			    	"		}");
		jsonStrings.add(
				"	{" +
			    "			'objectId'  : 14," +
			    "			'class'     : 'Department'," +
			    "			'name'      : 'Reception'," +
			    "			'employees' : [" +
			    "				{ " +
			    "					'objectId' : 7," +
			    "					'class'    : 'Employee'," +
			    "					'name'     : 'Dawn Tinsley'," +
			    "					'age'	   : 26" +
			    "				} " +
			    "			]" +
			    "	}");
		
		ResultsRow row1 = new ResultsRow();
		row1.add(sales);
		row1.add(tim);
		ResultsRow row2 = new ResultsRow();
		row2.add(sales);
		row2.add(gareth);
		ResultsRow row3 = new ResultsRow();
		row3.add(accounts);
		row3.add(keith);
		ResultsRow row4 = new ResultsRow();
		row4.add(distribution);
		row4.add(lee);
		ResultsRow row5 = new ResultsRow();
		row5.add(distribution);
		row5.add(alex);
		ResultsRow row6 = new ResultsRow();
		row6.add(reception);
		row6.add(dawn);
		
		os.addRow(row1);
		os.addRow(row2);
		os.addRow(row3);
		os.addRow(row4);
		os.addRow(row5);
		os.addRow(row6);
		
		PathQuery pq = new PathQuery(model);
        pq.addViews("Department.name", "Department.employees.name", "Department.employees.age"); 
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 6, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
                
        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
        
        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }
        
        assertEquals(4, got.size());
        for (int i = 0; i < jsonStrings.size(); i++) {
        	JSONObject jo = new JSONObject(jsonStrings.get(i));
        	assertEquals(null, getProblemsComparing(jo, got.get(i)));	
        }
	}
	 
	public void testMultipleObjectsWithRefs() throws Exception {
		os.setResultsSize(6);
		
		List<String> jsonStrings = new ArrayList<String>();
		
		jsonStrings.add(
					"	{" +
				    "			objectId  : 11," +
				    "			class     : 'Department'," +
				    "			name      : 'Sales'," +
				    "			manager   : { class: 'Manager', objectId: 3, age: 39, name: 'David Brent' }," +
				    "			company	  : { class: 'Company', objectId: 1, vatNumber: 101, name: 'Wernham-Hogg' }" +
				    "	}");
		jsonStrings.add(
				"	{" +
			    "			objectId  : 12," +
			    "			class     : 'Department'," +
			    "			name      : 'Accounts'," +
			    "			manager   : { class: 'Manager', objectId: 3, age: 39, name: 'David Brent' }," +
			    "			company	  : { class: 'Company', objectId: 1, vatNumber: 101, name: 'Wernham-Hogg' }" +
			    "	}");
		
		jsonStrings.add(
				 	"		{" +
				    "			objectId  : 13," +
				    "			class     : 'Department'," +
				    "			name      : 'Warehouse', " +	        	
				    "			manager   : { class: 'Manager', objectId: 4, age: 38, name: 'Glynn' }," +
				    "			company	  : { class: 'Company', objectId: 1, vatNumber: 101, name: 'Wernham-Hogg' }" +
			    	"		}");
		jsonStrings.add(
			 	"		{" +
			    "			objectId  : 19," +
			    "			class     : 'Department'," +
			    "			name      : 'Swindon', " +	        	
			    "			manager   : { class: 'Manager', objectId: 20, age: 35, name: 'Neil Godwin' }," +
			    "			company   : { class: 'Company', objectId: 1, vatNumber: 101, name: 'Wernham-Hogg' }" +
		    	"		}");
		
		ResultsRow row1 = new ResultsRow();
		row1.add(sales);
		row1.add(david);
		row1.add(wernhamHogg);
		ResultsRow row2 = new ResultsRow();
		row2.add(accounts);
		row2.add(david);
		row2.add(wernhamHogg);
		ResultsRow row3 = new ResultsRow();
		row3.add(distribution);
		row3.add(taffy);
		row3.add(wernhamHogg);
		ResultsRow row4 = new ResultsRow();
		row4.add(swindon);
		row4.add(neil);
		row4.add(wernhamHogg);
		
		os.addRow(row1);
		os.addRow(row2);
		os.addRow(row3);
		os.addRow(row4);
		
		PathQuery pq = new PathQuery(model);
        pq.addViews("Department.name", "Department.manager.name", "Department.manager.age", "Department.company.name", "Department.company.vatNumber"); 
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 4, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
                
        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
        
        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }
        
        assertEquals(4, got.size());
        for (int i = 0; i < jsonStrings.size(); i++) {
        	JSONObject jo = new JSONObject(jsonStrings.get(i));
        	assertEquals(null, getProblemsComparing(jo, got.get(i)));	
        }
	}                 
	
	public void testMultipleObjectsWithRefsAndCols() throws Exception {
	os.setResultsSize(7);
		
		List<String> jsonStrings = new ArrayList<String>();
		
		jsonStrings.add(
					"	{" +
				    "			objectId  : 11," +
				    "			class     : 'Department'," +
				    "			name      : 'Sales'," +
				    "			manager   : { class: 'Manager', objectId: 3, age: 39, name: 'David Brent' }," +
				    "			company	  : { class: 'Company', objectId: 1, vatNumber: 101, name: 'Wernham-Hogg' }," +
				    "			'employees' : [" +
				    "				{ " +
				    "					objectId : 5," +
				    "					class    : 'Employee'," +
				    "					name     : 'Tim Canterbury'," +
				    "					age	     : 30" +
				    "				}, " +
				    "				{ " +
				    "					objectId : 6," +
				    "					class	 : 'Employee'," +
				    "					name 	 : 'Gareth Keenan'," +
				    "					age      : 32" +
				    "				}" +
				    "			]" +
				    "	}");
		jsonStrings.add(
				"	{" +
			    "			objectId  : 12," +
			    "			class     : 'Department'," +
			    "			name      : 'Accounts'," +
			    "			manager   : { class: 'Manager', objectId: 3, age: 39, name: 'David Brent' }," +
			    "			company	  : { class: 'Company', objectId: 1, vatNumber: 101, name: 'Wernham-Hogg' }," +
			    "			employees : [" +
			    "				{ " +
			    "					objectId : 8," +
			    "					class    : 'Employee'," +
			    "					name     : 'Keith Bishop'," +
			    "					age	     : 41" +
			    "				} " +
			    "			]" +
			    "	}");
		
		jsonStrings.add(
				 	"		{" +
				    "			objectId  : 13," +
				    "			class     : 'Department'," +
				    "			name      : 'Warehouse', " +	        	
				    "			manager   : { class: 'Manager', objectId: 4, age: 38, name: 'Glynn' }," +
				    "			company	  : { class: 'Company', objectId: 1, vatNumber: 101, name: 'Wernham-Hogg' }," +
				    "			employees : [" +
			    	"				{" +
			    	"					objectId : 9," +
			    	"					class    : 'Employee'," +
			    	"					name     : 'Lee'," +
			    	"					age      : 28" +
			    	"				}," +
			    	"				{" +
			    	"					objectId : 10," +
			    	"					class    : 'Employee'," +
			    	"					name     : 'Alex'," +
			    	"					age      : 24" +
			    	"				}" +
			    	"			]" +
			    	"		}");
		jsonStrings.add(
			 	"		{" +
			    "			objectId  : 19," +
			    "			class     : 'Department'," +
			    "			name      : 'Swindon', " +	        	
			    "			manager   : { class: 'Manager', objectId: 20, age: 35, name: 'Neil Godwin' }," +
			    "			company   : { class: 'Company', objectId: 1, vatNumber: 101, name: 'Wernham-Hogg' }," +
			    "			employees : [" +
		    	"				{" +
		    	"					objectId : 21," +
		    	"					class    : 'Employee'," +
		    	"					name     : 'Rachel'," +
		    	"					age      : 34" +
		    	"				}," +
		    	"				{" +
		    	"					objectId : 22," +
		    	"					class    : 'Employee'," +
		    	"					name     : 'Trudy'," +
		    	"					age      : 25" +
		    	"				}" +
		    	"			]" +
		    	"		}");
		
		ResultsRow row1a = new ResultsRow();
		row1a.add(sales);
		row1a.add(david);
		row1a.add(wernhamHogg);
		row1a.add(tim);
		ResultsRow row1b = new ResultsRow();
		row1b.add(sales);
		row1b.add(david);
		row1b.add(wernhamHogg);
		row1b.add(gareth);
		ResultsRow row2 = new ResultsRow();
		row2.add(accounts);
		row2.add(david);
		row2.add(wernhamHogg);
		row2.add(keith);
		ResultsRow row3a = new ResultsRow();
		row3a.add(distribution);
		row3a.add(taffy);
		row3a.add(wernhamHogg);
		row3a.add(lee);
		ResultsRow row3b = new ResultsRow();
		row3b.add(distribution);
		row3b.add(taffy);
		row3b.add(wernhamHogg);
		row3b.add(alex);
		ResultsRow row4a = new ResultsRow();
		row4a.add(swindon);
		row4a.add(neil);
		row4a.add(wernhamHogg);
		row4a.add(rachel);
		ResultsRow row4b = new ResultsRow();
		row4b.add(swindon);
		row4b.add(neil);
		row4b.add(wernhamHogg);
		row4b.add(trudy);
		
		
		os.addRow(row1a);
		os.addRow(row1b);
		os.addRow(row2);
		os.addRow(row3a);
		os.addRow(row3b);
		os.addRow(row4a);
		os.addRow(row4b);
		
		PathQuery pq = new PathQuery(model);
        pq.addViews("Department.name", "Department.manager.name", "Department.manager.age", 
        				"Department.company.name", "Department.company.vatNumber",
        				"Department.employees.name", "Department.employees.age"); 
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 7, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
                
        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
        
        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }
        
        assertEquals(4, got.size());
        for (int i = 0; i < jsonStrings.size(); i++) {
        	JSONObject jo = new JSONObject(jsonStrings.get(i));
        	assertEquals(null, getProblemsComparing(jo, got.get(i)));	
        }
	}
	
	public void testUnsupportedOperations() throws Exception {
		os.setResultsSize(1);
		
		ResultsRow row = new ResultsRow();
		row.add(wernhamHogg);
		
		os.addRow(row);
		PathQuery pq = new PathQuery(model);
        pq.addViews("Company.name"); 
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 1, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
                
        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
        

        try {
        	jsonIter.remove();
        	fail("No exception was thrown when calling the unsupported method remove");
        } catch (AssertionFailedError e) {
        	// rethrow the fail from within the try
        	throw e; 
        } catch (UnsupportedOperationException e) {
        	// Test that this is what we thought would happen.
	    	assertEquals("Remove is not implemented in this class", e.getMessage());
	    } catch (Throwable e){
	    	// All other exceptions are failures
	    	fail("Got unexpected error: " + e); 
	    }
	}
	
	public void testCurrentArrayIsEmpty() throws Exception {
		os.setResultsSize(1);
		
		ResultsRow row = new ResultsRow();
		row.add(wernhamHogg);
		row.add(david);
		
		os.addRow(row);
		PathQuery pq = new PathQuery(model);
        pq.addViews("Company.name", "Company.contractors.oldComs.departments.manager.name"); 
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 1, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
                
        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
        

        try {

            List<JSONObject> got = new ArrayList<JSONObject>();
            for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
                got.add(gotRow);
            }
        	fail("No exception was thrown when calling the unsupported method remove");
        } catch (AssertionFailedError e) {
        	// rethrow the fail from within the try
        	throw e; 
        } catch (JSONFormattingException e) {
        	// Test that this is what we thought would happen.
	    	assertEquals("This array is empty - is the view in the wrong order?", e.getMessage());
	    } catch (Throwable e){
	    	// All other exceptions are failures
	    	fail("Got unexpected error: " + e); 
	    }
	}
	
	public void testCurrentMapIsNull() throws Exception {
		os.setResultsSize(1);
		
		ResultsRow row = new ResultsRow();
		row.add(sales);
		row.add(address);
		
		os.addRow(row);
		PathQuery pq = new PathQuery(model);
        pq.addViews("Department.name", "Department.company.contractors.personalAddress.address"); 
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 1, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
                
        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
        

        try {

            List<JSONObject> got = new ArrayList<JSONObject>();
            for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
                got.add(gotRow);
            }
        	fail("No exception was thrown when calling the unsupported method remove");
        } catch (AssertionFailedError e) {
        	// rethrow the fail from within the try
        	throw e; 
        } catch (JSONFormattingException e) {
        	// Test that this is what we thought would happen.
	    	assertEquals("This node is not properly initialised (it doesn't have an objectId) - is the view in the right order?", e.getMessage());
	    } catch (Throwable e){
	    	// All other exceptions are failures
	    	fail("Got unexpected error: " + e); 
	    }
	}
	
	public void testDateConversion() throws Exception {
		os.setResultsSize(1);
		
		Types typeContainer = new Types();
		typeContainer.setId(new Integer(100));
		Calendar cal = Calendar.getInstance();
		cal.set(108 + 1900, 6, 6);
		typeContainer.setDateObjType(cal.getTime());
		
		ResultsRow row = new ResultsRow();
		row.add(typeContainer);
		
		os.addRow(row);
		PathQuery pq = new PathQuery(model);
        pq.addViews("Types.dateObjType");
        
        String jsonString = "{ class: 'Types', objectId: 100, dateObjType: '2008-07-06' }";
        
        Map pathToQueryNode = new HashMap();
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 1, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
                
        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
        
        List<JSONObject> got = new ArrayList<JSONObject>();
        for (JSONObject gotRow : new IteratorIterable<JSONObject>(jsonIter)) {
            got.add(gotRow);
        }
        
        assertEquals(1, got.size());
        JSONObject expected = new JSONObject(jsonString);
        assertEquals(null, getProblemsComparing(expected, got.get(0)));
        
	}
	
	public void testThosePlacesOtherTestsCannotReach() throws Exception {
		// In normal circumstances these exceptions will never be thrown.
		// These tests are just to check that they will be
		// should abnormal circumstances ever occur.
		Path p = null;
		Path depP = null;
		Path empsP = null;
		Path badP = null;
		try {
			p = new Path(model, "Manager.name");
			depP = new Path(model, "Manager.department");
			empsP = new Path(model, "Manager.department.employees");
			badP = new BadPath(model, "Manager.name");
		} catch (PathException e) {
			e.printStackTrace();
		}
		ResultElement re = new ResultElement(david, p, false);
		Map<String, Object> jsonMap = new TreeMap<String, Object>();
		
		
		ResultsRow row = new ResultsRow();
		row.add(david);
		os.addRow(row);
		
		PathQuery pq = new PathQuery(model);
        pq.addViews("Manager.name");
		Map pathToQueryNode = new HashMap();
		
        Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 1, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        
        ExportResultsIterator iter = new ExportResultsIterator(pq, results, pathToQueryNode);
                
        JSONResultsIterator jsonIter = new JSONResultsIterator(iter);
		
        
	    jsonMap.put("objectId", 1000);
	    try {
			jsonIter.setOrCheckClassAndId(re, jsonMap);
			fail("No exception was thrown, although I tried very hard indeed to cause one");
		} catch (AssertionFailedError e) {
        	// rethrow the fail from within the try
        	throw e; 
        } catch (JSONFormattingException e) {
        	// Test that this is what we thought would happen.
	    	assertEquals(
	    			"This result element ( David Brent 3 Manager) " +
	    			"does not belong on this map ({class=Manager, objectId=1000}) - " +
	    			"objectIds don't match (1000 != 3)", 
	    			e.getMessage());
	    } catch (Throwable e){
	    	// All other exceptions are failures
	    	fail("Got unexpected error: " + e); 
	    }
	    
	    jsonMap.put("class", "Fool");
		try {
			jsonIter.setOrCheckClassAndId(re, jsonMap);
			fail("No exception was thrown, although I tried very hard indeed to cause one");
		} catch (AssertionFailedError e) {
        	// rethrow the fail from within the try
        	throw e; 
        } catch (JSONFormattingException e) {
        	// Test that this is what we thought would happen.
	    	assertEquals(
	    			"This result element ( David Brent 3 Manager) " +
	    			"does not belong on this map ({class=Fool, objectId=1000}) - " +
	    			"classes don't match (Fool != Manager)", 
	    			e.getMessage());
	    } catch (Throwable e){
	    	// All other exceptions are failures
	    	fail("Got unexpected error: " + e); 
	    }
	    jsonMap.clear();
	    jsonMap.put("name", "Neil");
	    try {
			jsonIter.addFieldToMap(re, p, jsonMap);
			fail("No exception was thrown, although I tried very hard indeed to cause one");
		} catch (AssertionFailedError e) {
        	// rethrow the fail from within the try
        	throw e; 
        } catch (JSONFormattingException e) {
        	// Test that this is what we thought would happen.
	    	assertEquals(
	    			"Trying to set key name as David Brent in {class=Manager, name=Neil, objectId=3} " +
	    			"but it already has the value Neil", 
	    			e.getMessage());
	    } catch (Throwable e){
	    	// All other exceptions are failures
	    	fail("Got unexpected error: " + e); 
	    }
	    
	    jsonIter.currentArray = null;
	    try {
			jsonIter.setCurrentMapFromCurrentArray(re);
			fail("No exception was thrown, although I tried very hard indeed to cause one");
		} catch (AssertionFailedError e) {
        	// rethrow the fail from within the try
        	throw e; 
        } catch (JSONFormattingException e) {
        	// Test that this is what we thought would happen.
	    	assertEquals(
	    			"Nowhere to put this field", 
	    			e.getMessage());
	    } catch (Throwable e){
	    	// All other exceptions are failures
	    	fail("Got unexpected error: " + e); 
	    }
	    
	    jsonIter.currentArray = null;
	    try {
			jsonIter.setCurrentMapFromCurrentArray();
			fail("No exception was thrown, although I tried very hard indeed to cause one");
		} catch (AssertionFailedError e) {
        	// rethrow the fail from within the try
        	throw e; 
        } catch (JSONFormattingException e) {
        	// Test that this is what we thought would happen.
	    	assertEquals(
	    			"Nowhere to put this reference", 
	    			e.getMessage());
	    } catch (Throwable e){
	    	// All other exceptions are failures
	    	fail("Got unexpected error: " + e); 
	    }
	    
	    jsonMap.put("department", "Clowning About");
	    jsonIter.currentMap = jsonMap;
	    try {
			jsonIter.addReferenceToCurrentNode(depP);
			fail("No exception was thrown, although I tried very hard indeed to cause one");
		} catch (AssertionFailedError e) {
        	// rethrow the fail from within the try
        	throw e; 
        } catch (JSONFormattingException e) {
        	// Test that this is what we thought would happen.
	    	assertEquals(
	    			"Trying to set a reference on department, " +
	    			"but this node " +
	    			"{class=Manager, department=Clowning About, name=Neil, objectId=3} " +
	    			"already has this key set, " +
	    			"and to something other than a map " +
	    			"(java.lang.String: Clowning About)", 
	    			e.getMessage());
	    } catch (Throwable e){
	    	// All other exceptions are failures
	    	fail("Got unexpected error: " + e); 
	    }
	    
	    jsonIter.currentMap = null;
	    try {
			jsonIter.addReferenceToCurrentNode(depP);
			fail("No exception was thrown, although I tried very hard indeed to cause one");
		} catch (AssertionFailedError e) {
        	// rethrow the fail from within the try
        	throw e; 
        } catch (JSONFormattingException e) {
        	// Test that this is what we thought would happen.
	    	assertEquals(
	    			"The current map should have been set " +
	    			"by a preceding attribute - " +
	    			"is the view in the right order?", 
	    			e.getMessage());
	    } catch (Throwable e){
	    	// All other exceptions are failures
	    	fail("Got unexpected error: " + e); 
	    }
	    
	    jsonMap.put("employees", "Poor Fools");
	    jsonIter.currentMap = jsonMap;
	    try {
			jsonIter.addCollectionToCurrentNode(empsP);
			fail("No exception was thrown, although I tried very hard indeed to cause one");
		} catch (AssertionFailedError e) {
        	// rethrow the fail from within the try
        	throw e; 
        } catch (JSONFormattingException e) {
        	// Test that this is what we thought would happen.
	    	assertEquals(
	    			"Trying to set a collection on employees, " +
	    			"but this node " +
	    			"{class=Manager, department=Clowning About, employees=Poor Fools, name=Neil, objectId=3} " +
	    			"already has this key set to something other than a list " +
	    			"(java.lang.String: Poor Fools)", 
	    			e.getMessage());
	    } catch (Throwable e){
	    	// All other exceptions are failures
	    	fail("Got unexpected error: " + e); 
	    }
	    
	    try {
			jsonIter.addReferencedCellToJsonMap(re, badP, jsonMap);
			fail("No exception was thrown, although I tried very hard indeed to cause one");
		} catch (AssertionFailedError e) {
        	// rethrow the fail from within the try
        	throw e; 
        } catch (JSONFormattingException e) {
        	// Test that this is what we thought would happen.
	    	assertEquals(
	    			"Bad path type: Manager.name", 
	    			e.getMessage());
	    } catch (Throwable e){
	    	// All other exceptions are failures
	    	fail("Got unexpected error: " + e); 
	    }
	    

	}
	
    private class BadPath extends Path {
    	
    	public BadPath(Model m, String s) throws PathException {
    		super(m, s);	
    	}
    	
    	@Override 
    	public List<Path> decomposePath() {
    		return Arrays.asList((Path) this);
    	}
    	@Override
    	public boolean endIsAttribute() {
    		return false;
    	}
    	@Override
    	public boolean endIsReference() {
    		return false;
    	}
    	@Override
    	public boolean endIsCollection() {
    		return false;
    	}
    	@Override
    	public boolean isRootPath() {
    		return false;
    	}
    	
    }
	
	
}
