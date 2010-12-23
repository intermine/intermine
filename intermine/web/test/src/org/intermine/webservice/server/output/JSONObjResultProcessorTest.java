package org.intermine.webservice.server.output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.api.query.MainHelper;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Contractor;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.dummy.DummyResults;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public class JSONObjResultProcessorTest extends TestCase {

	public JSONObjResultProcessorTest(String name) {
		super(name);
	}
	private ObjectStoreDummyImpl os;
	private Employee tim;
	private Employee gareth;
	private Employee dawn;
	private Employee keith;
	private Employee lee;
	
	private ExportResultsIterator iterator;
	
	private final Model model = Model.getInstanceByName("testmodel");	

    protected void setUp() {
    	os = new ObjectStoreDummyImpl();
        
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
        

        os.setResultsSize(5);	
    	
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
        Query q;
		try {
			q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
			List resultList = os.execute(q, 0, 5, true, true, new HashMap());
	        Results results = new DummyResults(q, resultList);
	        iterator = new ExportResultsIterator(pq, results, pathToQueryNode); 
	    } catch (ObjectStoreException e) {
			e.printStackTrace();
		}
    }
    
    public void testJSONObjResultProcessor() {
		JSONObjResultProcessor processor = new JSONObjResultProcessor();
		assertTrue(processor != null);
	}
    
	public void testWrite() {
		List<String> jsonStrings = Arrays.asList(
        	"{\"objectId\":5,\"name\":\"Tim Canterbury\",\"age\":30,\"class\":\"Employee\"}",
	    	"{\"objectId\":6,\"name\":\"Gareth Keenan\",\"age\":32,\"class\":\"Employee\"}",
	    	"{\"objectId\":7,\"name\":\"Dawn Tinsley\",\"age\":26,\"class\":\"Employee\"}",
	    	"{\"objectId\":8,\"name\":\"Keith Bishop\",\"age\":41,\"class\":\"Employee\"}",
	    	"{\"objectId\":9,\"name\":\"Lee\",\"age\":28,\"class\":\"Employee\"}"
	    );
	    
		List<List<String>> received = new ArrayList<List<String>>();
		List<List<String>> expected = new ArrayList<List<String>>();
		for (String s : jsonStrings) {
			expected.add(Arrays.asList(s));
		}
		
		Output out  = new DummyOutput(received);
		JSONObjResultProcessor processor = new JSONObjResultProcessor();
		processor.write(iterator, out);
		
		assertEquals(expected, received);
		
	}
	
	public void testBadArgument() {
		List<List<String>> received = new ArrayList<List<String>>();
		Output out  = new DummyOutput(received);
		JSONObjResultProcessor processor = new JSONObjResultProcessor();
		try {
			processor.write(null, out);
			fail("Expected an IllegalArgumentException");
		} catch (AssertionFailedError e) {
			throw e;
		} catch (IllegalArgumentException e) {
			assertEquals("The iterator must be an ExportResultsIterator", e.getMessage());
		} catch (Throwable t) {
			fail("Unexpected exception: " + t.getMessage());
		}
		
	}

	

	private class DummyOutput extends Output {

		List<List<String>> outputList;
		
		public DummyOutput(List<List<String>> list) {
			this.outputList = list;
		}
		@Override
		public void addResultItem(List<String> item) {
			outputList.add(item);
			
		}

		@Override
		public void flush() {
			fail("Not yet implemented");
			
		}

		@Override
		protected int getResultsCount() {
			fail("Not yet implemented");
			return 0;
		}
		
	}
}
