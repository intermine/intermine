package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Constructor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.widget.GraphCategoryURLGenerator;

/**
 * Action class to run an IQL query and constraint the results to
 * be in a bag, allowing the results to be displayed
 *
 * @author Xavier Watkins
 *
 */
public class QueryForGraphAction extends InterMineAction
{

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
    public ActionForward execute(ActionMapping mapping, @SuppressWarnings("unused") ActionForm form,
            HttpServletRequest request, @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        ObjectStore os = im.getObjectStore();
        BagManager bagManager = im.getBagManager();

        String bagName = request.getParameter("bagName");
        String urlGen = request.getParameter("urlGen");
        String series = request.getParameter("series");
        String category = request.getParameter("category");
        String extraKey = request.getParameter("extraKey"); // organism

        InterMineBag bag;

        /* get bag from user profile */
        Profile profile = SessionMethods.getProfile(session);
        bag = bagManager.getUserOrGlobalBag(profile, bagName);

        /* its all gone horribly wrong, no one has the bag! */
        if (bag == null) {
            return null;
        }
        Class clazz = TypeUtil.instantiate(urlGen);
        GraphCategoryURLGenerator urlGenerator = null;
        if (extraKey != null) {
            Constructor constr = clazz.getConstructor(new Class[] {String.class, String.class});
            urlGenerator = (GraphCategoryURLGenerator) constr.newInstance(new Object[] {bagName,
                extraKey});
        } else {
            Constructor constr = clazz.getConstructor(new Class[] {String.class});
            urlGenerator = (GraphCategoryURLGenerator) constr.newInstance(new Object[] {bagName});
        }
        PathQuery pathQuery = urlGenerator.generatePathQuery(os, bag, category, series);
        SessionMethods.loadQuery(pathQuery, session, response);
        String qid = SessionMethods.startQueryWithTimeout(request, true, pathQuery);

        Thread.sleep(200); // slight pause in the hope of avoiding holding page

        return new ForwardParameters(mapping.findForward("waiting"))
            .addParameter("trail", "|bag." + bagName)
            .addParameter("qid", qid).forward();
    }
}
