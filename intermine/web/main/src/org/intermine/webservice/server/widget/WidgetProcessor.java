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

import java.util.List;

import org.intermine.web.logic.widget.config.WidgetConfig;

/**
 * @author Alex Kalderimis
 */
public interface WidgetProcessor
{

    /**
     * Process a widget.
     * @param name The name of the list.
     * @param widgetConfig The widget.
     * @return A list of strings to output.
     */
    List<String> process(String name, WidgetConfig widgetConfig);
}
