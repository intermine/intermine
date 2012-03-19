package org.intermine.webservice.server.widget;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

public class EnrichmentJSONProcessor implements WidgetResultProcessor {

    private static final WidgetResultProcessor instance = new EnrichmentJSONProcessor();

    private EnrichmentJSONProcessor() {
        // Not to be instantiated.
    }

    public static WidgetResultProcessor instance() {
        return instance;
    }

    @Override
    public List<String> formatRow(List<Object> row) {
        Map<String, Object> backingMap = new HashMap<String, Object>();
        backingMap.put("item", row.get(0));
        backingMap.put("description", row.get(1));
        backingMap.put("p-value", row.get(2));
        // Counts (index 3) are not necessary here, as it it trivial to
        // fetch from the matches array (as result.matches.length)
        List<Map<String, Object>> matchesDetail = (List<Map<String, Object>>) row.get(4);
        backingMap.put("matches", matchesDetail);
        JSONObject jo = new JSONObject(backingMap);
        return new LinkedList<String>(Arrays.asList(jo.toString()));
    }

}
