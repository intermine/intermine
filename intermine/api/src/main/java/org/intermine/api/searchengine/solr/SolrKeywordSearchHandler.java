package org.intermine.api.searchengine.solr;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.intermine.api.InterMineAPI;
import org.intermine.api.data.Objects;
import org.intermine.api.searchengine.KeywordSearchFacet;
import org.intermine.api.searchengine.KeywordSearchFacetData;
import org.intermine.api.searchengine.KeywordSearchHandler;
import org.intermine.api.searchengine.KeywordSearchPropertiesManager;
import org.intermine.api.searchengine.KeywordSearchResultContainer;
import org.intermine.api.searchengine.KeywordSearchResults;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

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
    public KeywordSearchResults doKeywordSearch(InterMineAPI im, String queryString, Map<String,
            String> facetValues, List<Integer> ids, int offSet) {

        KeywordSearchPropertiesManager keywordSearchPropertiesManager
                = KeywordSearchPropertiesManager.getInstance(im.getObjectStore());
        Vector<KeywordSearchFacetData> facets = keywordSearchPropertiesManager.getFacets();

        QueryResponse resp = performSearch(im, queryString, facetValues, ids, offSet,
                keywordSearchPropertiesManager.PER_PAGE);

        SolrDocumentList results = resp.getResults();

        Set<Integer> objectIds = getObjectIds(results);

        Map<Integer, InterMineObject> objMap = null;

        try {
            objMap = Objects.getObjects(im, objectIds);

        } catch (ObjectStoreException e) {
            LOG.error("ObjectStoreException for query term : " + queryString, e);
        }

        Vector<KeywordSearchResultContainer> searchHits
                = getSearchHits(results, objMap, results.getMaxScore());

        Collection<KeywordSearchFacet> searchResultsFacets
                = parseFacets(resp, facets, facetValues);

        return new KeywordSearchResults(searchHits, searchResultsFacets,
                (int) results.getNumFound());

    }

    @Override
    public Set<Integer> getObjectIdsFromSearch(InterMineAPI im, String searchString, int offSet,
                                               Map<String, String> facetValues,
                                               List<Integer> ids, int listSize) {

        if (listSize == 0) {
            listSize = 10000;
        }

        QueryResponse resp = performSearch(im, searchString, facetValues, ids, offSet, listSize);

        SolrDocumentList results = resp.getResults();

        Set<Integer> objectIds = getObjectIds(results);

        return objectIds;
    }

    @Override
    public Collection<KeywordSearchFacet> doFacetSearch(InterMineAPI im, String queryString,
                                                        Map<String, String> facetValues) {

        KeywordSearchPropertiesManager keywordSearchPropertiesManager
                = KeywordSearchPropertiesManager.getInstance(im.getObjectStore());
        Vector<KeywordSearchFacetData> facets = keywordSearchPropertiesManager.getFacets();

        QueryResponse resp = performSearch(im, queryString, facetValues, null, 0, 0);

        SolrDocumentList results = resp.getResults();

        Collection<KeywordSearchFacet> searchResultsFacets = parseFacets(resp, facets, facetValues);

        return searchResultsFacets;

    }

    /**
     * @param documents the query results.
     *
     * @return set of IDs found in the search results
     */
    private Set<Integer> getObjectIds(SolrDocumentList documents) {
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
    private Vector<KeywordSearchResultContainer> getSearchHits(SolrDocumentList documents,
                                                              Map<Integer, InterMineObject> objMap,
                                                              float maxScore) {
        long time = System.currentTimeMillis();

        Vector<KeywordSearchResultContainer> searchHits
                = new Vector<KeywordSearchResultContainer>();

        for (int i = 0; i < documents.size(); i++) {

            SolrDocument document = documents.get(i);

            try {

                if (document == null) {
                    LOG.error("doc is null");
                } else {
                    Integer id          = Integer.valueOf(document.getFieldValue("id").toString());
                    InterMineObject obj = objMap.get(id);

                    Float score = Float.valueOf(document.getFieldValue("score")
                            .toString()) / maxScore;

                    searchHits.add(
                            new KeywordSearchResultContainer<SolrDocument>(document, obj, score));
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
    private Vector<KeywordSearchFacet> parseFacets(QueryResponse resp,
                                                  Vector<KeywordSearchFacetData> facetVector,
                                                  Map<String, String> facetValues) {
        long time = System.currentTimeMillis();
        Vector<KeywordSearchFacet> searchResultsFacets = new Vector<KeywordSearchFacet>();
        for (KeywordSearchFacetData facet : facetVector) {

            List<FacetField> facetFields = resp.getFacetFields();

            FacetField facetField = resp.getFacetField("facet_" + facet.getField());

            List<FacetField.Count> counts = facetField.getValues();

            //Empty List to get only the facet fields without zero count
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

    /**
     * This method is used to get the field names that are indexed from the solr managed schema
     * @param solrClient a solrclient instance
     * @return a list of field names
     */
    private List<String> getFieldNamesFromSolrSchema(SolrClient solrClient) {
        List<String> fieldNames = null;

        try {
            SchemaRequest.Fields request = new SchemaRequest.Fields();
            SchemaResponse.FieldsResponse response =  request.process(solrClient);

            List<Map<String, Object>> fieldList = response.getFields();

            fieldNames = new ArrayList<String>();

            for (int i = 0; i < fieldList.size(); i++) {
                String value = fieldList.get(i).get("name").toString();

                if (("_root_").equals(value) || ("_text_").equals(value)
                        || ("_version_").equals(value) || ("facet_").equals(value)) {
                    continue;
                }

                fieldNames.add(value);
            }

        } catch (Exception e) {
            LOG.error("Retrieving fieldNames failed", e);
            e.printStackTrace();
        }

        return fieldNames;
    }


    private QueryResponse performSearch(InterMineAPI im, String queryString, Map<String,
                                        String> facetValues, List<Integer> ids,
                                        int offSet, int rowSize) {

        SolrClient solrClient = SolrClientManager.getClientInstance(im.getObjectStore());

        QueryResponse resp = null;

        KeywordSearchPropertiesManager keywordSearchPropertiesManager
                = KeywordSearchPropertiesManager.getInstance(im.getObjectStore());

        Vector<KeywordSearchFacetData> facets = keywordSearchPropertiesManager.getFacets();

        Map<ClassDescriptor, Float> classBoost = keywordSearchPropertiesManager.getClassBoost();

        List<String> fieldNames = getFieldNamesFromSolrSchema(solrClient);

        try {

            SolrQuery newQuery = new SolrQuery();
            newQuery.setQuery(queryString);
            newQuery.setStart(offSet);
            newQuery.setRows(rowSize);
            newQuery.addField("score");
            newQuery.addField("id");
            newQuery.add("defType", "edismax");

            for (KeywordSearchFacetData keywordSearchFacetData : facets) {
                newQuery.addFacetField("facet_" + keywordSearchFacetData.getField());
            }

            // add faceting selections
            for (Map.Entry<String, String> facetValue : facetValues.entrySet()) {
                if (facetValue != null) {
                    newQuery.addFilterQuery(facetValue.getKey() + ":\""
                        + facetValue.getValue() + "\"");
                }
            }

            //limiting the query based on search bag
            if (ids != null && !ids.isEmpty()) {
                for (int id : ids) {
                    newQuery.addFilterQuery("id", Integer.toString(id));
                }
            }

            String boostQuery = "";

            for (Map.Entry<ClassDescriptor, Float> boostValue : classBoost.entrySet()) {
                if (boostValue != null) {
                    boostQuery += "classname:" + boostValue.getKey().getUnqualifiedName()
                            + "^" + boostValue.getValue() + " ";
                }
            }

            LOG.info("BoostQuery : " + boostQuery);

            String fieldListQuery = "";

            for (String field : fieldNames) {
                fieldListQuery = fieldListQuery + field;
                if (field.endsWith("_raw")) {
                    fieldListQuery = fieldListQuery + "^2.0";
                }
                fieldListQuery = fieldListQuery + " ";
            }

            LOG.info("Field List Query : " + fieldListQuery);


            newQuery.add("bq", boostQuery);
            newQuery.add("qf", fieldListQuery);

            resp = solrClient.query(newQuery, SolrRequest.METHOD.POST);

            return resp;

        } catch (SolrServerException e) {
            LOG.error("Query performed on solr failed for search term : " + queryString, e);
            e.printStackTrace();

        } catch (IOException e) {
            LOG.error("Query performed on solr failed for search term : " + queryString, e);
            e.printStackTrace();

        }

        return resp;
    }

}
