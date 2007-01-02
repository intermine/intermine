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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
        Map classDuplicates = makeClassDuplicatesMap(duplicates);
        context.putAttribute("classDuplicates", classDuplicates);
        return null;
    }


    /**
     * Given a Map of identifier to List of objects, create a Map from unqualified class name to
     * Map of identifiers to List of objects 
     * @param duplicates A Map from identifier to List of objects
     * @return a Map from unqualified class name to Map of identifiers to objects
     */
    protected static Map makeClassDuplicatesMap(Map duplicates) {
        Map classDuplicates = new HashMap();
        Iterator duplicatesIter = duplicates.entrySet().iterator();
        while (duplicatesIter.hasNext()) {
            Map.Entry entry = (Entry) duplicatesIter.next();
            String identifier = (String) entry.getKey();
            Set objects = (Set) entry.getValue();
            Iterator objectsIter = objects.iterator();
            while (objectsIter.hasNext()) {
                InterMineObject imo = (InterMineObject) objectsIter.next();
                addToMap(classDuplicates, identifier, imo);
            }
        }
        return classDuplicates;
    }


    private static void addToMap(Map classDuplicates, String identifier, InterMineObject imo) {
        List classes = new ArrayList(DynamicUtil.decomposeClass(imo.getClass()));
        String className = TypeUtil.unqualifiedName(((Class) classes.iterator().next()).getName());
        Map objectMap;
        if (classDuplicates.containsKey(className)) {
            objectMap = (Map) classDuplicates.get(className);
        } else {
            objectMap = new HashMap();
            classDuplicates.put(className, objectMap);
        }
        List objectList;
        if (objectMap.containsKey(identifier)) {
            objectList = (List) objectMap.get(identifier);
        } else {
            objectList = new ArrayList();
            objectMap.put(identifier, objectList);
        }
        objectList.add(imo);
    }
}
