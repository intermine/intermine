package org.intermine.webservice.client.widget;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.intermine.webservice.client.core.ServiceFactory;

public class Widget
{
    private final ServiceFactory factory;
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
     * @param factory the main service object for this web service.
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
            ServiceFactory factory,
            String name, String title, String description,
            String widgetType, String chartType,
            Collection<String> targets, Collection<String> filters,
            String xLabel, String yLabel) {
        this.factory = factory;
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

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getWidgetType() {
        return widgetType;
    }

    public boolean isEnrichment() {
        return "enrichment".equals(widgetType);
    }

    public boolean isChart() {
        return "chart".equals(widgetType);
    }

    public String getChartType() {
        return chartType;
    }

    public Set<String> getTargets() {
        return targets;
    }

    public Set<String> getFilters() {
        return filters;
    }

    public String getXAxisLabel() {
        return xLabel;
    }

    public String getYAxisLabel() {
        return yLabel;
    }
}
