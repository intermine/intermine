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


import java.io.PrintWriter;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import org.intermine.api.InterMineAPI;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;

import org.intermine.web.logic.config.WebConfig;

import org.intermine.web.logic.export.ResponseUtil;

import org.intermine.web.logic.session.SessionMethods;

import org.intermine.web.logic.widget.GraphWidget;

import org.intermine.web.logic.widget.config.GraphWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;

import org.intermine.webservice.server.core.JSONService;

import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;

import org.intermine.webservice.server.output.JSONFormatter;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;
import org.intermine.webservice.server.output.XMLFormatter;

/**
 * Service for presenting from Graph Widgets.
 * @author Alex Kalderimis
 *
 */
public class GraphService extends JSONService
{

    private class GraphXMLFormatter extends XMLFormatter
    {

        @Override
        public String formatResult(List<String> resultRow) {
            return StringUtils.join(resultRow, "");

        }

    }

    /**
     * Constructor
     * @param im The InterMine application object.
     */
    public GraphService(InterMineAPI im) {
        super(im);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void execute() {
        GraphInput input = new GraphInput(request);
        Profile profile = SessionMethods.getProfile(request.getSession());

        InterMineBag imBag = im.getBagManager().getUserOrGlobalBag(profile, input.list);
        if (imBag == null) {
            throw new BadRequestException("You do not have access to a bag named" + input.list);
        }
        addOutputInfo("type", imBag.getType());
        addOutputInfo("list", imBag.getName());
        addOutputInfo("requestedAt", new Date().toGMTString());

        WebConfig webConfig = SessionMethods.getWebConfig(request);
        WidgetConfig widgetConfig = webConfig.getWidgets().get(input.widget);

        if (widgetConfig == null || !(widgetConfig instanceof GraphWidgetConfig)) {
            throw new ResourceNotFoundException("Could not find a graph widget called \""
                    + input.widget + "\"");
        }

        addOutputInfo("title", widgetConfig.getTitle());
        addOutputInfo("description", widgetConfig.getDescription());
        addOutputInfo("chartType", ((GraphWidgetConfig) widgetConfig).getGraphType());

        GraphWidget widget = null;
        try {
            widget = (GraphWidget) widgetConfig.getWidget(imBag,
                    im.getObjectStore(), Arrays.asList(input.filter));
        } catch (ClassCastException e) {
            throw new ResourceNotFoundException("Could not find a graph widget called \""
                    + input.widget + "\"");
        }
        if (widget == null) {
            throw new InternalErrorException("Problem loading widget");
        }

        WidgetResultProcessor processor = getProcessor();
        Iterator<List<Object>> it = widget.getResults().iterator();
        while (it.hasNext()) {
            List<Object> row = it.next();
            List<String> processed = processor.formatRow(row);
            if (!formatIsFlatFile() && it.hasNext()) {
                processed.add("");
            }
            output.addResultItem(processed);
        }
    }

    @Override
    protected Map<String, Object> getHeaderAttributes() {
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.putAll(super.getHeaderAttributes());
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"results\":[");
            attributes.put(JSONFormatter.KEY_OUTRO, "]");
        }
        return attributes;
    }

    /**
     * Get the widget processor to use to present the data.
     * @return A processor for the results.
     */
    protected WidgetResultProcessor getProcessor() {
        if (formatIsJSON()) {
            return GraphJSONProcessor.instance();
        } else if (formatIsXML()) {
            return new GraphXMLProcessor();
        } else {
            return FlatFileWidgetResultProcessor.instance();
        }
    }

    @Override
    protected Output makeXMLOutput(PrintWriter out, String separator) {
        ResponseUtil.setXMLHeader(response, "result.xml");
        return new StreamedOutput(out, new GraphXMLFormatter());
    }

    private static class GraphInput
    {
        final String filter;
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
