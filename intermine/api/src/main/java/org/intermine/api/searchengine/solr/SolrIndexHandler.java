package org.intermine.api.searchengine.solr;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.AnalyzerDefinition;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.SolrInputDocument;
import org.intermine.api.searchengine.IndexHandler;
import org.intermine.api.searchengine.KeywordSearchFacetData;
import org.intermine.api.searchengine.KeywordSearchPropertiesManager;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.ObjectPipe;

import java.io.IOException;
import java.util.*;

/**
 * Solr Implementation of IndexHandler
 *
 * @author arunans23
 */
public final class SolrIndexHandler implements IndexHandler
{
    private final Logger LOG = Logger.getLogger(SolrIndexHandler.class);

    private final String FIELD_TYPE_NAME = "string_keyword";

    private boolean enableOptimize = true;

    private ObjectPipe<SolrInputDocument> indexingQueue = new ObjectPipe<SolrInputDocument>(100000);

    @Override
    public void createIndex(ObjectStore os, Map<String, List<FieldDescriptor>> classKeys)
            throws IOException {
        long time = System.currentTimeMillis();
        LOG.debug("Creating keyword search index...");

        SolrClient solrClient = SolrClientManager.getClientInstance(os);

        //delete previous documents in solr

        LOG.debug("Delete previous index begins");
        long deleteStartTime = System.currentTimeMillis();

        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();

        Map<String, Object> fieldTypeAttributes = new HashMap();
        fieldTypeAttributes.put("name", FIELD_TYPE_NAME);
        fieldTypeAttributes.put("class", "solr.TextField");
        fieldTypeAttributes.put("positionIncrementGap", 100);
        fieldTypeAttributes.put("multiValued", true);

        AnalyzerDefinition indexAnalyzerDefinition = new AnalyzerDefinition();
        Map<String, Object> indexTokenizerAttributes = new HashMap<String, Object>();
        indexTokenizerAttributes.put("class", "solr.WhitespaceTokenizerFactory");
        indexAnalyzerDefinition.setTokenizer(indexTokenizerAttributes);
        Map<String, Object> indexLowerCaseFilterAttributes = new HashMap<String, Object>();
        indexLowerCaseFilterAttributes.put("class", "solr.LowerCaseFilterFactory");
        List<Map<String, Object>> indexFilterAttributes = new ArrayList<Map<String, Object>>();
        indexFilterAttributes.add(indexLowerCaseFilterAttributes);
        indexAnalyzerDefinition.setFilters(indexFilterAttributes);

        AnalyzerDefinition queryAnalyzerDefinition = new AnalyzerDefinition();
        Map<String, Object> queryTokenizerAttributes = new HashMap<String, Object>();
        queryTokenizerAttributes.put("class", "solr.WhitespaceTokenizerFactory");
        queryAnalyzerDefinition.setTokenizer(queryTokenizerAttributes);
        Map<String, Object> queryLowerCaseFilterAttributes = new HashMap<String, Object>();
        queryLowerCaseFilterAttributes.put("class", "solr.LowerCaseFilterFactory");
        List<Map<String, Object>> queryFilterAttributes = new ArrayList<Map<String, Object>>();
        queryFilterAttributes.add(queryLowerCaseFilterAttributes);
        queryAnalyzerDefinition.setFilters(queryFilterAttributes);

        fieldTypeDefinition.setAttributes(fieldTypeAttributes);
        fieldTypeDefinition.setIndexAnalyzer(indexAnalyzerDefinition);
        fieldTypeDefinition.setQueryAnalyzer(queryAnalyzerDefinition);

        try{
            SchemaRequest.AddFieldType schemaRequest = new SchemaRequest.AddFieldType(fieldTypeDefinition);
            SchemaResponse.UpdateResponse response =  schemaRequest.process(solrClient);

        } catch (SolrServerException e){
            LOG.error("Error while adding fieldtype to the solrclient.", e);

            e.printStackTrace();
        }

//        Map<String, Object> textFieldAttributes = new HashMap();
//        textFieldAttributes.put("name", "_text_");
//        textFieldAttributes.put("type", FIELD_TYPE_NAME);
//        textFieldAttributes.put("stored", false);
//        textFieldAttributes.put("indexed", true);
//        textFieldAttributes.put("multiValued", true);
//
//        try{
//            SchemaRequest.ReplaceField replaceFieldRequest = new SchemaRequest.ReplaceField(textFieldAttributes);
//            SchemaResponse.UpdateResponse replaceFieldResponse =  replaceFieldRequest.process(solrClient);
//
//        } catch (SolrServerException e){
//            LOG.error("Error while modifying _text_ fieldtype", e);
//
//            e.printStackTrace();
//        }

        try {
            solrClient.deleteByQuery("*:*");
            solrClient.commit();

        } catch (SolrServerException e) {
            LOG.error("Deleting old index failed", e);
        }

        LOG.debug("Delete previous index ends and it took " + (System.currentTimeMillis() - deleteStartTime) + "ms");

        KeywordSearchPropertiesManager keywordSearchPropertiesManager
                = KeywordSearchPropertiesManager.getInstance(os);

//        //adding copy field to solr so that all the fields can be searchable
//        //No need to search for Category : gene. Searching for gene is enough
//
//        try{
//
//            List<String> copyFieldAttributes = new ArrayList<String>();
//            copyFieldAttributes.add("_text_");
//
//            SchemaRequest.DeleteCopyField deleteCopyField = new SchemaRequest.DeleteCopyField("*", copyFieldAttributes);
//            SchemaResponse.UpdateResponse deleteCopyFieldRes =  deleteCopyField.process(solrClient);
//
//            SchemaRequest.AddCopyField schemaCopyRequest = new SchemaRequest.AddCopyField("*", copyFieldAttributes);
//            SchemaResponse.UpdateResponse copyFieldResponse =  schemaCopyRequest.process(solrClient);
//
//        } catch (SolrServerException e){
//            LOG.error("Error while adding copy field to the solrclient.", e);
//
//            e.printStackTrace();
//        }

        addFieldNameToSchema("classname", FIELD_TYPE_NAME, false, true, true, solrClient);
        addFieldNameToSchema("Category", "string", false, true, true, solrClient);

        for (KeywordSearchFacetData facetData: keywordSearchPropertiesManager.getFacets()){
            for (String field : facetData.getFields()){
                addFieldNameToSchema(field, FIELD_TYPE_NAME, false, true, true, solrClient);
                addFieldNameToSchema("facet_" + field, "string", false, true, true, solrClient);
                addCopyFieldToSchema(field, "facet_" + field, solrClient);
            }
        }

        LOG.info("Starting fetcher thread...");
        SolrObjectHandler fetchThread =
                new SolrObjectHandler(os,
                        keywordSearchPropertiesManager.getClassKeys(),
                        indexingQueue,
                        keywordSearchPropertiesManager.getIgnoredClasses(),
                        keywordSearchPropertiesManager.getIgnoredFields(),
                        keywordSearchPropertiesManager.getSpecialReferences(),
                        keywordSearchPropertiesManager.getClassBoost(),
                        keywordSearchPropertiesManager.getFacets(),
                        keywordSearchPropertiesManager.getAttributePrefixes(),
                        solrClient);
        fetchThread.start();

        int indexed = 0;

        List<SolrInputDocument> solrInputDocuments = new ArrayList<SolrInputDocument>();

        // loop and index while we still have fetchers running
        LOG.debug("Starting to index...");

        long indexStartTime = System.currentTimeMillis();

        int tempDocs = 0;
        long tempTime = System.currentTimeMillis();

        while (indexingQueue.hasNext()) {
            SolrInputDocument doc = indexingQueue.next();

            solrInputDocuments.add(doc);

            indexed++;

            if (solrInputDocuments.size() == keywordSearchPropertiesManager.getIndexBatchSize()){

                tempTime = System.currentTimeMillis();

                addSolrDocuments(solrClient, solrInputDocuments);

                tempDocs = indexed - tempDocs;

                LOG.info("docs indexed=" + indexed + "; thread state="
                        + fetchThread.getState() + "; docs/ms=" + tempDocs * 1.0F
                        / (System.currentTimeMillis() - tempTime) + "; memory="
                        + Runtime.getRuntime().freeMemory() / 1024 + "k/"
                        + Runtime.getRuntime().maxMemory() / 1024 + "k" + "; time="
                        + (System.currentTimeMillis() - time) + "ms");

                solrInputDocuments.clear();
            }

        }

        addSolrDocuments(solrClient, solrInputDocuments);

        commit(solrClient);

        if (enableOptimize) {
            optimize(solrClient);
        }

        LOG.debug("Solr indexing ends and it took " + (System.currentTimeMillis() - indexStartTime) + "ms");

        if (fetchThread.getException() != null) {
            try {

            } catch (Exception e) {
                LOG.error("Error closing writer while handling exception.", e);
            }
            throw new RuntimeException("Indexing failed.", fetchThread.getException());
        }

        time = System.currentTimeMillis() - time;
        int seconds = (int) Math.floor(time / 1000);
        LOG.info("Indexing of " + indexed + " documents finished in "
                + String.format("%02d:%02d.%03d", (int) Math.floor(seconds / 60), seconds % 60,
                time % 1000) + " minutes");
    }


