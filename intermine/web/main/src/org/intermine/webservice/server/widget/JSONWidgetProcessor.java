package org.intermine.webservice.server.widget;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.web.logic.widget.config.GraphWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.json.JSONObject;

/**
 * @author Alex Kalderimis
 */
public final class JSONWidgetProcessor extends WidgetProcessorImpl
{

    private static final WidgetProcessor INSTANCE = new JSONWidgetProcessor();

    private JSONWidgetProcessor() {
        // Private constructor.
    }

    /**
     * @return An instance of WidgetProcessor
     */
    public static WidgetProcessor instance() {
        return INSTANCE;
    }

    @Override
    public List<String> process(String name, WidgetConfig widgetConfig) {
        Map<String, Object> backingMap = new HashMap<String, Object>();
        backingMap.put("name", name);
        backingMap.put("title", widgetConfig.getTitle());
        backingMap.put("description", widgetConfig.getDescription());
        backingMap.put("targets", getClasses(widgetConfig.getTypeClass()));
        backingMap.put("filters", widgetConfig.getFilters());
        WidgetType widgetType = getWidgetType(widgetConfig);
        backingMap.put("widgetType", widgetType.name().toLowerCase());
        if (widgetType == WidgetType.CHART) {
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
