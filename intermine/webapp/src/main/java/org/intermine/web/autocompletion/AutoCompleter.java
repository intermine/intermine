package org.intermine.web.autocompletion;

/*
 * Copyright (C) 2002-2020 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

/**
 * Autocompleter class for initializing and using the autocompletion
 *
 * @author Dominik Grimm
 * @author Michael Menden
 * @author arunans23
 */
public class AutoCompleter
{
    private static final Logger LOG = Logger.getLogger(AutoCompleter.class);

    private PropertiesManager propertiesManager;

    private HashMap<String, String> classFieldMap = new HashMap<String, String>();
    private HashMap<String, List<String>> fieldIndexMap = new HashMap<String, List<String>>();

    ObjectStore os;

    private static final String CLASSNAME_FIELD = "className";

    /**
     * Autocompleter build index constructor.
     *
     * @param os Objectstore
     */
    public AutoCompleter(ObjectStore os) {

        this.os = os;

        if (propertiesManager == null) {
            this.propertiesManager = PropertiesManager.getInstance();
            this.classFieldMap = propertiesManager.getClassFieldMap();
            createFieldIndexMap();
        }

    }


    /**
     * Returns n search results
     * @param query is the string used for search
     * @param field is the field in which you like to search (e.g. name)
     * @param className is the class in which you like to search (e.g. SOTerm)
     * @param n number of the first n search results
     * @return string array with search results and an error flag at position 0
     */
    public String[] getFastList(String query, String field, String className,  int n) {

        String status = "true";
        String[] stringResults = null;

        SolrClient solrClient
                = SolrClientHandler.getClientInstance(this.propertiesManager.getSolrUrl());

        QueryResponse resp = null;

        if (!"".equals(query) && !query.trim().startsWith("*")) {

            if (query.endsWith(" ")) {
                query = query.substring(0, query.length() - 1);
            }

            String[] tmp;
            if (query.contains(" ")) {
                tmp = query.replaceAll(" +", " ").trim().split(" ");
                query = new String();

                for (int i = 0; i < tmp.length; i++) {
                    query += tmp[i];
                    if (i < tmp.length - 1) {
                        query += "* AND " + field + ":";
                    }
                }
            }

            try {

                SolrQuery newQuery = new SolrQuery();
                newQuery.setQuery(field + ":" + query + "*"); //adding a wildcard in the end
                newQuery.setRequestHandler("select");
                newQuery.setRows(n); // FIXME: hardcoded maximum
                newQuery.setFilterQueries(CLASSNAME_FIELD + ":" + className);

                resp = solrClient.query(newQuery);

                SolrDocumentList results = resp.getResults();

                stringResults = new String[results.size() + 1];

                for (int i = 1; i < results.size() + 1; i++) {

                    try {
                        SolrDocument document = results.get(i - 1);

                        stringResults[i] = ((ArrayList<String>) document
                                .getFieldValue(field)).get(0);

                    } catch (Exception e) {
                        status = "No results! Please try again.";
                    }
                }

                stringResults[0] = status;


                return stringResults;

            } catch (SolrServerException e) {
                e.printStackTrace();

            } catch (IOException e) {
                e.printStackTrace();
                status = "Please type in more characters to get results.";
                stringResults = new String[1];
                stringResults[0] = status;
            }

            return stringResults;
        }

        return null;
    }

