package org.intermine.web.results;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;
import org.intermine.web.Constants;

/**
 * Implementation of <strong>Action</strong> that assembles data for viewing an object
 * @author Mark Woodbridge
 */
public class ObjectDetailsController extends Action
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

        String idString = (String) request.getParameter("id");
        if (idString != null && !"".equals(idString)
            && (session.getAttribute("object") == null
            || ((DisplayObject) session.getAttribute("object")).getId()
                                                != Integer.parseInt(idString))) {
            InterMineObject object = os.getObjectById(new Integer(idString));
            String field = request.getParameter("field");
            if (field != null) {
                object = (InterMineObject) TypeUtil.getFieldValue(object, field);
            }
            session.setAttribute("object", new DisplayObject(object, os.getModel()));
        }
        
        return null;
    }
}