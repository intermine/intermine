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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;

import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
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

        Map queryClasses = (Map) session.getAttribute(Constants.QUERY_CLASSES);
        String editingAlias = (String) session.getAttribute(Constants.EDITING_ALIAS);
        Map savedBagsInverse = (Map) session.getAttribute(Constants.SAVED_BAGS_INVERSE);
        ServletContext servletContext = session.getServletContext();
        Model model = (Model) servletContext.getAttribute(Constants.MODEL);
        Query q = (Query) session.getAttribute(Constants.QUERY);

        if (queryClasses == null) {
            session.setAttribute(Constants.QUERY_CLASSES, new LinkedHashMap());
        }
        
        //there's a query on the session but it hasn't been rendered yet
        if (q != null) {
            queryClasses = QueryBuildHelper.getQueryClasses(q, savedBagsInverse);

            session.setAttribute(Constants.QUERY_CLASSES, queryClasses);
            session.setAttribute(Constants.QUERY, null);
        }

        QueryBuildForm qbf = (QueryBuildForm) form;
        //someone has started editing a class - populate the form
        if (qbf.getButton().startsWith("editClass")) {
            qbf.setNewClassName(editingAlias);
            DisplayQueryClass d = (DisplayQueryClass) queryClasses.get(editingAlias);
            QueryBuildHelper.populateForm(qbf, d);
            
            ClassDescriptor cld = model.getClassDescriptorByName(d.getType());
            boolean bagsPresent = savedBagsInverse != null && savedBagsInverse.size () != 0;
            session.setAttribute("validOps", QueryBuildHelper.getValidOps(cld, bagsPresent));
            
            session.setAttribute("allFieldNames", QueryBuildHelper.getAllFieldNames(cld));
            
            session.setAttribute("validAliases",
                                 QueryBuildHelper.getValidAliases(cld, queryClasses));
        }
        
        return null;
    }
}

