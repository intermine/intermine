package org.intermine.api.searchengine.solr;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.intermine.api.InterMineAPI;
import org.intermine.api.data.Objects;
import org.intermine.api.searchengine.*;
import org.intermine.api.searchengine.solr.SolrClientManager;
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
import java.util.*;

/**
 * Solr implementation of KeywordSearchHandler
 *
 * @author nils
 * @author arunans23
 */

public final class SolrKeywordSearchHandler implements KeywordSearchHandler
{

    private static final Logger LOG = Logger.getLogger(SolrKeywordSearchHandler.class);

    @Override
    public KeywordSearchResults doKeywordSearch(InterMineAPI im, String queryString, Map<String, String> facetValues, List<Integer> ids, int offSet) {

        //TODO: prepare the querystring

        SolrClient solrClient = SolrClientManager.getClientInstance(im.getObjectStore());

        QueryResponse resp = null;

        KeywordSearchPropertiesManager keywordSearchPropertiesManager
                = KeywordSearchPropertiesManager.getInstance(im.getObjectStore());

        Vector<KeywordSearchFacetData> facets = keywordSearchPropertiesManager.getFacets();

        try {

            SolrQuery newQuery = new SolrQuery();
            newQuery.setQuery(queryString);
            newQuery.setStart(offSet);
            newQuery.setRows(KeywordSearchPropertiesManager.PER_PAGE);
            newQuery.addField("score");
            newQuery.addField("id");

            for (KeywordSearchFacetData keywordSearchFacetData : facets){
                newQuery.addFacetField(keywordSearchFacetData.getField());
            }

            // add faceting selections
            for (Map.Entry<String, String> facetValue : facetValues.entrySet()) {
                if (facetValue != null) {
                    newQuery.addFacetQuery(facetValue.getKey()+":"+facetValue.getValue());
                }
            }

            resp = solrClient.query(newQuery);

            SolrDocumentList results = resp.getResults();

            Set<Integer> objectIds = getObjectIds(results);
            Map<Integer, InterMineObject> objMap = Objects.getObjects(im, objectIds);
            Vector<KeywordSearchResultContainer> searchHits = getSearchHits(results, objMap, results.getMaxScore());

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

        SolrClient solrClient = SolrClientManager.getClientInstance(im.getObjectStore());

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
     * @param maxScore maxScore retrieved from solr document for score calculating purpose
     * @return matching object
     */
    public Vector<KeywordSearchResultContainer> getSearchHits(SolrDocumentList documents,
                                                         Map<Integer, InterMineObject> objMap, float maxScore) {
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
                    Float score = Float.valueOf(document.getFieldValue("score").toString())/maxScore;
                    searchHits.add(new KeywordSearchResultContainer<SolrDocument>(document, obj, score));
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

            List<FacetField.Count> counts = facetField.getValues();

            //Empty List to get only the facet fields with zero count
            List<FacetField.Count> countsFiltered = new ArrayList<FacetField.Count>();

            for (FacetField.Count count : counts) {
                if (count.getCount() == 0) {
                    continue;
                }
                countsFiltered.add(count);
            }

            if (facetField != null) {
                searchResultsFacets.add(new KeywordSearchFacet(facet.getField(), facet
                        .getName(), facetValues.get(facet.getField()), countsFiltered));
            }
        }
        LOG.debug("Parsing " + searchResultsFacets.size() + " facets took "
                + (System.currentTimeMillis() - time) + " ms");
        return searchResultsFacets;
    }

}
