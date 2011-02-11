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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.lucene.document.Document;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.search.KeywordSearch;
import org.intermine.web.search.KeywordSearchFacetData;
import org.intermine.web.search.KeywordSearchHit;

import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.FacetAccessible;

/**
 * Controller for keyword search.
 *
 * @author nils
 */
public class KeywordSearchResultsController extends TilesAction
{
    private static final Logger LOG = Logger.getLogger(KeywordSearchResultsController.class);
    private static Logger logSearches = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        long time = System.currentTimeMillis();
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());

        javax.servlet.ServletContext servletContext = request.getSession().getServletContext();
        String contextPath = servletContext.getRealPath("/");

        synchronized (this) { // if this decreases performance too much we might
            // have to change it
            if (logSearches == null) {
                logSearches =
                        Logger.getLogger(KeywordSearchResultsController.class.getName()
                                + ".searches");

                String logFileName =
                        SessionMethods.getWebProperties(servletContext).getProperty(
                                "project.title", "unknown").toLowerCase()
                                + "_searches.log";

                Layout layout = new PatternLayout("%d{ISO8601}\t%m%n");

                RollingFileAppender appender = new RollingFileAppender(layout, logFileName, true);
                appender.setMaximumFileSize(102400); // 100kb
                appender.setMaxBackupIndex(10);

                logSearches.addAppender(appender);
                LOG.info("Logging searches to: " + logFileName);
            }
        }

        KeywordSearch.initKeywordSearch(im, contextPath);
        Vector<KeywordSearchResult> searchResultsParsed = new Vector<KeywordSearchResult>();
        Vector<KeywordSearchFacet> searchResultsFacets = new Vector<KeywordSearchFacet>();
        Vector<KeywordSearchFacetData> facets = KeywordSearch.getFacets();
        int totalHits = 0;

        WebConfig webconfig = SessionMethods.getWebConfig(request);
        Model model = im.getModel();
        Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();

        // term
        String searchTerm = request.getParameter("searchTerm");
        LOG.debug("SEARCH TERM: '" + searchTerm + "'");

        // search in bag (list)
        List<Integer> ids = null;
        String searchBag = request.getParameter("searchBag");
        if (searchBag == null) {
            searchBag = "";
        }

        if (!StringUtils.isEmpty(searchBag)) {
            LOG.debug("SEARCH BAG: '" + searchBag + "'");
            InterMineBag bag = im.getBagManager().getUserOrGlobalBag(
                    SessionMethods.getProfile(request.getSession()), searchBag);
            if (bag != null) {
                ids = bag.getContentsAsIds();
                LOG.debug("SEARCH LIST: " + Arrays.toString(ids.toArray()) + "");
            }
        }

        // offset (-> paging)
        int offset = 0;

        try {
            if (!StringUtils.isBlank(request.getParameter("searchOffset"))) {
                offset = Integer.valueOf(request.getParameter("searchOffset"));
            }
        } catch (NumberFormatException e) {
            LOG.info("invalid offset", e);
        }
        LOG.debug("SEARCH OFFSET: " + offset + "");

        // faceting
        HashMap<String, String> facetValues = new HashMap<String, String>();
        // if this is a new search (searchSubmit set) only keep facets if
        // searchSubmitRestricted used
        if (StringUtils.isBlank(request.getParameter("searchSubmit"))
                || !StringUtils.isBlank(request.getParameter("searchSubmitRestricted"))) {
            // find all parameters that begin with facet_ and have a
            // value, add them to map
            for (Entry<String, String[]> requestParameter : ((Map<String, String[]>) request
                    .getParameterMap()).entrySet()) {
                if (requestParameter.getKey().startsWith("facet_")
                        && requestParameter.getValue().length > 0
                        && !StringUtils.isBlank(requestParameter.getValue()[0])) {
                    String facetField = requestParameter.getKey().substring("facet_".length());
                    boolean found = false;
                    for (KeywordSearchFacetData keywordSearchFacetData : facets) {
                        if (facetField.equals(keywordSearchFacetData.getField())) {
                            found = true;
                            break;
                        }
                    }

                    if (found) {
                        facetValues.put(facetField, requestParameter.getValue()[0]);
                    }
                }
            }

            for (Entry<String, String> facetValue : facetValues.entrySet()) {
                LOG.debug("SEARCH FACET: " + facetValue.getKey() + " = " + facetValue.getValue());
            }
        }

        LOG.debug("Initializing took " + (System.currentTimeMillis() - time) + " ms");

        // show overview by default
        if (StringUtils.isBlank(searchTerm)) {
            searchTerm = "*:*";
        }

        if (!StringUtils.isBlank(searchTerm)) {
            // KeywordSearch.runLuceneSearch(searchTerm); //TODO remove - just
            // for performance testing

            Vector<KeywordSearchHit> searchHits = new Vector<KeywordSearchHit>();
            long timeSearch = System.currentTimeMillis();
            BrowseResult result = KeywordSearch.runBrowseSearch(searchTerm, offset, facetValues,
                    ids);
            timeSearch = System.currentTimeMillis() - timeSearch;

            long time2 = System.currentTimeMillis();
            if (result != null) {
                totalHits = result.getNumHits();
                LOG.debug("Browse found " + result.getNumHits() + " hits");

                BrowseHit[] browseHits = result.getHits();

                HashSet<Integer> objectIds = new HashSet<Integer>();
                for (BrowseHit browseHit : browseHits) {
                    try {
                        Document doc = browseHit.getStoredFields();
                        if (doc != null) {
                            objectIds.add(Integer.valueOf(doc.getFieldable("id").stringValue()));
                        } else {
                            LOG.error("doc is null for browseHit " + browseHit);
                        }
                    } catch (NumberFormatException e) {
                        LOG.info("Invalid id '" + browseHit.getField("id") + "' for hit '"
                                + browseHit + "'", e);
                    }
                }

                LOG.debug("Getting IDs took " + (System.currentTimeMillis() - time2) + " ms");
                time2 = System.currentTimeMillis();

                // fetch objects for the IDs returned by lucene search
                Map<Integer, InterMineObject> objMap = new HashMap<Integer, InterMineObject>();
                for (InterMineObject obj : im.getObjectStore().getObjectsByIds(objectIds)) {
                    objMap.put(obj.getId(), obj);
                }

                LOG.debug("Getting objects took " + (System.currentTimeMillis() - time2) + " ms");
                time2 = System.currentTimeMillis();

                for (BrowseHit browseHit : browseHits) {
                    try {
                        Document doc = browseHit.getStoredFields();
                        if (doc != null) {
                            InterMineObject obj =
                                    objMap.get(Integer
                                            .valueOf(doc.getFieldable("id").stringValue()));

                            searchHits.add(new KeywordSearchHit(browseHit.getScore(), doc, obj));
                        } else {
                            LOG.error("doc is null for browseHit " + browseHit);
                        }
                    } catch (NumberFormatException e) {
                    }
                }

                LOG.debug("Creating list of search hits took "
                        + (System.currentTimeMillis() - time2) + " ms");
                time2 = System.currentTimeMillis();

                for (KeywordSearchHit keywordSearchHit : searchHits) {
                    Class<?> objectClass = DynamicUtil.getSimpleClass(keywordSearchHit.getObject()
                            .getClass());
                    ClassDescriptor classDescriptor =
                            model.getClassDescriptorByName(objectClass.getName());

                    KeywordSearchResult ksr =
                            new KeywordSearchResult(webconfig, keywordSearchHit.getObject(),
                                    classKeys, classDescriptor, keywordSearchHit.getScore(),
                                    // templatesForClass.get(classDescriptor)
                                    null);

                    searchResultsParsed.add(ksr);
                }

                LOG.debug("Parsing search hits took " + (System.currentTimeMillis() - time2)
                        + " ms");
                time2 = System.currentTimeMillis();

                for (KeywordSearchFacetData facet : facets) {
                    FacetAccessible boboFacet = result.getFacetMap().get(facet.getField());

                    if (boboFacet != null) {
                        searchResultsFacets.add(new KeywordSearchFacet(facet.getField(), facet
                                .getName(), facetValues.get(facet.getField()), boboFacet
                                .getFacets()));
                    }
                }

                LOG.debug("Parsing " + searchResultsFacets.size() + " facets took "
                        + (System.currentTimeMillis() - time2) + " ms");
                time2 = System.currentTimeMillis();
            }

            // log this search to search log
            StringBuilder searchLogLine = new StringBuilder();
            searchLogLine.append("query=").append(searchTerm).append("; ");
            searchLogLine.append("hits=").append(totalHits).append("; ");
            searchLogLine.append("timeTotal=").append(System.currentTimeMillis() - time).append(
                    "; ");
            searchLogLine.append("timeSearch=").append(timeSearch).append("; ");
            searchLogLine.append("offset=").append(offset).append("; ");

            searchLogLine.append("restrictions=");
            for (Entry<String, String> facetValue : facetValues.entrySet()) {
                searchLogLine.append(facetValue.getKey()).append(":'")
                        .append(facetValue.getValue()).append("', ");
            }
            searchLogLine.append("; ");

            searchLogLine.append("bag=").append(searchBag).append(";");

            logSearches.info(searchLogLine);
        }

        LOG.debug("SEARCH RESULTS: " + searchResultsParsed.size());

        // don't display *:* in search box
        if ("*:*".equals(searchTerm)) {
            searchTerm = "";
        }

        // there are needed in the form too so we have to use request (i
        // think...)
        request.setAttribute("searchResults", searchResultsParsed);
        request.setAttribute("searchTerm", searchTerm);
        request.setAttribute("searchBag", searchBag);
        request.setAttribute("searchFacetValues", facetValues);

        context.putAttribute("searchResults", request.getAttribute("searchResults"));
        context.putAttribute("searchTerm", request.getAttribute("searchTerm"));
        context.putAttribute("searchBag", request.getAttribute("searchBag"));
        context.putAttribute("searchFacetValues", request.getAttribute("searchFacetValues"));

        // pagination
        context.putAttribute("searchOffset", offset);
        context.putAttribute("searchPerPage", KeywordSearch.PER_PAGE);
        context.putAttribute("searchTotalHits", totalHits);

        // facet lists
        context.putAttribute("searchFacets", searchResultsFacets);

        // facet values
        for (Entry<String, String> facetValue : facetValues.entrySet()) {
            context.putAttribute("facet_" + facetValue.getKey(), facetValue.getValue());
        }

        // time for debugging
        context.putAttribute("searchTime", System.currentTimeMillis() - time);

        LOG.debug("--> TOTAL: " + (System.currentTimeMillis() - time) + " ms");

        return null;
    }
}
