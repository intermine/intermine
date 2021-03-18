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
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.AnalyzerDefinition;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.SolrInputDocument;
import org.intermine.api.searchengine.IndexHandler;
import org.intermine.api.searchengine.KeywordSearchFacetData;
import org.intermine.api.searchengine.KeywordSearchPropertiesManager;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.ObjectPipe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Solr Implementation of IndexHandler
 *
 * @author arunans23
 */
public final class SolrIndexHandler implements IndexHandler
{
    private static final Logger LOG = Logger.getLogger(SolrIndexHandler.class);

    //this field type is analyzed
    private static final String ANALYZED_FIELD_TYPE_NAME = "analyzed_string";

    //this field type is not analyzed
    private static final String RAW_FIELD_TYPE_NAME = "raw_string";

    private ObjectPipe<SolrInputDocument> indexingQueue = new ObjectPipe<SolrInputDocument>(100000);

    //ArrayLists to store the existing schema data in Solr
    //Based on the existence, schema will be created or updated
    private List<String> existingFields;
    private List<String> existingFieldTypes;
    private List<String> existingCopyFields;

    private List<String> indexedFields;

    @Override
    public void createIndex(ObjectStore os, Map<String, List<FieldDescriptor>> classKeys)
            throws IOException, SolrServerException {
        long time = System.currentTimeMillis();
        LOG.debug("Creating keyword search index...");

        SolrClient solrClient = SolrClientManager.getClientInstance(os);

        //delete previous documents in solr

        LOG.debug("Delete previous index begins");
        long deleteStartTime = System.currentTimeMillis();

        try {
            solrClient.deleteByQuery("*:*");
            solrClient.commit();

        } catch (SolrServerException e) {
            LOG.error("Deleting old index failed", e);
        }

        LOG.debug("Delete previous index ends and it took "
                + (System.currentTimeMillis() - deleteStartTime) + "ms");

        try {
            this.existingFields = getAllExistingFieldsFromSolr(solrClient);
            this.existingFieldTypes = getAllExisitingFieldTypesFromSolr(solrClient);
            this.existingCopyFields = getAllExisitingCopyFieldsFromSolr(solrClient);

        } catch (SolrServerException e) {
            LOG.error("Retrieving existing schema Definitions in Solr failed");
        }

        this.indexedFields = new ArrayList<String>();

        createFieldTypeDefinitions(solrClient);


        KeywordSearchPropertiesManager keywordSearchPropertiesManager
                = KeywordSearchPropertiesManager.getInstance(os);

        addFieldNameToSchema("classname", ANALYZED_FIELD_TYPE_NAME, false, true, solrClient);
        addFieldNameToSchema("Category", "string", false, true, solrClient);

        for (KeywordSearchFacetData facetData: keywordSearchPropertiesManager.getFacets()) {
            for (String field : facetData.getFields()) {
                addFieldNameToSchema(field, ANALYZED_FIELD_TYPE_NAME, false, true, solrClient);
                addFieldNameToSchema("facet_" + field, "string", false, true, solrClient);
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
                        solrClient,
                        indexedFields,
                        existingFields);
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

            if (solrInputDocuments.size() == keywordSearchPropertiesManager.getIndexBatchSize()) {

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

        if (keywordSearchPropertiesManager.getEnableOptimize()) {
            optimize(solrClient);
        }

        LOG.debug("Solr indexing ends and it took "
                + (System.currentTimeMillis() - indexStartTime) + "ms");

        if (fetchThread.getException() != null) {

            throw new RuntimeException("Indexing failed.", fetchThread.getException());
        }

        time = System.currentTimeMillis() - time;
        int seconds = (int) Math.floor(time / 1000);
        LOG.info("Indexing of " + indexed + " documents finished in "
                + String.format("%02d:%02d.%03d", (int) Math.floor(seconds / 60), seconds % 60,
                time % 1000) + " minutes");
    }


    private void addSolrDocuments(SolrClient solrClient, List<SolrInputDocument> solrDocumentList)
            throws IOException, SolrServerException {

        if (solrDocumentList.size() != 0) {

            LOG.debug("Beginning to commit Solr Documents into Solr");

            try {
                UpdateResponse response = solrClient.add(solrDocumentList, 30000);

            } catch (SolrServerException e) {

                LOG.error("Error while commiting the SolrInputdocuments to the Solrclient. "
                        + "Make sure the Solr instance is up", e);

                throw e;
            }
        }

    }

    private void addFieldNameToSchema(String fieldName, String fieldType, boolean stored,
                                     boolean indexed, SolrClient solrClient) throws IOException {

        if (!this.indexedFields.contains(fieldName)) {
            if (this.existingFields != null) {
                if (!this.existingFields.contains(fieldName)) {
                    Map<String, Object> fieldAttributes = new HashMap();
                    fieldAttributes.put("name", fieldName);
                    fieldAttributes.put("type", fieldType);
                    fieldAttributes.put("stored", stored);
                    fieldAttributes.put("indexed", indexed);
                    fieldAttributes.put("multiValued", true);
                    fieldAttributes.put("required", false);

                    try {
                        SchemaRequest.AddField schemaRequest =
                                new SchemaRequest.AddField(fieldAttributes);
                        SchemaResponse.UpdateResponse response = schemaRequest.process(solrClient);

                        this.indexedFields.add(fieldName);

                    } catch (SolrServerException e) {
                        LOG.error("Error while adding fields to the solrclient.", e);

                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void addCopyFieldToSchema(String source, String dest, SolrClient solrClient)
            throws IOException {

        try {
            if (this.existingCopyFields != null) {
                if (!this.existingCopyFields.contains(dest)) {

                    List<String> copyFieldAttributes = new ArrayList<String>();
                    copyFieldAttributes.add(dest);

                    SchemaRequest.AddCopyField schemaCopyRequest
                            = new SchemaRequest.AddCopyField(source, copyFieldAttributes);

                    SchemaResponse.UpdateResponse copyFieldResponse
                            =  schemaCopyRequest.process(solrClient);
                }
            }

        } catch (SolrServerException e) {
            LOG.error("Error while adding copyfields to the solrclient.", e);
            e.printStackTrace();
        }
    }

    private void commit(SolrClient solrClient) throws IOException {
        try {
            solrClient.commit();

        } catch (SolrServerException e) {
            LOG.error("Error while commiting.", e);
            e.printStackTrace();
        }
    }

    private void optimize(SolrClient solrClient) throws IOException {

        long startTime = System.currentTimeMillis();

        try {
            solrClient.optimize();

            LOG.info("Optimizing Solr Index finished in "
                    + (System.currentTimeMillis() -  startTime) + "ms");

        } catch (SolrServerException e) {
            LOG.error("Error while optimizing", e);
            e.printStackTrace();
        }

    }

    private void createFieldTypeDefinitions(SolrClient solrClient) throws IOException {

        if (this.existingFieldTypes != null) {
            if (!this.existingFieldTypes.contains(ANALYZED_FIELD_TYPE_NAME)) {
                FieldTypeDefinition analyzedFieldTypeDefinition = new FieldTypeDefinition();

                Map<String, Object> analyzedFieldTypeAttributes = new HashMap();
                analyzedFieldTypeAttributes.put("name", ANALYZED_FIELD_TYPE_NAME);
                analyzedFieldTypeAttributes.put("class", "solr.TextField");
                analyzedFieldTypeAttributes.put("positionIncrementGap", 100);
                analyzedFieldTypeAttributes.put("multiValued", true);

                AnalyzerDefinition indexAnalyzerDefinition1 = new AnalyzerDefinition();
                Map<String, Object> indexTokenizerAttributes1 = new HashMap<String, Object>();
                indexTokenizerAttributes1.put("class", "solr.WhitespaceTokenizerFactory");
                indexAnalyzerDefinition1.setTokenizer(indexTokenizerAttributes1);
                Map<String, Object> indexLowerCaseFilterAttributes1 = new HashMap<String, Object>();
                indexLowerCaseFilterAttributes1.put("class", "solr.LowerCaseFilterFactory");
                List<Map<String, Object>> indexFilterAttributes1 =
                        new ArrayList<Map<String, Object>>();
                indexFilterAttributes1.add(indexLowerCaseFilterAttributes1);
                indexAnalyzerDefinition1.setFilters(indexFilterAttributes1);

                AnalyzerDefinition queryAnalyzerDefinition1 = new AnalyzerDefinition();
                Map<String, Object> queryTokenizerAttributes1 = new HashMap<String, Object>();
                queryTokenizerAttributes1.put("class", "solr.WhitespaceTokenizerFactory");
                queryAnalyzerDefinition1.setTokenizer(queryTokenizerAttributes1);
                Map<String, Object> queryLowerCaseFilterAttributes1 = new HashMap<String, Object>();
                queryLowerCaseFilterAttributes1.put("class", "solr.LowerCaseFilterFactory");
                List<Map<String, Object>> queryFilterAttributes1 =
                        new ArrayList<Map<String, Object>>();
                queryFilterAttributes1.add(queryLowerCaseFilterAttributes1);
                queryAnalyzerDefinition1.setFilters(queryFilterAttributes1);

                analyzedFieldTypeDefinition.setAttributes(analyzedFieldTypeAttributes);
                analyzedFieldTypeDefinition.setIndexAnalyzer(indexAnalyzerDefinition1);
                analyzedFieldTypeDefinition.setQueryAnalyzer(queryAnalyzerDefinition1);

                try {
                    SchemaRequest.AddFieldType schemaRequest
                            = new SchemaRequest.AddFieldType(analyzedFieldTypeDefinition);

                    SchemaResponse.UpdateResponse response =  schemaRequest.process(solrClient);

                } catch (SolrServerException e) {
                    LOG.error("Error while adding fieldtype '"
                            + ANALYZED_FIELD_TYPE_NAME + "' to the solrclient.", e);

                    e.printStackTrace();
                }
            }
        }

        if (this.existingFieldTypes != null) {
            if (!this.existingFieldTypes.contains(RAW_FIELD_TYPE_NAME)) {
                FieldTypeDefinition rawFieldTypeDefinition = new FieldTypeDefinition();

                Map<String, Object> rawFieldTypeAttributes = new HashMap();
                rawFieldTypeAttributes.put("name", RAW_FIELD_TYPE_NAME);
                rawFieldTypeAttributes.put("class", "solr.TextField");
                rawFieldTypeAttributes.put("positionIncrementGap", 100);
                rawFieldTypeAttributes.put("multiValued", true);

                AnalyzerDefinition indexAnalyzerDefinition2 = new AnalyzerDefinition();
                Map<String, Object> indexTokenizerAttributes2 = new HashMap<String, Object>();
                indexTokenizerAttributes2.put("class", "solr.KeywordTokenizerFactory");
                indexAnalyzerDefinition2.setTokenizer(indexTokenizerAttributes2);
                Map<String, Object> indexLowerCaseFilterAttributes2 = new HashMap<String, Object>();
                indexLowerCaseFilterAttributes2.put("class", "solr.LowerCaseFilterFactory");
                List<Map<String, Object>> indexFilterAttributes2 =
                        new ArrayList<Map<String, Object>>();
                indexFilterAttributes2.add(indexLowerCaseFilterAttributes2);
                indexAnalyzerDefinition2.setFilters(indexFilterAttributes2);

                AnalyzerDefinition queryAnalyzerDefinition2 = new AnalyzerDefinition();
                Map<String, Object> queryTokenizerAttributes2 = new HashMap<String, Object>();
                queryTokenizerAttributes2.put("class", "solr.KeywordTokenizerFactory");
                queryAnalyzerDefinition2.setTokenizer(queryTokenizerAttributes2);
                Map<String, Object> queryLowerCaseFilterAttributes2 = new HashMap<String, Object>();
                queryLowerCaseFilterAttributes2.put("class", "solr.LowerCaseFilterFactory");
                List<Map<String, Object>> queryFilterAttributes2
                        = new ArrayList<Map<String, Object>>();
                queryFilterAttributes2.add(queryLowerCaseFilterAttributes2);
                queryAnalyzerDefinition2.setFilters(queryFilterAttributes2);

                rawFieldTypeDefinition.setAttributes(rawFieldTypeAttributes);
                rawFieldTypeDefinition.setIndexAnalyzer(indexAnalyzerDefinition2);
                rawFieldTypeDefinition.setQueryAnalyzer(queryAnalyzerDefinition2);

                try {
                    SchemaRequest.AddFieldType schemaRequest
                            = new SchemaRequest.AddFieldType(rawFieldTypeDefinition);

                    SchemaResponse.UpdateResponse response =  schemaRequest.process(solrClient);

                } catch (SolrServerException e) {
                    LOG.error("Error while adding fieldtype '"
                            +  RAW_FIELD_TYPE_NAME + "' to the solrclient.", e);

                    e.printStackTrace();
                }
            }
        }
    }


    private List<String> getAllExistingFieldsFromSolr(SolrClient solrClient)
            throws IOException, SolrServerException {
        List<String> allFields = new ArrayList<String>();
        SchemaRequest.Fields listFields = new SchemaRequest.Fields();
        SchemaResponse.FieldsResponse fieldsResponse = listFields.process(solrClient);
        List<Map<String, Object>> solrFields = fieldsResponse.getFields();
        for (Map<String, Object> field : solrFields) {
            allFields.add((String) field.get("name"));
        }
        return allFields;
    }

    private List<String> getAllExisitingFieldTypesFromSolr(SolrClient solrClient)
            throws IOException, SolrServerException {
        List<String> allFieldTypes = new ArrayList<String>();
        SchemaRequest.FieldTypes listFieldTypes = new SchemaRequest.FieldTypes();
        SchemaResponse.FieldTypesResponse fieldTypesResponse = listFieldTypes.process(solrClient);
        List<FieldTypeRepresentation> solrFieldTypes = fieldTypesResponse.getFieldTypes();
        for (FieldTypeRepresentation fieldType : solrFieldTypes) {
            allFieldTypes.add((String) fieldType.getAttributes().get("name"));
        }
        return allFieldTypes;
    }

    private List<String> getAllExisitingCopyFieldsFromSolr(SolrClient solrClient)
            throws IOException, SolrServerException {
        List<String> allCopyFields = new ArrayList<String>();
        SchemaRequest.CopyFields listCopyFields = new SchemaRequest.CopyFields();
        SchemaResponse.CopyFieldsResponse copyFieldsResponse = listCopyFields.process(solrClient);
        List<Map<String, Object>> solrCopyFields = copyFieldsResponse.getCopyFields();
        for (Map<String, Object> copyField : solrCopyFields) {
            allCopyFields.add((String) copyField.get("dest"));
        }
        return allCopyFields;
    }

}
