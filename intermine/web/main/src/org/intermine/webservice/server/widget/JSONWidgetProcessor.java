package org.intermine.webservice.server.widget;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.web.logic.widget.config.GraphWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.json.JSONObject;

public class JSONWidgetProcessor extends WidgetProcessorImpl
{

    private static final WidgetProcessor instance = new JSONWidgetProcessor();

    private JSONWidgetProcessor() {
        // Private constructor.
    }

    public static WidgetProcessor instance() {
        return instance;
    }

    @Override
    public List<String> process(String name, WidgetConfig widgetConfig) {
        Map<String, Object> backingMap = new HashMap<String, Object>();
        backingMap.put("name", name);
        backingMap.put("title", widgetConfig.getTitle());
        backingMap.put("description", widgetConfig.getDescription());
        backingMap.put("targets", getClasses(widgetConfig.getTypeClass()));
        backingMap.put("filters", widgetConfig.getFilters());
        String widgetType = getWidgetType(widgetConfig);
        backingMap.put("widgetType", widgetType);
        if (widgetType.equals("chart")) {
            backingMap.put("chartType", 
                    ((GraphWidgetConfig) widgetConfig).getGraphType());
            backingMap.put("labels", getLabels((GraphWidgetConfig) widgetConfig));
        }
        return new LinkedList<String>(Arrays.asList(new JSONObject(backingMap).toString()));
    }

    private List<String> getClasses(String tc) {
        List<String> ret = new LinkedList<String>();
        for (String s: StringUtils.split(tc, ",")) {
            ret.add(s);
        }
        return ret;
    }

}
