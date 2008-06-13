package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.dataconversion.ChadoSequenceProcessor.FeatureData;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

/**
 * Create items from the modENCODE metadata extensions to the chado schema.
 * @author Kim Rutherford, sergio contrino
 */
public class ModEncodeMetaDataProcessor extends ChadoProcessor
{
    private static final Logger LOG = Logger.getLogger(ModEncodeMetaDataProcessor.class);

    // maps to link chado identifiers with intermineObjectId (Integer, Integer)
    // and chado identifiers with item identifiers (Integer, String)
    private Map<Integer, Integer> protocolIdMap = new HashMap<Integer, Integer>();
    private Map<Integer, String> protocolIdRefMap = new HashMap<Integer, String>();
    private Map<Integer, Integer> appliedProtocolIdMap = new HashMap<Integer, Integer>();
    private Map<Integer, String> appliedProtocolIdRefMap = new HashMap<Integer, String>();

    // for providers, the maps link the provider name with the identifiers...
    private Map<String, Integer> providerIdMap = new HashMap<String, Integer>();
    private Map<String, String> providerIdRefMap = new HashMap<String, String>();
    // ...we need a further map to link to experiment 
    private Map<Integer, String> experimentProviderMap = new HashMap<Integer, String>();

    // maps from chado identifier to specific objects
    private Map<Integer, ExperimentSubmissionDetails> experimentMap =
        new HashMap<Integer, ExperimentSubmissionDetails>();
    private Map<Integer, AppliedProtocol> appliedProtocolMap =
        new HashMap<Integer, AppliedProtocol>();
    private Map<Integer, AppliedData> appliedDataMap =
        new HashMap<Integer, AppliedData>();

    // list of firstAppliedProtocols, first level of the DAG linking
    // the applied protocols through the data (and giving the flow
    // of data)
    private List<Integer> firstAppliedProtocols = new ArrayList<Integer>();

    // maps of the initial input data and final output data for an experiment
    private Map<Integer, List<Integer>> experimentInDataMap = new HashMap<Integer, List<Integer>>();
    private Map<Integer, List<Integer>> experimentOutDataMap =
        new HashMap<Integer, List<Integer>>();

    // map used to store all data relative to an experiment
    // experimentId, list of appliedDataIds
    private Map<Integer, List<Integer>> experimentDataMap = new HashMap<Integer, List<Integer>>();

    // maps of each submission input with its (submission) output data and vice versa
    private Map<Integer, List<Integer>> inOutDataMap = new HashMap<Integer, List<Integer>>();
    private Map<Integer, List<Integer>> outInDataMap = new HashMap<Integer, List<Integer>>();

    // just for debugging
    private Map<String, String> debugMap = new HashMap<String, String>(); // itemIdentifier, type
    private static final String PREFIX = "http://www.flymine.org/model/genomic#";


    private static class ExperimentSubmissionDetails
    {
        // the identifier assigned to Item eg. "0_23"
        private String itemIdentifier;
        // the object id of the stored Item
        private Integer interMineObjectId;
        // the identifier assigned to Provider Item for this object
        private String providerItemIdentifier;
    }

    /**
     * AppliedProtocol class
     * to reconstruct the flow of submission data
     *
     */
    private static class AppliedProtocol
    {
        private Integer experimentId;      // chado
        private Integer protocolId;
        private String itemIdentifier;     // e.g. "0_12"
        private Integer intermineObjectId;
        private Integer levelDag;          // not used
        // the output data associated to this applied protocol
        private List<Integer> outputData = new ArrayList<Integer>();
        private List<Integer> inputData = new ArrayList<Integer>();
    }

    /**
     * AppliedData class
     * to reconstruct the flow of submission data
     *
     */
    private static class AppliedData
    {
        private String itemIdentifier;
        private Integer intermineObjectId;
        // the list of applied protocols for which this data item is an input
        private List<Integer> nextAppliedProtocols = new ArrayList<Integer>();
        private List<Integer> previousAppliedProtocols = new ArrayList<Integer>();
        private List<Integer> appliedDataIds = new ArrayList<Integer>();
    }

    /**
     * Create a new ChadoModuleProcessor object
     * @param chadoDBConverter the converter that created this Processor
     */
    public ModEncodeMetaDataProcessor(ChadoDBConverter chadoDBConverter) {
        super(chadoDBConverter);
    }

    /**
     * {@inheritDoc}
     * Note:TODO
     * Inbox
     */
    @Override
    public void process(Connection connection) throws Exception {

        LOG.info("ICI:  provider");
        processProviderTable(connection);
        //processProviderAttributes(connection);

        LOG.info("ICI:  experiment");
        processExperimentTable(connection);
        LOG.info("ICI:  experimentProps");
        processExperimentProps(connection);
        LOG.info("ICI:  protocol");

        processProtocolTable(connection);
        processProtocolAttributes(connection);

        processAppliedProtocolTable(connection);

        processAppliedData(connection);
        processDag(connection);

        // process features and keep a map from chado feature_id to info
        Map<Integer, FeatureData> featureMap = processFeatures(connection, experimentMap);
        processDataFeatureTable(connection, featureMap);

        // links submission inputs with their respective submission outputs
        // also set the references
        linksInOut(connection);

        // set references
        LOG.info("ICI:  setExperimentRefs");
        setExperimentRefs(connection);
        LOG.info("ICI:  setExperiment");
        setExperimentInputRefs(connection);
        LOG.info("ICI:  setExperimentResults");
        setExperimentResultsRefs(connection);
        LOG.info("ICI:  setExperimentProtocols");
        setExperimentProtocolsRefs(connection);
        LOG.info("ICI:  setDAG");
        setDAGRefs(connection);
    }


