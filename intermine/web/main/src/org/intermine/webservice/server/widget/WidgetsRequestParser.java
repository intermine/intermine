package org.intermine.webservice.server.widget;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.intermine.webservice.server.exceptions.BadRequestException;

/**
 * Request processor for WidgetsService that process request, validates it and returns
 * parsed input as a parameter object.
 * @author "Xavier Watkins"
 *
 */
public class WidgetsRequestParser
{
    /**
     * The identifier for the widget config
     */
    private static final String WIDGET_ID = "widget";

    // These parameters are part of the public API - please don't change them without:
    //   a) making them backwards compatible where possible, and
    //   b) Letting all client library maintainers known what has changed and why.
    static final String BAG_NAME = "list";
    static final String POPULATION_BAG_NAME_OLD = "population";
    static final String SAVE_POPULATION = "remember_population";
    static final String FILTER = "filter";
    static final String MAXP = "maxp";
    static final String ERROR_CORRECTION = "correction";
    static final String EXTRA_ATTRIBUTE = "gene_length_correction";
    /* But this parameter is also accessed in the EnrichmentWidgetResultService */
    static final String POPULATION_BAG_NAME = "current_population";

    private Set<String> requiredParameters;
    private Map<String, String> defaults;

    /**
     * ListsRequestProcessor constructor.
     */
    public WidgetsRequestParser() {
        this.requiredParameters = new HashSet<String>();
        defaults = new HashMap<String, String>();
        requiredParameters.add(BAG_NAME);
        requiredParameters.add(WIDGET_ID);
    }

    /**
     * Set a parameter default.
     * @param name the parameter name
     * @param value the default value.
     */
    public void setDefaultValue(String name, String value) {
        defaults.put(name, value);
    }

    /**
     * Declare that a particular parameter is required.
     * @param parameter The parameter we insist on having.
     */
    public void parameterIsRequired(String parameter) {
        requiredParameters.add(parameter);
    }

    private String getOrDefault(HttpServletRequest request, String parameter) {
        return StringUtils.defaultIfBlank(
                request.getParameter(parameter), defaults.get(parameter));
    }

    /**
     * Returns parameters of web service as a parameter object.
     *
     * @param request request
     * @return ListsService input
     */
    public WidgetsServiceInput getInput(HttpServletRequest request) {

        Set<String> missingParameters = new HashSet<String>();
        for (String param: requiredParameters) {
            if (isBlank(request.getParameter(param))) {
                missingParameters.add(param);
            }
        }
        if (!missingParameters.isEmpty()) {
            throw new BadRequestException("Bad parameters. I expected a value for each of "
                    + requiredParameters
                    + " but I didn't get any values for "
                    + missingParameters);
        }

        WidgetsServiceInput.Builder ret = new WidgetsServiceInput.Builder();

        String widgetId = request.getParameter(WIDGET_ID);
        String bagName = request.getParameter(BAG_NAME);
        String populationBagName = request.getParameter(POPULATION_BAG_NAME);
        String savePopulation = request.getParameter(SAVE_POPULATION);
        String filter = getOrDefault(request, FILTER);
        String maxP = getOrDefault(request, MAXP);
        String errorCorrection = getOrDefault(request, ERROR_CORRECTION);
        String extraAttribute = getOrDefault(request, EXTRA_ATTRIBUTE);

        // This horror of a piece of code is to deal with a backwards
        // incompatible parameter change.
        // POPULATION_BAG_NAME takes precedence over POPULATION_BAG_NAME_OLD.
        String oldPopName = request.getParameter(POPULATION_BAG_NAME_OLD);
        if (isBlank(populationBagName) && !isBlank(oldPopName)) {
            populationBagName = oldPopName;
        }

        ret.setBagName(bagName);
        ret.setWidgetId(widgetId);
        ret.setFilter(filter);
        ret.setExtraAttribute(extraAttribute);
        ret.setCorrection(errorCorrection);
        ret.setPopulationBagName(populationBagName);
        ret.setSavePopulation("true".equalsIgnoreCase(savePopulation));
        if (!isBlank(maxP)) {
            try {
                ret.setMaxP(Double.parseDouble(maxP));
            } catch (NumberFormatException e) {
                throw new BadRequestException(
                        "The value of " + MAXP + " should be a valid number.");
            }
            if (ret.getMaxPValue() < 0 || ret.getMaxPValue() > 1) {
                throw new BadRequestException(
                        "The value of " + MAXP + " should be between 0 and 1.");
            }
        }
        return ret;
    }
}
