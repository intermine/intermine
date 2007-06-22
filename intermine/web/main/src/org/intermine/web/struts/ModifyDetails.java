package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.apache.struts.tiles.ComponentContext;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.results.DisplayObject;
import org.intermine.web.logic.results.InlineTemplateTable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateListHelper;
import org.intermine.web.logic.template.TemplateQuery;

/**
 * Action to handle events from the object details page
 *
 * @author Mark Woodbridge
 */
public class ModifyDetails extends DispatchAction
{
    /**
     * Show in table for inline template queries.
     *
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws an exception
     */
    public ActionForward runTemplate(ActionMapping mapping, 
                                     @SuppressWarnings("unused") ActionForm form,
                                     HttpServletRequest request, HttpServletResponse response)
                    throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        String name = request.getParameter("name");
        String scope = request.getParameter("scope");
        String bagName = request.getParameter("bagName");
        String idForLookup = request.getParameter("idForLookup");
        String userName = ((Profile) session.getAttribute(Constants.PROFILE)).getUsername();
        TemplateQuery template = TemplateHelper.findTemplate(servletContext, session, userName,
                                                             name, scope);
        String trail = request.getParameter("trail");
        InlineTemplateTable itt = null;
        
        if (idForLookup != null && idForLookup.length() != 0) {
            Integer objectId = new Integer(idForLookup);
            itt =
                TemplateHelper.getInlineTemplateTable(servletContext, name,
                                                      objectId, userName);
        } else if (bagName != null && bagName.length() != 0) {
            InterMineBag interMineBag = ((Profile) session
                            .getAttribute(Constants.PROFILE)).getSavedBags().get(bagName);
            itt = TemplateHelper.getInlineTemplateTable(servletContext, name, 
                                                        interMineBag, userName);
        }
        String identifier = "itt." + template.getName() + "." + idForLookup;
        SessionMethods.setResultsTable(session, identifier, itt.getPagedTable());

        return new ForwardParameters(mapping.findForward("results"))
                        .addParameter("table", identifier)
                        .addParameter("size", "10")
                        .addParameter("trail", trail).forward();
    }

    /**
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws an exception
     */
    public ActionForward verbosify(ActionMapping mapping, 
                                   @SuppressWarnings("unused") ActionForm form,
                                   HttpServletRequest request, 
                                   @SuppressWarnings("unused") HttpServletResponse response)
                    throws Exception {
        HttpSession session = request.getSession();
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
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws an exception
     */
    public ActionForward unverbosify(ActionMapping mapping,
                                     @SuppressWarnings("unused") ActionForm form,
                                     HttpServletRequest request,
                                     @SuppressWarnings("unused") HttpServletResponse response)
                    throws Exception {
        HttpSession session = request.getSession();
        String fieldName = request.getParameter("field");
        String trail = request.getParameter("trail");
        String placement = request.getParameter("placement");
        DisplayObject object = getDisplayObject(session, request.getParameter("id"));

        object.setVerbosity(placement + "_" + fieldName, false);

        return forwardToObjectDetails(mapping, request.getParameter("id"), trail);
    }

    /**
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws an exception
     */
    public ActionForward ajaxVerbosify(ActionMapping mapping, 
                                       @SuppressWarnings("unused") ActionForm form,
                                       HttpServletRequest request, 
                                       @SuppressWarnings("unused") HttpServletResponse response)
                    throws Exception {
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
        } else {
            return null;
        }
    }

    /**
     * Count number of results for a template on the object details page.
     *
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws an exception
     */
    public ActionForward ajaxTemplateCount(ActionMapping mapping, ActionForm form,
                                           HttpServletRequest request, HttpServletResponse response)
                    throws Exception {
        HttpSession session = request.getSession();
        ServletContext sc = session.getServletContext();
        String userName = ((Profile) session.getAttribute(Constants.PROFILE)).getUsername();
        String type = request.getParameter("type");
        String id = request.getParameter("id");
        String templateName = request.getParameter("template");
        String detailsType = request.getParameter("detailsType");
        ObjectStore os = (ObjectStore) sc.getAttribute(Constants.OBJECTSTORE);

        TemplateQuery tq = TemplateHelper.findTemplate(sc, session, userName, templateName, type);
        ComponentContext cc = new ComponentContext();

        if (detailsType.equals("object")) {
            InterMineObject o = os.getObjectById(new Integer(id));
            Map displayObjects = (Map) session.getAttribute(Constants.DISPLAY_OBJECT_CACHE);
            DisplayObject obj = (DisplayObject) displayObjects.get(o);
            cc.putAttribute("displayObject", obj);
            cc.putAttribute("templateQuery", tq);
            cc.putAttribute("placement", request.getParameter("placement"));
            Map fieldExprs = new HashMap();
            TemplateListHelper.getAspectTemplateForClass(request.getParameter("placement"), sc, o,
                                                         fieldExprs);
            cc.putAttribute("fieldExprMap", fieldExprs);
            new ObjectDetailsTemplateController().execute(cc, mapping, form, request, response);
            request.setAttribute("org.apache.struts.taglib.tiles.CompContext", cc);
            return mapping.findForward("objectDetailsTemplateTable");
        } else {
            Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
            InterMineBag interMineIdBag = (InterMineBag) profile.getSavedBags().get(id);
            cc.putAttribute("interMineIdBag", interMineIdBag);
            cc.putAttribute("templateQuery", tq);
            cc.putAttribute("placement", request.getParameter("placement"));
            Map fieldExprs = new HashMap();
            TemplateListHelper.getAspectTemplatesForType(request.getParameter("placement"), sc,
                                                         interMineIdBag, fieldExprs);
            cc.putAttribute("fieldExprMap", fieldExprs);
            new ObjectDetailsTemplateController().execute(cc, mapping, form, request, response);
            request.setAttribute("org.apache.struts.taglib.tiles.CompContext", cc);
            return mapping.findForward("objectDetailsTemplateTable");
        }
    }

    /**
     * For a dynamic class, find the class descriptor from which a field is derived
     *
     * @param clds
     *            the class descriptors for the dynamic class
     * @param fieldName
     *            the field name
     * @return the relevant class descriptor
     */
    protected ClassDescriptor cldContainingField(Set clds, String fieldName) {
        for (Iterator i = clds.iterator(); i.hasNext();) {
            ClassDescriptor cld = (ClassDescriptor) i.next();
            if (cld.getFieldDescriptorByName(fieldName) != null) {
                return cld;
            }
        }
        return null;
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
     * @param session
     *            the current http session
     * @param idString
     *            intermine object id
     * @return DisplayObject for the intermine object
     */
    protected DisplayObject getDisplayObject(HttpSession session, String idString) {
        Map displayObjects = (Map) session.getAttribute("displayObjects");
        return (DisplayObject) displayObjects.get(new Integer(idString));
    }
}
