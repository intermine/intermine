package org.flymine.postprocess;

import java.sql.*;
import java.util.Iterator;
import java.util.Collections;

import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;

import org.flymine.model.genomic.Contig;
import org.flymine.model.genomic.Sequence;



public class StoreSequences {

    protected ObjectStoreWriter osw;
    private Database db;

    public StoreSequences (ObjectStoreWriter osw, String dbAlias)
          throws SQLException, ClassNotFoundException {
          this.osw = osw;
          db = DatabaseFactory.getDatabase(dbAlias);
    }


    public void storeContigSequences() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Contig.class);
        q.addToSelect(qc);
        q.addFrom(qc);

        ObjectStore os = osw.getObjectStore();
        SingletonResults res = new SingletonResults(q, os, os.getSequence());
        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            osw.beginTransaction();
            Contig contig = (Contig) PostProcessUtil.cloneInterMineObject((Contig) resIter.next());
            String sequence = getSequence(contig.getIdentifier());
            Sequence seq = (Sequence) DynamicUtil.createObject(Collections.singleton(Sequence.class));
            seq.setResidues(sequence);
            contig.setSequence(seq);
            osw.store(contig);
            osw.store(seq);
            osw.commitTransaction();
        }
    }


    protected String getSequence(String contigId) throws SQLException{
        String sequence = null;

        Connection connection = db.getConnection();
        Statement statement = connection.createStatement();
        ResultSet rs = null;
        String query = null;
        query = "SELECT d.sequence FROM dna d, seq_region s where d.seq_region_id = s.seq_region_id and s.name = '"  + contigId + "'";

        rs = statement.executeQuery(query);
        while (rs.next()) {
            sequence =  rs.getString("sequence");
        }
        rs.close();

        return sequence;
    }

}

