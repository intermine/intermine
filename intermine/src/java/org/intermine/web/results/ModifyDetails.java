package org.intermine.web.results;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.actions.DispatchAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.*;
import org.intermine.web.Constants;

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

        ((DisplayObject) session.getAttribute("object")).setVerbosity(fieldName, true);

        return mapping.findForward("objectDetails");
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

        ((DisplayObject) session.getAttribute("object")).setVerbosity(fieldName, false);

        return mapping.findForward("objectDetails");
    }

    /**
     * Filter a collection by selecting only the elements of certain types
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
     public ActionForward filter(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
         throws Exception {
         HttpSession session = request.getSession();
         ServletContext servletContext = session.getServletContext();
         ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
         String fieldName = request.getParameter("field");
         int index = new Integer(request.getParameter("index")).intValue();

         DisplayObject object = (DisplayObject) session.getAttribute("object");
         DisplayCollection collection = (DisplayCollection) object.getCollections().get(fieldName);

         //e.g. all the broke departments in the "departments" field of a company
         //select a from (Department, Broke) as a, Company
         //where Company.departments contains a and Company.id=24000006;
         //i.e. select qc1 from qc1, qc2 where cc and sc
         Set clds = (Set) new ArrayList(collection.getClasses().keySet()).get(index);
         Set types = new HashSet();
         for (Iterator i = clds.iterator(); i.hasNext();) {
             types.add(((ClassDescriptor) i.next()).getType());
         }
         QueryClass qc1 = new QueryClass(types);

         ClassDescriptor cld = cldContainingField(object.getClds(), fieldName);
         QueryClass qc2 = new QueryClass(cld.getType());

         ContainsConstraint cc = 
             new ContainsConstraint(new QueryCollectionReference(qc2, fieldName),
                                    ConstraintOp.CONTAINS,
                                    qc1);
         SimpleConstraint sc = new SimpleConstraint(new QueryField(qc2, "id"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue(new Integer(object.getId())));

         ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
         cs.addConstraint(cc);
         cs.addConstraint(sc);

         Query q = new Query();
         q.addToSelect(qc1);
         q.addFrom(qc1);
         q.addFrom(qc2);
         q.setConstraint(cs);

         session.setAttribute(Constants.RESULTS_TABLE, TableHelper.makeTable(os, q));
         
         return mapping.findForward("results");
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
}