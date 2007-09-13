package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.intermine.web.logic.Constants;
import org.intermine.web.logic.config.TableExportConfig;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.TableExporter;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

import org.apache.log4j.Logger;

/**
 * Controller to initialise for the export.tile
 *
 * @author Kim Rutherford
 */

public class ExportController extends TilesAction
{
    private static final Logger LOG = Logger.getLogger(ExportController.class);

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();

        WebConfig wc = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);

        PagedTable pt = SessionMethods.getResultsTable(session, request.getParameter("table"));

        if (pt == null) {
            LOG.error("PagedTable " + request.getParameter("table") + " is null");
            return null;
        }
        
        Map allExporters = wc.getTableExportConfigs();
        Map usableExporters = new HashMap();

        for (Iterator i = allExporters.keySet().iterator(); i.hasNext(); ) {
            String exporterId = (String) i.next();
            TableExportConfig tableExportConfig = (TableExportConfig) allExporters.get(exporterId);

            TableExporter tableExporter =
                (TableExporter) Class.forName(tableExportConfig.getClassName()).newInstance();

            boolean canExport = false;
            try {
                canExport = tableExporter.canExport(pt);
            } catch (Exception e) {
                LOG.error("Caught an error running canExport() for: " + exporterId + ". " + e);
            }
            if (canExport) {
                usableExporters.put(exporterId, tableExportConfig);
            } else {
                usableExporters.put(exporterId, null);
            }
        }

        request.setAttribute("exporters", usableExporters);

        return null;
    }
}
