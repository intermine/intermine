package org.intermine.dwr;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.query.Query;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.web.Constants;
import org.intermine.web.Profile;
import org.intermine.web.ProfileManager;
import org.intermine.web.SavedQuery;
import org.intermine.web.SessionMethods;
import org.intermine.web.TemplateHelper;
import org.intermine.web.TemplateQuery;
import org.intermine.web.WebUtil;
import org.intermine.web.bag.InterMineBag;
import org.intermine.web.tagging.TagTypes;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import uk.ltd.getahead.dwr.WebContext;
import uk.ltd.getahead.dwr.WebContextFactory;

/**
 * This class contains the methods called through DWR Ajax
 *
 * @author Xavier Watkins
 *
 */
public class AjaxServices
{
    protected static final Logger LOG = Logger.getLogger(AjaxServices.class);

    /**
     * Creates a favourite Tag for the given templateName
     *
     * @param templateName
     *            the name of the template we want to set as a favourite
     */
    public void setFavouriteTemplate(String templateName) {
        WebContext ctx = WebContextFactory.get();
        HttpSession session = ctx.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        HttpServletRequest request = ctx.getHttpServletRequest();
        templateName = templateName.replaceAll("#039;", "'");
        ProfileManager pm = (ProfileManager) request.getSession().getServletContext().getAttribute(
                Constants.PROFILE_MANAGER);
        pm.addTag("favourite", templateName, TagTypes.TEMPLATE, profile.getUsername());
    }

    /**
     * Precomputes the given templat query
     * @param templateName the template query name
     * @return a String to guarantee the service ran properly
     */
    public String preCompute(String templateName) {
        WebContext ctx = WebContextFactory.get();
        HttpSession session = ctx.getSession();
        ServletContext servletContext = ctx.getServletContext();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        Map templates = profile.getSavedTemplates();
        TemplateQuery template = (TemplateQuery) templates.get(templateName);
        ObjectStoreInterMineImpl os = (ObjectStoreInterMineImpl) servletContext
                .getAttribute(Constants.OBJECTSTORE);
        List indexes = new ArrayList();
        Query query = TemplateHelper.getPrecomputeQuery(template, indexes);

        try {
            if (!os.isPrecomputed(query, "template")) {
                session.setAttribute("precomputing_" + templateName, "true");
                os.precompute(query, indexes, "template");
                session.removeAttribute("precomputing_" + templateName);
            }
        } catch (ObjectStoreException e) {
            LOG.error(e);
        }
        return ("precomputed");
    }
    
    /**
     * Rename a element such as history, name, bag
     * @param name the name of the element
     * @param type history, saved, bag
     * @param newName the new name for the element
     * @return the new name of the element as a String
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public String rename(String name, String type, String newName)
        throws Exception {
        WebContext ctx = WebContextFactory.get();
        HttpSession session = ctx.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        SavedQuery sq;
        if (name.equals(newName) || StringUtils.isEmpty(newName)) {
            return name;
        }
        if (type.equals("history")) {
            if (profile.getHistory().get(newName) != null) {
                return "<i>" + newName + " already exists</i>";
            }
            profile.renameHistory(name, newName);
        } else if (type.equals("saved")) {
            if (profile.getSavedQueries().get(newName) != null) {
                return "<i>" + newName + " already exists</i>";
            }
            sq = (SavedQuery) profile.getSavedQueries().get(name);
            profile.deleteQuery(sq.getName());
            sq = new SavedQuery(newName, sq.getDateCreated(), sq.getPathQuery());
            profile.saveQuery(sq.getName(), sq);
        } else if (type.equals("bag")) {
           if (profile.getSavedBags().get(newName) != null) {
               return "<i>" + newName + " already exists</i>";
           }
           InterMineBag bag = (InterMineBag) profile.getSavedBags().get(name);
           profile.deleteBag(name);
           SessionMethods.invalidateBagTable(session, name);
           int maxNotLoggedSize = WebUtil.getIntSessionProperty(session,
                                          "max.bag.size.notloggedin",
                                          Constants.MAX_NOT_LOGGED_BAG_SIZE);
           bag.setName(newName);
           profile.saveBag(newName, bag, maxNotLoggedSize);
        } else {
            return "Type unknown";
        }
        return newName;
    }
}
