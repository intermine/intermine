package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.WebResults;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplatePopulator;
import org.intermine.api.template.TemplatePopulatorException;
import org.intermine.api.template.TemplateQuery;
import org.intermine.api.template.TemplateValue;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.http.TableExporterFactory;
import org.intermine.web.logic.export.http.TableHttpExporter;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateResultInput;

/**
 * Implementation of <strong>Action</strong> that runs a template
 *
 * @author Julie Sullivan
 */
public class LoadTemplateAction extends DispatchAction
{

    private static final Logger LOG = Logger.getLogger(LoadTemplateAction.class);
    /**
     * Load a template from template name passed as parameter.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward results(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                              HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        // forward to list or results
        String forward = request.getParameter("forward");
        // or export
        String exportFormat = request.getParameter("format");
        // template name
        String name = request.getParameter("name");

        TemplateResultInput input = new TemplateResultInput();
        // parse constraints from request
        input.setConstraints(TemplateHelper.parseConstraints(request));
        input.setName(name);

        TemplateManager templateManager = im.getTemplateManager();
        TemplateQuery template = templateManager.getGlobalTemplate(name);
        if (template == null) {
            throw new RuntimeException("template not found: " + name);
        }

        Map<String, List<TemplateValue>> templateValues = TemplateHelper.getValuesFromInput(
                template, input);

        // make new template
        try {
            template =
                TemplatePopulator.getPopulatedTemplate(template, templateValues);
        } catch (TemplatePopulatorException e) {
            throw new RuntimeException("Error in applying constraint values to template: "
                    + name);
        }

        // run query
        if (true) {
            SessionMethods.loadQuery(template, session, response);
            String qid = SessionMethods.startQueryWithTimeout(request, false, template);
            Thread.sleep(200); // slight pause in the hope of avoiding holding page
            return new ForwardParameters(mapping.findForward("waiting"))
                .addParameter("qid", qid).forward();


        }
        return null;
    }
}
