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

import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.widget.GraphWidget;
import org.intermine.web.logic.widget.config.GraphWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;
import org.intermine.webservice.server.output.XMLFormatter;

/** @author Alex Kalderimis **/
public class GraphService extends WidgetService
{

    private class GraphXMLFormatter extends XMLFormatter
    {

        @Override
        public String formatResult(List<String> resultRow) {
            return StringUtils.join(resultRow, "");

        }

    }

    private final WidgetsRequestParser requestParser;

    /** @param im the InterMine state object **/
    public GraphService(InterMineAPI im) {
        super(im);
        requestParser = new WidgetsRequestParser();
    }

    @Override
    protected void execute() throws Exception {
        WidgetsServiceInput input = requestParser.getInput(request);

        InterMineBag imBag = retrieveBag(input.getBagName());
        addOutputListInfo(imBag);

        WebConfig webConfig = InterMineContext.getWebConfig();
        WidgetConfig widgetConfig = webConfig.getWidgets().get(input.getWidgetId());
        if (widgetConfig == null || !(widgetConfig instanceof GraphWidgetConfig)) {
            throw new ResourceNotFoundException(
                    "Could not find a graph widget called \""
                    + input.getWidgetId() + "\"");
        }

        addOutputConfig(widgetConfig);

        //filters
        String filterSelectedValue = input.filter;
        if (StringUtils.isBlank(filterSelectedValue)) {
            filterSelectedValue = getDefaultFilterValue(widgetConfig, imBag);
        }
        addOutputFilter(widgetConfig, filterSelectedValue, imBag);

        GraphWidget widget = null;
        try {
            widget = (GraphWidget) widgetConfig.getWidget(imBag, null,
                    im.getObjectStore(), input);
            if (filterSelectedValue != null) {
                widget.setFilter(filterSelectedValue);
            }
            widget.process();
        } catch (ClassCastException e) {
            throw new ResourceNotFoundException(
                    "Could not find a graph widget called \""
                    + input.getWidgetId() + "\"", e);
        }
        addOutputInfo("notAnalysed", Integer.toString(widget.getNotAnalysed()));
        addOutputInfo("simplePathQuery", widget.getSimplePathQuery().toJson());
        addOutputInfo("pathQuery", widget.getPathQuery().toJson());

        addOutputResult(widget);
    }

    @Override
    protected void addOutputConfig(WidgetConfig config) {
        super.addOutputConfig(config);
        GraphWidgetConfig graphConfig = (GraphWidgetConfig) config;
        addOutputInfo("chartType", graphConfig.getGraphType());
        addOutputAttribute("seriesValues", graphConfig.getSeriesValues());
        addOutputAttribute("seriesLabels", graphConfig.getSeriesLabels());
        addOutputAttribute("seriesPath", graphConfig.getSeriesPath());
        addOutputAttribute("domainLabel", graphConfig.getDomainLabel());
        addOutputAttribute("rangeLabel", graphConfig.getRangeLabel());
    }

    /** @return a widget result processor **/
    protected WidgetResultProcessor getProcessor() {
        if (formatIsJSON()) {
            return GraphJSONProcessor.instance();
        } else if (formatIsXML()) {
            return new GraphXMLProcessor();
        } else {
            return FlatFileWidgetResultProcessor.instance();
        }
    }

    /**
     * @param out The raw XML output.
     * @return An output object.
     */
    protected Output makeXMLOutput(PrintWriter out) {
        ResponseUtil.setXMLHeader(response, "result.xml");
        return new StreamedOutput(out, new GraphXMLFormatter());
    }

}
