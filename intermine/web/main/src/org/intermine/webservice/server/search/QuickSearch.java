package org.intermine.webservice.server.search;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.model.InterMineObject;
import org.intermine.web.logic.RequestUtil;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.Exporter;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.search.KeywordSearch;
import org.intermine.web.search.KeywordSearchFacetData;
import org.intermine.web.search.KeywordSearchHit;
import org.intermine.web.search.KeywordSearchResult;
import org.intermine.web.struts.KeywordSearchFacet;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.output.JSONFormatter;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;
import org.intermine.webservice.server.output.XMLFormatter;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseResult;

public class QuickSearch extends JSONService {

    private static final Logger LOG = Logger.getLogger(QuickSearch.class);

    private static final String FACET_PREFIX = "facet_";
    private static final int PREFIX_LEN = FACET_PREFIX.length();

    private Map<String, Map<String, Object>> HEADER_OBJS
        = new HashMap<String, Map<String, Object>>();

    public QuickSearch(InterMineAPI im) {
        super(im);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void execute() throws Exception {
        javax.servlet.ServletContext servletContext = request.getSession().getServletContext();
        String contextPath = servletContext.getRealPath("/");
        KeywordSearch.initKeywordSearch(im, contextPath);

        QuickSearchRequest input = new QuickSearchRequest();
        Vector<KeywordSearchFacetData> facets = KeywordSearch.getFacets();
        Map<String, String> facetValues = getFacetValues(facets);

        int totalHits = 0;

        long searchTime = System.currentTimeMillis();
        BrowseResult result = KeywordSearch.runBrowseSearch(input.searchTerm, input.offset,
                facetValues, input.getListIds());
        searchTime = System.currentTimeMillis() - searchTime;

        Vector<KeywordSearchResult> searchResultsParsed = new Vector<KeywordSearchResult>();
        Vector<KeywordSearchFacet> searchResultsFacets = new Vector<KeywordSearchFacet>();

        Set<Integer> objectIds = new HashSet<Integer>();
        if (result != null) {
            totalHits = result.getNumHits();
            LOG.debug("Browse found " + result.getNumHits() + " hits");
            BrowseHit[] browseHits = result.getHits();
            objectIds = KeywordSearch.getObjectIds(browseHits);
            Map<Integer, InterMineObject> objMap = KeywordSearch.getObjects(im, objectIds);
            Vector<KeywordSearchHit> searchHits = KeywordSearch.getSearchHits(browseHits, objMap);
            WebConfig wc = SessionMethods.getWebConfig(request);
            searchResultsParsed = KeywordSearch.parseResults(im, wc, searchHits);
            searchResultsFacets = KeywordSearch.parseFacets(result, facets, facetValues);
        }

        if (input.getIncludeFacets()) {
            Map<String, Object> facetData = new HashMap<String, Object>();
            for (KeywordSearchFacet kwsf: searchResultsFacets) {
                Map<String, Integer> sfData = new HashMap<String, Integer>();
                for (BrowseFacet bf: kwsf.getItems()) {
                    sfData.put(bf.getValue(), bf.getFacetValueHitCount());
                }
                facetData.put(kwsf.getField(), sfData);
            }
            HEADER_OBJS.put("facets", facetData);
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
            attributes.put(JSONFormatter.KEY_HEADER_OBJS, HEADER_OBJS);
        }
        return attributes;
    }

    private Map<String, String> getFacetValues(Vector<KeywordSearchFacetData> facets) {
        HashMap<String, String> facetValues = new HashMap<String, String>();
        PARAM_LOOP: for (Enumeration<String> params = request.getParameterNames();
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

    private class QuickSearchRequest {

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
                InterMineBag bag = im.getBagManager().getUserOrGlobalBag(
                        SessionMethods.getProfile(request.getSession()), searchBag);
                if (bag != null) {
                    ids.addAll(bag.getContentsAsIds());
                }
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
