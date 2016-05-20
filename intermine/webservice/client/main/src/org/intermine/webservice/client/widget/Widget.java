package org.intermine.webservice.client.widget;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A representation of a list analysis widget, containing the metadata necessary
 * to describe its usage and to choose how and whether to display its results.
 * @author Alex Kalderimis
 *
 */
public class Widget
{
    private final String name;
    private final String title;
    private final String description;
    private final String widgetType;
    private final String chartType;
    private final Set<String> targets;
    private final Set<String> filters;
    private final String xLabel, yLabel;

    /**
     * Constructor.
     *
     * Intended for use by the widget parser.
     *
     * @param name The Name of this widget.
     * @param title The title of this widget.
     * @param description the description of this widget.
     * @param widgetType The type of widget (enrichment, chart, etc).
     * @param chartType The type of chart if a chart widget (otherwise null).
     * @param targets The classes this widget may consume.
     * @param filters The available filter values.
     * @param xLabel The label for the X-Axis if a chart widget.
     * @param yLabel The label for the Y-Axis if a chart widget.
     */
    public Widget(
            String name, String title, String description,
            String widgetType, String chartType,
            Collection<String> targets, Collection<String> filters,
            String xLabel, String yLabel) {
        this.name = name;
        this.title = title;
        this.description = description;
        this.widgetType = widgetType;
        this.chartType = chartType;
        this.targets
            = Collections.unmodifiableSet(new HashSet<String>(targets));
        this.filters
            = Collections.unmodifiableSet(new HashSet<String>(filters));
        this.xLabel = xLabel;
        this.yLabel = yLabel;
    }

    /** @return the name of the widget **/
    public String getName() {
        return name;
    }

    /** @return the human readable descriptive title of the widget **/
    public String getTitle() {
        return title;
    }

    /** @return the longer description of this widget, if any **/
    public String getDescription() {
        return description;
    }

    /** @return the type of widget this object describes (eg. 'enrichment', 'chart'). **/
    public String getWidgetType() {
        return widgetType;
    }

    /** @return whether or not this is an enrichment widget **/
    public boolean isEnrichment() {
        return "enrichment".equals(widgetType);
    }

    /** @return whether or not this is a chart widget **/
    public boolean isChart() {
        return "chart".equals(widgetType);
    }

    /**
     * @return the type of chart widget this object represents. Only makes sense for chart widgets.
     */
    public String getChartType() {
        return chartType;
    }

    /**
     * @return The names of classes this widget can analyse.
     */
    public Set<String> getTargets() {
        return targets;
    }

    /** @return the valid values of the 'filter' property. **/
    public Set<String> getFilters() {
        return filters;
    }

    /** @return the label for the domain (X) axis. Only makes sense for chart widgets. **/
    public String getXAxisLabel() {
        return xLabel;
    }

    /** @return the label for the range (Y) axis. Only makes sense for chart widgets. **/
    public String getYAxisLabel() {
        return yLabel;
    }
}