    /**
     *
     * ==============
     *    FEATURES
     * ==============
     *
     * @param connection
     * @param featureMap
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private void processDataFeatureTable(Connection connection, Map<Integer,
            FeatureData> featureMap)
    throws SQLException, ObjectStoreException {
        ResultSet res = getDataFeatureResultSet(connection);
        while (res.next()) {
            Integer dataId = new Integer(res.getInt("data_id"));
            Integer featureId = new Integer(res.getInt("feature_id"));
            FeatureData featureData = featureMap.get(featureId);
            if (featureData == null) {
                LOG.error("FIXME: no data for feature_id: " + featureId
                        + " in processDataFeatureTable()");
                continue;
            }
            String featureItemId = featureData.getItemIdentifier();
            FeatureData fd = featureData;
            LOG.error(fd.getInterMineType() + ": " + fd.getChadoFeatureName()
                    + ", " + fd.getChadoFeatureUniqueName());
            Reference featureRef = new Reference("feature", featureItemId);
            getChadoDBConverter().store(featureRef,
                    appliedDataMap.get(dataId).intermineObjectId);
        }
    }

    private ResultSet getDataFeatureResultSet(Connection connection)
    throws SQLException {
        String query =
            "SELECT df.data_id, df.feature_id"
            + " FROM data d, data_feature df"
            + " WHERE df.data_id = d.data_id"
            + " AND d.heading != 'Result File'";

        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    private Map<Integer, FeatureData> processFeatures(Connection connection,
            Map<Integer, ExperimentSubmissionDetails> experimentMap)
            throws Exception {
        Map<Integer, FeatureData> featureMap = new HashMap<Integer, FeatureData>();
        for (Map.Entry<Integer, ExperimentSubmissionDetails> entry: experimentMap.entrySet()) {
            Integer chadoExperimentId = entry.getKey();
            ExperimentSubmissionDetails experimentSubmissionDetails = entry.getValue();
            String experimentItemIdentifier = experimentSubmissionDetails.itemIdentifier;
            String providerItemIdentifier = experimentSubmissionDetails.providerItemIdentifier;
            ModEncodeFeatureProcessor processor =
                new ModEncodeFeatureProcessor(getChadoDBConverter(), experimentItemIdentifier,
                        providerItemIdentifier, experimentDataMap.get(chadoExperimentId));

            processor.process(connection);
            featureMap.putAll(processor.getFeatureMap());

            LOG.info("FEAT2:     " + chadoExperimentId);
            LOG.info("FEAT2  featureMap keys:   " + featureMap.keySet().size());
            LOG.info("FEAT2  featureMap values: " + featureMap.values().size());
        }
        return featureMap;
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
     * (ExperimentSubmissionDetails, AppliedProtocol, AppliedData) and their
     * respective maps to chado identifiers needed to traverse the DAG.
     * It then traverse the DAG, assigning the experiment_id to all data.
     *
     * @param connection
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private void processDag(Connection connection)
    throws SQLException, ObjectStoreException {
        ResultSet res = getDAGResultSet(connection);
        AppliedProtocol node = new AppliedProtocol();
        AppliedData branch = null;

        Integer count = 0;
        
        Integer actualExperimentId = 0;

        Integer previousAppliedProtocolId = 0;
        while (res.next()) {
            Integer experimentId = new Integer(res.getInt("experiment_id"));
            Integer protocolId = new Integer(res.getInt("protocol_id"));
            Integer appliedProtocolId = new Integer(res.getInt("applied_protocol_id"));
//            Integer appliedDataId = new Integer(res.getInt("applied_protocol_data_id"));
            Integer dataId = new Integer(res.getInt("data_id"));
            String direction = res.getString("direction");

            // build a data node for each iteration
            if (appliedDataMap.containsKey(dataId)) {
                branch = appliedDataMap.get(dataId);
            } else {
                branch = new AppliedData();
            }
            // could use > (order by apid, apdataid, direction)
            // NB: using isLast() is expensive
            if (!appliedProtocolId.equals(previousAppliedProtocolId) || res.isLast()) {
                // last one: fill the list (should be an output)
                if (res.isLast()) {
                    if (direction.equalsIgnoreCase("output")) {
                        node.outputData.add(dataId);
                    }
                }
                // if it is not the first iteration, let's store it
                if (previousAppliedProtocolId > 0) {
                    appliedProtocolMap.put(previousAppliedProtocolId, node);
                }

                // the experimentId != null for the first applied protocol
                if (experimentId > 0) {
                    
                    LOG.info("EXPERIMENT: " + experimentId);

                    firstAppliedProtocols.add(appliedProtocolId);
                    if (direction.startsWith("in")) {
                        // .. the map of initial data for the experiment
                        addToMap (experimentInDataMap, experimentId, dataId);
                        addToMap (experimentDataMap, experimentId, dataId);
                    }
                    // and set actual...
                    actualExperimentId = experimentId;                    
                } else {
                    experimentId = actualExperimentId;
                }

                // new node
                AppliedProtocol newNode = new AppliedProtocol();
                newNode.protocolId = protocolId;                
                newNode.experimentId = experimentId;

                if (direction.startsWith("in")) {
                    // add this applied protocol to the list of nextAppliedProtocols
                    branch.nextAppliedProtocols.add(appliedProtocolId);
                    // ..and update the map
                    if (appliedDataMap.containsKey(dataId)) {
                        appliedDataMap.remove(dataId);
                    }
                    appliedDataMap.put(dataId, branch);
                    // .. and add the dataId to the list of input Data for this applied protocol
                    newNode.inputData.add(dataId);
                } else if (direction.startsWith("out")) {
                    // add the dataId to the list of output Data for this applied protocol:
                    // it will be used to link to the next set of applied protocols
                    newNode.outputData.add(dataId);
                } else {
                    // in case there is some problem with the strings 'input' or 'output'
                    throw new IllegalArgumentException("Data direction not valid for dataId: "
                            + dataId + "|" + direction + "|");
                }
                // for the new round..
                node = newNode;
                previousAppliedProtocolId = appliedProtocolId;
            } else {
                // keep feeding IN et OUT
                if (direction.startsWith("in")) {
                    node.inputData.add(dataId);
                    if (experimentId > 0) {
                        // initial data
                        // the rest of the map (for dag with levels > 1) is filled through outputs
                        // TODO: test with another submission (with depth > 1 )
                        addToMap (experimentDataMap, experimentId, dataId);
                        addToMap (experimentInDataMap, experimentId, dataId);
                    }
                    // as above
                    branch.nextAppliedProtocols.add(appliedProtocolId);
                    if (!appliedDataMap.containsKey(dataId)) {
                        appliedDataMap.put(dataId, branch);
                    } else {
                        appliedDataMap.remove(dataId);
                        appliedDataMap.put(dataId, branch);
                    }
                } else if (direction.startsWith("out")) {
                    node.outputData.add(dataId);
                } else {
                    throw new IllegalArgumentException("Data direction not valid for dataId: "
                            + dataId + "|" + direction + "|");
                }
            }
            count++;
        }
        LOG.info("created " + appliedProtocolMap.size() 
                + "|[" + count + " applied data points] DAG nodes in map");
        res.close();

        // now traverse the DAG, and associate experiment with all the applied protocols
        traverseDag();
    }

    /**
     * Return the rows needed to construct the DAG of the data/protocols.
     * The reference to the experiment is available only for the first set
     * of applied protocols, hence the outer join.
     * This is a protected method so that it can be overridden for testing
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getDAGResultSet(Connection connection)
    throws SQLException {
        String query =
            "SELECT eap.experiment_id, ap.protocol_id, apd.applied_protocol_id"
            + " , apd.data_id, apd.applied_protocol_data_id, apd.direction"
            + " FROM applied_protocol ap LEFT JOIN experiment_applied_protocol eap"
            + " ON (eap.first_applied_protocol_id = ap.applied_protocol_id )"
            + " , applied_protocol_data apd"
            + " WHERE apd.applied_protocol_id = ap.applied_protocol_id"
            + " ORDER By 3,5,6";

        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Applies iteratively buildADaglevel
     *
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private void traverseDag()
    throws SQLException, ObjectStoreException {

        List<Integer> currentIterationAP = firstAppliedProtocols;
        List<Integer> nextIterationAP = new ArrayList<Integer>();

        while (currentIterationAP.size() > 0) {
            nextIterationAP = buildADagLevel (currentIterationAP);
            currentIterationAP = nextIterationAP;
        }
    }

    /**
     * This method is given a set of applied protocols (already associated with an experiment)
     * and produces the next set of applied protocols. The latter are the protocols attached to the
     * output data of the starting set (output data for a applied protocol is the input data for the
     * next one).
     * It also fills the map linking directly results ('leaf' output data) with experiment
     *
     * @param previousAppliedProtocols
     * @return the next batch of appliedProtocolId
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private List<Integer> buildADagLevel(List<Integer> previousAppliedProtocols)
    throws SQLException, ObjectStoreException {
        List<Integer> nextIterationProtocols = new ArrayList<Integer>();
        Iterator<Integer> pap = previousAppliedProtocols.iterator();
        while (pap.hasNext()) {
            List<Integer> outputs = new ArrayList<Integer>();
            Integer currentId = pap.next();
            outputs.addAll(appliedProtocolMap.get(currentId).outputData);
            Integer experimentId = appliedProtocolMap.get(currentId).experimentId;
            Iterator<Integer> od = outputs.iterator();

            LOG.info("DB REFEXA: " + experimentId + "|" + currentId);
            while (od.hasNext()) {
                Integer currentOD = od.next();
                List<Integer> nextProtocols = new ArrayList<Integer>();

                LOG.info("DB REFEXB: " + currentOD);

                // build map experiment-data
                addToMap (experimentDataMap, experimentId, currentOD);

                if (appliedDataMap.containsKey(currentOD)) {
                    // fill the list of next (children) protocols
                    nextProtocols.addAll(appliedDataMap.get(currentOD).nextAppliedProtocols);
                    if (appliedDataMap.get(currentOD).nextAppliedProtocols.isEmpty()) {
                        // this is a leaf!!
                        // we store it in a map that links it directly to the experiment
                        LOG.info("DB REFEXBB: " + experimentId + "|" + currentOD);

                        addToMap(experimentOutDataMap, experimentId, currentOD);
                    }
                }

                // build the list of children applied protocols chado identifiers
                // as input for the next iteration
                Iterator<Integer> nap = nextProtocols.iterator();
                while (nap.hasNext()) {
                    Integer currentAPId = nap.next();
                    // and fill the map with the chado experimentId
                    appliedProtocolMap.get(currentAPId).experimentId = experimentId;
                    //appliedProtocolMap.get(currentAPId).levelDag = (levelDag);
                    nextIterationProtocols.add(currentAPId);

                    // and set the reference from applied protocol to the submission
                    Reference reference = new Reference();
                    reference.setName("experimentSubmission");
                    reference.setRefId(experimentMap.get(experimentId).itemIdentifier);
                    getChadoDBConverter().store(reference, appliedProtocolIdMap.get(currentAPId));

                    LOG.info("DB REFEX: " + experimentId + "|"
                            + currentAPId + "|" +  appliedProtocolIdMap.get(currentAPId));
                }
            }
        }
        return nextIterationProtocols;
    }

    /**
     *
     * =======================
     *    SUBMISSION IN-OUT
     * =======================
     *
     *    This part of the code deals with setting references between
     *    each submission input (experimentInData) with its respective
     *    submission output(s) (experimentOutData), black-boxing the DAG.
     *    This method loop through all the submissions, each time
     *    creating the maps linking ins and outs and then setting
     *    the references.
     * @param connection
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private void linksInOut(Connection connection)
    throws SQLException, ObjectStoreException {
        Set <Integer> experiments = experimentInDataMap.keySet();
        Iterator <Integer> thisInput = experiments.iterator();

        while (thisInput.hasNext()) {
            // FOR EACH EXPERIMENT
            // clear the maps
            inOutDataMap.clear();
            outInDataMap.clear();

            LOG.info("INOUT MAP =========");
            // build In-Out map
            buildInOutMap (thisInput.next());
            // the reverse map is built from the previous one
            buildOutInMap();

            setInOutRefs(connection);
            setOutInRefs(connection);
        }
    }


    /**
     * Builds the map linking each input to its outputs
     *
     * @param experimentId
     */
    private void buildInOutMap(Integer experimentId) {
        // get all the submission input data
        List <Integer> inputData = experimentInDataMap.get(experimentId);
        Iterator <Integer> thisInput = inputData.iterator();

        while (thisInput.hasNext()) {
            Integer currentId = thisInput.next();
            // for each find the related outputs.
            // note: the first time only the submission input is sent to the loop,
            // then it could be list of intermediate outputs (which are
            // inputs to other applied protocols) that need to be processed
            // (i.e. used to find the their respective outputs).
            List <Integer> currentIteration = new ArrayList<Integer>();
            List<Integer> nextIteration = new ArrayList<Integer>();
            currentIteration.add(currentId);

            while (currentIteration.size() > 0) {
                nextIteration = getOutputs (currentIteration, currentId);
                currentIteration = nextIteration;
            }
            LOG.info("INOUT MAP " + currentId + "|" +  inOutDataMap.get(currentId));
        }
    }


