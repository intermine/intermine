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
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.ResultsRow;

import java.util.List;
import java.util.Map;

/**
 * Solr Implementation of IndexHandler
 *
 * @author arunans23
 */
public final class SolrIndexHandler
{
    private static final Logger LOG = Logger.getLogger(SolrIndexHandler.class);

    private SolrIndexHandler() {

    }

    /**
     *
     * @param os Objectstore that is passed CreateSearchIndexTask
     * @param classKeys classKeys
     */
    public static void createIndex(ObjectStore os,
                                   Map<String, List<FieldDescriptor>> classKeys) {

        try {
            LOG.debug("Creating Solr Index");

            ObjectStore objectStore = os;

            String solrUrlString = "http://localhost:8983/solr/intermine";
            SolrClient solrClient = new HttpSolrClient.Builder(solrUrlString).build();

            String fieldName = "primaryIdentifier";
            classKeys.remove("DataSet");
            classKeys.remove("DataSource");
            classKeys.remove("GOTerm");
            classKeys.remove("SOTerm");
            classKeys.remove("OntologyTerm");
            classKeys.remove("Organism");
            classKeys.remove("Author");
            classKeys.remove("Publication");

            for (String className : classKeys.keySet()) {
                System.out.println("Indexing: " + className);
                LOG.debug("Indexing: " + className);

                ClassDescriptor classDescriptor = objectStore.getModel().getClassDescriptorByName(className);

                if (classDescriptor == null) {
                    throw new RuntimeException("a class mentioned in ObjectStore "
                            + "summary properties "
                            + "file (" + className + ") is not in the model");
                }

                String classAndField = classDescriptor.getUnqualifiedName();

                Query query = new Query();
                query.setDistinct(true);
                QueryClass queryClass = new QueryClass(Class.forName(classDescriptor.getName()));
                query.addToSelect(new QueryField(queryClass, fieldName));
                query.addToSelect(new QueryField(queryClass, "id"));
                query.addFrom(queryClass);
                Results results = objectStore.execute(query);

                for (Object resRow: results) {

                    @SuppressWarnings("rawtypes")
                    SolrInputDocument document = new SolrInputDocument();
                    Object fieldValue = ((ResultsRow) resRow).get(0);
                    Object fieldId = ((ResultsRow) resRow).get(1);
                    if (fieldValue != null) {
                        document.addField("value", fieldValue.toString());
                        document.addField("type", classAndField);
                        document.addField("objectId", fieldId.toString());
                    }
                    else {
                        System.out.println("ERROR?" + " " + fieldValue + " " + fieldId + " " + classAndField);
                        LOG.debug("ERROR" + " " + fieldValue + " " + fieldId + " " + classAndField);
                    }

                    UpdateResponse response = solrClient.add(document);
                }

                solrClient.commit();

            }

        } catch (Exception e) {
            e.printStackTrace();
        }



    }
}
