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

import javax.servlet.http.HttpServletRequest;

import org.intermine.webservice.server.exceptions.BadRequestException;
import static org.apache.commons.lang.StringUtils.isBlank;

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
    private static final String WIDGET_ID = "widget";

    private static final String BAG_NAME = "list";
    private static final String POPULATION_BAG_NAME = "population";
    private static final String FILTER = "filter";
    private static final String MAXP = "maxp";
    private static final String ERROR_CORRECTION = "correction";

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
        String bagName = request.getParameter(BAG_NAME);
        String populationBagName = request.getParameter(POPULATION_BAG_NAME);
        String filter = request.getParameter(FILTER);
        String maxP = request.getParameter(MAXP);
        String errorCorrection = request.getParameter(ERROR_CORRECTION);

        if (isBlank(widgetId) || isBlank(bagName)
                || isBlank(maxP) || isBlank(errorCorrection)) {
            throw new BadRequestException("Bad parameters. I expected a value for each of "
                + Arrays.asList(WIDGET_ID, BAG_NAME, FILTER, MAXP, ERROR_CORRECTION)
                + " but I got these parameters instead: " + request.getParameterMap().keySet());
        }
        ret.setBagName(bagName);
        ret.setPopulationBagName(populationBagName);
        ret.setWidgetId(widgetId);
        ret.setExtraAttributes(Arrays.asList(filter, maxP, errorCorrection));

        return ret;
    }
}
