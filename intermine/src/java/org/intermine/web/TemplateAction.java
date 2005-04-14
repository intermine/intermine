package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
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

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;

/**
 * Action to handle submit from the template page. <code>setSavingQueries</code>
 * can be used to set whether or not queries run by this action are automatically
 * saved in the user's query history. This property is true by default.
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class TemplateAction extends InterMineAction
{
    /**
     * Build a query based on the template and the input from the user. The template to
     * be run is identified by the "queryName" and "templateType" request parameters or,
     * if they are not found there, session attributes. There are also some request
     * parameters that, if present, effect the behaviour of the action. These are:
     *
     * <dl>
     * <dt>skipBuilder</dt>
     *      <dd>If this attribute is specifed (with any value) then the action will forward
     *      directly to the object details page if the results contain just one object.</dd>
     * <dt>noSaveQuery</dt>
     *      <dd>If this attribute is specifed (with any value) then the query is not
     *      automatically saved in the user's query history.</dd>
     * </dl>
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
        String queryName = request.getParameter("queryName");
        String templateType = request.getParameter("templateType");
        boolean saveQuery = (request.getParameter("noSaveQuery") == null);
        boolean skipBuilder = (request.getParameter("skipBuilder") != null);
        
        if (templateType == null) {
            templateType = (String) session.getAttribute("templateType");
        }
        
        if (queryName == null) {
            queryName = (String) session.getAttribute("queryName");
        }
        
        SessionMethods.logTemplateQueryUse(session, templateType, queryName);
        
        TemplateQuery template = TemplateHelper.findTemplate(request, queryName, templateType);
        PathQuery queryCopy = TemplateHelper.templateFormToQuery((TemplateForm) form, template);
        SessionMethods.loadQuery(queryCopy, request.getSession());
        form.reset (mapping, request);
        
        if (!skipBuilder) {
            return mapping.findForward("query");
        }
        
        QueryMonitorTimeout clientState
                = new QueryMonitorTimeout(Constants.QUERY_TIMEOUT_SECONDS * 1000);
        MessageResources messages = (MessageResources) request.getAttribute(Globals.MESSAGES_KEY);
        String qid = SessionMethods.startQuery(clientState, session, messages, saveQuery);
        Thread.sleep(200);
        return new ForwardParameters(mapping.findForward("waiting"))
                            .addParameter("qid", qid).forward();
    }
}
