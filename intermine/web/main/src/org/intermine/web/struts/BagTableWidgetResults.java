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

import java.util.Map;

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SimpleConstraint;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.path.Path;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * @author Xavier Watkins
 *
 */
public class BagTableWidgetResults extends InterMineAction
{
    private static int index = 0;
    
    /**
     * Runs the query as defined in the BagTableWidgetLoader
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
                    throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Model model = os.getModel();
        
        String typeA = request.getParameter("typeA");
        String typeB = request.getParameter("typeB");
        String reverseCollection = request.getParameter("collection");
        String bagName = request.getParameter("bagName");
        String id = request.getParameter("id");
        

        Profile currentProfile = (Profile) session.getAttribute(Constants.PROFILE);
        Map<String, InterMineBag> allBags =
            WebUtil.getAllBags(currentProfile.getSavedBags(), servletContext);
        InterMineBag bag = allBags.get(bagName);

        Query q = new Query();
        
        Class clazzA = Class.forName(model.getPackageName() + "." + typeA);
        Class clazzB = Class.forName(model.getPackageName() + "." + typeB);

        QueryClass qClassA = new QueryClass(clazzA);
        QueryClass qClassB = new QueryClass(clazzB);
        q.addFrom(qClassA);
        q.addFrom(qClassB);
        
        q.addToSelect(qClassA);
        
        ConstraintSet cstSet = new ConstraintSet(ConstraintOp.AND);
        QueryReference qr;
        try {
            qr = new QueryCollectionReference(qClassA, reverseCollection);
        } catch (Exception e) {
            qr = new QueryObjectReference(qClassA, reverseCollection);
        }
        ContainsConstraint cstr = new ContainsConstraint(qr, ConstraintOp.CONTAINS, qClassB);
        cstSet.addConstraint(cstr);
        
        SimpleConstraint simpleConstraint = new SimpleConstraint(new QueryField(qClassB, "id"),
                                                                 ConstraintOp.EQUALS,
                                                                 new QueryValue(new Integer(id)));
        cstSet.addConstraint(simpleConstraint);

        QueryField qf = new QueryField(qClassA, "id");
        BagConstraint bagCstr = new BagConstraint(qf, ConstraintOp.IN, bag.getOsb());
        cstSet.addConstraint(bagCstr);
        
        q.setConstraint(cstSet);

        Results results = os.execute(q);

        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
        WebPathCollection webPathCollection = 
            new WebPathCollection(os, new Path(model, bag.getType()), results, model, webConfig,
                              classKeys);
        PagedTable pagedColl = new PagedTable(webPathCollection);
        String identifier = "qid" + index++;

        SessionMethods.setResultsTable(session, identifier, pagedColl);
        
        return new ForwardParameters(mapping.findForward("results"))
                        .addParameter("table", identifier)
                        .addParameter("size", "10")
                        .addParameter("trail", "|bag." + bagName).forward();
    }

}
