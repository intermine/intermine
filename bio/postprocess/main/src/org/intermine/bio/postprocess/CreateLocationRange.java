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
    private static final String RANGE_TYPE = "int4range";
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
            if (!db.isVersionAtLeast(MIN_POSTGRES_VERSION)) {
                throw new IllegalArgumentException("Creating an int4range index on the location "
                        + " table requires at least PostgreSQL " + MIN_POSTGRES_VERSION
                        + " but this database has version: " + db.getVersion());
            }
        } else {
            throw new RuntimeException("the ObjectStoreWriter is not an "
                    + "ObjectStoreWriterInterMineImpl");
        }
    }

    /**
     * Create an int4range index on the start and end columns of the location table. If Postgres is
     * of version 9.3 or later this will be an SPGIST index, otherwise GIST.
     * @throws SQLException if commands fail
     */
    public void create() throws SQLException {
        Connection con = db.getConnection();

        try {
            // SPGIST indexes are fastest for range queries but only added support for
            // ranges in 9.3, for older versions we need to use GIST
            String indexType = db.isVersionAtLeast("9.3") ? "SPGIST" : "GIST";

            long startTime = System.currentTimeMillis();
            String indexSql = "CREATE INDEX location__int4range " + "ON location USING "
                    + indexType + " (" + RANGE_TYPE + "(intermine_start, intermine_end))";
            LOG.info(indexSql);
            Statement statement = con.createStatement();
            statement.executeUpdate(indexSql);
            statement.close();
            long took = System.currentTimeMillis() - startTime;
            LOG.info("Created " + RANGE_TYPE + " index on location, took: " + took + "ms.");
        } catch (SQLException e) {
            LOG.info("Index location__" + RANGE_TYPE + " index already existed.");
        }
        con.close();
    }
}
