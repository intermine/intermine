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

    private static final Map<String, String> FIELD_NAME_MAP =
        new HashMap<String, String>();
    
    static {
        FIELD_NAME_MAP.put("Protocol Type", "type");

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
        
        processProtocolTable(connection);
        processProtocolAttributes(connection);        
    }

    /* PROTOCOL */
    
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
     * This is a protected method so that it can be overriden for testing
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
     * This is a protected method so that it can be overriden for testing
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
    
    
    /* EXPERIMENT */
    
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
     * This is a protected method so that it can be overriden for testing
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
     * This is a protected method so that it can be overriden for testing
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
    
    
}
