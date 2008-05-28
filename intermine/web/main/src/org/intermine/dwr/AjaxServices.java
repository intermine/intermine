package org.intermine.dwr;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.ParseException;
import org.apache.struts.Globals;
import org.apache.struts.util.MessageResources;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.intermine.InterMineException;
import org.intermine.metadata.Model;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.path.Path;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.BagHelper;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.BagQueryRunner;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.bag.TypeConverter;
import org.intermine.web.logic.config.Type;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.query.PageTableQueryMonitor;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.query.QueryMonitorTimeout;
import org.intermine.web.logic.query.SavedQuery;
import org.intermine.web.logic.results.GuiObject;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.WebResultsSimple;
import org.intermine.web.logic.results.WebTable;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.search.WebSearchable;
import org.intermine.web.logic.session.QueryCountQueryMonitor;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateQuery;
import org.intermine.web.logic.widget.EnrichmentWidget;
import org.intermine.web.logic.widget.GraphWidget;
import org.intermine.web.logic.widget.TableWidget;
import org.intermine.web.logic.widget.Widget;


/**
 * This class contains the methods called through DWR Ajax
 *
 * @author Xavier Watkins
 *
 */
public class AjaxServices
{
    protected static final Logger LOG = Logger.getLogger(AjaxServices.class);
    private static final Object ERROR_MSG = "Error happened during DWR ajax service.";

    /**
     * Creates a favourite Tag for the given templateName
     *
     * @param name the name of the template we want to set as a favourite
     * @param type type of tag (bag or template)
     * @param isFavourite whether or not this item is currently a favourite
     */
    public void setFavourite(String name, String type, boolean isFavourite) {
        try {
            WebContext ctx = WebContextFactory.get();
            HttpSession session = ctx.getSession();
            Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
            HttpServletRequest request = ctx.getHttpServletRequest();
            String nameCopy = name.replaceAll("#039;", "'");
            ProfileManager pm = (ProfileManager) request.getSession().
                getServletContext().getAttribute(Constants.PROFILE_MANAGER);

            // already a favourite.  turning off.
            if (isFavourite) {

                List<Tag> tags;
                Tag tag;
                if (type.equals(TagTypes.TEMPLATE)) {
                    tags = pm.getTags("favourite", nameCopy, TagTypes.TEMPLATE,
                            profile.getUsername());
                } else if (type.equals(TagTypes.BAG)) {
                    tags = pm.getTags("favourite", nameCopy, TagTypes.BAG, profile.getUsername());
                } else {
                    throw new RuntimeException("Unknown tag type.");
                }
                if (tags.isEmpty()) {
                    throw new RuntimeException("User tried to mark a non-existent template "
                                               + "as favourite");
                }
                tag = tags.get(0);
                pm.deleteTag(tag);
            // not a favourite.  turning on.
            } else {
                if (type.equals(TagTypes.TEMPLATE)) {
                    pm.addTag("favourite", nameCopy, TagTypes.TEMPLATE, profile.getUsername());
                } else if (type.equals(TagTypes.BAG)) {
                    pm.addTag("favourite", nameCopy, TagTypes.BAG, profile.getUsername());
                } else {
                    throw new RuntimeException("Unknown tag type.");
                }
            }
        } catch (RuntimeException e) {
            processException(e);
        }
    }

