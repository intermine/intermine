package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

/**
 * Create items from the modENCODE metadata extensions to the chado schema.
 * @author Kim Rutherford
 */
public class ModEncodeMetaDataChadoProcessor extends ChadoProcessor
{
    protected static final Logger LOG = Logger.getLogger(ModEncodeMetaDataChadoProcessor.class);
    private Map<Integer, Integer> protocolIdMap = new HashMap<Integer, Integer>();

    /**
     * Create a new ChadoModuleProcessor object.
     * @param chadoDBConverter the converter that created this Processor
     */
    public ModEncodeMetaDataChadoProcessor(ChadoDBConverter chadoDBConverter) {
        super(chadoDBConverter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Connection connection) throws Exception {
        processProtocolTable(connection);
    }

    private void processProtocolTable(Connection connection)
        throws SQLException, ObjectStoreException{
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

}
