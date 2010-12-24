package org.intermine.webservice.server.output;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.api.query.MainHelper;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.dummy.DummyResults;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.PathQuery;

import junit.framework.TestCase;

public class JSONObjFormatterTest extends TestCase {

	public JSONObjFormatterTest(String name) {
		super(name);
	}
	private ObjectStoreDummyImpl os;
	private Employee tim;
	private Employee gareth;
	private Employee dawn;
	private Employee keith;
	private Employee lee;
	public static SimpleDateFormat ISO8601FORMAT 
    	= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	private ExportResultsIterator iterator;
	
	private final Model model = Model.getInstanceByName("testmodel");	
	
	StringWriter sw;
	PrintWriter pw;
	
	Map<String, String> attributes;
	
	JSONObjResultProcessor processor;

    protected void setUp() throws Exception {
    	os = new ObjectStoreDummyImpl();
    
    	sw = new StringWriter();
    	pw = new PrintWriter(sw);
    	
    	attributes = new HashMap<String, String>();
		attributes.put("rootClass", "Gene");
		attributes.put("views", "['foo', 'bar', 'baz']");
		attributes.put("modelName", model.getName());
		Calendar cal = Calendar.getInstance();
		cal.set(2008, 6, 6, 15, 0, 0);
		Date executionDate = cal.getTime();
		attributes.put("executionTime", ISO8601FORMAT.format(executionDate));
    	
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
		q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
		List resultList = os.execute(q, 0, 5, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        iterator = new ExportResultsIterator(pq, results, pathToQueryNode); 
        processor = new JSONObjResultProcessor();
    }

    protected void tearDown() throws Exception {
		super.tearDown();
	}
    
    public void testJSONObjectFormatter() {
		JSONObjectFormatter fmtr = new JSONObjectFormatter();
		assertTrue(fmtr != null);
	}

	public void testFormatHeader() {
		JSONObjectFormatter fmtr = new JSONObjectFormatter();
		
		String expected = "{'views':['foo', 'bar', 'baz'],'model':'testmodel','executed_at':'2008-07-06T15:00:00+0100','Gene':[";
		assertEquals(expected, fmtr.formatHeader(attributes));
	}

	public void testFormatResult() {
		JSONObjectFormatter fmtr = new JSONObjectFormatter();
		String expected = "One,Two,Three";
		assertEquals(expected, fmtr.formatResult(Arrays.asList("One", "Two", "Three")));
		expected = "";
		assertEquals(expected, fmtr.formatResult(new ArrayList<String>()));
	}
	
	public void testFormatFooter() {
		JSONObjectFormatter fmtr = new JSONObjectFormatter();
		String expected = "]}";
		assertEquals(expected, fmtr.formatFooter());
	}
	
	public void testFormatAll() {
		JSONObjectFormatter fmtr = new JSONObjectFormatter();
		StreamedOutput out = new StreamedOutput(pw, fmtr);
		out.setHeaderAttributes(attributes);
		
		processor.write(iterator, out);
		
		String expected = 
		"{" +
			"'views':['foo', 'bar', 'baz']," +
			"'model':'testmodel'," +
			"'executed_at':'2008-07-06T15:00:00+0100'," +
			"'Gene':[\n" +
				"{\"objectId\":5,\"name\":\"Tim Canterbury\",\"age\":30,\"class\":\"Employee\"},\n" + 
				"{\"objectId\":6,\"name\":\"Gareth Keenan\",\"age\":32,\"class\":\"Employee\"},\n" +
				"{\"objectId\":7,\"name\":\"Dawn Tinsley\",\"age\":26,\"class\":\"Employee\"},\n" +
				"{\"objectId\":8,\"name\":\"Keith Bishop\",\"age\":41,\"class\":\"Employee\"},\n" +
				"{\"objectId\":9,\"name\":\"Lee\",\"age\":28,\"class\":\"Employee\"}\n" +
		"]}";
		
		assertTrue(pw == out.getWriter());
		assertEquals(5, out.getResultsCount());
		assertEquals(expected, sw.toString());
		
	}
	
}
