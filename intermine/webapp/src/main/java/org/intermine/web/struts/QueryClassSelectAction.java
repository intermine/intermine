package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.Model;
import org.intermine.pathquery.PathQuery;
import org.intermine.metadata.TypeUtil;
import org.intermine.web.logic.session.SessionMethods;

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
    @Override
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
        }
        newQuery(className, session);
        return mapping.findForward("query");
    }

    /**
     * Add a new query, based on the specified class, to the session.
     *
     * @param className the name of the starting class
     * @param session the session
     */
    public static void newQuery(String className, HttpSession session) {
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Model model = im.getModel();

        PathQuery query = new PathQuery(model);
        SessionMethods.setQuery(session, query);
        session.setAttribute("prefix", TypeUtil.unqualifiedName(className));
        session.setAttribute("path", TypeUtil.unqualifiedName(className));
    }
}
