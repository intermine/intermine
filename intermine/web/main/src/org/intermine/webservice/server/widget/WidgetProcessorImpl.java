package org.intermine.webservice.server.widget;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.lang.reflect.InvocationTargetException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.web.logic.widget.config.EnrichmentWidgetConfig;
import org.intermine.web.logic.widget.config.GraphWidgetConfig;
import org.intermine.web.logic.widget.config.TableWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;

public abstract class WidgetProcessorImpl implements WidgetProcessor
{

    public abstract List<String> process(String name, WidgetConfig widgetConfig);

    protected String getWidgetType(WidgetConfig widgetConfig) {
        String widgetType = "unknown";
        if (widgetConfig instanceof EnrichmentWidgetConfig) {
            widgetType = "enrichment";
        } else if (widgetConfig instanceof GraphWidgetConfig) {
            widgetType = "chart";
        } else if (widgetConfig instanceof TableWidgetConfig) {
            widgetType = "table";
        }
        return widgetType;
    }

    protected Map<String, String> getLabels(GraphWidgetConfig widgetConfig) {
        Map<String, String> labels = new HashMap<String, String>();
        String domainAxis, rangeAxis;
        if ("StackedBarChart".equals(widgetConfig.getGraphType())) {
            domainAxis = "y";
            rangeAxis = "x";
        } else {
            domainAxis = "x";
            rangeAxis = "y";
        }
        labels.put(domainAxis, widgetConfig.getDomainLabel());
        labels.put(rangeAxis, widgetConfig.getRangeLabel());
        return labels;
    }
}
