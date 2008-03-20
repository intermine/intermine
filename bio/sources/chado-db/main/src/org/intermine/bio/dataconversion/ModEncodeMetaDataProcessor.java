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

import org.apache.log4j.Logger;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * Create items from the modENCODE metadata extensions to the chado schema.
 * @author Kim Rutherford
 */
public class ModEncodeMetaDataProcessor extends ChadoProcessor
{
    protected static final Logger LOG = Logger.getLogger(ModEncodeMetaDataProcessor.class);
    private Map<Integer, Integer> protocolIdMap = new HashMap<Integer, Integer>();
    private Map<Integer, String> protocolIdRefMap = new HashMap<Integer, String>();
    private Map<Integer, Integer> experimentIdMap = new HashMap<Integer, Integer>();
    private Map<Integer, String> experimentIdRefMap = new HashMap<Integer, String>();
    private Map<Integer, Integer> providerIdMap = new HashMap<Integer, Integer>();
    private Map<Integer, String> providerIdRefMap = new HashMap<Integer, String>();
    private Map<Integer, Integer> dataIdMap = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> appliedProtocolIdMap = new HashMap<Integer, Integer>();

    private static final Map<String, String> FIELD_NAME_MAP =
        new HashMap<String, String>();

    private static final Map<String, String> PROVIDER_FIELD_NAME_MAP =
        new HashMap<String, String>();

    private static final String NOT_TO_BE_LOADED = "not_needed";

