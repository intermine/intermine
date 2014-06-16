package org.intermine.webservice.client.benchmark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
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

    private int expectedSize = -1;

    static int runs = 20;

    @Before
    public void setup() {
        query = new PathQuery(flymine.getFactory().getModel());
        query.addViews("Gene.symbol", "Gene.length");
        query.addConstraint(Constraints.eq("Gene.symbol", "c*"));
        query.addConstraint(Constraints.isNotNull("Gene.length"));
        expectedSize = flymine.getCount(query);
    }

    private static interface RowCounter {
        public int count(PathQuery query) throws Exception;
        public String getName();
    }

    private void doBenchmark(RowCounter counter) throws Exception {
        long start = System.currentTimeMillis();
        for (int i = 0; i < runs; i++) {
            int c = counter.count(query);
            assertEquals(expectedSize, c);
        }
        long end = System.currentTimeMillis();
        System.out.printf("%s: %.2f seconds per run of %d\n",
                counter.getName(),
                Float.valueOf(end - start) / 1000 / runs, expectedSize);
    }

    @Test
    public void benchmarkJsonRows() throws Exception {
        RowCounter counter = new RowCounter() {
            @Override
            public int count(PathQuery query) {
                int c = 0, cells = query.getView().size();
                Iterator<List<Object>> it = flymine.getRowListIterator(query);
                while (it.hasNext()) {
                    List<Object> row = it.next();
                    assertEquals(cells, row.size());
                    c++;
                }
                return c;
            }

            @Override
            public String getName() {
                return "JSON-ROWS";
            }
        };
        doBenchmark(counter);
    }

    @Test
    public void benchmarkJsonObj() throws Exception {
        RowCounter counter = new RowCounter() {
            @Override
            public int count(PathQuery query) throws JSONException {
                int c = 0;
                List<String> paths = query.getView();
                List<String> fields = new ArrayList<String>();
                for (String path: paths) {
                    fields.add(path.replaceAll("[^.]*\\.", ""));
                }
                Iterator<JSONObject> it = flymine.getAllJSONResults(query).iterator();
                while (it.hasNext()) {
                    JSONObject res = it.next();
                    for (String field: fields) {
                        assertNotNull(field + " is not null", res.get(field));
                    }
                    c++;
                }
                return c;
            }

            @Override
            public String getName() {
                return "JSON-OBJ";
            }
        };
        doBenchmark(counter);
    }

    @Test
    public void benchmarkJsonXML() throws Exception {
        RowCounter counter = new RowCounter() {
            @Override
            public int count(PathQuery query) throws JSONException {
                int c = 0, cells = query.getView().size();
                Iterator<List<String>> it = flymine.getAllResults(query).iterator();
                while (it.hasNext()) {
                    List<String> res = it.next();
                    assertEquals(cells, res.size());
                    c++;
                }
                return c;
            }
            @Override
            public String getName() {
                return "XML";
            }
        };
        doBenchmark(counter);
    }

}
