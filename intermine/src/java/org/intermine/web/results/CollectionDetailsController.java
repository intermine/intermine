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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.tiles.ComponentContext;

import org.intermine.objectstore.ObjectStore;
import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.util.TypeUtil;
import org.intermine.web.Constants;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.ArrayList;

/**
 * Implementation of <strong>TilesAction</strong>. Assembles data for
 * a Collection details view.
 *
 * @author Kim Rutherford
 */

public class CollectionDetailsController extends TilesAction
{
    /**
     * Set up session attributes for the Collection view page.
     *
     * @param context The Tiles ComponentContext
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if an error occurs
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Model model = os.getModel();
        Integer id = new Integer((String) request.getParameter("id"));
        String field = request.getParameter("field");
        String pageSize = request.getParameter("pageSize");

        Object o = os.getObjectById(id);

        Set cds = model.getClassDescriptorsForClass(o.getClass());

        ReferenceDescriptor refDesc = null;
        
        Iterator iter = cds.iterator();

        while (iter.hasNext()) {
            ClassDescriptor cd = (ClassDescriptor) iter.next();

            refDesc = (ReferenceDescriptor) cd.getFieldDescriptorByName(field);

            if (refDesc != null) {
                break;
            }
        }

        ClassDescriptor refClass = refDesc.getReferencedClassDescriptor();
        Collection c;
        Object fieldValue = TypeUtil.getFieldValue(o, field);

        if (fieldValue instanceof Collection) {
            c = (Collection) fieldValue;
        } else {
            c = new ArrayList();
            c.add(o);
        }

        PagedCollection pc = new PagedCollection(field, c, refClass);
        if (pageSize != null) {
            try {
                int pageSizeInt = Integer.parseInt(pageSize);
                ((ChangeResultsSizeForm) form).setPageSize(pageSize);
                pc.setPageSize(pageSizeInt);
            } catch (NumberFormatException e) {
                // ignore badly formatted numbers
            }
        }
        session.setAttribute(Constants.RESULTS_TABLE, pc);

        return null;
    }
}
