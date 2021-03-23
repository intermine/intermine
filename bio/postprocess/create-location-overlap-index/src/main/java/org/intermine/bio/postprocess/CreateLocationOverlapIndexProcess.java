package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2021 FlyMine
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
import org.intermine.sql.DatabaseConnectionException;

import org.intermine.postprocess.PostProcessor;
import org.intermine.objectstore.ObjectStoreException;



/**
 * Create an index on the location table to be used for range queries.
 * Uses the built-in int4range type.
 * @author Richard Smith
 */
public class CreateLocationOverlapIndexProcess extends PostProcessor
{

    private static final String RANGE_TYPE = "int4range";
    private static final Logger LOG = Logger.getLogger(CreateLocationOverlapIndexProcess.class);

    /**
     * Create a new instance
     *
     * @param osw object store writer
     */
    public CreateLocationOverlapIndexProcess(ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * {@inheritDoc}
     * <br/>
     * Main post-processing routine.
     * Create an int4range index on the start and end columns of the of the location
     * table. If Postgres is of version 9.3 or later this will be an SPGIST index.
     *
     * @throws ObjectStoreException if the objectstore throws an exception
     */
    public void postProcess()
            throws ObjectStoreException {

        if (!(osw instanceof ObjectStoreWriterInterMineImpl)) {
            throw new RuntimeException("The ObjectStoreWriter is not an "
                    + "ObjectStoreWriterInterMineImpl");
        }

        Database db = ((ObjectStoreWriterInterMineImpl) osw).getDatabase();
        Connection con = null;

        try {
            con = db.getConnection();

            if (hasIndexAlready(con)) {
                // index was already created during this build
                return;
            }
            String indexType = "SPGIST";

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
        } catch (DatabaseConnectionException e) {
            throw new RuntimeException("Cannot get database connection. " + e);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create location__" + RANGE_TYPE
                    + ". You likely have bad locations. ", e);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
            }
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
