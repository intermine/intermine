package org.flymine.web;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;

import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.presentation.DisplayModel;
import org.flymine.objectstore.query.Query;

/**
 * Perform initialisation steps for query editing tile prior to calling query.jsp
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class QueryBuildController extends TilesAction
{
    /**
     * @see TilesAction#execute
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();

        Map queryClasses = (Map) session.getAttribute("queryClasses");
        String editingAlias = (String) session.getAttribute("editingAlias");
        Map savedBagsInverse = (Map) session.getAttribute("savedBagsInverse");
        Model model = ((DisplayModel) session.getAttribute("model")).getModel();
        Query q = (Query) session.getAttribute("query");

        if (queryClasses == null) {
            session.setAttribute("queryClasses", new LinkedHashMap());
        }
        
        //there's a query on the session but it hasn't been rendered yet
        if (q != null) {
            queryClasses = QueryBuildHelper.getQueryClasses(q, savedBagsInverse);

            session.setAttribute("queryClasses", queryClasses);
            session.setAttribute("query", null);
        }
    
        //we are editing a QueryClass - render it as a form
        if (editingAlias != null) {
            String type = ((DisplayQueryClass) queryClasses.get(editingAlias)).getType();
            ClassDescriptor cld = model.getClassDescriptorByName(type);
            
            DisplayQueryClass d = (DisplayQueryClass) queryClasses.get(editingAlias);
            QueryBuildForm qbf = (QueryBuildForm) form;
            qbf.setFieldOps(QueryBuildHelper.toStrings(d.getFieldOps()));
            qbf.setFieldValues(QueryBuildHelper.toStrings(d.getFieldValues()));
            
            session.setAttribute("validOps", QueryBuildHelper.getValidOps(cld, false));
            List allFieldNames = QueryBuildHelper.getAllFieldNames(cld);
            if (allFieldNames.size() == 0) {
                session.setAttribute("allFieldNames", null);
            } else {
                session.setAttribute("allFieldNames", allFieldNames);
            }
        }

        return null;
    }
}

