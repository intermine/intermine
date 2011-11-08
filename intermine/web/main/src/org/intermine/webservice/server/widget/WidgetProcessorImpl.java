package org.intermine.webservice.server.widget;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

import org.intermine.web.logic.widget.config.EnrichmentWidgetConfig;
import org.intermine.web.logic.widget.config.GraphWidgetConfig;
import org.intermine.web.logic.widget.config.TableWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;

public abstract class WidgetProcessorImpl implements WidgetProcessor
{

    public abstract List<String> process(String name, WidgetConfig widgetConfig);
    protected Collection<String> getAvailableFilters(WidgetConfig widgetConfig) {
        Collection<String> availableFilters = Collections.EMPTY_LIST;
        try {
            Class<EnrichmentWidgetLdr> clazz =
                    (Class<EnrichmentWidgetLdr>) Class.forName(widgetConfig.getDataSetLoader());
            Method m = clazz.getDeclaredMethod("getAvailableFilters");
            availableFilters = (Collection<String>) m.invoke(null);
        } catch (NullPointerException e) {
            // Ignore
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            // Ignore
        } catch (SecurityException e) {
            e.printStackTrace();
            // Ignore
        } catch (NoSuchMethodException e) {
            // Ignore
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            // Ignore
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            // Ignore
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            // Ignore
        }
        return availableFilters;
    }

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
