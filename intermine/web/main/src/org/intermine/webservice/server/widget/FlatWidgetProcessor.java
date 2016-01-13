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
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.intermine.web.logic.widget.config.WidgetConfig;

/**
 * @author Alex Kalderimis
 */
public final class FlatWidgetProcessor extends WidgetProcessorImpl
{

    private static final WidgetProcessor INSTANCE = new FlatWidgetProcessor();

    private FlatWidgetProcessor() {
        // Do not construct;
    }

    /** @return an instance of WidgetProcessor **/
    public static WidgetProcessor instance() {
        return INSTANCE;
    }

    @Override
    public List<String> process(String name, WidgetConfig widgetConfig) {
        return new LinkedList<String>(
                Arrays.asList(
                        name,
                        widgetConfig.getTitle(),
                        widgetConfig.getDescription(),
                        getWidgetType(widgetConfig).toString(),
                        formatTypeClass(widgetConfig.getTypeClass()),
                        widgetConfig.getFilters()));
    }

    private static String formatTypeClass(String tc) {
        StringBuilder sb = new StringBuilder();
        for (String s: StringUtils.split(tc, ",")) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(s);
        }
        return sb.toString();
    }
}
