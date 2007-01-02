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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.Constants;
import org.intermine.web.results.ResultElement;

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
 * Controller for the bagUploadConfirmDuplicates tile.
 * @author Kim Rutherford
 */
public class BagUploadConfirmDuplicatesController extends TilesAction
{
    /**
     * Initialise attrobutes for the bagUploadConfirmDuplicates.
     * @see TilesAction#execute
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        Map duplicates = (Map) context.getAttribute("duplicates");
        Map resultElementMap = makeResultElementMap(request, duplicates);
        context.putAttribute("resultElementMap", resultElementMap);
        return null;
    }

    private static Map makeResultElementMap(HttpServletRequest request, Map duplicates) {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Map resultElementMap = new HashMap();
        Iterator duplicatesIter = duplicates.entrySet().iterator();
        while (duplicatesIter.hasNext()) {
            Map.Entry entry = (Entry) duplicatesIter.next();
            String identifier = (String) entry.getKey();
            Set objects = (Set) entry.getValue();
            Set resultsElementList = new HashSet();
            resultElementMap.put(identifier, resultsElementList);
            Iterator objectsIter = objects.iterator();
            while (objectsIter.hasNext()) {
                InterMineObject imo = (InterMineObject) objectsIter.next();
                String bagType = (String) request.getAttribute("bagType");
                ResultElement resultElement =
                    new ResultElement(os, identifier, imo.getId(), bagType, true);
                resultsElementList.add(resultElement);
            }
        }
        return resultElementMap;
    }
}
