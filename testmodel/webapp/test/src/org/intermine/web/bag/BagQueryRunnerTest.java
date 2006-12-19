package org.intermine.web.bag;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.web.ClassKeyHelper;

public class BagQueryRunnerTest extends TestCase {
	private ObjectStore os;
	private Map classKeys, eIds;
	
	public BagQueryRunnerTest(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
		super.setUp();
		os = ObjectStoreFactory.getObjectStore("os.unittest");
		Properties props = new Properties();
		props.load(getClass().getClassLoader().getResourceAsStream("WEB-INF/class_keys.properties"));
		classKeys = ClassKeyHelper.readKeys(os.getModel(), props);
		eIds = getEmployeeIds();
	}

	// expect each input string to match one object
	public void testSearchForBagMatches() throws Exception {
		List input = Arrays.asList(new Object[] {"EmployeeA1", "EmployeeA2"});
		BagQueryRunner runner = new BagQueryRunner(os, classKeys);
		BagQueryResult res = runner.searchForBag("Employee", input);
		assertEquals(2, res.getMatches().values().size());
		assertTrue(res.getIssues().isEmpty());
		assertTrue(res.getUnresolved().isEmpty());
	}
	
	// expect to get two objects back for 'Mr.'
	public void testSearchForBagDuplicates() throws Exception {
		List input = Arrays.asList(new Object[] {"Mr."});
		BagQueryRunner runner = new BagQueryRunner(os, classKeys);
		BagQueryResult res = runner.searchForBag("Manager", input);		
		assertEquals(0, res.getMatches().size());
		Map expected = new HashMap();
		Map queries = new HashMap();
		Set ids = new HashSet(Arrays.asList(new Object[] {eIds.get("EmployeeB1"), eIds.get("EmployeeB3")}));
		Map results = new HashMap();
		results.put("Mr.", ids);
		queries.put("default", results);
		expected.put(BagQueryResult.DUPLICATE, queries);
		assertEquals(expected, res.getIssues());
		assertTrue(res.getUnresolved().isEmpty());
	}
	
	// expect one match and one unresolved
	public void testSearchForBagUnresolved() throws Exception {
		List input = Arrays.asList(new Object[] {"EmployeeA1", "rubbish"});
		BagQueryRunner runner = new BagQueryRunner(os, classKeys);
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
		BagQueryRunner runner = new BagQueryRunner(os, classKeys);
		BagQueryResult res = runner.searchForBag("CEO", input);		
		assertEquals(2, res.getMatches().size());
		assertEquals(1, new HashSet(res.getMatches().values()).size());
		assertTrue(res.getIssues().isEmpty());
		assertTrue(res.getUnresolved().isEmpty());
	}
	
	// two identifiers for same object - once matches twice
	public void testSearchForBagDoubleInput2() throws Exception {
		List input = Arrays.asList(new Object[] {"EmployeeB1", "Mr."});
		BagQueryRunner runner = new BagQueryRunner(os, classKeys);
		BagQueryResult res = runner.searchForBag("Manager", input);		
		assertEquals(1, res.getMatches().size());
		Map expected = new HashMap();
		Map queries = new HashMap();
		Set ids = new HashSet(Arrays.asList(new Object[] {eIds.get("EmployeeB1"), eIds.get("EmployeeB3")}));
		Map results = new HashMap();
		results.put("Mr.", ids);
		queries.put("default", results);
		expected.put(BagQueryResult.DUPLICATE, queries);
		assertEquals(expected, res.getIssues());
		assertTrue(res.getUnresolved().isEmpty());
	}
	
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
