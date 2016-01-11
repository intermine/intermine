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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.pathqueryresult.PathQueryResultHelper;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.config.WebConfig;


/**
 * Action that creates a table of collection elements for display in a table widget.
 *
 * @author unknown.
 */
public class CollectionTableAction extends Action
{
    /**
     * Create PagedTable for this collection, register it with an identifier
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
     *                if an error occurs
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        final HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        final ObjectStore os = im.getObjectStore();
        final WebConfig webConfig = SessionMethods.getWebConfig(request);
        final Integer id = new Integer(request.getParameter("id"));
        final String field = request.getParameter("field");

        InterMineObject o = os.getObjectById(id);
        String referencedClassName = getReferencedCD(os.getModel(), o, field).getUnqualifiedName();

        PathQuery collectionQuery = PathQueryResultHelper.makePathQueryForCollection(
                webConfig, os, o, referencedClassName, field);

        request.setAttribute("collectionQuery", collectionQuery);

        return mapping.findForward("table");
    }

    private ClassDescriptor getReferencedCD(Model m, InterMineObject imo, String fieldName) {
        for (ClassDescriptor cld : m.getClassDescriptorsForClass(imo.getClass())) {
            FieldDescriptor fd = cld.getFieldDescriptorByName(fieldName);
            if (fd != null) {
                if (!(fd instanceof ReferenceDescriptor)) {
                    throw new RuntimeException("Fields submitted for display in collection tables "
                            + " must be references, not attributes. BAD FIELD: " + fieldName);
                }
                return ((ReferenceDescriptor) fd).getReferencedClassDescriptor();
            }
        }
        throw new RuntimeException("Could not find a field named " + fieldName
                + " for an object of type: " + imo.getClass());
    }
}
