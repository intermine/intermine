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

import java.util.Iterator;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.tiles.ComponentContext;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;
import org.intermine.web.Constants;

/**
 * Implementation of <strong>TilesAction</strong>. Assembles data for
 * viewing an object.
 * @author Mark Woodbridge
 */
public class ObjectDetailsController extends TilesAction
{
    /**
     * @see TilesAction#execute
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Model model = os.getModel();
        String id = request.getParameter("id");
        String fieldName = request.getParameter("field");

        Object o = os.getObjectById(new Integer(id));
        if (fieldName != null) {
            o = TypeUtil.getFieldValue(o, fieldName);
        }

        String leafCldNames = "";
        for (Iterator i = ObjectViewController.getLeafClds(o.getClass(), model).iterator();
             i.hasNext();
             leafCldNames += " ") {
            leafCldNames += ((ClassDescriptor) i.next()).getUnqualifiedName();
        }
        session.setAttribute(Constants.RESULTS_TABLE, new PagedObject(leafCldNames, o));

        return null;
    }
}
