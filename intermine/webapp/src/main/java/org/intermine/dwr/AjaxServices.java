package org.intermine.dwr;

/*
 * Copyright (C) 2002-2020 FlyMine
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
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
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
import org.intermine.api.bag.UnknownBagTypeException;
import org.intermine.api.beans.PartnerLink;
import org.intermine.api.mines.FriendlyMineManager;
import org.intermine.api.mines.Mine;
import org.intermine.api.mines.ObjectRequest;
import org.intermine.api.profile.BagDoesNotExistException;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileAlreadyExistsException;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.SavedQuery;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.UserAlreadyShareBagException;
import org.intermine.api.profile.UserNotFoundException;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.WebTable;
import org.intermine.api.search.SearchRepository;
import org.intermine.api.search.SearchResults;
import org.intermine.api.search.SearchTarget;
import org.intermine.api.search.TagFilter;
import org.intermine.api.search.WebSearchable;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplateSummariser;
import org.intermine.api.util.NameUtil;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.StringUtil;
import org.intermine.metadata.TypeUtil;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.template.TemplateQuery;
import org.intermine.util.Emailer;
import org.intermine.web.autocompletion.AutoCompleter;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.context.MailAction;
import org.intermine.web.displayer.InterMineLinkGenerator;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.PortalHelper;
import org.intermine.web.logic.bag.BagConverter;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.profile.UpgradeBagList;
import org.intermine.web.logic.query.PageTableQueryMonitor;
import org.intermine.web.logic.query.QueryMonitorTimeout;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.WebState;
import org.intermine.web.logic.session.QueryCountQueryMonitor;
import org.intermine.web.logic.session.SessionMethods;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class contains the methods called through DWR Ajax
 *
 * @author Xavier Watkins
 * @author Daniela Butano
 *
 */
@SuppressWarnings("deprecation")
public class AjaxServices
{
    protected static final Logger LOG = Logger.getLogger(AjaxServices.class);
    private static final Object ERROR_MSG = "Error happened during DWR ajax service.";

    private static final Set<String> NON_WS_TAG_TYPES = new HashSet<String>(Arrays.asList(
            TagTypes.CLASS, TagTypes.COLLECTION, TagTypes.REFERENCE));

