package org.intermine.webservice.server.widget;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.intermine.webservice.server.exceptions.BadRequestException;

/**
 * Request processor for WidgetsService that process request, validates it and returns
 * parsed input as a parameter object.
 * @author "Xavier Watkins"
 *
 */
public class WidgetsRequestParser
{
    private HttpServletRequest request;

    /**
     * The identifier for the widget config
     */
    private static final String WIDGET_ID = "widgetId";
    /**
     * The type for building the list
     */
    private static final String CLASS_NAME = "className";
    /**
     * The list of extra attributed, depending on the type of WidgetConfig
     */
    private static final String EXTRA_ATTRIBUTES = "extraAttributes";
    /**
     * The list of ids for building the list
     */
    private static final String IDS = "ids";

    /**
     * ListsRequestProcessor constructor.
     * @param request request
     */
    public WidgetsRequestParser(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Returns parameters of web service as a parameter object.
     * @return ListsService input
     */
    public WidgetsServiceInput getInput() {
        WidgetsServiceInput ret = new WidgetsServiceInput();

        String widgetId = request.getParameter(WIDGET_ID);
        String className = request.getParameter(CLASS_NAME);
        String extraAttributes = request.getParameter(EXTRA_ATTRIBUTES);
        String ids = request.getParameter(IDS);

        if (widgetId == null || widgetId.length() <= 0
                        || className == null || className.length() <= 0
                        || ids == null || ids.length() <= 0) {
            throw new BadRequestException("Parameters: " + WIDGET_ID + ", " + CLASS_NAME + ", "
                         + EXTRA_ATTRIBUTES + ", " + IDS + " are required.");
        }
        ret.setClassName(className);
        ret.setExtraAttributes(Arrays.asList(extraAttributes.split(",")));
        ret.setIds(Arrays.asList(ids.split(",")));
        ret.setWidgetId(widgetId);

        return ret;
    }
}