    private void addSolrDocuments(SolrClient solrClient, List<SolrInputDocument> solrDocumentList) throws IOException {
        //Accessing SchemaAPI from solr and create the schema dynamically

        if (solrDocumentList.size() != 0) {

            LOG.debug("Beginning to commit Solr Documents into Solr");

            try {
                UpdateResponse response = solrClient.add(solrDocumentList, 30000);

            } catch (SolrServerException e) {

                LOG.error("Error while commiting the SolrInputdocuments to the Solrclient. " +
                        "Make sure the Solr instance is up", e);

                e.printStackTrace();
            }
        }

    }

    public void addFieldNameToSchema(String fieldName, String fieldType, boolean stored,
                                     boolean indexed, boolean omitNorms, SolrClient solrClient) throws IOException{

        Map<String, Object> fieldAttributes = new HashMap();
        fieldAttributes.put("name", fieldName);
        fieldAttributes.put("type", fieldType);
        fieldAttributes.put("stored", stored);
        fieldAttributes.put("indexed", indexed);
        fieldAttributes.put("omitNorms", omitNorms);
        fieldAttributes.put("multiValued", true);
        fieldAttributes.put("required", false);

        try {
            SchemaRequest.AddField schemaRequest = new SchemaRequest.AddField(fieldAttributes);
            SchemaResponse.UpdateResponse response = schemaRequest.process(solrClient);

        } catch (SolrServerException e) {
            LOG.error("Error while adding fields to the solrclient.", e);

            e.printStackTrace();
        }

    }

