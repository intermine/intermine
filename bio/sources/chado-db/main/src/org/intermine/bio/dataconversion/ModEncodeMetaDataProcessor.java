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
import java.util.HashMap;
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
    private Map<Integer, Integer> experimentIdMap = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> providerIdMap = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> dataIdMap = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> appliedProtocolIdMap = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> appliedProtocolDataIdMap = new HashMap<Integer, Integer>();
    private Map<Integer, Integer> appliedProtocolProtocolIdMap = new HashMap<Integer, Integer>();

    private static final Map<String, String> FIELD_NAME_MAP =
        new HashMap<String, String>();

    private static final Map<String, String> PROVIDER_FIELD_NAME_MAP =
        new HashMap<String, String>();
    
    static {
        //protocol
        FIELD_NAME_MAP.put("Protocol Type", "type");
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
    }

    static {        
        PROVIDER_FIELD_NAME_MAP.put("Person Last Name", "lab");
        PROVIDER_FIELD_NAME_MAP.put("Person Affiliation", "affiliation");       
    }

    
    
    /**
     * Create a new ChadoModuleProcessor object.
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

        processExperimentTable(connection);
        processExperimentAttributes(connection);
        
        processProviderTable(connection);
        processProviderAttributes(connection);

        processProtocolTable(connection);
        processProtocolAttributes(connection);        

        processAppliedProtocolTable(connection);
        processAppliedProtocolData(connection);        
        processAppliedProtocolDataAttributes(connection);
                
        processDataTable(connection);
        //processDataAttributes(connection);
        //processDataTableAgain(connection);
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
//            String type = res.getString("type");
            Item protocol = getChadoDBConverter().createItem("Protocol");
            protocol.setAttribute("name", name);
            protocol.setAttribute("description", description);
            //protocol.setAttribute("type", type);
            Integer intermineObjectId = getChadoDBConverter().store(protocol);
            protocolIdMap .put(protocolId, intermineObjectId);
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
        LOG.error("NOT FOUND: " + heading + " has no mapping in FIELD_NAME_MAP");
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
        Integer intermineObjectId = getChadoDBConverter().store(experiment);
        experimentIdMap .put(experimentId, intermineObjectId);
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
        LOG.error("NOT FOUND: " + heading + " has no mapping in FIELD_NAME_MAP");
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
    

    /* PROVIDER -------------------------------------------------------------------------*/
 
    private void processProviderTable(Connection connection)
    throws SQLException, ObjectStoreException {
    ResultSet res = getProviderResultSet(connection);
    int count = 0;
    while (res.next()) {
        //Integer providerId = new Integer(res.getInt("provider_id"));
       Integer experimentId = new Integer(res.getInt("experiment_id"));
       //String heading = res.getString("name");
        String value = res.getString("value");
        Item provider = getChadoDBConverter().createItem("Provider");

        /*
        String fieldName = PROVIDER_FIELD_NAME_MAP.get(heading);
        if (fieldName == null) { 
        LOG.error("NOT FOUND in PROVIDER_FIELD_NAME_MAP: " + heading);
        continue;
        }
*/
        
        provider.setAttribute("lab", value);
        Integer intermineObjectId = getChadoDBConverter().store(provider);
        providerIdMap .put(experimentId, intermineObjectId);
        count++;
    }
    LOG.info("created " + count + " providers");
    res.close();
}

    /**
     * Return the rows needed from the provider table.
     * NB: for the moment not using the uniquename, but the name from the 
     * provider_prop table
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
            //Integer providerId = new Integer(res.getInt("provider_id"));
            Integer experimentId = new Integer(res.getInt("experiment_id"));
            String heading = res.getString("name");
            String value = res.getString("value");
            
            String fieldName = PROVIDER_FIELD_NAME_MAP.get(heading);
            if (fieldName == null) { 
            LOG.error("NOT FOUND in PROVIDER_FIELD_NAME_MAP: " + heading);
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
    
    /* DATA OLD-----------------------------------------------------------------------------*/
    
    private void processDataTable2(Connection connection)
    throws SQLException, ObjectStoreException {
    ResultSet res = getDataResultSet2(connection);
    int count = 0;
    while (res.next()) {
        Integer dataId = new Integer(res.getInt("data_id"));
        String name = res.getString("name");
        String value = res.getString("value");
        Item data = getChadoDBConverter().createItem("SubmissionData");
        //experiment.setAttribute("name", name);
        //Integer intermineObjectId = getChadoDBConverter().store(data);
        //dataIdMap .put(dataId, intermineObjectId);
        //count++;
              
        
        ClassDescriptor cd = 
            getChadoDBConverter().getModel().getClassDescriptorByName("SubmissionData");
        
        if (cd.getAttributeDescriptorByName(name) == null) {
            String fieldName = FIELD_NAME_MAP.get(name);
            if (fieldName == null) { 
                LOG.error("NOT FOUND in FIELD_NAME_MAP: " + name);
                } else {
            data.setAttribute(fieldName, value);
        }
        }    else {
            data.setAttribute(name, value);            
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
     * NB: for the moment not using the uniquename, but the name from the 
     * data_prop table
     * This is a protected method so that it can be overridden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getDataResultSet2(Connection connection) 
    throws SQLException {
        String query =
            "SELECT data_id, name, value"
            + " FROM data"
            + " WHERE heading = 'Parameter Value'"
            + " UNION"
            + " SELECT data_id, heading, value"
            + " FROM data"
            + " WHERE heading = 'Source Name'";
        
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    private void processDataAttributes2(Connection connection)
    throws SQLException, ObjectStoreException {
    ResultSet res = getDataAttributesResultSet2(connection);
    int count = 0;
    while (res.next()) {
        Integer dataId = new Integer(res.getInt("data_id"));

        String heading = res.getString("heading");
        String value = res.getString("value");
        String fieldName = FIELD_NAME_MAP.get(heading);
        if (fieldName == null) { 
        LOG.error("NOT FOUND: " + heading + " has no mapping in FIELD_NAME_MAP");
        continue;
        }
        
        setAttribute(dataIdMap.get(dataId), fieldName, value);
        
        count++;
    }
    LOG.info("created " + count + " data attributes");
    res.close();
}

    /**
     * Return the rows needed for data from the data_prop table.
     * This is a protected method so that it can be overridden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getDataAttributesResultSet2(Connection connection) 
    throws SQLException {
        String query =
            "select da.data_id, a.heading, a.value"
            + " from data_attribute da, attribute a"
            + " where"
            + " da.attribute_id = a.attribute_id"
            + " order by da.data_id";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }
    

    private void processDataTableAgain2(Connection connection)
    throws SQLException, ObjectStoreException {
    ResultSet res = getDataResultSetAgain2(connection);
    int count = 0;
    while (res.next()) {
        Integer dataId = new Integer(res.getInt("data_id"));
        String name = res.getString("heading");
        String value = res.getString("value");
        Item data = getChadoDBConverter().createItem("SubmissionData");
        //experiment.setAttribute("name", name);
        //Integer intermineObjectId = getChadoDBConverter().store(data);
        //dataIdMap .put(dataId, intermineObjectId);
        //count++;
              
        
        ClassDescriptor cd = 
            getChadoDBConverter().getModel().getClassDescriptorByName("SubmissionData");
        
        if (cd.getAttributeDescriptorByName(name) == null) {
            String fieldName = FIELD_NAME_MAP.get(name);
            if (fieldName == null) { 
                LOG.error("NOT FOUND in FIELD_NAME_MAP: " + name);
                } else {
            data.setAttribute(fieldName, value);
        }
        }    else {
            data.setAttribute(name, value);            
        }

        Integer intermineObjectId = getChadoDBConverter().store(data);
        //dataIdMap .put(dataId, intermineObjectId);

        count++;
    }
    LOG.info("created " + count + " submissionData");
    res.close();
}
    
    
    /**
     * Return the rows needed from the data table.
     * NB: for the moment not using the uniquename, but the name from the 
     * data_prop table
     * This is a protected method so that it can be overridden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getDataResultSetAgain2(Connection connection) 
    throws SQLException {
        String query =
            "SELECT data_id, heading, value"
            + " FROM data"
            + " WHERE heading != 'Parameter Value'"
            + " AND heading != 'Source Name'";
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
        Integer dataId = new Integer(res.getInt("data_id"));
        String direction = res.getString("direction");
        //String value = res.getString("value");
        Item appliedProtocol = getChadoDBConverter().createItem("AppliedProtocol");
        appliedProtocol.setAttribute("direction", direction);
        Integer intermineObjectId = getChadoDBConverter().store(appliedProtocol);
        appliedProtocolIdMap .put(appliedProtocolId, intermineObjectId);
        appliedProtocolProtocolIdMap .put(protocolId, intermineObjectId);
        appliedProtocolDataIdMap .put(dataId, intermineObjectId);
        count++;
              
        
    }
    LOG.info("created " + count + " appliedProtocol");
    res.close();
}
    
    
    /**
     * Return the rows needed from the data table.
     * NB: for the moment not using the uniquename, but the name from the 
     * data_prop table
     * This is a protected method so that it can be overridden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getAppliedProtocolResultSet(Connection connection) 
    throws SQLException {
        String query =
            "SELECT ap.applied_protocol_id, ap.protocol_id, apd.data_id, apd.direction"
            + " FROM applied_protocol ap, applied_protocol_data apd"
            + " WHERE apd.applied_protocol_id = ap.applied_protocol_id";
   /*     
        "SELECT eap.experiment_id"
        + " ,ap.applied_protocol_id, ap.protocol_id, apd.data_id, apd.direction"
        + " FROM applied_protocol_data apd"
        + " applied_protocol ap LEFT JOIN experiment_applied_protocol eap"
        + " ON (eap.first_applied_protocol_id = ap.applied_protocol_id )"
        + " WHERE apd.applied_protocol_id = ap.applied_protocol_id";
*/

            
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    private void processAppliedProtocolData(Connection connection)
    throws SQLException, ObjectStoreException {
    ResultSet res = getAppliedProtocolDataResultSet(connection);
    int count = 0;
    while (res.next()) {
        Integer dataId = new Integer(res.getInt("data_id"));

        String name = res.getString("name");
        String value = res.getString("value");
        //String fieldName = FIELD_NAME_MAP.get(name);

        /*
        if (fieldName == null) { 
        LOG.error("NOT FOUND: " + heading + " has no mapping in FIELD_NAME_MAP");
        continue;
        }
        setAttribute(dataIdMap.get(dataId), fieldName, value);      
        count++;
*/
    
        
        ClassDescriptor cd = 
            getChadoDBConverter().getModel().getClassDescriptorByName("AppliedProtocol");
        
        if (cd.getAttributeDescriptorByName(name) == null) {
            String fieldName = FIELD_NAME_MAP.get(name);
            if (fieldName == null) { 
                LOG.error("NOT FOUND in FIELD_NAME_MAP: " + name);
                } else {
            setAttribute(appliedProtocolDataIdMap.get(dataId), fieldName, value);
        }
        }    else {
            setAttribute(appliedProtocolDataIdMap.get(dataId), name, value);            
        }

        //Integer intermineObjectId = getChadoDBConverter().store(data);
        //dataIdMap .put(appliedProtocolId, intermineObjectId);

        count++;  
    }
    LOG.info("created " + count + " protocol data ");
    res.close();
}

        
    /**
     * Return the rows needed for data from the data_prop table.
     * This is a protected method so that it can be overridden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getAppliedProtocolDataResultSet(Connection connection) 
    throws SQLException {

        String query =
        "SELECT data_id, name, value"
        + " FROM data"
        + " WHERE heading = 'Parameter Value'"
        + " UNION"
        + " SELECT data_id, heading, value"
        + " FROM data"
        + " WHERE heading = 'Source Name'";

        
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
        Integer dataId = new Integer(res.getInt("data_id"));

        String heading = res.getString("heading");
        String value = res.getString("value");
        String fieldName = FIELD_NAME_MAP.get(heading);
        if (fieldName == null) { 
        LOG.error("NOT FOUND: " + heading + " has no mapping in FIELD_NAME_MAP");
        continue;
        }
        
        setAttribute(appliedProtocolDataIdMap.get(dataId), fieldName, value);
        
        count++;
    }
    LOG.info("created " + count + " data attributes");
    res.close();
}

    /**
     * Return the rows needed for data from the data_prop table.
     * This is a protected method so that it can be overridden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getAppliedProtocolDataAttributesResultSet(Connection connection) 
    throws SQLException {
        String query =
            "select da.data_id, a.heading, a.value"
            + " from data_attribute da, attribute a"
            + " where"
            + " da.attribute_id = a.attribute_id"
            + " order by da.data_id";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }
    

    
    
/*
    private void processAppliedProtocolDataTableAgain(Connection connection)
    throws SQLException, ObjectStoreException {
    ResultSet res = getDataResultSetAgain(connection);
    int count = 0;
    while (res.next()) {
        Integer dataId = new Integer(res.getInt("data_id"));
        String name = res.getString("heading");
        String value = res.getString("value");
        //Item data = getChadoDBConverter().createItem("SubmissionData");
        //experiment.setAttribute("name", name);
        //Integer intermineObjectId = getChadoDBConverter().store(data);
        //dataIdMap .put(dataId, intermineObjectId);
        //count++;
              
        
        ClassDescriptor cd = 
            getChadoDBConverter().getModel().getClassDescriptorByName("AppliedProtocol");
        
        if (cd.getAttributeDescriptorByName(name) == null) {
            String fieldName = FIELD_NAME_MAP.get(name);
            if (fieldName == null) { 
                LOG.error("NOT FOUND in FIELD_NAME_MAP: " + name);
                } else {
            setAttribute(appliedProtocolDataIdMap.get(dataId), fieldName, value);
        }
        }    else {
            setAttribute(appliedProtocolDataIdMap.get(dataId), name, value);            
        }
        count++;
    }
    LOG.info("created " + count + " submissionData");
    res.close();
}
  */  
    
    /**
     * Return the rows needed from the data table.
     * NB: for the moment not using the uniquename, but the name from the 
     * data_prop table
     * This is a protected method so that it can be overridden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getAppliedProtocolDataResultSetAgain(Connection connection) 
    throws SQLException {
        String query =
            "SELECT data_id, heading, value"
            + " FROM data"
            + " WHERE heading != 'Parameter Value'"
            + " AND heading != 'Source Name'";
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
        
        if (cd.getAttributeDescriptorByName(heading) == null) {
            String fieldName = FIELD_NAME_MAP.get(heading);
            if (fieldName == null && name != null) {                 
                if (cd.getAttributeDescriptorByName(name) == null) {       
                    fieldName = FIELD_NAME_MAP.get(name);
                    if (fieldName == null) {  
                        LOG.error("NOT FOUND in FIELD_NAME_MAP: " + name);
                    } else {
                        data.setAttribute(fieldName, value);
                    }
                } else {
                data.setAttribute(name, value);            
            }
        } else {
            data.setAttribute(fieldName, value);     
        }
        }else {
            data.setAttribute(heading, value);     
        }
        /*
        if (cd.getAttributeDescriptorByName(heading) == null) {
            String fieldName = FIELD_NAME_MAP.get(name);
            if (fieldName == null) { 
                LOG.error("NOT FOUND in FIELD_NAME_MAP: " + name);
                } else {
            data.setAttribute(fieldName, value);
        }
        }    else {
            data.setAttribute(name, value);            
        }
*/
        
        
        Integer intermineObjectId = getChadoDBConverter().store(data);
        dataIdMap .put(dataId, intermineObjectId);

        count++;
    }
    LOG.info("created " + count + " submissionData");
    res.close();
}
    
    
    /**
     * Return the rows needed from the data table.
     * NB: for the moment not using the uniquename, but the name from the 
     * data_prop table
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
            + " AND heading != 'Source Name'";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }
    
    
    private void processDataAttributes(Connection connection)
    throws SQLException, ObjectStoreException {
    ResultSet res = getDataAttributesResultSet(connection);
    int count = 0;
    while (res.next()) {
        Integer dataId = new Integer(res.getInt("data_id"));

        String heading = res.getString("heading");
        String value = res.getString("value");
        String fieldName = FIELD_NAME_MAP.get(heading);
        if (fieldName == null) { 
        LOG.error("NOT FOUND: " + heading + " has no mapping in FIELD_NAME_MAP");
        continue;
        }
        
        setAttribute(dataIdMap.get(dataId), fieldName, value);
        
        count++;
    }
    LOG.info("created " + count + " data attributes");
    res.close();
}

    /**
     * Return the rows needed for data from the data_prop table.
     * This is a protected method so that it can be overridden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getDataAttributesResultSet(Connection connection) 
    throws SQLException {
        String query =
            "select da.data_id, a.heading, a.value"
            + " from data_attribute da, attribute a"
            + " where"
            + " da.attribute_id = a.attribute_id"
            + " order by da.data_id";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }
    
/*
    private void processDataTableAgain(Connection connection)
    throws SQLException, ObjectStoreException {
    ResultSet res = getDataResultSetAgain(connection);
    int count = 0;
    while (res.next()) {
        Integer dataId = new Integer(res.getInt("data_id"));
        String name = res.getString("heading");
        String value = res.getString("value");
        Item data = getChadoDBConverter().createItem("SubmissionData");
        //experiment.setAttribute("name", name);
        //Integer intermineObjectId = getChadoDBConverter().store(data);
        //dataIdMap .put(dataId, intermineObjectId);
        //count++;
              
        
        ClassDescriptor cd = 
            getChadoDBConverter().getModel().getClassDescriptorByName("SubmissionData");
        
        if (cd.getAttributeDescriptorByName(name) == null) {
            String fieldName = FIELD_NAME_MAP.get(name);
            if (fieldName == null) { 
                LOG.error("NOT FOUND in FIELD_NAME_MAP: " + name);
                } else {
            data.setAttribute(fieldName, value);
        }
        }    else {
            data.setAttribute(name, value);            
        }

        Integer intermineObjectId = getChadoDBConverter().store(data);
        //dataIdMap .put(dataId, intermineObjectId);

        count++;
    }
    LOG.info("created " + count + " submissionData");
    res.close();
}
    
    

  */  
    
    
    
    
    
}