    /**
     * get the outputs related to a list of inputs and builds the inOutData map.
     * note: for each submission input, when this is first called the list contains
     * only the submission input. Then it is filled with the outputs found (that are
     * potentially inputs to following steps).
     *
     * @param ids list of intermediate inputs (they are outputs of previous steps)
     * @param submissionInput  the initial input we are considering
     * @return the list of outputs related to the list of inputs
     */
    private List<Integer> getOutputs(List<Integer> ids, Integer submissionInput) {
        // actually this method also set the map.
        List <Integer> outs = new ArrayList<Integer>();

        Iterator <Integer> dataId = ids.iterator();
        while (dataId.hasNext()) {
            Integer currentIn = dataId.next();
            List <Integer> nextAppliedProtocols =
                appliedDataMap.get(currentIn).nextAppliedProtocols;

            if (nextAppliedProtocols.isEmpty()) {
                // this is a submission output: let's connect it to the
                // submission input
                addToMap(inOutDataMap, submissionInput, currentIn);
            } else {
                // keep gathering the outputs..
                Iterator <Integer> nap = nextAppliedProtocols.iterator();
                while (nap.hasNext()) {
                    // addToList checks if the various ids are already present in
                    // the results list before adding them
                    addToList(outs, appliedProtocolMap.get(nap.next()).outputData);
                }
            }
        }
        return outs;
    }

    /**
     * builds the map from each submission output to its related inputs.
     * it uses the previously built reverse map (each submission input
     * with its related submission output(s))
     *
     */
    private void buildOutInMap() {
        LOG.error("ICI: buildOutInMap");

        Set <Integer> in = inOutDataMap.keySet();
        Iterator <Integer> ins = in.iterator();
        while (ins.hasNext()) {
            Integer thisIn = ins.next();
            List <Integer> out = inOutDataMap.get(thisIn);
            Iterator <Integer> outs = out.iterator();
            while (outs.hasNext()) {
                addToMap (outInDataMap, outs.next(), thisIn);
            }
        }
    }


