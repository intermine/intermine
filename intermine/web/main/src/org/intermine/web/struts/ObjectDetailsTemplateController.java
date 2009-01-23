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

import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.query.WebResultsExecutor;
import org.intermine.web.logic.results.DisplayObject;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.WebResults;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateQuery;

/**
 * Controller for an inline table created by running a template on an object details page.
 * @author Kim Rutherford
 */
public class ObjectDetailsTemplateController extends TilesAction
{
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
        ServletContext servletContext = session.getServletContext();
        DisplayObject displayObject = (DisplayObject) context.getAttribute("displayObject");
        InterMineBag interMineIdBag = (InterMineBag) context.getAttribute("interMineIdBag");

        if (displayObject == null && interMineIdBag == null) {
            return null;
        }

        TemplateQuery templateQuery = (TemplateQuery) context.getAttribute("templateQuery");

        TemplateForm templateForm = new TemplateForm();
        Model model = SessionMethods.getObjectStore(servletContext).getModel();
        
        // this is either a report page for an InterMineObject or a list analysis page        
        if (displayObject != null) {
            InterMineObject object = displayObject.getObject();
            if (!TemplateHelper.fillTemplateForm(templateQuery, object, null, templateForm,
                    model)) {
                return null;
            }
        } else {
            if (!TemplateHelper.fillTemplateForm(templateQuery, null, interMineIdBag, templateForm,
                    model)) {
                return null;
            }
        }
            
        templateForm.parseAttributeValues(templateQuery, null, new ActionErrors(), false);

        // note that savedBags parameter is an empty set, we are on a report page for an object,
        // the object/bag we are using is already set in the TemplateForm, we can't use other bags
        TemplateQuery populatedTemplate = TemplateHelper.templateFormToTemplateQuery(templateForm, 
                templateQuery, new HashMap());
                
        WebResultsExecutor executor = SessionMethods.getWebResultsExecutor(session);
        WebResults webResults = executor.execute(populatedTemplate);
        PagedTable pagedResults = new PagedTable(webResults, 10);
        
        context.putAttribute("resultsTable", pagedResults);
        return null;
    }
}
