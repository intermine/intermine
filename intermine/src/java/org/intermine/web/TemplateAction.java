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

import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.intermine.model.InterMineObject;
import org.intermine.web.results.PagedResults;

/**
 * Action to handle submit from the template page.
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class TemplateAction extends Action
{
    /**
     * Build a query based on the template and the input from the user.
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
        ServletContext servletContext = session.getServletContext();
        String queryName = (String) session.getAttribute("queryName");
        String templateType = (String) session.getAttribute("templateType");
        
        TemplateQuery template = TemplateHelper.findTemplate(request, queryName, templateType);
        PathQuery queryCopy = TemplateHelper.templateFormToQuery((TemplateForm) form, template);
        
        SessionMethods.loadQuery(queryCopy, request.getSession());
        form.reset (mapping, request);
        return handleTemplateQuery(mapping, request, response,
                                    (request.getParameter("skipBuilder") != null));
    }
    
    /**
     * Called after the form has been read and the query has been loaded into the session.
     * Decides whether to forward to the query builder, the results page or to an object
     * details page.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @param skipBuilder If true then skip query builder
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if the application business logic throws
     *  an exception
     */
    protected ActionForward handleTemplateQuery(ActionMapping mapping,
                                                HttpServletRequest request,
                                                HttpServletResponse response,
                                                boolean skipBuilder)
                                                 
        throws Exception {
        HttpSession session = request.getSession();
        
        if (skipBuilder) {
            // If the form wants to skip the query builder we need to execute the query
            if (!SessionMethods.runQuery (session, request)) {
                return mapping.findForward("failure");
            }
            // Look at results, if only one result, go straight to object details page
            PagedResults pr = (PagedResults) session.getAttribute (Constants.QUERY_RESULTS);
            if (pr.getSize () == 1) {
                Object o = ((List) pr.getAllRows ().get(0)).get(0);
                if (o instanceof InterMineObject) {
                    return new ActionForward("/objectDetails.do?id="
                                                        + ((InterMineObject) o).getId(), true);
                }
            }
            return mapping.findForward("results");
        } else {
            return mapping.findForward("query");
        }
    }
}