    /**
     *
     * ==============
     *    PROVIDER
     * ==============
     *
     * Providers are loaded statically. A map is built between experimentId and
     * provider's name and used for the references. 2 maps store intermine
     * objectId and itemId, with key the provider name.
     * Note: the provider and the project are now put in the chadoxml now, as surnames,
     * and we could use those instead.
     * 
     * @param connection
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private void processProviderTable(Connection connection)
    throws SQLException, ObjectStoreException {
        ResultSet res = getProviderResultSet(connection);
        int count = 0;
        while (res.next()) {
            Integer experimentId = new Integer(res.getInt("experiment_id"));
            String value = res.getString("value");
            experimentProviderMap.put(experimentId, value);
            count++;
        }
        res.close();

        Set <Integer> exp = experimentProviderMap.keySet();
        LOG.info("PROV " + exp.toString());
        
        Iterator <Integer> i  = exp.iterator();
        while (i.hasNext()) {
            Integer thisExp = i.next();
            String prov = experimentProviderMap.get(thisExp);  

            if (providerIdMap.containsKey(prov)) {
                continue;
            }
            LOG.info("PROV: " + prov);            
            Item provider = getChadoDBConverter().createItem("ModEncodeProvider");
            provider.setAttribute("name", prov);
            Integer intermineObjectId = getChadoDBConverter().store(provider);
            storeInProviderMaps(provider, prov, intermineObjectId);
        }
        LOG.info("created " + providerIdMap.size() + " providers");
    
    }
    
    /**
     * Return the rows needed from the provider table.
     * We use the surname of the Principal Investigator (person ranked 0)
     * as the provider name.
     * This is a protected method so that it can be overridden for testing
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getProviderResultSet(Connection connection)
    throws SQLException {
        String query =

            "SELECT distinct a.experiment_id, a.value||' '||b.value as value"
            + " FROM experiment_prop a, experiment_prop b"
            + " where a.experiment_id = b.experiment_id"
            + " and b.name = 'Person Last Name'"
            + " and a.name = 'Person First Name'"
            + " and a.rank = 0"
            + " and b.rank = 0";

        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * to store provider attributes
     * only affiliation for now!!
     * NOTE: Not used now. 
     * TODO: Putting affiliation in the static file.
     *
     * @param connection
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private void processProviderAttributes(Connection connection)
    throws SQLException, ObjectStoreException {
        ResultSet res = getProviderAttributesResultSet(connection);
        int count = 0;
        while (res.next()) {
            Integer experimentId = new Integer(res.getInt("experiment_id"));
            String heading = res.getString("name");
            String value = res.getString("value");
            String fieldName = PROVIDER_FIELD_NAME_MAP.get(heading);
            if (fieldName == null) {
                LOG.error("NOT FOUND in PROVIDER_FIELD_NAME_MAP: " + heading);
                continue;
            } else if (fieldName == NOT_TO_BE_LOADED) {
                continue;
            }
            setAttribute(providerIdMap.get(experimentId), fieldName, value);
            count++;
        }
        LOG.info("created " + count + " provider properties");
        res.close();
    }

    /**
     * Return the rows needed for provider from the provider_prop table.
     * This is a protected method so that it can be overridden for testing
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */

    protected ResultSet getProviderAttributesResultSet(Connection connection)
    throws SQLException {
        String query =
            "SELECT experiment_id, name, value"
            + " FROM experiment_prop"
            + " where rank=0 ";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     *
     * ================
     *    EXPERIMENT
     * ================
     *
     * @param connection
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private void processExperimentTable(Connection connection)
    throws SQLException, ObjectStoreException {
        ResultSet res = getExperimentResultSet(connection);
        int count = 0;
        while (res.next()) {
            Integer experimentId = new Integer(res.getInt("experiment_id"));
            // String name = res.getString("name");
            Item experiment = getChadoDBConverter().createItem("ExperimentSubmission");
            // experiment.setAttribute("name", name);

            LOG.info("PROV EX " + providerIdRefMap.entrySet().toString());

            String providerName = experimentProviderMap.get(experimentId);
            String providerItemIdentifier = providerIdRefMap.get(providerName);
            experiment.setReference("provider", providerItemIdentifier);
            // ..store all
            Integer intermineObjectId = getChadoDBConverter().store(experiment);
            // ..and fill the ExperimentSubmissionDetails object
            ExperimentSubmissionDetails details = new ExperimentSubmissionDetails();
            details.interMineObjectId = intermineObjectId;
            details.itemIdentifier = experiment.getIdentifier();
            details.providerItemIdentifier = providerItemIdentifier;
            experimentMap.put(experimentId, details);

            debugMap .put(details.itemIdentifier, experiment.getClassName());
            count++;
        }
        LOG.info("created " + count + " experiments");
        res.close();
    }

    /**
     * Return the rows needed from the experiment table.
     * NB: for the moment not using the uniquename, but the name from the
     * experiment_prop table
     * This is a protected method so that it can be overridden for testing
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getExperimentResultSet(Connection connection)
    throws SQLException {
        String query =
            "SELECT experiment_id, uniquename"
            + "  FROM experiment";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * to store experiment attributes
     *
     * @param connection
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private void processExperimentProps(Connection connection)
    throws SQLException, ObjectStoreException {
        ResultSet res = getExperimentPropResultSet(connection);
        int count = 0;
        while (res.next()) {
            Integer experimentId = new Integer(res.getInt("experiment_id"));
            String heading = res.getString("name");
            String value = res.getString("value");
            String fieldName = FIELD_NAME_MAP.get(heading);
            if (fieldName == null) {
                LOG.error("NOT FOUND in FIELD_NAME_MAP: " + heading + " [experiment]");
                continue;
            } else if (fieldName == NOT_TO_BE_LOADED) {
                continue;
            }
            setAttribute(experimentMap.get(experimentId).interMineObjectId, fieldName, value);
            count++;
        }
        LOG.info("created " + count + " experiment properties");
        res.close();
    }

    /**
     * Return the rows needed for experiment from the experiment_prop table.
     * This is a protected method so that it can be overridden for testing
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getExperimentPropResultSet(Connection connection)
    throws SQLException {
        String query =
            "SELECT ep.experiment_id, ep.name, ep.value "
            + "from experiment_prop ep ";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     *
     * ==============
     *    PROTOCOL
     * ==============
     *
     * @param connection
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private void processProtocolTable(Connection connection)
    throws SQLException, ObjectStoreException {
        ResultSet res = getProtocolResultSet(connection);
        int count = 0;
        while (res.next()) {
            Integer protocolId = new Integer(res.getInt("protocol_id"));
            String name = res.getString("name");
            String description = res.getString("description");
            Item protocol = getChadoDBConverter().createItem("Protocol");
            protocol.setAttribute("name", name);
            protocol.setAttribute("description", description);
            Integer intermineObjectId = getChadoDBConverter().store(protocol);
            storeInProtocolMaps (protocol, protocolId, intermineObjectId);
            //protocolIdMap .put(protocolId, intermineObjectId);
            //protocolIdRefMap .put(protocolId, protocol.getIdentifier());
            count++;
        }
        LOG.info("created " + count + " protocols");
        res.close();
    }

    /**
     * Return the rows needed from the protocol table.
     * This is a protected method so that it can be overridden for testing
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getProtocolResultSet(Connection connection) throws SQLException {
        String query =
            "SELECT protocol_id, name, description"
            + "  FROM protocol";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * to store protocol attributes
     *
     * @param connection
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private void processProtocolAttributes(Connection connection)
    throws SQLException, ObjectStoreException {
        ResultSet res = getProtocolAttributesResultSet(connection);
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
            setAttribute(protocolIdMap.get(protocolId), fieldName, value);
            count++;
        }
        LOG.info("created " + count + " protocol attributes");
        res.close();
    }

    /**
     * Return the rows needed for protocols from the attribute table.
     * This is a protected method so that it can be overridden for testing
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getProtocolAttributesResultSet(Connection connection) throws SQLException {
        String query =
            "SELECT p.protocol_id, a.heading, a.value "
            + "from protocol p, attribute a, protocol_attribute pa "
            + "where pa.attribute_id = a.attribute_id "
            + "and pa.protocol_id = p.protocol_id ";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     *
     * ======================
     *    APPLIED PROTOCOL
     * ======================
     *
     * @param connection
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private void processAppliedProtocolTable(Connection connection)
    throws SQLException, ObjectStoreException {
        ResultSet res = getAppliedProtocolResultSet(connection);
        int count = 0;
        while (res.next()) {
            Integer appliedProtocolId = new Integer(res.getInt("applied_protocol_id"));
            Integer protocolId = new Integer(res.getInt("protocol_id"));
            Integer experimentId = new Integer(res.getInt("experiment_id"));
            Item appliedProtocol = getChadoDBConverter().createItem("AppliedProtocol");
            // for DEBUG, to rm
            if (!debugMap.get(protocolIdRefMap.get(protocolId)).
                    equalsIgnoreCase(PREFIX + "Protocol")) {
                throw new IllegalArgumentException(
                        "Type mismatch!!: expecting Protocol, getting "
                        + debugMap.get(protocolIdRefMap.get(protocolId)).substring(37)
                        + " with protocolId = "
                        + protocolId + ", appliedProtocolId = " + appliedProtocolId);
            }
            // setting references to protocols
            appliedProtocol.setReference("protocol", protocolIdRefMap.get(protocolId));
            if (experimentId > 0) {
                // for DEBUG, to rm
                if (!debugMap.get(experimentMap.get(experimentId).itemIdentifier).
                        equals(PREFIX + "ExperimentSubmission")) {
                    throw new IllegalArgumentException(
                            "Type mismatch!!: expecting ExperimentSubmission, getting "
                            + debugMap.get(experimentMap.get(experimentId)
                                    .itemIdentifier).substring(37)
                                    + " with experimentId = "
                                    + experimentId + ", appliedProtocolId = " + appliedProtocolId);
                }
                // setting reference to experimentSubmission
                // probably to rm (we do it later anyway). TODO: check
                appliedProtocol.setReference("experimentSubmission",
                        experimentMap.get(experimentId).itemIdentifier);
            }
            // store it and add to maps
            Integer intermineObjectId = getChadoDBConverter().store(appliedProtocol);
            appliedProtocolIdMap .put(appliedProtocolId, intermineObjectId);
            appliedProtocolIdRefMap .put(appliedProtocolId, appliedProtocol.getIdentifier());

            count++;
        }
        LOG.info("created " + count + " appliedProtocol");
        res.close();
    }

    /**
     * Return the rows needed from the appliedProtocol table.
     * This is a protected method so that it can be overridden for testing
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getAppliedProtocolResultSet(Connection connection)
    throws SQLException {
        String query =
            "SELECT eap.experiment_id ,ap.applied_protocol_id, ap.protocol_id"
            + " FROM applied_protocol ap"
            + " LEFT JOIN experiment_applied_protocol eap"
            + " ON (eap.first_applied_protocol_id = ap.applied_protocol_id )";

        /*        "SELECT ap.applied_protocol_id, ap.protocol_id, apd.data_id, apd.direction"
        + " FROM applied_protocol ap, applied_protocol_data apd"
        + " WHERE apd.applied_protocol_id = ap.applied_protocol_id";
         */

        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }




