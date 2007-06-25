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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.path.Path;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.BagQueryResult;
import org.intermine.web.logic.query.OrderBy;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.results.Column;
import org.intermine.web.logic.results.PageOutOfRangeException;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateQuery;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
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
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
                                 ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        String pageStr = request.getParameter("page");
        String sizeStr = request.getParameter("size");
        String trail = request.getParameter("trail");
        
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        if (query != null) {
            HashMap<String, String> sortOrderMap = setSortOrderMap(query);        
            request.setAttribute("sortOrderMap", sortOrderMap);
        }
                
        request.setAttribute("trail", trail);
        
        SaveBagForm bagForm = (SaveBagForm) session.getAttribute("saveBagForm");
        if (bagForm != null) {
            bagForm.reset(mapping, request);
        }
        
        PagedTable pt = SessionMethods.getResultsTable(session, request.getParameter("table"));
        if (pt == null) {
            LOG.error("PagedTable for " + request.getParameter("table") + " is null");
            return null;
        }
        request.setAttribute("resultsTable", pt);
        if (pt.getAllRows().getPathToBagQueryResult() != null) {
            Map<String, BagQueryResult> pathToBagQueryResult = pt.getAllRows()
                .getPathToBagQueryResult();
            Map<String, DisplayLookup> lookupResults = new HashMap<String, DisplayLookup>();
            for (Map.Entry<String, BagQueryResult> entry : pathToBagQueryResult.entrySet()) {
                String path = entry.getKey();
                String type = query.getNode(path).getType();
                BagQueryResult bqr = entry.getValue();
                int matches = bqr.getMatchAndIssueIds().size();
                Set<String> unresolved = bqr.getUnresolved().keySet();
                Set<String> duplicates = new HashSet<String>();
                Set<String> lowQuality = new HashSet<String>();
                Set<String> translated = new HashSet<String>();
                Map<String, List> wildcards = new HashMap<String, List>();
                Map<String, Map<String, List>> duplicateMap = bqr.getIssues().get(BagQueryResult
                        .DUPLICATE);
                if (duplicateMap != null) {
                    for (Map.Entry<String, Map<String, List>> queries : duplicateMap.entrySet()) {
                        duplicates.addAll(queries.getValue().keySet());
                    }
                }
                Map<String, Map<String, List>> translatedMap = bqr.getIssues().get(BagQueryResult
                        .TYPE_CONVERTED);
                if (translatedMap != null) {
                    for (Map.Entry<String, Map<String, List>> queries : translatedMap.entrySet()) {
                        translated.addAll(queries.getValue().keySet());
                    }
                }
                Map<String, Map<String, List>> lowQualityMap = bqr.getIssues().get(BagQueryResult
                        .OTHER);
                if (lowQualityMap != null) {
                    for (Map.Entry<String, Map<String, List>> queries : lowQualityMap.entrySet()) {
                        lowQuality.addAll(queries.getValue().keySet());
                    }
                }
                Map<String, Map<String, List>> wildcardMap = bqr.getIssues().get(BagQueryResult
                                                                  .WILDCARD);
                if (wildcardMap != null) {
                    for (Map.Entry<String, Map<String, List>> queries : wildcardMap.entrySet()) {
                        wildcards.putAll(queries.getValue());
                    }
                }
                
                lookupResults.put(path, new DisplayLookup(type, matches, unresolved, duplicates,
                            translated, lowQuality, wildcards));
            }
            request.setAttribute("lookupResults", lookupResults);
        } else {
            request.setAttribute("lookupResults", Collections.EMPTY_MAP);
        }

        if (session.getAttribute(Constants.QUERY) != null) {
            if (session.getAttribute(Constants.QUERY) instanceof TemplateQuery) {
                request.setAttribute("templateQuery", session.getAttribute(Constants.QUERY));
            }
        }
        
        int page = (pageStr == null ? 0 : Integer.parseInt(pageStr));
        int size = (sizeStr == null ? pt.getPageSize() : Integer.parseInt(sizeStr));
        
        try {
            pt.setPageAndPageSize(page, size);
        } catch (PageOutOfRangeException e) {
            ActionMessages actionMessages = getErrors(request);
            actionMessages.add(ActionMessages.GLOBAL_MESSAGE,
                               new ActionMessage("results.maxoffsetreached"));
            saveErrors(request, actionMessages);
        }
        
        List<Column> columns = pt.getColumns();
        
        // a Map from column index to List of column indexes - if any element in a column is
        // selected then all the columns in the coresponding list should be disabled
        Map<String, List<String>> columnsToDisableMap = new HashMap<String, List<String>>();

        /*
         * code to make a columnsToDisableMap that disables only those columns that have a type
         * that isn't a sub-type of the selected column
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            Column column = columns.get(columnIndex);
            Path columnPath = column.getPath();
            if (columnPath != null) {
                Class columnEndType = columnPath.getLastClassDescriptor().getType();
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
                        } else {
                            Path otherColumnPath = otherColumn.getPath();
                            if (otherColumnPath != null) {
                                Class otherColumnEndType = 
                                    otherColumnPath.getLastClassDescriptor().getType();
                                if (otherColumnEndType != null) {
                                    if (!columnEndType.isAssignableFrom(otherColumnEndType)) {
                                        columnsToDisable.add("" + otherColumnIndex);
                                    }
                                }
                            }
                        }
                    }
                    columnsToDisableMap.put("" + columnIndex, columnsToDisable);
                }
            }
        }
        */
        
        // disable all other columns that have a different type
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            Column column = columns.get(columnIndex);
            Path columnPath = column.getPath();
            if (columnPath != null) {
                Class columnEndType = columnPath.getLastClassDescriptor().getType();
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
                        } else {
                            Path otherColumnPath = otherColumn.getPath();
                            if (otherColumnPath != null) {
                                Class otherColumnEndType = 
                                    otherColumnPath.getLastClassDescriptor().getType();
                                if (otherColumnEndType != null) {
                                    if (!columnEndType.equals(otherColumnEndType)) {
                                        columnsToDisable.add("" + otherColumnIndex);
                                    }
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
                    if (columnPath.getElements().size() > 0
                        && otherColumnPath.getElements().size() > 0
                        && columnPath.getPrefix().equals(otherColumnPath.getPrefix())) {
                        columnsToHighlight.add("" + otherColumnIndex);
                    }
                }

                columnsToHighlightMap.put("" + columnIndex, columnsToHighlight);
            }
        }
        
        request.setAttribute("columnsToHighlight", jsonWriter.write(columnsToHighlightMap));
        request.setAttribute("pathQuery", pt.getWebTable().getPathQuery());
        request.setAttribute("table", request.getParameter("table"));

        return null;
    }
    
    private HashMap<String, String> setSortOrderMap(PathQuery q) {
        HashMap<String, String> mappy = new HashMap<String, String>();
        String sortBy, direction = null;
        List<OrderBy> sortOrderList = q.getSortOrder();
        List<String> selectList = q.getViewStrings();
        if (sortOrderList.isEmpty()) {
            // do something if nothing selected
            sortBy = selectList.get(0);
            direction = "asc";
        } else {
            sortBy = sortOrderList.get(0).getField().toStringNoConstraints();
            direction = sortOrderList.get(0).getDirection();
        }
        
        // loop through query and populate map
        for (String s:  selectList) {
            if (s.equals(sortBy)) {
                mappy.put(s, direction);
            } else {
                mappy.put(s, null);                
            }
        }
            
        return mappy;
    }
}
