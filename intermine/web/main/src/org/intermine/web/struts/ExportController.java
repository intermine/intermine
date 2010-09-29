package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.config.TableExportConfig;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.http.TableHttpExporter;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

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
    @Override
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        WebConfig webConfig = SessionMethods.getWebConfig(request);

        String  tableName = (String) request.getAttribute("tableName");
        PagedTable pt = SessionMethods.getResultsTable(session, tableName);

        if (pt == null) {
            LOG.error("PagedTable " + request.getParameter("table") + " is null");
            return null;
        }

        Map<String, TableExportConfig> allExporters = webConfig.getTableExportConfigs();
        Map<String, Map<String, String>> usableExporters
            = new HashMap<String, Map<String, String>>();

        for (Iterator<String> i = allExporters.keySet().iterator(); i.hasNext(); ) {
            String exporterId = i.next();
            TableExportConfig tableExportConfig = allExporters.get(exporterId);
            TableHttpExporter tableExporter =
                (TableHttpExporter) Class.forName(tableExportConfig.getClassName()).newInstance();

            boolean canExport = false;
            try {
                canExport = tableExporter.canExport(pt);
            } catch (Exception e) {
                LOG.error("Caught an error running canExport() for: " + exporterId + ". " + e);
            }
            if (canExport) {
                // parameters to pass via the URL to the exportOptions page
                Map<String, String> config = new HashMap<String, String>();
                config.put("id", tableExportConfig.getId());
                config.put("className", tableExportConfig.getClassName());
                usableExporters.put(exporterId, config);
            } else {
                usableExporters.put(exporterId, null);
            }
        }
        request.setAttribute("exporters", usableExporters);

        // TODO A HACK for Galaxy export
        // Allow to export public list
        // Private Bag is not support so far, Galaxy can not fetch data by the query, fix me
        PathQuery query = pt.getWebTable().getPathQuery();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        String isUserBag = "false";
        Set<String> bagNames = query.getBagNames();
        if (!bagNames.isEmpty()) {
            // if the bags are all public, export to Galaxy is allowed
            if (!im.getBagManager().getGlobalBags().isEmpty()
                    && im.getBagManager().getGlobalBags().keySet()
                            .containsAll(bagNames)) {
                isUserBag = "false";
            } else {
                isUserBag = "true";
            }
        }
        request.setAttribute("isUserBag", isUserBag);

        /* if (tableName.startsWith("bag")) {
            String bagName = tableName.substring(4, tableName.length());
            LOG.info("bagName is - " + bagName);
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            if (im.getBagManager().getGlobalBag(bagName) == null) {
                LOG.info("bag is - " + im.getBagManager().getGlobalBag(bagName));
                request.setAttribute("isPubBag", "false");
            }
        } */

        return null;
    }
}
