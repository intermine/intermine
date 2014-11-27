/**
 *
 */
package org.intermine.webservice.server.output;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.query.MainHelper;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.dummy.DummyResults;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.context.InterMineContext;

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

    private final InterMineAPI api = new DummyAPI();

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

        ResultsRow<Employee> row1 = new ResultsRow<Employee>();
        row1.add(tim);
        ResultsRow<Employee> row2 = new ResultsRow<Employee>();
        row2.add(gareth);
        ResultsRow<Employee> row3 = new ResultsRow<Employee>();
        row3.add(dawn);
        ResultsRow<Employee> row4 = new ResultsRow<Employee>();
        row4.add(keith);
        ResultsRow<Employee> row5 = new ResultsRow<Employee>();
        row5.add(lee);

        os.addRow(row1);
        os.addRow(row2);
        os.addRow(row3);
        os.addRow(row4);
        os.addRow(row5);


        PathQuery pq = new PathQuery(model);
        pq.addViews("Employee.age", "Employee.name");

        Map<String, QuerySelectable> pathToQueryNode = new HashMap<String, QuerySelectable>();
        Query q;
        try {
            q = MainHelper.makeQuery(pq, new HashMap<String, InterMineBag>(), pathToQueryNode, null, null);
            @SuppressWarnings("unchecked")
            List<Object> resultList = os.execute(q, 0, 5, true, true, new HashMap<Object, Integer>());
            Results results = new DummyResults(q, resultList);
            iterator = new ExportResultsIterator(pq, q, results, pathToQueryNode);

            List<Object> emptyList = new ArrayList<Object>();
            Results emptyResults = new DummyResults(q, emptyList);
            emptyIterator = new ExportResultsIterator(pq, q, emptyResults, pathToQueryNode);

        } catch (ObjectStoreException e) {
            e.printStackTrace();
        }
        InterMineContext.initilise(api, new Properties(), null);
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        InterMineContext.doShutdown();
        super.tearDown();
    }

    public void testJSONRowResultProcessor() {
        JSONRowResultProcessor processor = new JSONRowResultProcessor(api);
        assertTrue(processor != null);
    }

    public void testZeroResults() {
        List<String> inner = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        List<List<String>> expected = Arrays.asList(inner);

        MemoryOutput out  = new MemoryOutput();
        JSONRowResultProcessor processor = new JSONRowResultProcessor(api);
        processor.write(emptyIterator, out);

        assertEquals(expected.toString(), out.getResults().toString());
    }

    public void testWrite() throws IOException {
        InputStream is = getClass().getResourceAsStream("JSONRowResultProcessorTest.expected");
        StringWriter sw = new StringWriter();
        IOUtils.copy(is, sw);
        String expected = sw.toString();

        MemoryOutput out  = new MemoryOutput();
        JSONRowResultProcessor processor = new JSONRowResultProcessor(api);
        processor.write(iterator, out);

        /* For debugging, as ant can't give long enough error messages */
//        FileWriter fw = new FileWriter(new File("/tmp/ant_" + this.getClass().getName() + ".out"));
//        fw.write("EXPECTED:\n=====\n");
//        fw.write(expected);
//        fw.write("\nGOT:\n======\n");
//        fw.write(out.getResults().toString());
//        fw.close();

        assertEquals(expected, out.getResults().toString() + "\n");

    }

}
