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
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.SolrInputDocument;

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

            String urlString = "http:localhost:8983/solr/autocomplete";
            HttpSolrClient solrClient = new HttpSolrClient.Builder(urlString).build();

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

            for (int i = 0; i < objClass.getSizeValues(); i++) {
                SolrInputDocument doc = new SolrInputDocument();
                for (int j = 0; j < objClass.getSizeFields(); j++) {
                    doc.addField(objClass.getFieldName(j),
                            objClass.getValuesForField(objClass.getFieldName(j), i));
                }

                UpdateResponse response = solrClient.add(doc);

                solrClient.commit();
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

}