    /**
     * to store appliedProtocol attributes
     *
     * ======================
     *    APPLIED DATA
     * ======================
     * 
     * TODO: check what if you have different 'unit' for different parameters
     * of the applied protocol
     *
     * @param connection
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private void processAppliedData(Connection connection)
    throws SQLException, ObjectStoreException {
        //ResultSet res = getAppliedProtocolDataResultSet(connection);
        ResultSet res = getAppliedDataResultSet(connection);
        int count = 0;
        while (res.next()) {
            Integer dataId = new Integer(res.getInt("data_id"));
            String name = res.getString("name");
            String value = res.getString("value");
            //String direction = res.getString("direction");
            String heading = res.getString("heading");
            Item submissionData = getChadoDBConverter().createItem("SubmissionData");

            if (name != null && !name.equals("")) {
                submissionData.setAttribute("name", name);
            }
            if (!StringUtils.isEmpty(value)) {
                submissionData.setAttribute("value", value);
            }
            //submissionData.setAttribute("direction", direction);
            submissionData.setAttribute("type", heading);

            // store it and add to object and maps
            Integer intermineObjectId = getChadoDBConverter().store(submissionData);

            AppliedData aData = new AppliedData();
            aData.intermineObjectId = intermineObjectId;
            aData.itemIdentifier = submissionData.getIdentifier();
            appliedDataMap.put(dataId, aData);

            count++;
        }
        LOG.info("created " + count + " SubmissionData");

        res.close();
    }

    //  private void processAppliedProtocolData(Connection connection)
//  throws SQLException, ObjectStoreException {
//  ResultSet res = getAppliedProtocolDataResultSet(connection);
//  int count = 0;
//  while (res.next()) {
//  Integer appliedProtocolDataId = new Integer(res.getInt("applied_protocol_data_id"));
//  Integer appliedProtocolId = new Integer(res.getInt("applied_protocol_id"));
//  Integer dataId = new Integer(res.getInt("data_id"));
//  String name = res.getString("name");
//  String value = res.getString("value");
//  String direction = res.getString("direction");
//  String heading = res.getString("heading");
//  Item submissionData = getChadoDBConverter().createItem("SubmissionData");

//  if (name != null && !name.equals("")) {
//  submissionData.setAttribute("name", name);
//  }
//  if (!StringUtils.isEmpty(value)) {
//  submissionData.setAttribute("value", value);
//  }
//  submissionData.setAttribute("direction", direction);
//  submissionData.setAttribute("type", heading);

//  // store it and add to object and maps
//  Integer intermineObjectId = getChadoDBConverter().store(submissionData);

//  AppliedData aData = new AppliedData();
//  aData.dataId = dataId; //++check if needed
//  aData.intermineObjectId = intermineObjectId;
//  aData.itemIdentifier = submissionData.getIdentifier();
//  appliedDataMap.put(appliedProtocolDataId, aData);

//  count++;
//  }
//  LOG.info("created " + count + " SubmissionData");

//  res.close();
//  }


    /**
     * Return the rows needed for data from the applied_protocol_data table.
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getAppliedProtocolDataResultSet(Connection connection)
    throws SQLException {
        String query =
            "SELECT apd.applied_protocol_id, apd.applied_protocol_data_id, apd.data_id,"
            + " apd.direction, d.heading, d.name, d.value"
            + " FROM applied_protocol_data apd, data d"
            + " WHERE apd.data_id = d.data_id";

        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Return the rows needed for data from the applied_protocol_data table.
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getAppliedDataResultSet(Connection connection)
    throws SQLException {
        String query =
            "SELECT d.data_id,"
            + " d.heading, d.name, d.value"
            + " FROM data d";

        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }


    /**
     * to store applied protocols attributes
     * 
     *  NOT USED
     *
     * @param connection
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private void processAppliedProtocolDataAttributes(Connection connection)
    throws SQLException, ObjectStoreException {
        ResultSet res = getAppliedProtocolDataAttributesResultSet(connection);
        int count = 0;
        while (res.next()) {
            Integer appliedProtocolId = new Integer(res.getInt("applied_protocol_id"));
            String heading = res.getString("heading");
            String value = res.getString("value");
            String fieldName = FIELD_NAME_MAP.get(heading);
            if (fieldName == null) {
                LOG.error("NOT FOUND in FIELD_NAME_MAP: " + heading + " [appliedProtocol]");
                continue;
            }
            setAttribute(appliedProtocolIdMap.get(appliedProtocolId), fieldName, value);
            count++;
        }
        LOG.info("created " + count + " data attributes");
        res.close();
    }

    /**
     * Query to get the attributes for data linked to applied protocols
     * (see previous get method).
     * This is a protected method so that it can be overridden for testing.
     *
     *  NOT USED
     *
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getAppliedProtocolDataAttributesResultSet(Connection connection)
    throws SQLException {
        String query =
            "select apd.applied_protocol_id, da.data_id, a.heading, a.value"
            + " from applied_protocol_data apd, data_attribute da, attribute a"
            + " where"
            + " apd.data_id = da.data_id"
            + " and da.attribute_id = a.attribute_id";

        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * to store references between experiment and submissionData
     * (1 to many)
     */
    private void setExperimentRefs(Connection connection)
    throws ObjectStoreException {
        Iterator<Integer> exp = experimentDataMap.keySet().iterator();
        while (exp.hasNext()) {
            Integer thisExperimentId = exp.next();
            List<Integer> dataIds = experimentDataMap.get(thisExperimentId);
            Iterator<Integer> dat = dataIds.iterator();
            while (dat.hasNext()) {
                Integer currentId = dat.next();
                if (appliedDataMap.get(currentId).intermineObjectId == null) {
                    LOG.info("REF: WWW" + currentId);
                    continue;
                }

                Reference reference = new Reference();
                reference.setName("experimentSubmission");
                reference.setRefId(experimentMap.get(thisExperimentId).itemIdentifier);

                LOG.info("EXPREF: " + currentId + "|" 
                        + appliedDataMap.get(currentId).intermineObjectId);

                getChadoDBConverter().store(reference,
                        appliedDataMap.get(currentId).intermineObjectId);
            }
        }
    }


