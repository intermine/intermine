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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.SavedQuery;
import org.intermine.api.search.Scope;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathConstraintMultiValue;
import org.intermine.pathquery.PathConstraintNull;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.api.template.TemplateManager;
import org.intermine.template.TemplateQuery;
import org.intermine.web.autocompletion.AutoCompleter;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.query.DisplayConstraint;
import org.intermine.web.logic.query.DisplayConstraintFactory;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for the template tile. This tile can be used for real template
 * input or to preview a TemplateQuery that is in construction. The preview
 * case involves the page controller or action putting the TemplateQuery in
 * the request attribute "previewTemplate". The jsp can also render differently
 * based on the presence of the "previewTemplate" attribute.<p>
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 * @author Richard Smith
 */
public class TemplateController extends TilesAction
{
    /**
     * Finds the correct template to display in the following ways:
     * <ol>
     *    <li>First, looks for a "name" request parameter and uses that for a the lookup.
     *    <li>If "name" parameter doesn't exist, looks for TemplateQuery in request attribute
     *        "previewTemplate".
     * </ol>
     * In all cases where a template name is provided (in other words, the template query
     * was not provided in the "previewTemplate" request attribute), the template is then
     * looked up based on the value of the "scope" request parameter.
     *
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(
            ComponentContext context,
            ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);
        TemplateManager templateManager = im.getTemplateManager();
        String templateName = request.getParameter("name");
        String scope = request.getParameter("scope");
        String loadModifiedTemplate = request.getParameter("loadModifiedTemplate");

        TemplateForm tf = (TemplateForm) form;
        tf.setScope(scope);
        // FIXME see #2239
        // If the template has been modified and uses an object bag constraint
        // it will be missing one of its original constraints (which will have been
        // replaced by constraining the id to be in the bag.
        TemplateQuery modifiedTemplate = null;
        TemplateQuery template = null;
        TemplateQuery originalTemplate = null;
        if (loadModifiedTemplate != null) {
            String savedQueryName = request.getParameter("savedQueryName");
            if (savedQueryName != null) {
                modifiedTemplate = getHistoryTemplate(session, savedQueryName);
            } else {
                PathQuery query = SessionMethods.getQuery(session);
                if (query instanceof TemplateQuery) {
                    modifiedTemplate = (TemplateQuery) query;
                }
            }
            templateName = modifiedTemplate.getName();
            template = modifiedTemplate;
            if (scope == null) {
                scope = Scope.ALL;
            }
            originalTemplate = templateManager.getTemplate(profile, templateName, scope);
        }
        // we're in querybuilder with queryBuilderTemplatePreview.jsp
        if (context.getAttribute("builder") != null
            || session.getAttribute(Constants.NEW_TEMPLATE) != null) {
            template = (TemplateQuery) SessionMethods.getQuery(session);
        }

        if (template == null) {
            if (scope == null) {
                scope = Scope.ALL;
            }
            template = templateManager.getTemplate(profile, templateName, scope);
        }

        TemplateQuery displayTemplate = null;
        // something's gone horribly wrong
        if (template == null) {
            ActionMessages actionMessages = getErrors(request);
            actionMessages.add(ActionMessages.GLOBAL_MESSAGE,
                               new ActionMessage("errors.template.missing", templateName));
            saveErrors(request, actionMessages);
            request.setAttribute("templateQuery", displayTemplate);
            return null;
        }

        List<DisplayConstraint> displayConstraintList = new ArrayList<DisplayConstraint>();
        displayTemplate = template.clone();

        DisplayConstraintFactory factory = getFactory(session);
        int index = 0;
//        Path path = null;
        DisplayConstraint displayConstraint = null;
        for (PathConstraint pathConstraint : template.getEditableConstraints()) {
            if ((loadModifiedTemplate != null) && (pathConstraint instanceof PathConstraintBag)) {
                String code = template.getConstraints().get(pathConstraint);
                PathConstraint originalPathConstraint = originalTemplate.getConstraintForCode(code);
//                path = originalTemplate.makePath(originalPathConstraint.getPath());
                displayConstraint = factory.get(originalPathConstraint, profile, originalTemplate);
                displayConstraint.setSwitchOffAbility(template.getSwitchOffAbility(pathConstraint));
                displayConstraint.setBagSelected(true);
                displayConstraint.setSelectedBagOp(pathConstraint.getOp());
                displayConstraint.setSelectedBagValue(
                                  ((PathConstraintBag) pathConstraint).getBag());
            } else {
//                path = template.makePath(pathConstraint.getPath());
                displayConstraint = factory.get(pathConstraint, profile, template);
            }
            displayConstraintList.add(displayConstraint);
            // to allow struts tag to set the combo checked
            if (displayConstraint.isBoolean()) {
                if (!displayConstraint.isBagSelected()) {
                    tf.setAttributeValues("" + (index + 1), displayConstraint.getSelectedValue());
                } else {
                    tf.setAttributeValues("" + (index + 1), displayConstraint.getOriginalValue());
                }
            }
            if (pathConstraint instanceof PathConstraintNull) {
                tf.setNullConstraint("" + (index + 1), displayConstraint.getSelectedValue());
            }
            if (pathConstraint instanceof PathConstraintBag) {
                tf.setUseBagConstraint("" + (index + 1), displayConstraint.isBagSelected());
            }
            if (pathConstraint instanceof PathConstraintMultiValue) {
                Collection<String> selectedValues = displayConstraint.getMultiValues();
                String[] multiSelecteValue = new String[selectedValues.size()];
                int multiValueIndex = 0;
                for (String selectedvalue : selectedValues) {
                    multiSelecteValue[multiValueIndex++] = selectedvalue;
                }
                tf.setMultiValues("" + (index + 1), multiSelecteValue);
            }
            index++;
        }
        verifyDisplayExtraValue(displayConstraintList, template);
        request.setAttribute("dcl", displayConstraintList);
        request.setAttribute("templateQuery", displayTemplate);
        String constraintLogic = template.getConstraintLogicForEditableConstraints();
        if (constraintLogic.contains("or") || constraintLogic.contains("not")) {
            request.setAttribute("displayLogicExpression", "true");
        }
        if (profile.getSavedTemplates().get(displayTemplate.getName()) != null) {
            request.setAttribute("IS_OWNER", true);
        }
        return null;
    }

    private TemplateQuery getHistoryTemplate(HttpSession session, String templateName) {
        Profile profile = SessionMethods.getProfile(session);
        SavedQuery savedQuery = profile.getHistory().get(templateName);
        TemplateQuery template = null;
        if (savedQuery != null && savedQuery.getPathQuery() instanceof TemplateQuery) {
            template = (TemplateQuery) savedQuery.getPathQuery();
        }
        return template;
    }

    private DisplayConstraintFactory getFactory(HttpSession session) {
        InterMineAPI im = SessionMethods.getInterMineAPI(session);
        AutoCompleter ac = SessionMethods.getAutoCompleter(session.getServletContext());
        DisplayConstraintFactory factory =  new DisplayConstraintFactory(im, ac);
        return factory;
    }

    private void verifyDisplayExtraValue(List<DisplayConstraint> displayConstraintList,
        TemplateQuery template) {
        Set<PathConstraint> pathConstraints = template.getConstraints().keySet();
        Model model = template.getModel();
        for (DisplayConstraint dc : displayConstraintList) {
            if (dc.isExtraConstraint()) {
                String mainPath = dc.getPath().getPath().toStringNoConstraints();
                String connectField = dc.getExtraConnectFieldPath();
                for (PathConstraint pathConstraint : pathConstraints) {
                    String  path = pathConstraint.getPath();
                    if (!mainPath.equals(path)) {
                        try {
                            if (path.equals(connectField)
                                || (path.startsWith(connectField)
                                    && connectField.split("\\.").length
                                        == path.split("\\.").length - 1)
                                    && (new Path(model, path)).endIsAttribute()) {
                                dc.setShowExtraConstraint(false);
                            }
                        } catch (PathException pe) {
                            //nothing
                        }
                    }
                }
            }
        }
    }
}
