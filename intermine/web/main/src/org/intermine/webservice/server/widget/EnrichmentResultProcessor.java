package org.intermine.webservice.server.widget;

import java.util.List;

public interface EnrichmentResultProcessor
{
    List<String> formatRow(List<Object> row);
}
