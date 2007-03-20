package org.intermine.web.results;

/*
 * Copyright (C) 2002-2005 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.util.MessageResources;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.util.TypeUtil;
import org.intermine.web.Constants;
import org.intermine.web.Constraint;
import org.intermine.web.ForwardParameters;
import org.intermine.web.ObjectDetailsTemplateController;
import org.intermine.web.PathNode;
import org.intermine.web.PathQuery;
import org.intermine.web.Profile;
import org.intermine.web.QueryMonitorTimeout;
import org.intermine.web.SessionMethods;
import org.intermine.web.TemplateHelper;
import org.intermine.web.TemplateListHelper;
import org.intermine.web.TemplateQuery;
import org.intermine.web.bag.InterMineBag;

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
    public ActionForward runTemplate(ActionMapping mapping, ActionForm form,
                                     HttpServletRequest request, HttpServletResponse response)
                    throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        String name = request.getParameter("name");
        String type = request.getParameter("type");
        String bagName = request.getParameter("bagName");
        String useBagNode = request.getParameter("useBagNode");
        String userName = ((Profile) session.getAttribute(Constants.PROFILE)).getUsername();
        TemplateQuery template = TemplateHelper.findTemplate(servletContext, session, userName,
                                                             name, type);
        PathQuery query = (PathQuery) template.clone();
        String trail = request.getParameter("trail");
        
        for (Iterator i = template.getEditableNodes().iterator(); i.hasNext();) {
            PathNode node = (PathNode) i.next();
            PathNode nodeCopy = (PathNode) query.getNodes().get(node.getPath());

            name = TypeUtil.unqualifiedName(node.getParentType());

            // object details page - fill in identified constraint with value from object
            if (bagName == null || bagName.length() == 0) {
                for (int ci = 0; ci < node.getConstraints().size(); ci++) {
                    Constraint c = (Constraint) node.getConstraint(ci);
                    if (c.getIdentifier() != null) {
                        // If special request parameter key is present then we initialise
                        // the form bean with the parameter value
                        String paramName = c.getIdentifier() + "_value";
                        String constraintValue = request.getParameter(paramName);
                        if (constraintValue != null) {
                            nodeCopy
                                    .setConstraintValue(nodeCopy.getConstraint(ci), constraintValue);
                        }
                    }
                }
            } else if (useBagNode != null) {// && name.equals(useBagNode)) {
                // bag details page - remove the identified constraint and constrain
                // its parent to be in the bag
                PathNode parent = (PathNode) query.getNodes().get(nodeCopy.getParent().getPath());
                for (int ci = 0; ci < node.getConstraints().size(); ci++) {
                    Constraint c = (Constraint) node.getConstraint(ci);
                    if (c.getIdentifier() != null) {
                        // Constraint c = (Constraint) node.getConstraint(0);
                        ConstraintOp constraintOp = ConstraintOp.IN;
                        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
                        InterMineBag interMineIdBag = (InterMineBag) profile
                                                                            .getSavedBags()
                                                                            .get(bagName);
                        Constraint bagConstraint = new Constraint(constraintOp, interMineIdBag,
                                                                  true, c.getDescription(),
                                                                  c.getCode(), c.getIdentifier());
                        parent.getConstraints().add(bagConstraint);

                        // remove the constraint on this node, possibly remove node
                        if (nodeCopy.getConstraints().size() == 1) {
                            query.getNodes().remove(nodeCopy.getPath());
                        } else {
                            nodeCopy.getConstraints().remove(node.getConstraints().indexOf(c));
                        }
                    }
                }
            }
        }
        SessionMethods.loadQuery(query, request.getSession(), response);
        QueryMonitorTimeout clientState = new QueryMonitorTimeout(
                                                                  Constants.QUERY_TIMEOUT_SECONDS * 1000);
        MessageResources messages = (MessageResources) request.getAttribute(Globals.MESSAGES_KEY);
        String qid = SessionMethods.startQuery(clientState, session, messages, false);
        Thread.sleep(200); // slight pause in the hope of avoiding holding page
        return new ForwardParameters(mapping.findForward("waiting"))
                                                                    .addParameter("qid", qid)
                                                                    .addParameter("trail", trail)
                                                                    .forward();
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
    public ActionForward verbosify(ActionMapping mapping, ActionForm form,
                                   HttpServletRequest request, HttpServletResponse response)
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
    public ActionForward unverbosify(ActionMapping mapping, ActionForm form,
                                     HttpServletRequest request, HttpServletResponse response)
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
    public ActionForward ajaxVerbosify(ActionMapping mapping, ActionForm form,
                                       HttpServletRequest request, HttpServletResponse response)
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
