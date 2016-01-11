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

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.template.TemplatePopulator;
import org.intermine.api.util.NameUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.api.template.TemplateManager;
import org.intermine.template.TemplatePopulatorException;
import org.intermine.template.TemplateQuery;
import org.intermine.template.TemplateValue;
import org.intermine.web.logic.bag.BagHelper;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.http.TableExporterFactory;
import org.intermine.web.logic.export.http.TableHttpExporter;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateResultInput;
import org.intermine.web.logic.template.Templates;
import org.intermine.web.logic.template.Templates.TemplateValueParseException;

/**
 * Implementation of <strong>Action</strong> that runs a template
 *
 * @author Julie Sullivan
 */
@SuppressWarnings("deprecation")
public class LoadTemplateAction extends DispatchAction
{
    private TemplateQuery parseTemplate(HttpServletRequest request, InterMineAPI im) {

        // template name
        String name = request.getParameter("name");

        TemplateResultInput input = new TemplateResultInput();
        // parse constraints from request
        try {
            input.setConstraints(Templates.parseConstraints(request));
        } catch (TemplateValueParseException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        input.setName(name);

        TemplateManager templateManager = im.getTemplateManager();
        TemplateQuery template = templateManager.getGlobalTemplate(name);
        if (template == null) {
            throw new RuntimeException("template not found: " + name);
        }

        Map<String, List<TemplateValue>> templateValues;
        try {
            templateValues = Templates.getValuesFromInput(template, input);
        } catch (TemplateValueParseException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        // make new template
        try {
            template =
                TemplatePopulator.getPopulatedTemplate(template, templateValues);
        } catch (TemplatePopulatorException e) {
            throw new RuntimeException("Error in applying constraint values to template: "
                    + name);
        }
        return template;
    }

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
        TemplateQuery template = parseTemplate(request, im);
        SessionMethods.loadQuery(template, session, response);
        SessionMethods.logQuery(request.getSession());
        return new ForwardParameters(mapping.findForward("results"))
            .forward();
    }

    /**
     * Load a template from template name passed as parameter.  Create a list from the results
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward list(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                              HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        TemplateQuery template = parseTemplate(request, im);
        Profile profile = SessionMethods.getProfile(session);
        String path = request.getParameter("path");
        String bagName = template.getName() + "_results";
        bagName = NameUtil.generateNewName(profile.getSavedBags().keySet(), bagName);
        BagHelper.createBagFromPathQuery(template, bagName,
                template.getDescription(), path, profile, im);
        ForwardParameters forwardParameters =
            new ForwardParameters(mapping.findForward("bagDetails"));
        return forwardParameters.addParameter("bagName", bagName).forward();
    }

    /**
     * Load a template from template name passed as parameter.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *  an exception
     */
    public ActionForward export(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                              HttpServletResponse response) {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        String exportFormat = request.getParameter("format");
        Profile profile = SessionMethods.getProfile(session);
        PagedTable pt = null;
        TemplateQuery template = parseTemplate(request, im);

        WebResultsExecutor executor = im.getWebResultsExecutor(profile);
        try {
            pt = new PagedTable(executor.execute(template));
        } catch (ObjectStoreException e) {
            throw new RuntimeException("couldn't execute template:" + template.getName(), e);
        }

        WebConfig webConfig = SessionMethods.getWebConfig(request);
        TableExporterFactory factory = new TableExporterFactory(webConfig);

        TableHttpExporter exporter = null;
        try {
            exporter = factory.getExporter(exportFormat);
        } catch (Exception e) {
            throw new RuntimeException("bad exporter", e);
        }

        if (exporter == null) {
            throw new RuntimeException("unknown export format: " + exportFormat);
        }

        exporter.export(pt, request, response, null, null, null);

        // If null is returned then no forwarding is performed and
        // to the output is not flushed any jsp output, so user
        // will get only required export data
        return null;
    }
}