    /**
     * to store references between experiment and its initial submissionData
     * (initial input of the experiment
     * (1 to many)
     */
    private void setExperimentInputRefs(Connection connection)
    throws ObjectStoreException {
        LOG.info("REF: IN");
        Iterator<Integer> exp = experimentInDataMap.keySet().iterator();
        while (exp.hasNext()) {
            Integer thisExperimentId = exp.next();
            List<Integer> dataIds = experimentInDataMap.get(thisExperimentId);
            Iterator<Integer> dat = dataIds.iterator();
            ReferenceList collection = new ReferenceList();
            collection.setName("experimentInData");
            while (dat.hasNext()) {
                Integer currentId = dat.next();
                if (appliedDataMap.get(currentId) == null) {
                    LOG.info("REF: uuu" + currentId);
                    continue;
                }
                LOG.info("REF: " + currentId);
//                LOG.info("REF: " + experimentMap.get(thisExperimentId).itemIdentifier);
//                LOG.info("REF: ---------------------------");
                collection.addRefId(appliedDataMap.get(currentId).itemIdentifier);
            }
            getChadoDBConverter().store(collection,
                    experimentMap.get(thisExperimentId).interMineObjectId);
        }
        LOG.info("REF: OUT");
    }

    /**
     * to store references between experiment and its resulting submissionData
     * (final output of the experiment)
     * (1 to many)
     */
    private void setExperimentResultsRefs(Connection connection)
    throws ObjectStoreException {
        Iterator<Integer> exp = experimentOutDataMap.keySet().iterator();
        while (exp.hasNext()) {
            Integer thisExperimentId = exp.next();
            List<Integer> dataIds = experimentOutDataMap.get(thisExperimentId);
            Iterator<Integer> dat = dataIds.iterator();
            ReferenceList collection = new ReferenceList();
            collection.setName("experimentOutData");
            while (dat.hasNext()) {
                Integer currentId = dat.next();
                if (appliedDataMap.get(currentId) == null) {
                    continue;
                }
                collection.addRefId(appliedDataMap.get(currentId).itemIdentifier);
            }
            getChadoDBConverter().store(collection,
                    experimentMap.get(thisExperimentId).interMineObjectId);
        }
    }

    //exp -> prot
    private void setExperimentProtocolsRefs(Connection connection)
    throws ObjectStoreException {

        Map<Integer, List<Integer>> expProtocolMap = new HashMap<Integer, List<Integer>>();

        Iterator<Integer> apId = appliedProtocolMap.keySet().iterator();
        while (apId.hasNext()) {
            Integer thisAP = apId.next();
            AppliedProtocol ap = appliedProtocolMap.get(thisAP);
          LOG.info("PROTo " + thisAP + "|" + ap.experimentId 
                  + "|" + ap.protocolId); 
            addToMap(expProtocolMap, ap.experimentId, ap.protocolId);
        }

        Iterator<Integer> exp = expProtocolMap.keySet().iterator();
        while (exp.hasNext()) {
            Integer thisExperimentId = exp.next();
            List<Integer> protocolIds = expProtocolMap.get(thisExperimentId);
            Iterator<Integer> dat = protocolIds.iterator();
            ReferenceList collection = new ReferenceList();
            collection.setName("protocols");
            while (dat.hasNext()) {
                Integer currentId = dat.next();
                LOG.info("PROT: " + currentId 
                        + "|" + protocolIdRefMap.get(currentId));

                collection.addRefId(protocolIdRefMap.get(currentId));
            }
            LOG.info("PROT: END size = " + expProtocolMap.size());            
            LOG.info("PROT: END exp= " + thisExperimentId);

            getChadoDBConverter().store(collection,
                    experimentMap.get(thisExperimentId).interMineObjectId);
        }
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

        Iterator<Integer> apId = appliedProtocolMap.keySet().iterator();
        while (apId.hasNext()) {
            Integer thisAP = apId.next();
            LOG.info("REF: LAST: " + thisAP);

            AppliedProtocol ap = appliedProtocolMap.get(thisAP);

            List<Integer> dataIds = ap.inputData;
            LOG.info("REF: LAST: inData" + dataIds);
            if (!dataIds.isEmpty()) {
                Iterator<Integer> i = dataIds.iterator();
                ReferenceList collection = new ReferenceList();
                collection.setName("inputData");
                while (i.hasNext()) {
                    Integer n = i.next();
                    collection.addRefId(appliedDataMap.get(n).itemIdentifier);
                    LOG.info("REF: LAST: it--" + appliedDataMap.get(n).itemIdentifier);
                }
                LOG.info("REF: LAST: ob--" + ap.intermineObjectId);
                LOG.info("REF: LAST: ob--" + appliedProtocolIdMap.get(thisAP));

                getChadoDBConverter().store(collection, appliedProtocolIdMap.get(thisAP));
            }
            LOG.info("REF: LAST: -----------------------");


            List<Integer> outIds = ap.outputData;
            if (!outIds.isEmpty()) {
                Iterator<Integer> i = dataIds.iterator();
                ReferenceList collection = new ReferenceList();
                collection.setName("outputData");
                while (i.hasNext()) {
                    Integer n = i.next();
                    collection.addRefId(appliedDataMap.get(n).itemIdentifier);
                    LOG.info("REF: LAST: it--" + appliedDataMap.get(n).itemIdentifier);
                }
                LOG.info("REF: LAST: ob--" + appliedProtocolIdMap.get(thisAP));
                getChadoDBConverter().store(collection, appliedProtocolIdMap.get(thisAP));
            }

            LOG.info("REF: LAST: ===================");
        }
        LOG.info("REF: LAST: +++++++++++++++++++++++");
    }



