package org.intermine.api.searchengine.solr;

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
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.SolrInputDocument;
import org.intermine.api.searchengine.IndexHandler;
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
    private static final Logger LOG = Logger.getLogger(SolrIndexHandler.class);

    private static final String LUCENE_INDEX_DIR = "keyword_search_index";

    private static ObjectPipe<SolrInputDocument> indexingQueue = new ObjectPipe<SolrInputDocument>(100000);

    @Override
    public void createIndex(ObjectStore os, Map<String, List<FieldDescriptor>> classKeys)
            throws IOException {
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

        LOG.debug("Delete previous index ends and it took " + (System.currentTimeMillis() - deleteStartTime) + "ms");

        KeywordSearchPropertiesManager keywordSearchPropertiesManager
                = KeywordSearchPropertiesManager.getInstance(os);


        LOG.info("Starting fetcher thread...");
        SolrObjectHandler fetchThread =
                new SolrObjectHandler(os, classKeys, indexingQueue,
                        keywordSearchPropertiesManager.getIgnoredClasses(),
                        keywordSearchPropertiesManager.getIgnoredFields(),
                        keywordSearchPropertiesManager.getSpecialReferences(),
                        keywordSearchPropertiesManager.getClassBoost(),
                        keywordSearchPropertiesManager.getFacets(),
                        keywordSearchPropertiesManager.getAttributePrefixes());
        fetchThread.start();

        int indexed = 0;

        List<SolrInputDocument> solrInputDocuments = new ArrayList<SolrInputDocument>();

        // loop and index while we still have fetchers running
        LOG.debug("Starting to index...");
        while (indexingQueue.hasNext()) {
            SolrInputDocument doc = indexingQueue.next();

            solrInputDocuments.add(doc);

            indexed++;

            //This following log is not needed anymore because actual indexing happens below

//            if (indexed % 10000 == 1) {
//                LOG.info("docs indexed=" + indexed + "; thread state="
//                        + fetchThread.getState() + "; docs/ms=" + indexed * 1.0F
//                        / (System.currentTimeMillis() - time) + "; memory="
//                        + Runtime.getRuntime().freeMemory() / 1024 + "k/"
//                        + Runtime.getRuntime().maxMemory() / 1024 + "k" + "; time="
//                        + (System.currentTimeMillis() - time) + "ms");
//            }
        }

        Set<String> fieldNames = fetchThread.getFieldNames();

        //Accessing SchemaAPI from solr and create the schema dynamically

        LOG.debug("Creating Schema in Solr begins...");

        long schemaStartTime = System.currentTimeMillis();

        fieldNames.add("Category");
        fieldNames.add("classname");

        for(String fieldName: fieldNames){
            Map<String, Object> fieldAttributes = new HashMap();
            fieldAttributes.put("name", fieldName);
            fieldAttributes.put("type", "string");
            fieldAttributes.put("stored", false);
            fieldAttributes.put("indexed", true);
            fieldAttributes.put("multiValued", true);
            fieldAttributes.put("required", false);

            try{
                SchemaRequest.AddField schemaRequest = new SchemaRequest.AddField(fieldAttributes);
                SchemaResponse.UpdateResponse response =  schemaRequest.process(solrClient);

            } catch (SolrServerException e){
                LOG.error("Error while adding fields to the solrclient.", e);

                e.printStackTrace();
            }
        }

        //adding copy field to solr so that all the fields can be searchable
        //No need to search for Category : gene. Searching for gene is enough

        try{

            List<String> copyFieldAttributes = new ArrayList<String>();
            copyFieldAttributes.add("_text_");

            SchemaRequest.AddCopyField schemaCopyRequest = new SchemaRequest.AddCopyField("*", copyFieldAttributes);
            SchemaResponse.UpdateResponse copyFieldResponse =  schemaCopyRequest.process(solrClient);

        } catch (SolrServerException e){
            LOG.error("Error while adding copy field to the solrclient.", e);

            e.printStackTrace();
        }

        LOG.debug("Creating Schema in Solr ends and it took " + (System.currentTimeMillis() - schemaStartTime) + "ms");

        long indexStartTime = System.currentTimeMillis();

        LOG.debug("Beginning to commit Solr Documents into Solr");

        try {
            UpdateResponse response = solrClient.add(solrInputDocuments);

            solrClient.commit();
        } catch (SolrServerException e) {

            LOG.error("Error while commiting the SolrInputdocuments to the Solrclient. " +
                    "Make sure the Solr instance is up", e);

            e.printStackTrace();
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




}
