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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.sql.writebatch.Batch;
import org.intermine.sql.writebatch.BatchWriterPostgresCopyImpl;
import org.intermine.util.Util;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;


/**
 * Create items from the modENCODE metadata extensions to the chado schema.
 * @author Kim Rutherford,sc,rns
 */
public class ModEncodeMetaDataProcessor extends ChadoProcessor
{
    private static final Logger LOG = Logger.getLogger(ModEncodeMetaDataProcessor.class);
    private static final String DATA_IDS_TABLE_NAME = "data_ids";
    private static final String WIKI_URL = "http://wiki.modencode.org/project/index.php?title=";
    private static final String FILE_URL = "http://submit.modencode.org/submit/public/get_file/";
    private static final String DCC_PREFIX = "modENCODE_";
    private static final String NA_PROP = "not applicable";
    private static final Set<String> DB_RECORD_TYPES =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                    "GEO_record",
                    "ArrayExpress_record",
                    "TraceArchive_record",
                    "dbEST_record",
                    "ShortReadArchive_project_ID (SRA)",
                    "ShortReadArchive_project_ID_list (SRA)")));

    // submission maps
    // ---------------
    private Map<Integer, String> submissionOrganismMap = new HashMap<Integer, String>();
    // maps from chado identifier to lab/project details
    private Map<Integer, SubmissionDetails> submissionMap =
            new HashMap<Integer, SubmissionDetails>();
    // chado submission id to list of top level attributes, e.g. dev stage, organism_part
    private Map<Integer, ExperimentalFactor> submissionEFMap =
            new HashMap<Integer, ExperimentalFactor>();
    // subId to dcc id
    private Map<Integer, String> dccIdMap = new HashMap<Integer, String>();

    // superseded/deleted subId to dcc id: to be checked in case we need to skip
    // loading a sub
    private Map<Integer, String> deletedSubMap = new HashMap<Integer, String>();

    // applied_protocol/data/attribute maps
    // -------------------
    // chado submission id to chado data_id
    private Map<Integer, List<Integer>> submissionDataMap = new HashMap<Integer, List<Integer>>();
    // chado data id to chado submission id
    private Map<Integer, Integer> dataSubmissionMap = new HashMap<Integer, Integer>();

    // to store protocol data until we create applied protocols
    private Map<Integer, Protocol> protocolMap =
            new HashMap<Integer, Protocol>();
    // used when traversing dag of applied protocols
    private Map<Integer, AppliedProtocol> appliedProtocolMap =
            new HashMap<Integer, AppliedProtocol>();
    // used when traversing dag of applied protocols
    private Map<Integer, AppliedData> appliedDataMap =
            new HashMap<Integer, AppliedData>();

    // project/lab/experiment/submission maps
    // --------------------------------------

    // for projects, the maps link the project name with the identifiers...
    private Map<String, Integer> projectIdMap = new HashMap<String, Integer>();
    private Map<String, String> projectIdRefMap = new HashMap<String, String>();
    // for labs, the maps link the lab name with the identifiers...
    private Map<String, Integer> labIdMap = new HashMap<String, Integer>();
    private Map<String, String> labIdRefMap = new HashMap<String, String>();
    // for experiment, the maps link the exp name (description!) with the identifiers...
    private Map<String, Integer> experimentIdMap = new HashMap<String, Integer>();
    private Map<String, String> experimentIdRefMap = new HashMap<String, String>();
    private Map<String, List<Integer>> expSubMap = new HashMap<String, List<Integer>>();


    // ...we need a further map to link to submission
    private Map<Integer, String> submissionProjectMap = new HashMap<Integer, String>();
    private Map<Integer, String> submissionLabMap = new HashMap<Integer, String>();

    // to store the category in experiment
    private Map<Integer, String> submissionExpCatMap = new HashMap<Integer, String>();
    // to check if experiment type is set
    private Set<Integer> submissionWithExpTypeSet = new HashSet<Integer>();

    // submission/applied_protocol/protocol maps
    // -----------------------------------------

    private Map<String, String> protocolsMap = new HashMap<String, String>();
    private Map<Integer, String> protocolItemIds = new HashMap<Integer, String>();
    private Map<String, Integer> protocolItemToObjectId = new HashMap<String, Integer>();
    // submission chado id to item identifier of Protocol used to generate GFF
    private Map<Integer, String> scoreProtocols = new HashMap<Integer, String>();
    private Map<Integer, String> protocolTypesMap = new HashMap<Integer, String>();
    private Map<Integer, Integer> appliedProtocolIdMap = new HashMap<Integer, Integer>();
    private Map<Integer, String> appliedProtocolIdRefMap = new HashMap<Integer, String>();
    // list of firstAppliedProtocols, first level of the DAG linking
    // the applied protocols through the data (and giving the flow of data)
    private List<Integer> firstAppliedProtocols = new ArrayList<Integer>();

    private Map<Integer, Integer> publicationIdMap = new HashMap<Integer, Integer>();
    private Map<Integer, String> publicationIdRefMap = new HashMap<Integer, String>();

    // experimental factor maps
    // ------------------------
    private Map<String, Integer> eFactorIdMap = new HashMap<String, Integer>();
    private Map<String, String> eFactorIdRefMap = new HashMap<String, String>();
    private Map<Integer, List<String>> submissionEFactorMap = new HashMap<Integer, List<String>>();

    // caches
    // ------
    // cache cv term names by id
    private Map<String, String> cvtermCache = new HashMap<String, String>();

    private Map<String, String> devStageTerms = new HashMap<String, String>();
    private Map<String, String> devOntologies = new HashMap<String, String>();
    // just for debugging, itemIdentifier, type
    private Map<String, String> debugMap = new HashMap<String, String>();

    private Map<String, Item> nonWikiSubmissionProperties = new HashMap<String, Item>();
    private Map<String, Item> subItemsMap = new HashMap<String, Item>();
    Map<Integer, List<SubmissionReference>> submissionRefs = null;
    private IdResolverFactory flyResolverFactory = null;
    private IdResolverFactory wormResolverFactory = null;
    private Map<String, String> geneToItemIdentifier = new HashMap<String, String>();

    // DbRecords Integer=objectId String=submission.itemId
    // the second map is used directly to build the references
    private Map<DatabaseRecordKey, Integer> dbRecords = new HashMap<DatabaseRecordKey, Integer>();
    private Map<Integer, List<String>> dbRecordIdSubItems = new HashMap<Integer, List<String>>();


    private static final class SubmissionDetails
    {
        // the identifier assigned to Item eg. "0_23"
        private String itemIdentifier;
        // the object id of the stored Item
        private Integer interMineObjectId;
        // the identifier assigned to lab Item for this object
        private String labItemIdentifier;
        private String title;

        private SubmissionDetails() {
            // don't instantiate
        }
    }

    /**
     * AppliedProtocol class to reconstruct the flow of submission data
     */
    private static final class AppliedProtocol
    {
        private Integer submissionId;      // chado
        private Integer protocolId;
        private Integer step;              // the level in the dag for the AP

        // the output data associated to this applied protocol
        private List<Integer> outputs = new ArrayList<Integer>();
        private List<Integer> inputs = new ArrayList<Integer>();

        private AppliedProtocol() {
            // don't instantiate
        }
    }

    /**
     * Protocol class to store protocol data
     */
    private static final class Protocol
    {
        private Integer protocolId;      // possibly we don't need this (map)
        private String name;
        private String description;
        private String wikiLink;
        private Integer version;       // the level in the dag for the AP

        private Protocol() {
            // don't instantiate
        }
    }

    /**
     * AppliedData class
     * to reconstruct the flow of submission data
     *
     */
    private static final class AppliedData
    {
        private String itemIdentifier;
        private Integer intermineObjectId;
        private Integer dataId;
        private String value;
        private String actualValue;
        private String type;
        private String name;
        private String url;    // in particular, it stores the dccid of the related sub needed for
                               // linking to a result file
        // the list of applied protocols for which this data item is an input
        private List<Integer> nextAppliedProtocols = new ArrayList<Integer>();
        // the list of applied protocols for which this data item is an output
        private List<Integer> previousAppliedProtocols = new ArrayList<Integer>();

        private AppliedData() {
            // don't instantiate
        }
    }

    /**
     * Experimental Factor class
     * to store the couples (type, name/value) of EF
     * note that in chado sometime the name is given, other times is the value
     */
    private static final class ExperimentalFactor
    {
        private Map<String, String> efTypes = new HashMap<String, String>();
        private List<String> efNames = new ArrayList<String>();

        private ExperimentalFactor() {
            // don't instantiate
        }
    }

    /**
     * Create a new ChadoProcessor object
     * @param chadoDBConverter the converter that created this Processor
     */
    public ModEncodeMetaDataProcessor(ChadoDBConverter chadoDBConverter) {
        super(chadoDBConverter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Connection connection) throws Exception {

        processDeleted(connection);

        processProjectTable(connection);
        processLabTable(connection);

        processSubmissionOrganism(connection);
        processSubmission(connection);
        processSubmissionAttributes(connection);
        processProtocolTable(connection);
        processAppliedProtocolTable(connection);
        processProtocolAttributes(connection);

        processDag(connection);
        processAppliedData(connection);
        processAppliedDataAttributes(connection);
        processExperiment(connection);
        findScoreProtocols();

        processFeatures(connection, submissionMap);

        // set references
        setSubmissionRefs(connection);
        setSubmissionExperimentRefs(connection);
        setDAGRefs(connection);

        // create DatabaseRecords where necessary for each submission
        createDatabaseRecords(connection);
        // create result files per submission
        createResultFiles(connection);

        // for high level attributes and experimental factors (EF)
        // TODO: clean up
        processEFactor(connection);

        flyResolverFactory = new FlyBaseIdResolverFactory("gene");
        wormResolverFactory = new WormBaseChadoIdResolverFactory("gene");
        processSubmissionProperties(connection);
        createRelatedSubmissions(connection);
        setSubmissionProtocolsRefs(connection);
        setSubmissionEFactorsRefs(connection);
        setSubmissionPublicationRefs(connection);
    }

    /**
     * =========================
     *    DELETED SUBS in CHADO
     * =========================
     * To build a map of deleted subs (submissionId, dcc)
     *
     * @param connection
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private void processDeleted(Connection connection)
        throws SQLException, ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        ResultSet res = getDeleted(connection);
        while (res.next()) {
            Integer submissionId = new Integer(res.getInt("experiment_id"));
            String value = res.getString("value");
            deletedSubMap.put(submissionId, value);
        }
        res.close();

        LOG.info("found " + deletedSubMap.size() + " deleted submissions to skip in the build.");
        LOG.info("PROCESS TIME deleted subs: " + (System.currentTimeMillis() - bT) + " ms");
    }

    /**
     * Return deleted subs in the chado db.
     * This is a protected method so that it can be overridden for testing
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getDeleted(Connection connection)
        throws SQLException {
        String query =
                "SELECT distinct a.experiment_id, a.value "
                        + " FROM experiment_prop a "
                        + " where a.name = 'deleted' ";
        return doQuery(connection, query, "getDeleted");
    }


    /**
     *
     * ==============
     *    FEATURES
     * ==============
     *
     * @param connection
     * @param submissionMap
     * @throws Exception
     */

    private void processFeatures(Connection connection,
            Map<Integer, SubmissionDetails> submissionMap)
        throws Exception {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        // keep map of feature to submissions it has been referenced by, some features appear in
        // more than one submission
        Map<Integer, List<String>> subCollections = new HashMap<Integer, List<String>>();

        // hold features that should only be processed once across all submissions, initialise
        // processor with this map each time
        Map<Integer, FeatureData> commonFeaturesMap = new HashMap<Integer, FeatureData>();

        for (Map.Entry<Integer, SubmissionDetails> entry: submissionMap.entrySet()) {

            Integer chadoExperimentId = entry.getKey();
            if (deletedSubMap.containsKey(chadoExperimentId)) {
                continue;
            }
            Map<Integer, FeatureData> subFeatureMap = new HashMap<Integer, FeatureData>();
            SubmissionDetails submissionDetails = entry.getValue();
            String submissionItemIdentifier = submissionDetails.itemIdentifier;
            String labItemIdentifier = submissionDetails.labItemIdentifier;
            String submissionTitle = submissionDetails.title;

            List<Integer> thisSubmissionDataIds = submissionDataMap.get(chadoExperimentId);
            LOG.info("DATA IDS for " + dccIdMap.get(chadoExperimentId) + ": "
                    + thisSubmissionDataIds.size());

            // Create a temporary table with the feature ids related to this submission
            // based on the data_feature table
            String dataIdsTempTable = createDataIdsTempTable(connection, chadoExperimentId,
                    thisSubmissionDataIds);

            ModEncodeFeatureProcessor processor =
                    new ModEncodeFeatureProcessor(getChadoDBConverter(), submissionItemIdentifier,
                            labItemIdentifier, dataIdsTempTable, submissionTitle,
                            scoreProtocols.get(chadoExperimentId));
            processor.initialiseCommonFeatures(commonFeaturesMap);
            processor.process(connection);

            // all features related to this submission
            subFeatureMap.putAll(processor.getFeatureMap());

            // features common across many submissions
            commonFeaturesMap.putAll(processor.getCommonFeaturesMap());
            LOG.info("COMMON FEATURES: " + commonFeaturesMap.size());

            if (subFeatureMap.keySet().size() == 0) {
                LOG.error("FEATMAP: submission " + chadoExperimentId
                        + " has no featureMap keys.");
                continue;
            }
            LOG.info("FEATMAP: submission " + chadoExperimentId + "|"
                    + "featureMap: " + subFeatureMap.keySet().size());

            // Populate map of submissions to features, some features are in multiple submissions
            processDataFeatureTable(connection, subCollections, subFeatureMap,
                    chadoExperimentId, dataIdsTempTable);

            dropDataIdsTempTable(connection, dataIdsTempTable);

            // read any genes that have been created so we can re-use the same item identifiers
            // when creating antibody/strain target genes later
            extractGenesFromSubFeatureMap(processor, subFeatureMap);
        }

        storeSubmissionsCollections(subCollections);

        LOG.info("PROCESS TIME features: " + (System.currentTimeMillis() - bT) + " ms");
    }


    private void storeSubmissionsCollections(Map<Integer, List<String>> subCollections)
        throws ObjectStoreException {
        for (Map.Entry<Integer, List<String>> entry : subCollections.entrySet()) {
            Integer featureObjectId = entry.getKey();
            ReferenceList collection = new ReferenceList("submissions", entry.getValue());
            getChadoDBConverter().store(collection, featureObjectId);
        }
    }

    private void extractGenesFromSubFeatureMap(ModEncodeFeatureProcessor processor,
            Map<Integer, FeatureData> subFeatureMap) {
        for (FeatureData fData : subFeatureMap.values()) {
            if ("Gene".equals(fData.getInterMineType())) {
                String geneIdentifier = processor.fixIdentifier(fData, fData.getUniqueName());
                geneToItemIdentifier.put(geneIdentifier, fData.getItemIdentifier());
            }
        }
    }

    private void processDataFeatureTable(Connection connection, Map<Integer, List<String>> subCols,
            Map<Integer, FeatureData> featureMap, Integer chadoExperimentId, String dataIdTable)
        throws SQLException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        String submissionItemId = submissionMap.get(chadoExperimentId).itemIdentifier;

        bT = System.currentTimeMillis();     // to monitor time spent in the process
        ResultSet res = getDataFeature(connection, dataIdTable);
        while (res.next()) {
            Integer dataId = new Integer(res.getInt("data_id"));
            Integer featureId = new Integer(res.getInt("feature_id"));

            FeatureData featureData = featureMap.get(featureId);
            if (featureData == null) {
                LOG.debug("Check feature type: no data for feature_id: " + featureId
                        + " in processDataFeatureTable(), data_id =" + dataId);
                continue;
            }

            Integer featureObjectId = featureData.getIntermineObjectId();
            List<String> subs = subCols.get(featureObjectId);
            if (subs == null) {
                subs = new ArrayList<String>();
                subCols.put(featureObjectId, subs);
            }
            subs.add(submissionItemId);
        }
        LOG.info("DATA IDS PROCESS TIME data_feature table: " + (System.currentTimeMillis() - bT));
    }

    private String createDataIdsTempTable(Connection connection, Integer chadoExperimentId,
            List<Integer> dataIds) throws SQLException {

        // the batch writer system doesn't like to have duplicate named tables
        String tableName = DATA_IDS_TABLE_NAME + "_" + chadoExperimentId + "_"
                + System.currentTimeMillis();

        long bT = System.currentTimeMillis();
        String query =
                " CREATE TEMPORARY TABLE " + tableName + " (data_id int)";
        Statement stmt = connection.createStatement();
        LOG.info("executing: " + query);
        stmt.execute(query);

        try {
            BatchWriterPostgresCopyImpl batchWriter = new BatchWriterPostgresCopyImpl();
            Batch batch = new Batch(batchWriter);

            HashSet<Integer> uniqueDataIds = new HashSet<Integer>(dataIds);

            String[] colNames = new String[] {"data_id"};
            for (Integer dataId : uniqueDataIds) {
                batch.addRow(connection, tableName, dataId, colNames, new Object[] {dataId});
            }
            batch.flush(connection);
            batch.close(connection);

            LOG.info("CREATED DATA IDS TABLE: " + tableName + " with " + uniqueDataIds.size()
                    + " data ids in " + (System.currentTimeMillis() - bT) + "ms");

            String idIndexQuery = "CREATE INDEX " + tableName + "_data_id_index ON "
                    + tableName + "(data_id)";
            LOG.info("DATA IDS executing: " + idIndexQuery);
            long bT1 = System.currentTimeMillis();
            stmt.execute(idIndexQuery);
            LOG.info("DATA IDS TIME creating INDEX: " + (System.currentTimeMillis() - bT1) + "ms");
            String analyze = "ANALYZE " + tableName;
            LOG.info("executing: " + analyze);
            long bT2 = System.currentTimeMillis();
            stmt.execute(analyze);
            LOG.info("DATA IDS TIME analyzing: " + (System.currentTimeMillis() - bT2) + "ms");
        } catch (SQLException e) {
            // the batch writer system doesn't like to have duplicate named tables
            query = "DROP TABLE " + tableName;
            stmt.execute(query);
            throw e;
        }
        return tableName;
    }

    private void dropDataIdsTempTable(Connection connection, String dataIdsTableName)
        throws SQLException {
        long bT = System.currentTimeMillis();
        String query = " DROP TABLE " + dataIdsTableName;
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        stmt.execute(query);
        LOG.info("DATA IDS TIME dropping table '" + dataIdsTableName + "': "
                + (System.currentTimeMillis() - bT));
    }

    private ResultSet getDataFeature(Connection connection, String dataIdTable)
        throws SQLException {
        String query =
                "SELECT df.data_id, df.feature_id"
                        + " FROM data_feature df, " + dataIdTable + " d"
                        + " WHERE df.data_id = d.data_id";

        return doQuery(connection, query, "getDataFeature");
    }

    /**
     *
     * ====================
     *         DAG
     * ====================
     *
     * In chado, Applied protocols in a submission are linked to each other via
     * the flow of data (output of a parent AP are input to a child AP).
     * The method process the data from chado to build the objects
     * (SubmissionDetails, AppliedProtocol, AppliedData) and their
     * respective maps to chado identifiers needed to traverse the DAG.
     * It then traverse the DAG, assigning the experiment_id to all data.
     *
     * @param connection
     * @throws SQLException
     * @throws ObjectStoreException
     */

    private void processDag(Connection connection)
        throws SQLException, ObjectStoreException {
        long bT = System.currentTimeMillis();          // to monitor time spent in the process

        ResultSet res = getDAG(connection);
        AppliedProtocol node = new AppliedProtocol();
        AppliedData branch = null;
        Integer count = new Integer(0);
        Integer actualSubmissionId = new Integer(0);  // to store the experiment id (see below)
        Integer previousAppliedProtocolId = new Integer(0);
        boolean isADeletedSub = false;

        while (res.next()) {
            Integer submissionId = new Integer(res.getInt("experiment_id"));
            Integer protocolId = new Integer(res.getInt("protocol_id"));
            Integer appliedProtocolId = new Integer(res.getInt("applied_protocol_id"));
            Integer dataId = new Integer(res.getInt("data_id"));
            String direction = res.getString("direction");

            LOG.debug("DAG: " + submissionId + " p:" + protocolId + " ap:" + appliedProtocolId
                    + " d:" + dataId + " | " + direction);

            // the results are ordered, first ap have a subId
            // if we find a deleted sub, we know that subsequent records with null
            // subId belongs to the deleted sub
            // note that while the subId is null in the database, it is = 0 here
            if (submissionId == 0) {
                if (isADeletedSub) {
                    LOG.debug("DEL: skipping"  + isADeletedSub);
                    continue;
                }
            } else {
                if (deletedSubMap.containsKey(submissionId)) {
                    isADeletedSub = true;
                    LOG.debug("DEL: " + submissionId + " ->" + isADeletedSub);
                    continue;
                } else {
                    isADeletedSub = false;
                    LOG.debug("DEL: " + submissionId + " ->" + isADeletedSub);
                }
            }

            // build a data node for each iteration
            if (appliedDataMap.containsKey(dataId)) {
                branch = appliedDataMap.get(dataId);
            } else {
                branch = new AppliedData();
            }
            // could use > (order by apid, apdataid, direction)
            // NB: using isLast() is expensive
            if (!appliedProtocolId.equals(previousAppliedProtocolId) || res.isLast()) {
                // the submissionId != null for the first applied protocol
                if (submissionId > 0) {
                    firstAppliedProtocols.add(appliedProtocolId);
                    LOG.debug("DAG fap subId:" + submissionId + " apID: " + appliedProtocolId);
                    // set actual submission id
                    // we can either be at a first applied protocol (submissionId > 0)..
                    actualSubmissionId = submissionId;
                } else {
                    // ..or already down the dag, and we use the stored id.
                    submissionId = actualSubmissionId;
                }

                // last one: fill the list of outputs
                // and add to the general list of data ids for the submission,
                // used to fetch features
                if (res.isLast()) {
                    if ("output".equalsIgnoreCase(direction)) {
                        node.outputs.add(dataId);
                        mapSubmissionAndData(submissionId, dataId);
                    }
                }

                // if it is not the first iteration, let's store it
                if (previousAppliedProtocolId > 0) {
                    appliedProtocolMap.put(previousAppliedProtocolId, node);
                }

                // new node
                AppliedProtocol newNode = new AppliedProtocol();
                newNode.protocolId = protocolId;
                newNode.submissionId = submissionId;

                if (direction.startsWith("in")) {
                    // add this applied protocol to the list of nextAppliedProtocols
                    branch.nextAppliedProtocols.add(appliedProtocolId);
                    // ..and update the map
                    updateAppliedDataMap(branch, dataId);
                    // .. and add the dataId to the list of input Data for this applied protocol
                    newNode.inputs.add(dataId);
                    mapSubmissionAndData(submissionId, dataId); //***

                } else if (direction.startsWith("out")) {
                    // add the dataId to the list of output Data for this applied protocol:
                    // it will be used to link to the next set of applied protocols
                    newNode.outputs.add(dataId);
                    if (previousAppliedProtocolId > 0) {
                        branch.previousAppliedProtocols.add(previousAppliedProtocolId);
                        updateAppliedDataMap(branch, dataId); //***
                        mapSubmissionAndData(submissionId, dataId); //****
                    }
                } else {
                    // there is some problem with the strings 'input' or 'output'
                    throw new IllegalArgumentException("Data direction not valid for dataId: "
                            + dataId + "|" + direction + "|");
                }
                // for the new round..
                node = newNode;
                previousAppliedProtocolId = appliedProtocolId;

            } else {
                // keep feeding IN et OUT
                LOG.debug("DAG keep: current->" + appliedProtocolId + " prev->"
                        + previousAppliedProtocolId);
                if (direction.startsWith("in")) {
                    node.inputs.add(dataId);
                    if (submissionId > 0) {
                        // initial data
                        mapSubmissionAndData(submissionId, dataId);
                    }
                    // as above
                    branch.nextAppliedProtocols.add(appliedProtocolId);
                    updateAppliedDataMap(branch, dataId);
                } else if (direction.startsWith("out")) {
                    node.outputs.add(dataId);
                    branch.previousAppliedProtocols.add(appliedProtocolId);
                    updateAppliedDataMap(branch, dataId); //***
                } else {
                    throw new IllegalArgumentException("Data direction not valid for dataId: "
                            + dataId + "|" + direction + "|");
                }
            }
            count++;
        }
        LOG.info("created " + appliedProtocolMap.size()
                + "(" + count + " applied data points) DAG nodes (= applied protocols) in map");

        res.close();

        // now traverse the DAG, and associate submission with all the applied protocols
        traverseDag();
        // set the dag level as an attribute to applied protocol
        setAppliedProtocolSteps(connection);
        LOG.info("PROCESS TIME DAG: " + (System.currentTimeMillis() - bT) + " ms");
    }


    /**
     * @param newAD
     * @param dataId
     */
    private void updateAppliedDataMap(AppliedData newAD, Integer dataId) {
        if (appliedDataMap.containsKey(dataId)) {
            appliedDataMap.remove(dataId);
        }
        appliedDataMap.put(dataId, newAD);
    }

    /**
     * @param newAD
     * @param dataId
     */
    private void updateADMap(AppliedData newAD, Integer dataId, Integer intermineObjectId) {
        if (appliedDataMap.containsKey(dataId)) {
//            LOG.info("UPADdetails: " + dataId);
            AppliedData datum = appliedDataMap.get(dataId);
            datum.intermineObjectId = newAD.intermineObjectId;
            datum.itemIdentifier = newAD.itemIdentifier;
            datum.value = newAD.value;
            datum.actualValue = newAD.actualValue;
            datum.dataId = dataId;
            datum.type = newAD.type;
            datum.name = newAD.name;
            datum.url = newAD.url;
//            appliedDataMap.put(dataId, datum);
        } else {
            appliedDataMap.put(dataId, newAD);
        }
    }
   /**
     * to set the step attribute for the applied protocols
     */
    private void setAppliedProtocolSteps(Connection connection)
        throws ObjectStoreException {
        for (Integer appliedProtocolId : appliedProtocolMap.keySet()) {
            Integer step = appliedProtocolMap.get(appliedProtocolId).step;
            if (step != null) {
                Attribute attr = new Attribute("step", step.toString());
                getChadoDBConverter().store(attr, appliedProtocolIdMap.get(appliedProtocolId));
            } else {
                AppliedProtocol ap = appliedProtocolMap.get(appliedProtocolId);
                LOG.warn("AppliedProtocol.step not set for chado id: " + appliedProtocolId
                        + " sub " + dccIdMap.get(ap.submissionId) + " inputs " + ap.inputs
                        + " outputs " + ap.outputs);
            }
        }
    }

    // Look for protocols that were used to generated GFF files, these are passed to the feature
    // processor, if features have a score the protocol is set as the scoreProtocol reference.
    // NOTE this could equally be done with data, data_feature and applied_protocol_data
    private void findScoreProtocols() {
        for (Map.Entry<Integer, AppliedData> entry : appliedDataMap.entrySet()) {
            Integer dataId = entry.getKey();
            AppliedData aData = entry.getValue();
            if ("Result File".equals(aData.type)
                    && (aData.value.endsWith(".gff") || aData.value.endsWith("gff3"))) {
                for (Integer papId : aData.previousAppliedProtocols) {
                    AppliedProtocol aProtocol = appliedProtocolMap.get(papId);
                    String protocolItemId = protocolItemIds.get(aProtocol.protocolId);
                    scoreProtocols.put(dataSubmissionMap.get(dataId), protocolItemId);
                }
            }
        }
    }

    /**
     * Return the rows needed to construct the DAG of the data/protocols.
     * The reference to the submission is available only for the first set
     * of applied protocols, hence the outer join.
     * This is a protected method so that it can be overridden for testing
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getDAG(Connection connection)
        throws SQLException {
        String query =
                "SELECT eap.experiment_id, ap.protocol_id, apd.applied_protocol_id"
                        + " , apd.data_id, apd.applied_protocol_data_id, apd.direction"
                        + " FROM applied_protocol ap LEFT JOIN experiment_applied_protocol eap"
                        + " ON (eap.first_applied_protocol_id = ap.applied_protocol_id )"
                        + " , applied_protocol_data apd"
                        + " WHERE apd.applied_protocol_id = ap.applied_protocol_id"
                        + " ORDER By 3,5,6";
        return doQuery(connection, query, "getDAG");
    }

    /**
     * Applies iteratively buildADaglevel
     *
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private void traverseDag()
        throws ObjectStoreException {
        List<Integer> currentIterationAP = firstAppliedProtocols;
        List<Integer> nextIterationAP = new ArrayList<Integer>();
        Integer step = 1;     // DAG level

        while (currentIterationAP.size() > 0) {
            nextIterationAP = buildADagLevel (currentIterationAP, step);
            currentIterationAP = nextIterationAP;
            step++;
        }
    }

    /**
     * This method is given a set of applied protocols (already associated with a submission)
     * and produces the next set of applied protocols. The latter are the protocols attached to the
     * output data of the starting set (output data for a applied protocol is the input data for the
     * next one).
     * It also fills the map linking directly results ('leaf' output data) with submission
     *
     * @param previousAppliedProtocols
     * @return the next batch of appliedProtocolId
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private List<Integer> buildADagLevel(List<Integer> previousAppliedProtocols, Integer step)
        throws ObjectStoreException {
        List<Integer> nextIterationProtocols = new ArrayList<Integer>();
        Iterator<Integer> pap = previousAppliedProtocols.iterator();
        while (pap.hasNext()) {
            List<Integer> outputs = new ArrayList<Integer>();
            List<Integer> inputs = new ArrayList<Integer>();
            Integer currentId = pap.next();
            // add the DAG level here only if these are the first AP
            if (step == 1) {
                appliedProtocolMap.get(currentId).step = step;
            }
            outputs.addAll(appliedProtocolMap.get(currentId).outputs);
            Integer submissionId = appliedProtocolMap.get(currentId).submissionId;
            Iterator<Integer> od = outputs.iterator();
            while (od.hasNext()) {
                Integer currentOD = od.next();
                List<Integer> nextProtocols = new ArrayList<Integer>();
                // build map submission-data
                mapSubmissionAndData(submissionId, currentOD);
                if (appliedDataMap.containsKey(currentOD)) {
                    // fill the list of next (children) protocols
                    nextProtocols.addAll(appliedDataMap.get(currentOD).nextAppliedProtocols);
                    if (appliedDataMap.get(currentOD).nextAppliedProtocols.isEmpty()) {
                        // this is a leaf!!
                        // TODO check this
                        LOG.info("DAG leaf: " + submissionId + " dataId: " + currentOD);
                    }
                }

                // to fill submission-dataId map
                // this is needed, otherwise inputs to AP that are not outputs
                // of a previous protocol are not considered
                inputs.addAll(appliedProtocolMap.get(currentId).inputs);
                Iterator<Integer> in = inputs.iterator();
                while (in.hasNext()) {
                    Integer currentIn = in.next();
                    // build map submission-data
                    mapSubmissionAndData(submissionId, currentIn);
                }

                // build the list of children applied protocols chado identifiers
                // as input for the next iteration
                Iterator<Integer> nap = nextProtocols.iterator();
                while (nap.hasNext()) {
                    // and fill the map with the chado experiment_id and the DAG level
                    Integer currentAPId = nap.next();
                    appliedProtocolMap.get(currentAPId).submissionId = submissionId;
                    appliedProtocolMap.get(currentAPId).step = step + 1;

                    nextIterationProtocols.add(currentAPId);

                    // and set the reference from applied protocol to the submission
                    Reference reference = new Reference();
                    reference.setName("submission");
                    reference.setRefId(submissionMap.get(submissionId).itemIdentifier);
                    getChadoDBConverter().store(reference, appliedProtocolIdMap.get(currentAPId));
                }
            }
        }
        return nextIterationProtocols;
    }

    /**
     * ==============
     *    ORGANISM
     * ==============
     * Organism for a submission is derived from the organism associated with
     * the protocol of the first applied protocol (of the submission).
     * it is the name. a request to associate the submission directly with
     * the taxonid has been made to chado people.
     *
     * @param connection
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private void processSubmissionOrganism(Connection connection)
        throws SQLException, ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        ResultSet res = getSubmissionOrganism(connection);
        while (res.next()) {
            Integer submissionId = new Integer(res.getInt("experiment_id"));
            if (deletedSubMap.containsKey(submissionId)) {
                continue;
            }

            String value = res.getString("value");
            submissionOrganismMap.put(submissionId, value);
            LOG.debug("TAXID " + submissionId + "|" + value);
        }
        res.close();
        LOG.info("found an organism for " + submissionOrganismMap.size() + " submissions.");
        LOG.info("PROCESS TIME organisms: " + (System.currentTimeMillis() - bT) + " ms");
    }

    /**
     * Return the row needed for the organism.
     * This is a protected method so that it can be overridden for testing
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getSubmissionOrganism(Connection connection)
        throws SQLException {
        String query =
                "select distinct eap.experiment_id, a.value "
                        + " from experiment_applied_protocol eap, applied_protocol ap, "
                        + " protocol_attribute pa, attribute a "
                        + " where eap.first_applied_protocol_id = ap.applied_protocol_id "
                        + " and ap.protocol_id=pa.protocol_id "
                        + " and pa.attribute_id=a.attribute_id "
                        + " and a.heading='species' ";
        return doQuery(connection, query, "getSubmissionOrganism");
    }

    /**
     * ==============
     *    PROJECT
     * ==============
     * Projects are loaded statically. A map is built between submissionId and
     * project's name and used for the references. 2 maps store intermine
     * objectId and itemId, with key the project name.
     * Note: the project name in chado is the surname of the PI
     *
     * @param connection
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private void processProjectTable(Connection connection)
        throws SQLException, ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        ResultSet res = getProjects(connection);
        while (res.next()) {
            Integer submissionId = new Integer(res.getInt("experiment_id"));
            String value = res.getString("value");
            if (deletedSubMap.containsKey(submissionId)) {
                continue;
            }
            submissionProjectMap.put(submissionId, value);
        }
        res.close();

        Set<Integer> exp = submissionProjectMap.keySet();
        Iterator<Integer> i  = exp.iterator();
        while (i.hasNext()) {
            Integer thisExp = i.next();
            String project = submissionProjectMap.get(thisExp);

            if (projectIdMap.containsKey(project)) {
                continue;
            }
            LOG.debug("PROJECT: " + project);
            Item pro = getChadoDBConverter().createItem("Project");
            pro.setAttribute("surnamePI", project);
            Integer intermineObjectId = getChadoDBConverter().store(pro);
            storeInProjectMaps(pro, project, intermineObjectId);
        }
        LOG.info("created " + projectIdMap.size() + " project");
        LOG.info("PROCESS TIME projects: " + (System.currentTimeMillis() - bT) + " ms");
    }

    /**
     * Return the project name.
     * This is a protected method so that it can be overridden for testing
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getProjects(Connection connection)
        throws SQLException {
        String query =
                "SELECT distinct a.experiment_id, a.value "
                        + " FROM experiment_prop a "
                        + " where a.name = 'Project' "
                        + " AND rank=0";
        return doQuery(connection, query, "getProjects");
    }

    /**
     * ==============
     *    LAB
     * ==============
     * Labs are also loaded statically (affiliation is not given in the chado file).
     * A map is built between submissionId and
     * lab's name and used for the references. 2 maps store intermine
     * objectId and itemId, with key the lab name.
     * TODO: do project and lab together (1 query, 1 process)
     *
     * @param connection
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private void processLabTable(Connection connection)
        throws SQLException, ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        ResultSet res = getLabs(connection);
        while (res.next()) {
            Integer submissionId = new Integer(res.getInt("experiment_id"));
            String value = res.getString("value");
            if (deletedSubMap.containsKey(submissionId)) {
                continue;
            }
            submissionLabMap.put(submissionId, value);
        }
        res.close();

        Set<Integer> exp = submissionLabMap.keySet();
        Iterator<Integer> i  = exp.iterator();
        while (i.hasNext()) {
            Integer thisExp = i.next();
            String prov = submissionLabMap.get(thisExp);
            String project = submissionProjectMap.get(thisExp);

            if (labIdMap.containsKey(prov)) {
                continue;
            }
            LOG.debug("PROV: " + prov);
            Item lab = getChadoDBConverter().createItem("Lab");
            lab.setAttribute("surname", prov);
            lab.setReference("project", projectIdRefMap.get(project));

            Integer intermineObjectId = getChadoDBConverter().store(lab);
            storeInLabMaps(lab, prov, intermineObjectId);
        }
        LOG.info("created " + labIdMap.size() + " labs");
        LOG.info("PROCESS TIME labs: " + (System.currentTimeMillis() - bT) + " ms");
    }

    /**
     * Return the lab name.
     * This is a protected method so that it can be overridden for testing
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getLabs(Connection connection)
        throws SQLException {
        String query =
                "SELECT distinct a.experiment_id, a.name, a.value "
                        + " FROM experiment_prop a "
                        + " where a.name = 'Lab' "
                        + " AND a.rank=0";
        return doQuery(connection, query, "getLabs");
    }

    /**
     * ================
     *    EXPERIMENT
     * ================
     * Experiment is a collection of submissions. They all share the same description.
     * It has been added later to the model.
     * A map is built between submissionId and experiment name.
     * 2 maps store intermine objectId and itemId, with key the experiment name.
     * They are probably not needed.
     *
     */
    private void processExperiment(Connection connection)
        throws SQLException, ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        ResultSet res = getExperimentTitles(connection);
        Map<String, String> expProMap = new HashMap<String, String>();
        while (res.next()) {
            Integer submissionId = new Integer(res.getInt("experiment_id"));
            if (deletedSubMap.containsKey(submissionId)) {
                continue;
            }

            String name = cleanWikiLinks(res.getString("name"));

            Util.addToListMap(expSubMap, name, submissionId);
            expProMap.put(name, submissionProjectMap.get(submissionId));
        }
        res.close();

        Set<String> experiment = expSubMap.keySet();
        Iterator<String> i  = experiment.iterator();
        while (i.hasNext()) {
            String name = i.next();

            Item exp = getChadoDBConverter().createItem("Experiment");
            exp.setAttribute("name", name);

            // find experiment category from map (take first available)
            // use the commented lines to get a report of assignments
            String category =  null;
            for (Integer ii : expSubMap.get(name)) {
//                String dccId = dccIdMap.get(ii);
                category = submissionExpCatMap.get(ii);
                if (category != null && !category.isEmpty()) {
//                    LOG.info("ECS " + name + "|" + dccId + ": " + category);
                    exp.setAttribute("category", category);
                    break;
                } //else {
                    //LOG.warn("ECS " + name + "|" + dccId + ": no category");
                //}
            }

            String project = expProMap.get(name);
            exp.setReference("project", projectIdRefMap.get(project));
            // note: the reference to submission collection is in a separate method
            Integer intermineObjectId = getChadoDBConverter().store(exp);

            experimentIdMap .put(name, intermineObjectId);
            experimentIdRefMap .put(name, exp.getIdentifier());
        }
        LOG.info("created " + expSubMap.size() + " experiments");
        LOG.info("PROCESS TIME experiments: " + (System.currentTimeMillis() - bT) + " ms");
    }

    /**
     * method to clean a wiki reference (url to a named page) in chado
     * used also for experiment names
     * @param w       the wiki reference
     */
    private String cleanWikiLinks(String w) {
        String url = "http://wiki.modencode.org/project/index.php?title=";
        // we are stripping from first ':', maybe we want to include project suffix
        // e.g.:
        // original "http://...?title=Gene Model Prediction:SC:1&amp;oldid=12356"
        // now: Gene Model Prediction
        // maybe? Gene Model Prediction:SC:1
        // (:->&)
        String w1 = StringUtils.replace(w, url, "");
        String s1 = null;
        if (w1.contains(":")) {
            s1 = StringUtils.substringBefore(w1, ":");
        } else {
            // for links missing the : char, e.g.
            // MacAlpine Early Origin of Replication Identification&oldid=10464
            s1 = StringUtils.substringBefore(w1, "&");
        }
        String s = s1.replace('"', ' ').trim();
        if (s.contains("%E2%80%99")) {
            // prime: for the Piano experiment
            String s2 = s.replace("%E2%80%99", "'");
            return s2;
        }
        if (s.contains("%28A%29%2B")) {
            // this is (A)+, in
            // Stranded Cell Line Transcriptional Profiling Using Illumina poly%28A%29%2B RNA-seq
            String s2 = s.replace("%28A%29%2B", "(A)+");
            return s2;
        }
        if (s.contains("%2B")) {
            // +: for Celniker experiment "Tissue-specific Transcriptional Profiling..."
            String s2 = s.replace("%2B", "+");
            return s2;
        }
        return s;
    }

    /**
     * Return the rows needed for experiment from the experiment_prop table.
     * This is a protected method so that it can be overridden for testing
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getExperimentTitles(Connection connection)
        throws SQLException {
        // TODO use standard SQl and deal with string in java
        String query =
                "select e.experiment_id, "
                        + " translate(x.accession, '_', ' ') as name "
                        + " from experiment_prop e, dbxref x "
                        + " where e.dbxref_id = x.dbxref_id "
                        + " and e.name='Experiment Description' ";
        return doQuery(connection, query, "getExperimentTitles");
    }

    /**
     * ================
     *    SUBMISSION
     * ================
     */
    private void processSubmission(Connection connection)
        throws SQLException, ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        ResultSet res = getSubmissions(connection);
        int count = 0;
        while (res.next()) {
            Integer submissionId = new Integer(res.getInt("experiment_id"));
            if (deletedSubMap.containsKey(submissionId)) {
                continue;
            }
            String name = res.getString("uniquename");
            Item submission = getChadoDBConverter().createItem("Submission");
            submission.setAttribute("title", name);

            String project = submissionProjectMap.get(submissionId);
            String projectItemIdentifier = projectIdRefMap.get(project);
            submission.setReference("project", projectItemIdentifier);

            String labName = submissionLabMap.get(submissionId);
            String labItemIdentifier = labIdRefMap.get(labName);
            submission.setReference("lab", labItemIdentifier);
            String organismName = submissionOrganismMap.get(submissionId);


            int divPos = organismName.indexOf(' ');
            String genus = organismName.substring(0, divPos);
            String species = organismName.substring(divPos + 1);

            OrganismRepository or = OrganismRepository.getOrganismRepository();

            Integer taxId = Integer.valueOf(
                    or.getOrganismDataByGenusSpecies(genus, species).getTaxonId());
            LOG.debug("SPECIES: " + organismName + "|" + taxId);
            String organismItemIdentifier = getChadoDBConverter().getOrganismItem(
                    or.getOrganismDataByGenusSpecies(genus, species).getTaxonId()).getIdentifier();
            submission.setReference("organism", organismItemIdentifier);
            // ..store all
            Integer intermineObjectId = getChadoDBConverter().store(submission);
            // ..and fill the SubmissionDetails object
            SubmissionDetails details = new SubmissionDetails();
            details.interMineObjectId = intermineObjectId;
            details.itemIdentifier = submission.getIdentifier();
            details.labItemIdentifier = labItemIdentifier;
            details.title = name;
            submissionMap.put(submissionId, details);
            debugMap .put(details.itemIdentifier, submission.getClassName());
            count++;
        }
        LOG.info("created " + count + " submissions");
        res.close();
        LOG.info("PROCESS TIME submissions: " + (System.currentTimeMillis() - bT) + " ms");
    }

    /**
     * Return the rows needed for the submission table.
     * This is a protected method so that it can be overridden for testing
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getSubmissions(Connection connection)
        throws SQLException {
        String query =
                "SELECT experiment_id, uniquename "
                        + "FROM experiment";
        return doQuery(connection, query, "getSubmissions");
    }

    /**
     * submission attributes (table experiment_prop)
     *
     * @param connection
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private void processSubmissionAttributes(Connection connection)
        throws SQLException, ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        ResultSet res = getExperimentProperties(connection);
        int count = 0;
        while (res.next()) {
            Integer submissionId = new Integer(res.getInt("experiment_id"));
            if (deletedSubMap.containsKey(submissionId)) {
                continue;
            }
            String heading = res.getString("name");
            String value = res.getString("value");

            // TODO this is a temporary hack to make sure we get properly matched Experiment.factors
            // EF are dealt with separately
            if (heading.startsWith("Experimental Factor")) {
                continue;
            }

            String fieldName = FIELD_NAME_MAP.get(heading);
            if (fieldName == null) {
                LOG.error("NOT FOUND in FIELD_NAME_MAP: " + heading + " [experiment]");
                continue;
            } else if (fieldName == NOT_TO_BE_LOADED) {
                continue;
            }
            if ("DCCid".equals(fieldName)) {
                value = DCC_PREFIX + value;
                LOG.debug("DCC: " + submissionId + ", " + value);
                dccIdMap.put(submissionId, value);
            }

            if ("category".equals(fieldName)) {
                // Data Type, stored in experiment
                submissionExpCatMap.put(submissionId, value);
                continue;
            }

            if (fieldName.endsWith("Read Count")) {
                // all read counts are considered a collection for submission
                Item readCount = getChadoDBConverter().createItem("ReadCount");
                readCount.setAttribute("name", fieldName);
                readCount.setAttribute("value", value);

                // setting references to SubmissionData
                readCount.setReference("submission",
                        submissionMap.get(submissionId).itemIdentifier);
                Integer intermineObjectId = getChadoDBConverter().store(readCount);
                continue;
            }

            if ("experimentType".equals(fieldName)) {
                // Assay Type
                submissionWithExpTypeSet.add(submissionId);
            }

            if ("pubMedId".equals(fieldName)) {
                // sometime in the form PMID:16938558
                if (value.contains(":")) {
                    value = value.substring(value.indexOf(':') + 1);
                }

                Item pub = getChadoDBConverter().createItem("Publication");
                pub.setAttribute(fieldName, value);
                Integer intermineObjectId = getChadoDBConverter().store(pub);

                publicationIdMap.put(submissionId, intermineObjectId);
                publicationIdRefMap.put(submissionId, pub.getIdentifier());
                continue;
            }

            setAttribute(submissionMap.get(submissionId).interMineObjectId, fieldName, value);
            count++;
        }
        LOG.info("created " + count + " submissions attributes");
        res.close();
        LOG.info("PROCESS TIME submissions attributes: "
                + (System.currentTimeMillis() - bT) + " ms");
    }

    /**
     * Return the rows needed for submission from the experiment_prop table.
     * This is a protected method so that it can be overridden for testing
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getExperimentProperties(Connection connection)
        throws SQLException {
        String query =
                "SELECT ep.experiment_id, ep.name, ep.value, ep.rank "
                        + "from experiment_prop ep ";
        return doQuery(connection, query, "getExperimentProperties");
    }

    /**
     * ==========================
     *    EXPERIMENTAL FACTORS
     * ==========================
     */
    private void processEFactor(Connection connection)
        throws SQLException, ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        ResultSet res = getEFactors(connection);
        int count = 0;
        int prevRank = -1;
        int prevSub = -1;
        ExperimentalFactor ef = null;
        String name = null;

        while (res.next()) {
            Integer submissionId = new Integer(res.getInt("experiment_id"));
            if (deletedSubMap.containsKey(submissionId)) {
                continue;
            }

            Integer rank = new Integer(res.getInt("rank"));
            String  value    = res.getString("value");

            // the data is alternating between EF types and names, in order.
            if (submissionId != prevSub) {
                // except for the first record, this is a new EF object
                if (!res.isFirst()) {
                    submissionEFMap.put(prevSub, ef);
                }
                ef = new ExperimentalFactor();
            }
            if (rank != prevRank || submissionId != prevSub) {
                // this is a name
                if (getPreferredSynonym(value) != null) {
                    value = getPreferredSynonym(value);
                }
                ef.efNames.add(value);
                name = value;
                count++;
            } else {
                // this is a type
                ef.efTypes.put(name, value);
                name = null;
                if (res.isLast()) {
                    submissionEFMap.put(submissionId, ef);
                    LOG.debug("EF MAP last: " + submissionId + "|" + rank + "|" + ef.efNames);
                }
            }
            prevRank = rank;
            prevSub = submissionId;
        }
        res.close();
        LOG.info("created " + count + " experimental factors");
        LOG.info("PROCESS TIME experimental factors: " + (System.currentTimeMillis() - bT) + " ms");
    }

    /**
     * Return the rows needed for the experimental factors.
     * This is a protected method so that it can be overridden for testing
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getEFactors(Connection connection)
        throws SQLException {
        String query =
                "SELECT ep.experiment_id, ep.name, ep.value, ep.rank "
                        + " FROM experiment_prop ep "
                        + " where ep.name = 'Experimental Factor Name' "
                        + " OR ep.name = 'Experimental Factor Type' "
                        + " ORDER BY 1,4,2";
        return doQuery(connection, query, "getEFactors");
    }


    /**
     * ==============
     *    PROTOCOL
     * ==============
     */
    private void processProtocolTable(Connection connection)
        throws SQLException, ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        ResultSet res = getProtocols(connection);
        int count = 0;
        while (res.next()) {
            Integer protocolChadoId = new Integer(res.getInt("protocol_id"));
            String name = res.getString("name");
            String description = res.getString("description");
            String wikiLink = res.getString("accession");
            Integer version = res.getInt("version");
            // needed: it breaks otherwise
            if (description.length() == 0) {
                description = "N/A";
            }
            Protocol thisProt = new Protocol();
            thisProt.protocolId = protocolChadoId;     // rm?
            thisProt.name = name;
            thisProt.description = description;
            thisProt.wikiLink = wikiLink;
            thisProt.version = version;
            protocolMap.put(protocolChadoId, thisProt);
            // we'll do it when creating AP
            //createProtocol(protocolChadoId, name, description, wikiLink, version);
            count++;
        }
        res.close();
        LOG.info("found " + count + " protocols");
        LOG.info("PROCESS TIME protocols: " + (System.currentTimeMillis() - bT) + " ms");
    }

    // now doing it with applied protocol (to avoid creating it for delete subs)
    private String createProtocol(Integer chadoId, String name, String description, String wikiLink,
            Integer version) throws ObjectStoreException {
        String protocolItemId = protocolsMap.get(wikiLink);     // rename?
        if (protocolItemId == null) {
            Item protocol = getChadoDBConverter().createItem("Protocol");
            protocol.setAttribute("name", name);
            protocol.setAttribute("description", description);
            protocol.setAttribute("wikiLink", wikiLink);
            protocol.setAttribute("version", "" + version);
            Integer intermineObjectId = getChadoDBConverter().store(protocol);
            protocolItemId = protocol.getIdentifier();
            protocolItemToObjectId.put(protocolItemId, intermineObjectId);
            protocolsMap.put(wikiLink, protocolItemId);
        }
        protocolItemIds.put(chadoId, protocolItemId);
        return protocolItemId;
    }

    private Integer getProtocolInterMineId(Integer chadoId) {
        return protocolItemToObjectId.get(protocolItemIds.get(chadoId));
    }

    /**
     * Return the rows needed from the protocol table.
     * This is a protected method so that it can be overridden for testing
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getProtocols(Connection connection) throws SQLException {
        String query =
                "SELECT protocol_id, name, protocol.description, accession, protocol.version"
                        + "  FROM protocol, dbxref"
                        + "  WHERE protocol.dbxref_id = dbxref.dbxref_id";
        return doQuery(connection, query, "getProtocols");
    }

    /**
     * to store protocol attributes
     */
    private void processProtocolAttributes(Connection connection)
        throws SQLException, ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        ResultSet res = getProtocolAttributes(connection);
        int count = 0;
        while (res.next()) {
            Integer protocolId = new Integer(res.getInt("protocol_id"));
            String heading = res.getString("heading");
            String value = res.getString("value");
            String fieldName = FIELD_NAME_MAP.get(heading);
            if (fieldName == null) {
                LOG.error("NOT FOUND in FIELD_NAME_MAP: " + heading + " [protocol]");
                continue;
            } else if (fieldName == NOT_TO_BE_LOADED) {
                continue;
            }
            if (getProtocolInterMineId(protocolId) != null) {     // in case of deleted sub
                setAttribute(getProtocolInterMineId(protocolId), fieldName, value);
                if ("type".equals(fieldName)) {
                    protocolTypesMap.put(protocolId, value);
                }
                count++;
            }
        }
        LOG.info("created " + count + " protocol attributes");
        res.close();
        LOG.info("PROCESS TIME protocol attributes: " + (System.currentTimeMillis() - bT) + " ms");
    }

    /**
     * Return the rows needed for protocols from the attribute table.
     * This is a protected method so that it can be overridden for testing
     * @param connection database connection
     * @return rows needed for protocols from the attribute table
     * @throws SQLException if something goes wrong.
     */
    protected ResultSet getProtocolAttributes(Connection connection) throws SQLException {
        String query =
                "SELECT p.protocol_id, a.heading, a.value "
                        + "from protocol p, attribute a, protocol_attribute pa "
                        + "where pa.attribute_id = a.attribute_id "
                        + "and pa.protocol_id = p.protocol_id ";
        return doQuery(connection, query, "getProtocolAttributes");
    }

    /**
     * ======================
     *    APPLIED PROTOCOL
     * ======================
     */
    private void processAppliedProtocolTable(Connection connection)
        throws SQLException, ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        ResultSet res = getAppliedProtocols(connection);
        int count = 0;
        boolean isADeletedSub = false;
        while (res.next()) {
            Integer appliedProtocolId = new Integer(res.getInt("applied_protocol_id"));
            Integer protocolId = new Integer(res.getInt("protocol_id"));
            Integer submissionId = new Integer(res.getInt("experiment_id"));
            // the results are ordered, first ap have a subId
            // if we find a deleted sub, we know that subsequent records with null
            // subId belongs to the deleted sub
            if (submissionId == 0) {
                boolean t = true;
                if (isADeletedSub == t) {
                    continue;
                }
            } else {
                if (deletedSubMap.containsKey(submissionId)) {
                    isADeletedSub = true;
                    continue;
                } else {
                    isADeletedSub = false;
                }
            }

            Item appliedProtocol = getChadoDBConverter().createItem("AppliedProtocol");

            // creating and setting references to protocols
            //            String protocolItemId = protocolItemIds.get(protocolId);
            if (protocolId != null) {
                Protocol qq = protocolMap.get(protocolId);
                String protocolItemId = createProtocol(qq);
                appliedProtocol.setReference("protocol", protocolItemId);
            }
            if (submissionId > 0) {
                // setting reference to submission
                // probably to rm (we do it later anyway). TODO: check
                appliedProtocol.setReference("submission",
                        submissionMap.get(submissionId).itemIdentifier);
            }
            // store it and add to maps
            Integer intermineObjectId = getChadoDBConverter().store(appliedProtocol);
            appliedProtocolIdMap .put(appliedProtocolId, intermineObjectId);
            appliedProtocolIdRefMap .put(appliedProtocolId, appliedProtocol.getIdentifier());
            count++;
        }
        LOG.info("created " + count + " appliedProtocol");
        res.close();
        LOG.info("PROCESS TIME applied protocols: " + (System.currentTimeMillis() - bT) + " ms");
    }
    private String createProtocol(Protocol p)
        throws ObjectStoreException {
        String protocolItemId = protocolsMap.get(p.wikiLink);     // rename map?
        if (protocolItemId == null) {
            Item protocol = getChadoDBConverter().createItem("Protocol");
            protocol.setAttribute("name", p.name);
            protocol.setAttribute("description", p.description);
            protocol.setAttribute("wikiLink", p.wikiLink);
            protocol.setAttribute("version", "" + p.version);
            Integer intermineObjectId = getChadoDBConverter().store(protocol);
            protocolItemId = protocol.getIdentifier();
            protocolItemToObjectId.put(protocolItemId, intermineObjectId);
            protocolsMap.put(p.wikiLink, protocolItemId);
        }
        protocolItemIds.put(p.protocolId, protocolItemId);
        return protocolItemId;
    }

    /**
     * Return the rows needed from the appliedProtocol table.
     * This is a protected method so that it can be overridden for testing
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getAppliedProtocols(Connection connection)
        throws SQLException {
        String query =
                "SELECT eap.experiment_id ,ap.applied_protocol_id, ap.protocol_id"
                        + " FROM applied_protocol ap"
                        + " LEFT JOIN experiment_applied_protocol eap"
                        + " ON (eap.first_applied_protocol_id = ap.applied_protocol_id )";
        return doQuery(connection, query, "getAppliedProtocols");
    }

    /**
     * ======================
     *    APPLIED DATA
     * ======================
     */
    private void processAppliedData(Connection connection)
        throws SQLException, ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        ResultSet res = getAppliedData(connection);
        int count = 0;
        while (res.next()) {
            Integer dataId = new Integer(res.getInt("data_id"));
            // check if not belonging to a deleted sub
            Integer submissionId = dataSubmissionMap.get(dataId);
            if (submissionId == null || deletedSubMap.containsKey(submissionId)) {
                continue;
            }
            String name = res.getString("name");
            String heading = res.getString("heading");
            String value = res.getString("value");
            String typeId = res.getString("type_id");
            String url = res.getString("url");

            // check if this datum has an official name:
            ResultSet oName = getOfficialName(connection, dataId);
            String officialName = null;
            while (oName.next()) {
                officialName = oName.getString(1);
            }

            // if there is one, use it instead of the value
            String datumType = getCvterm(connection, typeId);
            name = getCvterm(connection, typeId);
            if (!StringUtils.isEmpty(officialName)
                    && doReplaceWithOfficialName(heading, datumType)) {
                value = officialName;
            }
            Item submissionData = getChadoDBConverter().createItem("SubmissionData");
            if (name != null && !"".equals(name)) {
                submissionData.setAttribute("name", name);
            }
            // if no name for attribute fetch the cvterm of the type
            if ((name == null || "".equals(name)) && typeId != null) {
                name = getCvterm(connection, typeId);
                submissionData.setAttribute("name", name);
            }

            if (!StringUtils.isEmpty(value)) {
                submissionData.setAttribute("value", value);
            }
            submissionData.setAttribute("type", heading);

            // store it and add to object and maps
            Integer intermineObjectId = getChadoDBConverter().store(submissionData);

            AppliedData aData = new AppliedData();
            aData.intermineObjectId = intermineObjectId;
            aData.itemIdentifier = submissionData.getIdentifier();
            aData.value = value;
            aData.actualValue = res.getString("value");
            aData.dataId = dataId;
            aData.type = heading;
            aData.name = name;
            aData.url = url;
            updateADMap(aData,dataId,intermineObjectId);
            //appliedDataMap.put(dataId, aData);
            count++;
        }
        LOG.info("created " + count + " SubmissionData");
        res.close();
        LOG.info("PROCESS TIME submission data: " + (System.currentTimeMillis() - bT) + " ms");
    }

    // For some data types we don't want to replace with official name - e.g. file names and
    // database record ids.  It looks like the official name shouldn't actually be present.
    private boolean doReplaceWithOfficialName(String heading, String type) {
        if ("Result File".equals(heading)) {
            return false;
        }

        if ("Result Value".equals(heading) && DB_RECORD_TYPES.contains(type)) {
            return false;
        }
        return true;
    }

    /**
     * Return the rows needed for data from the applied_protocol_data table.
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getAppliedData(Connection connection)
        throws SQLException {
        String query =
                "SELECT d.data_id,"
                        + " d.heading, d.name, d.value, d.type_id, z.url"
                        + " FROM data d"
                        + " LEFT JOIN dbxref as y ON (d.dbxref_id = y.dbxref_id)"
                        + " LEFT JOIN db as z ON (y.db_id = z.db_id)";
        return doQuery(connection, query, "getAppliedData");
    }

    /**
     * Return the rows needed for data from the applied_protocol_data table.
     *
     * @param connection the db connection
     * @param dataId the dataId
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getOfficialName(Connection connection, Integer dataId)
        throws SQLException {
        String query =
                "SELECT a.value "
                        + " from attribute a, data_attribute da "
                        + " where a.attribute_id=da.attribute_id "
                        + " and da.data_id=" + dataId
                        + " and a.heading='official name'";
        return doQuery(connection, query);
    }

    /**
     * Fetch a cvterm by id and cache results in cvtermCache.  Returns null if the cv terms isn't
     * found.
     * @param connection to chado database
     * @param cvtermId internal chado id for a cvterm
     * @return the cvterm name or null if not found
     * @throws SQLException if database access problem
     */
    private String getCvterm(Connection connection, String cvtermId) throws SQLException {
        String cvTerm = cvtermCache.get(cvtermId);
        if (cvTerm == null) {
            String query =
                    "SELECT c.name "
                            + " from cvterm c"
                            + " where c.cvterm_id=" + cvtermId;
            Statement stmt = connection.createStatement();
            ResultSet res = stmt.executeQuery(query);
            while (res.next()) {
                cvTerm = res.getString("name");
            }
            cvtermCache.put(cvtermId, cvTerm);
        }
        return cvTerm;
    }

    /**
     * =====================
     *    DATA ATTRIBUTES
     * =====================
     */
    private void processAppliedDataAttributesNEW(Connection connection)
        throws SQLException, ObjectStoreException {
        // attempts to collate attributes
        // TODO check!
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        ResultSet res = getAppliedDataAttributes(connection);
        int count = 0;
        Integer previousDataId = 0;
        String previousName = null;
        String value = null;
        String type = null;
        while (res.next()) {
            Integer dataId = new Integer(res.getInt("data_id"));
            // check if not belonging to a deleted sub
            // better way?
            Integer submissionId = dataSubmissionMap.get(dataId);
            if (submissionId == null || deletedSubMap.containsKey(submissionId)) {
                continue;
            }
            String name  = res.getString("heading");

//            LOG.info("DA " + dataId + ": " + name);

            if (previousDataId == 0) { //first pass
                value = res.getString("value");
                type  = res.getString("name");
                previousDataId = dataId;
                previousName = name;
                LOG.info("DA0 " + dataId + ": " + name + "|" + value);
                continue;
            }

            if (dataId > previousDataId) {
                Item dataAttribute = storeDataAttribute(value, type, previousDataId, previousName);
                value = res.getString("value");
                type  = res.getString("name");
                count++;
                previousDataId = dataId;
                previousName = name;
                LOG.info("DA1 new: " + previousDataId + ": " + previousName + "|" + value);
                continue;
            }

            if (!name.equalsIgnoreCase(previousName)) {
                Item dataAttribute = storeDataAttribute(value, type, dataId, previousName);
//                LOG.info("DA2 store: " + dataId + ": " + previousName + "|" + value);
                count ++;
                value = res.getString("value");
                previousName = name;
                LOG.info("DA2 new: " + dataId + ": " + previousName + "|" + value);
            } else {
                value = value + ", " + res.getString("value");
            }
            type  = res.getString("name");

            previousDataId = dataId;
            if (res.isLast()) {
                Item dataAttribute = storeDataAttribute(value, type, dataId, name);
                count++;
            }
        }
        LOG.info("created " + count + " data attributes");
        res.close();
        LOG.info("PROCESS TIME data attributes: " + (System.currentTimeMillis() - bT) + " ms");
    }

    /**
     * @param value
     * @param type
     * @param dataId
     * @param name
     * @return
     * @throws ObjectStoreException
     */
    private Item storeDataAttribute(String value, String type, Integer dataId,
            String name) throws ObjectStoreException {
        Item dataAttribute = getChadoDBConverter().createItem("SubmissionDataAttribute");
        if (name != null && !"".equals(name)) {
            dataAttribute.setAttribute("name", name);
        }
        if (!StringUtils.isEmpty(value)) {
            dataAttribute.setAttribute("value", value);
        }
        if (!StringUtils.isEmpty(type)) {
            dataAttribute.setAttribute("type", type);
        }
        // setting references to SubmissionData
        dataAttribute.setReference("submissionData", appliedDataMap.get(dataId).itemIdentifier);
        getChadoDBConverter().store(dataAttribute);
        return dataAttribute;
    }

    private void processAppliedDataAttributes(Connection connection)
        throws SQLException, ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        ResultSet res = getAppliedDataAttributes(connection);
        int count = 0;
        while (res.next()) {
            Integer dataId = new Integer(res.getInt("data_id"));

            // check if not belonging to a deleted sub
            // better way?
            Integer submissionId = dataSubmissionMap.get(dataId);
            if (submissionId == null || deletedSubMap.containsKey(submissionId)) {
                continue;
            }
            String name  = res.getString("heading");
            String value = res.getString("value");
            String type  = res.getString("name");
            Item dataAttribute = storeDataAttribute(value, type, dataId, name);
            count++;
        }
        LOG.info("created " + count + " data attributes");
        res.close();
        LOG.info("PROCESS TIME data attributes: " + (System.currentTimeMillis() - bT) + " ms");
    }

    // first value in the list of synonyms is the 'preferred' value
    private static String[][] synonyms = new String[][]{
        new String[] {"developmental stage", "stage", "developmental_stage", "dev stage",
            "devstage"},
        new String[] {"strain", "strain_or_line"},
        new String[] {"cell line", "cell_line", "Cell line", "cell id"},
        new String[] {"array", "adf"},
        new String[] {"compound", "Compound"},
        new String[] {"incubation time", "Incubation Time"},
        new String[] {"RNAi reagent", "RNAi_reagent", "dsRNA"},
        new String[] {"temperature", "temp"}
    };

    private static List<String> makeLookupList(String initialLookup) {
        for (String[] synonymType : synonyms) {
            for (String synonym : synonymType) {
                if (synonym.equals(initialLookup)) {
                    return Arrays.asList(synonymType);
                }
            }
        }
        return new ArrayList<String>(Collections.singleton(initialLookup));
    }

    private static String getPreferredSynonym(String initialLookup) {
        return makeLookupList(initialLookup).get(0);
    }

    private static Set<String> unifyFactorNames(Collection<String> original) {
        Set<String> unified = new HashSet<String>();
        for (String name : original) {
            unified.add(getPreferredSynonym(name));
        }
        return unified;
    }

    private class SubmissionReference
    {
        public SubmissionReference(Integer referencedSubmissionId, String dataValue) {
            this.referencedSubmissionId = referencedSubmissionId;
            this.dataValue = dataValue;
        }
        private Integer referencedSubmissionId;
        private String dataValue;
    }

    // process new query
    // get DCC id
    // add antibody to types
    private void processSubmissionProperties(Connection connection) throws SQLException,
    IOException, ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        ResultSet res = getAppliedDataAll(connection);
        final String comma = ",";
        String reportName = "build/"
                + getChadoDBConverter().getDatabase().getName() + "_subs_report.csv";
        File f = new File(reportName);
        FileWriter writer = new FileWriter(f);
        writeHeader(comma, writer);

        SubmissionProperty buildSubProperty = null;
        Integer lastDataId = new Integer(-1);
        Map<String, SubmissionProperty> props = new HashMap<String, SubmissionProperty>();

        Map<Integer, Map<String, List<SubmissionProperty>>> subToTypes =
                new HashMap<Integer, Map<String, List<SubmissionProperty>>>();

        submissionRefs = new HashMap<Integer, List<SubmissionReference>>();

        while (res.next()) {
            Integer dataId = new Integer(res.getInt("data_id"));
            String dataHeading  = res.getString("data_heading");
            String dataName = res.getString("data_name");
            String wikiPageUrl  = res.getString("data_value");
            String cvTerm = res.getString("cv_term");
            String attHeading = res.getString("att_heading");
            String attName = res.getString("att_name");
            String attValue = res.getString("att_value");
            String attDbxref = res.getString("att_dbxref");
            int attRank = res.getInt("att_rank");

            Integer submissionId = dataSubmissionMap.get(dataId);
            String dccId = dccIdMap.get(submissionId);

            writer.write(dccId + comma + dataHeading + comma + dataName + comma
                    + wikiPageUrl + comma + cvTerm + comma + attHeading + comma + attName
                    + comma + attValue + comma + attDbxref + System.getProperty("line.separator"));

            if (submissionId == null) {
                LOG.warn("Failed to find a submission id for data id " + dataId + " - this probably"
                        + " means there is a problem with the applied_protocol DAG strucuture.");
                continue;
            }

            // Currently using attValue for referenced submission DCC id, should be dbUrl but seems
            // to be filled in incorrectly
            if (attHeading != null && attHeading.startsWith("modENCODE Reference")) {
                attValue = checkRefSub(wikiPageUrl, attValue, submissionId, dccId);
                LOG.info("EEFF " + attValue + "|" + wikiPageUrl);
            }

            // we are starting a new data row
            if (dataId.intValue() != lastDataId.intValue()) {
                // have we seen this modencodewiki entry before?
                if (props.containsKey(wikiPageUrl)) {
                    buildSubProperty = null;
                } else {
                    buildSubProperty =
                            new SubmissionProperty(getPreferredSynonym(dataName), wikiPageUrl);
                    props.put(wikiPageUrl, buildSubProperty);
                }
                // submissionId -> [type -> SubmissionProperty]
                addToSubToTypes(subToTypes, submissionId, props.get(wikiPageUrl));
            }
            if (buildSubProperty != null) {
                // we are building a new submission attribute, this is the first time we have
                // seen a data.value that points to modencodewiki
                buildSubProperty.addDetail(attHeading, attValue, attRank);
            }
            lastDataId = dataId;
        }
        writer.flush();
        writer.close();

        // Characteristics are modelled differently to protocol inputs/outputs, read in extra
        // properties here
        addSubmissionPropsFromCharacteristics(subToTypes, connection);

        // some submissions use reagents created in reference submissions, find the properties
        // of the reagents and add to referencing submission
        addSubmissionPropsFromReferencedSubmissions(subToTypes, props, submissionRefs);

        // create and store properties of submission
        storeSubProperties(subToTypes);
        LOG.info("PROCESS TIME submission properties: "
                + (System.currentTimeMillis() - bT) + " ms");
    }

    /**
     * @param subToTypes
     * @throws ObjectStoreException
     */
    private void storeSubProperties(
            Map<Integer, Map<String, List<SubmissionProperty>>> subToTypes)
        throws ObjectStoreException {
        for (Integer submissionId : subToTypes.keySet()) {
            Integer storedSubmissionId = submissionMap.get(submissionId).interMineObjectId;

            if (deletedSubMap.containsKey(submissionId)) {
                continue;
            }

            Map<String, List<SubmissionProperty>> typeToProp = subToTypes.get(submissionId);
            String dccId = dccIdMap.get(submissionId);

            ExperimentalFactor ef = submissionEFMap.get(submissionId);
            if (ef == null) {
                LOG.warn("No experimental factors found for submission: " + dccId);
                continue;
            }
            Set<String> exFactorNames = unifyFactorNames(ef.efNames);
            LOG.info("PROPERTIES " + dccId + " typeToProp keys: " + typeToProp.keySet());
            List<Item> allPropertyItems = new ArrayList<Item>();

            // DEVELOPMENTAL STAGE
            List<Item> devStageItems = new ArrayList<Item>();
            devStageItems.addAll(createFromWikiPage(dccId, "DevelopmentalStage", typeToProp,
                    makeLookupList("developmental stage")));
            if (devStageItems.isEmpty()) {
                devStageItems.addAll(lookForAttributesInOtherWikiPages(dccId, "DevelopmentalStage",
                        typeToProp, new String[] {
                            "developmental stage.developmental stage",
                            "tissue.developmental stage",
                            "tissue source.developmental stage",
                            "cell line.developmental stage",
                            "cell id.developmental stage"
                            //, "RNA extract.Cell Type"
                        }));
            }

            if (devStageItems.isEmpty()) {
                addNotApplicable(devStageItems, "DevelopmentalStage", "developmental stage");
            }
            storeSubmissionCollection(storedSubmissionId, "developmentalStages", devStageItems);
            if (!devStageItems.isEmpty() && exFactorNames.contains("developmental stage")) {
                createExperimentalFactors(submissionId, "developmental stage", devStageItems);
                exFactorNames.remove("developmental stage");
            }
            allPropertyItems.addAll(devStageItems);

            // STRAIN
            List<Item> strainItems = new ArrayList<Item>();
            strainItems.addAll(createFromWikiPage(
                    dccId, "Strain", typeToProp, makeLookupList("strain")));
            if (strainItems.isEmpty()) {
                addNotApplicable(strainItems, "Strain", "strain");
            }
            storeSubmissionCollection(storedSubmissionId, "strains", strainItems);
            //            if (!strainItems.isEmpty() && exFactorNames.contains("strain")) {
            if (exFactorNames.contains("strain")) {
                createExperimentalFactors(submissionId, "strain", strainItems);
                exFactorNames.remove("strain");
            }
            allPropertyItems.addAll(strainItems);

            // ARRAY
            List<Item> arrayItems = new ArrayList<Item>();
            arrayItems.addAll(createFromWikiPage(
                    dccId, "Array", typeToProp, makeLookupList("array")));
            LOG.debug("ARRAY: " + typeToProp.get("array"));
            if (arrayItems.isEmpty()) {
                arrayItems.addAll(lookForAttributesInOtherWikiPages(dccId, "Array",
                        typeToProp, new String[] {"adf.official name"}));
                if (!arrayItems.isEmpty()) {
                    LOG.debug("Attribute found in other wiki pages: " + dccId + " ARRAY ");
                }
            }
            if (arrayItems.isEmpty()) {
                addNotApplicable(arrayItems, "Array", "array");
            }
            storeSubmissionCollection(storedSubmissionId, "arrays", arrayItems);
            //            if (!arrayItems.isEmpty() && exFactorNames.contains("array")) {
            if (exFactorNames.contains("array")) {
                createExperimentalFactors(submissionId, "array", arrayItems);
                exFactorNames.remove("array");
            }
            allPropertyItems.addAll(arrayItems);

            // CELL LINE
            List<Item> lineItems = new ArrayList<Item>();
            lineItems.addAll(createFromWikiPage(dccId, "CellLine", typeToProp,
                    makeLookupList("cell line")));
            if (lineItems.isEmpty()) {
                addNotApplicable(lineItems, "CellLine", "cell line");
            }
            storeSubmissionCollection(storedSubmissionId, "cellLines", lineItems);
            // if (!lineItems.isEmpty() && exFactorNames.contains("cell line")) {
            if (exFactorNames.contains("cell line")) {
                createExperimentalFactors(submissionId, "cell line", lineItems);
                exFactorNames.remove("cell line");
            }
            allPropertyItems.addAll(lineItems);

            // RNAi REAGENT
            List<Item> reagentItems = new ArrayList<Item>();
            reagentItems.addAll(createFromWikiPage(dccId, "SubmissionProperty", typeToProp,
                    makeLookupList("dsRNA")));
            if (!reagentItems.isEmpty() && exFactorNames.contains("RNAi reagent")) {
                createExperimentalFactors(submissionId, "RNAi reagent", reagentItems);
                exFactorNames.remove("RNAi reagent");
            }
            allPropertyItems.addAll(reagentItems);

            // ANTIBODY
            List<Item> antibodyItems = new ArrayList<Item>();
            antibodyItems.addAll(createFromWikiPage(dccId, "Antibody", typeToProp,
                    makeLookupList("antibody")));
            if (antibodyItems.isEmpty()) {
                LOG.debug("ANTIBODY: " + typeToProp.get("antibody"));
                antibodyItems.addAll(lookForAttributesInOtherWikiPages(dccId, "Antibody",
                        typeToProp, new String[] {"antibody.official name"}));
                if (!antibodyItems.isEmpty()) {
                    LOG.debug("Attribute found in other wiki pages: " + dccId + " ANTIBODY ");
                }
            }
            if (antibodyItems.isEmpty()) {
                addNotApplicable(antibodyItems, "Antibody", "antibody");
            }
            storeSubmissionCollection(storedSubmissionId, "antibodies", antibodyItems);
            //            if (!antibodyItems.isEmpty() && exFactorNames.contains("antibody")) {
            if (exFactorNames.contains("antibody")) {
                createExperimentalFactors(submissionId, "antibody", antibodyItems);
                exFactorNames.remove("antibody");
            }
            allPropertyItems.addAll(antibodyItems);

            // TISSUE
            List<Item> tissueItems = new ArrayList<Item>();
            tissueItems.addAll(createFromWikiPage(
                    dccId, "Tissue", typeToProp, makeLookupList("tissue")));
            if (tissueItems.isEmpty()) {
                tissueItems.addAll(lookForAttributesInOtherWikiPages(dccId, "Tissue",
                        typeToProp, new String[] {"stage.tissue"
                        , "cell line.tissue"
                        , "cell id.tissue"}));
                if (!tissueItems.isEmpty()) {
                    LOG.info("Attribute found in other wiki pages: " + dccId + " TISSUE");
                }
            }
            if (tissueItems.isEmpty()) {
                addNotApplicable(tissueItems, "Tissue", "tissue");
            }
            storeSubmissionCollection(storedSubmissionId, "tissues", tissueItems);
            //            if (!tissueItems.isEmpty() && exFactorNames.contains("tissue")) {
            if (exFactorNames.contains("tissue")) {
                createExperimentalFactors(submissionId, "tissue", tissueItems);
                exFactorNames.remove("tissue");
            }
            allPropertyItems.addAll(tissueItems);

            // There may be some other experimental factors that require SubmissionProperty objects
            // but don't fall into the categories above.  Create them here and set experimental
            // factors.
            ArrayList<String> extraPropNames = new ArrayList<String>(exFactorNames);
            for (String exFactor : extraPropNames) {
                List<Item> extraPropItems = new ArrayList<Item>();
                extraPropItems.addAll(lookForAttributesInOtherWikiPages(dccId, "SubmissionProperty",
                        typeToProp, new String[] {exFactor}));
                allPropertyItems.addAll(extraPropItems);
                createExperimentalFactors(submissionId, exFactor, extraPropItems);
                exFactorNames.remove(exFactor);
            }

            // deal with remaining factor names (e.g. the ones for which we did
            // not find a corresponding attribute
            for (String exFactor : exFactorNames) {
                String type = ef.efTypes.get(exFactor);
                createEFItem(submissionId, type, exFactor, null);
            }

            storeSubmissionCollection(storedSubmissionId, "properties", allPropertyItems);
        }
    }

    /**
     * @param wikiPageUrl
     * @param attValue
     * @param submissionId
     * @param dccId
     * @return
     */
    private String checkRefSub(String wikiPageUrl, String attValue,
            Integer submissionId, String dccId) {
        if (attValue.indexOf(":") > 0) {
            attValue = attValue.substring(0, attValue.indexOf(":"));
        }
        attValue = DCC_PREFIX + attValue;
        Integer referencedSubId = getSubmissionIdFromDccId(attValue);
        if (referencedSubId != null) {
            SubmissionReference subRef =
                    new SubmissionReference(referencedSubId, wikiPageUrl);
            Util.addToListMap(submissionRefs, submissionId, subRef);
            LOG.info("Submission " + dccId + " (" + submissionId + ") has reference to "
                    + attValue + " (" + referencedSubId + ")");
        } else {
            LOG.warn("Could not find submission " + attValue + " referenced by " + dccId);
        }
        return attValue;
    }

    /**
     * @param comma
     * @param writer
     * @throws IOException
     */
    private void writeHeader(final String comma, FileWriter writer)
        throws IOException {
        writer.write("submission" + comma);
        writer.write("data_heading" + comma);
        writer.write("data_name" + comma);
        writer.write("data_value" + comma);
        writer.write("cv_term" + comma);
        writer.write("att_heading" + comma);
        writer.write("att_name" + comma);
        writer.write("att_value" + comma);
        writer.write(System.getProperty("line.separator"));
    }

    //    /**
    //     * @param submissionId
    //     * @param exFactorNames
    //     * @param devStageItems
    //     * @param clsName
    //     * @param propName
    //     * @throws ObjectStoreException
    //     */
    //    private void finishProp(Integer submissionId, Integer storedSubmissionId,
    //            Set<String> exFactorNames,
    //            List<Item> devStageItems, String clsName, String propName)
    //            throws ObjectStoreException {
    //        if (devStageItems.isEmpty()) {
    //            addNotApplicable(devStageItems, clsName, propName);
    //  storeSubmissionCollection(storedSubmissionId, "developmentalStages", devStageItems);
    //        } else {
    //            if (exFactorNames.contains(propName)) {
    //                createExperimentalFactors(submissionId, propName, devStageItems);
    //                exFactorNames.remove(propName);
    //            }
    //        }
    //    }

    /**
     * @param devStageItems
     * @param qq
     * @throws ObjectStoreException
     */
    //    private void addNotApplicable(List<Item> items, String propName) {
    //        Item subProperty = getChadoDBConverter().createItem("SubmissionProperty");
    //        subProperty.setAttribute("type", propName);
    //        subProperty.setAttribute("name", NA_PROP);
    //        items.add(subProperty);
    //    }

    private void addNotApplicable(List<Item> items, String clsName, String propName)
        throws ObjectStoreException {
        Item subProperty = getChadoDBConverter().createItem(clsName);
        subProperty.setAttribute("type", propName);
        subProperty.setAttribute("name", NA_PROP);
        items.add(subProperty);
        getChadoDBConverter().store(subProperty);
    }

    // Traverse DAG following previous applied protocol links to build a list of all AppliedData
    private void findAppliedProtocolsAndDataFromEarlierInDag(Integer startDataId,
            List<AppliedData> foundAppliedData, List<AppliedProtocol> foundAppliedProtocols) {
        AppliedData aData = appliedDataMap.get(startDataId);

        LOG.info("EEFF earlierinDAG: " + startDataId + "|" + aData.actualValue + " ->pap: "
                + aData.previousAppliedProtocols);

        if (foundAppliedData != null) {
            foundAppliedData.add(aData);
        }

        for (Integer previousAppliedProtocolId : aData.previousAppliedProtocols) {
            AppliedProtocol ap = appliedProtocolMap.get(previousAppliedProtocolId);
            LOG.info("EEFF earlierinDAG get proto " + previousAppliedProtocolId + ": in "
                    + ap.inputs + "| out " + ap.outputs);
            if (foundAppliedProtocols != null) {
                foundAppliedProtocols.add(ap);
            }
            for (Integer previousDataId : ap.inputs) {
                LOG.info("EEFF earlierinDAG get proto2 " + ap.inputs + "|" + ap.outputs);
                findAppliedProtocolsAndDataFromEarlierInDag(previousDataId, foundAppliedData,
                        foundAppliedProtocols);
            }
        }
    }


    private void createExperimentalFactors(Integer submissionId, String type,
            Collection<Item> items) throws ObjectStoreException {
        for (Item item : items) {
            createEFItem(submissionId, type, item.getAttribute("name").getValue(),
                    item.getIdentifier());
        }
    }


    private void createEFItem(Integer current, String type,
            String efName, String propertyIdentifier) throws ObjectStoreException {
        // create the EF, if not there already
        if (!eFactorIdMap.containsKey(efName)) {
            Item ef = getChadoDBConverter().createItem("ExperimentalFactor");
            String preferredType = getPreferredSynonym(type);
            ef.setAttribute ("type", preferredType);
            ef.setAttribute ("name", efName);
            if (propertyIdentifier != null) {
                ef.setReference("property", propertyIdentifier);
            }
            LOG.debug("ExFactor created for sub " + current + ":" + efName + "|" + type);

            Integer intermineObjectId = getChadoDBConverter().store(ef);
            eFactorIdMap.put(efName, intermineObjectId);
            eFactorIdRefMap.put(efName, ef.getIdentifier());
        }
        // if pertinent to the current sub, add to the map for the references
        Util.addToListMap(submissionEFactorMap, current, efName);
    }


    private void addToSubToTypes(Map<Integer, Map<String, List<SubmissionProperty>>> subToTypes,
            Integer submissionId, SubmissionProperty prop) {
        // submissionId -> [type -> SubmissionProperty]
        if (submissionId == null) {
            throw new RuntimeException("Called addToSubToTypes with a null sub id!");
        }

        Map<String, List<SubmissionProperty>> typeToSubProp = subToTypes.get(submissionId);
        if (typeToSubProp == null) {
            typeToSubProp = new HashMap<String, List<SubmissionProperty>>();
            subToTypes.put(submissionId, typeToSubProp);
        }
        List<SubmissionProperty> subProps = typeToSubProp.get(prop.type);
        if (subProps == null) {
            subProps = new ArrayList<SubmissionProperty>();
            typeToSubProp.put(prop.type, subProps);
        }
        subProps.add(prop);
    }


    private void addSubmissionPropsFromCharacteristics(
            Map<Integer, Map<String, List<SubmissionProperty>>> subToTypes,
            Connection connection)
        throws SQLException {

        ResultSet res = getAppliedDataCharacteristics(connection);

        Integer lastAttDbXref = new Integer(-1);
        Integer lastDataId = new Integer(-1);

        Map<Integer, SubmissionProperty> createdProps = new HashMap<Integer, SubmissionProperty>();
        SubmissionProperty buildSubProperty = null;
        boolean isValidCharacteristic = false;
        Integer currentSubId = null;    // we need those to attach the property to the correct sub
        Integer previousSubId = null;

        while (res.next()) {
            Integer dataId = new Integer(res.getInt("data_id"));
            String attHeading = res.getString("att_heading");
            String attName = res.getString("att_name");
            String attValue = res.getString("att_value");
            Integer attDbxref = new Integer(res.getInt("att_dbxref"));
            int attRank = res.getInt("att_rank");

            currentSubId = dataSubmissionMap.get(dataId);

            if (dataId.intValue() != lastDataId.intValue()
                    || attDbxref.intValue() != lastAttDbXref.intValue()
                    || currentSubId != previousSubId) {
                // store the last build property if created, type is set only if we found an
                // attHeading of Characteristics
                // note: dbxref can remain the same in different subs -> or
                if (buildSubProperty != null && buildSubProperty.type != null) {
                    createdProps.put(lastAttDbXref, buildSubProperty);
                    addToSubToTypes(subToTypes, previousSubId, buildSubProperty);
                }
                // set up for next attDbxref
                if (createdProps.containsKey(attDbxref) && isValidCharacteristic) {
                    // seen this property before so just add for this submission, don't build again
                    buildSubProperty = null;
                    isValidCharacteristic = false;
                    addToSubToTypes(subToTypes, currentSubId, createdProps.get(attDbxref));
                } else {
                    buildSubProperty = new SubmissionProperty();
                    isValidCharacteristic = false;
                }
            }

            if (attHeading.startsWith("Characteristic")) {
                isValidCharacteristic = true;
            }

            if (buildSubProperty != null) {
                if (attHeading.startsWith("Characteristic")) {
                    buildSubProperty.type = getPreferredSynonym(attName);
                    buildSubProperty.wikiPageUrl = attValue;
                    // add detail here as some Characteristics don't reference a wiki page
                    // but have all information on single row
                    buildSubProperty.addDetail(attName, attValue, attRank);
                } else {
                    buildSubProperty.addDetail(attHeading, attValue, attRank);
                }
            }
            previousSubId = currentSubId;
            lastAttDbXref = attDbxref;
            lastDataId = dataId;
        }

        if (buildSubProperty != null && buildSubProperty.type != null) {
            createdProps.put(lastAttDbXref, buildSubProperty);
            addToSubToTypes(subToTypes, currentSubId, buildSubProperty);
        }
    }

    // Some submission mention e.g. an RNA Sample but the details of how that sample was created,
    // stage, strain, etc are in a previous submission.  There are references to previous submission
    // DCC ids where a sample with the corresponding name can be found.  We then need to traverse
    // backwards along the AppliedProtocol DAG to find the stage, strain, etc wiki pages.  These
    // should already have been processed so the properties can just be added to the referencing
    // submission.
    private void addSubmissionPropsFromReferencedSubmissions(
            Map<Integer, Map<String, List<SubmissionProperty>>> subToTypes,
            Map<String, SubmissionProperty> props,
            Map<Integer, List<SubmissionReference>> submissionRefs) {


        for (Map.Entry<Integer, List<SubmissionReference>> entry : submissionRefs.entrySet()) {
            Integer submissionId = entry.getKey();
            List<SubmissionReference> lref = entry.getValue();

            Iterator<SubmissionReference> i = lref.iterator();
            while (i.hasNext()) {
                SubmissionReference ref = i.next();
                List<AppliedData> refAppliedData = findAppliedDataFromReferencedSubmission(ref);
                for (AppliedData aData : refAppliedData) {
                    String possibleWikiUrl = aData.actualValue;
                    if (possibleWikiUrl != null && props.containsKey(possibleWikiUrl)) {
                        LOG.info("EEFF possible wikiurl: " + possibleWikiUrl);
                        SubmissionProperty propFromReferencedSub = props.get(possibleWikiUrl);
                        if (propFromReferencedSub != null) { //??
                            addToSubToTypes(subToTypes, submissionId, propFromReferencedSub);
                            LOG.info("EEFF from referenced sub: " + propFromReferencedSub.details);
                        }
                    }
                }
            }
        }
    }

    private List<AppliedData> findAppliedDataFromReferencedSubmission(SubmissionReference subRef) {
        List<AppliedData> foundAppliedData = new ArrayList<AppliedData>();
        findAppliedProtocolsAndDataFromReferencedSubmission(subRef, foundAppliedData, null);
        return foundAppliedData;
    }

    private List<AppliedProtocol> findAppliedProtocolsFromReferencedSubmission(
            SubmissionReference subRef) {
        List<AppliedProtocol> foundAppliedProtocols = new ArrayList<AppliedProtocol>();
        findAppliedProtocolsAndDataFromReferencedSubmission(subRef, null, foundAppliedProtocols);
        return foundAppliedProtocols;
    }

    private void findAppliedProtocolsAndDataFromReferencedSubmission(
            SubmissionReference subRef,
            List<AppliedData> foundAppliedData,
            List<AppliedProtocol> foundAppliedProtocols) {
        String refDataValue = subRef.dataValue;
        Integer refSubId = subRef.referencedSubmissionId;

        for (AppliedData aData : appliedDataMap.values()) {
            String currentDataValue = aData.value;
            Integer currentDataSubId = dataSubmissionMap.get(aData.dataId);
            // added check that referenced and referring are not the same.
            // TODO check
            if (refDataValue.equals(currentDataValue)
                    && refSubId.equals(currentDataSubId)) {
                LOG.info("Found a matching data value: " + currentDataValue + " in sub "
                        + dccIdMap.get(currentDataSubId) + " for referenced sub "
                        + dccIdMap.get(refSubId));

//                Integer refDataId = getDataFromRefSub(currentDataValue,refSubId);
                Integer foundDataId = aData.dataId;

                LOG.info("EEFF referenced data_id: " + foundDataId + "|" + aData.actualValue);
                findAppliedProtocolsAndDataFromEarlierInDag(foundDataId, foundAppliedData,
                        foundAppliedProtocols);
            }
        }
    }

    private List<Item> createFromWikiPage(String dccId, String clsName,
            Map<String, List<SubmissionProperty>> typeToProp, List<String> types)
        throws ObjectStoreException {
        List<Item> items = new ArrayList<Item>();

        List<SubmissionProperty> props = new ArrayList<SubmissionProperty>();
        for (String type : types) {
            if (typeToProp.containsKey(type)) {
                props.addAll(typeToProp.get(type));
            }
        }
        items.addAll(createItemsForSubmissionProperties(dccId, clsName, props));
        return items;
    }

    private void storeSubmissionCollection(Integer storedSubmissionId, String name,
            List<Item> items)
        throws ObjectStoreException {
        if (!items.isEmpty()) {
            ReferenceList refList = new ReferenceList(name, getIdentifiersFromItems(items));
            getChadoDBConverter().store(refList, storedSubmissionId);
        }
    }

    private List<String> getIdentifiersFromItems(Collection<Item> items) {
        List<String> ids = new ArrayList<String>();
        for (Item item : items) {
            ids.add(item.getIdentifier());
        }
        return ids;
    }

    private List<Item> createItemsForSubmissionProperties(String dccId, String clsName,
            List<SubmissionProperty> subProps)
        throws ObjectStoreException {
        List<Item> items = new ArrayList<Item>();
        for (SubmissionProperty subProp : subProps) {
            Item item = getItemForSubmissionProperty(clsName, subProp, dccId);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private Item getItemForSubmissionProperty(String clsName, SubmissionProperty prop, String dccId)
        throws ObjectStoreException {
        Item propItem = subItemsMap.get(prop.wikiPageUrl);
        if (propItem == null) {

            if (clsName != null) {
                List<String> checkOfficialName = prop.details.get("official name");

                if (checkOfficialName == null) {
                    LOG.warn("No 'official name', using 'name' instead for: " + prop.wikiPageUrl);
                    checkOfficialName = prop.details.get("name");
                }
                if (checkOfficialName == null) {
                    LOG.info("Official name - missing for property: "
                            + prop.type + ", " + prop.wikiPageUrl);
                    return null;
                } else if (checkOfficialName.size() != 1) {
                    LOG.info("Official name - multiple times for property: "
                            + prop.type + ", " + prop.wikiPageUrl + ", "
                            + checkOfficialName);
                }

                String officialName = getCorrectedOfficialName(prop);
                propItem = createSubmissionProperty(clsName, officialName);
                propItem.setAttribute("type", getPreferredSynonym(prop.type));
                propItem.setAttribute("wikiLink", WIKI_URL + prop.wikiPageUrl);
                if ("DevelopmentalStage".equals(clsName)) {
                    setAttributeOnProp(prop, propItem, "sex", "sex");

                    List<String> devStageValues = prop.details.get("developmental stage");
                    if (devStageValues != null) {
                        for (String devStageValue : devStageValues) {
                            propItem.addToCollection("ontologyTerms",
                                    getDevStageTerm(devStageValue, dccId));
                        }
                    } else {
                        LOG.error("METADATA FAIL: no 'developmental stage' values for wiki page: "
                                + prop.wikiPageUrl);
                    }
                } else if ("Antibody".equals(clsName)) {
                    setAttributeOnProp(prop, propItem, "antigen", "antigen");
                    setAttributeOnProp(prop, propItem, "host", "hostOrganism");
                    setAttributeOnProp(prop, propItem, "target name", "targetName");
                    setGeneItem(dccId, prop, propItem, "Antibody");
                } else if ("Array".equals(clsName)) {
                    setAttributeOnProp(prop, propItem, "platform", "platform");
                    setAttributeOnProp(prop, propItem, "resolution", "resolution");
                    setAttributeOnProp(prop, propItem, "genome", "genome");
                } else if ("CellLine".equals(clsName)) {
                    setAttributeOnProp(prop, propItem, "sex", "sex");
                    setAttributeOnProp(prop, propItem, "short description", "description");
                    setAttributeOnProp(prop, propItem, "species", "species");
                    setAttributeOnProp(prop, propItem, "tissue", "tissue");
                    setAttributeOnProp(prop, propItem, "cell type", "cellType");
                    setAttributeOnProp(prop, propItem, "target name", "targetName");
                    setGeneItem(dccId, prop, propItem, "CellLine");
                } else if ("Strain".equals(clsName)) {
                    setAttributeOnProp(prop, propItem, "species", "species");
                    setAttributeOnProp(prop, propItem, "source", "source");
                    // the following 2 should be mutually exclusive
                    setAttributeOnProp(prop, propItem, "Description", "description");
                    setAttributeOnProp(prop, propItem, "details", "description");
                    setAttributeOnProp(prop, propItem, "aliases", "name");
                    setAttributeOnProp(prop, propItem, "reference", "reference");
                    setAttributeOnProp(prop, propItem, "target name", "targetName");
                    setGeneItem(dccId, prop, propItem, "Strain");
                }  else if ("Tissue".equals(clsName)) {
                    setAttributeOnProp(prop, propItem, "species", "species");
                    setAttributeOnProp(prop, propItem, "sex", "sex");
                    setAttributeOnProp(prop, propItem, "organismPart", "organismPart");
                }
                getChadoDBConverter().store(propItem);
            }
            subItemsMap.put(prop.wikiPageUrl, propItem);
        }
        return propItem;
    }

    private void setGeneItem(String dccId, SubmissionProperty prop, Item propItem, String source)
        throws ObjectStoreException {
        String targetText = null;
        String[] possibleTypes = new String[] {"target id"};
        boolean tooMany = false;
        for (String targetType : possibleTypes) {
            if (prop.details.containsKey(targetType)) {
                if (prop.details.get(targetType).size() != 1) {

                    // we used to complain if multiple values, now only
                    // if they don't have the same value
                    if (sameTargetValue(prop, source, targetType)) {
                        tooMany = true;
                        break;
                    }
                }
                if (!tooMany) {
                    targetText = prop.details.get(targetType).get(0);
                }
                break;
            }
        }
        if (targetText != null) {
            // if no target name was found use the target id
            if (!propItem.hasAttribute("targetName")) {
                propItem.setAttribute("targetName", targetText);
            }
            String geneItemId = getTargetGeneItemIdentfier(targetText, dccId);
            if (geneItemId != null) {
                propItem.setReference("target", geneItemId);
            }
        }
    }

    private boolean sameTargetValue(SubmissionProperty prop, String source,
            String targetType) {
        String value = prop.details.get(targetType).get(0);
        for (int i = 1; i < prop.details.get(targetType).size(); i++) {
            String newValue = prop.details.get(targetType).get(i);
            if (!newValue.equals(value)) {
                LOG.error(source + " (" + prop.wikiPageUrl + ") has more than 1 value for '"
                        + targetType + "' field: " + prop.details.get(targetType));
                //throw new RuntimeException(source + " should only have one value for '"
                //        + targetType + "' field: " + prop.details.get(targetType));
                return true;
            }
        }
        return false;
    }

    private void setAttributeOnProp(SubmissionProperty subProp, Item item, String metadataName,
            String attributeName) {

        if (subProp.details.containsKey(metadataName)) {
            if ("aliases".equalsIgnoreCase(metadataName)) {
                for (String s :subProp.details.get(metadataName)) {
                    if ("yellow cinnabar brown speck".equalsIgnoreCase(s)) {
                        // swapping name with fullName
                        String full = item.getAttribute("name").getValue();
                        item.setAttribute("fullName", full);

                        item.setAttribute(attributeName, s);
                        break;
                    }
                }
            } else if ("description".equalsIgnoreCase(metadataName)
                    || "details".equalsIgnoreCase(metadataName)) {
                // description is often split in more than 1 line, details should be correct order
                StringBuffer sb = new StringBuffer();
                for (String desc : subProp.details.get(metadataName)) {
                    sb.append(desc);
                }
                if (sb.length() > 0) {
                    item.setAttribute(attributeName, sb.toString());
                }
            } else {
                String value = subProp.details.get(metadataName).get(0);
                item.setAttribute(attributeName, value);
            }
        }
    }

    private String getTargetGeneItemIdentfier(String geneTargetIdText, String dccId)
        throws ObjectStoreException {
        // TODO check: why not using only the else?

        String taxonId = "";
        String originalId = null;

        String flyPrefix = "fly_genes:";
        String wormPrefix = "worm_genes:";

        if (geneTargetIdText.startsWith(flyPrefix)) {
            originalId = geneTargetIdText.substring(flyPrefix.length());
            taxonId = "7227";
        } else if (geneTargetIdText.startsWith(wormPrefix)) {
            originalId = geneTargetIdText.substring(wormPrefix.length());
            taxonId = "6239";
        } else {
            // attempt to work out the organism from the submission
            taxonId = getTaxonIdForSubmission(dccId);
            originalId = geneTargetIdText;
            LOG.debug("RESOLVER: found taxon " + taxonId + " for submission " + dccId);
        }

        IdResolver resolver = null;
        if ("7227".equals(taxonId)) {
            resolver = flyResolverFactory.getIdResolver();
        } else if ("6239".equals(taxonId)) {
            resolver = wormResolverFactory.getIdResolver();
        } else {
            LOG.info("RESOLVER: unable to work out organism for target id text: "
                    + geneTargetIdText + " in submission " + dccId);
        }

        String geneItemId = null;

        String primaryIdentifier = resolveGene(originalId, taxonId, resolver);
        if (primaryIdentifier != null) {
            geneItemId = geneToItemIdentifier.get(primaryIdentifier);
            if (geneItemId == null) {
                Item gene = getChadoDBConverter().createItem("Gene");
                geneItemId = gene.getIdentifier();
                gene.setAttribute("primaryIdentifier", primaryIdentifier);
                getChadoDBConverter().store(gene);
                geneToItemIdentifier.put(primaryIdentifier, geneItemId);
            } else {
                LOG.info("RESOLVER fetched gene from cache: " + primaryIdentifier);
            }
        }
        return geneItemId;
    }

    private String resolveGene(String originalId, String taxonId, IdResolver resolver) {
        String primaryIdentifier = null;
        int resCount = resolver.countResolutions(taxonId, originalId);
        if (resCount != 1) {
            LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring "
                    + "gene: " + originalId + " for organism " + taxonId + " count: " + resCount
                    + " found ids: " + resolver.resolveId(taxonId, originalId) + ".");
        } else {
            primaryIdentifier =
                    resolver.resolveId(taxonId, originalId).iterator().next();
            LOG.info("RESOLVER found gene " + primaryIdentifier
                    + " for original id: " + originalId);
        }
        return primaryIdentifier;
    }

    private List<Item> lookForAttributesInOtherWikiPages(String dccId, String clsName,
            Map<String, List<SubmissionProperty>> typeToProp, String[] lookFor)
        throws ObjectStoreException {

        List<Item> items = new ArrayList<Item>();
        for (String typeProp : lookFor) {
            if (typeProp.indexOf(".") > 0) {
                String[] bits = StringUtils.split(typeProp, '.');

                String type = bits[0];
                String propName = bits[1];

                if (typeToProp.containsKey(type)) {
                    for (SubmissionProperty subProp : typeToProp.get(type)) {
                        if (subProp.details.containsKey(propName)) {
                            for (String value : subProp.details.get(propName)) {
                                items.add(createNonWikiSubmissionPropertyItem(dccId, clsName,
                                        getPreferredSynonym(propName),
                                        correctAttrValue(value)));
                            }
                        }
                    }
                    if (!items.isEmpty()) {
                        break;
                    }
                }
            } else {
                // no attribute type given so use the data.value (SubmissionProperty.wikiPageUrl)
                // which probably won't be a wiki page
                if (typeToProp.containsKey(typeProp)) {
                    for (SubmissionProperty subProp : typeToProp.get(typeProp)) {

                        String value = subProp.wikiPageUrl;

                        // This is an ugly special case to deal with 'exposure time/24 hours'
                        if (subProp.details.containsKey("Unit")) {
                            String unit = subProp.details.get("Unit").get(0);
                            value = value + " " + unit + (unit.endsWith("s") ? "" : "s");
                        }

                        items.add(createNonWikiSubmissionPropertyItem(dccId, clsName, subProp.type,
                                correctAttrValue(value)));
                    }
                }
            }
        }
        return items;
    }


    private List<Item> lookForAttributesInOtherWikiPagesNEW(String dccId, String clsName,
            Map<String, List<SubmissionProperty>> typeToProp, String[] lookFor)
        throws ObjectStoreException {

        List<Item> items = new ArrayList<Item>();
        for (String typeProp : lookFor) {
            if (typeProp.indexOf(".") > 0) {
                String[] bits = StringUtils.split(typeProp, '.');

                String type = bits[0];
                String propName = bits[1];
                LOG.info("EEFFinotherW: " + type + "|" + propName);
                if (typeToProp.containsKey(type)) {
                    for (SubmissionProperty subProp : typeToProp.get(type)) {
                        LOG.info("EEFFinotherWo: " + subProp + "|" + propName);

                        if (subProp.details.containsKey(propName)) {
                            int count = 0;
                            int max = subProp.details.get(propName).size();
                            StringBuffer compoundValue = new StringBuffer();
                            for (String value : subProp.details.get(propName)) {
                                count++;
                                if (count > 1) {
                                    compoundValue.append(", ");
                                }
                                if (count < max) {
                                    compoundValue.append(value);
                                } else {
                                    LOG.info("EEFFinotherW " + max + ": " + count + "|" + value
                                            + "||" + compoundValue.toString());
                                    compoundValue.append(value);
                                    items.add(createNonWikiSubmissionPropertyItem(dccId, clsName,
                                            getPreferredSynonym(propName),
                                            correctAttrValue(compoundValue.toString())));
                                    compoundValue.setLength(0);
                                }
                            }
                        }
                    }
                    if (!items.isEmpty()) {
                        break;
                    }
                }
            } else {
                // no attribute type given so use the data.value (SubmissionProperty.wikiPageUrl)
                // which probably won't be a wiki page
                if (typeToProp.containsKey(typeProp)) {
                    LOG.info("EEFFinotherWelse: " + typeProp);
                    for (SubmissionProperty subProp : typeToProp.get(typeProp)) {

                        String value = subProp.wikiPageUrl;
                        LOG.info("EEFFinotherWelsefor: " + value);

                        // This is an ugly special case to deal with 'exposure time/24 hours'
                        if (subProp.details.containsKey("Unit")) {
                            String unit = subProp.details.get("Unit").get(0);
                            value = value + " " + unit + (unit.endsWith("s") ? "" : "s");
                        }

                        items.add(createNonWikiSubmissionPropertyItem(dccId, clsName, subProp.type,
                                correctAttrValue(value)));
                    }
                }
            }
        }
        return items;
    }



    private String correctAttrValue(String value) {
        if (value == null) {
            return null;
        }
        value = value.replace("–", "-");
        return value;
    }

    private Item createNonWikiSubmissionPropertyItem(String dccId, String clsName, String type,
            String name)
        throws ObjectStoreException {
        if ("DevelopmentalStage".equals(clsName)) {
            name = correctDevStageTerm(name);
        }

        Item item = nonWikiSubmissionProperties.get(name);
        if (item == null) {
            item = createSubmissionProperty(clsName, name);
            item.setAttribute("type", getPreferredSynonym(type));

            if ("DevelopmentalStage".equals(clsName)) {
                String ontologyTermId = getDevStageTerm(name, dccId);
                item.addToCollection("ontologyTerms", ontologyTermId);
            }
            getChadoDBConverter().store(item);

            nonWikiSubmissionProperties.put(name, item);
        }
        return item;
    }

    private Item createSubmissionProperty(String clsName, String name) {
        Item subProp = getChadoDBConverter().createItem(clsName);
        if (name != null) {
            subProp.setAttribute("name", name);
        }

        return subProp;
    }

    private String getCorrectedOfficialName(SubmissionProperty prop) {
        String preferredType = getPreferredSynonym(prop.type);
        String name = null;
        if (prop.details.containsKey("official name")) {
            name = prop.details.get("official name").get(0);
        } else if (prop.details.containsKey("name")) {
            name = prop.details.get("name").get(0);
        } else {
            // no official name so maybe there is a key that matches the type - sometimes the
            // setup for Characteristics
            for (String lookup : makeLookupList(prop.type)) {
                if (prop.details.containsKey(lookup)) {
                    name = prop.details.get(lookup).get(0);
                }
            }
        }
        return correctOfficialName(name, preferredType);
    }

    /**
     * Unify variations on similar official names.
     * @param name the original 'official name' value
     * @param type the treatment depends on the type
     * @return a unified official name
     */
    protected String correctOfficialName(String name, String type) {
        if (name == null) {
            return null;
        }

        if ("developmental stage".equals(type)) {
            name = name.replace("_", " ");
            name = name.replaceFirst("embryo", "Embryo");
            name = name.replaceFirst("Embyro", "Embryo");
            if (name.matches("E\\d.*")) {
                name = name.replaceFirst("^E", "Embryo ");
            }
            if (name.matches("Embryo.*\\d")) {
                name = name + " h";
            }
            if (name.matches(".*hr")) {
                name = name.replace("hr", "h");
            }
            if (name.matches("Embryo.*\\dh")) {
                name = name.replaceFirst("h", " h");
            }
            if (name.startsWith("DevStage:")) {
                name = name.replaceFirst("DevStage:", "").trim();
            }
            if (name.matches("L\\d")) {
                name = name + " stage larvae";
            }
            if (name.matches(".*L\\d")) {
                name = name + " stage larvae";
            }
            if (name.matches("WPP.*")) {
                name = name.replaceFirst("WPP", "White prepupae (WPP)");
            }
        }
        return name;
    }

    private String getDevStageTerm(String value, String dccId) throws ObjectStoreException {
        value = correctDevStageTerm(value);

        // there may be duplicate terms for fly and worm, include taxon in key
        String taxonId = getTaxonIdForSubmission(dccId);
        OrganismRepository or = OrganismRepository.getOrganismRepository();
        String genus = or.getOrganismDataByTaxon(Integer.parseInt(taxonId)).getGenus();
        String key = value + "_" + genus;
        String identifier = devStageTerms.get(key);
        if (identifier == null) {
            Item term = getChadoDBConverter().createItem("OntologyTerm");
            term.setAttribute("name", value);
            String ontologyRef = getDevelopmentOntologyByTaxon(taxonId);
            if (ontologyRef != null) {
                term.setReference("ontology", ontologyRef);
            }
            getChadoDBConverter().store(term);
            devStageTerms.put(key, term.getIdentifier());
            identifier = term.getIdentifier();
        }
        return identifier;
    }

    private String correctDevStageTerm(String value) {
        // some terms are prefixed with ontology namespace
        String prefix = "FlyBase development CV:";
        if (value.startsWith(prefix)) {
            value = value.substring(prefix.length());
        }
        return value;
    }

    private String getTaxonIdForSubmission(String dccId) {
        Integer subChadoId = getSubmissionIdFromDccId(dccId);
        String organism = submissionOrganismMap.get(subChadoId);
        OrganismRepository or = OrganismRepository.getOrganismRepository();
        return "" + or.getOrganismDataByFullName(organism).getTaxonId();
    }

    private String getDevelopmentOntologyByTaxon(String taxonId) throws ObjectStoreException {
        if (taxonId == null) {
            return null;
        }
        String ontologyName = null;
        OrganismRepository or = OrganismRepository.getOrganismRepository();
        String genus = or.getOrganismDataByTaxon(Integer.parseInt(taxonId)).getGenus();
        if ("Drosophila".equals(genus)) {
            ontologyName = "Fly Development";
        } else {
            ontologyName = "Worm Development";
        }

        String ontologyId = devOntologies.get(ontologyName);
        if (ontologyId == null) {
            Item ontology = getChadoDBConverter().createItem("Ontology");
            ontology.setAttribute("name", ontologyName);
            getChadoDBConverter().store(ontology);
            ontologyId = ontology.getIdentifier();
            devOntologies.put(ontologyName, ontologyId);
        }
        return ontologyId;
    }

    private Integer getSubmissionIdFromDccId(String dccId) {
        for (Map.Entry<Integer, String> entry : dccIdMap.entrySet()) {
            if (entry.getValue().equals(dccId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Return the rows needed for data from the applied_protocol_data table.
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getAppliedDataAll(Connection connection)
        throws SQLException {

        String sraAcc = "SRA acc";

        String query = "SELECT d.data_id, d.heading as data_heading,"
                + " d.name as data_name, d.value as data_value,"
                + " c.name as cv_term,"
                + " a.attribute_id, a.heading as att_heading, a.name as att_name,"
                + " a.value as att_value,"
                + " a.dbxref_id as att_dbxref, a.rank as att_rank"
                + " FROM data d"
                + " LEFT JOIN data_attribute da ON (d.data_id = da.data_id)"
                + " LEFT JOIN attribute a ON (da.attribute_id = a.attribute_id)"
                + " LEFT JOIN cvterm c ON (d.type_id = c.cvterm_id)"
                + " LEFT JOIN dbxref as x ON (a.dbxref_id = x.dbxref_id)"
                + " WHERE d.name != '" + sraAcc + "'"
                + " AND d.value != '' "
                + " ORDER BY d.data_id";
        return doQuery(connection, query, "getAppliedDataAll");
    }

    /**
     * Return the rows needed for data from the applied_protocol_data table.
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getAppliedDataCharacteristics(Connection connection)
        throws SQLException {
        String query = "select d.data_id, d.heading as data_heading,"
                + " d.name as data_name, d.value as data_value,"
                + " a.attribute_id, a.heading as att_heading, a.name as att_name,"
                + " a.value as att_value,"
                + " a.dbxref_id as att_dbxref, a.rank as att_rank"
                + " FROM data d, data_attribute da, attribute a, dbxref ax, db"
                + " WHERE d.data_id = da.data_id"
                + " AND da.attribute_id = a.attribute_id"
                + " AND a.dbxref_id = ax.dbxref_id"
                + " AND ax.db_id = db.db_id"
                + " ORDER BY d.data_id, a.dbxref_id ";

        return doQuery(connection, query, "getAppliedDataCharacteristics");
    }

    private class SubmissionProperty
    {
        protected String type;
        protected String wikiPageUrl;
        protected Map<String, List<String>> details;

        protected SubmissionProperty() {
            details = new HashMap<String, List<String>>();
        }

        public SubmissionProperty(String type, String wikiPageUrl) {
            this.type = type;
            this.wikiPageUrl = wikiPageUrl;
            details = new HashMap<String, List<String>>();
        }

        public void addDetail(String type, String value, int rank) {
            List<String> values = details.get(type);
            if (values == null) {
                values = new ArrayList<String>();
                details.put(type, values);
            }
            while (values.size() <= rank) {
                values.add(null);
            }
            values.set(rank, value);
        }

        public String toString() {
            return this.type + ": " + this.wikiPageUrl + this.details.entrySet();
        }
    }

    private final class DatabaseRecordConfig
    {
        private String dbName;
        private String dbDescrition;
        private String dbURL;
        private Set<String> types = new HashSet<String>();

        private DatabaseRecordConfig() {
            // don't
        }
    }

    private Set<DatabaseRecordConfig> initDatabaseRecordConfigs() {
        Set<DatabaseRecordConfig> configs = new HashSet<DatabaseRecordConfig>();

        DatabaseRecordConfig geo = new DatabaseRecordConfig();
        geo.dbName = "GEO";
        geo.dbDescrition = "Gene Expression Omnibus (NCBI)";
        geo.dbURL = "http://www.ncbi.nlm.nih.gov/projects/geo/query/acc.cgi?acc=";
        geo.types.add("GEO_record");
        configs.add(geo);

        DatabaseRecordConfig ae = new DatabaseRecordConfig();
        ae.dbName = "ArrayExpress";
        ae.dbDescrition = "ArrayExpress (EMBL-EBI)";
        ae.dbURL = "http://www.ebi.ac.uk/microarray-as/ae/browse.html?keywords=";
        ae.types.add("ArrayExpress_record");
        configs.add(ae);

        DatabaseRecordConfig sra = new DatabaseRecordConfig();
        sra.dbName = "SRA";
        sra.dbDescrition = "Sequence Read Archive (NCBI)";
        sra.dbURL =
                "http://www.ncbi.nlm.nih.gov/Traces/sra/sra.cgi?cmd=viewer&m=data&s=viewer&run=";
        sra.types.add("ShortReadArchive_project_ID_list (SRA)");
        sra.types.add("ShortReadArchive_project_ID (SRA)");
        configs.add(sra);

        DatabaseRecordConfig ta = new DatabaseRecordConfig();
        ta.dbName = "Trace Archive";
        ta.dbDescrition = "Trace Archive (NCBI)";
        ta.dbURL = "http://www.ncbi.nlm.nih.gov/Traces/trace.cgi?&cmd=retrieve&val=";
        ta.types.add("TraceArchive_record");
        configs.add(ta);

        DatabaseRecordConfig de = new DatabaseRecordConfig();
        de.dbName = "dbEST";
        de.dbDescrition = "Expressed Sequence Tags database (NCBI)";
        de.dbURL = "http://www.ncbi.nlm.nih.gov/nucest/";
        de.types.add("dbEST_record");
        configs.add(de);

        return configs;
    }

    /**
     * Query to get data attributes.
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getAppliedDataAttributes(Connection connection)
        throws SQLException {
        String query =
                "select da.data_id, a.heading, a.value, a.name "
                        + " from data_attribute da, attribute a"
                        + " where da.attribute_id = a.attribute_id";
        return doQuery(connection, query, "getAppliedDataAttributes");
    }

    /**
     * ================
     *    REFERENCES
     * ================
     * to store references between submission and submissionData
     * (1 to many)
     */
    private void setSubmissionRefs(Connection connection)
        throws ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process
        // note: the map should contain only live submissions
        for (Integer submissionId : submissionDataMap.keySet()) {
            for (Integer dataId : submissionDataMap.get(submissionId)) {
                LOG.debug("DAG subRef subid: " + submissionId + " dataId: " + dataId);
                if (appliedDataMap.get(dataId).intermineObjectId == null) {
                    continue;
                }
                Reference reference = new Reference();
                reference.setName("submission");
                reference.setRefId(submissionMap.get(submissionId).itemIdentifier);

                getChadoDBConverter().store(reference,
                        appliedDataMap.get(dataId).intermineObjectId);
            }
        }
        LOG.info("TIME setting submission-data references: "
                + (System.currentTimeMillis() - bT) + " ms");
    }


    /**
     * =====================
     *    DATABASE RECORDS
     * =====================
     */
    private void createDatabaseRecords(Connection connection)
        throws ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        Set<DatabaseRecordConfig> configs = initDatabaseRecordConfigs();

        for (Integer submissionId : submissionDataMap.keySet()) {
            LOG.info("DB RECORD for sub " + dccIdMap.get(submissionId) + "...");
            List<Integer> submissionDbRecords = new ArrayList<Integer>();
            for (Integer dataId : submissionDataMap.get(submissionId)) {
                AppliedData ad = appliedDataMap.get(dataId);
                if (ad.type.equalsIgnoreCase("Result Value")) {
                    for (DatabaseRecordConfig conf : configs) {
                        for (String type : conf.types) {
                            if (ad.name.equals(type)) {
                                submissionDbRecords.addAll(createDatabaseRecords(ad.value, conf));
                            }
                        }
                    }
                }
                // prepare map for setting references
                if (!submissionDbRecords.isEmpty()) {
                    for (Integer dbRecord : submissionDbRecords) {
                        addToMap(dbRecordIdSubItems, dbRecord,
                                submissionMap.get(submissionId).itemIdentifier);
                    }
                }
            }
        }
        LOG.info("TIME creating DatabaseRecord objects: "
                + (System.currentTimeMillis() - bT) + " ms");

        bT = System.currentTimeMillis();
        // set references
        // NB: we are setting them from dbRecord to submissions because more efficient
        // (a few subs have a big collection of dbRecords)
        for (Integer dbRecordId : dbRecordIdSubItems.keySet()) {
            ReferenceList col = new ReferenceList("submissions",
                    dbRecordIdSubItems.get(dbRecordId));
            getChadoDBConverter().store(col, dbRecordId);
        }

        LOG.info("TIME creating refs DatabaseRecord objects: "
                + (System.currentTimeMillis() - bT) + " ms");
    }

    private List<Integer> createDatabaseRecords(String accession, DatabaseRecordConfig config)
        throws ObjectStoreException {
        List<Integer> dbRecordIds = new ArrayList<Integer>();

        String defaultURL = config.dbURL;

        Set<String> cleanAccessions = new HashSet<String>();

        // NOTE - this is a special case to deal with a very strange SRA accession format in some
        // Celniker submissions.  The 'accession' is provided as e.g.
        //   SRR013492.225322.1;SRR013492.462158.1;...
        // We just want the unique SRR ids
        if ("SRA".equals(config.dbName) && (accession.indexOf(';') != -1
                || accession.indexOf('.') != -1)) {
            for (String part : accession.split(";")) {
                if (part.indexOf('.') != -1) {
                    cleanAccessions.add(part.substring(0, part.indexOf('.')));
                } else {
                    cleanAccessions.add(part);
                }
            }
        } else if ("SRA".equals(config.dbName) && (accession.startsWith("SRX"))) {
            config.dbURL = "http://www.ncbi.nlm.nih.gov/sra/";
            dbRecordIds.add(createDatabaseRecord(accession, config));
            config.dbURL = defaultURL;

        } else {
            cleanAccessions.add(accession);
        }

        for (String cleanAccession : cleanAccessions) {
            dbRecordIds.add(createDatabaseRecord(cleanAccession, config));
        }
        return dbRecordIds;
    }

    private Integer createDatabaseRecord(String accession, DatabaseRecordConfig config)
        throws ObjectStoreException {
        DatabaseRecordKey key = new DatabaseRecordKey(config.dbName, accession);
        Integer dbRecordId = dbRecords.get(key);
        if (dbRecordId == null) {
            Item dbRecord = getChadoDBConverter().createItem("DatabaseRecord");
            dbRecord.setAttribute("database", config.dbName);
            dbRecord.setAttribute("description", config.dbDescrition);
            if (StringUtils.isEmpty(accession)) {
                dbRecord.setAttribute("accession", "To be confirmed");
            } else {
                dbRecord.setAttribute("url", config.dbURL + accession);
                dbRecord.setAttribute("accession", accession);
            }
            dbRecordId = getChadoDBConverter().store(dbRecord);
            dbRecords.put(key, dbRecordId);
        }
        return dbRecordId;
    }

    private class DatabaseRecordKey
    {
        private String db;
        private String accession;

        /**
         * Construct with the database and accession
         * @param db database name
         * @param accession id in database
         */
        public DatabaseRecordKey(String db, String accession) {
            this.db = db;
            this.accession = accession;
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object o) {
            if (o instanceof DatabaseRecordKey) {
                DatabaseRecordKey otherKey = (DatabaseRecordKey) o;
                if (StringUtils.isNotEmpty(accession)
                        && StringUtils.isNotEmpty(otherKey.accession)) {
                    return this.db.equals(otherKey.db)
                            && this.accession.equals(otherKey.accession);
                }
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public int hashCode() {
            return db.hashCode() + 3 * accession.hashCode();
        }
    }


    /**
     * =====================
     *    RESULT FILES
     * =====================
     */
    private void createResultFiles(Connection connection)
        throws ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        for (Integer submissionId : submissionDataMap.keySet()) {
            // the applied data is repeated for each protocol
            // so we want to uniquefy the created object
            Set<String> subFiles = new HashSet<String>();
            for (Integer dataId : submissionDataMap.get(submissionId)) {
                AppliedData ad = appliedDataMap.get(dataId);
                // now checking only for 'file', not 'result file'
                if (StringUtils.containsIgnoreCase(ad.type, "file")) {
                    if (!StringUtils.isBlank(ad.value)
                            && !subFiles.contains(ad.value)) {
                        String direction = null;

                        if (StringUtils.containsIgnoreCase(ad.type, "result")) {
                            direction = "result";
                        } else {
                            direction = "input";
                        }
                        createResultFile(ad.value, ad.name, ad.url, direction, submissionId);
                        subFiles.add(ad.value);
                    }
                }
            }
        }
        LOG.info("TIME creating ResultFile objects: " + (System.currentTimeMillis() - bT) + " ms");
    }

    private void createResultFile(String fileName, String type, String relDccId, String direction,
            Integer submissionId)
        throws ObjectStoreException {
        Item resultFile = getChadoDBConverter().createItem("ResultFile");
        resultFile.setAttribute("name", unversionName(fileName));
        String url = null;
        if (fileName.startsWith("http") || fileName.startsWith("ftp")) {
            url = fileName;
        } else {

            if (relDccId != null) {     // the file actually belongs to a related sub
                url = FILE_URL + relDccId + "/extracted/" + unversionName(fileName);
            } else {
                // note: on ftp site submission directories are named with the digits only
                String dccId = dccIdMap.get(submissionId).substring(DCC_PREFIX.length());
                url = FILE_URL + dccId + "/extracted/" + unversionName(fileName);
            }
        }
        resultFile.setAttribute("url", url);
        resultFile.setAttribute("type", type);
        resultFile.setAttribute("direction", direction);
        resultFile.setReference("submission", submissionMap.get(submissionId).itemIdentifier);
        getChadoDBConverter().store(resultFile);
    }

    /**
     * @param fileName
     */
    private String unversionName(String fileName) {
        //        String versionRegex = "\\.*_*[WS|ws]+\\d\\d\\d+";
        String versionRegex = "\\.*_*[Ww][Ss]+\\d\\d\\d+";
        LOG.debug("FFFF: " + fileName + "--->>"
                + "====>" + fileName.replaceAll(versionRegex, ""));
        return fileName.replaceAll(versionRegex, "");
    }

    private void createRelatedSubmissions(Connection connection) throws ObjectStoreException {
        Map<Integer, Set<String>> relatedSubs = new HashMap<Integer, Set<String>>();

        for (Map.Entry<Integer, List<SubmissionReference>> entry : submissionRefs.entrySet()) {
            Integer submissionId = entry.getKey();

            List<SubmissionReference> lref = entry.getValue();
            Iterator<SubmissionReference> i = lref.iterator();
            while (i.hasNext()) {
                SubmissionReference ref = i.next();
                addRelatedSubmissions(relatedSubs, submissionId, ref.referencedSubmissionId);
                addRelatedSubmissions(relatedSubs, ref.referencedSubmissionId, submissionId);
            }
            LOG.debug("RRSS11 " + relatedSubs.size() + "|" + relatedSubs.keySet() + "|"
                    + relatedSubs.values());
        }
        for (Map.Entry<Integer, Set<String>> entry : relatedSubs.entrySet()) {
            ReferenceList related = new ReferenceList("relatedSubmissions",
                    new ArrayList<String>(entry.getValue()));
            getChadoDBConverter().store(related, entry.getKey());
        }
    }

    private void addRelatedSubmissions(Map<Integer, Set<String>> relatedSubs, Integer subId,
            Integer relatedId) {
        Integer subIdObjectId = submissionMap.get(subId).interMineObjectId;
        Set<String> itemIds = relatedSubs.get(subIdObjectId);
        if (itemIds == null) {
            itemIds = new HashSet<String>();
            relatedSubs.put(submissionMap.get(subId).interMineObjectId, itemIds);
        }
        itemIds.add(submissionMap.get(relatedId).itemIdentifier);
    }


    //sub -> prot
    private void setSubmissionProtocolsRefs(Connection connection)
        throws ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        Map<Integer, List<Integer>> submissionProtocolMap = new HashMap<Integer, List<Integer>>();
        Iterator<Integer> apId = appliedProtocolMap.keySet().iterator();
        while (apId.hasNext()) {
            Integer thisAP = apId.next();
            AppliedProtocol ap = appliedProtocolMap.get(thisAP);
            Util.addToListMap(submissionProtocolMap, ap.submissionId, ap.protocolId);
        }

        Iterator<Integer> subs = submissionProtocolMap.keySet().iterator();
        while (subs.hasNext()) {
            Integer thisSubmissionId = subs.next();
            List<Integer> protocolChadoIds = submissionProtocolMap.get(thisSubmissionId);

            ReferenceList collection = new ReferenceList();
            collection.setName("protocols");
            for (Integer protocolChadoId : protocolChadoIds) {
                collection.addRefId(protocolItemIds.get(protocolChadoId));
            }
            Integer storedSubmissionId = submissionMap.get(thisSubmissionId).interMineObjectId;
            getChadoDBConverter().store(collection, storedSubmissionId);

            // TODO use Item?
            // if the experiment type is not set in the db, check protocols
            if (!submissionWithExpTypeSet.contains(thisSubmissionId)) {
                LOG.warn("EXPERIMENT TYPE NOT SET in chado for submission: "
                        + dccIdMap.get(thisSubmissionId));
                // may need protocols from referenced submissions to work out experiment type
                List<Integer> relatedSubsProtocolIds = new ArrayList<Integer>(
                        findProtocolIdsFromReferencedSubmissions(thisSubmissionId));
                if (relatedSubsProtocolIds != null) {
                    protocolChadoIds.addAll(relatedSubsProtocolIds);
                }
                String piName = submissionProjectMap.get(thisSubmissionId);
                setSubmissionExperimentType(storedSubmissionId, protocolChadoIds, piName);
            }
        }
        LOG.info("TIME setting submission-protocol references: "
                + (System.currentTimeMillis() - bT) + " ms");
    }

    // store Submission.experimentType if it can be inferred from protocols
    private void setSubmissionExperimentType(Integer storedSubId, List<Integer> protocolIds,
            String piName) throws ObjectStoreException {
        Set<String> protocolTypes = new HashSet<String>();
        for (Integer protocolId : protocolIds) {
            protocolTypes.add(protocolTypesMap.get(protocolId).trim());
        }

        String experimentType = inferExperimentType(protocolTypes, piName);
        if (experimentType != null) {
            Attribute expTypeAtt = new Attribute("experimentType", experimentType);
            getChadoDBConverter().store(expTypeAtt, storedSubId);
        }
    }

    // Fetch protocols used to create reagents that are inputs to this submission, these are
    // found in referenced submissions
    private List<Integer> findProtocolIdsFromReferencedSubmissions(Integer submissionId) {
        List<Integer> protocolIds = new ArrayList<Integer>();

        if (submissionRefs == null) {
            throw new RuntimeException("Attempting to access submissionRefs before it has been"
                    + " populated, this method needs to be called after"
                    + " processSubmissionProperties");
        }

        List<SubmissionReference> refs = submissionRefs.get(submissionId);
        if (refs == null) {
            return protocolIds;
        }
        for (SubmissionReference subRef : refs) {
            LOG.info("RRSSprot: " + subRef.referencedSubmissionId
                    + "|" + subRef.dataValue);
            for (AppliedProtocol aProtocol : findAppliedProtocolsFromReferencedSubmission(subRef)) {
                LOG.info("RRSSprotId: " + aProtocol.protocolId);
                protocolIds.add(aProtocol.protocolId);
            }
        }
        return protocolIds;
    }

    /**
     * Work out an experiment type give the combination of protocols used for the
     * submission.  e.g. *immunoprecipitation + hybridization = chIP-chip
     * @param protocolTypes the protocol types
     * @param piName name of PI
     * @return a short experiment type
     */
    protected String inferExperimentType(Set<String> protocolTypes, String piName) {

        // extraction + sequencing + reverse transcription - ChIP = RTPCR
        // extraction + sequencing - reverse transcription - ChIP = RNA-seq
        if (containsMatch(protocolTypes, "nucleic_acid_extraction|RNA extraction")
                && containsMatch(protocolTypes, "sequencing(_protocol)?")
                && !containsMatch(protocolTypes, "chromatin_immunoprecipitation")) {
            if (containsMatch(protocolTypes, "reverse_transcription")) {
                return "RTPCR";
            } else {
                return "RNA-seq";
            }
        }

        // reverse transcription + PCR + RACE = RACE
        // reverse transcription + PCR - RACE = RTPCR
        if (containsMatch(protocolTypes, "reverse_transcription")
                && containsMatch(protocolTypes, "PCR(_amplification)?")) {
            if (containsMatch(protocolTypes, "RACE")) {
                return "RACE";
            } else {
                return "RTPCR";
            }
        }

        // ChIP + hybridization = ChIP-chip
        // ChIP - hybridization = ChIP-seq
        if (containsMatch(protocolTypes, "(.*)?immunoprecipitation")) {
            if (containsMatch(protocolTypes, "hybridization")) {
                return "ChIP-chip";
            } else {
                return "ChIP-seq";
            }
        }

        // hybridization - ChIP =
        //    Celniker:  RNA tiling array
        //    Henikoff:  Chromatin-chip
        //    otherwise: Tiling array
        if (containsMatch(protocolTypes, "hybridization")
                && !containsMatch(protocolTypes, "immunoprecipitation")) {
            if ("Celniker".equals(piName)) {
                return "RNA tiling array";
            } else if ("Henikoff".equals(piName)) {
                return "Chromatin-chip";
            } else {
                return "Tiling array";
            }
        }
        // annotation = Computational annotation
        if (containsMatch(protocolTypes, "annotation")) {
            return "Computational annotation";
        }
        // If we haven't found a type yet, and there is a growth protocol, then
        // this is probably an RNA sample creation experiment from Celniker
        if (containsMatch(protocolTypes, "grow")) {
            return "RNA sample creation";
        }
        return null;
    }

    // utility method for looking up in a set by regular expression
    private boolean containsMatch(Set<String> testSet, String regex) {
        boolean matches = false;
        Pattern p = Pattern.compile(regex);
        for (String test : testSet) {
            Matcher m = p.matcher(test);
            if (m.matches()) {
                matches = true;
            }
        }
        return matches;
    }

    //sub -> exp
    private void setSubmissionExperimentRefs(Connection connection)
        throws ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        // the map should contain only live submissions
        Iterator<String> exp = expSubMap.keySet().iterator();
        while (exp.hasNext()) {
            String thisExp = exp.next();
            List<Integer> subs = expSubMap.get(thisExp);
            Iterator<Integer> s = subs.iterator();
            while (s.hasNext()) {
                Integer thisSubId = s.next();
                Reference reference = new Reference();
                reference.setName("experiment");
                reference.setRefId(experimentIdRefMap.get(thisExp));
                getChadoDBConverter().store(reference,
                        submissionMap.get(thisSubId).interMineObjectId);
            }
        }
        LOG.info("TIME setting submission-experiment references: "
                + (System.currentTimeMillis() - bT) + " ms");
    }

    //sub -> ef
    private void setSubmissionEFactorsRefs(Connection connection)
        throws ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process
        Iterator<Integer> subs = submissionEFactorMap.keySet().iterator();
        while (subs.hasNext()) {
            Integer thisSubmissionId = subs.next();
            List<String> eFactors = submissionEFactorMap.get(thisSubmissionId);

            LOG.debug("EF REFS: " + thisSubmissionId + " (" + eFactors + ")");
            Iterator<String> ef = eFactors.iterator();
            ReferenceList collection = new ReferenceList();
            collection.setName("experimentalFactors");
            while (ef.hasNext()) {
                String currentEF = ef.next();
                collection.addRefId(eFactorIdRefMap.get(currentEF));
                LOG.debug("EF REFS: ->" + currentEF + " ref: " + eFactorIdRefMap.get(currentEF));
            }
            if (!collection.equals(null)) {
                LOG.debug("EF REFS: ->" + thisSubmissionId + "|"
                        + submissionMap.get(thisSubmissionId).interMineObjectId);
                getChadoDBConverter().store(collection,
                        submissionMap.get(thisSubmissionId).interMineObjectId);
            }
        }
        LOG.info("TIME setting submission-exFactors references: "
                + (System.currentTimeMillis() - bT) + " ms");
    }

    //sub -> publication
    private void setSubmissionPublicationRefs(Connection connection)
        throws ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        Iterator<Integer> subs = publicationIdMap.keySet().iterator();
        while (subs.hasNext()) {
            Integer thisSubmissionId = subs.next();
            Reference reference = new Reference();
            reference.setName("publication");
            reference.setRefId(publicationIdRefMap.get(thisSubmissionId));
            getChadoDBConverter().store(reference,
                    submissionMap.get(thisSubmissionId).interMineObjectId);
        }
        LOG.info("TIME setting submission-publication references: "
                + (System.currentTimeMillis() - bT) + " ms");
    }

    /**
     * to store references between applied protocols and their input data
     * reverse reference: data -> next appliedProtocols
     * and between applied protocols and their output data
     * reverse reference: data -> previous appliedProtocols
     * (many to many)
     */
    private void setDAGRefs(Connection connection)
        throws ObjectStoreException {
        long bT = System.currentTimeMillis();     // to monitor time spent in the process

        for (Integer thisAP : appliedProtocolMap.keySet()) {
            AppliedProtocol ap = appliedProtocolMap.get(thisAP);

            if (!ap.inputs.isEmpty()) {
                ReferenceList collection = new ReferenceList("inputs");
                for (Integer inputId : ap.inputs) {
                    collection.addRefId(appliedDataMap.get(inputId).itemIdentifier);
                    //if (collection.getRefIds().contains(null)) {
                    //  LOG.info("Applied Protocol " + thisAP + " of protocol " + ap.protocolId
                    //            + " and inputs " + ap.inputs );
                    //  }
                }
                if (collection.getRefIds().contains(null)) {
                    LOG.warn("Applied Protocol " + thisAP + " has only inputs not corresponding"
                            + " to any output in previous protocol"
                            + " and cannot be linked in the DAG.");
                    continue;
                }
                getChadoDBConverter().store(collection, appliedProtocolIdMap.get(thisAP));
            }

            if (!ap.outputs.isEmpty()) {
                ReferenceList collection = new ReferenceList("outputs");
                for (Integer outputId : ap.outputs) {
                    collection.addRefId(appliedDataMap.get(outputId).itemIdentifier);
                }
                if (collection.getRefIds().contains(null)) {
                    LOG.warn("Applied Protocol " + thisAP + " has null AD.itemidentifiers");
                    continue;
                }
                getChadoDBConverter().store(collection, appliedProtocolIdMap.get(thisAP));

            }
        }
        LOG.info("TIME setting DAG references: " + (System.currentTimeMillis() - bT) + " ms");
    }

    /**
     * maps from chado field names to ours.
     *
     * TODO: check if up to date
     *
     * if a field is not needed it is marked with NOT_TO_BE_LOADED
     * a check is performed and fields unaccounted for are logged.
     */
    private static final Map<String, String> FIELD_NAME_MAP =
            new HashMap<String, String>();
    private static final String NOT_TO_BE_LOADED = "this is ; illegal - anyway";

    static {
        // experiment
        // swapping back to uniquename in experiment table
        // FIELD_NAME_MAP.put("Investigation Title", "title");
        FIELD_NAME_MAP.put("Investigation Title", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Project", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Project URL", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Lab", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Experiment Description", "description");
        FIELD_NAME_MAP.put("Experimental Design", "design");
        FIELD_NAME_MAP.put("Experimental Factor Type", "factorType");
        FIELD_NAME_MAP.put("Experimental Factor Name", "factorName");
        FIELD_NAME_MAP.put("Quality Control Type", "qualityControl");
        FIELD_NAME_MAP.put("Replicate Type", "replicate");
        FIELD_NAME_MAP.put("Date of Experiment", "experimentDate");
        FIELD_NAME_MAP.put("Public Release Date", "publicReleaseDate");
        FIELD_NAME_MAP.put("Embargo Date", "embargoDate");
        FIELD_NAME_MAP.put("dcc_id", "DCCid");
        FIELD_NAME_MAP.put("replaces", "replacesSubmission");
        FIELD_NAME_MAP.put("PubMed ID", "pubMedId");
        FIELD_NAME_MAP.put("Person First Name", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Person Mid Initials", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Person Last Name", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Person Affiliation", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Person Address", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Person Phone", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Person Email", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Person Roles", NOT_TO_BE_LOADED);

        FIELD_NAME_MAP.put("Data Type", "category");
        FIELD_NAME_MAP.put("Assay Type", "experimentType");
        FIELD_NAME_MAP.put("Release Reservations", "notice");
        FIELD_NAME_MAP.put("RNAsize", "RNAsize");
        // these are names in name/value couples for ReadCount
        FIELD_NAME_MAP.put("Total Read Count", "totalReadCount");
        FIELD_NAME_MAP.put("Total Mapped Read Count", "totalMappedReadCount");
        FIELD_NAME_MAP.put("Multiply Mapped Read Count", "multiplyMappedReadCount");
        FIELD_NAME_MAP.put("Uniquely Mapped Read Count", "uniquelyMappedReadCount");

        // data: parameter values
        FIELD_NAME_MAP.put("Array Data File", "arrayDataFile");
        FIELD_NAME_MAP.put("Array Design REF", "arrayDesignRef");
        FIELD_NAME_MAP.put("Derived Array Data File", "derivedArrayDataFile");
        FIELD_NAME_MAP.put("Result File", "resultFile");

        // protocol
        FIELD_NAME_MAP.put("Protocol Type", "type");
        FIELD_NAME_MAP.put("url protocol", "url");
        FIELD_NAME_MAP.put("Characteristics", "characteristics");
        FIELD_NAME_MAP.put("species", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("references", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("lab", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Comment", NOT_TO_BE_LOADED);
    }

    /**
     * to store identifiers in project maps.
     * @param i
     * @param chadoId
     * @param intermineObjectId
     * @throws ObjectStoreException
     */
    private void storeInProjectMaps(Item i, String surnamePI, Integer intermineObjectId)
        throws ObjectStoreException {
        if ("Project".equals(i.getClassName())) {
            projectIdMap .put(surnamePI, intermineObjectId);
            projectIdRefMap .put(surnamePI, i.getIdentifier());
        } else {
            throw new IllegalArgumentException(
                    "Type mismatch: expecting Project, getting "
                            + i.getClassName().substring(37) + " with intermineObjectId = "
                            + intermineObjectId + ", project = " + surnamePI);
        }
        debugMap .put(i.getIdentifier(), i.getClassName());
    }

    /**
     * to store identifiers in lab maps.
     * @param i
     * @param chadoId
     * @param intermineObjectId
     * @throws ObjectStoreException
     */
    private void storeInLabMaps(Item i, String labName, Integer intermineObjectId)
        throws ObjectStoreException {
        if ("Lab".equals(i.getClassName())) {
            labIdMap .put(labName, intermineObjectId);
            labIdRefMap .put(labName, i.getIdentifier());
        } else {
            throw new IllegalArgumentException(
                    "Type mismatch: expecting Lab, getting "
                            + i.getClassName().substring(37) + " with intermineObjectId = "
                            + intermineObjectId + ", lab = " + labName);
        }
        debugMap .put(i.getIdentifier(), i.getClassName());
    }

    private void mapSubmissionAndData(Integer submissionId, Integer dataId) {
        Util.addToListMap(submissionDataMap, submissionId, dataId);
        dataSubmissionMap.put(dataId, submissionId);
    }

    /**
     * =====================
     *    UTILITY METHODS
     * =====================
     */

    /**
     * method to wrap the execution of a query, without log info
     * @param connection
     * @param query
     * @return the result set
     * @throws SQLException
     */
    private ResultSet doQuery(Connection connection, String query)
        throws SQLException {
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
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
     * adds an element to a list which is the value of a map
     * @param m       the map (<Integer, List<String>>)
     * @param key     the key for the map
     * @param value   the list
     */
    private static void addToMap(Map<Integer, List<String>> m, Integer key, String value) {

        List<String> ids = new ArrayList<String>();

        if (m.containsKey(key)) {
            ids = m.get(key);
        }
        if (!ids.contains(value)) {
            ids.add(value);
            m.put(key, ids);
        }
    }

}
