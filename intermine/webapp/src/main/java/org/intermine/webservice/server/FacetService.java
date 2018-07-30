package org.intermine.webservice.server;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField;
import org.intermine.api.InterMineAPI;
import org.intermine.api.searchengine.KeywordSearchFacet;
import org.intermine.api.searchengine.KeywordSearchFacetData;
import org.intermine.api.searchengine.KeywordSearchHandler;
import org.intermine.api.searchengine.KeywordSearchPropertiesManager;
import org.intermine.api.searchengine.solr.SolrKeywordSearchHandler;
import org.intermine.webservice.server.core.JSONService;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.*;


public class FacetService extends JSONService {

    private static final Logger LOG = Logger.getLogger(FacetService.class);

    public FacetService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {

        KeywordSearchPropertiesManager keywordSearchPropertiesManager
                = KeywordSearchPropertiesManager.getInstance(im.getObjectStore());

        Vector<KeywordSearchFacetData> facets = keywordSearchPropertiesManager.getFacets();

        KeywordSearchHandler searchHandler = new SolrKeywordSearchHandler();

        String searchTerm = request.getParameter("q");

        Map<String, String> facetValues = getFacetValues(request, facets);

        Collection<KeywordSearchFacet> keywordSearchFacets = searchHandler.doFacetSearch(im, searchTerm, facetValues);

        output.setHeaderAttributes(getHeaderAttributes());

        Map<String, Map<String, Long>> ckData = new HashMap<String, Map<String, Long>>();

        JSONArray rootArray = new JSONArray();

        for(KeywordSearchFacet<FacetField.Count> keywordSearchFacet : keywordSearchFacets){
            Map<String, Long> temp = new HashMap<String, Long>();

            JSONArray innerArray = new JSONArray();
            for (FacetField.Count count : keywordSearchFacet.getItems()){
                temp.put(count.getName(), count.getCount());
                JSONObject innerObject = new JSONObject();
                innerObject.put("name", count.getName());
                innerObject.put("value", count.getCount());

                innerArray.put(innerObject);
            }

            ckData.put(keywordSearchFacet.getName(), temp);
            JSONObject outerObject  = new JSONObject();
            outerObject.put(keywordSearchFacet.getName(), innerArray);
            rootArray.put(outerObject);
        }

        System.out.println(rootArray.toString());

        JSONObject jo = new JSONObject();
        jo.put("results", rootArray);

        output.addResultItem(Collections.singletonList(jo.toString()));

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
            for (Map.Entry<String, String[]> requestParameter
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

            for (Map.Entry<String, String> facetValue : facetValues.entrySet()) {
                LOG.debug("SEARCH FACET: " + facetValue.getKey() + " = " + facetValue.getValue());
            }
        }
        return facetValues;
    }
}
