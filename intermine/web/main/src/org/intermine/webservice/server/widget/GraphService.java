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

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.widget.GraphWidget;
import org.intermine.web.logic.widget.config.GraphWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;
import org.intermine.webservice.server.output.XMLFormatter;

public class GraphService extends WidgetService
{

    private class GraphXMLFormatter extends XMLFormatter {

        @Override
        public String formatResult(List<String> resultRow) {
            return StringUtils.join(resultRow, "");

        }

    }

    public GraphService(InterMineAPI im) {
        super(im);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void execute() throws Exception {
        GraphInput input = new GraphInput(request);

        InterMineBag imBag = retrieveBag(input.list);
        addOutputListInfo(imBag);

        WebConfig webConfig = InterMineContext.getWebConfig();
        WidgetConfig widgetConfig = webConfig.getWidgets().get(input.widget);
        if (widgetConfig == null || !(widgetConfig instanceof GraphWidgetConfig)) {
            throw new ResourceNotFoundException("Could not find a graph widget called \""
                    + input.widget + "\"");
        }

        addOutputConfig(widgetConfig);

        //filters
        String filterSelectedValue = input.filter;
        if (filterSelectedValue == null || "".equals(filterSelectedValue)) {
            String filters = widgetConfig.getFiltersValues(im.getObjectStore(), imBag);
            if (filters != null && !"".equals(filters)) {
                filterSelectedValue = filters.split("\\,")[0];
            }
        }
        addOutputFilter(widgetConfig, filterSelectedValue, imBag);

        GraphWidget widget = null;
        try {
            widget = (GraphWidget) widgetConfig.getWidget(imBag,
                    im.getObjectStore(), Arrays.asList(filterSelectedValue));
        } catch (ClassCastException e) {
            throw new ResourceNotFoundException("Could not find a graph widget called \""
                    + input.widget + "\"", e);
        }
        if (widget == null) {
            throw new InternalErrorException("Problem loading widget");
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
        addOutputAttribute("domainLabel", graphConfig.getDomainLabel());
        addOutputAttribute("rangeLabel", graphConfig.getRangeLabel());
    }

    protected WidgetResultProcessor getProcessor() {
        if (formatIsJSON()) {
            return GraphJSONProcessor.instance();
        } else if (formatIsXML()) {
            return new GraphXMLProcessor();
        } else {
            return FlatFileWidgetResultProcessor.instance();
        }
    }

    protected Output makeXMLOutput(PrintWriter out) {
        ResponseUtil.setXMLHeader(response, "result.xml");
        return new StreamedOutput(out, new GraphXMLFormatter());
    }

    private static class GraphInput
    {
        String filter;
        final String widget;
        final String list;

        GraphInput(HttpServletRequest request) {
            filter = request.getParameter("filter");
            widget = request.getParameter("widget");
            list = request.getParameter("list");
            if (StringUtils.isBlank(list)
                    || StringUtils.isBlank(widget)) {
                throw new BadRequestException("The parameters "
                        + "\"widget\" and \"list\" are required, but "
                        + "I got " + request.getParameterMap().keySet());
            }
        }
    }

}
