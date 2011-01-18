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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.apache.struts.tiles.ComponentContext;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.WebResults;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplatePopulator;
import org.intermine.api.template.TemplatePopulatorException;
import org.intermine.api.template.TemplateQuery;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.logic.results.DisplayObject;
import org.intermine.web.logic.results.DisplayObjectFactory;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Action to handle events related to displaying inline templates.
 *
 * @author Mark Woodbridge
 */
public class ModifyDetails extends DispatchAction
{
    private static final Logger LOG = Logger.getLogger(ModifyDetails.class);
    /**
     * Show in table for inline template queries.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws an exception
     */
    public ActionForward runTemplate(ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        String name = request.getParameter("name");
        String scope = request.getParameter("scope");
        String bagName = request.getParameter("bagName");
        String idForLookup = request.getParameter("idForLookup");
        Profile profile = SessionMethods.getProfile(session);

        TemplateManager templateManager = im.getTemplateManager();
        TemplateQuery template = templateManager.getTemplate(profile, name, scope);

        BagManager bagManager = im.getBagManager();
        InterMineBag bag = bagManager.getUserOrGlobalBag(profile, bagName);

        TemplateQuery populatedTemplate;
        try {
            if (idForLookup != null && idForLookup.length() != 0) {
                Integer objectId = new Integer(idForLookup);
                ObjectStore os = im.getObjectStore();
                InterMineObject object = os.getObjectById(objectId);
                populatedTemplate = TemplatePopulator.populateTemplateWithObject(template, object);
            } else {
                populatedTemplate = TemplatePopulator.populateTemplateWithBag(template, bag);
            }
        } catch (TemplatePopulatorException e) {
            LOG.error("Error running up template '" + template.getName() + "' from report page for"
                    + ((idForLookup == null) ? " bag " + bagName
                        : " object " + idForLookup) + ".", e);
            throw new RuntimeException("Error running up template '" + template.getName()
                    + "' from report page for" + ((idForLookup == null) ? " bag " + bagName
                        : " object " + idForLookup) + ".", e);
        }
        String identifier = "itt." + populatedTemplate.getName() + "." + idForLookup;

        WebResultsExecutor executor = im.getWebResultsExecutor(profile);
        WebResults webResults = executor.execute(populatedTemplate);
        PagedTable pagedResults = new PagedTable(webResults, 10);

        SessionMethods.setResultsTable(session, identifier, pagedResults);

        // add results table to trail
        String trail = request.getParameter("trail");
        if (trail != null) {
            trail += "|results." + identifier;
        } else {
            trail = "|results." + identifier;
        }

        return new ForwardParameters(mapping.findForward("results"))
            .addParameter("templateQueryTitle", template.getTitle())
            .addParameter("templateQueryDescription",
                (template.getDescription() != null) ? template.getDescription() : "")
            .addParameter("table", identifier).addParameter("trail", trail).forward();
    }

    /**
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws an exception
     * @deprecated ajaxVerbosify is used instead
     */
    @Deprecated
    public ActionForward verbosify(ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        if (session == null) {
            return null;
        }

        String fieldName = request.getParameter("field");
        String trail = request.getParameter("trail");
        String placement = request.getParameter("placement");
        DisplayObject object = getDisplayObject(session, request.getParameter("id"));

        if (object != null) {
            object.setVerbosity(placement + "_" + fieldName, true);
        }

        return forwardToObjectDetails(mapping, request.getParameter("id"), trail);
    }

    /**
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws an exception
     * @deprecated ajaxVerbosify is used instead
     */
    @Deprecated
    public ActionForward unverbosify(ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        String fieldName = request.getParameter("field");
        String trail = request.getParameter("trail");
        String placement = request.getParameter("placement");
        DisplayObject object = getDisplayObject(session, request.getParameter("id"));

        object.setVerbosity(placement + "_" + fieldName, false);

        return forwardToObjectDetails(mapping, request.getParameter("id"), trail);
    }

    /**
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws an exception
     */
    public ActionForward ajaxVerbosify(ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        String fieldName = request.getParameter("field");
        String trail = request.getParameter("trail");
        String placement = request.getParameter("placement");
        DisplayObject object = getDisplayObject(session, request.getParameter("id"));
        Object collection = object.getRefsAndCollections().get(fieldName);

        String key = placement + "_" + fieldName;

        object.setVerbosity(key, !object.isVerbose(key));

        request.setAttribute("object", object);
        request.setAttribute("trail", trail);
        request.setAttribute("collection", collection);
        request.setAttribute("fieldName", fieldName);

        if (object.isVerbose(key)) {
            return mapping.findForward("objectDetailsCollectionTable");
        }
        return null;
    }

    /**
     * Count number of results for a template for inline templates
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws an exception
     */
    public ActionForward ajaxTemplateCount(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);
        String type = request.getParameter("type");
        String id = request.getParameter("id");
        String templateName = request.getParameter("template");
        String detailsType = request.getParameter("detailsType");
        ObjectStore os = im.getObjectStore();

        TemplateManager templateManager = im.getTemplateManager();
        TemplateQuery tq = templateManager.getTemplate(profile, templateName, type);

        ComponentContext cc = new ComponentContext();

        if ("object".equals(detailsType)) {
            InterMineObject o = os.getObjectById(new Integer(id));
            DisplayObjectFactory displayObjects = SessionMethods.getDisplayObjects(session);
            DisplayObject obj = displayObjects.get(o);
            cc.putAttribute("displayObject", obj);
            cc.putAttribute("templateQuery", tq);
            cc.putAttribute("placement", request.getParameter("placement"));

            new ObjectDetailsTemplateController().execute(cc, mapping, form, request, response);
            request.setAttribute("org.apache.struts.taglib.tiles.CompContext", cc);
            return mapping.findForward("objectDetailsTemplateTable");
        }
        BagManager bagManager = im.getBagManager();

        InterMineBag interMineBag = bagManager.getUserOrGlobalBag(profile, id);
        cc.putAttribute("interMineIdBag", interMineBag);
        cc.putAttribute("templateQuery", tq);
        cc.putAttribute("placement", request.getParameter("placement"));

        new ObjectDetailsTemplateController().execute(cc, mapping, form, request, response);
        request.setAttribute("org.apache.struts.taglib.tiles.CompContext", cc);
        return mapping.findForward("objectDetailsTemplateTable");
    }

    /**
     * Construct an ActionForward to the object details page.
     */
    private ActionForward forwardToObjectDetails(ActionMapping mapping, String id, String trail) {
        ForwardParameters forward = new ForwardParameters(mapping.findForward("objectDetails"));
        forward.addParameter("id", id);
        forward.addParameter("trail", trail);
        return forward.forward();
    }

    /**
     * Get a DisplayObject from the session given the object id as a string.
     *
     * @param session the current http session
     * @param idString intermine object id
     * @return DisplayObject for the intermine object
     */
    protected DisplayObject getDisplayObject(HttpSession session, String idString) {
        ObjectStore os = SessionMethods.getInterMineAPI(session).getObjectStore();
        InterMineObject obj = null;
        try {
            obj = os.getObjectById(new Integer(idString));
        } catch (ObjectStoreException e) {
            LOG.error("Exception while fetching object with id " + idString, e);
        }
        if (obj == null) {
            LOG.error("Could not find object with id " + idString);
            return null;
        }
        DisplayObjectFactory displayObjects = SessionMethods.getDisplayObjects(session);
        DisplayObject retval = displayObjects.get(obj);
        if (retval == null) {
            LOG.error("Could not find DisplayObject on session for id " + idString);
        }
        return retval;
    }
}
