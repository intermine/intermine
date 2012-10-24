package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
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

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.WebResults;
import org.intermine.api.template.TemplatePopulator;
import org.intermine.model.InterMineObject;
import org.intermine.template.TemplatePopulatorException;
import org.intermine.template.TemplateQuery;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateHelper;

/**
 * Controller for an inline table created by running a template on a report page.
 * @author Kim Rutherford
 * @author Richard Smith
 */
public class ReportTemplateController extends TilesAction
{
    private static final Logger LOG = Logger.getLogger(ReportTemplateController.class);

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("null")
    @Override
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        ReportObject reportObject = (ReportObject) context.getAttribute("reportObject");
        InterMineBag interMineBag = (InterMineBag) context.getAttribute("interMineIdBag");

        TemplateQuery template = (TemplateQuery) context.getAttribute("templateQuery");

        // this is either a report page for an InterMineObject or a list analysis page
        TemplateQuery populatedTemplate;
        try {
            if (reportObject != null) {
                InterMineObject obj = reportObject.getObject();
                template = TemplateHelper.removeDirectAttributesFromView(template);
                populatedTemplate = TemplatePopulator.populateTemplateWithObject(template, obj);
            } else if (interMineBag != null) {
                populatedTemplate = TemplatePopulator.populateTemplateWithBag(template,
                                                                              interMineBag);
            } else {
                // should only have been called with an object or a bag
                return null;
            }
        } catch (TemplatePopulatorException e) {
            LOG.error("Error setting up template '" + template.getName() + "' on report page for"
                    + ((reportObject == null) ? " bag " + interMineBag.getName()
                        : " object " + reportObject.getId()) + ".", e);
            throw new RuntimeException("Error setting up template '" + template.getName()
                    + "' on report page for" + ((reportObject == null) ? " bag "
                        + interMineBag.getName() : " object " + reportObject.getId()) + ".", e);
        }

        Profile profile = SessionMethods.getProfile(session);
        WebResultsExecutor executor = im.getWebResultsExecutor(profile);
        WebResults webResults = executor.execute(populatedTemplate);
        // if there was a problem running query ignore and don't put up results
        if (webResults != null) {
            PagedTable pagedResults = new PagedTable(webResults, 20);
            pagedResults.setTableid("itt." + populatedTemplate.getName());
            context.putAttribute("resultsTable", pagedResults);
        }
        return null;
    }
}
