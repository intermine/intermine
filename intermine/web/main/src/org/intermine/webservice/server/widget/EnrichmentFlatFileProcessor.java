package org.intermine.webservice.server.widget;

import java.util.LinkedList;
import java.util.List;

public class EnrichmentFlatFileProcessor implements EnrichmentResultProcessor {

    private static final EnrichmentResultProcessor instance = new EnrichmentFlatFileProcessor();

    private EnrichmentFlatFileProcessor() {
        // Do not instantiate.
    }

    public static EnrichmentResultProcessor instance() {
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
