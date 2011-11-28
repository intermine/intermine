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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.session.SessionMethods;
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
    private class EnrichmentXMLFormatter extends XMLFormatter {

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
     * @param request request
     * @param response response
     * @throws Exception an error has occurred
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void execute() throws Exception {
        WidgetsServiceInput input = getInput();
        Profile profile = SessionMethods.getProfile(request.getSession());

        InterMineBag imBag = im.getBagManager().getUserOrGlobalBag(profile, input.getBagName());
        if (imBag == null) {
            throw new BadRequestException("You do not have access to a bag named" + input.getBagName());
        }
        addOutputInfo("type", imBag.getType());
        addOutputInfo("list", imBag.getName());
        addOutputInfo("requestedAt", new Date().toGMTString());

        WebConfig webConfig = SessionMethods.getWebConfig(request);
        WidgetConfig widgetConfig = webConfig.getWidgets().get(input.getWidgetId());

        if (widgetConfig == null || !(widgetConfig instanceof EnrichmentWidgetConfig)) {
            throw new ResourceNotFoundException("Could not find an enrichment widget called \"" + input.getWidgetId() + "\"");
        }

        EnrichmentWidget widget = null;
        try {
            widget = (EnrichmentWidget) widgetConfig.getWidget(imBag, im.getObjectStore(), input.getExtraAttributes());
        } catch (ClassCastException e) {
            throw new ResourceNotFoundException("Could not find an enrichment widget called \"" + input.getWidgetId() + "\"");
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
