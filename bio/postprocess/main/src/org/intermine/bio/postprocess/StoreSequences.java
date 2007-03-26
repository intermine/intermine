package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.*;
import java.util.Iterator;
import java.util.Collections;

import org.intermine.sql.Database;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;

import org.flymine.model.genomic.Contig;
import org.flymine.model.genomic.Sequence;

/**
 * Fetch sequences from the ensembl human src db and store them via ObjectStore
 * Temp fix for ensembl human
 *
 * @author Wenyan Ji
 * @author Richard Smith
 */

public class StoreSequences
{

    protected ObjectStoreWriter osw;
    private Database db;


    /**
     * Create a new StoreSequences object from the given ObjectStoreWriter
     * @param osw writer on genomic ObjectStore
     * @param db database
     */
    public StoreSequences (ObjectStoreWriter osw, Database db) {
          this.osw = osw;
          this.db = db;
    }


    /**
     * Iterator through all the contigs in the ObjectStore, fetch every sequence and store it
     * in the ObjectStore
     * @throws Exception if there are any problems
     */
    public void storeContigSequences() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Contig.class);
        q.addToSelect(qc);
        q.addFrom(qc);

        ObjectStore os = osw.getObjectStore();
        SingletonResults res = new SingletonResults(q, os, os.getSequence());

        Connection connection = db.getConnection();
        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            osw.beginTransaction();
            Contig contig = (Contig) PostProcessUtil.cloneInterMineObject((Contig) resIter.next());

            String sequence = getSequence(connection, contig.getIdentifier());
            Sequence seq = (Sequence) DynamicUtil.createObject(
                             Collections.singleton(Sequence.class));
            seq.setResidues(sequence);
            contig.setSequence(seq);
            osw.store(contig);
            osw.store(seq);
            osw.commitTransaction();
        }
    }


    /**
     * Get contig sequences from ensembl human src db by contigId
     * @param connection the Connection to use when creating Statement objects
     * @param contigId the id for the contig
     * @throws SQLException if there are any problems
     * @return a sequence for this contig
     */
    protected String getSequence(Connection connection, String contigId) throws SQLException {
        String sequence = null;
        
        Statement statement = connection.createStatement();
        ResultSet rs = null;
        String query = null;
        query = "SELECT d.sequence FROM dna d, seq_region s "
              + "WHERE d.seq_region_id = s.seq_region_id and s.name = '"
              + contigId + "'";

        rs = statement.executeQuery(query);
        while (rs.next()) {
            sequence =  rs.getString("sequence");
        }
        rs.close();

        return sequence;
    }

}

