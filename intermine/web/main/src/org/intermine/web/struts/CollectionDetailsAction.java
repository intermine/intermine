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
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Action that creates a table of collection elements for display.
 *
 * @author Kim Rutherford
 * @author Thomas Riley
 */
@SuppressWarnings("deprecation")
public class CollectionDetailsAction extends Action
{
    /**
     * Create PagedTable for this collection, register it with an identifier and redirect to
     * results.do?table=identifier
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
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        ObjectStore os = im.getObjectStore();

        Integer id = new Integer(request.getParameter("id"));
        String field = request.getParameter("field");
        String trail = request.getParameter("trail");

        InterMineObject o = os.getObjectById(id);

        ReferenceDescriptor refDesc = null;
        for (ClassDescriptor cld : os.getModel().getClassDescriptorsForClass(o.getClass())) {
            refDesc = (ReferenceDescriptor) cld.getFieldDescriptorByName(field);
            if (refDesc != null) {
                break;
            }
        }
        String referencedClassName = refDesc.getReferencedClassDescriptor().getUnqualifiedName();

        PagedTable pagedTable = SessionMethods.doQueryGetPagedTable(request, o, field,
                                                                    referencedClassName);

        // add results table to trail
        if (trail != null) {
            trail += "|results." + pagedTable.getTableid();
        } else {
            trail = "|results." + pagedTable.getTableid();
        }

        return new ForwardParameters(mapping.findForward("results")).addParameter("noSelect",
                        "true").addParameter("table", pagedTable.getTableid())
                        .addParameter("trail", trail).forward();
    }
}
