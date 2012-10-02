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

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.intermine.api.InterMineAPI;
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
            iterator = new ExportResultsIterator(pq, q, results, pathToQueryNode);

            List emptyList = new ArrayList();
            Results emptyResults = new DummyResults(q, emptyList);
            emptyIterator = new ExportResultsIterator(pq, q, emptyResults, pathToQueryNode);

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
        JSONRowResultProcessor processor = new JSONRowResultProcessor(api);
        assertTrue(processor != null);
    }

    public void testZeroResults() {
        List<String> inner = new ArrayList<String>();
        List<List<String>> expected = Arrays.asList(inner);

        MemoryOutput out  = new MemoryOutput();
        JSONRowResultProcessor processor = new JSONRowResultProcessor(api);
        processor.write(emptyIterator, out);

        assertEquals(expected.toString(), out.getResults().toString());
    }

    @SuppressWarnings("unchecked")
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
