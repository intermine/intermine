package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
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
 * @author Alexis Kalderimis
 *
 */
public class JSONRowFormatterTest extends TestCase {

    /**
     * @param name
     */
    public JSONRowFormatterTest(String name) {
        super(name);
    }

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
    private Properties testProps;

    StringWriter sw;
    PrintWriter pw;

    private final InterMineAPI dummyAPI = new DummyAPI();

    Map<String, Object> attributes;

    JSONRowResultProcessor processor;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected void setUp() throws Exception {

        testProps = new Properties();
        testProps.load(getClass().getResourceAsStream("JSONRowFormatterTest.properties"));

        os = new ObjectStoreDummyImpl();

        sw = new StringWriter();
        pw = new PrintWriter(sw);

        attributes = new HashMap<String, Object>();
        attributes.put(JSONResultFormatter.KEY_ROOT_CLASS, "Gene");
        attributes.put(JSONResultFormatter.KEY_VIEWS, Arrays.asList("foo", "bar", "baz"));
        attributes.put(JSONResultFormatter.KEY_MODEL_NAME, model.getName());
        attributes.put(JSONRowFormatter.KEY_TITLE, "Test Results");
        attributes.put(JSONRowFormatter.KEY_EXPORT_CSV_URL, "some.csv.url");
        attributes.put(JSONRowFormatter.KEY_EXPORT_TSV_URL, "some.tsv.url");
        attributes.put(JSONRowFormatter.KEY_PREVIOUS_PAGE, "url.to.previous");
        attributes.put(JSONRowFormatter.KEY_NEXT_PAGE, "url.to.next");
        attributes.put("SOME_NULL_KEY", null);

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

        iterator = getIterator(pq);
        processor = new JSONRowResultProcessor(dummyAPI);
    }

    private ExportResultsIterator getIterator(PathQuery pq) throws ObjectStoreException {
        Map pathToQueryNode = new HashMap();
        Query q;
        q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode, null, null);
        List resultList = os.execute(q, 0, 5, true, true, new HashMap());
        Results results = new DummyResults(q, resultList);
        return new ExportResultsIterator(pq, q, results, pathToQueryNode);
    }

    /*
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testJSONRowFormatter() {
        JSONRowFormatter fmtr = new JSONRowFormatter();
        assertTrue(fmtr != null);
    }

    public void testFormatHeader() {
        JSONRowFormatter fmtr = new JSONRowFormatter();

        String expected = testProps.getProperty("result.header");
        assertEquals(expected, fmtr.formatHeader(attributes));

        String callback = "user_defined_callback";
        expected = callback + "(" + expected;
        attributes.put(JSONRowFormatter.KEY_CALLBACK, callback);

        assertEquals(expected, fmtr.formatHeader(attributes));
    }

    public void testFormatResult() {
        JSONRowFormatter fmtr = new JSONRowFormatter();
        String expected = testProps.getProperty("result.body");
        assertEquals(expected,
                fmtr.formatResult(Arrays.asList("One", "Two", "Three")));

        expected = "";
        assertEquals(expected, fmtr.formatResult(new ArrayList<String>()));
    }

    public void testFormatFooter() {
        JSONRowFormatter fmtr = new JSONRowFormatter();
        Date now = Calendar.getInstance().getTime();
        DateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm::ss");
        String executionTime = dateFormatter.format(now);
        String expected = "],\"executionTime\":\"" + executionTime
             + "\",\"wasSuccessful\":true,\"error\":null,\"statusCode\":200}";
        assertEquals(expected, fmtr.formatFooter(null, 200));

        expected = "],\"executionTime\":\"" + executionTime
           + "\",\"wasSuccessful\":false,\"error\":\"Not feeling like it\","
           + "\"statusCode\":400}";

        assertEquals(expected, fmtr.formatFooter("Not feeling like it", 400));

        expected += ");";
        attributes.put(JSONRowFormatter.KEY_CALLBACK, "should_not_appear_in_footer");

        fmtr.formatHeader(attributes); // needs to be called to set the callback parameter
        assertEquals(expected, fmtr.formatFooter("Not feeling like it", 400));
    }

    public void testFormatAll() throws IOException {
        JSONRowFormatter fmtr = new JSONRowFormatter();
        StreamedOutput out = new StreamedOutput(pw, fmtr);
        out.setHeaderAttributes(attributes);

        // These are the two steps the service must perform to get good JSON.
        processor.write(iterator, out);
        out.flush();
        Date now = Calendar.getInstance().getTime();
        DateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm::ss");
        String executionTime = dateFormatter.format(now);
        String expected = testProps.getProperty("result.all.good").replace("{0}",
                executionTime);
        assertTrue(pw == out.getWriter());
        assertEquals(5, out.getResultsCount());
        assertEquals(expected, sw.toString());
    }

    public void testFormatAllBad() {
        JSONRowFormatter fmtr = new JSONRowFormatter();
        StreamedOutput out = new StreamedOutput(pw, fmtr);
        out.setHeaderAttributes(attributes);

        // These are the two steps the service must perform to get good JSON.
        processor.write(iterator, out);
        out.setError("Not feeling like it.", 500);
        out.flush();
        Date now = Calendar.getInstance().getTime();
        DateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm::ss");
        String executionTime = dateFormatter.format(now);
        String expected = testProps.getProperty("result.all.bad").replace("{0}",
                executionTime);
        assertTrue(pw == out.getWriter());
        assertEquals(5, out.getResultsCount());
        assertEquals(expected, sw.toString());

    }
}
