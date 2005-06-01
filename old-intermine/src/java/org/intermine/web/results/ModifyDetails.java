package org.intermine.web.results;

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
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.web.ForwardParameters;

/**
 * Action to handle events from the object details page
 * @author Mark Woodbridge
 */
public class ModifyDetails extends DispatchAction
{
    
    /**
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward verbosify(ActionMapping mapping,
                                   ActionForm form,
                                   HttpServletRequest request,
                                   HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        String fieldName = request.getParameter("field");
        String trail = request.getParameter("trail");
        DisplayObject object = getDisplayObject(session, request.getParameter("id"));
        
        if (object != null) {
            object.setVerbosity(fieldName, true);
        }
        
        return forwardToObjectDetails(mapping, request.getParameter("id"), trail);
    }

    /**
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward unverbosify(ActionMapping mapping,
                                     ActionForm form,
                                     HttpServletRequest request,
                                     HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        String fieldName = request.getParameter("field");
        String trail = request.getParameter("trail");
        DisplayObject object = getDisplayObject(session, request.getParameter("id"));
        
        object.setVerbosity(fieldName, false);

        return forwardToObjectDetails(mapping, request.getParameter("id"), trail);
    }
    
    /**
     * For a dynamic class, find the class descriptor from which a field is derived
     * @param clds the class descriptors for the dynamic class
     * @param fieldName the field name
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
     * @param session the current http session
     * @param idString intermine object id
     * @return DisplayObject for the intermine object
     */
    protected DisplayObject getDisplayObject(HttpSession session, String idString) {
        Map displayObjects = (Map) session.getAttribute("displayObjects");
        return (DisplayObject) displayObjects.get(new Integer(idString));
    }
}