    /**
     * to store references between experiment and its resulting submissionData
     * (final output of the experiment)
     * (1 to many)
     */
    private void setInOutRefs(Connection connection)
    throws ObjectStoreException {
        Iterator<Integer> exp = inOutDataMap.keySet().iterator();
        while (exp.hasNext()) {
            Integer thisId = exp.next();
            List<Integer> dataIds = inOutDataMap.get(thisId);
            Iterator<Integer> dat = dataIds.iterator();
            ReferenceList collection = new ReferenceList();
            collection.setName("relatedOutputs");
            while (dat.hasNext()) {
                Integer currentId = dat.next();
                if (appliedDataMap.get(currentId) == null) {
                    continue;
                }
                collection.addRefId(appliedDataMap.get(currentId).itemIdentifier);
            }
            getChadoDBConverter().store(collection,
                    appliedDataMap.get(thisId).intermineObjectId);
        }
    }

    /**
     * to store references between experiment and its resulting submissionData
     * (final output of the experiment)
     * (1 to many)
     */
    private void setOutInRefs(Connection connection)
    throws ObjectStoreException {
        Iterator<Integer> exp = outInDataMap.keySet().iterator();
        while (exp.hasNext()) {
            Integer thisId = exp.next();
            List<Integer> dataIds = outInDataMap.get(thisId);
            Iterator<Integer> dat = dataIds.iterator();
            ReferenceList collection = new ReferenceList();
            collection.setName("relatedInputs");
            while (dat.hasNext()) {
                Integer currentId = dat.next();
                if (appliedDataMap.get(currentId) == null) {
                    continue;
                }
                collection.addRefId(appliedDataMap.get(currentId).itemIdentifier);
            }
            getChadoDBConverter().store(collection,
                    appliedDataMap.get(thisId).intermineObjectId);
        }
    }



    /**
     * maps from chado field names to ours.
     * if a field is not needed it is marked with NOT_TO_BE_LOADED
     * a check is performed and fields unaccounted for are logged.
     *
     * a specific provider field map is needed because we are using the same
     * chado table of the experiment to get the data.
     * used only for affiliation(!)
     */
    private static final Map<String, String> FIELD_NAME_MAP =
        new HashMap<String, String>();
    private static final Map<String, String> PROVIDER_FIELD_NAME_MAP =
        new HashMap<String, String>();
    private static final String NOT_TO_BE_LOADED = "this is ; illegal - anyway";