    public void addCopyFieldToSchema(String source, String dest, SolrClient solrClient) throws IOException{

        try {

            List<String> copyFieldAttributes = new ArrayList<String>();
            copyFieldAttributes.add(dest);

            SchemaRequest.DeleteCopyField deleteCopyField = new SchemaRequest.DeleteCopyField(source, copyFieldAttributes);
            SchemaResponse.UpdateResponse deleteCopyFieldRes =  deleteCopyField.process(solrClient);

            SchemaRequest.AddCopyField schemaCopyRequest = new SchemaRequest.AddCopyField(source, copyFieldAttributes);
            SchemaResponse.UpdateResponse copyFieldResponse =  schemaCopyRequest.process(solrClient);

        } catch (SolrServerException e) {
            LOG.error("Error while adding copyfields to the solrclient.", e);
            e.printStackTrace();
        }
    }

    private void commit(SolrClient solrClient) throws IOException{
        try {
            solrClient.commit();

        } catch (SolrServerException e) {
            LOG.error("Error while commiting.", e);
            e.printStackTrace();
        }
    }

    private void optimize(SolrClient solrClient) throws IOException{

        long startTime = System.currentTimeMillis();

        try {
            solrClient.optimize();

            LOG.info("Optimizing Solr Index finished in " + (System.currentTimeMillis() -  startTime) + "ms");

        } catch (SolrServerException e) {
            LOG.error("Error while optimizing", e);
            e.printStackTrace();
        }

    }

}
