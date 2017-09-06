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

import org.apache.log4j.Logger;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.sql.Database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A task the replace the sequencefeatureoverlappingfeatures table with a view that uses the
 * bioseg type to calculate the overlaps.
 * @author Kim Rutherford
 */
public class OverlapViewTask
{
    private static final Logger LOG = Logger.getLogger(OverlapViewTask.class);
    protected ObjectStoreWriterInterMineImpl osw;

    /**
     * Construct a new OverlapViewTask that will change the given object store.
     * @param osw an ObjectStore to write to
     */
    public OverlapViewTask(ObjectStoreWriter osw) {
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            this.osw = (ObjectStoreWriterInterMineImpl) osw;
        } else {
            throw new RuntimeException("the ObjectStoreWriter is not an "
                                       + "ObjectStoreWriterInterMineImpl");
        }
    }

    /**
     * Drop the sequencefeatureoverlappingfeatures table and replace it with a view that
     * uses the bioseg type to calculate the overlaps.
     * @throws SQLException if there is a problem dropping the table or creating the view
     */
    public void createView() throws SQLException {
        Database db = this.osw.getDatabase();
        Connection con = db.getConnection();

        // autocommit as we may fail DROP TABLE and use same connection for DROP VIEW
        con.setAutoCommit(true);

        String viewSql;
        if (osw.getSchema().useRangeTypes()) {
            viewSql =
                    "CREATE VIEW overlappingfeaturessequencefeature "
                            + " AS SELECT l1.featureid AS overlappingfeatures, "
                            + "           l2.featureid AS sequencefeature "
                            + "      FROM location l1, location l2 "
                            + "     WHERE l1.locatedonid = l2.locatedonid "
                            + "       AND l1.featureid != l2.featureid"
                            + "       AND int4range(l1.intermine_start, l1.intermine_end + 1) "
                            + "           && int4range(l2.intermine_start, l2.intermine_end + 1)";
        } else if (osw.getSchema().hasBioSeg()) {
            viewSql =
                    "CREATE VIEW overlappingfeaturessequencefeature "
                            + " AS SELECT l1.featureid AS overlappingfeatures, "
                            + "           l2.featureid AS sequencefeature "
                            + "      FROM location l1, location l2 "
                            + "     WHERE l1.locatedonid = l2.locatedonid "
                            + "       AND l1.featureid != l2.featureid"
                            + "       AND bioseg_create(l1.intermine_start, l1.intermine_end) "
                            + "            && bioseg_create(l2.intermine_start, l2.intermine_end)";
        } else {
            throw new IllegalArgumentException("Attempt to create overlappingfeatures view but"
                    + " database doesn't support Postgres built in ranges (has to be > 9.2"
                    + " and doesn't have bioseg installed. Aborting.");
        }

        LOG.info("Creating overlap view with SQL: " + viewSql);

        // initially this is a table, need to try dropping table first, if the postprocess has been
        // run before then it will be a view. We need to try dropping table first then view.
        String dropSql = "DROP TABLE overlappingfeaturessequencefeature";
        try {
            Statement statement = con.createStatement();
            statement.executeUpdate(dropSql);
            statement.close();
        } catch (SQLException e) {
            // if the postprocess has already been run will be a view
            dropSql = "DROP VIEW overlappingfeaturessequencefeature";
            Statement statement = con.createStatement();
            statement.executeUpdate(dropSql);
            statement.close();
        }

        Statement statement = con.createStatement();
        statement.executeUpdate(viewSql);
        statement.close();

        con.close();
    }
}
