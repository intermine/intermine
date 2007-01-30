package org.intermine.web.bag;

/* 
 * Copyright (C) 2002-2005 FlyMine
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
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * Controller for the bagUploadConfirm
 * @author Kim Rutherford
 */
public class BagUploadConfirmController extends TilesAction
{
    /**
     * Set up the bagUploadConfirm page.
     * @see TilesAction#execute(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)
     */
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        BagQueryResult bagQueryResult = (BagQueryResult) session.getAttribute("bagQueryResult");
        request.setAttribute("matches", bagQueryResult.getMatches());
        Map issues = bagQueryResult.getIssues();
        request.setAttribute("issues", issues);
        request.setAttribute("unresolved", bagQueryResult.getUnresolved());

        // get all of the "low quality" matches ie. those found by queries other than matching 
        // class keys
        Map lowQualityMatches = new LinkedHashMap();
        Map otherMatchMap = (Map) bagQueryResult.getIssues().get(BagQueryResult.OTHER);
        if (otherMatchMap != null) {
            Iterator otherMatchesIter = otherMatchMap.values().iterator();
            while (otherMatchesIter.hasNext()) {
                Map inputToObjectsMap = (Map) otherMatchesIter.next();
                lowQualityMatches.putAll(inputToObjectsMap);
            }
        }
        request.setAttribute("lowQualityMatches", lowQualityMatches);
        
        // find all input strings that match more than one object
        Map duplicates = new LinkedHashMap();
        Map duplicateMap = (Map) bagQueryResult.getIssues().get(BagQueryResult.DUPLICATE);
        if (duplicateMap != null) {
            Iterator duplicateMapIter = duplicateMap.values().iterator();
            while (duplicateMapIter.hasNext()) {
                Map inputToObjectsMap = (Map) duplicateMapIter.next();
                duplicates.putAll(inputToObjectsMap);
            }
        }
        request.setAttribute("duplicates", duplicates);

        // make a List of [input string, ConvertedObjectPair]
        List convertedObjects = new ArrayList();
        Map convertedMap = (Map) bagQueryResult.getIssues().get(BagQueryResult.TYPE_CONVERTED);
        if (convertedMap != null) {
            Iterator convertedMapIter = convertedMap.values().iterator();
            while (convertedMapIter.hasNext()) {
                Map inputToObjectsMap = (Map) convertedMapIter.next();
                Iterator inputToObjectsMapIter = inputToObjectsMap.entrySet().iterator();
                Entry entry = (Entry) inputToObjectsMapIter.next();
                String input = (String) entry.getKey();
                List pairs = (List) entry.getValue();
                Iterator pairIter = pairs.iterator();
                while (pairIter.hasNext()) {
                    List row = new ArrayList();
                    row.add(input);
                    ConvertedObjectPair pair = (ConvertedObjectPair) pairIter.next();
                    row.add(pair.getOldObject());
                    row.add(pair.getNewObject());
                    convertedObjects.add(pair);
                }
            }
        }
        request.setAttribute("convertedObjects", convertedObjects);

        // create a string containing the ids of the high-quality matches
        StringBuffer matchesStringBuffer = new StringBuffer();
        BagUploadConfirmForm bagUploadConfirmForm = ((BagUploadConfirmForm) form);
        Map matches = bagQueryResult.getMatches();
        // matches will be null if we get here if the form.validate() method fails
        if (matches != null) {
            Iterator matchIDIter = matches.keySet().iterator();
            while (matchIDIter.hasNext()) {
                matchesStringBuffer.append(matchIDIter.next()).append(' ');
            }
            bagUploadConfirmForm.setMatchIDs(matchesStringBuffer.toString().trim());
        }
        if (request.getAttribute("bagType") != null) {
            bagUploadConfirmForm.setBagType((String) request.getAttribute("bagType"));
        }
        String trimmedIds = bagUploadConfirmForm.getMatchIDs().trim();
        int matchCount;
        if (trimmedIds.length() > 0) {
            int spaceCount = StringUtils.countMatches(trimmedIds, " ");
            matchCount = spaceCount + 1;
        } else {
            matchCount = 0;
        }
        request.setAttribute("matchCount", new Integer(matchCount));
        return null;
    }
}
