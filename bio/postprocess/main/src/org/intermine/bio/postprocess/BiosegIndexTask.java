package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.sql.Database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A task to create a bioseg GIST index on the location table to help with overlap queries.
 * @author Kim Rutherford
 */
public class BiosegIndexTask
{
    protected ObjectStoreWriterInterMineImpl osw;

    /**
     * Construct a new BiosegIndexTask that will change the given object store.
     *
     * @param osw an ObjectStore to write to
     */
    public BiosegIndexTask(ObjectStoreWriter osw) {
        if (osw instanceof ObjectStoreWriterInterMineImpl) {
            this.osw = (ObjectStoreWriterInterMineImpl) osw;
        } else {
            throw new RuntimeException("the ObjectStoreWriter is not an "
                                       + "ObjectStoreWriterInterMineImpl");
        }
    }

    /**
     * Create a bioseg index on the location table.
     *
     * @throws SQLException if there is a problem dropping the tabe or creating the view
     */
    public void createIndex() throws SQLException {
        Database db = this.osw.getDatabase();
        Connection con = db.getConnection();

        con.setAutoCommit(false);

        String indexSql = "CREATE INDEX location_object_bioseg ON location "
            + "USING gist (locatedonid, bioseg_create(intermine_start, intermine_end))";

        Statement statement = con.createStatement();
        statement.executeUpdate(indexSql);
        statement.close();

        con.commit();
        con.close();
    }
}
