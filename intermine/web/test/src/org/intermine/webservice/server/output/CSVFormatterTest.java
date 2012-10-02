package org.intermine.webservice.server.output;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.api.InterMineAPI;
import org.intermine.api.query.MainHelper;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.dummy.DummyResults;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.server.core.ResultProcessor;

public class CSVFormatterTest extends TestCase {

    private ObjectStoreDummyImpl os;
    private Employee tim;
    private Employee gareth;
    private Employee dawn;
    private Employee keith;
    private Employee lee;
    public static SimpleDateFormat ISO8601FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssZ");
    private ExportResultsIterator iterator;

    private final Model model = Model.getInstanceByName("testmodel");

    StringWriter sw;
    PrintWriter pw;

    private final InterMineAPI dummyAPI = new DummyAPI();

    Map<String, Object> attributes;

    ResultProcessor processor;

    @Override
    protected void setUp() throws Exception {

        os = new ObjectStoreDummyImpl();

        sw = new StringWriter();
        pw = new PrintWriter(sw);

        attributes = new HashMap<String, Object>();
        List<String> view = new ArrayList<String>(Arrays.asList("foo", "bar", "baz"));
        attributes.put(CSVFormatter.COLUMN_HEADERS, view);

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
        pq.addViews("Employee.name", "Employee.age");

        Map pathToQueryNode = new HashMap();
        Query q;
        q = MainHelper
                .makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 5, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        iterator = new ExportResultsIterator(pq, q, results, pathToQueryNode);
        processor =  new ResultProcessor();
 }

    /*
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testInstantiation() {
        CSVFormatter fmtr = new CSVFormatter();
        assertNotNull(fmtr);
    }

    public void testFormatHeader() {
        CSVFormatter fmtr = new CSVFormatter();

        String expected = "\"foo\",\"bar\",\"baz\"";

        assertEquals(expected, fmtr.formatHeader(attributes));

        expected = "";
        Map<String, Object> emptyMap = new HashMap<String, Object>();
        assertEquals(expected, fmtr.formatHeader(emptyMap));
    }

    public void testFormatResult() {
        CSVFormatter fmtr = new CSVFormatter();
        String expected = "\"One\",\"Two\",\"Three\"";
        assertEquals(expected,
                fmtr.formatResult(Arrays.asList("One", "Two", "Three")));

        expected = "";
        assertEquals(expected, fmtr.formatResult(new ArrayList<String>()));
    }

    public void testFormatFooter() {
        CSVFormatter fmtr = new CSVFormatter();

        String expected = "";
        assertEquals(expected, fmtr.formatFooter(null, 200));
        expected = "[ERROR] 400 This is a test";
        assertEquals(expected, fmtr.formatFooter("This is a test", 400));
    }

    public void testFormatAll() {
        CSVFormatter fmtr = new CSVFormatter();
        StreamedOutput out = new StreamedOutput(pw, fmtr);
        out.setHeaderAttributes(attributes);

        // These are the two steps the service must perform.
        processor.write(iterator, out);
        out.flush();

        String expected =
              "\"foo\",\"bar\",\"baz\"\n"
            + "\"Tim Canterbury\",\"30\"\n"
            + "\"Gareth Keenan\",\"32\"\n"
            + "\"Dawn Tinsley\",\"26\"\n"
            + "\"Keith Bishop\",\"41\"\n"
            + "\"Lee\",\"28\"\n";

        assertTrue(pw == out.getWriter());
        assertEquals(5, out.getResultsCount());
        assertEquals(expected, sw.toString());
    }

    public void testFormatAllWithProblem() {
        CSVFormatter fmtr = new CSVFormatter();
        StreamedOutput out = new StreamedOutput(pw, fmtr);
        out.setHeaderAttributes(attributes);

        // These are the two steps the service must perform.
        processor.write(iterator, out);
        out.setError("Our bad", 500);
        out.flush();

        String expected =
              "\"foo\",\"bar\",\"baz\"\n"
            + "\"Tim Canterbury\",\"30\"\n"
            + "\"Gareth Keenan\",\"32\"\n"
            + "\"Dawn Tinsley\",\"26\"\n"
            + "\"Keith Bishop\",\"41\"\n"
            + "\"Lee\",\"28\"\n"
            + "[ERROR] 500 Our bad";

        assertTrue(pw == out.getWriter());
        assertEquals(5, out.getResultsCount());
        assertEquals(expected, sw.toString());
    }


}
