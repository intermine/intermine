package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathQuery;
import org.intermine.metadata.StringUtil;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.http.TableExporterFactory;
import org.intermine.web.logic.export.http.TableHttpExporter;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for exportOptions.tile
 * @author Kim Rutherford
 */
@SuppressWarnings("deprecation")
public class ExportOptionsController extends TilesAction
{
    private static final Logger LOG = Logger.getLogger(ExportOptionsController.class);

    /**
     * Set up the exportOptions tile.
     *
     * @param context The Tiles ComponentContext
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     */
    @Override
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {

        HttpSession session = request.getSession();
        String type = request.getParameter("type");
        String table = request.getParameter("table");
        PagedTable pt = SessionMethods.getResultsTable(session, table);
        if (pt == null) {
            LOG.error("PagedTable for " + table + " is null");
            return null;
        }

        WebConfig webConfig = SessionMethods.getWebConfig(request);
        TableExporterFactory factory = new TableExporterFactory(webConfig);

        try {
            TableHttpExporter exporter = factory.getExporter(type);
            List<Path> initialPaths = exporter.getInitialExportPaths(pt);
            Map<String, String> pathsMap = new LinkedHashMap<String, String>();
            PathQuery query = pt.getWebTable().getPathQuery();
            for (Path path : initialPaths) {
                String pathString = path.toStringNoConstraints();
                String title = query.getGeneratedPathDescription(pathString);
                title = WebUtil.formatColumnName(title);
                pathsMap.put(pathString, title);
            }

            request.setAttribute("pathsMap", pathsMap);
            String pathStrings = StringUtil.join(pathsMap.keySet(), " ");
            request.setAttribute("pathsString", pathStrings);
        } catch (Exception e) {
            LOG.error("Exception", e);
            SessionMethods.recordError("An internal error has occured while creating the "
                                       + "export options page: " + e.getMessage(), session);
        }

        request.setAttribute("table", table);
        request.setAttribute("type", type);
        return null;
    }
}
