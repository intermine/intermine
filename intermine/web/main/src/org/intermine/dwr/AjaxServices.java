package org.intermine.dwr;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

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
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.TypeConverter;
import org.intermine.api.mines.FriendlyMineManager;
import org.intermine.api.mines.FriendlyMineQueryRunner;
import org.intermine.api.mines.Mine;
import org.intermine.api.profile.BagState;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileAlreadyExistsException;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.SavedQuery;
import org.intermine.api.profile.TagManager;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.WebTable;
import org.intermine.api.search.Scope;
import org.intermine.api.search.SearchFilterEngine;
import org.intermine.api.search.SearchRepository;
import org.intermine.api.search.WebSearchable;
import org.intermine.api.tag.TagNames;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplateQuery;
import org.intermine.api.template.TemplateSummariser;
import org.intermine.api.util.NameUtil;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.autocompletion.AutoCompleter;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.config.Type;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.query.PageTableQueryMonitor;
import org.intermine.web.logic.query.QueryMonitorTimeout;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.WebState;
import org.intermine.web.logic.session.QueryCountQueryMonitor;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.widget.EnrichmentWidget;
import org.intermine.web.logic.widget.GraphWidget;
import org.intermine.web.logic.widget.HTMLWidget;
import org.intermine.web.logic.widget.TableWidget;
import org.intermine.web.logic.widget.config.EnrichmentWidgetConfig;
import org.intermine.web.logic.widget.config.GraphWidgetConfig;
import org.intermine.web.logic.widget.config.HTMLWidgetConfig;
import org.intermine.web.logic.widget.config.TableWidgetConfig;
import org.intermine.web.logic.widget.config.WidgetConfig;
import org.intermine.web.util.InterMineLinkGenerator;
import org.json.JSONException;
import org.json.JSONObject;


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
    private static final String INVALID_NAME_MSG = "Invalid name.  Names may only contain letters, "
        + "numbers, spaces, and underscores.";

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
            Profile profile = SessionMethods.getProfile(session);
            String nameCopy = name.replaceAll("#039;", "'");
            TagManager tagManager = getTagManager();

            // already a favourite.  turning off.
            if (isFavourite) {
                tagManager.deleteTag(TagNames.IM_FAVOURITE, nameCopy, type, profile.getUsername());
            // not a favourite.  turning on.
            } else {
                tagManager.addTag(TagNames.IM_FAVOURITE, nameCopy, type, profile.getUsername());
            }
        } catch (RuntimeException e) {
            processException(e);
        }
    }

    private static void processWidgetException(Exception e, String widgetId) {
        String msg = "Failed to render widget: " + widgetId;
        LOG.error(msg, e);
    }

    private static void processException(Exception e) {
        LOG.error(ERROR_MSG, e);
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        throw new RuntimeException(e);
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
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            Profile profile = SessionMethods.getProfile(session);
            Map<String, TemplateQuery> templates = profile.getSavedTemplates();
            TemplateQuery t = templates.get(templateName);
            WebResultsExecutor executor = im.getWebResultsExecutor(profile);

            try {
                session.setAttribute("precomputing_" + templateName, "true");
                executor.precomputeTemplate(t);
            } catch (ObjectStoreException e) {
                LOG.error("Error while precomputing", e);
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
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            Profile profile = SessionMethods.getProfile(session);
            Map<String, TemplateQuery> templates = profile.getSavedTemplates();
            TemplateQuery template = templates.get(templateName);
            TemplateSummariser summariser = im.getTemplateSummariser();
            try {
                session.setAttribute("summarising_" + templateName, "true");
                summariser.summarise(template);
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
            Profile profile = SessionMethods.getProfile(session);
            SavedQuery sq;
            if (name.equals(newName) || StringUtils.isEmpty(newName)) {
                return name;
            }
            // TODO get error text from properties file
            if (!NameUtil.isValidName(newName)) {
                return INVALID_NAME_MSG;
            }
            if ("history".equals(type)) {
                if (profile.getHistory().get(name) == null) {
                    return "<i>" + name + " does not exist</i>";
                }
                if (profile.getHistory().get(newName) != null) {
                    return "<i>" + newName + " already exists</i>";
                }
                profile.renameHistory(name, newName);
            } else if ("saved".equals(type)) {
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
            } else if ("bag".equals(type)) {
                try {
                    profile.renameBag(name, newName);
                } catch (IllegalArgumentException e) {
                    return "<i>" + name + " does not exist</i>";
                } catch (ProfileAlreadyExistsException e) {
                    return "<i>" + newName + " already exists</i>";
                }
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
     * Generate a new API key for a given user.
     * @param username the user to generate the key for.
     * @return A new API key, or null if something untoward happens.
     * @throws Exception an exception.
     */
    public String generateApiKey(String username) throws Exception {
        try {
            WebContext ctx = WebContextFactory.get();
            HttpSession session = ctx.getSession();
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            final ProfileManager pm = im.getProfileManager();
            Profile p = pm.getProfile(username);
            return pm.generateApiKey(p);
        } catch (RuntimeException e) {
            processException(e);
            return null;
        }
    }

    /**
     * Delete a user's API key, thus disabling webservice access. A message "deleted"
     * is returned to confirm success.
     * @param username The user whose key we should delete.
     * @return A confirmation string.
     * @throws Exception if somethign bad happens
     */
    public String deleteApiKey(String username)
        throws Exception {
        try {
            WebContext ctx = WebContextFactory.get();
            HttpSession session = ctx.getSession();
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            final ProfileManager pm = im.getProfileManager();
            Profile p = pm.getProfile(username);
            p.setApiKey(null);
            return "deleted";
        } catch (RuntimeException e) {
            processException(e);
            return null;
        }
    }

    /**
     * For a given bag, set its description
     * @param bagName the bag
     * @param description the description as entered by the user
     * @return the description for display on the jsp page
     * @throws Exception an exception
     */
    public String saveBagDescription(String bagName, String description) throws Exception {
        try {
            WebContext ctx = WebContextFactory.get();
            HttpSession session = ctx.getSession();
            Profile profile = SessionMethods.getProfile(session);
            InterMineBag bag = profile.getSavedBags().get(bagName);
            if (bag == null) {
                throw new InterMineException("List \"" + bagName + "\" not found.");
            }
            bag.setDescription(description);
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
            PathQuery query = SessionMethods.getQuery(session);
            Path path = query.makePath(pathString);
            Path prefixPath = path.getPrefix();
            if (descr == null) {
                // setting to null removes the description
                query.setDescription(prefixPath.getNoConstraintsString(), null);
            } else {
                query.setDescription(prefixPath.getNoConstraintsString(), descr);
            }
            if (descr == null) {
                return null;
            }
            return descr.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        } catch (RuntimeException e) {
            processException(e);
            return null;
        } catch (PathException e) {
            processException(e);
            return null;
        }
    }

    /*
     * Cannot be refactored from AjaxServices, else WebContextFactory.get() returns null
     */
    private static WebState getWebState() {
        HttpSession session = WebContextFactory.get().getSession();
        return SessionMethods.getWebState(session);
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
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            Profile profile = SessionMethods.getProfile(session);
            WebResultsExecutor webResultsExecutor = im.getWebResultsExecutor(profile);

            WebTable webTable = (SessionMethods.getResultsTable(session, tableName))
                                   .getWebTable();
            PathQuery pathQuery = webTable.getPathQuery();
            List<ResultsRow> results = (List) webResultsExecutor.summariseQuery(pathQuery,
                    summaryPath);

            // Start the count of results
            Query countQuery = webResultsExecutor.makeSummaryQuery(pathQuery, summaryPath);
            QueryCountQueryMonitor clientState
                = new QueryCountQueryMonitor(Constants.QUERY_TIMEOUT_SECONDS * 1000, countQuery);
            MessageResources messages = (MessageResources) ctx.getHttpServletRequest()
                                                              .getAttribute(Globals.MESSAGES_KEY);
            String qid = SessionMethods.startQueryCount(clientState, session, messages);
            List<ResultsRow> pageSizeResults = new ArrayList<ResultsRow>();
            int rowCount = 0;
            for (ResultsRow row : results) {
                rowCount++;
                if (rowCount > 10) {
                    break;
                }
                pageSizeResults.add(row);
            }
            return Arrays.asList(new Object[] {pageSizeResults, qid, new Integer(rowCount)});
        } catch (RuntimeException e) {
            processException(e);
            return null;
        }
    }

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

            // this could happen if the user navigates away then back to the page
            if (controller == null) {
                return null;
            }

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
                    return new Integer(pt.getExactSize());
                }
                if (controller instanceof QueryCountQueryMonitor) {
                    return new Integer(((QueryCountQueryMonitor) controller).getCount());
                }
                LOG.debug("query qid " + qid + " - unknown controller type");
                return null;
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
            HttpSession session = WebContextFactory.get().getSession();
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            ProfileManager pm = im.getProfileManager();
            Profile profile = SessionMethods.getProfile(session);
            Map<String, WebSearchable> wsMap;
            Map<WebSearchable, Float> hitMap = new LinkedHashMap<WebSearchable, Float>();
            Map<WebSearchable, String> highlightedDescMap = new HashMap<WebSearchable, String>();

            if (filterText != null && filterText.length() > 1) {
                wsMap = new LinkedHashMap<String, WebSearchable>();
                //Map<WebSearchable, String> scopeMap = new LinkedHashMap<WebSearchable, String>();
                SearchRepository globalSearchRepository =
                    SessionMethods.getGlobalSearchRepository(servletContext);
                try {
                    long time =
                        SearchRepository.runLeuceneSearch(filterText, scope, type, profile,
                                                        globalSearchRepository,
                                                        hitMap, null, highlightedDescMap);
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

                if (scope.equals(Scope.USER)) {
                    SearchRepository searchRepository = profile.getSearchRepository();
                    wsMap = (Map<String, WebSearchable>) searchRepository.getWebSearchableMap(type);
                } else {
                    SearchRepository globalRepository = SessionMethods
                        .getGlobalSearchRepository(servletContext);
                    if (scope.equals(Scope.GLOBAL)) {
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
                if (tag.startsWith(TagNames.IM_ASPECT_PREFIX)) {
                    aspectTags.add(tag);
                } else {
                    userTags.add(tag);
                }
            }
            if (aspectTags.size() > 0) {
                wsMap = new SearchFilterEngine().filterByTags(wsMap, aspectTags, type,
                                                              pm.getSuperuser(), getTagManager());
            }

            if (profile.getUsername() != null && userTags.size() > 0) {
                filteredWsMap = new SearchFilterEngine().filterByTags(wsMap, userTags, type,
                        profile.getUsername(), getTagManager());
            } else {
                filteredWsMap = wsMap;
            }

            List returnList = new ArrayList<String>();

            returnList.add(callId);

            // We need a modifiable map so we can filter out invalid templates
            LinkedHashMap<String, ? extends WebSearchable> modifiableWsMap =
                new LinkedHashMap(filteredWsMap);

            SearchRepository.filterOutInvalidTemplates(modifiableWsMap);
            for (WebSearchable ws: modifiableWsMap.values()) {
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
            HttpSession session = WebContextFactory.get().getSession();
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            String pckName = im.getModel().getPackageName();
            Profile profile = SessionMethods.getProfile(session);
            BagManager bagManager = im.getBagManager();
            TemplateManager templateManager = im.getTemplateManager();

            WebResultsExecutor webResultsExecutor = im.getWebResultsExecutor(profile);

            InterMineBag imBag = null;
            int count = 0;
            try {
                imBag = bagManager.getUserOrGlobalBag(profile, bagName);
                List<TemplateQuery> conversionTemplates = templateManager.getConversionTemplates();

                PathQuery pathQuery = TypeConverter.getConversionQuery(conversionTemplates,
                    TypeUtil.instantiate(pckName + "." + imBag.getType()),
                    TypeUtil.instantiate(pckName + "." + type), imBag);
                count = webResultsExecutor.count(pathQuery);
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
     * used on REPORT page
     *
     * For a gene, generate links to other intermines.  Include gene and orthologues.
     *
     * Returns NULL if no values found.  It's possible that the identifier in the local mine will
     * match more than one entry in the remote mine but this will be handled by the portal of the
     * remote mine.
     *
     * @param mineName name of mine to query
     * @param organismName gene.organism
     * @param primaryIdentifier identifier for gene
     * @param symbol identifier for gene or NULL
     * @return the links to friendly intermines
     */
    public static String getFriendlyMineReportLinks(String mineName, String organismName,
            String primaryIdentifier, String symbol) {
        HttpSession session = WebContextFactory.get().getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        FriendlyMineManager fmm = im.getFriendlyMineManager();
        InterMineLinkGenerator linkGen = null;
        Class<?> clazz
            = TypeUtil.instantiate("org.intermine.bio.web.util.FriendlyMineReportLinkGenerator");
        Constructor<?> constructor;
        try {
            constructor = clazz.getConstructor(new Class[] {});
            linkGen = (InterMineLinkGenerator) constructor.newInstance(new Object[] {});
        } catch (Exception e) {
            LOG.error("Failed to instantiate FriendlyMineReportLinkGenerator because: " + e);
            return null;
        }
        Collection<JSONObject> results = linkGen.getLinks(fmm, mineName, organismName,
                primaryIdentifier);
        if (results == null || results.isEmpty()) {
            return null;
        }
        return results.toString();
    }

    /**
     * For LIST ANALYSIS page - For a mine, test if that mine has orthologues
     *
     * @param mineName name of a friendly mine
     * @param organisms list of organisms for genes in this list
     * @param identifiers list of identifiers of genes in this list
     * @return the links to friendly intermines or an error message
     */
    public static String getFriendlyMineListLinks(String mineName, String organisms,
            String identifiers) {
        if (StringUtils.isEmpty(mineName) || StringUtils.isEmpty(organisms)
                || StringUtils.isEmpty(identifiers)) {
            return null;
        }
        HttpSession session = WebContextFactory.get().getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        FriendlyMineManager linkManager = im.getFriendlyMineManager();
        InterMineLinkGenerator linkGen = null;
        Class<?> clazz
            = TypeUtil.instantiate("org.intermine.bio.web.util.FriendlyMineListLinkGenerator");
        Constructor<?> constructor;
        Collection<JSONObject> results = null;
        try {
            constructor = clazz.getConstructor(new Class[] {});
            linkGen = (InterMineLinkGenerator) constructor.newInstance(new Object[] {});
            // runs remote templates (possibly)
            results = linkGen.getLinks(linkManager, mineName, organisms, identifiers);
        } catch (Exception e) {
            LOG.error("Failed to instantiate FriendlyMineListLinkGenerator because: " + e);
            return null;
        }
        if (results == null || results.isEmpty()) {
            return null;
        }
        return results.toString();
    }


    /**
     * used on REPORT page
     *
     * For a gene, display pathways found in other mines for orthologous genes
     *
     * @param mineName mine to query
     * @param orthologues list of genes to query for
     * @return the links to friendly intermines
     */
    public static String getFriendlyMinePathways(String mineName, String orthologues) {
        if (StringUtils.isEmpty(orthologues)) {
            return null;
        }
        HttpSession session = WebContextFactory.get().getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        FriendlyMineManager linkManager = im.getFriendlyMineManager();
        Mine mine = linkManager.getMine(mineName);
        if (mine == null || mine.getReleaseVersion() == null) {
            // mine is dead
            return null;
        }
        final String xmlQuery = "<query name=\"\" model=\"genomic\" view=\"Gene.pathways.id "
            + "Gene.pathways.name\" sortOrder=\"Gene.pathways.name asc\"><constraint path=\"Gene\" "
            + "op=\"LOOKUP\" value=\"" + orthologues + "\" extraValue=\"\"/></query>";
        try {
            JSONObject results
                = FriendlyMineQueryRunner.runJSONWebServiceQuery(mine, xmlQuery);
            if (results == null) {
                LOG.error("Couldn't query " + mine.getName() + " for pathways");
                return null;
            }
            results.put("mineURL", mine.getUrl());
            return results.toString();
        } catch (IOException e) {
            LOG.error("Couldn't query " + mine.getName() + " for pathways", e);
            return null;
        } catch (JSONException e) {
            LOG.error("Error adding Mine URL to pathways results", e);
            return null;
        }
    }

    /**
     * Return list of disease ontology terms associated with list of provided rat genes.  Returns
     * JSONObject as string with ID (intermine ID) and name (ontologyTerm.name)
     *
     * @param orthologues list of rat genes
     * @return JSONobject.toString of JSON object
     */
    public static String getRatDiseases(String orthologues) {
        if (StringUtils.isEmpty(orthologues)) {
            return null;
        }
        HttpSession session = WebContextFactory.get().getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        FriendlyMineManager linkManager = im.getFriendlyMineManager();
        Mine mine = linkManager.getMine("RatMine");
        if (mine == null || mine.getReleaseVersion() == null) {
            // mine is dead
            return null;
        }
        final String xmlQuery = "<query name=\"rat_disease\" model=\"genomic\" view="
            + "\"Gene.doAnnotation.ontologyTerm.id Gene.doAnnotation.ontologyTerm.name\"  "
            + "sortOrder=\"Gene.doAnnotation.ontologyTerm.name asc\"> "
            + "<pathDescription pathString=\"Gene.doAnnotation.ontologyTerm\" "
            + "description=\"Disease Ontology Term\"/> "
            + "<constraint path=\"Gene\" op=\"LOOKUP\" value=\"" + orthologues
            + "\" extraValue=\"\"/></query>";
        try {
            JSONObject results
                = FriendlyMineQueryRunner.runJSONWebServiceQuery(mine, xmlQuery);
            if (results != null) {
                results.put("mineURL", mine.getUrl());
                return results.toString();
            }
        } catch (IOException e) {
            LOG.error("Couldn't query ratmine for diseases", e);
        } catch (JSONException e) {
            LOG.error("Couldn't process ratmine disease results", e);
        }
        return null;
    }

    /**
     * Saves information, that some element was toggled - displayed or hidden.
     *
     * @param elementId element id
     * @param opened new aspect state
     */
    public static void saveToggleState(String elementId, boolean opened) {
        try {
            AjaxServices.getWebState().getToggledElements().put(elementId,
                    Boolean.valueOf(opened));
        } catch (RuntimeException e) {
            processException(e);
        }
    }

    /**
     * Set state that should be saved during the session.
     * @param name name of state
     * @param value value of state
     */
    public static void setState(String name, String value) {
        try {
            AjaxServices.getWebState().setState(name, value);
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
            HttpSession session = WebContextFactory.get().getSession();
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            Profile profile = SessionMethods.getProfile(session);
            BagManager bagManager = im.getBagManager();

            bagName = bagName.trim();
            // TODO get message text from the properties file
            if ("".equals(bagName)) {
                return "You cannot save a list with a blank name";
            }

            if (!NameUtil.isValidName(bagName)) {
                return INVALID_NAME_MSG;
            }

            if (profile.getSavedBags().get(bagName) != null) {
                return "The list name you have chosen is already in use";
            }

            if (bagManager.getGlobalBag(bagName) != null) {
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
            ServletContext servletContext = WebContextFactory.get().getServletContext();
            HttpSession session = WebContextFactory.get().getSession();
            Profile profile = SessionMethods.getProfile(session);

            // TODO get error text from the properties file
            if (selectedBags.length == 0) {
                return "No lists are selected";
            }
            if ("delete".equals(operation)) {
                for (int i = 0; i < selectedBags.length; i++) {
                    Set<String> queries = new HashSet<String>();
                    queries.addAll(queriesThatMentionBag(profile.getSavedQueries(),
                                selectedBags[i]));
                    queries.addAll(queriesThatMentionBag(profile.getHistory(), selectedBags[i]));
                    if (queries.size() > 0) {
                        return "List " + selectedBags[i] + " cannot be deleted as it is referenced "
                            + "by other queries " + queries;
                    }
                }
                for (int i = 0; i < selectedBags.length; i++) {
                    if (profile.getSavedBags().get(selectedBags[i]) == null) {
                        return "List " + selectedBags[i] + " cannot be deleted as it is a shared "
                            + "list";
                    }
                }
            } else if (!"copy".equals(operation)) {
                Properties properties = SessionMethods.getWebProperties(servletContext);
                String defaultName = properties.getProperty("lists.input.example");
                if (("".equals(bagName) || (bagName.equalsIgnoreCase(defaultName)))) {
                    return "New list name is required";
                } else if (!NameUtil.isValidName(bagName)) {
                    return INVALID_NAME_MSG;
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
     * @param savedQueries a saved queries map (name -&gt; query)
     * @param bagName the name of a bag
     * @return the list of queries
     */
    private static List<String> queriesThatMentionBag(Map<String, SavedQuery> savedQueries,
            String bagName) {
        try {
            List<String> queries = new ArrayList<String>();
            for (Iterator<String> i = savedQueries.keySet().iterator(); i.hasNext();) {
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
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            WebConfig webConfig = SessionMethods.getWebConfig(servletContext);
            ObjectStore os = im.getObjectStore();
            Model model =  os.getModel();
            Profile profile = SessionMethods.getProfile(session);
            BagManager bagManager = im.getBagManager();
            InterMineBag imBag = bagManager.getUserOrGlobalBag(profile, bagName);

            Type type = webConfig.getTypes().get(model.getPackageName()
                    + "." + imBag.getType());
            List<WidgetConfig> widgets = type.getWidgets();
            for (WidgetConfig widget: widgets) {
                if (widget.getId().equals(widgetId)) {
                    GraphWidgetConfig graphWidgetConf = (GraphWidgetConfig) widget;
                    graphWidgetConf.setSession(session);
                    GraphWidget graphWidget = new GraphWidget(graphWidgetConf, imBag, os,
                                    selectedExtraAttribute);
                    return graphWidget;
                }
            }
        } catch (RuntimeException e) {
            processWidgetException(e, widgetId);
        }
        return null;
    }

    /**
     * @param widgetId unique id for this widget
     * @param bagName name of list
     * @return graph widget
     */
    public static HTMLWidget getProcessHTMLWidget(String widgetId, String bagName) {
        try {
            ServletContext servletContext = WebContextFactory.get().getServletContext();
            HttpSession session = WebContextFactory.get().getSession();
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            WebConfig webConfig = SessionMethods.getWebConfig(servletContext);
            Model model = im.getModel();
            Profile profile = SessionMethods.getProfile(session);

            BagManager bagManager = im.getBagManager();
            InterMineBag imBag = bagManager.getUserOrGlobalBag(profile, bagName);

            Type type = webConfig.getTypes().get(model.getPackageName()
                            + "." + imBag.getType());
            List<WidgetConfig> widgets = type.getWidgets();
            for (WidgetConfig widget: widgets) {
                if (widget.getId().equals(widgetId)) {
                    HTMLWidgetConfig htmlWidgetConf = (HTMLWidgetConfig) widget;
                    HTMLWidget htmlWidget = new HTMLWidget(htmlWidgetConf);
                    return htmlWidget;
                }
            }
        } catch (RuntimeException e) {
            processWidgetException(e, widgetId);
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
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            WebConfig webConfig = SessionMethods.getWebConfig(servletContext);
            ObjectStore os = im.getObjectStore();
            Model model =  os.getModel();
            Profile profile = SessionMethods.getProfile(session);
            BagManager bagManager = im.getBagManager();
            InterMineBag imBag = bagManager.getUserOrGlobalBag(profile, bagName);
            Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();

            Type type = webConfig.getTypes().get(model.getPackageName()
                            + "." + imBag.getType());
            List<WidgetConfig> widgets = type.getWidgets();
            for (WidgetConfig widgetConfig: widgets) {
                if (widgetConfig.getId().equals(widgetId)) {
                    TableWidgetConfig tableWidgetConfig = (TableWidgetConfig) widgetConfig;
                    tableWidgetConfig.setClassKeys(classKeys);
                    tableWidgetConfig.setWebConfig(webConfig);
                    TableWidget tableWidget = new TableWidget(tableWidgetConfig, imBag, os, null);
                    return tableWidget;
                }
            }
        } catch (RuntimeException e) {
            processWidgetException(e, widgetId);
        }
        return null;
    }

    /**
     *
     * @param widgetId unique ID for each widget
     * @param bagName name of list
     * @param errorCorrection error correction method to use
     * @param max maximum value to display
     * @param filters list of strings used to filter widget results, ie Ontology
     * @param externalLink link to external datasource
     * @param externalLinkLabel name of external datasource.
     * @return enrichment widget
     */
    public static EnrichmentWidget getProcessEnrichmentWidget(String widgetId, String bagName,
            String errorCorrection, String max, String filters, String externalLink,
            String externalLinkLabel) {
        try {
            ServletContext servletContext = WebContextFactory.get().getServletContext();
            HttpSession session = WebContextFactory.get().getSession();
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            WebConfig webConfig = SessionMethods.getWebConfig(servletContext);
            ObjectStore os = im.getObjectStore();
            Model model = os.getModel();
            Profile profile = SessionMethods.getProfile(session);
            BagManager bagManager = im.getBagManager();

            InterMineBag imBag = bagManager.getUserOrGlobalBag(profile, bagName);
            Type type = webConfig.getTypes().get(model.getPackageName()
                    + "." + imBag.getType());
            List<WidgetConfig> widgets = type.getWidgets();
            for (WidgetConfig widgetConfig : widgets) {
                if (widgetConfig.getId().equals(widgetId)) {
                    EnrichmentWidgetConfig enrichmentWidgetConfig =
                                                        (EnrichmentWidgetConfig) widgetConfig;
                    enrichmentWidgetConfig.setExternalLink(externalLink);
                    enrichmentWidgetConfig.setExternalLinkLabel(externalLinkLabel);
                    EnrichmentWidget enrichmentWidget = new EnrichmentWidget(
                                    enrichmentWidgetConfig, imBag, os, filters, max,
                                    errorCorrection);
                    return enrichmentWidget;
                }
            }
        } catch (RuntimeException e) {
            processWidgetException(e, widgetId);
        }
        return null;
    }

    /**
     * Add an ID to the PagedTable selection
     * @param selectedId the id
     * @param tableId the identifier for the PagedTable
     * @param columnIndex the column of the selected id
     * @return the field values of the first selected objects
     */
    public static List<String> selectId(String selectedId, String tableId, String columnIndex) {
        WebContext ctx = WebContextFactory.get();
        HttpSession session = ctx.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        PagedTable pt = SessionMethods.getResultsTable(session, tableId);
        pt.selectId(new Integer(selectedId), (new Integer(columnIndex)).intValue());
        Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();
        ObjectStore os = im.getObjectStore();
        return pt.getFirstSelectedFields(os, classKeys);
    }

    /**
     * remove an Id from the PagedTable
     * @param deSelectId the ID to remove from the selection
     * @param tableId the PagedTable identifier
     * @return the field values of the first selected objects
     */
    public static List<String> deSelectId(String deSelectId, String tableId) {
        WebContext ctx = WebContextFactory.get();
        HttpSession session = ctx.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        PagedTable pt = SessionMethods.getResultsTable(session, tableId);
        pt.deSelectId(new Integer(deSelectId));
        Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();
        ObjectStore os = im.getObjectStore();
        return pt.getFirstSelectedFields(os, classKeys);
    }

    /**
     * Select all the elements in a PagedTable
     * @param index the index of the selected column
     * @param tableId the PagedTable identifier
     */
    public static void selectAll(int index, String tableId) {
        HttpSession session = WebContextFactory.get().getSession();
        PagedTable pt = SessionMethods.getResultsTable(session, tableId);
        pt.clearSelectIds();
        pt.setAllSelectedColumn(index);
    }

    /**
     * AJAX request - reorder view.
     * @param newOrder the new order as a String
     * @param oldOrder the previous order as a String
     */
    public void reorder(String newOrder, String oldOrder) {
        HttpSession session = WebContextFactory.get().getSession();
        List<String> newOrderList =
            new LinkedList<String>(StringUtil.serializedSortOrderToMap(newOrder).values());
        List<String> oldOrderList =
            new LinkedList<String>(StringUtil.serializedSortOrderToMap(oldOrder).values());

        List<String> view = SessionMethods.getEditingView(session);
        ArrayList<String> newView = new ArrayList<String>();

        for (int i = 0; i < view.size(); i++) {
            String newi = newOrderList.get(i);
            int oldi = oldOrderList.indexOf(newi);
            newView.add(view.get(oldi));
        }

        PathQuery query = SessionMethods.getQuery(session);
        query.clearView();
        query.addViews(newView);
    }

    /**
     * AJAX request - reorder the constraints.
     * @param newOrder the new order as a String
     * @param oldOrder the previous order as a String
     */
    public void reorderConstraints(String newOrder, String oldOrder) {
        HttpSession session = WebContextFactory.get().getSession();
        List<String> newOrderList =
            new LinkedList<String>(StringUtil.serializedSortOrderToMap(newOrder).values());
        List<String> oldOrderList =
            new LinkedList<String>(StringUtil.serializedSortOrderToMap(oldOrder).values());

        PathQuery query = SessionMethods.getQuery(session);
        if (query instanceof TemplateQuery) {
            TemplateQuery template = (TemplateQuery) query;
            for (int index = 0; index < newOrderList.size() - 1; index++) {
                String newi = newOrderList.get(index);
                int oldi = oldOrderList.indexOf(newi);
                if (index != oldi) {
                    List<PathConstraint> editableConstraints =
                        template.getModifiableEditableConstraints();
                    PathConstraint editableConstraint = editableConstraints.remove(oldi);
                    editableConstraints.add(index, editableConstraint);
                    template.setEditableConstraints(editableConstraints);
                    break;
                }
            }
        }
    }

    /**
     * Add a Node from the sort order
     * @param path the Path as a String
     * @param direction the direction to sort by
     * @exception Exception if the application business logic throws
     */
    public void addToSortOrder(String path, String direction)
        throws Exception {
        HttpSession session = WebContextFactory.get().getSession();
        PathQuery query = SessionMethods.getQuery(session);
        OrderDirection orderDirection = OrderDirection.ASC;
        if ("DESC".equals(direction.toUpperCase())) {
            orderDirection = OrderDirection.DESC;
        }
        query.clearOrderBy();
        query.addOrderBy(path, orderDirection);
    }

    /**
     * Work as a proxy for fetching remote file (RSS)
     * @param rssURL the url
     * @return String representation of a file
     */
    public static String getNewsPreview(String rssURL) {
        try {
            URL url = new URL(rssURL);

            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            StringBuffer sb = new StringBuffer();
            // append to string buffer
            while ((str = in.readLine()) != null) {
                sb.append(str);
            }
            in.close();
            return sb.toString();
        } catch (MalformedURLException e) {
            return "";
        } catch (IOException e) {
            return "";
        }
    }

    //*****************************************************************************
    // Tags AJAX Interface
    //*****************************************************************************

    /**
     * Adds tag and assures that there is only one tag for this combination of tag name, tagged
     * Object and type.
     * @param tag tag name
     * @param taggedObject object id that is tagged by this tag
     * @param type  tag type
     * @return 'ok' string if succeeded else error string
     */
    public static String addTag(String tag, String taggedObject, String type) {
        String tagName = tag;
        LOG.info("Called addTag(). tagName:" + tagName + " taggedObject:"
                + taggedObject + " type: " + type);

        try {
            HttpServletRequest request = getRequest();
            Profile profile = getProfile(request);
            tagName = tagName.trim();
            HttpSession session = request.getSession();

            if (profile.getUsername() != null
                    && !StringUtils.isEmpty(tagName)
                    && !StringUtils.isEmpty(type)
                    && !StringUtils.isEmpty(taggedObject)) {
                if (tagExists(tagName, taggedObject, type)) {
                    return "Already tagged with this tag.";
                }
                if (!TagManager.isValidTagName(tagName)) {
                    return INVALID_NAME_MSG;
                }
                if (tagName.startsWith(TagNames.IM_PREFIX)
                        && !SessionMethods.isSuperUser(session)) {
                    return "You cannot add a tag starting with " + TagNames.IM_PREFIX + ", "
                        + "that is a reserved word.";
                }

                TagManager tagManager = getTagManager();
                tagManager.addTag(tagName, taggedObject, type, profile.getUsername());

                ServletContext servletContext = session.getServletContext();
                if (SessionMethods.isSuperUser(session)) {
                    SearchRepository tr = SessionMethods.
                        getGlobalSearchRepository(servletContext);
                    tr.webSearchableTagChange(type, tagName);
                }
                return "ok";
            }
            return "Adding tag failed.";
        } catch (Throwable e) {
            LOG.error("Adding tag failed", e);
            return "Adding tag failed.";
        }
    }

    /**
     * Deletes tag.
     * @param tagName tag name
     * @param tagged id of tagged object
     * @param type tag type
     * @return 'ok' string if succeeded else error string
     */
    public static String deleteTag(String tagName, String tagged, String type) {
        LOG.info("Called deleteTag(). tagName:" + tagName + " taggedObject:"
                + tagged + " type: " + type);
        try {
            HttpServletRequest request = getRequest();
            HttpSession session = request.getSession();
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            Profile profile  = getProfile(request);
            TagManager manager = im.getTagManager();
            manager.deleteTag(tagName, tagged, type, profile.getUsername());
            ServletContext servletContext = session.getServletContext();
            if (SessionMethods.isSuperUser(session)) {
                SearchRepository tr =
                    SessionMethods.getGlobalSearchRepository(servletContext);
                tr.webSearchableTagChange(type, tagName);
            }
            return "ok";
        } catch (Throwable e) {
            LOG.error("Deleting tag failed", e);
            return "Deleting tag failed.";
        }
    }

    /**
     * Returns all tags of specified tag type together with prefixes of these tags.
     * For instance: for tag 'bio:experiment' it automatically adds 'bio' tag.
     * @param type tag type
     * @return tags
     */
    public static Set<String> getTags(String type) {
        HttpServletRequest request = getRequest();
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        TagManager tagManager = im.getTagManager();
        Profile profile = getProfile(request);
        if (profile.isLoggedIn()) {
            return tagManager.getUserTagNames(type, profile.getUsername());
        }
        return new TreeSet<String>();
    }

    /**
     * Returns all tags by which is specified object tagged.
     * @param type tag type
     * @param tagged id of tagged object
     * @return tags
     */
    public static Set<String> getObjectTags(String type, String tagged) {
        HttpServletRequest request = getRequest();
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        TagManager tagManager = im.getTagManager();
        Profile profile = getProfile(request);
        if (profile.isLoggedIn()) {
            return tagManager.getObjectTagNames(tagged, type, profile.getUsername());
        }
        return new TreeSet<String>();
    }

    private static boolean tagExists(String tag, String taggedObject, String type) {
        HttpServletRequest request = getRequest();
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        TagManager tagManager = im.getTagManager();
        String userName = getProfile(request).getUsername();
        return tagManager.getObjectTagNames(taggedObject, type, userName).contains(tag);
    }


    private static Profile getProfile(HttpServletRequest request) {
        return SessionMethods.getProfile(request.getSession());
    }

    private static HttpServletRequest getRequest() {
        return WebContextFactory.get().getHttpServletRequest();
    }

    private static TagManager getTagManager() {
        HttpServletRequest request = getRequest();
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        return im.getTagManager();
    }

    /**
     * Set the constraint logic on a query to be the given expression.
     *
     * @param expression the constraint logic for the query
     * @throws PathException if the query is invalid
     */
    public static void setConstraintLogic(String expression) throws PathException {
        WebContext ctx = WebContextFactory.get();
        HttpSession session = ctx.getSession();
        PathQuery query = SessionMethods.getQuery(session);
        query.setConstraintLogic(expression);
        List<String> messages = query.fixUpForJoinStyle();
        for (String message : messages) {
            SessionMethods.recordMessage(message, session);
        }
    }

    /**
     * Get the grouped constraint logic
     * @return a list representing the grouped constraint logic
     */
    public static String getConstraintLogic() {
        WebContext ctx = WebContextFactory.get();
        HttpSession session = ctx.getSession();
        PathQuery query = SessionMethods.getQuery(session);
        return (query.getConstraintLogic());
    }

    /**
     * @param suffix string of input before request for more results
     * @param wholeList whether or not to show the entire list or a truncated version
     * @param field field name from the table for the lucene search
     * @param className class name (table in the database) for lucene search
     * @return an array of values for this classname.field
     */
    public String[] getContent(String suffix, boolean wholeList, String field, String className) {
        ServletContext servletContext = WebContextFactory.get().getServletContext();
        AutoCompleter ac = SessionMethods.getAutoCompleter(servletContext);
        ac.createRAMIndex(className + "." + field);
        if (!wholeList && suffix.length() > 0) {
            String[] shortList = ac.getFastList(suffix, field, 31);
            return shortList;
        } else if (suffix.length() > 2 && wholeList) {
            String[] longList = ac.getList(suffix, field);
            return longList;
        }
        String[] defaultList = {""};
        return defaultList;
    }

    @SuppressWarnings("unchecked")
    public String getSavedBagStatus() throws JSONException {
        HttpSession session = WebContextFactory.get().getSession();
        @SuppressWarnings("unchecked")
        Map<String, String> savedBagStatus =
            (Map<String, String>) session.getAttribute(Constants.SAVED_BAG_STATUS);

        // this is where my lists go
        Collection<JSONObject> lists = new HashSet<JSONObject>();
        try {
            for (Map.Entry<String, String> entry : savedBagStatus.entrySet()) {
                // save to the resulting JSON object only if these are 'actionable' lists
                if (entry.getValue().equals(BagState.CURRENT.toString()) ||
                        entry.getValue().equals(BagState.TO_UPGRADE.toString())) {
                    JSONObject list = new JSONObject();
                    list.put("name", entry.getKey());
                    list.put("status", entry.getValue());
                    lists.add(list);
                }
            }
        } catch (JSONException jse) {
            LOG.error("Errors generating json objects", jse);
        }

        return lists.toString();
    }

}
