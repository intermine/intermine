package org.flymine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
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

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.FromElement;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.iql.IqlQuery;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.path.Path;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.ForwardParameters;
import org.intermine.web.struts.InterMineAction;
import org.intermine.web.struts.WebPathCollection;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Action class to run an IQL query and constraint the results to
 * be in a bag, allowing the results to be displayed
 * 
 * @author Xavier Watkins
 * @author Julie Sullivan
 */
public class OrthologueAction extends InterMineAction
{
    private static int index = 0;
    
    /**
     * Action class to run an IQL query and constraint the results to
     * be in a bag, allowing the results to be displayed
     * 
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
                    throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Profile currentProfile = (Profile) session.getAttribute(Constants.PROFILE);
        
        String bagName = request.getParameter("bagName");
        String queryString = request.getParameter("query");
        String id = request.getParameter("id");
        String columnName = "Gene";
        // if we have an ID, we don't need to rebuild query, just move along to the details page
        if (id != null && id.length() > 0) {
            return new ForwardParameters(mapping.findForward("details"))
                .addParameter("id", id)
                .addParameter("size", "10")
                .addParameter("trail", "").forward();
        }
                
        // rebuild query with IDs from bag
        IqlQuery iqlQuery = new IqlQuery(queryString, os.getModel().getPackageName());
        Query query = iqlQuery.toQuery();        
        InterMineBag bag = (InterMineBag) currentProfile.getSavedBags().get(bagName);
        Set queryFrom = query.getFrom();
        QueryClass queryClass = null;
        if (bag != null && !(bag.size() == 0)) {
            for (Iterator iter = queryFrom.iterator(); iter.hasNext();) {
                FromElement fromElt = (FromElement) iter.next();
                if ((fromElt instanceof QueryClass)
                        && (((QueryClass) fromElt).getType().isAssignableFrom((Class
                                    .forName(os.getModel().getPackageName() 
                                        + "." + bag.getType()))))) {
                    queryClass = (QueryClass) fromElt;
                }
            }
            QueryField qf = new QueryField(queryClass, "id");
            ((ConstraintSet) query.getConstraint())
                .addConstraint(new BagConstraint(qf, ConstraintOp.IN, bag.getOsb()));
            columnName = bag.getType();
        }
        Results results = os.execute(query);

        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
        Model model = os.getModel();
        WebPathCollection webPathCollection = 
            new WebPathCollection(os, new Path(model, columnName), results, model, webConfig,
                              classKeys);
        PagedTable pagedColl = new PagedTable(webPathCollection);
        String identifier = "qid" + index++;
        SessionMethods.setResultsTable(session, identifier, pagedColl);
        
        return new ForwardParameters(mapping.findForward("results"))
            .addParameter("table", identifier)
            .addParameter("size", "10")
            .addParameter("trail", "").forward();    
         
    }

}
