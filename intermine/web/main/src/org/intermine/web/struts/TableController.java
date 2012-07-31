package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
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
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.results.Column;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;
import org.stringtree.json.JSONWriter;

/**
 * Implementation of <strong>TilesAction</strong>. Sets up PagedTable
 * for table tile.
 *
 * @author Thomas Riley
 */
public class TableController extends TilesAction
{
    private static final Logger LOG = Logger.getLogger(TableController.class);

    /**
     * Set up table tile.
     *
     * @param context The Tiles ComponentContext
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if an error occurs
     */
    @Override
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        ServletContext servletContext = session.getServletContext();

        String pageStr = request.getParameter("page");
        String sizeStr = request.getParameter("size");
        String trail = request.getParameter("trail");

        request.setAttribute("trail", trail);

        SaveBagForm bagForm = (SaveBagForm) session.getAttribute("saveBagForm");
        if (bagForm != null) {
            bagForm.reset(mapping, request);
        }

        String table = request.getParameter("table");
        PagedTable pt = SessionMethods.getResultsTable(session, table);
        if (pt == null) {
            LOG.error("PagedTable for " + table + " is null");
            throw new NullPointerException("PagedTable for " + table + " is null");
        }

        PathQuery query = pt.getWebTable().getPathQuery();

        request.setAttribute("resultsTable", pt);
        if ((request.getAttribute("lookupResults") != null)) {
          //Do nothing
        } else if (pt.getAllRows().getPathToBagQueryResult() != null) {
            Map<String, BagQueryResult> pathToBagQueryResult = pt.getAllRows()
                .getPathToBagQueryResult();
            List<DisplayLookupMessageHandler> lookupResults =
                new ArrayList<DisplayLookupMessageHandler>();
            for (Map.Entry<String, BagQueryResult> entry : pathToBagQueryResult.entrySet()) {
                String path = entry.getKey();
                String type = TypeUtil.unqualifiedName(query.makePath(path).getEndType().getName());
                String extraValue = "";
                for (PathConstraint con : query.getConstraints().keySet()) {
                    if (con instanceof PathConstraintLookup && con.getPath().equals(path)) {
                        extraValue = ((PathConstraintLookup) con).getExtraValue();
                    }
                }
                BagQueryResult bqr = entry.getValue();
                Properties properties = SessionMethods.getWebProperties(servletContext);
                DisplayLookupMessageHandler.handleMessages(bqr, session, properties, type,
                                                           extraValue);
            }
            request.setAttribute("lookupResults", lookupResults);
        } else {
            request.setAttribute("lookupResults", Collections.EMPTY_MAP);
        }

        if (query != null) {
            if (query instanceof TemplateQuery) {
                request.setAttribute("templateQuery", query);
            }
        }

        int page = (pageStr == null ? 0 : Integer.parseInt(pageStr));

        int newPageSize;
        if (sizeStr != null) {
            newPageSize = Integer.parseInt(sizeStr);
        } else {
            if (session.getAttribute(Constants.RESULTS_TABLE_SIZE) != null) {
                newPageSize = ((Integer)
                                session.getAttribute(Constants.RESULTS_TABLE_SIZE)).intValue();
            } else {
                newPageSize = pt.getPageSize();
            }
        }
        pt.setPageAndPageSize(page, newPageSize);
        session.setAttribute(Constants.RESULTS_TABLE_SIZE, Integer.valueOf(newPageSize));

        List<Column> columns = pt.getColumns();

        // a Map from column index to List of column indexes - if any element in a column is
        // selected then all the columns in the coresponding list should be disabled
        Map<String, List<String>> columnsToDisableMap = new HashMap<String, List<String>>();

        // disable all other columns that have a different type
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            Column column = columns.get(columnIndex);
            Path columnPath = column.getPath();
            if (columnPath != null) {
                Class<?> columnEndType = columnPath.getLastClassDescriptor().getType();
                if (columnEndType != null) {
                    List<String> columnsToDisable = new ArrayList<String>();
                    // find columns that should be disabled if an object from this column is
                    // selected
                    for (int otherColumnIndex = 0;
                        otherColumnIndex < columns.size();
                        otherColumnIndex++) {
                        Column otherColumn = columns.get(otherColumnIndex);
                        if (otherColumn.equals(column) || !otherColumn.isSelectable()) {
                            continue;
                        }
                        Path otherColumnPath = otherColumn.getPath();
                        if (otherColumnPath != null) {
                            Class<?> otherColumnEndType =
                                otherColumnPath.getLastClassDescriptor().getType();
                            if (otherColumnEndType != null) {
                                if (!columnEndType.equals(otherColumnEndType)) {
                                    columnsToDisable.add("" + otherColumnIndex);
                                }
                            }
                        }

                    }
                    columnsToDisableMap.put("" + columnIndex, columnsToDisable);
                }
            }
        }

        JSONWriter jsonWriter = new JSONWriter();
        request.setAttribute("columnsToDisable", jsonWriter.write(columnsToDisableMap));

        // a Map from column index to List of column indexes - if an element in row R and column C
        // is selected then the elements in the columns in the coresponding list should be
        // highlighted if they are in row R (because they are fields from the object)
        Map<String, List<String>> columnsToHighlightMap = new HashMap<String, List<String>>();

        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            Column column = columns.get(columnIndex);
            Path columnPath = column.getPath();
            if (columnPath != null) {
                List<String> columnsToHighlight = new ArrayList<String>();
                for (int otherColumnIndex = 0;
                     otherColumnIndex < columns.size();
                     otherColumnIndex++) {
                    Column otherColumn = columns.get(otherColumnIndex);
                    Path otherColumnPath = otherColumn.getPath();
                    if (!columnPath.isRootPath()
                        && !otherColumnPath.isRootPath()
                        && columnPath.getPrefix().equals(otherColumnPath.getPrefix())) {
                        columnsToHighlight.add("" + otherColumnIndex);
                    }
                }

                columnsToHighlightMap.put("" + columnIndex, columnsToHighlight);
            }
        }

        request.setAttribute("columnsToHighlight", jsonWriter.write(columnsToHighlightMap));
        request.setAttribute("query", pt.getWebTable().getPathQuery());
        request.setAttribute("table", table);

        Map<Path, String> pathNames = new HashMap<Path, String> ();
        for (Column column : columns) {
            Path path = column.getPath();
            if (path != null) {
                pathNames.put(path, path.toStringNoConstraints());
            }
        }
        request.setAttribute("pathNames", pathNames);

        Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();
        ObjectStore os = im.getObjectStore();
        request.setAttribute("firstSelectedFields", pt.getFirstSelectedFields(os, classKeys));

        return null;
    }
}
