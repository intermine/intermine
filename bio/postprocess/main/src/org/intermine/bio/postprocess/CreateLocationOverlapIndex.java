package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2016 FlyMine
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

import org.apache.log4j.Logger;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.sql.Database;


/**
* Create an index on the location table to be used for range queries. If Postgres is version 9.2 or
* later this will use the built-in int4range type, otherwise it will use BioSeg if installed. If
* neither of these conditions are met an exception will be thrown.
* @author Richard Smith
*
*/
public class CreateLocationOverlapIndex
{
    protected ObjectStoreWriterInterMineImpl osw;
    private static final String RANGE_TYPE = "int4range";
    private static final Logger LOG = Logger.getLogger(CreateLocationOverlapIndex.class);

    /**
     * Construct a new CreateLocationRange
     *
     * @param osw
     *            an ObjectStore to fetch database from
     */
    public CreateLocationOverlapIndex(ObjectStoreWriter osw) {
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            this.osw = (ObjectStoreWriterInterMineImpl) osw;
        } else {
            throw new RuntimeException("The ObjectStoreWriter is not an "
                                       + "ObjectStoreWriterInterMineImpl");
        }
    }

    /**
     * Create an int4range or bioseg index on the start and end columns of the of the location
     * table. If Postgres is of version 9.3 or later this will be an SPGIST index, otherwise GIST.
     *
     * @throws SQLException
     *             if commands fail
     */
    public void create() throws SQLException {
        Database db = this.osw.getDatabase();



        if (osw.getSchema().useRangeTypes()) {
            Connection con = db.getConnection();

            try {
                if (hasIndexAlready(con)) {
                    // index was already created during this build
                    return;
                }

                // SPGIST indexes are fastest for range queries but only added support for
                // ranges in 9.3, for older versions we need to use GIST
                String indexType = db.isVersionAtLeast("9.3") ? "SPGIST" : "GIST";

                long startTime = System.currentTimeMillis();
                String indexSql = "CREATE INDEX location__int4range "
                        + "ON location USING " + indexType + " (" + RANGE_TYPE
                        + "(intermine_start, intermine_end + 1))";
                LOG.info(indexSql);
                Statement statement = con.createStatement();
                statement.executeUpdate(indexSql);
                statement.close();
                long took = System.currentTimeMillis() - startTime;
                LOG.info("Created " + RANGE_TYPE + " index on location, took: " + took + "ms.");
            } catch (SQLException e) {
                throw new SQLException("Failed to create location__" + RANGE_TYPE
                        + ". You likely have bad locations. ", e);
            } finally {
                con.close();
            }
        } else if (osw.getSchema().hasBioSeg()) {
            // We have bioseg but don't support built-in range queries

            Connection con = db.getConnection();
            try {
                con.setAutoCommit(false);

                String indexSql = "CREATE INDEX location__bioseg ON location "
                    + "USING gist (bioseg_create(intermine_start, intermine_end))";

                Statement statement = con.createStatement();
                statement.executeUpdate(indexSql);
                statement.close();

                con.commit();
            } finally {
                con.close();
            }
        } else {
            throw new IllegalArgumentException("Attempt to create index for range queries on"
                    + " location table but database doesn't support Postgres built in ranges (has"
                    + " to be > 9.2) and doesn't have bioseg installed. Aborting.");
        }
    }

    private boolean hasIndexAlready(Connection con) throws SQLException {
        final String sql = "SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = "
                + "c.relnamespace WHERE c.relname = 'location__int4range' "
                + "AND n.nspname = 'public'";
        Statement statement = con.createStatement();
        ResultSet res = statement.executeQuery(sql);
        // true if index is present, false if this query returned no rows
        boolean hasIndex = res.next();
        statement.close();
        return hasIndex;
    }
}
