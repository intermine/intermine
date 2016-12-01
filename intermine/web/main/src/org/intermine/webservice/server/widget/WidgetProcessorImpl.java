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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.web.logic.widget.config.EnrichmentWidgetConfig;
import org.intermine.web.logic.widget.config.GraphWidgetConfig;
import org.intermine.web.logic.widget.config.TableWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;

/**
 * A base class for Widget Processors
 * @author Alex Kalderimis
 *
 */
public abstract class WidgetProcessorImpl implements WidgetProcessor
{
    /** Of widget types there are several: */
    public enum WidgetType {
        /** There is the enrichment type, that calculates relationship enrichment **/
        ENRICHMENT,
        /** There is the chart type, that produces data suitable for visual display. **/
        CHART,
        /** There is the table type, that produces boring tables of boring data. **/
        TABLE,
        /** And there is a catch-all type, in case someone goes ahead and creates a new type. **/
        UNKNOWN
    };

    /**
     * Process a list called x with a widget y
     * @param name the name of the list.
     * @param widgetConfig the description of the widget.
     * @return results.
     */
    public abstract List<String> process(String name, WidgetConfig widgetConfig);

    /**
     * @param widgetConfig The description of the widget.
     * @return What type of widget we have here.
     */
    protected WidgetType getWidgetType(WidgetConfig widgetConfig) {
        WidgetType ret = WidgetType.UNKNOWN;
        if (widgetConfig instanceof EnrichmentWidgetConfig) {
            ret = WidgetType.ENRICHMENT;
        } else if (widgetConfig instanceof GraphWidgetConfig) {
            ret = WidgetType.CHART;
        } else if (widgetConfig instanceof TableWidgetConfig) {
            ret = WidgetType.TABLE;
        }
        return ret;
    }

    /**
     * Get the labels for Graph widgets.
     * @param widgetConfig The Graph widget config.
     * @return A mapping from axis name (x or y) to meat-friendly label.
     */
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
