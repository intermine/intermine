package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.log4j.Logger;
import org.intermine.bio.util.OrganismData;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.sql.Database;
import org.intermine.xml.full.Item;

/**
 *
 * @author
 */
public class BarConverter extends BioDBConverter
{
    private static final Logger LOG = Logger.getLogger(BarConverter.class);
    private Map<String, String> pubs = new HashMap<String, String>();
    private Map<String, String> proteins = new HashMap<String, String>();
    private Map<String, Object> experimentNames = new HashMap<String, Object>();
    private Map<String, String> terms = new HashMap<String, String>();
    private Map<String, String> regions = new HashMap<String, String>();
    private String termId = null;
    private static final String INTERACTION_TYPE = "physical";
    private Set<String> taxonIds = null;
    private Map<String, String> genes = new HashMap<String, String>();
    private Map<MultiKey, Item> interactions = new HashMap<MultiKey, Item>();
    private static final String SPOKE_MODEL = "prey";   // don't store if all roles prey
    private static final String DEFAULT_DATASOURCE = "";
    private static final String DATASET_TITLE = "BAR interactions";
    private static final String DATA_SOURCE_NAME = "The Bio-Analytic Resource for Plant Biology";


    Set<String> prots = null;
    protected IdResolver rslv;


    //


    /**
     * Construct a new BarConverter.
     * @param database the database to read from
     * @param model the Model used by the object store we will write to with the ItemWriter
     * @param writer an ItemWriter used to handle Items created
     */
    public BarConverter(Database database, Model model, ItemWriter writer) {
        super(database, model, writer, DATA_SOURCE_NAME, DATASET_TITLE);
    }


    /**
     * {@inheritDoc}
     */
    public void process() throws Exception {
        // a database has been initialised from properties starting with db.bar

        Connection connection = getDatabase().getConnection();

        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        ResultSet res = getData(connection);
        while (res.next()) {
            String protein1 = res.getString("Protein1");
            String protein2 = res.getString("Protein2");
            // Integer  = new Integer(res.getInt("experiment_id"));
            String iTypeMi = res.getString("Interactions_type_mi");
            String iType = res.getString("Interactions_type");
            String iDetectionMi = res.getString("Interactions_detection_mi");
            String iDetection = res.getString("Interactions_detection");
            String publication = res.getString("Bind_id");


            String p1Ref = getProteinRef(protein1);
            String p2Ref = getProteinRef(protein2);

            Item interaction = getInteraction(interactions, p1Ref, p2Ref);

        }
        res.close();

//        LOG.info("found " + deletedSubMap.size() + " deleted submissions to skip in the build.");
//        LOG.info("PROCESS TIME deleted subs: " + (System.currentTimeMillis() - bT) + " ms");
    }


    private String getProteinRef( String protein) throws ObjectStoreException {

        String itemId = proteins.get(protein);
        if (itemId == null) {
            itemId= createItem("Protein").getIdentifier();
            proteins.put(protein, itemId);
        }
        return itemId;
    }



    private Item getInteraction(Map<MultiKey, Item> interactions, String p1RefId,
            String p2RefId) throws ObjectStoreException {
        MultiKey key = new MultiKey(p1RefId, p2RefId);
        Item item = interactions.get(key);
        if (item == null) {
            item = createItem("Interaction");
            item.setReference("protein1", p1RefId);
            item.setReference("protein2", p2RefId);
            interactions.put(key, item);
        }
        return item;
    }

//    private Item getInteraction(Map<MultiKey, Item> interactions, String protein1,
//            String protein2) throws ObjectStoreException {
//        MultiKey key = new MultiKey(protein1, protein2);
//        Item item = interactions.get(key);
//        if (item == null) {
//            item = getChadoDBConverter().createItem("Interaction");
//            item.setReference("gene1", refId);
//            item.setReference("gene2", gene2RefId);
//            interactions.put(key, item);
//        }
//        return item;
//    }


