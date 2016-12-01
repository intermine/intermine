package org.intermine.webservice.server.search;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.lucene.KeywordSearch;
import org.intermine.api.lucene.KeywordSearchFacet;
import org.intermine.api.lucene.KeywordSearchFacetData;
import org.intermine.api.lucene.ResultsWithFacets;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.RequestUtil;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.Exporter;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.search.KeywordSearchResult;
import org.intermine.web.search.SearchUtils;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.output.JSONFormatter;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;
import org.intermine.webservice.server.output.XMLFormatter;

import com.browseengine.bobo.api.BrowseFacet;

/**
 * A service that runs key-word searches.
 * @author Alex Kalderimis
 *
 */
public class QuickSearch extends JSONService
{

    private static final Logger LOG = Logger.getLogger(QuickSearch.class);

    private static final String FACET_PREFIX = "facet_";
    private static final int PREFIX_LEN = FACET_PREFIX.length();

    private Map<String, Map<String, Object>> headerObjs
        = new HashMap<String, Map<String, Object>>();

    private final ServletContext servletContext;

    /**
     * @param im The InterMine state object
     * @param ctx The servlet context so that the index can be located.
     */
    public QuickSearch(InterMineAPI im, ServletContext ctx) {
        super(im);
        this.servletContext = ctx;
    }

    @Override
    protected void execute() throws Exception {
        String contextPath = servletContext.getRealPath("/");
        KeywordSearch.initKeywordSearch(im, contextPath);
        WebConfig wc = InterMineContext.getWebConfig();

        QuickSearchRequest input = new QuickSearchRequest();
        Vector<KeywordSearchFacetData> facets = KeywordSearch.getFacets();
        Map<String, String> facetValues = getFacetValues(facets);

        ResultsWithFacets results = KeywordSearch.runBrowseWithFacets(
                im, input.searchTerm, input.offset, facetValues, input.getListIds());

        Collection<KeywordSearchResult> searchResultsParsed =
                SearchUtils.parseResults(im, wc, results.getHits());

        if (input.getIncludeFacets()) {
            Map<String, Object> facetData = new HashMap<String, Object>();
            for (KeywordSearchFacet kwsf: results.getFacets()) {
                Map<String, Integer> sfData = new HashMap<String, Integer>();
                for (BrowseFacet bf: kwsf.getItems()) {
                    sfData.put(bf.getValue(), bf.getFacetValueHitCount());
                }
                facetData.put(kwsf.getField(), sfData);
            }
            headerObjs.put("facets", facetData);
        }

        QuickSearchResultProcessor processor = getProcessor();
        Iterator<KeywordSearchResult> it = searchResultsParsed.iterator();
        for (int i = 0; input.wantsMore(i) && it.hasNext(); i++) {
            KeywordSearchResult kwsr = it.next();
            output.addResultItem(processor.formatResult(kwsr,
                    input.wantsMore(i + 1) && it.hasNext()));
        }
    }

    @Override
    protected Map<String, Object> getHeaderAttributes() {
        final Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.putAll(super.getHeaderAttributes());
        if (formatIsJSON()) {
            attributes.put(JSONFormatter.KEY_INTRO, "\"results\":[");
            attributes.put(JSONFormatter.KEY_OUTRO, "]");
            attributes.put(JSONFormatter.KEY_HEADER_OBJS, headerObjs);
        }
        return attributes;
    }

    private Map<String, String> getFacetValues(Vector<KeywordSearchFacetData> facets) {
        HashMap<String, String> facetValues = new HashMap<String, String>();
    PARAM_LOOP:
        for (@SuppressWarnings("unchecked")
            Enumeration<String> params = request.getParameterNames();
                params.hasMoreElements();) {
            String param = params.nextElement();
            String value = request.getParameter(param);
            if (!param.startsWith(FACET_PREFIX) || StringUtils.isBlank(value)) {
                continue;
            }
            String facetField = param.substring(PREFIX_LEN);
            if (StringUtils.isBlank(facetField)) {
                continue;
            }
            for (KeywordSearchFacetData facet: facets) {
                if (facetField.equals(facet.getField())) {
                    facetValues.put(facetField, value);
                    continue PARAM_LOOP;
                }
            }
        }
        return facetValues;
    }

    private class QuickSearchRequest
    {

        private final String searchTerm;
        private final int offset;
        private final Integer limit;
        private final String searchBag;
        private final boolean includeFacets;

        QuickSearchRequest() {

            String query = request.getParameter("q");
            if (StringUtils.isBlank(query)) {
                searchTerm = "*:*";
            } else {
                searchTerm = query;
            }
            LOG.debug(String.format("SEARCH TERM: '%s'", searchTerm));

            includeFacets = !Boolean.valueOf(request.getParameter("nofacets"));

            String limitParam = request.getParameter("size");
            Integer lim = null;
            if (!StringUtils.isBlank(limitParam)) {
                try {
                    lim = Integer.valueOf(limitParam);
                } catch (NumberFormatException e) {
                    throw new BadRequestException("Expected a number for size: got " + limitParam);
                }
            }
            this.limit = lim;

            String offsetP = request.getParameter("start");
            int parsed = 0;
            if (!StringUtils.isBlank(offsetP)) {
                try {
                    parsed = Integer.valueOf(offsetP);
                } catch (NumberFormatException e) {
                    throw new BadRequestException("Expected a number for start: got " + offsetP);
                }
            }
            offset = parsed;

            searchBag = request.getParameter("list");
        }

        public boolean wantsMore(int i) {
            if (limit == null) {
                return true;
            }
            return i < limit;
        }

        public boolean getIncludeFacets() {
            return includeFacets;
        }

        public String toString() {
            return String.format("<%s searchTerm=%s offset=%d>",
                    getClass().getName(), searchTerm, offset);
        }

        public List<Integer> getListIds() {
            List<Integer> ids = new ArrayList<Integer>();
            if (!StringUtils.isBlank(searchBag)) {
                LOG.debug("SEARCH BAG: '" + searchBag + "'");
                final BagManager bm = im.getBagManager();
                final Profile p = getPermission().getProfile();
                final InterMineBag bag = bm.getBag(p, searchBag);
                if (bag == null) {
                    throw new BadRequestException(
                            "You do not have access to a bag named '" + searchBag + "'");
                }
                ids.addAll(bag.getContentsAsIds());
            }
            return ids;
        }
    }

    private class QuickSearchXMLFormatter extends XMLFormatter
    {
        @Override
        public String formatResult(List<String> resultRow) {
            return StringUtils.join(resultRow, "");
        }
    }

    private QuickSearchResultProcessor getProcessor() {
        if (formatIsJSON()) {
            return new QuickSearchJSONProcessor();
        } else if (formatIsXML()) {
            return new QuickSearchXMLProcessor();
        } else {
            final String separator;
            if (RequestUtil.isWindowsClient(request)) {
                separator = Exporter.WINDOWS_SEPARATOR;
            } else {
                separator = Exporter.UNIX_SEPARATOR;
            }
            return new QuickSearchTextProcessor(separator);
        }
    }

    @Override
    protected Output makeXMLOutput(PrintWriter out, String separator) {
        ResponseUtil.setXMLHeader(response, "search.xml");
        return new StreamedOutput(out, new QuickSearchXMLFormatter());
    }


}
