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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.widget.EnrichmentWidget;
import org.intermine.web.logic.widget.config.EnrichmentWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.output.JSONFormatter;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;
import org.intermine.webservice.server.output.XMLFormatter;

/**
 * Web service that returns a widget for a given list of identifiers See
 * {@link WidgetsRequestProcessor} for parameter description
 * URL examples: get an EnrichmentWidget
 * /service/widgets?widgetId=go_enrichment&amp;className=Gene&amp;extraAttributes=Bonferroni,0.1
 * ,biological_process&amp;ids=S000000003,S000000004&amp;format=html
 * get a GraphWidget
 * /service/widgets?widgetId=flyatlas
 *   &amp;className=Gene&amp;extraAttributes=
 *   &amp;ids=FBgn0011648,FBgn0011655,FBgn0025800
 *   &amp;format=html
 *
 * @author Alex Kalderimis
 * @author Xavier Watkins
 */
public class EnrichmentWidgetResultService extends JSONService
{
    private class EnrichmentXMLFormatter extends XMLFormatter
    {

        @Override
        public String formatResult(List<String> resultRow) {
            return StringUtils.join(resultRow, "");
        }

    }

    public EnrichmentWidgetResultService(InterMineAPI im) {
        super(im);
    }

    /**
     * Executes service specific logic.
     *
     * @throws Exception an error has occurred
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void execute() throws Exception {
        WidgetsServiceInput input = getInput();
        Profile profile = permission.getProfile();

        InterMineBag imBag = im.getBagManager().getUserOrGlobalBag(profile, input.getBagName());
        if (imBag == null) {
            throw new BadRequestException("You do not have access to a bag named"
                                          + input.getBagName());
        }
        addOutputInfo("type", imBag.getType());
        addOutputInfo("list", imBag.getName());
        addOutputInfo("requestedAt", new Date().toGMTString());

        WebConfig webConfig = InterMineContext.getWebConfig();
        WidgetConfig widgetConfig = webConfig.getWidgets().get(input.getWidgetId());

        if (widgetConfig == null || !(widgetConfig instanceof EnrichmentWidgetConfig)) {
            throw new ResourceNotFoundException("Could not find an enrichment widget called \""
                                                + input.getWidgetId() + "\"");
        }
        EnrichmentWidgetConfig enrichmentWidgetConfig = (EnrichmentWidgetConfig) widgetConfig;
        addOutputConfig(enrichmentWidgetConfig);
        //filters
        String filterSelectedValue = input.getExtraAttributes().get(0);
        if (filterSelectedValue == null || "".equals(filterSelectedValue)) {
            String filters = enrichmentWidgetConfig.getFilters();
            if (filters != null && !"".equals(filters)) {
                filterSelectedValue = filters.split("\\,")[0];
                input.getExtraAttributes().set(0, filterSelectedValue);
            }
        }
        addOutputFilter(enrichmentWidgetConfig, filterSelectedValue);

        EnrichmentWidget widget = null;
        try {
            widget = (EnrichmentWidget) widgetConfig.getWidget(imBag, im.getObjectStore(),
                input.getExtraAttributes());
        } catch (ClassCastException e) {
            throw new ResourceNotFoundException("Could not find an enrichment widget called \""
                                               + input.getWidgetId() + "\"");
        }
        addOutputInfo("notAnalysed", Integer.toString(widget.getNotAnalysed()));
        addOutputPathQuery(widget, enrichmentWidgetConfig);
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

    private void addOutputConfig(EnrichmentWidgetConfig config) {
        addOutputInfo("label", config.getLabel());
        addOutputInfo("title", config.getTitle());
        addOutputInfo("description", config.getDescription());
    }

    private void addOutputFilter(EnrichmentWidgetConfig widgetConfig, String filterSelectedValue) {
        String filterLabel = widgetConfig.getFilterLabel();
        if (filterLabel != null && !"".equals(filterLabel)) {
            addOutputInfo("filterLabel", filterLabel);
        }
        String filters = widgetConfig.getFilters();
        if (filters != null && !"".equals(filters)) {
            addOutputInfo("filters", filters);
            addOutputInfo("filterSelectedValue", filterSelectedValue);
        }
    }

    private void addOutputPathQuery(EnrichmentWidget widget, EnrichmentWidgetConfig config) {
        addOutputInfo("pathQuery", widget.getPathQuery().toJson());
        String enrichIdentifier = config.getEnrichIdentifier();
        String pathConstraint = "";
        if (enrichIdentifier != null && !"".equals(enrichIdentifier)) {
            pathConstraint = enrichIdentifier;
        } else {
            pathConstraint = config.getEnrich();
        }
        if (pathConstraint.contains("[")) {
            String part1 = pathConstraint.substring(0, pathConstraint.indexOf("["));
            String part2 = pathConstraint.substring(pathConstraint.indexOf("]") + 1);
            pathConstraint = part1 + part2;
        }
        addOutputInfo("pathConstraint", config.getStartClass() + "." + pathConstraint);
        //addOutputInfo("matchPathConstraint", config.getStartClass() + ".id");
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

    private WidgetResultProcessor getProcessor() {
        if (formatIsJSON()) {
            return EnrichmentJSONProcessor.instance();
        } else if (formatIsXML()) {
            return EnrichmentXMLProcessor.instance();
        } else {
            return FlatFileWidgetResultProcessor.instance();
        }
    }

    protected Output makeXMLOutput(PrintWriter out) {
        ResponseUtil.setXMLHeader(response, "result.xml");
        return new StreamedOutput(out, new EnrichmentXMLFormatter());
    }

    private WidgetsServiceInput getInput() {
        return new WidgetsRequestParser(request).getInput();
    }
}