    private static void processException(Exception e) {
        LOG.error(ERROR_MSG, e);
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        } else {
            throw new RuntimeException(e);
        }
    }

    /**
     * Precomputes the given template query
     * @param templateName the template query name
     * @return a String to guarantee the service ran properly
     */
    public String preCompute(String templateName) {
        try {
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
        } catch (RuntimeException e) {
            processException(e);
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
        try {
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
                NullPointerException e2 = new NullPointerException("No such template "
                        + templateName);
                e2.initCause(e);
                throw e2;
            } finally {
                session.removeAttribute("summarising_" + templateName);
            }
        } catch (RuntimeException e) {
            processException(e);
        }
        return "summarised";
    }

    /**
     * Rename a element such as history, name, bag
     * @param name the name of the element
     * @param type history, saved, bag
     * @param reName the new name for the element
     * @return the new name of the element as a String
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public String rename(String name, String type, String reName)
        throws Exception {
        String newName;
        try {
            newName = reName.trim();
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
            // TODO get error text from properties file
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
        } catch (RuntimeException e) {
            processException(e);
            return null;
        }
    }

    /**
     * For a given bag, set its description
     * @param bagName the bag
     * @param description the desciprion as entered by the user
     * @return the description for display on the jsp page
     * @throws Exception an exception
     */
    public String saveBagDescription(String bagName, String description) throws Exception {
        try {
            WebContext ctx = WebContextFactory.get();
            HttpSession session = ctx.getSession();
            Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
            ServletContext servletContext = ctx.getServletContext();
            ObjectStoreWriter uosw = ((ProfileManager) servletContext.getAttribute(
                        Constants.PROFILE_MANAGER)).getUserProfileObjectStore();
            InterMineBag bag = profile.getSavedBags().get(bagName);
            if (bag == null) {
                throw new InterMineException("List \"" + bagName + "\" not found.");
            }
            bag.setDescription(description, uosw);
            profile.getSearchRepository().descriptionChanged(bag);
            return description;
        } catch (RuntimeException e) {
            processException(e);
            return null;
        }
    }

    /**
     * Set the description of a view path.
     * @param pathString the string representation of the path
     * @param description the new description
     * @return the description, or null if the description was empty
     */
    public String changeViewPathDescription(String pathString, String description) {
        try {
            String descr = description;
            if (description.trim().length() == 0) {
                descr = null;
            }
            WebContext ctx = WebContextFactory.get();
            HttpSession session = ctx.getSession();
            PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
            Path path = MainHelper.makePath(query.getModel(), query, pathString);
            Path prefixPath = path.getPrefix();
            if (descr == null) {
                query.getPathDescriptions().remove(prefixPath);
            } else {
                query.getPathDescriptions().put(prefixPath, descr);
            }
            return descr;
        } catch (RuntimeException e) {
            processException(e);
            return null;
        }
    }

    /**
     * Get the summary for the given column
     * @param summaryPath the path for the column as a String
     * @param tableName name of column-owning table
     * @return a collection of rows
     * @throws Exception an exception
     */
    public static List getColumnSummary(String tableName, String summaryPath) throws Exception {
        try {
            WebContext ctx = WebContextFactory.get();
            HttpSession session = ctx.getSession();
            ServletContext servletContext = session.getServletContext();
            ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);

            WebTable webTable = (SessionMethods.getResultsTable(session, tableName))
                                   .getWebTable();
            PathQuery pathQuery = webTable.getPathQuery();

            SearchRepository globalRepository =
                (SearchRepository) servletContext.getAttribute(Constants.
                                                               GLOBAL_SEARCH_REPOSITORY);
            Profile currentProfile = (Profile) session.getAttribute(Constants.PROFILE);
            SearchRepository userSearchRepository = currentProfile.getSearchRepository();
            Map<String, InterMineBag> userWsMap =
                (Map<String, InterMineBag>) userSearchRepository.getWebSearchableMap(TagTypes.BAG);
            Map<String, InterMineBag> globalWsMap =
                (Map<String, InterMineBag>) globalRepository.getWebSearchableMap(TagTypes.BAG);
            Map<String, InterMineBag> allBags = new HashMap<String, InterMineBag>(userWsMap);
            allBags.putAll(globalWsMap);

            Query distinctQuery = MainHelper.makeSummaryQuery(pathQuery, allBags,
                    new HashMap<String, QuerySelectable>(), summaryPath, servletContext);

            Results results = os.execute(distinctQuery);
            WebResultsSimple webResults = new WebResultsSimple(results,
                        Arrays.asList(new String[] {"col1", "col2"}));
            PagedTable pagedTable = new PagedTable(webResults);

            // Start the count of results
            Query countQuery = MainHelper.makeSummaryQuery(pathQuery, allBags,
                    new HashMap<String, QuerySelectable>(), summaryPath, servletContext);
            QueryCountQueryMonitor clientState
                = new QueryCountQueryMonitor(Constants.QUERY_TIMEOUT_SECONDS * 1000, countQuery);
            MessageResources messages = (MessageResources) ctx.getHttpServletRequest()
                                                              .getAttribute(Globals.MESSAGES_KEY);
            String qid = SessionMethods.startQueryCount(clientState, session, messages);
            return Arrays.asList(new Object[] {
                        pagedTable.getRows(), qid, new Integer(pagedTable.getExactSize())
                    });
        } catch (RuntimeException e) {
            processException(e);
            return null;
        }
    }

    /**
     * Return the results from the query with the given query id.  If the results aren't yet
     * available, return null.  The returned List is the visible rows from the PagedTable associated
     * with the query id.
     * @param qid the id
     * @return the current rows from the table
     */
   /* public static List getResults(String qid) {
        // results to return if there is an internal error
        List<List<String>> unavailableListList = new ArrayList<List<String>>();
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
*/

    /**
     * Return the number of rows of results from the query with the given query id.  If the size
     * isn't yet available, return null.  The query must be started with
     * SessionMethods.startPagedTableCount().
     * @param qid the id
     * @return the row count or null if not yet available
     */
    public static Integer getResultsSize(String qid) {
        try {
            WebContext ctx = WebContextFactory.get();
            HttpSession session = ctx.getSession();
            QueryMonitorTimeout controller = (QueryMonitorTimeout)
                SessionMethods.getRunningQueryController(qid, session);

            // First tickle the controller to avoid timeout
            controller.tickle();

            if (controller.isCancelledWithError()) {
                LOG.debug("query qid " + qid + " error");

                return null;
            } else if (controller.isCancelled()) {
                LOG.debug("query qid " + qid + " cancelled");
                return null;
            } else if (controller.isCompleted()) {
                LOG.debug("query qid " + qid + " complete");

                if (controller instanceof PageTableQueryMonitor) {
                    PagedTable pt = ((PageTableQueryMonitor) controller).getPagedTable();
                    return pt.getExactSize();
                } else {
                    if (controller instanceof QueryCountQueryMonitor) {
                        return ((QueryCountQueryMonitor) controller).getCount();
                    } else {
                        LOG.debug("query qid " + qid + " - unknown controller type");
                        return null;
                    }
                }
            } else {
                // query still running
                LOG.debug("query qid " + qid + " still running, making client wait");
                return null;
            }
        } catch (RuntimeException e) {
            processException(e);
            return null;
        }
    }

    /**
     * Given a scope, type, tags and some filter text, produce a list of matching WebSearchable, in
     * a format useful in JavaScript.  Each element of the returned List is a List containing a
     * WebSearchable name, a score (from Lucene) and a string with the matching parts of the
     * description highlighted.
     * @param scope the scope (from TemplateHelper.GLOBAL_TEMPLATE or TemplateHelper.USER_TEMPLATE,
     * even though not all WebSearchables are templates)
     * @param type the type (from TagTypes)
     * @param tags the tags to filter on
     * @param filterText the text to pass to Lucene
     * @param filterAction toggles favourites filter off an on; will be blank or 'favourites'
     * @param callId unique id
     * @return a List of Lists
     */
    public static List<String> filterWebSearchables(String scope, String type,
                                                    List<String> tags, String filterText,
                                                    String filterAction, String callId) {
        try {
            ServletContext servletContext = WebContextFactory.get().getServletContext();
            ProfileManager pm = SessionMethods.getProfileManager(servletContext);
            HttpSession session = WebContextFactory.get().getSession();
            Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
            Map<String, WebSearchable> wsMap;
            Map<WebSearchable, Float> hitMap = new LinkedHashMap<WebSearchable, Float>();
            Map<WebSearchable, String> highlightedDescMap = new HashMap<WebSearchable, String>();

            if (filterText != null && filterText.length() > 1) {
                wsMap = new LinkedHashMap<String, WebSearchable>();
                Map<WebSearchable, String> scopeMap = new LinkedHashMap<WebSearchable, String>();
                try {
                    long time =
                        SearchRepository.runLeuceneSearch(filterText, scope, type, profile,
                                                        servletContext,
                                                        hitMap, scopeMap, null, highlightedDescMap);
                    LOG.info("Lucene search took " + time + " milliseconds");
                } catch (ParseException e) {
                    LOG.error("couldn't run lucene filter", e);
                    ArrayList<String> emptyList = new ArrayList<String>();
                    emptyList.add(callId);
                    return emptyList;
                } catch (IOException e) {
                    LOG.error("couldn't run lucene filter", e);
                    ArrayList<String> emptyList = new ArrayList<String>();
                    emptyList.add(callId);
                    return emptyList;
                }

                //long time = System.currentTimeMillis();

                for (WebSearchable ws: hitMap.keySet()) {
                    wsMap.put(ws.getName(), ws);
                }
            } else {

                if (scope.equals("user")) {
                    SearchRepository searchRepository = profile.getSearchRepository();
                    wsMap = (Map<String, WebSearchable>) searchRepository.getWebSearchableMap(type);
                } else {
                    SearchRepository globalRepository =
                        (SearchRepository) servletContext.getAttribute(Constants.
                                                                       GLOBAL_SEARCH_REPOSITORY);
                    if (scope.equals("global")) {
                        wsMap = (Map<String, WebSearchable>) globalRepository.
                            getWebSearchableMap(type);
                    } else {
                        // must be "all"
                        SearchRepository userSearchRepository = profile.getSearchRepository();
                        Map<String, ? extends WebSearchable> userWsMap =
                            userSearchRepository.getWebSearchableMap(type);
                        Map<String, ? extends WebSearchable> globalWsMap =
                            globalRepository.getWebSearchableMap(type);
                        wsMap = new HashMap<String, WebSearchable>(userWsMap);
                        wsMap.putAll(globalWsMap);
                    }
                }
            }


            Map<String, ? extends WebSearchable> filteredWsMap
                                    = new LinkedHashMap<String, WebSearchable>();
            //Filter by aspects (defined in superuser account)
            List<String> aspectTags = new ArrayList<String>();
            List<String> userTags = new ArrayList<String>();
            for (String tag :tags) {
                if (tag.startsWith("aspect:")) {
                    aspectTags.add(tag);
                } else {
                    userTags.add(tag);
                }
            }
            if (aspectTags.size() > 0) {
                wsMap = pm.filterByTags(wsMap, aspectTags, type,
                        (String) servletContext.getAttribute(Constants.SUPERUSER_ACCOUNT));
            }

            if (profile.getUsername() != null && userTags != null && userTags.size() > 0) {
                filteredWsMap = pm.filterByTags(wsMap, userTags, type, profile.getUsername());
            } else {
                filteredWsMap = wsMap;
            }

            List returnList = new ArrayList<String>();

            returnList.add(callId);

            SearchRepository.filterOutInvalidTemplates(filteredWsMap);
            for (WebSearchable ws: filteredWsMap.values()) {
                List row = new ArrayList();
                row.add(ws.getName());
                if (filterText != null && filterText.length() > 1) {
                    row.add(highlightedDescMap.get(ws));
                    row.add(hitMap.get(ws));
                } else {
                    row.add(ws.getDescription());
                }
                returnList.add(row);
            }

            return returnList;
        } catch (RuntimeException e) {
            processException(e);
            return null;
        }
    }

    /**
     * For a given bag name and a type different from the bag type, give the number of
     * converted objects
     *
     * @param bagName the name of the bag
     * @param type the type to convert to
     * @return the number of converted objects
     */
    public static int getConvertCountForBag(String bagName, String type) {
        try {
            ServletContext servletContext = WebContextFactory.get().getServletContext();
            ProfileManager pm = SessionMethods.getProfileManager(servletContext);
            HttpSession session = WebContextFactory.get().getSession();
            ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
            String pckName =  os.getModel().getPackageName();
            Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
            SearchRepository searchRepository =
                SearchRepository.getGlobalSearchRepository(servletContext);
            InterMineBag imBag = null;
            int count = 0;
            try {
                imBag = BagHelper.getBag(profile, searchRepository, bagName);
                Map<String, QuerySelectable> pathToQueryNode = new HashMap();
                Map<String, InterMineBag> bagMap = new HashMap<String, InterMineBag>();
                bagMap.put(imBag.getName(), imBag);

                PathQuery pathQuery = TypeConverter.getConversionQuery(BagQueryRunner.
                    getConversionTemplates(servletContext),
                    TypeUtil.instantiate(pckName + "." + imBag.getType()),
                    TypeUtil.instantiate(pckName + "." + type), imBag);
                Query query = MainHelper.makeQuery(pathQuery, bagMap, pathToQueryNode,
                    servletContext, null, false,
                    (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE),
                    (Map) servletContext.getAttribute(Constants.CLASS_KEYS),
                    (BagQueryConfig) servletContext.getAttribute(Constants.BAG_QUERY_CONFIG));
                count = os.count(query, ObjectStore.SEQUENCE_IGNORE);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return count;
        } catch (RuntimeException e) {
            processException(e);
            return 0;
        }
    }

    /**
     * Saves information, that some element was toggled - displayed or hidden.
     *
     * @param elementId element id
     * @param opened new aspect state
     */
    public static void saveToggleState(String elementId, boolean opened) {
        try {
            HttpSession session = WebContextFactory.get().getSession();
            GuiObject guiObject = (GuiObject) session.getAttribute(Constants.GUI_OBJECT);
            if (guiObject == null) {
                guiObject = new GuiObject();
                session.setAttribute(Constants.GUI_OBJECT, guiObject);
            }
            guiObject.getToggledElements().put(elementId, Boolean.valueOf(opened));
        } catch (RuntimeException e) {
            processException(e);
        }
    }

    /**
     * validate bag upload
     * @param bagName name of new bag to be validated
     * @return error msg to display, if any
     */
    public static String validateBagName(String bagName) {

        try {
            ServletContext servletContext = WebContextFactory.get().getServletContext();
            ProfileManager pm = SessionMethods.getProfileManager(servletContext);
            HttpSession session = WebContextFactory.get().getSession();
            Profile profile = (Profile) session.getAttribute(Constants.PROFILE);

            // TODO get message text from the properties file
            if (bagName.equals("")) {
                return "You cannot save a list with a blank name";
            }

            if (!WebUtil.isValidName(bagName)) {
                return "Invalid name. Names can only contain letters, numbers, spaces, "
                + "and underscores.";
            }

            if (profile.getSavedBags().get(bagName) != null) {
                return "The list name you have chosen is already in use";
            }

            SearchRepository searchRepository =
                SearchRepository.getGlobalSearchRepository(servletContext);
            Map<String, ? extends WebSearchable> publicBagMap =
                searchRepository.getWebSearchableMap(TagTypes.BAG);
            if (publicBagMap.get(bagName) != null) {
                return "The list name you have chosen is already in use -"
                + " there is a public list called " + bagName;
            }

            return "";
        } catch (RuntimeException e) {
            processException(e);
            return null;
        }
    }

    /**
     * validation that happens before new bag is saved
     * @param bagName name of new bag
     * @param selectedBags bags involved in operation
     * @param operation which operation is taking place - delete, union, intersect or subtract
     * @return error msg, if any
     */
    public static String validateBagOperations(String bagName, String[] selectedBags,
                                               String operation) {

        try {
            bagName = bagName.trim();
            ServletContext servletContext = WebContextFactory.get().getServletContext();
            HttpSession session = WebContextFactory.get().getSession();
            Profile profile = (Profile) session.getAttribute(Constants.PROFILE);

            // TODO get error text from the properties file
            if (selectedBags.length == 0) {
                return "No lists are selected";
            }
            if (operation.equals("delete")) {
                for (int i = 0; i < selectedBags.length; i++) {
                    Set<String> queries = new HashSet<String>();
                    queries.addAll(queriesThatMentionBag(profile.getSavedQueries(),
                                                         selectedBags[i]));
                    queries.addAll(queriesThatMentionBag(profile.getHistory(),
                                                         selectedBags[i]));
                    if (queries.size() > 0) {
                        return "List " + selectedBags[i] + " cannot be deleted as it is referenced "
                        + "by other queries " + queries;
                    }
                }
            } else {
                Properties properties = (Properties)
                    servletContext.getAttribute(Constants.WEB_PROPERTIES);
                String defaultName = properties.getProperty("lists.input.example");

                if (!operation.equals("copy") && (bagName.equals("") 
                        || (bagName.equalsIgnoreCase(defaultName)))) {
                    return "New list name is required";
                } else if (!WebUtil.isValidName(bagName)) {
                    return "Invalid name. Names can only contain letters, numbers, spaces, "
                    + "and underscores.";
                }
            }
            return "";
        } catch (RuntimeException e) {
            processException(e);
            return null;
        }
    }

    /**
     * Provide a list of queries that mention a named bag
     * @param savedQueries a saved queries map (name -> query)
     * @param bagName the name of a bag
     * @return the list of queries
     */
    public static List<String> queriesThatMentionBag(Map savedQueries, String bagName) {
        try {
            List<String> queries = new ArrayList<String>();
            for (Iterator i = savedQueries.keySet().iterator(); i.hasNext();) {
                String queryName = (String) i.next();
                SavedQuery query = (SavedQuery) savedQueries.get(queryName);
                if (query.getPathQuery().getBagNames().contains(bagName)) {
                    queries.add(queryName);
                }
            }
            return queries;
        } catch (RuntimeException e) {
            processException(e);
            return null;
        }
    }

    /**
     * @param widgetId unique id for this widget
     * @param bagName name of list
     * @param selectedExtraAttribute extra attribute (like organism)
     * @return graph widget
     */
    public static GraphWidget getProcessGraphWidget(String widgetId, String bagName,
                                                    String selectedExtraAttribute) {
        try {
            ServletContext servletContext = WebContextFactory.get().getServletContext();
            HttpSession session = WebContextFactory.get().getSession();
            WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
            ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
            Model model =  os.getModel();
            Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
            SearchRepository searchRepository =
                SearchRepository.getGlobalSearchRepository(servletContext);
            InterMineBag imBag = BagHelper.getBag(profile, searchRepository, bagName);

            Type type = (Type) webConfig.getTypes().get(model.getPackageName()
                            + "." + imBag.getType());
            List<Widget> widgets = type.getWidgets();
            for (Widget widget: widgets) {
                if (widget.getId() == Integer.parseInt(widgetId)) {
                    GraphWidget graphWidget = (GraphWidget) widget;
                    graphWidget.setSession(session);
                    graphWidget.setSelectedExtraAttribute(selectedExtraAttribute);
                    graphWidget.process(imBag, os);
                    return graphWidget;
                }
            }
        } catch (RuntimeException e) {
            processException(e);
        } catch (InterMineException e) {
            processException(e);
        }
        return null;
    }

    /**
     *
     * @param widgetId unique ID for this widget
     * @param bagName name of list
     * @return table widget
     */
    public static TableWidget getProcessTableWidget(String widgetId, String bagName) {
        try {
            ServletContext servletContext = WebContextFactory.get().getServletContext();
            HttpSession session = WebContextFactory.get().getSession();
            WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
            ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
            Model model =  os.getModel();
            Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
            SearchRepository searchRepository =
                SearchRepository.getGlobalSearchRepository(servletContext);
            InterMineBag imBag = BagHelper.getBag(profile, searchRepository, bagName);
            Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);

            Type type = (Type) webConfig.getTypes().get(model.getPackageName()
                            + "." + imBag.getType());
            List<Widget> widgets = type.getWidgets();
            for (Widget widget: widgets) {
                if (widget.getId() == Integer.parseInt(widgetId)) {
                    TableWidget tableWidget = (TableWidget) widget;
                    tableWidget.setClassKeys(classKeys);
                    tableWidget.setWebConfig(webConfig);
                    try {
                        tableWidget.process(imBag, os);
                    } catch (Exception e) {
                        return null;
                    }

                    return tableWidget;
                }
            }
        } catch (RuntimeException e) {
            processException(e);
        } catch (InterMineException e) {
            processException(e);
        }
        return null;
    }

    /**
     * #
     * @param widgetId unique ID for each widget
     * @param bagName name of list
     * @param errorCorrection error correction method to use
     * @param max maximum value to display
     * @param selectedExtraAttribute extra attribute to filter by
     * @param externalLink link to external datasource
     * @param externalLinkLabel name of external datasource.
     * @return enrichment widget
     */
    public static EnrichmentWidget getProcessEnrichmentWidget(String widgetId, String bagName,
                                                              String errorCorrection, String max,
                                                              String selectedExtraAttribute,
                                                              String externalLink,
                                                              String externalLinkLabel) {
        try {
            ServletContext servletContext = WebContextFactory.get().getServletContext();
            HttpSession session = WebContextFactory.get().getSession();
            WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
            ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
            Model model = os.getModel();
            Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
            SearchRepository searchRepository = SearchRepository
                            .getGlobalSearchRepository(servletContext);
            InterMineBag imBag = BagHelper.getBag(profile, searchRepository, bagName);
            Type type = (Type) webConfig.getTypes().get(model.getPackageName()
                    + "." + imBag.getType());
            List<Widget> widgets = type.getWidgets();
            for (Widget widget : widgets) {
                if (widget.getId() == Integer.parseInt(widgetId)) {
                    EnrichmentWidget enrichmentWidget = (EnrichmentWidget) widget;
                    enrichmentWidget.setMax(max);
                    enrichmentWidget.setErrorCorrection(errorCorrection);
                    enrichmentWidget.setSelectedExtraAttribute(selectedExtraAttribute);
                    enrichmentWidget.setExternalLink(externalLink);
                    enrichmentWidget.setExternalLinkLabel(externalLinkLabel);
                    enrichmentWidget.process(imBag, os);
                    return enrichmentWidget;
                }
            }
        } catch (RuntimeException e) {
            processException(e);
        } catch (InterMineException e) {
            processException(e);
        }
        return null;
    }
    
    /**
     * Add an ID to the PagedTable selection
     * @param selectedId the id
     * @param tableId the identifier for the PagedTable
     * @return a String[]
     */
    public static List<String> selectId(String selectedId, String tableId) {
        ServletContext servletContext = WebContextFactory.get().getServletContext();
        HttpSession session = WebContextFactory.get().getSession();
        PagedTable pt = SessionMethods.getResultsTable(session, tableId);
        pt.selectId(new Integer(selectedId));
        return new ArrayList<String>(pt.getSelectedIds().values());
    }
    
    /**
     * De-select an Id from the PagedTable
     * @param deSelectId the ID to remove from the selection
     * @param tableId the PagedTable identifier
     * @return a String[]
     */
    public static List<String> deSelectId(String deSelectId, String tableId) {
        ServletContext servletContext = WebContextFactory.get().getServletContext();
        HttpSession session = WebContextFactory.get().getSession();
        PagedTable pt = SessionMethods.getResultsTable(session, tableId);
        pt.getSelectedIds().remove(new Integer(deSelectId));
        return new ArrayList<String>(pt.getSelectedIds().values());
    }
    
    /**
     * Set the class of the selected column of ids for the PagedTable
     * @param className the className
     * @param tableId the PagedTable identifier
     */
    public static void setClassForId(String className, String tableId) {
        ServletContext servletContext = WebContextFactory.get().getServletContext();
        HttpSession session = WebContextFactory.get().getSession();
        PagedTable pt = SessionMethods.getResultsTable(session, tableId);
        pt.setSelectedClass(className);
    }
    
    /**
     * Select all the elements in a PagedTable
     * @param index the index of the selected column
     * @param tableId the PagedTable identifier
     */
    public static void selectAll(int index, String tableId) {
        ServletContext servletContext = WebContextFactory.get().getServletContext();
        HttpSession session = WebContextFactory.get().getSession();
        PagedTable pt = SessionMethods.getResultsTable(session, tableId);
        pt.clearSelectIds();
        pt.setAllSelected(index);
    }
    
}
