package org.intermine.web.autocompletion;

/*
 * Copyright (C) 2002-2017 FlyMine
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
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SuggesterResponse;
import org.apache.solr.client.solrj.response.Suggestion;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.intermine.api.searchengine.KeywordSearchPropertiesManager;

import java.io.IOException;
import java.util.*;

/**
 * Creates the indexes for Autocomplete fields
 * @author Dominik Grimm
 * @author arunans23
 */
public class SolrIndex {

    private static final Logger LOG = Logger.getLogger(SolrIndex.class);

    private List<SearchObjectClass> lObjClass = null;

    public SolrIndex() {
        this.lObjClass = new Vector<SearchObjectClass>();
    }

    private void indexClass(SearchObjectClass objClass) {
        try {

            String urlString = "http://localhost:8983/solr/autocomplete";
            HttpSolrClient solrClient = new HttpSolrClient.Builder(urlString).build();

            try {
                solrClient.deleteByQuery("*:*");
                solrClient.commit();
            } catch (SolrServerException e) {
                LOG.error("Deleting old index failed", e);
            }

            for(String fieldName: objClass.getFieldNames()){
                Map<String, Object> fieldAttributes = new HashMap();
                fieldAttributes.put("name", fieldName);
                fieldAttributes.put("type", "text_general");
                fieldAttributes.put("stored", true);
                fieldAttributes.put("indexed", true);
                fieldAttributes.put("multiValued", true);
                fieldAttributes.put("required", false);

                try{
                    SchemaRequest.AddField schemaRequest = new SchemaRequest.AddField(fieldAttributes);
                    SchemaResponse.UpdateResponse response =  schemaRequest.process(solrClient);

                } catch (SolrServerException e){
                    LOG.error("Error while adding autocomplete fields to the solrclient.", e);

                    e.printStackTrace();
                }
            }

            //adding copy field to solr so that all the fields can be recognised by suggest handler

            try{

                List<String> copyFieldAttributes = new ArrayList<String>();
                copyFieldAttributes.add("suggestTerm");

                SchemaRequest.AddCopyField schemaCopyRequest = new SchemaRequest.AddCopyField("*", copyFieldAttributes);
                SchemaResponse.UpdateResponse copyFieldResponse =  schemaCopyRequest.process(solrClient);

            } catch (SolrServerException e){
                LOG.error("Error while adding autocomplete copy field to the solrclient.", e);

                e.printStackTrace();
            }

            List<SolrInputDocument> solrInputDocumentList = new ArrayList<SolrInputDocument>();

            for (int i = 0; i < objClass.getSizeValues(); i++) {
                SolrInputDocument doc = new SolrInputDocument();
                for (int j = 0; j < objClass.getSizeFields(); j++) {
                    doc.addField(objClass.getFieldName(j),
                            objClass.getValuesForField(objClass.getFieldName(j), i));
                }

                solrInputDocumentList.add(doc);
            }

            try {
                UpdateResponse response = solrClient.add(solrInputDocumentList);

                solrClient.commit();
            } catch (SolrServerException e) {

                LOG.error("Error while commiting the AutoComplete SolrInputdocuments to the Solrclient. " +
                        "Make sure the Solr instance is up", e);

                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * added a new SearchObjectClass to internal map
     * @param objClass SearchObjectClass which contains the data for the specific field
     * @return true if adding to the map was successful else objectclass is already added
     */
    public boolean addClass(SearchObjectClass objClass) {
        if (!lObjClass.contains(objClass)) {
            lObjClass.add(objClass);
            return true;
        }
        return false;
    }

    /**
     * rebuild all indexes from SearchObjectClasses in the map
     * @throws IOException IOException
     */
    public void rebuildClassIndexes() throws IOException {

        for (int i = 0; i < lObjClass.size(); i++) {
            indexClass(lObjClass.get(i));
        }

    }

    /**
     * Perform the lucene search.
     * @param queryString
     *            the string for what you search in the indexes
     * @param toSearch
     *            the field in which you search
     * @return Hits list of documents (search results)
     * @throws IOException
     *             IOException
     */
    public static String[] fastSearch(String queryString, String toSearch, int n) {

//        if (!"".equals(queryString) && !queryString.trim().startsWith("*")) {
//
//            Query query;
//
//            if (queryString.endsWith(" ")) {
//                queryString = queryString.substring(0, queryString.length() - 1);
//            }
//
//            String[] tmp;
//            if (queryString.contains(" ")) {
//                tmp = queryString.replaceAll(" +", " ").trim().split(" ");
//                queryString = new String();
//
//                for (int i = 0; i < tmp.length; i++) {
//                    queryString += tmp[i];
//                    if (i < tmp.length - 1) {
//                        queryString += "* AND ";
//                    }
//                }
//            }
//            query = parser.parse(queryString + "*");
//
//            return indexSearch.search(query, 500); // FIXME: hardcoded maximum
//            // number of results
//        }

        SolrClient solrClient = SolrClientHandler.getClientInstance();
        QueryResponse resp = null;
        try {

            SolrQuery newQuery = new SolrQuery();
            newQuery.setQuery(queryString);
            newQuery.setRequestHandler("suggest");
            newQuery.setRows(n); // FIXME: hardcoded maximum

            resp = solrClient.query(newQuery);

            SuggesterResponse suggesterResponse = resp.getSuggesterResponse();
            Map<String, List<Suggestion>> array = suggesterResponse.getSuggestions();

            System.out.println(array.toString());

            SolrDocumentList results = resp.getResults();

            String[] stringResults = new String[results.size()];

            return stringResults;

        } catch (SolrServerException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
