package org.intermine.webservice.client.benchmark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.services.QueryService;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class ResultFormats {

    private static final String baseUrl = "http://www.flymine.org/query/service";
    private static final QueryService flymine = new ServiceFactory(baseUrl).getQueryService();

    private PathQuery query;

    static int expectedSize = 17872;

    static int runs = 10;

    @Before
    public void setup() {
        query = new PathQuery(flymine.getFactory().getModel());
        query.addViews("Gene.symbol", "Gene.length");
        query.addConstraint(Constraints.eq("Gene.symbol", "c*"));
    }

    @Test
    public void benchmarkJsonRows() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < runs; i++) {
            Iterator<List<Object>> it = flymine.getRowListIterator(query);
            int c = 0;
            while (it.hasNext()) {
                List<Object> row = it.next();
                assertEquals(2, row.size());
                c++;
            }
            assertEquals(expectedSize, c);
        }
        long end = System.currentTimeMillis();
        System.out.printf("JSON-ROWS: %.2f seconds per run of %d\n",
                Float.valueOf(end - start) / 1000 / runs, expectedSize);
    }

    @Test
    public void benchmarkJsonObj() throws JSONException {
        long start = System.currentTimeMillis();
        for (int i = 0; i < runs; i++) {
            Iterator<JSONObject> it = flymine.getAllJSONResults(query).iterator();
            int c = 0;
            while (it.hasNext()) {
                JSONObject res = it.next();
                assertTrue(res.getString("symbol").toLowerCase().startsWith("c"));
                c++;
            }
            assertEquals(expectedSize, c);
        }
        long end = System.currentTimeMillis();
        System.out.printf("JSON-OBJ: %.2f seconds per run of %d\n",
                Float.valueOf(end - start) / 1000 / runs, expectedSize);
    }

    @Test
    public void benchmarkJsonXML() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < runs; i++) {
            Iterator<List<String>> it = flymine.getAllResults(query).iterator();
            int c = 0;
            while (it.hasNext()) {
                List<String> res = it.next();
                assertEquals(2, res.size());
                c++;
            }
            assertEquals(expectedSize, c);
        }
        long end = System.currentTimeMillis();
        System.out.printf("XML: %.2f seconds per run of %d\n",
                Float.valueOf(end - start) / 1000 / runs, expectedSize);
    }

}
