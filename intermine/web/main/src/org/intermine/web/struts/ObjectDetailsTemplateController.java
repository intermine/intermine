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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.WebResults;
import org.intermine.api.template.TemplatePopulator;
import org.intermine.api.template.TemplatePopulatorException;
import org.intermine.api.template.TemplateQuery;
import org.intermine.api.template.TemplateValue;
import org.intermine.model.InterMineObject;
import org.intermine.web.logic.results.DisplayObject;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateHelper;

/**
 * Controller for an inline table created by running a template on an object details page.
 * @author Kim Rutherford
 * @author Richard Smith
 */
public class ObjectDetailsTemplateController extends TilesAction
{
    private static final Logger LOG = Logger.getLogger(ObjectDetailsTemplateController.class);
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        DisplayObject displayObject = (DisplayObject) context.getAttribute("displayObject");
        InterMineBag interMineBag = (InterMineBag) context.getAttribute("interMineIdBag");

        TemplateQuery template = (TemplateQuery) context.getAttribute("templateQuery");

        Map<String, List<TemplateValue>> templateValues;

        // this is either a report page for an InterMineObject or a list analysis page
        if (displayObject != null) {
            InterMineObject obj = displayObject.getObject();
            templateValues = TemplateHelper.objectToTemplateValues(template, obj);
        } else if (interMineBag != null) {
            templateValues = TemplateHelper.bagToTemplateValues(template, interMineBag.getName());
        } else {
            // should only have been called with an object or a bag
            return null;
        }

        TemplateQuery populatedTemplate;
        try {
            populatedTemplate = TemplatePopulator.getPopulatedTemplate(template, 
                    templateValues);
        } catch (TemplatePopulatorException e) {
            LOG.error("Error setting up template '" + template.getName() + "' on report page for"
                    + ((displayObject == null) ? " bag " + interMineBag.getName() :
                        " object " + displayObject.getId()) + ".");
            return null;
        }
        
        WebResultsExecutor executor = SessionMethods.getWebResultsExecutor(session);
        WebResults webResults = executor.execute(populatedTemplate);
        // if there was a problem running query ignore and don't put up results
        if (webResults != null) {
            PagedTable pagedResults = new PagedTable(webResults, 10);
            pagedResults.setTableid("itt." + populatedTemplate.getName());
            context.putAttribute("resultsTable", pagedResults);
        }
        return null;
    }
}
