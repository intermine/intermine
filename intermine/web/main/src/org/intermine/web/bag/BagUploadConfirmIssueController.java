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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;
import org.intermine.web.Constants;
import org.intermine.web.config.WebConfig;
import org.intermine.web.results.InlineResultsTable;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * Controller for the bagUploadConfirmIssue tile.
 * @author Kim Rutherford
 * @author Xavier Watkins
 */
public class BagUploadConfirmIssueController extends TilesAction
{
    /**
     * Initialise attributes for the bagUploadConfirmIssue.
     * @see TilesAction#execute
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        Map issuesMap = (Map) context.getAttribute("issueMap");
        
        // Make a Map from identifier to a List of rows for display.  Each row will contain
        // information about one object.  The row List will contain (first) the class name, then
        // a ResultElement object for each field to display.

        Map orderedIssuesMap = new LinkedHashMap(issuesMap);
        
        // a map from identifiers to indexes into objectList (and hence into the InlineResultsTable)
        Map identifierResultElementMap = new LinkedHashMap();
        
        List objectList = new ArrayList();
        
        int objectListIndex = 0;
        Iterator identifierIter = orderedIssuesMap.keySet().iterator();
        while (identifierIter.hasNext()) {
            String identifier = (String) identifierIter.next();
            identifierResultElementMap.put(identifier, new ArrayList());
            List objectListPerIdentifierMap = (List) orderedIssuesMap.get(identifier);
            for (int objIndex = 0; objIndex < objectListPerIdentifierMap.size(); objIndex++) {
                objectList.add(objectListPerIdentifierMap.get(objIndex));
                List objectListForIdentifierList = 
                    (List) identifierResultElementMap.get(identifier);
                objectListForIdentifierList.add(new Integer(objectListIndex));
                objectListIndex++;
            }
        }
        
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
        Map webPropertiesMap = (Map) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
        
        InlineResultsTable table = 
            new InlineResultsTable(objectList, os.getModel(), webConfig, webPropertiesMap, 
                                   classKeys, -1);
        
        identifierIter = identifierResultElementMap.keySet().iterator();
        while (identifierIter.hasNext()) {
            String identifier = (String) identifierIter.next();
            List objectForIdentifierList = (List) identifierResultElementMap.get(identifier);
            for (int i = 0; i < objectForIdentifierList.size(); i++) {
                Integer thisObjectListIndex = (Integer) objectForIdentifierList.get(i);
                List resultElementRow = 
                    new ArrayList(table.getResultElementRow(os, thisObjectListIndex.intValue()));
                Set cds = (Set) (table.getTypes().get(thisObjectListIndex.intValue()));
                ClassDescriptor classDesc = (ClassDescriptor) cds.iterator().next();
                String className = TypeUtil.unqualifiedName(classDesc.getName());
                resultElementRow.add(0, className);
                InterMineObject rowObject = 
                    (InterMineObject) table.getRowObjects().get(thisObjectListIndex.intValue());
                resultElementRow.add(rowObject.getId());
                objectForIdentifierList.set(i, resultElementRow);
            }
        }

        Map resultElementMap = identifierResultElementMap;
        context.putAttribute("resultElementMap", resultElementMap);
        context.putAttribute("columnNames", table.getColumnFullNames());
        return null;
    }
}