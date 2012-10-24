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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.bag.ConvertedObjectPair;
import org.intermine.model.InterMineObject;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for the bagUploadConfirm
 * @author Kim Rutherford
 */
public class BagUploadConfirmController extends TilesAction
{
    /**
     * Set up the bagUploadConfirm page.
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        String bagName = (String) request.getAttribute("newBagName");
        String bagQueryResultLabel = "bagQueryResult";
        if (bagName != null) {
            bagQueryResultLabel = bagQueryResultLabel + "_" + bagName;
        }

        BagQueryResult bagQueryResult = (BagQueryResult) session.getAttribute(bagQueryResultLabel);

        // MATCHES
        request.setAttribute("matches", bagQueryResult.getMatches());

        // NO MATCH
        request.setAttribute("unresolved", bagQueryResult.getUnresolved());

        Map<String, Map<String, Map<String, List>>> issues = bagQueryResult.getIssues();

        // TODO delete -- not ordering by type anymore.

        request.setAttribute("issues", issues);


        StringBuffer flattenedArray = new StringBuffer();

        // get all of the "low quality" matches ie. those found by queries other than matching
        // class keys
        Map<String, ArrayList<Object>> lowQualityMatches = new LinkedHashMap<String,
            ArrayList<Object>>();
        Map<String, Map<String, List>> otherMatchMap = bagQueryResult.getIssues()
            .get(BagQueryResult.OTHER);
        Set<Integer> matchesIds = bagQueryResult.getMatches().keySet();
        if (otherMatchMap != null) {
            Iterator otherMatchesIter = otherMatchMap.values().iterator();
            while (otherMatchesIter.hasNext()) {
                Map<String, ArrayList<Object>> inputToObjectsMap = (Map) otherMatchesIter.next();
                //before adding inputToObjectsMap to lowQualityMatches
                //we removed all object having the id already contained in the matchesIds
                Map<String, ArrayList<Object>> inputToObjectsMapUpdated = new LinkedHashMap<String,
                ArrayList<Object>>();
                for (String key : inputToObjectsMap.keySet()) {
                    ArrayList<Object> listObjects = inputToObjectsMap.get(key);
                    ArrayList<Object> listObjectsUpdated = new ArrayList<Object>();
                    for (Object obj : listObjects) {
                        InterMineObject intermineObj = (InterMineObject) obj;
                        if (matchesIds.isEmpty() || !matchesIds.contains(intermineObj.getId())) {
                            listObjectsUpdated.add(obj);
                        }
                    }
                    if (!listObjectsUpdated.isEmpty()) {
                        inputToObjectsMapUpdated.put(key, listObjects);
                    }
                }
                if (!inputToObjectsMapUpdated.isEmpty()) {
                    lowQualityMatches.putAll(inputToObjectsMapUpdated);
                }
            }
        }
        request.setAttribute("lowQualityMatches", lowQualityMatches);
        String flatLowQualityMatches = setJSArray(lowQualityMatches, "lowQ");
        flattenedArray.append(flatLowQualityMatches);
        request.setAttribute("flatLowQualityMatches", flatLowQualityMatches);

        // find all input strings that match more than one object
        Map<String, ArrayList<Object>> duplicates = new LinkedHashMap<String, ArrayList<Object>>();
        Map<String, Map<String, List>> duplicateMap = bagQueryResult.getIssues()
            .get(BagQueryResult.DUPLICATE);
        if (duplicateMap != null) {
            Iterator duplicateMapIter = duplicateMap.values().iterator();
            while (duplicateMapIter.hasNext()) {
                Map<String, ArrayList<Object>> inputToObjectsMap = (Map) duplicateMapIter.next();
                duplicates.putAll(inputToObjectsMap);
            }
        }
        request.setAttribute("duplicates", duplicates);
        String flatDuplicate = setJSArray(duplicates, "duplicate");
        flattenedArray.append(flatDuplicate);
        request.setAttribute("flatDuplicate", flatDuplicate);

        // make a List of [input string, ConvertedObjectPair]
        Map<String, ArrayList<Object>> convertedObjects
            = new LinkedHashMap<String, ArrayList<Object>>();
        Map<String, Map<String, List>> convertedMap = bagQueryResult.getIssues()
            .get(BagQueryResult.TYPE_CONVERTED);
        if (convertedMap != null) {
            Iterator convertedMapIter = convertedMap.values().iterator();
            while (convertedMapIter.hasNext()) {
                Map<String, ArrayList<Object>> inputToObjectsMap = (Map) convertedMapIter.next();
                convertedObjects.putAll(inputToObjectsMap);
            }
        }
        request.setAttribute("convertedObjects", convertedObjects);
        String flatConverted = setJSArray(convertedObjects, "converted");
        flattenedArray.append(flatConverted);
        request.setAttribute("flatConverted", flatConverted);

        // create a string containing the ids of the high-quality matches
        StringBuffer matchesStringBuffer = new StringBuffer();
        BagUploadConfirmForm bagUploadConfirmForm = ((BagUploadConfirmForm) form);
        Map matches = bagQueryResult.getMatches();
        // matches will be null if we get here if the form.validate() method fails
        int matchCount = 0;
        if (matches != null) {
            Iterator matchIDIter = matches.keySet().iterator();
            while (matchIDIter.hasNext()) {
                matchesStringBuffer.append(matchIDIter.next()).append(' ');
            }
            bagUploadConfirmForm.setMatchIDs(matchesStringBuffer.toString().trim());
            matchCount = matches.keySet().size();
        }
        if (request.getAttribute("bagType") != null) {
            bagUploadConfirmForm.setBagType((String) request.getAttribute("bagType"));
        }
        //String trimmedIds = bagUploadConfirmForm.getMatchIDs().trim();
        //if (trimmedIds.length() > 0) {
        //    int spaceCount = StringUtils.countMatches(trimmedIds, " ");
        //    matchCount = spaceCount + 1;
        //} else {
        //    matchCount = 0;
        //}
        // TODO put field name here.
        BagQueryConfig bagQueryConfig = im.getBagQueryConfig();
        String extraClassName = bagQueryConfig.getExtraConstraintClassName();
        bagUploadConfirmForm.setExtraFieldValue(TypeUtil.unqualifiedName(extraClassName));
        request.setAttribute("matchCount", new Integer(matchCount));
        request.setAttribute("jsArray", flattenedArray);
        if (bagName != null) {
            request.setAttribute("bagName", bagName);
        }

        return null;
    }

    // takes all the issues and puts them in a flattened array that the javascript can use for
    // the "add all" and "remove all" buttons
    private String setJSArray(Map<String, ArrayList<Object>> issues, String issueType) {

        StringBuffer sb = new StringBuffer();
        Map<String, ArrayList<Object>> orderedIssuesMap
            = new LinkedHashMap<String, ArrayList<Object>>(issues);

        // Make a Map from identifier to a List of rows for display.  Each row will contain
        // information about one object.  The row List will contain (first) the class name, then
        // a ResultElement object for each field to display.

        // a map from identifiers to indexes into objectList (and hence into the InlineResultsTable)
        Map<String, ArrayList<String>> identifierResultElementMap = new LinkedHashMap<String,
            ArrayList<String>>();

        int objectListIndex = 0;
        Iterator identifierIter = orderedIssuesMap.keySet().iterator();
        while (identifierIter.hasNext()) {
            String identifier = (String) identifierIter.next();
            identifierResultElementMap.put(identifier, new ArrayList<String>());
            List objectListPerIdentifierMap = orderedIssuesMap.get(identifier);
            for (int objIndex = 0; objIndex < objectListPerIdentifierMap.size(); objIndex++) {
                Object obj = objectListPerIdentifierMap.get(objIndex);
                InterMineObject o;
                if (obj instanceof ConvertedObjectPair) {
                    ConvertedObjectPair pair = (ConvertedObjectPair) obj;
                    o = pair.getNewObject();
                } else {
                    o = (InterMineObject) obj;
                }
                sb.append(o.getId() + "," + objectListIndex + ","
                        + identifier.replace("'", "\\'") + "," + issueType + "|");
                objectListIndex++;
            }
        }

        return sb.toString();
    }

}
