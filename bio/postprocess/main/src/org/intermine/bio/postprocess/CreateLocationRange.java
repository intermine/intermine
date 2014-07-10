package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.sql.Database;

/**
 * Add a column to the location table to hold a Postgres built in int8range type for use with range
 * queries. This is an alternative to BioSeg for fast range queries but requires Postgres 9.2 or
 * later.
 * @author Richard Smith
 *
 */
public class CreateLocationRange
{
    protected Database db;
    private static final String COLUMN_NAME = "intermine_locrange";
    private static final String RANGE_TYPE = "int8range";
    private static final Logger LOG = Logger.getLogger(CreateLocationRange.class);

    /**
     * Construct a new BiosegIndexTask that will change the given object store.
     *
     * @param osw
     *            an ObjectStore to write to
     */
    public CreateLocationRange(ObjectStoreWriter osw) {
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            this.db = ((ObjectStoreWriterInterMineImpl) osw).getDatabase();
        } else {
            throw new RuntimeException("the ObjectStoreWriter is not an "
                    + "ObjectStoreWriterInterMineImpl");
        }
    }

    /**
     * Create and populate the int8range column on location.
     * @throws SQLException if commands fail
     */
    public void create() throws SQLException {
        // create the range column if it doesn't exist
        createRangeColumn();

        // populate and index
        populateRangeColumn();
    }

    /**
     * Add a range column to the location table if it doesn't already exist.
     *
     * @throws SQLException
     *             if there is a problem add the column
     */
    private void createRangeColumn() throws SQLException {
        Connection con = db.getConnection();

        // first command will throw and SQLException if the column doesn't exist, if autocommit
        // is false the connection will give an error on next command.
        con.setAutoCommit(true);

        // check if column exists
        try {
            String testSql = "SELECT " + COLUMN_NAME + " FROM location LIMIT 1";
            Statement statement = con.createStatement();
            statement.execute(testSql);
            statement.close();
        } catch (SQLException e) {
            // error so column must not exist
            String sql = "ALTER TABLE location ADD COLUMN " + COLUMN_NAME + " " + RANGE_TYPE;
            Statement statement = con.createStatement();
            statement.executeUpdate(sql);
            statement.close();
        }
        con.close();
    }

    /**
     * Update the location table to populate range table.
     * @throws SQLException if commands fail
     */
    private void populateRangeColumn() throws SQLException {
        Connection con = db.getConnection();

        con.setAutoCommit(true);

        // populate range column
        long startTime = System.currentTimeMillis();
        String sql = "UPDATE location SET intermine_locrange = "
                + "int8range(intermine_start, intermine_end, '[]')";
        Statement statement = con.createStatement();
        statement.executeUpdate(sql);
        statement.close();
        long took = System.currentTimeMillis() - startTime;
        LOG.info("Populated location.intermine_locrange, took: " + took + "ms.");

        try {
            // TODO may need to check Postgres version, use SPGIST on 9.3 or later, otherwise GIST

            startTime = System.currentTimeMillis();
            String indexSql = "CREATE INDEX location__locatedon_locrange "
                    + "ON location USING GIST (locatedonid, intermine_locrange)";
            statement = con.createStatement();
            statement.executeUpdate(indexSql);
            statement.close();
            took = System.currentTimeMillis() - startTime;
            LOG.info("Created index on location.intermine_locrange, took: " + took + "ms.");
        } catch (SQLException e) {
            LOG.info("Index location_intermine_locrange already existed.");
        }
        con.close();

        // TODO store details of ranges defined in intermine_metadata table
        // TODO add a RangeDefinition class that marshals/unmarshals to text

    }
}