    /**
     * Build the index from the database blob
     * @param os Objectstore
     * @throws IOException IOException
     * @throws ObjectStoreException ObjectStoreException
     * @throws ClassNotFoundException ClassNotFoundException
     * @throws SolrServerException solr exception
     */
    public void buildIndex(ObjectStore os)
        throws IOException, ObjectStoreException, ClassNotFoundException, SolrServerException {

        List<SolrInputDocument> solrDocumentList = new ArrayList<SolrInputDocument>();
        List<String> fieldList = new ArrayList<String>();

        fieldList.add(CLASSNAME_FIELD);

        for (Map.Entry<String, String> entry: classFieldMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            String className = key;
            ClassDescriptor cld = os.getModel().getClassDescriptorByName(className);
            if (cld == null) {
                throw new RuntimeException("a class mentioned in ObjectStore summary properties "
                                           + "file (" + className + ") is not in the model");
            }
            List<String> fieldNames = Arrays.asList(value.split(" "));
            for (Iterator<String> i = fieldNames.iterator(); i.hasNext();) {

                String fieldName = i.next();
                String classAndField = cld.getUnqualifiedName() + "." + fieldName;
                System.out .println("Indexing " + classAndField);

                if (!fieldList.contains(fieldName)) {
                    fieldList.add(fieldName);
                }

                Query q = new Query();
                q.setDistinct(true);
                QueryClass qc = new QueryClass(Class.forName(cld.getName()));
                q.addToSelect(new QueryField(qc, fieldName));
                q.addFrom(qc);
                Results results = os.execute(q);

                for (Object resRow: results) {
                    @SuppressWarnings("rawtypes")
                    Object fieldValue = ((ResultsRow) resRow).get(0);
                    if (fieldValue != null) {
                        SolrInputDocument solrInputDocument = new SolrInputDocument();
                        solrInputDocument.addField(fieldName, fieldValue.toString());
                        solrInputDocument.addField(CLASSNAME_FIELD, cld.getUnqualifiedName());
                        solrDocumentList.add(solrInputDocument);
                    }
                }
            }
        }

        SolrClient solrClient = SolrClientHandler
                .getClientInstance(this.propertiesManager.getSolrUrl());

        try {
            solrClient.deleteByQuery("*:*");
            solrClient.commit();
        } catch (SolrServerException e) {
            LOG.error("Deleting old index failed", e);
            throw e;
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }

        List<String> existingFields = null;

        try {
            existingFields = getAllExistingFieldsFromSolr(solrClient);

        } catch (SolrServerException e) {
            LOG.error("Retrieving existing Fieldnames in Solr failed");
        }


        for (String fieldName: fieldList) {
            if (existingFields != null) {
                if (!existingFields.contains(fieldName)) {
                    Map<String, Object> fieldAttributes = new HashMap();
                    fieldAttributes.put("name", fieldName);
                    fieldAttributes.put("type", "text_general");
                    fieldAttributes.put("stored", true);
                    fieldAttributes.put("indexed", true);
                    fieldAttributes.put("multiValued", true);
                    fieldAttributes.put("required", false);

                    try {
                        SchemaRequest.AddField schemaRequest
                                = new SchemaRequest.AddField(fieldAttributes);
                        SchemaResponse.UpdateResponse response =  schemaRequest.process(solrClient);

                    } catch (SolrServerException e) {
                        LOG.error("Error while adding autocomplete fields to the solrclient.", e);
                        e.printStackTrace();
                    }
                }
            }
        }

        try {
            if (!solrDocumentList.isEmpty()) {
                UpdateResponse response = solrClient.add(solrDocumentList);
                solrClient.commit();
            }
        } catch (SolrServerException e) {

            LOG.error("Error while commiting the AutoComplete "
                    + "SolrInputdocuments to the Solrclient. "
                    + "Make sure the Solr instance is up", e);

            e.printStackTrace();
        }
    }


    /**
     * checks if an autocompletion exists
     * @param type The name of the class to search.
     * @param field The name of the field to search for.
     * @return whether an autocompletion exists
     */
    public boolean hasAutocompleter(String type, String field) {

        if (fieldIndexMap.containsKey(type) && fieldIndexMap.get(type).contains(field)) {
            return true;
        }

        return false;
    }

    private void createFieldIndexMap() {
        for (Map.Entry<String, String> entry: classFieldMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            String className = key;
            ClassDescriptor cld = os.getModel().getClassDescriptorByName(className);
            if (cld == null) {
                throw new RuntimeException("a class mentioned in ObjectStore summary properties "
                        + "file (" + className + ") is not in the model");
            }
            List<String> fieldNames = Arrays.asList(value.split(" "));

            fieldIndexMap.put(cld.getUnqualifiedName(), fieldNames);
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

}
