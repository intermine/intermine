package org.intermine.web.bag;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Contractor;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.web.ClassKeyHelper;

/*
 * NOTE - this test depends on data being present in os.unittest which is
 * currently inserted before running the testmodel webapp tests.  If this
 * changes then this class will need to extend StoreDataTestCase.
 */
public class BagQueryRunnerTest extends TestCase {
	private ObjectStore os;
	private Map eIds;
	private BagQueryRunner runner;
	
	public BagQueryRunnerTest(String arg0) {
		super(arg0);
	}
    public void setUp() throws Exception {
        super.setUp();
		os = ObjectStoreFactory.getObjectStore("os.unittest");
		Properties props = new Properties();
		props.load(getClass().getClassLoader().getResourceAsStream("WEB-INF/class_keys.properties"));
		Map classKeys = ClassKeyHelper.readKeys(os.getModel(), props);
		eIds = getEmployeeIds();
		
		InputStream is = getClass().getClassLoader().getResourceAsStream("WEB-INF/bag-queries.xml");
		Map bagQueries = BagQueryHelper.readBagQueries(os.getModel(), is);
		runner = new BagQueryRunner(os, classKeys, bagQueries);
    }
    
	// expect each input string to match one object
	public void testSearchForBagMatches() throws Exception {
		List input = Arrays.asList(new Object[] {"EmployeeA1", "EmployeeA2"});
		BagQueryResult res = runner.searchForBag("Employee", input);
		assertEquals(2, res.getMatches().values().size());
		assertTrue(res.getIssues().isEmpty());
		assertTrue(res.getUnresolved().isEmpty());
	}
    
    // test for the case when an identifier appears twice in the input - ignore duplicates
    public void testSearchForBagDuplicates1() throws Exception {
        List input = Arrays.asList(new Object[] {"EmployeeA1", "EmployeeA2", "EmployeeA1"});
        BagQueryResult res = runner.searchForBag("Employee", input);
        assertEquals(2, res.getMatches().values().size());
        assertTrue(res.getIssues().isEmpty());
        assertTrue(res.getUnresolved().isEmpty());
    }
    
	// expect to get two objects back for 'Mr.'
	public void testSearchForBagDuplicates2() throws Exception {
		List input = Arrays.asList(new Object[] {"Mr."});
		BagQueryResult res = runner.searchForBag("Manager", input);		
		assertEquals(0, res.getMatches().size());
		Map expected = new HashMap();
		Map queries = new HashMap();
		List ids = new ArrayList(Arrays.asList(new Object[] {eIds.get("EmployeeB1"), eIds.get("EmployeeB3")}));
		Map results = new HashMap();
		results.put("Mr.", ids);
		queries.put(BagQueryHelper.DEFAULT_MESSAGE, results);
		expected.put(BagQueryResult.DUPLICATE, queries);
		assertEquals(expected, res.getIssues());
		assertTrue(res.getUnresolved().isEmpty());
	}
	
	// expect one match and one unresolved
	public void testSearchForBagUnresolved() throws Exception {
		List input = Arrays.asList(new Object[] {"EmployeeA1", "rubbish"});
		BagQueryResult res = runner.searchForBag("Employee", input);
		assertEquals(1, res.getMatches().values().size());
		assertTrue(res.getIssues().isEmpty());
		assertEquals(res.getUnresolved().size(), 1);
	}
	
	// two identifiers for same object - both match once
	// in this case there are two entries in matches but both have
	// the same id - so getMatches().values().size() == 1
	public void testSearchForBagDoubleInput1() throws Exception {
		List input = Arrays.asList(new Object[] {"EmployeeB1", "Mr."});
		BagQueryResult res = runner.searchForBag("CEO", input);		
		assertEquals(1, res.getMatches().size());
		assertEquals(2, ((List) ((Collection) res.getMatches().values()).iterator().next()).size());
		assertTrue(res.getIssues().isEmpty());
		assertTrue(res.getUnresolved().isEmpty());
	}
	
	// two identifiers for same object - one matches twice
	public void testSearchForBagDoubleInput2() throws Exception {
		List input = Arrays.asList(new Object[] {"EmployeeB1", "Mr."});
		BagQueryResult res = runner.searchForBag("Manager", input);		
		assertEquals(1, res.getMatches().size());
		Map expected = new HashMap();
		Map queries = new HashMap();
		List ids = new ArrayList(Arrays.asList(new Object[] {eIds.get("EmployeeB1"), eIds.get("EmployeeB3")}));
		Map results = new HashMap();
		results.put("Mr.", ids);
		queries.put(BagQueryHelper.DEFAULT_MESSAGE, results);
		expected.put(BagQueryResult.DUPLICATE, queries);
		assertEquals(expected, res.getIssues());
		assertTrue(res.getUnresolved().isEmpty());
	}
	
	// match nothing from first query, match both from second
	public void testSecondQueryIssue() throws Exception {
		List input = Arrays.asList(new Object[] {"1"});
		BagQueryResult res = runner.searchForBag("Employee", input);
		assertEquals(0, res.getMatches().values().size());
		Map expected = new HashMap();
		Map queries = new HashMap();
		List ids = new ArrayList(Arrays.asList(new Object[] {eIds.get("EmployeeA1")}));
		Map results = new HashMap();
		results.put("1", ids);
		queries.put("employee end", results);
		expected.put(BagQueryResult.OTHER, queries);
		assertEquals(expected, res.getIssues());
		assertTrue(res.getUnresolved().isEmpty());
	}
    
    // test searching for an input string that doesn't match any object
    public void testObjectNotFound() throws Exception {
        String nonMatchingString = "non_matching_string";
        List input = Arrays.asList(new Object[] {nonMatchingString});
        BagQueryResult res = runner.searchForBag("Employee", input);
        assertEquals(0, res.getMatches().values().size());
        Map resUnresolved = res.getUnresolved();
        assertTrue(resUnresolved.size() == 1);
        assertTrue(resUnresolved.containsKey(nonMatchingString));
        assertNull(resUnresolved.get(nonMatchingString));
    }
    
    // test searching for an input string that only matches an object of the wrong type and can't
    // be converted
    public void testObjectWrongType() throws Exception {
        String contractorName = "ContractorA";
        List input = Arrays.asList(new Object[] {contractorName});
        BagQueryResult res = runner.searchForBag("Employee", input);
        assertEquals(0, res.getMatches().values().size());
        Map resUnresolved = res.getUnresolved();
        assertTrue(resUnresolved.size() == 1);
        assertTrue(resUnresolved.containsKey(contractorName));
        Set contractors = (Set) resUnresolved.get(contractorName);
        assertEquals(contractorName, ((Contractor) contractors.iterator().next()).getName());
    }
    

	// we need to test a query that matches a different type.  Probably 
	// need to add another query to: testmodel/webapp/main/resources/webapp/WEB-INF/bag-queries.xml
	
	private Map getEmployeeIds() throws ObjectStoreException {
		Map employees = new HashMap();
		Query q = new Query();
		QueryClass qc = new QueryClass(Employee.class);
		q.addFrom(qc);
		q.addToSelect(qc);
		Results res = os.execute(q);
		Iterator resIter = res.iterator();
		while (resIter.hasNext()) {
			Employee e = (Employee) ((List) resIter.next()).get(0);
			employees.put(e.getName(), e);
		}
		return employees;
	}
}
