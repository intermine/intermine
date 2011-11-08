package org.intermine.webservice.server.widget;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;

public class GraphJSONProcessor implements WidgetResultProcessor
{
    private static final WidgetResultProcessor instance = new GraphJSONProcessor();

    private GraphJSONProcessor() {
        // Not to be instantiated.
    }

    public static WidgetResultProcessor instance() {
        return instance;
    }

    @Override
    public List<String> formatRow(List<Object> row) {
        JSONArray ja = new JSONArray(row);
        return new LinkedList<String>(Arrays.asList(ja.toString()));
    }
}
