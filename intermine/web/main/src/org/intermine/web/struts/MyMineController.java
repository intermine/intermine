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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.ClassKeyHelper;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.query.SavedQuery;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.tagging.TagTypes;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.iterators.IteratorChain;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * Tiles controller for history tile (page).
 *
 * @author Thomas Riley
 */
public class MyMineController extends TilesAction
{
    /**
     * Set up attributes for the myMine page.
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Model model = ((ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE)).getModel();
        ProfileManager pm = SessionMethods.getProfileManager(servletContext);
        String page = request.getParameter("page");

        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        
        if (page != null && !profile.isLoggedIn()) {
            page = "bags";
        }
        
        if (!StringUtils.isEmpty(page)) {
            session.setAttribute(Constants.MYMINE_PAGE, page);
        }

        if (page != null) {
            if (page.equals("templates")) {
                // prime the tags cache so that the templates tags will be quick to access
                String userName = ((Profile) session.getAttribute(Constants.PROFILE)).getUsername();
                if (userName != null) {
                    // discard result
                    pm.getTags(null, null, TagTypes.TEMPLATE, userName);
                }
            }
        }


        request.setAttribute("queryAgeClasses", getQueryAgeClasses(profile.getSavedQueries()));

        return null;
    }

    /**
     * Return a Map from query name to css class to use on the div/tr/td displaying the query.
     * @param queries a Map from query name to SavedQuery
     * @return a Map from query name to CSS class
     */
    static Map<String, String> getQueryAgeClasses(Map<String, SavedQuery> queries) {
        Map<String, String> ageClassMap = new HashMap<String, String>();
        for (Map.Entry<String, SavedQuery> entry: queries.entrySet()) {
            String queryName = entry.getKey();
            ageClassMap.put(queryName, getCSSClassForAge(entry.getValue().getDateCreated()));
        }
        return ageClassMap;
    }

    /**
     * For a given Date, return a CSS class to when displaying objects of that age.
     * @param date the Date
     * @return a css class
     */
    static String getCSSClassForAge(Date date) {
        if (date == null) {
            // give up
            return "queryAgeOld";
        }
        Date currentDate = new Date();
        long age = (currentDate.getTime() - date.getTime()) / 1000;
        if (age < 10 * 60) {
            // 10 minutes
            return "queryAgeYoung";
        } else {
            if (age < 60 * 60 * 12) {
                // today (-ish)
                return "queryAgeToday";                    
            } else {
                return "queryAgeOld";
            }
        }
    }

}
