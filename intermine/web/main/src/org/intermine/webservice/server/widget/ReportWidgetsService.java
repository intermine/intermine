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
import org.intermine.web.logic.config.ReportWidgetConfig; // ReportWidgetConfig

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

public class ReportWidgetsService extends WidgetService
{

    public ReportWidgetsService(InterMineAPI im) {
        super(im);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void execute() throws Exception {
        // Fetch the config.
        for (ReportWidgetConfig config : InterMineContext.getWebConfig().getReportWidgetConfigs()) {
            addOutputInfo("cfg", config.getId());
        }

        // Get the request parameters.
        String id = request.getParameter("id");
        String callback = request.getParameter("callback");

        // Form the output.
        addOutputInfo("id", id);
        addOutputInfo("callback", callback);
    }

    @Override
    protected WidgetResultProcessor getProcessor() {
        if (formatIsJSON()) {
            return TableJSONProcessor.instance();
        } else if (formatIsXML()) {
            return TableXMLProcessor.instance();
        } else {
            return FlatFileWidgetResultProcessor.instance();
        }
    }

}
