/**
 *
 */
package org.intermine.webservice.server.output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

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

/**
 * @author alex
 *
 */
public class JSONRowResultProcessorTest extends TestCase {

    private ObjectStoreDummyImpl os;
    private Employee tim;
    private Employee gareth;
    private Employee dawn;
    private Employee keith;
    private Employee lee;

    private ExportResultsIterator iterator;
    private ExportResultsIterator emptyIterator;

    private final Model model = Model.getInstanceByName("testmodel");

    /**
     * @param name
     */
    public JSONRowResultProcessorTest(String name) {
        super(name);
    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
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
        pq.addViews("Employee.age", "Employee.name");

        Map pathToQueryNode = new HashMap();
        Query q;
        try {
            q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
            List resultList = os.execute(q, 0, 5, true, true, new HashMap());
            Results results = new DummyResults(q, resultList);
            iterator = new ExportResultsIterator(pq, results, pathToQueryNode);
            
            List emptyList = new ArrayList();
            Results emptyResults = new DummyResults(q, emptyList);
            emptyIterator = new ExportResultsIterator(pq, emptyResults, pathToQueryNode);
            
        } catch (ObjectStoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testJSONRowResultProcessor() {
        JSONRowResultProcessor processor = new JSONRowResultProcessor();
        assertTrue(processor != null);
    }
    
    public void testZeroResults() {
        List<List<String>> expected = Collections.EMPTY_LIST;
        
        MemoryOutput out  = new MemoryOutput();
        JSONRowResultProcessor processor = new JSONRowResultProcessor();
        processor.write(emptyIterator, out);

        assertEquals(expected.toString(), out.getResults().toString());
    }

    @SuppressWarnings("unchecked")
    public void testWrite() {
        List<List<String>> expected = Arrays.asList(
        Arrays.asList("[" +
                "{\"value\":30,\"url\":\"http://the.base.url/objectdetails.do?id=5\"}," +
                "{\"value\":\"Tim Canterbury\",\"url\":\"http://the.base.url/objectdetails.do?id=5\"}" +
                "]", ""),
        Arrays.asList("[" +
                "{\"value\":32,\"url\":\"http://the.base.url/objectdetails.do?id=6\"}," +
                "{\"value\":\"Gareth Keenan\",\"url\":\"http://the.base.url/objectdetails.do?id=6\"}" +
                "]", ""),
        Arrays.asList("[" +
                "{\"value\":26,\"url\":\"http://the.base.url/objectdetails.do?id=7\"}," +
                "{\"value\":\"Dawn Tinsley\",\"url\":\"http://the.base.url/objectdetails.do?id=7\"}" +
                "]", ""),
        Arrays.asList("[" +
                "{\"value\":41,\"url\":\"http://the.base.url/objectdetails.do?id=8\"}," +
                "{\"value\":\"Keith Bishop\",\"url\":\"http://the.base.url/objectdetails.do?id=8\"}" +
                "]", ""),
        Arrays.asList("[" +
                "{\"value\":28,\"url\":\"http://the.base.url/objectdetails.do?id=9\"}," +
                "{\"value\":\"Lee\",\"url\":\"http://the.base.url/objectdetails.do?id=9\"}" +
                "]")
        );

        MemoryOutput out  = new MemoryOutput();
        JSONRowResultProcessor processor = new JSONRowResultProcessor();
        processor.write(iterator, out);

        assertEquals(expected.toString(), out.getResults().toString());

    }

}
