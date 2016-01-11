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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.ConvertedObjectPair;
import org.intermine.metadata.Model;
import org.intermine.util.DynamicUtil;
import org.intermine.metadata.TypeUtil;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.results.BagUploadConfirmInlineResultsTable;
import org.intermine.web.logic.results.BagUploadConfirmInlineResultsTableRow;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for the bagUploadConfirmIssue tile.
 * @author Kim Rutherford
 * @author Xavier Watkins
 * @author Radek Stepan
 */
public class BagUploadConfirmIssueController extends TilesAction
{
    /**
     * Initialise attributes for the bagUploadConfirmIssue.
     * {@inheritDoc}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" }) // Written by someone with fear of generics??
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        Map issuesMap = (Map) context.getAttribute("issueMap");

        // Make a Map from identifier to a List of rows for display.  Each row will contain
        // information about one object.  The row List will contain (first) the class name, then
        // a ResultElement object for each field to display.
        Map orderedIssuesMap = new LinkedHashMap(issuesMap);

        // a map from identifiers to indexes into objectList (and hence into the InlineResultsTable)
        Map identifierResultElementMap = new LinkedHashMap();

        // a map from identifier to initial type (for converted identifiers)
        Map initialTypeMap = new HashMap();

        List objectList = new ArrayList();

        int objectListIndex = 0;
        Iterator identifierIter = orderedIssuesMap.keySet().iterator();
        while (identifierIter.hasNext()) {
            String identifier = (String) identifierIter.next();
            identifierResultElementMap.put(identifier, new ArrayList());
            List objectListPerIdentifierMap = (List) orderedIssuesMap.get(identifier);
            for (int objIndex = 0; objIndex < objectListPerIdentifierMap.size(); objIndex++) {
                Object obj = objectListPerIdentifierMap.get(objIndex);
                if (obj instanceof ConvertedObjectPair) {
                    ConvertedObjectPair pair = (ConvertedObjectPair) obj;
                    objectList.add(pair.getNewObject());
                    if (initialTypeMap.get(identifier) == null) {
                        initialTypeMap.put(identifier, TypeUtil.unqualifiedName(DynamicUtil
                                .getSimpleClassName(pair.getOldObject().getClass())));
                    }
                } else {
                    objectList.add(obj);
                }
                List objectListForIdentifierList =
                    (List) identifierResultElementMap.get(identifier);
                objectListForIdentifierList.add(new Integer(objectListIndex));
                objectListIndex++;
            }
        }

        Model model = im.getModel();
        WebConfig webConfig = SessionMethods.getWebConfig(request);

        // create a BagUploadConfirmInlineResultsTable which is a special case of InlineResultsTable
        //  that uses BagUploadConfirmInlineResultsTableRow objects which is a special case of
        //  InlineResultsTableRow containing methods to set/get identifier & rowspan
        BagUploadConfirmInlineResultsTable table =
            new BagUploadConfirmInlineResultsTable(
                    objectList, model, webConfig, im.getClassKeys(), -1, true, null);

        // map additional matches onto the table
        table = mapResultTableOnAdditionalMatches(table, identifierResultElementMap);

        context.putAttribute("table", table);
        context.putAttribute("initialTypeMap", initialTypeMap);
        return null;
    }

    /**
     * Map additional matches onto the table of result elements
     *
     * @param table
     * @param resultElementMap
     * @return
     */
    @SuppressWarnings("rawtypes")
    private BagUploadConfirmInlineResultsTable mapResultTableOnAdditionalMatches(
                BagUploadConfirmInlineResultsTable table,
                Map resultElementMap) {
        // table rows
        List<Object> tableRows = table.getResultElementRows();

        // traverse map
        for (Object identifierKey : resultElementMap.keySet()) {
            // fetch the list of row numbers that correspond to the identifier
            List value = (List) resultElementMap.get(identifierKey);
            Boolean first = true;
            for (Object rowNumber : value) {
                // fetch the actual row in the table
                BagUploadConfirmInlineResultsTableRow tableRow =
                    (BagUploadConfirmInlineResultsTableRow) tableRows.get((Integer) rowNumber);
                // set the new values
                if (first) {
                    tableRow.setRowSpan(value.size());
                    tableRow.setShowIdentifier(true);
                }
                tableRow.setIdentifier((String) identifierKey);
                // save the row back
                tableRows.set((Integer) rowNumber, tableRow);
                // switch
                first = false;
            }
        }

        return table;
    }

}
