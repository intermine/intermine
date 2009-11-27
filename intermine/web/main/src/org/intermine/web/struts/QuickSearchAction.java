package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.util.MessageResources;
import org.intermine.api.search.Scope;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplatePopulator;
import org.intermine.api.template.TemplateQuery;
import org.intermine.api.template.TemplateValue;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.query.QueryMonitorTimeout;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateHelper;

/**
 * @author Xavier Watkins
 *
 */
public class QuickSearchAction extends InterMineAction
{
    private static final Logger LOG = Logger.getLogger(QuickSearchAction.class);
    /**
     * Method called when user has submitted search form.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    @Override
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();
        ServletContext context = session.getServletContext();
        QuickSearchForm qsf = (QuickSearchForm) form;
        String qsType = qsf.getQuickSearchType();
        session.setAttribute("quickSearchType", qsType);
        if (qsType.equals("ids")) {
            Map webPropertiesMap = (Map) context.getAttribute(Constants.WEB_PROPERTIES);

            // remove the last query ran, otherwise the old query will show up on the results page
            session.removeAttribute(Constants.QUERY);

            String templateName = (String) webPropertiesMap.get("begin.browse.template");

            if (templateName == null) {
                LOG.error("'begin.browse.template' not configured correctly in properties file.");
                recordError(new ActionMessage("quicksearch.fail"), request);
                return mapping.findForward("error");
            }

            SessionMethods.logTemplateQueryUse(session, Scope.GLOBAL, templateName);

            TemplateManager templateManager = SessionMethods.getTemplateManager(session);
            TemplateQuery template = templateManager.getGlobalTemplate(templateName);

            if (template == null) {
                LOG.error("'begin.browse.template' not configured correctly in properties file.");
                recordError(new ActionMessage("quicksearch.fail"), request);
                return mapping.findForward("error");
            }

            QueryMonitorTimeout clientState = new QueryMonitorTimeout(Constants.
                                                                      QUERY_TIMEOUT_SECONDS * 1000);
            MessageResources messages =
                (MessageResources) request.getAttribute(Globals.MESSAGES_KEY);

            String value = qsf.getParsedValue();
            Map<String, List<TemplateValue>> templateValues = 
                TemplateHelper.singleConstraintTemplateValues(template, ConstraintOp.EQUALS, value);
            TemplateQuery populatedTemplate = TemplatePopulator.getPopulatedTemplate(template, 
                    templateValues);
            
            String qid = SessionMethods.startQuery(clientState, session, messages, false,
                                                   populatedTemplate);
            Thread.sleep(200);
            return new ForwardParameters(mapping.findForward("waiting"))
                .addParameter("qid", qid)
                .addParameter("trail", "")
                .forward();
        } else if (qsType.equals("bgs")) {
            request.setAttribute("type", "bag");
            request.setAttribute("initialFilterText", qsf.getValue());
            return new ForwardParameters(mapping.findForward("bags"))
                .addParameter("subtab", "view").forward();
        } else if (qsType.equals("tpls")) {
            request.setAttribute("type", "template");
            request.setAttribute("initialFilterText", qsf.getValue());
            return mapping.findForward("templates");
        } else {
            throw new RuntimeException("Quick search type not valid");
        }
    }
}
