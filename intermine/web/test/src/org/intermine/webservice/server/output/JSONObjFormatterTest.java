package org.intermine.webservice.server.output;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

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

public class JSONObjFormatterTest extends TestCase {

    public JSONObjFormatterTest(String name) {
        super(name);
    }

    private Properties testProps;
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

    Map<String, Object> attributes;

    JSONObjResultProcessor processor;

    @Override
    protected void setUp() throws Exception {

        testProps = new Properties();
        testProps.load(getClass().getResourceAsStream(
                "JSONObjFormatterTest.properties"));

        os = new ObjectStoreDummyImpl();

        sw = new StringWriter();
        pw = new PrintWriter(sw);

        attributes = new HashMap<String, Object>();
        attributes.put(JSONResultFormatter.KEY_ROOT_CLASS, "Gene");
        attributes.put(JSONResultFormatter.KEY_VIEWS, Arrays.asList("foo", "bar", "baz"));
        attributes.put(JSONResultFormatter.KEY_MODEL_NAME, model.getName());

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
        q = MainHelper
                .makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 5, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        iterator = new ExportResultsIterator(pq, q, results, pathToQueryNode);
        processor = new JSONObjResultProcessor();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testJSONObjectFormatter() {
        JSONObjectFormatter fmtr = new JSONObjectFormatter();
        assertTrue(fmtr != null);
    }

    public void testFormatHeader() {
        JSONObjectFormatter fmtr = new JSONObjectFormatter();

        String expected = testProps.getProperty("result.header");
        assertEquals(expected, fmtr.formatHeader(attributes));
    }

    public void testFormatResult() {
        JSONObjectFormatter fmtr = new JSONObjectFormatter();
        String expected = "One,Two,Three";
        assertEquals(expected,
                fmtr.formatResult(Arrays.asList("One", "Two", "Three")));
        expected = "";
        assertEquals(expected, fmtr.formatResult(new ArrayList<String>()));
    }

    public void testFormatFooter() {
        JSONObjectFormatter fmtr = new JSONObjectFormatter();
        Date now = Calendar.getInstance().getTime();
        DateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm::ss");
        String executionTime = dateFormatter.format(now);
        String expected = "],\"executionTime\":\"" + executionTime
                        + "\",\"wasSuccessful\":true,\"error\":null,\"statusCode\":200}";
        assertEquals(expected, fmtr.formatFooter(null, 200));
        expected = "],\"executionTime\":\"" + executionTime
        + "\",\"wasSuccessful\":false,\"error\":\"this error\",\"statusCode\":501}";
        assertEquals(expected, fmtr.formatFooter("this error", 501));
    }

    public void testFormatAll() {
        JSONObjectFormatter fmtr = new JSONObjectFormatter();
        StreamedOutput out = new StreamedOutput(pw, fmtr);
        out.setHeaderAttributes(attributes);

        // These are the two steps the service must perform to get good JSON.
        processor.write(iterator, out);
        out.flush();
        Date now = Calendar.getInstance().getTime();
        DateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm::ss");
        String executionTime = dateFormatter.format(now);
        String expected = testProps.getProperty("result.all").replace("{0}",
                executionTime);

        assertTrue(pw == out.getWriter());
        assertEquals(5, out.getResultsCount());
        assertEquals(expected, sw.toString());
    }
}
