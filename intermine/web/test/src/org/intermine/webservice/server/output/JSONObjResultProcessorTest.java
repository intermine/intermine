package org.intermine.webservice.server.output;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.AssertionFailedError;
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
            iterator = new ExportResultsIterator(pq, q, results, pathToQueryNode);
        } catch (ObjectStoreException e) {
            e.printStackTrace();
        }
    }

    public void testJSONObjResultProcessor() {
        JSONObjResultProcessor processor = new JSONObjResultProcessor();
        assertTrue(processor != null);
    }

    @SuppressWarnings("unchecked")
    public void testWrite() {
        List<List<String>> expected = Arrays.asList(
            Arrays.asList("{\"objectId\":5,\"name\":\"Tim Canterbury\",\"age\":30,\"class\":\"Employee\"}", ""),
            Arrays.asList("{\"objectId\":6,\"name\":\"Gareth Keenan\",\"age\":32,\"class\":\"Employee\"}", ""),
            Arrays.asList("{\"objectId\":7,\"name\":\"Dawn Tinsley\",\"age\":26,\"class\":\"Employee\"}", ""),
            Arrays.asList("{\"objectId\":8,\"name\":\"Keith Bishop\",\"age\":41,\"class\":\"Employee\"}", ""),
            Arrays.asList("{\"objectId\":9,\"name\":\"Lee\",\"age\":28,\"class\":\"Employee\"}")
        );

        MemoryOutput out  = new MemoryOutput();
        JSONObjResultProcessor processor = new JSONObjResultProcessor();
        processor.write(iterator, out);

        assertEquals(expected.toString(), out.getResults().toString());

    }

}
