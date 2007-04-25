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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.path.Path;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Action that creates a table of collection elements for display.
 *
 * @author Kim Rutherford
 * @author Thomas Riley
 */
public class CollectionDetailsAction extends Action
{
    private static int index = 0;

    /**
     * Create PagedTable for this collection, register it with an identifier and
     * redirect to results.do?table=identifier
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if an error occurs
     */
    public ActionForward execute(ActionMapping mapping,
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
        String trail = request.getParameter("trail");
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

        Collection c;

        if (refDesc instanceof CollectionDescriptor) {
            c = (Collection) TypeUtil.getFieldValue(o, field);
        } else {
            c = Collections.singletonList(TypeUtil.getFieldValue(o, field));
        }

        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
        String referencedClassName = TypeUtil.unqualifiedName(refDesc.getReferencedClassName());
        WebPathCollection webPathCollection = 
            new WebPathCollection(os, new Path(model, referencedClassName), c, model, webConfig,
                              classKeys);
        PagedTable pc = new PagedTable(webPathCollection);
        String identifier = "col" + index++;
        SessionMethods.setResultsTable(session, identifier, pc);
        
        // add results table to trail 
        if (trail != null) {
            trail += "|results." + identifier;
        } else {
            trail = "|results." + identifier;
        }
        
        return new ForwardParameters(mapping.findForward("results"))
                        .addParameter("table", identifier)
                        .addParameter("size", pageSize)
                        .addParameter("trail", trail).forward();
    }
}
