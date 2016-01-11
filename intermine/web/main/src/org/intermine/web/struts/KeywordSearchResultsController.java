package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.lucene.KeywordSearch;
import org.intermine.api.lucene.KeywordSearchFacet;
import org.intermine.api.lucene.KeywordSearchFacetData;
import org.intermine.api.lucene.ResultsWithFacets;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.search.KeywordSearchResult;
import org.intermine.web.search.SearchUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Controller for keyword search.
 *
 * @author nils
 */
public class KeywordSearchResultsController extends TilesAction
{
    private static final String QUERY_TERM_ALL = "*:*";
    private static final Logger LOG = Logger.getLogger(KeywordSearchResultsController.class);
    private static Logger searchLog = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        // term
        String searchTerm = request.getParameter("searchTerm");
        LOG.debug("SEARCH TERM: '" + searchTerm + "'");

        // show overview by default
        if (StringUtils.isBlank(searchTerm)) {
            return null;
            // searchTerm = QUERY_TERM_ALL;
        }

        long time = System.currentTimeMillis();
        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        ServletContext servletContext = request.getSession().getServletContext();
        String contextPath = servletContext.getRealPath("/");
        synchronized (this) {
            // if this decreases performance too much we might have to change it
            intialiseLogging(SessionMethods.getWebProperties(servletContext).getProperty(
                    "project.title", "unknown").toLowerCase());
        }
        KeywordSearch.initKeywordSearch(im, contextPath);
        Vector<KeywordSearchFacetData> facets = KeywordSearch.getFacets();
        int totalHits = 0;

        //track the keyword search
        Profile profile = SessionMethods.getProfile(request.getSession());
        im.getTrackerDelegate().trackKeywordSearch(searchTerm, profile,
                request.getSession().getId());
        WebConfig wc = SessionMethods.getWebConfig(request);

        // search in bag (list)
        String searchBag = request.getParameter("searchBag");
        if (searchBag == null) {
            searchBag = "";
        }
        List<Integer> ids = getBagIds(im, request, searchBag);
        int offset = getOffset(request);
        Map<String, String> facetValues = getFacetValues(request, facets);
        LOG.debug("Initializing took " + (System.currentTimeMillis() - time) + " ms");


        long searchTime = System.currentTimeMillis();

        ResultsWithFacets results =
                KeywordSearch.runBrowseWithFacets(im, searchTerm, offset, facetValues, ids);

        Collection<KeywordSearchResult> searchResultsParsed =
                SearchUtils.parseResults(im, wc, results.getHits());

        Collection<KeywordSearchFacet> searchResultsFacets = results.getFacets();
        totalHits = results.getTotalHits();

        logSearch(searchTerm, totalHits, time, offset, searchTime, facetValues, searchBag);
        LOG.debug("SEARCH RESULTS FOR " + searchTerm  + ": " + totalHits);

        // don't display *:* in search box
        if (QUERY_TERM_ALL.equals(searchTerm)) {
            searchTerm = "";
        }

        // there are needed in the form too so we have to use request (i think...)
        request.setAttribute("searchResults", searchResultsParsed);
        request.setAttribute("searchTerm", searchTerm);
        request.setAttribute("searchBag", searchBag);
        request.setAttribute("searchFacetValues", facetValues);

        // used for re-running the search in case of creating a list for ALL results
        request.setAttribute("jsonFacets", javaMapToJSON(facetValues));

        context.putAttribute("searchResults", request.getAttribute("searchResults"));
        context.putAttribute("searchTerm", request.getAttribute("searchTerm"));
        context.putAttribute("searchBag", request.getAttribute("searchBag"));
        context.putAttribute("searchFacetValues", request.getAttribute("searchFacetValues"));
        context.putAttribute("jsonFacets", request.getAttribute("jsonFacets"));

        // pagination
        context.putAttribute("searchOffset", Integer.valueOf(offset));
        context.putAttribute("searchPerPage", Integer.valueOf(KeywordSearch.PER_PAGE));
        context.putAttribute("searchTotalHits", Integer.valueOf(totalHits));

        // facet lists
        context.putAttribute("searchFacets", searchResultsFacets);

        // facet values
        for (Entry<String, String> facetValue : facetValues.entrySet()) {
            context.putAttribute("facet_" + facetValue.getKey(), facetValue.getValue());
        }

        // time for debugging
        long totalTime = System.currentTimeMillis() - time;
        context.putAttribute("searchTime", new Long(totalTime));
        LOG.debug("--> TOTAL: " + (System.currentTimeMillis() - time) + " ms");
        return null;
    }

    private int getOffset(HttpServletRequest request) {
        // offset (-> paging)
        Integer offset = new Integer(0);
        try {
            if (!StringUtils.isBlank(request.getParameter("searchOffset"))) {
                offset = Integer.valueOf(request.getParameter("searchOffset"));
            }
        } catch (NumberFormatException e) {
            LOG.info("invalid offset", e);
        }
        LOG.debug("SEARCH OFFSET: " + offset + "");
        return offset.intValue();
    }

    private  List<Integer> getBagIds(InterMineAPI im, HttpServletRequest request,
            String searchBag) {
        List<Integer> ids = new ArrayList<Integer>();
        if (!StringUtils.isEmpty(searchBag)) {
            LOG.debug("SEARCH BAG: '" + searchBag + "'");
            InterMineBag bag = im.getBagManager().getBag(
                    SessionMethods.getProfile(request.getSession()), searchBag);
            if (bag != null) {
                ids = bag.getContentsAsIds();
                LOG.debug("SEARCH LIST: " + Arrays.toString(ids.toArray()) + "");
            }
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getFacetValues(HttpServletRequest request,
            Vector<KeywordSearchFacetData> facets) {
        HashMap<String, String> facetValues = new HashMap<String, String>();
        // if this is a new search (searchSubmit set) only keep facets if
        // searchSubmitRestricted used
        if (StringUtils.isBlank(request.getParameter("searchSubmit"))
                || !StringUtils.isBlank(request.getParameter("searchSubmitRestricted"))) {
            // find all parameters that begin with facet_ and have a
            // value, add them to map
            for (Entry<String, String[]> requestParameter
                    : ((Map<String, String[]>) request.getParameterMap()).entrySet()) {
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
        return facetValues;
    }

    private void logSearch(String searchTerm, int totalHits, long time, int offset,
            long timeSearch, Map<String, String> facetValues, String searchBag) {
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
        searchLog.debug(searchLogLine);
    }

    private void intialiseLogging(String projectName) throws IOException {
        if (searchLog == null) {
            searchLog = Logger.getLogger(KeywordSearchResultsController.class.getName()
                        + ".searches");
            String logFileName = projectName + "_searches.log";
            Layout layout = new PatternLayout("%d{ISO8601}\t%m%n");
            try {
                RollingFileAppender appender = new RollingFileAppender(layout, logFileName, true);
                appender.setMaximumFileSize(102400); // 100kb
                appender.setMaxBackupIndex(10);
                searchLog.addAppender(appender);
            } catch (FileNotFoundException e) {
                LOG.error("Could not open searches log", e);
                return;
            }
            LOG.info("Logging searches to: " + logFileName);
        }
    }

    private JSONObject javaMapToJSON(Map<String, String> facets) throws JSONException {
        JSONObject jo = new JSONObject();
        JSONArray ja = new JSONArray();
        for (Map.Entry<String, String> entry : facets.entrySet()) {
            JSONObject facet = new JSONObject();
            facet.put("facetName", entry.getKey());
            facet.put("facetValue", entry.getValue());
            ja.put(facet);
        }
        jo.put("facets", ja);
        return jo;
    }
}
