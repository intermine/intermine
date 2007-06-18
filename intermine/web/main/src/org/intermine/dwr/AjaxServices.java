package org.intermine.dwr;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.Results;

import org.intermine.InterMineException;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.path.Path;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.query.QueryMonitorTimeout;
import org.intermine.web.logic.query.SavedQuery;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.WebResultsSimple;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateQuery;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.Globals;
import org.apache.struts.util.MessageResources;

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
     * Precomputes the given template query
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
        Query query = TemplateHelper.getPrecomputeQuery(template, indexes, null);

        try {
            if (!os.isPrecomputed(query, "template")) {
                session.setAttribute("precomputing_" + templateName, "true");
                os.precompute(query, indexes, "template");
            }
        } catch (ObjectStoreException e) {
            LOG.error(e);
        } finally {
            session.removeAttribute("precomputing_" + templateName);
        }
        return "precomputed";
    }

    /**
     * Summarises the given template query.
     *
     * @param templateName the template query name
     * @return a String to guarantee the service ran properly
     */
    public String summarise(String templateName) {
        WebContext ctx = WebContextFactory.get();
        HttpSession session = ctx.getSession();
        ServletContext servletContext = ctx.getServletContext();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        Map templates = profile.getSavedTemplates();
        TemplateQuery template = (TemplateQuery) templates.get(templateName);
        ObjectStoreInterMineImpl os = (ObjectStoreInterMineImpl) servletContext
                .getAttribute(Constants.OBJECTSTORE);
        ObjectStoreWriter osw = ((ProfileManager) servletContext.getAttribute(
                    Constants.PROFILE_MANAGER)).getUserProfileObjectStore();
        try {
            session.setAttribute("summarising_" + templateName, "true");
            template.summarise(os, osw);
        } catch (ObjectStoreException e) {
            LOG.error("Failed to summarise " + templateName, e);
        } catch (NullPointerException e) {
            NullPointerException e2 = new NullPointerException("No such template " + templateName);
            e2.initCause(e);
            throw e2;
        } finally {
            session.removeAttribute("summarising_" + templateName);
        }
        return "summarised";
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
        newName = newName.trim();
        WebContext ctx = WebContextFactory.get();
        HttpSession session = ctx.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ServletContext servletContext = ctx.getServletContext();
        ObjectStoreWriter uosw = ((ProfileManager) servletContext.getAttribute(
                    Constants.PROFILE_MANAGER)).getUserProfileObjectStore();
        SavedQuery sq;
        if (name.equals(newName) || StringUtils.isEmpty(newName)) {
            return name;
        }
        if (!WebUtil.isValidName(newName)) {       
            String errorMsg = "<i>Invalid name.  Names may only contain letters, "
                              + "numbers, spaces, and underscores.</i>";           
            return errorMsg;
        }
        if (type.equals("history")) {
            if (profile.getHistory().get(name) == null) {
                return "<i>" + name + " does not exist</i>";
            }
            if (profile.getHistory().get(newName) != null) {
                return "<i>" + newName + " already exists</i>";
            }
            profile.renameHistory(name, newName);
        } else if (type.equals("saved")) {
            if (profile.getSavedQueries().get(name) == null) {
                return "<i>" + name + " does not exist</i>";
            }
            if (profile.getSavedQueries().get(newName) != null) {
                return "<i>" + newName + " already exists</i>";
            }
            sq = profile.getSavedQueries().get(name);
            profile.deleteQuery(sq.getName());
            sq = new SavedQuery(newName, sq.getDateCreated(), sq.getPathQuery());
            profile.saveQuery(sq.getName(), sq);
        } else if (type.equals("bag")) {
            if (profile.getSavedBags().get(name) == null) {
                return "<i>" + name + " does not exist</i>";
            }
            if (profile.getSavedBags().get(newName) != null) {
                return "<i>" + newName + " already exists</i>";
            }
            InterMineBag bag = profile.getSavedBags().get(name);
            bag.setName(newName, uosw);
            profile.deleteBag(name);
            profile.saveBag(newName, bag);
        } else {
            return "Type unknown";
        }
        return newName;
    }
    
    /**
     * For a given bag, set its description
     * @param bagName the bag
     * @param description the desciprion as entered by the user
     * @return the description for display on the jsp page
     * @throws Exception an exception
     */
    public String saveBagDescription(String bagName, String description) throws Exception {
        WebContext ctx = WebContextFactory.get();
        HttpSession session = ctx.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ServletContext servletContext = ctx.getServletContext();
        ObjectStoreWriter uosw = ((ProfileManager) servletContext.getAttribute(
                    Constants.PROFILE_MANAGER)).getUserProfileObjectStore();
        InterMineBag bag = (InterMineBag) profile.getSavedBags().get(bagName);
        if (bag == null) {
            throw new InterMineException("Bag \"" + bagName + "\" not found.");
        }
        bag.setDescription(description, uosw);
        profile.getSearchRepository().descriptionChanged(bag);
        return description;
    }

    /**
     * Set the description of a view path.
     * @param pathString the string representation of the path
     * @param description the new description
     * @return the description, or null if the description was empty
     */
    public String changeViewPathDescription(String pathString, String description) {
        if (description.trim().length() == 0) {
            description = null;
        }
        WebContext ctx = WebContextFactory.get();
        HttpSession session = ctx.getSession();
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        Path path = MainHelper.makePath(query.getModel(), query, pathString);
        Path prefixPath = path.getPrefix();
        if (description == null) {
            query.getPathDescriptions().remove(prefixPath);
        } else {
            query.getPathDescriptions().put(prefixPath, description);
        }
        return description;
    }
    
    /**
     * Get the summary for the given column
     * @param summaryPath the path for the column as a String
     * @return a collection of rows
     * @throws Exception an exception
     */
    public static List getColumnSummary(String summaryPath) throws Exception {
        WebContext ctx = WebContextFactory.get();
        HttpSession session = ctx.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);

        PathQuery pathQuery = (PathQuery) session.getAttribute(Constants.QUERY);
        Profile currentProfile = (Profile) session.getAttribute(Constants.PROFILE);
        Query distinctQuery = MainHelper.makeSummaryQuery(pathQuery, currentProfile.getSavedBags(),
                                                  new HashMap<String, QueryNode>(), summaryPath
                                                  , servletContext);
        
        Results results = os.execute(distinctQuery);
        List columns = Arrays.asList(new String[] {"col1", "col2"});
        WebResultsSimple webResults = new WebResultsSimple(results, columns);
        PagedTable pagedTable = new PagedTable(webResults);
        
        // Start the count of results
        QueryMonitorTimeout clientState
        = new QueryMonitorTimeout(Constants.QUERY_TIMEOUT_SECONDS * 1000);
        MessageResources messages = (MessageResources) ctx.getHttpServletRequest()
                                                          .getAttribute(Globals.MESSAGES_KEY);
        Query countQuery =
            MainHelper.makeSummaryQuery(pathQuery, currentProfile.getSavedBags(),
                                        new HashMap<String, QueryNode>(), summaryPath
                                        , servletContext);
        String qid = SessionMethods.startQueryCount(clientState, session, messages, countQuery);
        return Arrays.asList(new Object[] {pagedTable.getRows(), qid});
    }
    
    /**
     * Return the results from the query with the given query id.  If the results aren't yet
     * available, return null.  The returned List is the visible rows from the PagedTable associated
     * with the query id.
     * @param qid the id
     * @return the current rows from the table
     */
    public static List getResults(String qid) {
        // results to return if there is an internal error
        List<List<String>> unavailableListList = new ArrayList();
        ArrayList<String> unavailableList = new ArrayList<String>();
        unavailableList.add("results unavailable");
        unavailableListList.add(unavailableList);

        if (StringUtils.isEmpty(qid)) {
            return unavailableListList;
        }
        WebContext ctx = WebContextFactory.get();
        HttpSession session = ctx.getSession();
        QueryMonitorTimeout controller = (QueryMonitorTimeout)
            SessionMethods.getRunningQueryController(qid, session);

        // First tickle the controller to avoid timeout
        controller.tickle();

        if (controller.isCancelledWithError()) {
            LOG.debug("query qid " + qid + " error");
            
            return unavailableListList;
        } else if (controller.isCancelled()) {
            LOG.debug("query qid " + qid + " cancelled");
            return unavailableListList;
        } else if (controller.isCompleted()) {
            LOG.debug("query qid " + qid + " complete");
            // Look at results, if only one result, go straight to object details page
            PagedTable pr = SessionMethods.getResultsTable(session, "results." + qid);
            return pr.getRows();
        } else {
            // query still running
            LOG.debug("query qid " + qid + " still running, making client wait");
            return null;
        }
    }
}
