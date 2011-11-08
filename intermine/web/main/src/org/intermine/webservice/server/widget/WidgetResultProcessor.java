package org.intermine.webservice.server.widget;

import java.util.List;

public interface WidgetResultProcessor
{
    List<String> formatRow(List<Object> row);
}
