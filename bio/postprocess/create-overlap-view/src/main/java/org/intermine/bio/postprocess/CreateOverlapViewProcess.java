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

import org.apache.log4j.Logger;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.sql.Database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.intermine.postprocess.PostProcessor;
import org.intermine.objectstore.ObjectStoreException;


/**
 * A task the replace the sequencefeatureoverlappingfeatures table with a view that uses the
 * int4range type to calculate the overlaps.
 * @author Kim Rutherford
 */
public class CreateOverlapViewProcess extends PostProcessor
{

    private static final Logger LOG = Logger.getLogger(CreateOverlapViewProcess.class);

    /**
     * Create a new instance
     *
     * @param osw object store writer
     */
    public CreateOverlapViewProcess(ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * {@inheritDoc}
     * <br/>
     * Main post-processing routine.
     * Drop the sequencefeatureoverlappingfeatures table and replace it with a view that
     * uses the int4range type to calculate the overlaps.
     *
     * @throws ObjectStoreException if the objectstore throws an exception
     */
    public void postProcess()
            throws ObjectStoreException {
        try {
            createView();
        } catch (SQLException e) {
            throw new RuntimeException("failed to create view " + e);
        }
    }

    /**
     * Drop the sequencefeatureoverlappingfeatures table and replace it with a view that
     * uses the int4range type to calculate the overlaps.
     * @throws SQLException if there is a problem dropping the table or creating the view
     */
    private void createView() throws SQLException {
        if (!(osw instanceof ObjectStoreWriterInterMineImpl)) {
            throw new RuntimeException("The ObjectStoreWriter is not an "
                    + "ObjectStoreWriterInterMineImpl");
        }

        Database db = ((ObjectStoreWriterInterMineImpl) osw).getDatabase();
        Connection con = db.getConnection();

        // autocommit as we may fail DROP TABLE and use same connection for DROP VIEW
        con.setAutoCommit(true);

        String viewSql =
                "CREATE VIEW overlappingfeaturessequencefeature "
                        + " AS SELECT l1.featureid AS overlappingfeatures, "
                        + "           l2.featureid AS sequencefeature "
                        + "      FROM location l1, location l2 "
                        + "     WHERE l1.locatedonid = l2.locatedonid "
                        + "       AND l1.featureid != l2.featureid"
                        + "       AND int4range(l1.intermine_start, l1.intermine_end + 1) "
                        + "           && int4range(l2.intermine_start, l2.intermine_end + 1)";

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
