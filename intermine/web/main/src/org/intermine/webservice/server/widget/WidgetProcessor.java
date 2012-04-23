package org.intermine.webservice.server.widget;

import java.util.List;

import org.intermine.web.logic.widget.config.WidgetConfig;

public interface WidgetProcessor
{

    List<String> process(String name, WidgetConfig widgetConfig);
}
