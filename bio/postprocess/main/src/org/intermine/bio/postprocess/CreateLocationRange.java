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
import org.intermine.modelproduction.MetadataManager;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.intermine.RangeDefinitions;
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
    // postgres range types were added in version 9.2
    private static final String MIN_POSTGRES_VERSION = "9.2";

    /**
     * Construct a new CreateLocationRange
     *
     * @param osw an ObjectStore to fetch database from
     */
    public CreateLocationRange(ObjectStoreWriter osw) {
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            this.db = ((ObjectStoreWriterInterMineImpl) osw).getDatabase();
            if (!db.isVersionIsLeast(MIN_POSTGRES_VERSION)) {
                throw new IllegalArgumentException("Creating a range column on the location table"
                        + " requires at least PostgreSQL " + MIN_POSTGRES_VERSION + " but database"
                        + " has version: " + db.getVersion());
            }
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
     * Add a range column to the location table if it doesn't already exist
     *
     * @throws SQLException if there is a problem adding the column
     */
    private void createRangeColumn() throws SQLException {
        Connection con = db.getConnection();

        // first command will throw an SQLException if the column doesn't exist, if autocommit
        // is false the connection will give an error on next command.
        con.setAutoCommit(true);

        // check if column exists by trying to select from it
        try {
            String testSql = "SELECT " + COLUMN_NAME + " FROM location LIMIT 1";
            Statement statement = con.createStatement();
            statement.execute(testSql);
            statement.close();
        } catch (SQLException e) {
            // error so column must not exist, create it now
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
        // NOTE '[]' means that the start and end coordinates are included in the range
        String sql = "UPDATE location SET intermine_locrange = "
                + "int8range(intermine_start, intermine_end, '[]')";
        Statement statement = con.createStatement();
        statement.executeUpdate(sql);
        statement.close();
        long took = System.currentTimeMillis() - startTime;
        LOG.info("Populated location.intermine_locrange, took: " + took + "ms.");

        try {
            // SPGIST indexes are fastest for range queries but only added support for
            // ranges in 9.3, for older versions we need to use GIST
            String indexType = db.isVersionIsLeast("9.3") ? "SPGIST" : "GIST";

            startTime = System.currentTimeMillis();
            String indexSql = "CREATE INDEX location__locrange "
                    + "ON location USING " + indexType + " (intermine_locrange)";
            statement = con.createStatement();
            statement.executeUpdate(indexSql);
            statement.close();
            took = System.currentTimeMillis() - startTime;
            LOG.info("Created index on location.intermine_locrange, took: " + took + "ms.");
        } catch (SQLException e) {
            LOG.info("Index location_intermine_locrange already existed.");
        }
        con.close();

        // read range definitions from metadata table (may be empty)
        String rangeDefStr = MetadataManager.retrieve(db, MetadataManager.RANGE_DEFINITIONS);
        RangeDefinitions rangeDefs = new RangeDefinitions(rangeDefStr);

        // create a new range definition and store in metadata table
        rangeDefs.addRange("location", COLUMN_NAME, RANGE_TYPE, "start", "end");
        MetadataManager.store(db,  MetadataManager.RANGE_DEFINITIONS, rangeDefs.toJson());
    }
}
