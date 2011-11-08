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
