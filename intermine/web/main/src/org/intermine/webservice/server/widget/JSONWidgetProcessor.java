package org.intermine.webservice.server.widget;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.json.JSONObject;

public class JSONWidgetProcessor implements WidgetProcessor {

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
        return new LinkedList<String>(Arrays.asList(new JSONObject(backingMap).toString()));
    }

    private List<String> getClasses(String tc) {
        List<String> ret = new LinkedList<String>();
        for (String s: StringUtils.split(tc, ",")) {
            ret.add(s.substring(s.lastIndexOf('.') + 1));
        }
        return ret;
    }

}
