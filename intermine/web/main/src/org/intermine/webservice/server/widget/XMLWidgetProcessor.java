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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.intermine.web.logic.widget.config.GraphWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;

/**
 * A processor for handling requests for widget metadata.
 * @author Alex Kalderimis
 *
 */
public final class XMLWidgetProcessor extends WidgetProcessorImpl
{

    private static final WidgetProcessor INSTANCE = new XMLWidgetProcessor();

    private XMLWidgetProcessor() {
        // Do not construct;
    }

    /**
     * @return The singleton widget processor.
     */
    public static WidgetProcessor instance() {
        return INSTANCE;
    }

    @Override
    public List<String> process(String name, WidgetConfig widgetConfig) {
        StringBuilder sb = new StringBuilder("<widget>");
        sb.append(formatCell("name", name));
        sb.append(formatCell("title", widgetConfig.getTitle()));
        sb.append(formatCell("description", widgetConfig.getDescription()));
        String widgetType = getWidgetType(widgetConfig);
        sb.append(formatCell("widgetType", widgetType));
        if ("chart".equals(widgetType)) {
            sb.append(formatCell("chartType",
                    ((GraphWidgetConfig) widgetConfig).getGraphType()));
            sb.append(formatCell("labels", getLabels((GraphWidgetConfig) widgetConfig)));
        }
        sb.append(formatCell("target", getClasses(widgetConfig.getTypeClass())));
        sb.append(formatCell("filter", widgetConfig.getFilters()));
        sb.append("</widget>");
        return new LinkedList<String>(Arrays.asList(sb.toString()));
    }

    @SuppressWarnings("rawtypes")
    private String formatCell(String name, Object contents) {
        StringBuffer sb = new StringBuffer();
        if (contents instanceof Collection) {
            for (Object o: (Collection) contents) {
                sb.append(formatCell(name, o));
            }
        }  else {
            sb.append("<" + name + ">");
            if (contents instanceof Map) {
                for (Object k: ((Map) contents).keySet()) {
                    sb.append(formatCell(k.toString(), ((Map) contents).get(k)));
                }
            } else {
                sb.append(StringEscapeUtils.escapeXml(contents.toString()));
            }
            sb.append("</" + name + ">");
        }
        return sb.toString();
    }

    private List<String> getClasses(String tc) {
        List<String> ret = new LinkedList<String>();
        for (String s: StringUtils.split(tc, ",")) {
            ret.add(s.substring(s.lastIndexOf('.') + 1));
        }
        return ret;
    }

}
