package org.intermine.web.results;

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
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;
import org.intermine.web.Constants;
import org.intermine.web.InterMineAction;

/**
 * Implementation of <strong>Action</strong> that assembles data for viewing an object.
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class ObjectDetailsController extends InterMineAction
{
    /**
     * @see Action#execute
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Map displayObjects = (Map) session.getAttribute("displayObjects");
        
        // Build map from object id to DisplayObject
        if (displayObjects == null) {
            displayObjects = new HashMap();
            session.setAttribute("displayObjects", displayObjects);
        }
        String idString = (String) request.getParameter("id");
        if (idString != null && !idString.equals("")
            && (session.getAttribute("object") == null
                || ((DisplayObject) session.getAttribute("object")).getId()
                != Integer.parseInt(idString))) {
            // Move to a different object
            Integer key = new Integer(idString);
            InterMineObject object = os.getObjectById(key);
            if (object == null) {
                // no such object
                session.removeAttribute("object");
                return null;
            }
            String field = request.getParameter("field");
            if (field != null) {
                object = (InterMineObject) TypeUtil.getFieldValue(object, field);
            }
            DisplayObject dobj = (DisplayObject) displayObjects.get(key);
            if (dobj == null) {
                Map webconfigTypeMap = (Map) servletContext.getAttribute(Constants.DISPLAYERS); 
                Map webPropertiesMap = (Map) servletContext.getAttribute(Constants.WEB_PROPERTIES);
                dobj = new DisplayObject(object, os.getModel(), webconfigTypeMap, webPropertiesMap);
                displayObjects.put(key, dobj);
            }
            session.setAttribute("object", dobj);
        }
        
        return null;
    }
}