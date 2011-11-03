package org.intermine.webservice.server.widget;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.intermine.web.logic.widget.config.WidgetConfig;

public class FlatWidgetProcessor implements WidgetProcessor {

    private static final WidgetProcessor instance = new FlatWidgetProcessor();

    private FlatWidgetProcessor() {
        // Do not construct;
    }

    public static WidgetProcessor instance() {
        return instance;
    }

    @Override
    public List<String> process(String name, WidgetConfig widgetConfig) {
        return new LinkedList<String>(
                Arrays.asList(name, widgetConfig.getTitle(), widgetConfig.getDescription(),
                formatTypeClass(widgetConfig.getTypeClass())));
    }

    private static String formatTypeClass(String tc) {
        StringBuilder sb = new StringBuilder();
        for (String s: StringUtils.split(tc, ",")) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(s.substring(s.lastIndexOf('.') + 1));
        }
        return sb.toString();
    }
}