    /**
     * Creates a favourite Tag for the given templateName
     *
     * @param name the name of the template we want to set as a favourite
     * @param type type of tag (bag or template)
     * @param isFavourite whether or not this item is currently a favourite
     */
    public void setFavourite(String name, String type, boolean isFavourite) {
        String nameCopy = name.replaceAll("#039;", "'");
        try {
            // already a favourite.  turning off.
            if (isFavourite) {
                AjaxServices.deleteTag(TagNames.IM_FAVOURITE, nameCopy, type);
            // not a favourite.  turning on.
            } else {
                AjaxServices.addTag(TagNames.IM_FAVOURITE, nameCopy, type);
            }
        } catch (Exception e) {
            processException(e);
        }
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
            Map<String, ApiTemplate> templates = profile.getSavedTemplates();
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
            Map<String, ApiTemplate> templates = profile.getSavedTemplates();
            ApiTemplate template = templates.get(templateName);
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
                return "<i>" + NameUtil.INVALID_NAME_MSG + "</i>";
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
            } else if ("invalid.bag.type".equals(type)) {
                try {
                    profile.fixInvalidBag(name, newName);
                    InterMineAPI im = SessionMethods.getInterMineAPI(session);
                    new Thread(new UpgradeBagList(profile, im.getBagQueryRunner()))
                        .start();
                } catch (UnknownBagTypeException e) {
                    return "<i>" + e.getMessage() + "</i>";
                } catch (ObjectStoreException e) {
                    return "<i>Error fixing type</i>";
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
            bag.setDescription(StringEscapeUtils.escapeHtml(description));
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
     * This method gets a map of ids of elements that were in the past (during session) toggled and
     * returns them in JSON
     * @return JSON serialized to a String
     * @throws JSONException
     */
    public static String getToggledElements() {
        HttpSession session = WebContextFactory.get().getSession();
        WebState webState = SessionMethods.getWebState(session);
        Collection<JSONObject> lists = new HashSet<JSONObject>();
        try {
            for (Map.Entry<String, Boolean> entry : webState.getToggledElements().entrySet()) {
                JSONObject list = new JSONObject();
                list.put("id", entry.getKey());
                list.put("opened", entry.getValue().toString());
                lists.add(list);
            }
        } catch (JSONException jse) {
            LOG.error("Errors generating json objects", jse);
        }

        return lists.toString();
    }

    /**
     * Get the summary for the given column
     * @param summaryPath the path for the column as a String
     * @param tableName name of column-owning table
     * @return a collection of rows
     * @throws Exception an exception
     */
    public static List<? extends Object> getColumnSummary(
            String tableName, String summaryPath) throws Exception {
        try {
            WebContext ctx = WebContextFactory.get();
            HttpSession session = ctx.getSession();
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            Profile profile = SessionMethods.getProfile(session);
            WebResultsExecutor webResultsExecutor = im.getWebResultsExecutor(profile);

            WebTable webTable = (SessionMethods.getResultsTable(session, tableName))
                                   .getWebTable();
            PathQuery pathQuery = webTable.getPathQuery();
            @SuppressWarnings({ "unchecked", "rawtypes" })
            List<ResultsRow> results = (List)
                webResultsExecutor.summariseQuery(pathQuery, summaryPath);

            // Start the count of results
            Query countQuery = webResultsExecutor.makeSummaryQuery(pathQuery, summaryPath);
            QueryCountQueryMonitor clientState
                = new QueryCountQueryMonitor(Constants.QUERY_TIMEOUT_SECONDS * 1000, countQuery);
            MessageResources messages = (MessageResources) ctx.getHttpServletRequest()
                                                              .getAttribute(Globals.MESSAGES_KEY);
            String qid = SessionMethods.startQueryCount(clientState, session, messages);
            @SuppressWarnings("rawtypes")
            List<ResultsRow> pageSizeResults = new ArrayList<ResultsRow>();
            int rowCount = 0;
            for (@SuppressWarnings("rawtypes") ResultsRow row : results) {
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
     * a format useful in JavaScript.
     * <p>
     * Each element of the returned List is a List containing a
     * WebSearchable name, a score (from Lucene) and a string with the matching parts of the
     * description highlighted.
     * <p>
     * ie - search for "<code>me</code>":
     * <pre>
     *   [
     *     [ "Some name", 0.123, "So&lt;i&gt;me&lt;/i&gt; name" ],
     *     ...
     *   ]
     * </pre>
     *
     * @param scope the scope (either Scope.GLOBAL or Scope.USER).
     * @param type the type (from TagTypes).
     * @param tags the tags to filter on.
     * @param filterText the text to pass to Lucene.
     * @param filterAction toggles favourites filter off and on; will be blank or 'favourites'
     * @param callId unique id
     * @return a List of Lists
     */
    public static List<? extends Object> filterWebSearchables(
                                              String scope, String type,
                                              List<String> tags, String filterText,
                                              String filterAction,
                                              String callId) {
        try {
            final HttpSession session = WebContextFactory.get().getSession();
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            final ProfileManager pm = im.getProfileManager();
            final Profile profile = SessionMethods.getProfile(session);
            final SearchRepository userRepository = profile.getSearchRepository();
            final SearchTarget target = new SearchTarget(scope, type);
            final SearchResults results;

            try {
                results = SearchResults.runLuceneSearch(filterText, target, userRepository);
            } catch (ParseException e) {
                LOG.error("couldn't run lucene filter", e);
                return Arrays.asList(callId);
            } catch (IOException e) {
                LOG.error("couldn't run lucene filter", e);
                return Arrays.asList(callId);
            }

            //Filter by aspects (defined in superuser account)
            List<String> aspectTags = new ArrayList<String>();
            List<String> userTags = new ArrayList<String>();
            for (String tag :tags) {
                if (tag.startsWith(TagNames.IM_ASPECT_PREFIX)) {
                    aspectTags.add(tag);
                } else {
                    if (profile.getUsername() != null) {
                        // Only allow filtering from registered users.
                        userTags.add(tag);
                    }
                }
            }

            TagFilter aspects = new TagFilter(aspectTags, pm.getSuperuserProfile(), type);
            TagFilter requiredTags = new TagFilter(userTags, profile, type);

            List<Object> returnList = new ArrayList<Object>();

            returnList.add(callId);

            for (org.intermine.api.search.SearchResult sr: results) {
                WebSearchable ws = sr.getItem();
                if (SearchResults.isInvalidTemplate(ws)) {
                    continue;
                }
                if (!(aspects.hasAllTags(ws) && requiredTags.hasAllTags(ws))) {
                    continue;
                }
                returnList.add(sr.asList());
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
            int count = 0;
            InterMineBag  imBag = bagManager.getBag(profile, bagName);
            List<ApiTemplate> conversionTemplates = templateManager.getConversionTemplates();
            String bagTypeName = pckName + "." + imBag.getType();
            @SuppressWarnings("unchecked")
            Class<? extends InterMineObject> fromType
                = (Class<? extends InterMineObject>) TypeUtil.instantiate(bagTypeName);
            @SuppressWarnings("unchecked")
            Class<? extends InterMineObject> toType
                = (Class<? extends InterMineObject>) TypeUtil.instantiate(pckName + "." + type);
            PathQuery pathQuery
                = TypeConverter.getConversionQuery(conversionTemplates, fromType, toType, imBag);
            count = webResultsExecutor.count(pathQuery);
            return count;
        } catch (Exception e) {
            LOG.error("failed to get type converted counts", e);
            return 0;
        }
    }

    /**
     * For a list and a converter, return types and related counts
     *
     * @param bagName the name of the bag
     * @param converterName Java class that processes the data
     * @return the number of converted objects
     */
    public static String getCustomConverterCounts(String bagName, String converterName) {
        try {
            final HttpSession session = WebContextFactory.get().getSession();
            final InterMineAPI im = SessionMethods.getInterMineAPI(session);
            final Profile profile = SessionMethods.getProfile(session);
            final BagManager bagManager = im.getBagManager();
            final InterMineBag  imBag = bagManager.getBag(profile, bagName);
            final ServletContext servletContext = WebContextFactory.get().getServletContext();
            final WebConfig webConfig = SessionMethods.getWebConfig(servletContext);
            final BagConverter bagConverter = PortalHelper.getBagConverter(im, webConfig,
                    converterName);
            // should be ordered
            Map<String, String> results = bagConverter.getCounts(profile, imBag);
            List<JSONObject> jsonResults = new LinkedList<JSONObject>();
            for (Map.Entry<String, String> entry : results.entrySet()) {
                JSONObject organism = new JSONObject();
                organism.put("name", entry.getKey());
                organism.put("count", entry.getValue());
                jsonResults.add(organism);
            }
            return jsonResults.toString();
        } catch (Exception e) {
            LOG.error("failed to get custom converter counts", e);
            return null;
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
     * @param domains The domains the domains these identifiers are valid for (eg. organisms).
     * @param idents The external object identifiers.
     * @return the links to friendly intermines
     */
    public static Collection<PartnerLink> getFriendlyMineLinks(String mineName, String domains,
        String idents) {
        if (StringUtils.isEmpty(mineName)
                || StringUtils.isEmpty(domains)
                || StringUtils.isEmpty(idents)) {
            return null;
        }
        final HttpSession session = WebContextFactory.get().getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        final ServletContext servletContext = WebContextFactory.get().getServletContext();
        final Properties webProperties = SessionMethods.getWebProperties(servletContext);
        final FriendlyMineManager fmm = FriendlyMineManager.getInstance(im, webProperties);
        final String linkGeneratorClass = webProperties.getProperty("friendlymines.linkgenerator");

        final Mine mine = fmm.getMine(mineName);
        Collection<PartnerLink> results = Collections.emptySet();
        if (mine == null || mine.getReleaseVersion() == null) {
            LOG.error(mineName + " seems to be dead");
        } else {
            // Mine is alive
            LOG.debug(mine.getName() + " is at " + mine.getReleaseVersion());
            ObjectRequest req = new ObjectRequest(domains, idents);
            MultiKey key = new MultiKey(mine.getName(), req);
            // From cache, or from service.
            results = fmm.getLinks(key);
            if (results == null) {
                InterMineLinkGenerator linkGen = TypeUtil.createNew(linkGeneratorClass);
                if (linkGen != null) {
                    results = linkGen.getLinks(fmm.getLocalMine(), mine, req);
                }
            }
            fmm.cacheLinks(key, results);
        }
        LOG.debug("Links: " + results);
        return results;
    }

    private static String getXMLQuery(String filename, Object... positionalArgs) {
        try {
            return String.format(
                IOUtils.toString(
                        AjaxServices.class.getResourceAsStream(filename)), positionalArgs);
        } catch (IOException e) {
            LOG.error(e);
            throw new RuntimeException("Could not read " + filename, e);
        } catch (NullPointerException npe) {
            LOG.error(npe);
            throw new RuntimeException(filename + " not found", npe);
        } catch (Throwable e) {
            LOG.error(e);
            throw new RuntimeException("Unexpected exception " + e.getMessage());
        }
    }

    /**
     * Return list of disease ontology terms associated with list of provided rat genes.  Returns
     * JSONObject as string with ID (intermine ID) and name (ontologyTerm.name)
     *
     * @param orthologues list of rat genes
     * @return JSONObject of the shape {status :: string, mineUrl :: string, results :: [[string]]}
     */
    public static Map<String, Object> getRatDiseases(String orthologues) {
        if (StringUtils.isEmpty(orthologues)) {
            return null;
        }
        final HashMap<String, Object> map = new HashMap<String, Object>();
        final HttpSession session = WebContextFactory.get().getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        final ServletContext servletContext = WebContextFactory.get().getServletContext();
        final Properties webProperties = SessionMethods.getWebProperties(servletContext);
        final FriendlyMineManager mines = FriendlyMineManager.getInstance(im, webProperties);
        final Mine ratMine = mines.getMine("RatMine");

        if (ratMine == null || ratMine.getReleaseVersion() == null) {
            map.put("status", "offline"); // mine is dead
            return map;
        }

        // It lives!
        map.put("status", "online");
        map.put("mineURL", ratMine.getUrl());

        final String xmlQuery = getXMLQuery("RatDiseases.xml");
        // As object rather than string to get proper escaping.
        PathQuery pq = PathQueryBinding.unmarshalPathQuery(
                new StringReader(xmlQuery), PathQuery.USERPROFILE_VERSION, ratMine.getModel());
        pq.addConstraint(Constraints.lookup("Gene", orthologues, null));

        map.put("results", ratMine.getRows(pq));

        return map;
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
                return TagManager.INVALID_NAME_MSG;
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
     * @param bagName name of new list
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
                        // TODO the javascript method relies on the content of this message.
                        // which is dumb and should be fixed.  in the meantime, don't change this.
                        final String msg = "You are trying to delete the list:  `"
                                + selectedBags[i] + "`, which is used by these queries: "
                                + queries + ".  Select OK to delete the list and queries or Cancel "
                                + "to cancel this operation.";
                        return msg;
                    }
                }
            } else if (!"copy".equals(operation)) {
                Properties properties = SessionMethods.getWebProperties(servletContext);
                String defaultName = properties.getProperty("lists.input.example");
                if (bagName.equalsIgnoreCase(defaultName)) {
                    return "New list name is required";
                } else if ("".equals(bagName)) {
                    return "Please enter a name for your new list";
                } else if (!NameUtil.isValidName(bagName)) {
                    return NameUtil.INVALID_NAME_MSG;
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

        if (StringUtils.isBlank(tagName)) {
            LOG.error("Adding tag failed");
            return "tag must not be blank";
        }
        if (StringUtils.isBlank(taggedObject)) {
            LOG.error("Adding tag failed");
            return "object to tag must not be blank";
        }
        try {
            final HttpServletRequest request = getRequest();
            final Profile profile = getProfile(request);
            final InterMineAPI im = SessionMethods.getInterMineAPI(request);
            tagName = tagName.trim();

            if (profile.getUsername() != null
                    && !StringUtils.isEmpty(tagName)
                    && !StringUtils.isEmpty(type)
                    && !StringUtils.isEmpty(taggedObject)) {
                if (tagExists(tagName, taggedObject, type)) {
                    return "Already tagged with this tag.";
                }

                TagManager tagManager = getTagManager();
                BagManager bm = im.getBagManager();
                TemplateManager tm = im.getTemplateManager();
                if (NON_WS_TAG_TYPES.contains(type)) {
                    if (TagTypes.CLASS.equals(type)) {
                        ClassDescriptor cd = im.getModel().getClassDescriptorByName(taggedObject);
                        tagManager.addTag(tagName, cd, profile);
                    } else {
                        String[] bits = taggedObject.split("\\.");
                        ClassDescriptor cd = im.getModel().getClassDescriptorByName(bits[0]);
                        FieldDescriptor fd = cd.getFieldDescriptorByName(bits[1]);
                        if (fd.isCollection() || fd.isReference()) {
                            tagManager.addTag(tagName, (ReferenceDescriptor) fd, profile);
                        }
                    }
                } else {
                    WebSearchable ws = null;
                    if (TagTypes.BAG.equals(type)) {
                        ws = bm.getBag(profile, taggedObject);
                    } else if (TagTypes.TEMPLATE.equals(type)) {
                        ws = tm.getUserOrGlobalTemplate(profile, taggedObject);
                    }
                    if (ws == null) {
                        throw new RuntimeException("Could not find " + type + " " + taggedObject);
                    } else {
                        tagManager.addTag(tagName, ws, profile);
                    }
                }
                return "ok";
            }
            LOG.error("Adding tag failed: tag='" + tag + "', taggedObject='" + taggedObject
                    + "', type='" + type + "'");
            return "Adding tag failed.";
        } catch (TagManager.TagNamePermissionException e) {
            LOG.error("Adding tag failed", e);
            return e.getMessage();
        } catch (TagManager.TagNameException e) {
            LOG.error("Adding tag failed", e);
            return e.getMessage();
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
            BagManager bm = im.getBagManager();
            TemplateManager tm = im.getTemplateManager();

            if (NON_WS_TAG_TYPES.contains(type)) {
                if (TagTypes.CLASS.equals(type)) {
                    ClassDescriptor cd = im.getModel().getClassDescriptorByName(tagged);
                    manager.deleteTag(tagName, cd, profile);
                } else {
                    String[] bits = tagged.split("\\.");
                    ClassDescriptor cd = im.getModel().getClassDescriptorByName(bits[0]);
                    FieldDescriptor fd = cd.getFieldDescriptorByName(bits[1]);
                    if (fd.isCollection() || fd.isReference()) {
                        manager.deleteTag(tagName, (ReferenceDescriptor) fd, profile);
                    }
                }
                return "ok";
            } else {
                WebSearchable ws = null;
                if (TagTypes.BAG.equals(type)) {
                    ws = (WebSearchable) bm.getBag(profile, tagged);
                } else if (TagTypes.TEMPLATE.equals(type)) {
                    ws = (WebSearchable) tm.getUserOrGlobalTemplate(profile, tagged);
                }
                if (ws == null) {
                    throw new RuntimeException("Could not find " + type + " " + tagged);
                }
                manager.deleteTag(tagName, ws, profile);
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
        final HttpServletRequest request = getRequest();
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        final TagManager tagManager = im.getTagManager();
        final Profile profile = getProfile(request);
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

    /**
     * Return the single use API key for the current profile
     * @return the single use APi key
     */
    public static String getSingleUseKey() {
        HttpServletRequest request = getRequest();
        Profile profile = SessionMethods.getProfile(request.getSession());
        return profile.getSingleUseKey();
    }

    /**
     * Return the request retrieved from the web contest
     * @return the request
     */
    private static HttpServletRequest getRequest() {
        return WebContextFactory.get().getHttpServletRequest();
    }

    /**
     * Return the TagManager
     * @return the tag manager
     */
    private static TagManager getTagManager() {
        HttpServletRequest request = getRequest();
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        return im.getTagManager();
    }

    /**
     * Set the constraint logic on a query to be the given expression.
     *
     * @param expression the constraint logic for the query
     * @return messages to display in the jsp page
     * @throws PathException if the query is invalid
     */
    public static String setConstraintLogic(String expression) throws PathException {
        WebContext ctx = WebContextFactory.get();
        HttpSession session = ctx.getSession();
        PathQuery query = SessionMethods.getQuery(session);
        query.setConstraintLogic(expression);
        List<String> messages = query.fixUpForJoinStyle();
        StringBuffer messagesToDisplay = new StringBuffer();
        for (String message : messages) {
            messagesToDisplay.append(message);
            //SessionMethods.recordMessage(message, session);
        }
        return messagesToDisplay.toString();
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

        // swap "-" for spaces, ticket #2357
        suffix = suffix.replace("-", " ");

        String[] stringList = null;

        if (!wholeList && suffix.length() > 0) {
            stringList = ac.getFastList(suffix, field, className, 31);

        } else if (suffix.length() > 2 && wholeList) {
            // String[] longList = ac.getList(suffix, field);
            // #451 I don't know what I am doing...
            stringList = ac.getFastList(suffix, field, className, 500);
        }

        if (stringList == null) {
            String[] defaultList = {""};
            return defaultList;
        }

        return stringList;
    }

    /**
     * This method gets the latest bags from the session (SessionMethods) and returns them in JSON
     * @return JSON serialized to a String
     * @throws JSONException json exception
     */
    public String getSavedBagStatus() throws JSONException {
        HttpSession session = WebContextFactory.get().getSession();
        Map<String, InterMineBag> savedBags = SessionMethods.getProfile(session).getSavedBags();
        // this is where my lists go
        Collection<JSONObject> lists = new HashSet<JSONObject>();
        try {
            for (Map.Entry<String, InterMineBag> entry : savedBags.entrySet()) {
                InterMineBag bag = entry.getValue();
                // save to the resulting JSON object only if these are 'actionable' lists
                if (bag.isCurrent() || bag.isToUpgrade()) {
                    JSONObject list = new JSONObject();
                    list.put("name", entry.getKey());
                    list.put("status", bag.getState());
                    if (bag.isCurrent()) {
                        try {
                            list.put("size", bag.getSize());
                        } catch (ObjectStoreException os) {
                            LOG.error("Problems retrieving size of bag " + bag.getName(), os);
                        }
                    } else {
                        list.put("size", 0);
                    }
                    lists.add(list);
                }
            }
        } catch (JSONException jse) {
            LOG.error("Errors generating json objects", jse);
        }

        return lists.toString();
    }

    /**
     * Update with the value given in input the field of the previous template
     * saved into the session
     * @param field the field to update
     * @param value the value
     */
    public void updateTemplate(String field, String value) {
        HttpSession session = WebContextFactory.get().getSession();
        boolean isNewTemplate = (session.getAttribute(Constants.NEW_TEMPLATE) != null)
                                ? true : false;
        TemplateQuery templateQuery = (TemplateQuery) SessionMethods.getQuery(session);
        if (!isNewTemplate && session.getAttribute(Constants.PREV_TEMPLATE_NAME) == null) {
            session.setAttribute(Constants.PREV_TEMPLATE_NAME, templateQuery.getName());
        }
        try {
            PropertyUtils.setSimpleProperty(templateQuery, field, value);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Share the bag given in input with the user which userName is input and send email
     * @param userName the user which the bag has to be shared with
     * @param bagName the bag name to share
     * @return 'ok' string if succeeded else error string
     */
    public String addUserToShareBag(String userName, String bagName) {
        HttpSession session = WebContextFactory.get().getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        final Profile profile = SessionMethods.getProfile(session);
        final BagManager bagManager = im.getBagManager();
        final ProfileManager pm = profile.getProfileManager();

        final InterMineBag bag = profile.getSavedBags().get(bagName);
        final Profile invitee = pm.getProfile(userName);
        if (bag == null) {
            return "This is not one of your lists";
        }
        if (invitee == null || invitee.getPreferences().containsKey(Constants.HIDDEN)) {
            return "User not found."; // Users can request not to be found.
        }
        if (profile.getUsername().equals(userName)) {
            return "You are trying to share this with yourself.";
        }
        try {
            bagManager.shareBagWithUser(bag, invitee);
        } catch (UserNotFoundException e1) {
            return "User not found."; // Shouldn't happen now, but, hey ho.
        } catch (UserAlreadyShareBagException e2) {
            return "The user already shares the bag.";
        }

        boolean queuedMessage = InterMineContext.queueMessage(new MailAction() {
            @Override
            public void act(Emailer emailer) throws Exception {
                emailer.informUserOfNewSharedBag(invitee.getEmailAddress(), profile, bag);
            }
        });
        if (!queuedMessage) {
            LOG.warn("Message queue full.");
        }
        return "ok";
    }

    /**
     * Un-share the bag given in input with the user which userName is input
     * @param userName the user which the bag has to be un-shared with
     * @param bagName the bag name to un-share
     * @return 'ok' string if succeeded else error string
     */
    public String deleteUserToShareBag(String userName, String bagName) {
        HttpSession session = WebContextFactory.get().getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);
        BagManager bagManager = im.getBagManager();
        try {
            bagManager.unshareBagWithUser(bagName, profile.getUsername(), userName);
        } catch (UserNotFoundException unfe) {
            return "User not found.";
        } catch (BagDoesNotExistException bnee) {
            return "That list does not exist.";
        }
        return "ok";
    }

    /**
     * Return the list of users who have access to this bag because it has been
     * shared with them.
     *
     * @param bagName the bag name that the users share
     * @return the list of users
     */
    public Collection<String> getUsersSharingBag(String bagName) {
        HttpSession session = WebContextFactory.get().getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);
        BagManager bagManager = im.getBagManager();
        return bagManager.getUsersSharingBag(bagName, profile.getUsername());
    }
}
