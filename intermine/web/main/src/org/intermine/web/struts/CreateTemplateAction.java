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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.search.SearchRepository;
import org.intermine.api.tag.TagTypes;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.util.NameUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.template.TemplateQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.query.DisplayConstraint;
import org.intermine.web.logic.query.DisplayConstraintFactory;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Action to create a new TemplateQuery from current query.
 *
 * @author Thomas Riley
 */
public class CreateTemplateAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(CreateTemplateAction.class);

    /**
     * Take the current query and TemplateBuildState from the session and create a
     * TemplateQuery. Put the query in the user's profile.
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
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);
        ApiTemplate template = new ApiTemplate((TemplateQuery) SessionMethods.getQuery(session));
        String prevTemplateName = (String) session.getAttribute(Constants.PREV_TEMPLATE_NAME);

        boolean seenProblem = false;
        TemplateSettingsForm tsf = (TemplateSettingsForm) form;
        ActionErrors errors = tsf.validate(mapping, request);
        saveErrors(request, (ActionMessages) errors);
        if (errors != null) {
            return mapping.findForward("query");
        }
        template.setDescription(tsf.getDescription());
        template.setName(tsf.getName());
        template.setTitle(tsf.getTitle());
        template.setComment(tsf.getComment());

        WebResultsExecutor webResultsExecutor = im.getWebResultsExecutor(profile);

        // Check whether query has at least one constraint and at least one output
        if (template.getView().size() == 0) {
            recordError(new ActionMessage("errors.createtemplate.nooutputs"), request);
            seenProblem = true;
        }
        if (template.getBagNames().size() != 0) {
            recordError(new ActionMessage("errors.createtemplate.templatewithlist"), request);
            seenProblem = true;
        }

        // Check for a name clash with system templates
        Profile superUser = im.getProfileManager().getSuperuserProfile();
        if (!superUser.equals(profile)) {
            if (superUser.getSavedTemplates().containsKey(template.getName())) {
                recordError(new ActionMessage("errors.createtemplate.existinginpublic", template.getName()),
                        request);
                seenProblem = true;
            }
        }

        // Check whether there is a template name clash
        boolean isNewTemplate = (session.getAttribute(Constants.NEW_TEMPLATE) != null
            && ((Boolean) session.getAttribute(Constants.NEW_TEMPLATE)).booleanValue())
            ? true : false;
        if (profile.getSavedTemplates().containsKey(template.getName())
            && (isNewTemplate
                || (prevTemplateName != null && !prevTemplateName.equals(template.getName())))) {
            recordError(new ActionMessage("errors.createtemplate.existing", template.getName()),
                    request);
            seenProblem = true;
        }

        if (StringUtils.isEmpty(template.getName())) {
            recordError(new ActionMessage("errors.required", "Template name"), request);
            seenProblem = true;
        } else if (!NameUtil.isValidName(template.getName())) {
            recordError(new ActionMessage("errors.badChars"), request);
            seenProblem = true;
        }

        // Ensure that we can actually execute the query
        if (!seenProblem) {
            try {
                if (webResultsExecutor.getQueryInfo(template) == null) {
                    webResultsExecutor.setQueryInfo(template, webResultsExecutor.explain(template));
                }
            } catch (ObjectStoreException e) {
                recordError(new ActionMessage("errors.query.objectstoreerror"), request, e, LOG);
                seenProblem = true;
            }
        }

        boolean foundEditableConstraint = false;
        boolean foundNonEditableLookup = false;
        for (PathConstraint c : template.getConstraints().keySet()) {
            if (template.isEditable(c)) {
                foundEditableConstraint = true;
            } else if (c instanceof PathConstraintLookup) {
                foundNonEditableLookup = true;
            }
        }

        // template must have at least one editable constrain
        if (!foundEditableConstraint) {
            recordError(new ActionMessage("errors.createtemplate.noconstraints"), request);
            seenProblem = true;
        }

        // template cannot have non-editable LOOKUP constraints
        if (foundNonEditableLookup) {
            recordError(new ActionMessage("errors.createtemplate.noneditablelookup"), request);
            seenProblem = true;
        }

        if (seenProblem) {
            return mapping.findForward("query");
        }

        // no problems!  TODO this should be updated somewhere else

        String key = (isNewTemplate) ? "templateBuilder.templateCreated"
                                       : "templateBuilder.templateUpdated";

        recordMessage(new ActionMessage(key, template.getName()), request);

        //ApiTemplate toSave = new ApiTemplate(template);
        if (isNewTemplate) {
            profile.saveTemplate(template.getName(), template);
        } else {
            String oldTemplateName = (prevTemplateName != null)
                ? prevTemplateName : template.getName();
            profile.updateTemplate(oldTemplateName, template);
            im.getTrackerDelegate().updateTemplateName(oldTemplateName, template.getName());
        }

        session.removeAttribute(Constants.NEW_TEMPLATE);
        session.removeAttribute(Constants.PREV_TEMPLATE_NAME);

        SessionMethods.loadQuery(template, request.getSession(), response);
        if ("SAVE".equals(tsf.getActionType())) {
            return mapping.findForward("query");
        } else {
            //prepare display constraint list to display  parameter values
            //edited by the user in the result page
            DisplayConstraintFactory factory =  new DisplayConstraintFactory(im, null);
            DisplayConstraint displayConstraint = null;
            List<DisplayConstraint> displayConstraintList = new ArrayList<DisplayConstraint>();
            for (PathConstraint pathConstraint : template.getEditableConstraints()) {
                displayConstraint = factory.get(pathConstraint, profile, template);
                displayConstraintList.add(displayConstraint);
            }
            session.setAttribute("dcl", displayConstraintList);
            return mapping.findForward("run");
        }
    }
}
