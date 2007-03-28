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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.PathQuery;
import org.intermine.web.logic.SessionMethods;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

/**
 * Implementation of <strong>Action</strong> that processes
 * QueryClass selection form.
 *
 * @author Richard Smith
 * @author Mark Woodbridge
 */

public class QueryClassSelectAction extends InterMineAction
{
    /**
     * Add a QueryClass of a specified type to the current query.
     * Return an <code>ActionForward</code> instance describing where and how
     * control should be forwarded, or <code>null</code> if the response has
     * already been completed.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        String className = ((QueryClassSelectForm) form).getClassName();

        if (className == null) {
            recordError(new ActionMessage("errors.queryClassSelect.noClass"), request);

            return mapping.findForward("classChooser");
        } else {
            newQuery(className, session);
            SessionMethods.setHasQueryCookie(session, response, true);
            return mapping.findForward("query");
        }
    }

    /**
     * Add a new query, based on the specified class, to the session
     * @param className the class name
     * @param session the session
     */
    public static void newQuery(String className, HttpSession session) {
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        PathQuery query = new PathQuery(os.getModel());
        session.setAttribute(Constants.QUERY, query);
        session.setAttribute("path", TypeUtil.unqualifiedName(className));
        session.setAttribute("prefix", TypeUtil.unqualifiedName(className));
        session.removeAttribute(Constants.TEMPLATE_BUILD_STATE);
        session.removeAttribute(Constants.EDITING_VIEW);
    }
}

