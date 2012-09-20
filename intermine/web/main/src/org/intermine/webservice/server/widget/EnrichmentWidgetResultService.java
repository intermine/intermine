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
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.widget.EnrichmentWidget;
import org.intermine.web.logic.widget.config.EnrichmentWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
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
public class EnrichmentWidgetResultService extends WidgetService
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
        InterMineBag imBag = retrieveBag(input.getBagName());
        addOutputListInfo(imBag);

        WebConfig webConfig = InterMineContext.getWebConfig();
        WidgetConfig widgetConfig = webConfig.getWidgets().get(input.getWidgetId());

        if (widgetConfig == null || !(widgetConfig instanceof EnrichmentWidgetConfig)) {
            throw new ResourceNotFoundException("Could not find an enrichment widget called \""
                                                + input.getWidgetId() + "\"");
        }
        addOutputConfig(widgetConfig);

        //filters
        String filterSelectedValue = input.getExtraAttributes().get(0);
        if (filterSelectedValue == null || "".equals(filterSelectedValue)) {
            String filters = widgetConfig.getFiltersValues(im.getObjectStore(), imBag);
            if (filters != null && !"".equals(filters)) {
                filterSelectedValue = filters.split("\\,")[0];
                input.getExtraAttributes().set(0, filterSelectedValue);
            }
        }
        addOutputFilter(widgetConfig, filterSelectedValue, imBag);

        String populationBagName = input.getPopulationBagName();
        InterMineBag populationBag = (populationBagName != null)
                                     ? retrieveBag(populationBagName)
                                     : null;
        EnrichmentWidget widget = null;
        try {
            widget = (EnrichmentWidget) widgetConfig.getWidget(imBag, populationBag,
                im.getObjectStore(), input.getExtraAttributes());
        } catch (ClassCastException e) {
            throw new ResourceNotFoundException("Could not find an enrichment widget called \""
                                               + input.getWidgetId() + "\"");
        }
        addOutputInfo("notAnalysed", Integer.toString(widget.getNotAnalysed()));
        addOutputPathQuery(widget, widgetConfig);

        addOutputResult(widget);
    }

    @Override
    protected void addOutputConfig(WidgetConfig config) {
        super.addOutputConfig(config);
        addOutputAttribute("label", ((EnrichmentWidgetConfig) config).getLabel());
        addOutputAttribute("externalLink", ((EnrichmentWidgetConfig) config).getExternalLink());
    }

    private void addOutputPathQuery(EnrichmentWidget widget, WidgetConfig config) {
        addOutputInfo("pathQuery", widget.getPathQuery().toJson());
        addOutputInfo("pathConstraint", widget.getPathConstraint());
        addOutputInfo("pathQueryForMatches", widget.getPathQueryForMatches().toJson());
    }

    protected WidgetResultProcessor getProcessor() {
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