    static {
        //experiment
        FIELD_NAME_MAP.put("Investigation Title", "title");
        FIELD_NAME_MAP.put("Experiment Description", "description");
        FIELD_NAME_MAP.put("Experimental Design", "design");
        FIELD_NAME_MAP.put("Experimental Factor Type", "factorType");
        FIELD_NAME_MAP.put("Experimental Factor Name", "factorName");
        FIELD_NAME_MAP.put("Quality Control Type", "qualityControl");
        FIELD_NAME_MAP.put("Replicate Type", "replicate");
        FIELD_NAME_MAP.put("Date of Experiment", "experimentDate");
        FIELD_NAME_MAP.put("Public Release Date", "publicReleaseDate");
        //FIELD_NAME_MAP.put("species", "organism");
        //FIELD_NAME_MAP.put("PubMed ID", "publication");
        FIELD_NAME_MAP.put("Person Last Name", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Person Affiliation", NOT_TO_BE_LOADED);       
        FIELD_NAME_MAP.put("Person First Name", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Person Address", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Person Phone", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Person Email", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("Person Roles", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("lab", NOT_TO_BE_LOADED);

        //data: parameter values
        FIELD_NAME_MAP.put("genome version", "genomeVersion");
        FIELD_NAME_MAP.put("median value", "medianValue");
        FIELD_NAME_MAP.put("transcript ID", "transcriptId");
        FIELD_NAME_MAP.put("inner primer", "innerPrimer");
        FIELD_NAME_MAP.put("TraceArchive ID", "traceArchiveId");
        FIELD_NAME_MAP.put("genbank ID", "genBankId");
        //data: source attributes
        FIELD_NAME_MAP.put("Source Name", "source");
        FIELD_NAME_MAP.put("RNA ID", "RNAId");
        FIELD_NAME_MAP.put("Cell Type", "cellType");
        FIELD_NAME_MAP.put("Biosample #", "biosampleNr");
        //data: parameter value attributes
        FIELD_NAME_MAP.put("Unit", "unit");
        FIELD_NAME_MAP.put("Characteristics", "characteristics");
        //data: the real thing?
        FIELD_NAME_MAP.put("Hybridization Name", "hybridizationName");
        FIELD_NAME_MAP.put("Array Data File", "arrayDataFile");
        FIELD_NAME_MAP.put("Array Design REF", "arrayDesignRef");
        FIELD_NAME_MAP.put("Derived Array Data File", "derivedArrayDataFile");
        FIELD_NAME_MAP.put("Result File", "resultFile");

        //data: obsolete?
        //FIELD_NAME_MAP.put("", "arrayMatrixDateFile");
        //FIELD_NAME_MAP.put("", "label");
        //FIELD_NAME_MAP.put("", "source");
        //FIELD_NAME_MAP.put("", "sample");
        //FIELD_NAME_MAP.put("", "extract");
        //FIELD_NAME_MAP.put("", "labelExtract");

        //protocol
        FIELD_NAME_MAP.put("Protocol Type", "type");
        FIELD_NAME_MAP.put("url protocol", "url");
        FIELD_NAME_MAP.put("species", NOT_TO_BE_LOADED);
        FIELD_NAME_MAP.put("references", NOT_TO_BE_LOADED);
    }

    static {        
        PROVIDER_FIELD_NAME_MAP.put("Person Last Name", "name");
        PROVIDER_FIELD_NAME_MAP.put("Person Affiliation", "affiliation");       
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

    private Map<Integer, DAGData> DAGIdMap = new HashMap<Integer, DAGData>();

    private List<Integer> firstAppliedProtocols = new ArrayList<Integer>();


    /**
     * Data to reconstruct the flow of submission data
     *
     */
    protected static class DAGData
    {
        private Integer experimentId;
        private Integer protocolId;
        //private Integer depth;

        List<Integer> inputData = new ArrayList<Integer>();
        List<Integer> outputData = new ArrayList<Integer>();

        private Integer intermineObjectId;
        /*
        //private Integer appliedProtocolId;
        //private Integer dataId;
        //private String  direction;
        static final short LENGTH_SET_BIT = 2;
        static final short LENGTH_SET =   2947 | output1 << LENGTH_SET_BIT;
         */

        /**
         * Return the id of the Item representing this feature.
         * @return the ID
         */
        public Integer getExperimentId(Integer dataId) {
            return experimentId;
        }

        /**
         * Return the id of the Item representing this feature.
         * @return the ID
         */
        public Integer getProtocolId(Integer dataId) {
            return protocolId;
        }


        /**
         * Return the id of the Item representing this feature.
         * @return the ID
         */
        public void setExperimentId(Integer experimentId, Integer inputData) {
            if (this.outputData.contains(inputData)) {
                if (this.experimentId == 0) {
                    this.experimentId = experimentId;
                } else {
                    LOG.error("DAGData: trying to overwrite experimetId " 
                            + this.experimentId + " with " + experimentId);
                }
            }
        }

        
        
        
        /**
         * Return the list of INput data ids for this applied protocol 
         * @return the ID
         */
        public List<Integer> getInputIds(Integer appliedProtocolId) {
            return inputData;
        }

        /**
         * Return the list of OUTput data ids for this applied protocol 
         * @return the ID
         */
        public List<Integer> getOutputIds(Integer appliedProtocolId) {
            return outputData;
        }

        /**
         * Return the id of the Item representing this feature.
         * @return the ID
         */
        public Integer getPath(Integer dataId) {
            return experimentId;
        }

        /**
         * Return the id of the Item representing this feature.
         * @return the ID
         */
        public Integer getIntermineObjectId() {
            return intermineObjectId;
        }

    }

    //take2
    
    private Map<Integer, AppliedProtocol> appliedProtocolMap = new HashMap<Integer, AppliedProtocol>();
    private Map<Integer, AppliedData> appliedDataMap = new HashMap<Integer, AppliedData>();


    /**
     * Data to reconstruct the flow of submission data
     *
     */
    protected static class AppliedProtocol
    {
        private Integer experimentId;
        private Integer levelDag;
        private Integer protocolId;
        //List<Integer> inputData = new ArrayList<Integer>();
        List<Integer> outputData = new ArrayList<Integer>();
        private Integer intermineObjectId;

        /**
         * Return the id of the Item representing this feature.
         * @return the ID
         */
        public Integer getExperimentId(Integer dataId) {
            return experimentId;
        }

        /**
         * Return the id of the Item representing this feature.
         * @return the ID
         */
        public Integer getProtocolId(Integer dataId) {
            return protocolId;
        }
        /**
         * Return the id of the Item representing this feature.
         * @return the ID
         */
        public Integer getLevelDag(Integer dataId) {
            return levelDag;
        }

        
        /**
         * Return the id of the Item representing this feature.
         * @return the ID
         */
        public Integer getIntermineObjectId() {
            return intermineObjectId;
        }

        /**
         * Return the list of OUTput data ids for this applied protocol 
         * @return the ID
         */
        public List<Integer> getOutputIds(Integer appliedProtocolId) {
            return outputData;
        }

        
    }

    /**
     * Data to reconstruct the flow of submission data
     *
     */
    protected static class AppliedData
    {
        private Integer dataId;
        List<Integer> nextAppliedProtocols = new ArrayList<Integer>();
        //List<Integer> outputData = new ArrayList<Integer>();
        private Integer intermineObjectId;


        
        /**
         * Return the id of the Item representing this feature.
         * @return the ID
         */
        public Integer getIntermineObjectId() {
            return intermineObjectId;
        }

        /**
         * Return the list of OUTput data ids for this applied protocol 
         * @return the ID
         */
        public List<Integer> getNextAppliedProtocols(Integer dataId) {
            return nextAppliedProtocols;
        }
        
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
     */
    @Override
    public void process(Connection connection) throws Exception {

        processProviderTable(connection);
        processProviderAttributes(connection);

        processExperimentTable(connection);
        processExperimentAttributes(connection);

        processProtocolTable(connection);
        processProtocolAttributes(connection);        

        processAppliedProtocolTable(connection);
        processAppliedProtocolData(connection);        
        processAppliedProtocolDataAttributes(connection);

        processDataTable(connection);

        processDag(connection);
    }
   
    private void processDag(Connection connection)
    throws SQLException, ObjectStoreException {
        ResultSet res = getDAGResultSet(connection);
        AppliedProtocol node = new AppliedProtocol();
        AppliedData branch = null;
        
        Integer previousAppliedProtocolId = 0;
        while (res.next()) {
            Integer experimentId = new Integer(res.getInt("experiment_id"));
            Integer protocolId = new Integer(res.getInt("protocol_id"));
            Integer appliedProtocolId = new Integer(res.getInt("applied_protocol_id"));
            Integer dataId = new Integer(res.getInt("data_id"));
            String direction = res.getString("direction");

            Integer level = 0;
            
            //build a data node for each iteration
            if (appliedDataMap.containsKey(dataId)) {
                branch = appliedDataMap.get(dataId);
            } else {
                branch = new AppliedData();
            }
            //could use > (order by apid, dataid, direction)
            //NB: using isLast() is expensive
            if (!appliedProtocolId.equals(previousAppliedProtocolId) || res.isLast()) {
                //last one: fill the list (should be an output)
                if (res.isLast()) {
                    if (direction.equalsIgnoreCase("output")) {
                        node.outputData.add(dataId);    
                    }                                   
                }
                //if it is not the first iteration, let's store it
                if (previousAppliedProtocolId > 0) { 
                    appliedProtocolMap.put(previousAppliedProtocolId, node);
                    LOG.info("APD " + node.experimentId + "|" + previousAppliedProtocolId + " || " 
                            + "||" + node.outputData.toString());
                }

                //new node
                AppliedProtocol newNode = new AppliedProtocol();

                newNode.protocolId = protocolId;
                newNode.experimentId = experimentId;
                newNode.levelDag = level;
                // the experimentId != null for the first applied protocol
                if (experimentId > 0) {
                    firstAppliedProtocols.add(appliedProtocolId);
                    newNode.levelDag = 1 ;
                    
                }
                
                if (direction.startsWith("in")) {
                    branch.nextAppliedProtocols.add(appliedProtocolId);

                    if (appliedDataMap.containsKey(dataId)) {
                        appliedDataMap.remove(dataId);
                    }   
                    appliedDataMap.put(dataId, branch);
                    LOG.info("APD2a " + dataId );                                            
                } else if (direction.equalsIgnoreCase("output")) {
                    newNode.outputData.add(dataId);    
                } else {
                    LOG.error("APD: problem with data " + dataId + "|" + direction + "|");
                }
                //for the new round..
                node=newNode;
                previousAppliedProtocolId = appliedProtocolId;

            } else {
                //keep feeding IN et OUT
                if (direction.startsWith("in")) { //should not happens
                    branch.nextAppliedProtocols.add(appliedProtocolId);
                    //to sub as above
                    if (!appliedDataMap.containsKey(dataId)) {
                        appliedDataMap.put(dataId, branch);
                        LOG.info("APD1 " + dataId );                        
                    } else {
                        appliedDataMap.remove(dataId);
                        appliedDataMap.put(dataId, branch);
                        LOG.info("APD2 " + dataId );                        
                    }
                } else if (direction.equalsIgnoreCase("output")) {
                    node.outputData.add(dataId);    
                } else {
                    LOG.error("APD: problem with data " + dataId + "|" + direction + "|");
                }               
            }
        }
        LOG.info("created " + appliedProtocolMap.size() + " DAG nodes in map");
        LOG.info("APD 1AppliedProtocol " + firstAppliedProtocols.toString());
        res.close();

        traverseDag();
        
    }

    private void traverseDag()
    throws SQLException, ObjectStoreException {

        List<Integer> currentIterationAP = firstAppliedProtocols;
        List<Integer> nextIterationAP = new ArrayList<Integer>();

        while (currentIterationAP.size() > 0) {
            nextIterationAP = buildADagLevel (currentIterationAP);
            currentIterationAP = nextIterationAP;
            LOG.info("ITER: " + currentIterationAP.toString());
        }        
    }
    
    
    private List<Integer> buildADagLevel(List<Integer> previousAppliedProtocols)
    throws SQLException, ObjectStoreException {
        List<Integer> nextIterationProtocols = new ArrayList<Integer>();
        
        Iterator<Integer> fip = previousAppliedProtocols.iterator();
        while (fip.hasNext()){
            List<Integer> outputs = new ArrayList<Integer>();
            Integer currentId = fip.next();
            outputs.addAll(appliedProtocolMap.get(currentId).outputData);
            Integer experimentId = appliedProtocolMap.get(currentId).experimentId;
            Integer startingLevel = appliedProtocolMap.get(currentId).levelDag;
            
            LOG.info("APDEXP: " + experimentId + " " + outputs.toString());
            Iterator<Integer> od = outputs.iterator();
            while (od.hasNext()) {
                Integer currentOD = od.next();                
                List<Integer> nextProtocols = new ArrayList<Integer>();
                if (appliedDataMap.containsKey(currentOD)) {
                    LOG.info("APDEXP: " + appliedDataMap.get(currentOD).toString() );                    
                    nextProtocols.addAll(appliedDataMap.get(currentOD).nextAppliedProtocols);
                } else {
                    //this is a leaf!!
                    LOG.info("APDEXP: no " + currentOD +  "!!" );
                    continue;
                }
                
                //nextProtocols.addAll(appliedDataMap.get(od.next()).nextAppliedProtocols);
                Iterator<Integer> nap = nextProtocols.iterator();
                while (nap.hasNext()) {
                    Integer currentAPId = nap.next();
                    //AppliedProtocol ap = appliedProtocolMap.get(nap);
                    appliedProtocolMap.get(currentAPId).experimentId = experimentId;
                    appliedProtocolMap.get(currentAPId).levelDag = (startingLevel++);  

                    LOG.info("APD XX " + appliedProtocolMap.get(currentAPId).levelDag.toString() );                    
                    LOG.info("APD XX " + appliedProtocolMap.get(currentAPId).experimentId.toString() );                                        

                    nextIterationProtocols.add(currentAPId);
                }                
            }
        }
        return nextIterationProtocols;
    }


    /**
     * Return the rows needed to construct the DAG of the data/protocols.
     * This is a protected method so that it can be overridden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getDAGResultSet(Connection connection) 
    throws SQLException {
        String query =
            "SELECT eap.experiment_id, ap.protocol_id, apd.applied_protocol_id"
            + " , apd.data_id, apd.direction"
            + " FROM applied_protocol ap LEFT JOIN experiment_applied_protocol eap"
            + " ON (eap.first_applied_protocol_id = ap.applied_protocol_id )"
            + " , applied_protocol_data apd"
            + " WHERE apd.applied_protocol_id = ap.applied_protocol_id"
            + " ORDER By 3,4,5";

        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }


    /* PROVIDER -------------------------------------------------------------------------*/

    private void processProviderTable(Connection connection)
    throws SQLException, ObjectStoreException {
        ResultSet res = getProviderResultSet(connection);
        int count = 0;
        while (res.next()) {
            Integer experimentId = new Integer(res.getInt("experiment_id"));
            String value = res.getString("value");
            Item provider = getChadoDBConverter().createItem("ModEncodeProvider");
            provider.setAttribute("name", value);
            Integer intermineObjectId = getChadoDBConverter().store(provider);            
            providerIdMap .put(experimentId, intermineObjectId);
            providerIdRefMap .put(experimentId, provider.getIdentifier());
            count++;
        }
        LOG.info("created " + count + " providers");
        res.close();
    }

    /**
     * Return the rows needed from the provider table.
     * We use the surname of the Principal Investigator (person ranked 0)
     * as the provider name.
     * This is a protected method so that it can be overridden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getProviderResultSet(Connection connection) 
    throws SQLException {
        String query =
            "SELECT experiment_id, value"
            + " FROM experiment_prop"
            + " where name='Person Last Name'" 
            + " and rank=0 ";

        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }


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
            } else if (fieldName == NOT_TO_BE_LOADED){
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


    /* EXPERIMENT -----------------------------------------------------------------------*/

    private void processExperimentTable(Connection connection)
    throws SQLException, ObjectStoreException {
        ResultSet res = getExperimentResultSet(connection);
        int count = 0;
        while (res.next()) {
            Integer experimentId = new Integer(res.getInt("experiment_id"));
            //String name = res.getString("name");
            Item experiment = getChadoDBConverter().createItem("ExperimentSubmission");
            //experiment.setAttribute("name", name);
            experiment.setReference("provider", providerIdRefMap.get(experimentId));
            Integer intermineObjectId = getChadoDBConverter().store(experiment);
            experimentIdMap .put(experimentId, intermineObjectId);
            experimentIdRefMap .put(experimentId, experiment.getIdentifier());
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

    private void processExperimentAttributes(Connection connection)
    throws SQLException, ObjectStoreException {
        ResultSet res = getExperimentAttributesResultSet(connection);
        int count = 0;
        while (res.next()) {
            Integer experimentId = new Integer(res.getInt("experiment_id"));

            String heading = res.getString("name");
            String value = res.getString("value");
            String fieldName = FIELD_NAME_MAP.get(heading);
            if (fieldName == null) { 
                LOG.error("NOT FOUND in FIELD_NAME_MAP: " + heading + " [experiment]");
                continue;
            } else if (fieldName == NOT_TO_BE_LOADED){
                continue;
            }

            setAttribute(experimentIdMap.get(experimentId), fieldName, value);
            count++;
        }
        LOG.info("created " + count + " experiment properties");
        res.close();
    }

    /**
     * Return the rows needed for experiment from the experiment_prop table.
     * This is a protected method so that it can be overridden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getExperimentAttributesResultSet(Connection connection) 
    throws SQLException {
        String query =
            "SELECT ep.experiment_id, ep.name, ep.value "
            + "from experiment_prop ep ";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /* PROTOCOL -------------------------------------------------------------------------*/

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
            protocolIdMap .put(protocolId, intermineObjectId);
            protocolIdRefMap .put(protocolId, protocol.getIdentifier());
            count++;
        }
        LOG.info("created " + count + " protocols");
        res.close();
    }

    /**
     * Return the rows needed from the protocol table.
     * This is a protected method so that it can be overridden for testing
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
            } else if (fieldName == NOT_TO_BE_LOADED){
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

    /* APPLIED PROTOCOLS-----------------------------------------------------------------*/

    private void processAppliedProtocolTable(Connection connection)
    throws SQLException, ObjectStoreException {
        ResultSet res = getAppliedProtocolResultSet(connection);
        int count = 0;
        while (res.next()) {
            Integer appliedProtocolId = new Integer(res.getInt("applied_protocol_id"));
            Integer protocolId = new Integer(res.getInt("protocol_id"));
            Integer experimentId = new Integer(res.getInt("experiment_id"));
            //String direction = res.getString("direction");
            Item appliedProtocol = getChadoDBConverter().createItem("AppliedProtocol");
            //appliedProtocol.setAttribute("direction", direction);
            appliedProtocol.setReference("protocol", protocolIdRefMap.get(protocolId));
            if (experimentId > 0) {
                appliedProtocol.setReference("experimentSubmission", 
                        experimentIdRefMap.get(experimentId));
            }
            Integer intermineObjectId = getChadoDBConverter().store(appliedProtocol);
            appliedProtocolIdMap .put(appliedProtocolId, intermineObjectId);            
            count++;
        }
        LOG.info("created " + count + " appliedProtocol");
        res.close();
    }

    /**
     * Return the rows needed from the appliedProtocol table.
     * This is a protected method so that it can be overridden for testing
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
            + " ON (eap.first_applied_protocol_id = ap.applied_protocol_id )" ;

        /*        "SELECT ap.applied_protocol_id, ap.protocol_id, apd.data_id, apd.direction"
        + " FROM applied_protocol ap, applied_protocol_data apd"
        + " WHERE apd.applied_protocol_id = ap.applied_protocol_id";
         */

        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**TODO: check what if you have different 'unit' for different parameters
     * of the applied protocol
     * 
     * @param connection
     * @throws SQLException
     * @throws ObjectStoreException
     */
    private void processAppliedProtocolData(Connection connection)
    throws SQLException, ObjectStoreException {
        ResultSet res = getAppliedProtocolDataResultSet(connection);
        int count = 0;
        while (res.next()) {
            Integer appliedProtocolId = new Integer(res.getInt("applied_protocol_id"));
            String name = res.getString("name");
            String value = res.getString("value");

            ClassDescriptor cd = 
                getChadoDBConverter().getModel().getClassDescriptorByName("AppliedProtocol");

            if (cd.getAttributeDescriptorByName(name) == null) {
                String fieldName = FIELD_NAME_MAP.get(name);
                if (fieldName == null) { 
                    LOG.error("NOT FOUND in FIELD_NAME_MAP: " + name + " [appliedProtocol]");
                } else {
                    setAttribute(appliedProtocolIdMap.get(appliedProtocolId), fieldName, value);
                }
            }    else {
                setAttribute(appliedProtocolIdMap.get(appliedProtocolId), name, value);            
            }

            count++;  
        }
        LOG.info("created " + count + " appliedProtocol data ");
        res.close();
    }

    /**
     * Return the rows needed for data from the appliedProtocol table.
     * Here we consider data that appear to be parameter of the protocol,
     * i.e. with heading 'Source Name' or 'Parameter Value' in the data table
     * in Chado.
     * UNION is used instead of IN clause just to have both 'name' and 'heading'
     * as 'name' in the result set.
     * This is a protected method so that it can be overridden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getAppliedProtocolDataResultSet(Connection connection) 
    throws SQLException {
        String query =
            "SELECT apd.applied_protocol_id, apd.data_id, d.name, d.value"
            + " FROM applied_protocol_data apd, data d"
            + " WHERE apd.data_id = d.data_id" 
            + " AND heading = 'Parameter Value'"
            + " UNION"
            + " SELECT apd.applied_protocol_id, apd.data_id, d.heading, d.value"
            + " FROM applied_protocol_data apd, data d"
            + " WHERE apd.data_id = d.data_id" 
            + " AND heading in ('Source Name', 'Hybridization Name')";

        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }


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
     * This is a protected method so that it can be overridden for testing
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

    /* DATA-----------------------------------------------------------------------------*/

    private void processDataTable(Connection connection)
    throws SQLException, ObjectStoreException {
        ResultSet res = getDataResultSet(connection);
        int count = 0;
        while (res.next()) {
            Integer dataId = new Integer(res.getInt("data_id"));
            String heading = res.getString("heading");
            String name = res.getString("name");
            String value = res.getString("value");
            Item data = getChadoDBConverter().createItem("SubmissionData");

            ClassDescriptor cd = 
                getChadoDBConverter().getModel().getClassDescriptorByName("SubmissionData");

            //For 'Result Value' heading we consider the name, otherwise the heading itself
            String test = null;
            if (heading.equalsIgnoreCase("Result Value")) {
                test = name;
            } else {
                test = heading;
            }
            if (cd.getAttributeDescriptorByName(test) == null) {       
                String fieldName = FIELD_NAME_MAP.get(test);
                if (fieldName == null) {  
                    LOG.error("NOT FOUND in FIELD_NAME_MAP: " + test + " [data]");
                } else {
                    data.setAttribute(fieldName, value);
                }
            } else {
                data.setAttribute(test, value);            
            }                
            Integer intermineObjectId = getChadoDBConverter().store(data);
            dataIdMap .put(dataId, intermineObjectId);

            count++;
        }
        LOG.info("created " + count + " submissionData");
        res.close();
    }


    /**
     * Return the rows needed from the data table.
     * Considering 'data' all that is not parameter of protocol (see before). 
     * This is a protected method so that it can be overridden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getDataResultSet(Connection connection) 
    throws SQLException {
        String query =
            "SELECT data_id, heading, name, value"
            + " FROM data"
            + " WHERE heading != 'Parameter Value'"
            + " AND heading != 'Source Name'"
            + " AND heading != 'Hybridization Name'"
            ;
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

}
