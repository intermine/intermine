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
import org.intermine.api.searchengine.SolrClientFactory;
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

        SolrClient solrClient = SolrClientFactory.getClientInstance(os);

        //delete previous documents in solr
        try {
            solrClient.deleteByQuery("*:*");
            solrClient.commit();
        } catch (SolrServerException e) {
            LOG.error("Deleting old index failed", e);
        }

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

            if (indexed % 10000 == 1) {
                LOG.info("docs indexed=" + indexed + "; thread state="
                        + fetchThread.getState() + "; docs/ms=" + indexed * 1.0F
                        / (System.currentTimeMillis() - time) + "; memory="
                        + Runtime.getRuntime().freeMemory() / 1024 + "k/"
                        + Runtime.getRuntime().maxMemory() / 1024 + "k" + "; time="
                        + (System.currentTimeMillis() - time) + "ms");
            }
        }

        Set<String> fieldNames = fetchThread.getFieldNames();
        fieldNames.add("Category");

        for(String fieldName: fieldNames){
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

                List<String> copyFieldAttributes = new ArrayList<String>();
                copyFieldAttributes.add("text");

                SchemaRequest.AddCopyField schemaCopyRequest = new SchemaRequest.AddCopyField("*", copyFieldAttributes);
                SchemaResponse.UpdateResponse copyFieldResponse =  schemaRequest.process(solrClient);

            } catch (SolrServerException e){
                LOG.error("Error while adding fields to the solrclient.", e);

                e.printStackTrace();
            }

        }

        try {
            UpdateResponse response = solrClient.add(solrInputDocuments);

            solrClient.commit();
        } catch (SolrServerException e) {

            LOG.error("Error while commiting the solrinputdocuments to the solrclient.", e);

            e.printStackTrace();
        }

        if (fetchThread.getException() != null) {
            try {

            } catch (Exception e) {
                LOG.error("Error closing writer while handling exception.", e);
            }
            throw new RuntimeException("Indexing failed.", fetchThread.getException());
        }
//        index.getFieldNames().addAll(fetchThread.getFieldNames());
//        LOG.debug("Indexing done, optimizing index files...");
        try {

        } catch (Exception e) {
            LOG.error("IOException while optimizing and closing IndexWriter", e);
        }

        time = System.currentTimeMillis() - time;
        int seconds = (int) Math.floor(time / 1000);
        LOG.info("Indexing of " + indexed + " documents finished in "
                + String.format("%02d:%02d.%03d", (int) Math.floor(seconds / 60), seconds % 60,
                time % 1000) + " minutes");
    }




}
