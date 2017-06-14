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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.OrderElement;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.SwitchOffAbility;
import org.intermine.template.TemplateQuery;
import org.intermine.metadata.StringUtil;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.ExportException;
import org.intermine.web.logic.export.http.TableExporterFactory;
import org.intermine.web.logic.export.http.TableHttpExporter;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Implementation of <strong>Action</strong> that allows the user to export a
 * PagedTable to a file.
 *
 * @author Kim Rutherford
 * @author Jakub Kulaviak
 */
@SuppressWarnings("deprecation")
public class TableExportAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(TableExportAction.class);

    private static final String ERROR_MSG = "Export failed.";

    /**
     * Method called to export a PagedTable object.  Uses the type request parameter to choose the
     * export method.
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws an exception
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        TableExportForm tef = (TableExportForm) form;

        if (tef.getPathsString().trim().isEmpty()) {
            response.setContentType("text/plain");
            OutputStream out = response.getOutputStream();
            PrintWriter writer = new PrintWriter(out);
            writer.println(ERROR_MSG + " No columns added.");
            writer.flush();
            writer.close();
            return null;
        }

        String type = tef.getType();
        String table = tef.getTable();
        String pathsString = tef.getPathsString();
        PagedTable pt = null;
        try {
            WebConfig webConfig = SessionMethods.getWebConfig(request);
            TableExporterFactory factory = new TableExporterFactory(webConfig);
            TableHttpExporter exporter = factory.getExporter(type);
            pt = SessionMethods.getResultsTable(session, table);

            checkTable(pt);

            // Create a union of old view (in pathquery) and new view (user selection)
            PathQuery pathQuery = pt.getPathQuery();
            List<String> oldPathList = pathQuery.getView();
            List<String> newPathList;
            try {
                if (!pathsString.contains("[]=")) { // BED format case
                    newPathList = Arrays.asList(pathsString.split(" "));
                } else {
                    newPathList = new ArrayList<String>(StringUtil
                            .serializedSortOrderToMap(pathsString).keySet());
                }
            } catch (RuntimeException e) {
                throw new RuntimeException("Error while converting " + pathsString, e);
            }

            Set<String> unionPathSet = new HashSet<String>();
            unionPathSet.addAll(oldPathList);
            unionPathSet.addAll(newPathList);

            // make a collection of Path
            List<Path> newPatCollection = new ArrayList<Path>();
            for (String pathString : newPathList) {
                newPatCollection.add(pathQuery.makePath(pathString));
            }

            List<Path> unionPathCollection = new ArrayList<Path>();
            for (String pathString : unionPathSet) {
                unionPathCollection.add(pathQuery.makePath(pathString));
            }

            PagedTable newPt = reorderPagedTable(pathQuery, unionPathSet, request);
            exporter.export(newPt, request, response, tef, unionPathCollection, newPatCollection);

            // If null is returned then no forwarding is performed and
            // to the output is not flushed any jsp output, so user
            // will get only required export data
            return null;
        } catch (RuntimeException e) {
            return processException(mapping, request, response, e);
        }
    }

    private void checkTable(PagedTable pt) {
        if (pt == null) {
            throw new ExportException("Export failed: result is null.");
        }

        if (pt.getExactSize() > pt.getMaxRetrievableIndex()) {
            throw new ExportException("Result is too big for export. "
                    + "Table for export can have at the most "
                    + pt.getMaxRetrievableIndex() + " rows.");
        }
    }

    /**
     * Copy the old PagedTable and make one with the new paths
     */
    private PagedTable reorderPagedTable(PathQuery pathQuery, Collection<String> newViews,
            HttpServletRequest request) throws ObjectStoreException {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        PathQuery newPathQuery = new PathQuery(pathQuery);
        if (pathQuery instanceof TemplateQuery) {
            TemplateQuery templateQuery = (TemplateQuery) pathQuery.clone();
            Map<PathConstraint, SwitchOffAbility>  constraintSwitchOffAbilityMap =
                                                   templateQuery.getConstraintSwitchOffAbility();
            for (Map.Entry<PathConstraint, SwitchOffAbility> entry
                : constraintSwitchOffAbilityMap.entrySet()) {
                if (entry.getValue().compareTo(SwitchOffAbility.OFF) == 0) {
                    newPathQuery.removeConstraint(entry.getKey());
                }
            }
        }
        newPathQuery.clearView();
        newPathQuery.addViews(newViews);

        // all order by paths should also be in the view, remove any that now aren't
        for (OrderElement orderElement : newPathQuery.getOrderBy()) {
            if (!newPathQuery.getView().contains(orderElement.getOrderPath())) {
                newPathQuery.removeOrderBy(orderElement.getOrderPath());
            }
        }
        for (Entry<String, Boolean> outerEntry : newPathQuery.getOuterMap().entrySet()) {
            if (outerEntry.getValue().equals(Boolean.TRUE)) {
                String joinPath = outerEntry.getKey();
                boolean outherJoinStatusRelevant = false;
                for (String viewPath : newPathQuery.getView()) {
                    if (viewPath.startsWith(joinPath)) {
                        outherJoinStatusRelevant = true;
                        break;
                    }
                }
                if (!outherJoinStatusRelevant) {
                    newPathQuery.setOuterJoinStatus(joinPath, null);
                }
            }
        }
        Profile profile = SessionMethods.getProfile(session);
        WebResultsExecutor executor = im.getWebResultsExecutor(profile);
        return new PagedTable(executor.execute(newPathQuery));
    }

    private ActionForward processException(ActionMapping mapping,
            HttpServletRequest request, HttpServletResponse response,
            RuntimeException e) throws IOException {
        LOG.error("Export failed.", e);
        String msg = null;
        if (e instanceof ExportException) {
            msg = e.getMessage();
            if (msg == null || msg.length() == 0) {
                msg = ERROR_MSG;
            }
        } else {
            msg = ERROR_MSG;
        }
        // If response wasn't commited then we can display error, else
        // there is only possibility to append error message to the end.
        if (!response.isCommitted()) {
            PrintWriter writer = null;
            response.reset();
            try {
                // Tricky. This is called to verify, that if the error will be forwarded to jsp
                // page that it will be displayed. If getWriter() method throws exception there it
                // means that getOutputStream() method already was called and displaying error
                // message in proper jsp error page would fail.
                response.getWriter();
                recordError(new ActionMessage("errors.export.displayonlyparameters", msg), request);
                return mapping.findForward("error");
            } catch (IllegalStateException ex) {
                OutputStream out = response.getOutputStream();
                writer = new PrintWriter(out);
                writer.println(msg);
                writer.flush();
            }
        } else {
            // Attempt to write error to output stream where data was already sent.
            // If there are textual data user will see the error else it will
            // makes binary file  probably unreadable and so the user knows
            // that something is wrong.
            // At this moment only excel format exports binary output and
            // it is flushed at the end - that's why an error can only happen before
            // response is  commited and user will see the error and won't get the
            // excel file.
            try {
                PrintWriter writer = response.getWriter();
                writer.println(msg);
                writer.flush();
            } catch (IllegalStateException ex) {
                OutputStream out = response.getOutputStream();
                PrintWriter writer = new PrintWriter(out);
                writer.println(msg);
                writer.flush();
            }
        }
        return null;
    }

    /**
     * @param request request
     * @param session session
     * @return PagedTable from session
     */
    protected PagedTable getPagedTable(HttpServletRequest request, HttpSession session) {
        return SessionMethods.getResultsTable(session, request.getParameter("table"));
    }
}