    /**
     * Create Interaction objects.
     */
    private void createInteractions(Connection connection)
        throws SQLException, ObjectStoreException {
        Map<MultiKey, Item> seenInteractions = new HashMap<MultiKey, Item>();
        ResultSet res = getInteractionResultSet(connection);
        String typeId = getRelationshipType();

        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            Integer otherFeatureId = new Integer(res.getInt("other_feature_id"));
            String pubTitle = res.getString("pub_title");
            Integer pubmedId = new Integer(res.getInt("pubmed_id"));
            FeatureData featureData = getFeatureMap().get(featureId);
            FeatureData otherFeatureData = getFeatureMap().get(otherFeatureId);

            OrganismData od = otherFeatureData.getOrganismData();
            Item dataSetItem = getChadoDBConverter().getDataSetItem(od.getTaxonId());
            String publicationItemId = makePublication(pubmedId);
            String name = "FlyBase:" + featureData.getChadoFeatureUniqueName() + "_"
                    + otherFeatureData.getChadoFeatureUniqueName();

            Item interaction = getInteraction(seenInteractions, featureData.getItemIdentifier(),
                    otherFeatureData.getItemIdentifier());
            createDetail(dataSetItem, pubTitle, publicationItemId, interaction, name, typeId);

            name = "FlyBase:" + otherFeatureData.getChadoFeatureUniqueName() + "_"
                    + featureData.getChadoFeatureUniqueName();
            interaction = getInteraction(seenInteractions, otherFeatureData.getItemIdentifier(),
                    featureData.getItemIdentifier());
            createDetail(dataSetItem, pubTitle, publicationItemId, interaction, name, typeId);
        }
        for (Item item : seenInteractions.values()) {
            getChadoDBConverter().store(item);
        }
    }

    private String getRelationshipType() throws ObjectStoreException {
        Item item = getChadoDBConverter().createItem("InteractionTerm");
        item.setAttribute("identifier", RELATIONSHIP_TYPE);
        getChadoDBConverter().store(item);
        return item.getIdentifier();
    }

    private void createDetail(Item dataSetItem, String pubTitle,
            String publicationItemId, Item interaction, String name, String typeId)
        throws SQLException, ObjectStoreException {
        Item detail = getChadoDBConverter().createItem("InteractionDetail");
        detail.setAttribute("name", name);
        detail.setAttribute("type", "genetic");
        detail.setAttribute("role1", DEFAULT_ROLE);
        detail.setAttribute("role2", DEFAULT_ROLE);
        String experimentItemIdentifier =
            makeInteractionExperiment(pubTitle, publicationItemId);
        detail.setReference("experiment", experimentItemIdentifier);
        detail.setReference("interaction", interaction);
        detail.setReference("relationshipType", typeId);
        detail.addToCollection("dataSets", dataSetItem);
        getChadoDBConverter().store(detail);
    }

    /**
     * Return the item identifier of the Interaction Item for the given pubmed id, creating the
     * Item if necessary.
     * @param experimentTitle the new title
     * @param publicationItemIdentifier the item identifier of the publication for this experiment
     * @return the interaction item identifier
     * @throws ObjectStoreException if the item can't be stored
     */
    protected String makeInteractionExperiment(String experimentTitle,
                                               String publicationItemIdentifier)
        throws ObjectStoreException {
        if (interactionExperiments.containsKey(experimentTitle)) {
            return interactionExperiments.get(experimentTitle);
        }
        Item experiment = getChadoDBConverter().createItem("InteractionExperiment");
        experiment.setAttribute("name", experimentTitle);
        experiment.setReference("publication", publicationItemIdentifier);
        getChadoDBConverter().store(experiment);
        String experimentId = experiment.getIdentifier();
        interactionExperiments.put(experimentTitle, experimentId);
        return experimentId;
    }





    /**
     * Return interaction data.
     * This is a protected method so that it can be overridden for testing
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getData(Connection connection)
        throws SQLException {
        String query =
                "SELECT i.Protein1, i.Protein2, i.Interactions_type_mi, i.Interactions_type "
                + " , i.Interactions_detection_mi, i.Interactions_detection "
                + " , i.Bind_id "
                + " FROM interactions i " ;
        return doQuery(connection, query, "getData");
    }

    /**
     * method to wrap the execution of a query with log info)
     * @param connection
     * @param query
     * @param comment for not logging
     * @return the result set
     * @throws SQLException
     */
    private ResultSet doQuery(Connection connection, String query, String comment)
        throws SQLException {
        // we could avoid passing comment if we trace the calling method
        // new Throwable().fillInStackTrace().getStackTrace()[1].getMethodName()
        LOG.info("executing: " + query);
        long bT = System.currentTimeMillis();
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        LOG.info("QUERY TIME " + comment + ": " + (System.currentTimeMillis() - bT) + " ms");
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDataSetTitle(int taxonId) {
        return DATASET_TITLE;
    }
}
