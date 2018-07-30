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


public class FacetListService extends JSONService {

    private static final Logger LOG = Logger.getLogger(FacetService.class);

    public FacetListService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {

        KeywordSearchHandler searchHandler = new SolrKeywordSearchHandler();

        //empty Map to pass to the method
        Map<String, String> facetValues = new HashMap<String, String>();

        Collection<KeywordSearchFacet> keywordSearchFacets = searchHandler.doFacetSearch(im, "*:*", facetValues);

        output.setHeaderAttributes(getHeaderAttributes());

        Map<String, List<String>> ckData = new HashMap<String, List<String>>();

        for(KeywordSearchFacet<FacetField.Count> keywordSearchFacet : keywordSearchFacets){
            List<String> facetInnerList = new ArrayList<String>();

            for (FacetField.Count count : keywordSearchFacet.getItems()){
                facetInnerList.add(count.getName());
            }

            ckData.put(keywordSearchFacet.getName(), facetInnerList);
        }

        System.out.println(ckData.toString());

        JSONObject jo = new JSONObject(ckData);

        output.addResultItem(Collections.singletonList(jo.toString()));

    }

}
