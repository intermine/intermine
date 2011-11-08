package org.intermine.webservice.server.widget;

import java.util.LinkedList;
import java.util.List;

public class EnrichmentFlatFileProcessor implements WidgetResultProcessor {

    private static final WidgetResultProcessor instance = new EnrichmentFlatFileProcessor();

    private EnrichmentFlatFileProcessor() {
        // Do not instantiate.
    }

    public static WidgetResultProcessor instance() {
        return instance;
    }

    @Override
    public List<String> formatRow(List<Object> row) {
        List<String> strings = new LinkedList<String>();
        for (Object o: row) {
            strings.add(o.toString());
        }
        return strings;
    }

}
