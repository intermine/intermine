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

import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.intermine.cache.InterMineCache;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

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
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        TemplateBuildState tbs =
            (TemplateBuildState) session.getAttribute(Constants.TEMPLATE_BUILD_STATE);
        
        boolean seenProblem = false;

        // Check whether query has at least one constraint and at least one output
        if (query.getView().size() == 0) {
            recordError(new ActionMessage("errors.createtemplate.nooutputs"), request);
            seenProblem = true;
        }
        Iterator iter = query.getNodes().values().iterator();
        boolean foundEditableConstraint = false;
        while (iter.hasNext()) {
            PathNode node = (PathNode) iter.next();
            if (node.isAttribute()) {
                Iterator citer = node.getConstraints().iterator();
                while (citer.hasNext()) {
                    Constraint c = (Constraint) citer.next();
                    if (c.isEditable()) {
                        foundEditableConstraint = true;
                        break;
                    }
                }
            }
        }
        if (!foundEditableConstraint) {
            recordError(new ActionMessage("errors.createtemplate.noconstraints"), request);
            seenProblem = true;
        }
        
        // Check for a name clash with system templates
        Profile superUser = SessionMethods.getSuperUserProfile(servletContext);
        if (superUser.getSavedTemplates().containsKey(tbs.getName())) {
            recordError(new ActionMessage("errors.createtemplate.existing", tbs.getName()),
                    request);
            seenProblem = true;
        }
        
        // Check whether there is a template name clash
        if (profile.getSavedTemplates().containsKey(tbs.getName())
                && (tbs.getUpdatingTemplate() == null
                    || !tbs.getUpdatingTemplate().getName().equals(tbs.getName()))) {
            recordError(new ActionMessage("errors.createtemplate.existing", tbs.getName()),
                    request);
            seenProblem = true;
        }
        
        if (StringUtils.isEmpty(tbs.getName())) {
            recordError(new ActionMessage("errors.required", "Template name"), request);
            seenProblem = true;
        } else if (!WebUtil.isValidName(tbs.getName())) { 
            recordError(new ActionMessage("errors.badChars"), request);
            seenProblem = true;
        }
           
        // Ensure that we can actually execute the query
        if (!seenProblem) {
            try {
                if (query.getInfo() == null) {
                    query.setInfo(os.estimate(MainHelper.makeQuery(query, profile.getSavedBags())));
                }
            } catch (ObjectStoreException e) {
                recordError(new ActionMessage("errors.query.objectstoreerror"), request, e, LOG);
                seenProblem = true;
            }
        }
        
        if (seenProblem) {
            return mapping.findForward("query");
        }
        
        TemplateQuery template = TemplateHelper.buildTemplateQuery(tbs, query);
        TemplateQuery editing = tbs.getUpdatingTemplate();
        
        String key = (editing == null) ? "templateBuilder.templateCreated"
                                       : "templateBuilder.templateUpdated";
        
        recordMessage(new ActionMessage(key, template.getName()), request);
        
        // Replace template if needed
        if (editing != null) {
            profile.deleteTemplate(editing.getName());
        }
        profile.saveTemplate(template.getName(), template);
        // If superuser then rebuild shared templates
        if (profile.getUsername() != null && profile.getUsername().equals
                (servletContext.getAttribute(Constants.SUPERUSER_ACCOUNT))) {
            TemplateRepository tr = TemplateRepository.getTemplateRepository(servletContext);
            if (editing != null) {
                tr.globalTemplateUpdated(template);
            } else {
                tr.globalTemplateAdded(template);
            }
        }
        
        session.removeAttribute(Constants.TEMPLATE_BUILD_STATE);

        cleanCache(servletContext, template);
        
        return mapping.findForward("mymine");
    }

    /**
     * Remove all entries from the cache that mention the given template.
     */
    private void cleanCache(ServletContext servletContext, TemplateQuery template) {
        InterMineCache cache = ServletMethods.getGlobalCache(servletContext);
        cache.flushByKey(TemplateHelper.TEMPLATE_TABLE_CACHE_TAG,
                         new Object[] {template.getName(), null, null});
    }
}