    static {
        // experiment
        FIELD_NAME_MAP.put("Investigation Title", "title");
        FIELD_NAME_MAP.put("Experiment Description", "description");
        FIELD_NAME_MAP.put("Experimental Design", "design");
        FIELD_NAME_MAP.put("Experimental Factor Type", "factorType");
        FIELD_NAME_MAP.put("Experimental Factor Name", "factorName");
        FIELD_NAME_MAP.put("Quality Control Type", "qualityControl");
        FIELD_NAME_MAP.put("Replicate Type", "replicate");
        //FIELD_NAME_MAP.put("Date of Experiment", "experimentDate");
        //FIELD_NAME_MAP.put("Public Release Date", "publicReleaseDate");
        // FIELD_NAME_MAP.put("species", "organism");
        // FIELD_NAME_MAP.put("PubMed ID", "publication");
        FIELD_NAME_MAP.put("Person Last Name", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Person Affiliation", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Person First Name", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Person Address", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Person Phone", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Person Email", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Person Roles", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("lab", NOT_TO_BE_LOADED);

        // data: parameter values
        FIELD_NAME_MAP.put("genome version", "genomeVersion");
        FIELD_NAME_MAP.put("median value", "medianValue");
        // data: result values
        FIELD_NAME_MAP.put("transcript ID", "transcriptId");
        //FIELD_NAME_MAP.put("inner primer", "innerPrimer");
        FIELD_NAME_MAP.put("outer primer", "outerPrimer");
        FIELD_NAME_MAP.put("TraceArchive ID", "traceArchiveId");
        FIELD_NAME_MAP.put("genbank ID", "genBankId");
        FIELD_NAME_MAP.put("EST acc", "estAcc");
        // data: source attributes
        FIELD_NAME_MAP.put("Source Name", "source");
        FIELD_NAME_MAP.put("RNA ID", "RNAId");
        FIELD_NAME_MAP.put("Cell Type", "cellType");
        FIELD_NAME_MAP.put("Biosample #", "biosampleNr");
        // data: parameter value attributes
        FIELD_NAME_MAP.put("Unit", "unit");
        FIELD_NAME_MAP.put("Characteristics", "characteristics");
        // data: the real thing?
        FIELD_NAME_MAP.put("Hybridization Name", "hybridizationName");
        FIELD_NAME_MAP.put("Array Data File", "arrayDataFile");
        FIELD_NAME_MAP.put("Array Design REF", "arrayDesignRef");
        FIELD_NAME_MAP.put("Derived Array Data File", "derivedArrayDataFile");
        FIELD_NAME_MAP.put("Result File", "resultFile");
        // data: obsolete?
        // FIELD_NAME_MAP.put("", "arrayMatrixDateFile");
        // FIELD_NAME_MAP.put("", "label");
        // FIELD_NAME_MAP.put("", "source");
        // FIELD_NAME_MAP.put("", "sample");
        // FIELD_NAME_MAP.put("", "extract");
        // FIELD_NAME_MAP.put("", "labelExtract");

        // protocol
        FIELD_NAME_MAP.put("Protocol Type", "type");
        FIELD_NAME_MAP.put("url protocol", "url");
        FIELD_NAME_MAP.put("species", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("references", NOT_TO_BE_LOADED);
    }

    static {
        PROVIDER_FIELD_NAME_MAP.put("Person Affiliation", "affiliation");
        PROVIDER_FIELD_NAME_MAP.put("Person Last Name", NOT_TO_BE_LOADED);
        PROVIDER_FIELD_NAME_MAP.put("Experiment Description", NOT_TO_BE_LOADED);
        PROVIDER_FIELD_NAME_MAP.put("Investigation Title", NOT_TO_BE_LOADED);
        PROVIDER_FIELD_NAME_MAP.put("Experimental Design", NOT_TO_BE_LOADED);
        PROVIDER_FIELD_NAME_MAP.put("Experimental Factor Name", NOT_TO_BE_LOADED);
        PROVIDER_FIELD_NAME_MAP.put("Experimental Factor Type", NOT_TO_BE_LOADED);
        PROVIDER_FIELD_NAME_MAP.put("Person First Name", NOT_TO_BE_LOADED);
        PROVIDER_FIELD_NAME_MAP.put("Person Address", NOT_TO_BE_LOADED);
        PROVIDER_FIELD_NAME_MAP.put("Person Phone", NOT_TO_BE_LOADED);
        PROVIDER_FIELD_NAME_MAP.put("Person Email", NOT_TO_BE_LOADED);
        PROVIDER_FIELD_NAME_MAP.put("Person Roles", NOT_TO_BE_LOADED);
        PROVIDER_FIELD_NAME_MAP.put("Quality Control Type", NOT_TO_BE_LOADED);
        PROVIDER_FIELD_NAME_MAP.put("Replicate Type", NOT_TO_BE_LOADED);
        PROVIDER_FIELD_NAME_MAP.put("PubMed ID", NOT_TO_BE_LOADED);
        PROVIDER_FIELD_NAME_MAP.put("Date of Experiment", NOT_TO_BE_LOADED);
        PROVIDER_FIELD_NAME_MAP.put("Public Release Date", NOT_TO_BE_LOADED);
    }

    /**
     * method to add an element to a list which is the value of a map
     * @param m       the map (<Integer, List<Integer>>)
     * @param key     the key for the map
     * @param value   the list
     */
    private void addToMap(Map<Integer, List<Integer>> m, Integer key, Integer value) {

        List<Integer> ids = new ArrayList<Integer>();

        if (m.containsKey(key)) {
            ids = m.get(key);
        }
        if (!ids.contains(value)) {
            ids.add(value);
            m.put(key, ids);
        }
    }

    /**
     * adds an element (Integer) to a list only if it is not there yet
     * @param l the list
     * @param i the element
     */
    private void addToList(List<Integer> l, Integer i) {

        if (!l.contains(i)) {
            l.add(i);
        }
    }

    /**
     * adds the elements of a list i to a list l only if they are not yet
     * there
     * @param l the receiving list
     * @param i the donating list
     */
    private void addToList(List<Integer> l, List<Integer> i) {
        Iterator <Integer> it  = i.iterator();
        while (it.hasNext()) {
            Integer thisId = it.next();
            if (!l.contains(thisId)) {
                l.add(thisId);
            }
        }
    }


    /**
     * to store identifiers in protocol maps.
     * simply store the proper values in the maps.
     * A check on the type is performed. Possibly can be avoided after more testing,
     * and the old commented lines can be reinstated (note that we need 3 methods, one
     * for each category of data.
     *
     * @param i
     * @param chadoId
     * @param intermineObjectId
     * @throws ObjectStoreException
     */
    private void storeInProtocolMaps(Item i, Integer chadoId, Integer intermineObjectId)
    throws ObjectStoreException {
        if (i.getClassName().equals("http://www.flymine.org/model/genomic#Protocol")) {
            protocolIdMap .put(chadoId, intermineObjectId);
            protocolIdRefMap .put(chadoId, i.getIdentifier());
        } else {
            throw new IllegalArgumentException("Type mismatch: expecting Protocol, getting "
                    + i.getClassName().substring(37) + " with intermineObjectId = "
                    + intermineObjectId + ", chadoId = " + chadoId);
        }
        debugMap .put(i.getIdentifier(), i.getClassName());
    }


    /**
     * to store identifiers in provider maps.
     * @param i
     * @param chadoId
     * @param intermineObjectId
     * @throws ObjectStoreException
     */
    private void storeInProviderMaps(Item i, String providerName, Integer intermineObjectId)
    throws ObjectStoreException {
        if (i.getClassName().equals("http://www.flymine.org/model/genomic#ModEncodeProvider")) {
            providerIdMap .put(providerName, intermineObjectId);
            providerIdRefMap .put(providerName, i.getIdentifier());
        } else {
            throw new IllegalArgumentException(
                    "Type mismatch: expecting ModEncodeProvider, getting "
                    + i.getClassName().substring(37) + " with intermineObjectId = "
                    + intermineObjectId + ", provider = " + providerName);
        }
        debugMap .put(i.getIdentifier(), i.getClassName());
    }

    private void storeInProviderMaps2(Item i, Integer chadoId, Integer intermineObjectId)
    throws ObjectStoreException {
        if (i.getClassName().equals("http://www.flymine.org/model/genomic#ModEncodeProvider")) {
            //providerIdMap .put(chadoId, intermineObjectId);
            //providerIdRefMap .put(chadoId, i.getIdentifier());
        } else {
            throw new IllegalArgumentException(
                    "Type mismatch: expecting ModEncodeProvider, getting "
                    + i.getClassName().substring(37) + " with intermineObjectId = "
                    + intermineObjectId + ", chadoId = " + chadoId);
        }
        debugMap .put(i.getIdentifier(), i.getClassName());
    }


    // utilities for debugging
    // to be removed
    private void printListMap (Map<Integer, List<Integer>> m) {
        Iterator i  = m.keySet().iterator();
        while (i.hasNext()) {
            Integer current = (Integer) i.next();

            List ids = m.get(current);
            Iterator i2 = ids.iterator();
            while (i2.hasNext()) {
                LOG.info("MAP: " + current + "|" + i2.next());
            }
            LOG.info("MAP: ....");
        }
    }

    private void printMap (Map<Integer, Integer> m) {
        Iterator<Integer> i = m.keySet().iterator();
        while (i.hasNext()) {
            Integer thisId = i.next();
            LOG.info("MAP: " + thisId + "|" + m.get(thisId));
        }
        LOG.info("MAP: ....");
    }

    private void printMapAP (Map<Integer, AppliedProtocol> m) {
        Iterator<Integer> i = m.keySet().iterator();
        while (i.hasNext()) {
            Integer a = i.next();
            //LOG.info("DB APMAP ***" + a +  ": " + i2.next() );
            AppliedProtocol ap = m.get(a);
            List<Integer> ids = ap.outputData;
            Iterator<Integer> i2 = ids.iterator();
            while (i2.hasNext()) {
                LOG.info("DB APMAP " + a +  ": " + i2.next());
            }
        }
    }
}
