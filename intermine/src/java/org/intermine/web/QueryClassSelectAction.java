package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.actions.LookupDispatchAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;

import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;

/**
 * Implementation of <strong>Action</strong> that processes
 * QueryClass selection form.
 *
 * @author Richard Smith
 * @author Mark Woodbridge
 */

public class QueryClassSelectAction extends LookupDispatchAction
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
    public ActionForward selectClass(ActionMapping mapping,
                                     ActionForm form,
                                     HttpServletRequest request,
                                     HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        String className = ((QueryClassSelectForm) form).getClassName();

        if (className == null) {
            ActionErrors actionErrors = new ActionErrors();
            actionErrors.add(ActionErrors.GLOBAL_ERROR,
                             new ActionError("errors.queryClassSelect.noClass"));
            saveErrors(request, actionErrors);

            return mapping.findForward("classChooser");
        } else {
            newQuery(className, session);

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
        session.removeAttribute("prefix");
    }
    
    /**
     * Browse the full class hierarchy and allow the user to choose a type to add
     * to the current query.
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
    public ActionForward browse(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        return mapping.findForward("tree");
    }

    /**
     * Distributes the actions to the necessary methods, by providing a Map from action to
     * the name of a method.
     *
     * @return a Map
     */
    protected Map getKeyMethodMap() {
        Map map = new HashMap();
        map.put("button.selectClass", "selectClass");
        map.put("button.browse", "browse");
        return map;
    }
}

