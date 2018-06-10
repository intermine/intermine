package org.intermine.api.searchengine.solr;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.intermine.api.InterMineAPI;
import org.intermine.api.data.Objects;
import org.intermine.api.searchengine.*;
import org.intermine.api.searchengine.SolrClientFactory;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.*;

/**
 * Solr implementation of KeywordSearchHandler
 *
 * @author arunans23
 */

public final class SolrKeywordSearchHandler implements KeywordSearchHandler
{

    private static final Logger LOG = Logger.getLogger(SolrKeywordSearchHandler.class);

    @Override
    public KeywordSearchResults doKeywordSearch(InterMineAPI im, String queryString, Map<String, String> facetValues, List<Integer> ids, int offSet) {

        //TODO: prepare the querystring

        SolrClient solrClient = SolrClientFactory.getClientInstance(im.getObjectStore());

        QueryResponse resp = null;

        KeywordSearchPropertiesManager keywordSearchPropertiesManager
                = KeywordSearchPropertiesManager.getInstance(im.getObjectStore());

        Vector<KeywordSearchFacetData> facets = keywordSearchPropertiesManager.getFacets();

        try {

            SolrQuery newQuery = new SolrQuery();
            newQuery.setQuery(queryString);
            newQuery.setStart(offSet);
            newQuery.setRows(KeywordSearchPropertiesManager.PER_PAGE);

            for (KeywordSearchFacetData keywordSearchFacetData : facets){
                newQuery.addFacetField(keywordSearchFacetData.getField());
            }

            resp = solrClient.query(newQuery);

            SolrDocumentList results = resp.getResults();

            Set<Integer> objectIds = getObjectIds(results);
            Map<Integer, InterMineObject> objMap = Objects.getObjects(im, objectIds);
            Vector<KeywordSearchResultContainer> searchHits = getSearchHits(results, objMap);

            Collection<KeywordSearchFacet> searchResultsFacets = parseFacets(resp, facets, facetValues);

            return new KeywordSearchResults(searchHits, searchResultsFacets, (int)results.getNumFound());

        } catch (SolrServerException e) {
            e.printStackTrace();

        } catch (IOException e) {

            e.printStackTrace();
        } catch (ObjectStoreException e){
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Set<Integer> getObjectIdsFromSearch(InterMineAPI im, String searchString, int offSet,
                                               Map<String, String> facetValues, List<Integer> ids) {
        //TODO: prepare the querystring

        SolrClient solrClient = SolrClientFactory.getClientInstance(im.getObjectStore());

        QueryResponse resp = null;

        KeywordSearchPropertiesManager keywordSearchPropertiesManager
                = KeywordSearchPropertiesManager.getInstance(im.getObjectStore());

        Vector<KeywordSearchFacetData> facets = keywordSearchPropertiesManager.getFacets();

        try {

            SolrQuery newQuery = new SolrQuery();
            newQuery.setQuery(searchString);
            newQuery.setStart(offSet);
            newQuery.setRows(KeywordSearchPropertiesManager.PER_PAGE);

            for (KeywordSearchFacetData keywordSearchFacetData : facets){
                newQuery.addFacetField(keywordSearchFacetData.getField());
            }

            resp = solrClient.query(newQuery);

            SolrDocumentList results = resp.getResults();

            Set<Integer> objectIds = getObjectIds(results);

            return objectIds;

        } catch (SolrServerException e) {
            e.printStackTrace();

        } catch (IOException e) {

            e.printStackTrace();
        }

        return null;
    }

    /**
     * @param documents the query results.
     *
     * @return set of IDs found in the search results
     */
    public Set<Integer> getObjectIds(SolrDocumentList documents) {
        long time = System.currentTimeMillis();
        Set<Integer> objectIds = new HashSet<Integer>();
        for (int i = 0; i < documents.size(); i++) {

            SolrDocument document = documents.get(i);

            try {
                if (document != null) {
                    objectIds.add(Integer.valueOf(document.getFieldValue("id").toString()));
                }
            } catch (NumberFormatException e) {
                LOG.info("Invalid id '" + document.getFieldValue("id") + "' for hit '"
                        + document + "'", e);
            }
        }
        LOG.debug("Getting IDs took " + (System.currentTimeMillis() - time) + " ms");
        return objectIds;
    }

    /**
     * @param documents search results
     * @param objMap object map
     * @return matching object
     */
    public Vector<KeywordSearchResultContainer> getSearchHits(SolrDocumentList documents,
                                                         Map<Integer, InterMineObject> objMap) {
        long time = System.currentTimeMillis();
        Vector<KeywordSearchResultContainer> searchHits = new Vector<KeywordSearchResultContainer>();
        for (int i = 0; i < documents.size(); i++) {

            SolrDocument document = documents.get(i);

            try {

                if (document == null) {
                    LOG.error("doc is null");
                } else {
                    Integer id          = Integer.valueOf(document.getFieldValue("id").toString());
                    InterMineObject obj = objMap.get(id);
                    searchHits.add(new KeywordSearchResultContainer<SolrDocument>(document, obj, Float.valueOf("2.0")));
                }
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        LOG.debug("Creating list of search hits took " + (System.currentTimeMillis() - time)
                + " ms");
        return searchHits;
    }

    /**
     * @param resp query response from solr
     * @param facetVector facets for search results
     * @param facetValues values for facets
     * @return search result for given facet
     */
    public Vector<KeywordSearchFacet> parseFacets(QueryResponse resp,
                                                         Vector<KeywordSearchFacetData> facetVector, Map<String, String> facetValues) {
        long time = System.currentTimeMillis();
        Vector<KeywordSearchFacet> searchResultsFacets = new Vector<KeywordSearchFacet>();
        for (KeywordSearchFacetData facet : facetVector) {

            List<FacetField> facetFields = resp.getFacetFields();

            FacetField facetField = resp.getFacetField(facet.getField());

            Map<String, Long> items = new HashMap<String, Long>();

            if (facetField != null) {
                for (FacetField.Count count : facetField.getValues()) {
                    if (count.getCount() == 0) {
                        continue;
                    }
                    items.put(count.getName(), count.getCount());
                }
            }

            if (facetField != null) {
                searchResultsFacets.add(new KeywordSearchFacet(facet.getField(), facet
                        .getName(), facetValues.get(facet.getField()), items));
            }
        }
        LOG.debug("Parsing " + searchResultsFacets.size() + " facets took "
                + (System.currentTimeMillis() - time) + " ms");
        return searchResultsFacets;
    }

}
