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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.intermine.model.InterMineObject;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.BagQueryResult;
import org.intermine.web.logic.bag.ConvertedObjectPair;

import javax.servlet.ServletContext;
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
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping, 
                                 ActionForm form, 
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response) 
    throws Exception {
        HttpSession session = request.getSession();
        BagQueryResult bagQueryResult = (BagQueryResult) session.getAttribute("bagQueryResult");
        request.setAttribute("matches", bagQueryResult.getMatches());
        Map<String, Map<String, Map<String, List>>> issues = bagQueryResult.getIssues();
        request.setAttribute("issues", issues);
        request.setAttribute("unresolved", bagQueryResult.getUnresolved());
        ServletContext servletContext = session.getServletContext();
        StringBuffer flattenedArray = new StringBuffer();
        
        // get all of the "low quality" matches ie. those found by queries other than matching 
        // class keys
        Map<String, ArrayList<String>> lowQualityMatches 
                                            = new LinkedHashMap<String, ArrayList<String>>();
        Map<String, Map<String, List>> otherMatchMap 
                                            = bagQueryResult.getIssues().get(BagQueryResult.OTHER);
        if (otherMatchMap != null) {
            Iterator otherMatchesIter = otherMatchMap.values().iterator();
            while (otherMatchesIter.hasNext()) {
                Map<String, ArrayList<String>> inputToObjectsMap = (Map) otherMatchesIter.next();
                lowQualityMatches.putAll(inputToObjectsMap);
            }
        }
        request.setAttribute("lowQualityMatches", lowQualityMatches);
        flattenedArray.append(setJSArray(lowQualityMatches, "lowQ"));
        
        // find all input strings that match more than one object
        Map<String, ArrayList<String>> duplicates = new LinkedHashMap<String, ArrayList<String>>();
        Map<String, Map<String, List>> duplicateMap 
                                    = bagQueryResult.getIssues().get(BagQueryResult.DUPLICATE);
        if (duplicateMap != null) {
            Iterator duplicateMapIter = duplicateMap.values().iterator();
            while (duplicateMapIter.hasNext()) {
                Map<String, ArrayList<String>> inputToObjectsMap 
                = (Map) duplicateMapIter.next();
                duplicates.putAll(inputToObjectsMap);
            }
        }
        request.setAttribute("duplicates", duplicates);
        flattenedArray.append(setJSArray(duplicates, "duplicate"));
        
        // make a List of [input string, ConvertedObjectPair]
        Map<String, ArrayList<String>> convertedObjects 
                                                = new LinkedHashMap<String, ArrayList<String>>();
        Map<String, Map<String, List>> convertedMap 
        = bagQueryResult.getIssues().get(BagQueryResult.TYPE_CONVERTED);
        if (convertedMap != null) {
            Iterator convertedMapIter = convertedMap.values().iterator();
            while (convertedMapIter.hasNext()) {
                Map<String, ArrayList<String>> inputToObjectsMap = (Map) convertedMapIter.next();
                convertedObjects.putAll(inputToObjectsMap);
            }
        }
        request.setAttribute("convertedObjects", convertedObjects);
        flattenedArray.append(setJSArray(convertedObjects, "converted"));
        
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
        // TODO put field name here.
        BagQueryConfig bagQueryConfig =
            (BagQueryConfig) servletContext.getAttribute(Constants.BAG_QUERY_CONFIG);
        String extraClassName = bagQueryConfig.getExtraConstraintClassName();
        bagUploadConfirmForm.setExtraFieldValue(TypeUtil.unqualifiedName(extraClassName));
        request.setAttribute("matchCount", new Integer(matchCount));
        request.setAttribute("jsArray", flattenedArray);
        return null;
    }
    
    // takes all the issues and puts them in a flattened array that the javascript can use for
    // the "add all" and "remove all" buttons
    private String setJSArray(Map<String, ArrayList<String>> issues, String issueType) {

        StringBuffer sb = new StringBuffer();
        Map<String, ArrayList<String>> orderedIssuesMap 
            = new LinkedHashMap<String, ArrayList<String>>(issues);

        // Make a Map from identifier to a List of rows for display.  Each row will contain
        // information about one object.  The row List will contain (first) the class name, then
        // a ResultElement object for each field to display.
        
        // a map from identifiers to indexes into objectList (and hence into the InlineResultsTable)
        Map<String, ArrayList<String>> identifierResultElementMap 
                                                   = new LinkedHashMap<String, ArrayList<String>>();

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
                          + identifier + "," + issueType + "|");                
                objectListIndex++;
            }
        }
     
        return sb.toString();
    }
    
}
