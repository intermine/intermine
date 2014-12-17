/**
 * 
 */
package org.intermine.bio.postprocess;

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.bio.util.Constants;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.ProteinFamily;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.postprocess.PostProcessor;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.postgresql.copy.CopyManager;

/**
 * @author jcarlson
 *
 */
public class PhytozomeHomologPostProcess extends PostProcessor {

  String methodIds;

  private static final Logger LOG = Logger.getLogger(PhytozomeHomologPostProcess.class);

  // the map of PAC transcript id -> (organismid,geneid)
  HashMap<Integer,GeneInfo> geneIdMap;
  // the map of clusterId to proteinfamilyid
  HashMap<Integer,Integer> familyIdMap;
  // tree id to tree map
  HashMap<Integer,String> treeIdMap;
  Connection pacConnection;

  public PhytozomeHomologPostProcess(ObjectStoreWriter osw) {
    super(osw);
    this.osw = osw;
    geneIdMap = new HashMap<Integer,GeneInfo>();
    familyIdMap = new HashMap<Integer,Integer>();
    treeIdMap = new HashMap<Integer,String>();
    pacConnection = null;
  }

  public void postProcess() throws BuildException, ObjectStoreException {
    if (methodIds==null) {
      LOG.error("Method Ids are not set.");
      throw new BuildException("Method Ids are not set.");
    }

    CopyManager copyManager = null;
    org.postgresql.PGConnection conn;
    ObjectStoreWriterInterMineImpl mm = (ObjectStoreWriterInterMineImpl)osw;
    try {
      conn = (org.postgresql.PGConnection) mm.getConnection();
      copyManager = conn.getCopyAPI();
    } catch (SQLException e) {
      LOG.error("Error getting CopyManager: "+e.getMessage());
      throw new BuildException("Error getting CopyManager: "+e.getMessage());
    } 

    LOG.info("Getting genes and family ids...");
    fillGeneIdHash();
    fillFamilyIdHash();
    fillTreeIdHash();

    // the innie and the outie of the COPY data
    PipedWriter dbOut = new PipedWriter();
    PipedReader dbIn;
    try {
      dbIn = new PipedReader(dbOut);
    } catch (IOException e1) {
      LOG.error("Error getting PipedReader: "+e1.getMessage());
      throw new BuildException("Error getting PipedReader: "+e1.getMessage());
    }
    // run the COPY process in a separate thread
    Thread copyThread = new Thread( new CopyThread(copyManager,dbIn));
    copyThread.start();

    int recCtr = 0;
    int insCtr = 0;
    boolean keepGoing = true;
    while (keepGoing) {
      ResultSet rs = getHomologData(recCtr);
      keepGoing = false;
      try {
        while (rs.next()) {
          // got something!
          keepGoing = true;
          recCtr++;
          // what the copy manager is expecting.
          // organism1id,gene1id,organism2id,gene2id,clusterid,type,relationship,bootscore,tree
          // and what the query is giving us:
          // t1.proteomeId as proteome_1,t1.id as gene_1,
          // t2.proteomeId as proteome_2,t2.id as gene_2,
          // r.name as relationship,bootscore, clusterId,t.name as tree_type, tree
          Integer t1 = rs.getInt(1);
          if (rs.wasNull()) {
            throw new BuildException("Gene Id cannot be null");
          }
          Integer t2 = rs.getInt(2);
          if (rs.wasNull()) {
            throw new BuildException("Gene Id cannot be null");
          }
          String relationship = rs.getString(3);
          if (rs.wasNull()) {
            relationship = null;
          }
          Double bootscore = rs.getDouble(4);
          if (rs.wasNull()) {
            bootscore = null;
          }
          Integer cluster = rs.getInt(5);
          if (rs.wasNull()) {
            cluster = null;
          }
          String tree_type = rs.getString(6);
          if (rs.wasNull()) {
            tree_type = null;
          }
          String tree = null;
          Integer treeId = rs.getInt(7);
          if (rs.wasNull()) {
            tree = null;
          } else if (treeIdMap.containsKey(treeId) ) {
            tree = treeIdMap.get(treeId);          
          }
          if (geneIdMap.containsKey(t1) && geneIdMap.containsKey(t2) ) {      
            // start with a null
            String clusterId = "\\N";
            // and try to fill
            if (cluster != null && familyIdMap.containsKey(cluster)) {
              clusterId = familyIdMap.get(cluster).toString();
            }
            try {
              //TODO: is writing binary faster?
              dbOut.write(
                  geneIdMap.get(t1).orgId().toString()+"\t" +
                      geneIdMap.get(t1).geneId().toString()+"\t"+
                      geneIdMap.get(t2).orgId().toString()+"\t" +
                      geneIdMap.get(t2).geneId().toString()+"\t"+
                      clusterId+"\t"+
                      ((tree_type!=null)?tree_type:"\\N")+"\t"+
                      ((relationship!=null)?relationship:"\\N")+"\t"+
                      ((bootscore!=null)?bootscore.toString():"\\N")+"\t"+
                      ((tree!=null)?tree:"\\N")+"\n");
              insCtr++;
            } catch (IOException e) {
              LOG.error("Trouble writing to SQL pipe: "+e.getMessage());
              throw new BuildException("Trouble writing to SQL pipe: "+e.getMessage());
            }
          }
          if ((recCtr%100000)==0) LOG.info("Processed "+recCtr+" records with "+
              insCtr+" insertions...");
        }
        rs.close();
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    LOG.info("Processed "+recCtr+" records. "+insCtr+" records inserted.");
    try {
      dbOut.flush();
      dbOut.close();
    } catch (IOException e1) {
      LOG.error("Problem closing PipedWriter: "+e1.getMessage());
      throw new BuildException("Problem closing PipedWriter: "+e1.getMessage());
    }
    // make sure the writer thread is done
    while ( copyThread.getState() != Thread.State.TERMINATED){
      LOG.info("Writer thread is not finished. Sleeping...");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }
    try {
      dbIn.close();
    } catch (IOException e) {
      LOG.error("Problem closing PipedReader: "+e.getMessage());
      throw new BuildException("Problem closing PipedWriter: "+e.getMessage());
    }
  
    mm.releaseConnection((Connection) conn);
  }
  
  private void fillTreeIdHash() {
    Database db=null;
    try {
      db = DatabaseFactory.getDatabase("db.PAC");
    } catch (ClassNotFoundException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (SQLException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    try {
      if (pacConnection==null) {
        LOG.info("Opening db connection to PAC.");
        pacConnection = db.getConnection();
      }
      Statement stmt = pacConnection.createStatement();
      String query = "SELECT id,tree from tree";
      LOG.info("About to execute: "+query);
      ResultSet res = stmt.executeQuery(query);
      while (res.next()) {
        Integer id = res.getInt(1);
        String tree = res.getString(2);
        treeIdMap.put(id,tree);
      }
      stmt.close();
    } catch (SQLException e) {
      throw new BuildException("Trouble getting family members names: " + e.getMessage());
    } catch (Exception e) {
      LOG.error("A totally unexpected problem: "+e.getMessage());
    }
  }
  private ResultSet getHomologData(int offset) {
    ResultSet res = null;
    if (methodIds == null) {
      throw new BuildException("Proteome Ids must set.");
    }

    Database db=null;
    try {
      db = DatabaseFactory.getDatabase("db.PAC");
    } catch (ClassNotFoundException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (SQLException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    try {
      if (pacConnection==null) {
        LOG.info("Opening db connection to PAC.");
        pacConnection = db.getConnection();
      }
      Statement stmt = pacConnection.createStatement();
      // this is the old query.
      // we're going to want transcript id and taxon id as strings. so cast them here
      /*String query = "select t1.proteomeId as proteome_1, " +
          "t1.id as gene_1, " +
          "t2.proteomeId as proteome_2, " +
          "t2.id as gene_2, " +
          "r.name as relationship, " +
          "bootscore, clusterId, " +
          "t.name as tree_type, tree " +
          "FROM " +
          "transcript t1, transcript t2, homolog h, " +
          "homologRelationship r, treeType t, tree " +
          "WHERE " +
          "t1.proteomeId in ("+ proteomeIds + ") AND " +
          "t2.proteomeId in ("+ proteomeIds + ") AND " +
          "t1.id=transcriptId_1 AND " +
          "t2.id=transcriptId_2 AND " +
          "tree.treeType=t.id AND " +
          "homologRelationshipId=r.id AND " +
          "reconTreeId=tree.id";*/
      String query = "SELECT " +
          "transcriptId_1 AS gene_1, " +
          "transcriptId_2 AS gene_2, " +
          "r.name AS relationship, " +
          "bootscore, " +
          "t.clusterId," +
          "tt.name as tree_type, " +
          "t.id " +
          "FROM " +
          "homolog h, " +
          "homologRelationship r, " +
          "treeType tt, " +
          "tree t " +
          "WHERE " +
          "familyNode in ("+ methodIds + ") AND " +
          "t.treeType=tt.id AND " +
          "homologRelationshipId=r.id AND " +
          "reconTreeId=t.id AND " +
          "active=1 " +
          "ORDER BY 1,2 " +
          "LIMIT 100000 " +
          "OFFSET " + offset;
      LOG.info("About to execute: "+query);
      res = stmt.executeQuery(query);
      LOG.info("Have result set.");
    } catch (SQLException e) {
      throw new BuildException("Trouble getting family members names: " + e.getMessage());
    } catch (Exception e) {
      LOG.error("A totally unexpected problem: "+e.getMessage());
    }
    return res;
  }
  
  private void fillGeneIdHash() throws BuildException {
    Pattern stripPac = Pattern.compile("PAC:");
    try {
      Query q = new Query();

      QueryClass qcOrganism = new QueryClass(Organism.class);
      QueryClass qcGene = new QueryClass(Gene.class);
      QueryValue qOv = null;
      QueryField qTransId = new QueryField(qcGene,"secondaryIdentifier");

      QueryField qsOrgId = new QueryField(qcOrganism,"id");
      QueryField qsGeneId = new QueryField(qcGene,"id");
      q.addFrom(qcGene);
      q.addFrom(qcOrganism);

      q.addToSelect(qTransId);
      q.addToSelect(qsOrgId);
      q.addToSelect(qsGeneId);

      ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
      QueryObjectReference orgOrganismRef = new QueryObjectReference(qcGene, "organism");
      cs.addConstraint(new ContainsConstraint(orgOrganismRef, ConstraintOp.CONTAINS, qcOrganism));

      q.setConstraint(cs);

      ((ObjectStoreInterMineImpl) osw.getObjectStore()).precompute(q, Constants.PRECOMPUTE_CATEGORY);
      Results res = osw.getObjectStore().execute(q, 1000, true, true, true);
      Iterator<Object> resIter = res.iterator();
      while (resIter.hasNext()) {
        @SuppressWarnings("unchecked")
        ResultsRow<Object> rr = (ResultsRow<Object>) resIter.next();
        String secondary = (String)rr.get(0);
        if (secondary != null) {
          Matcher m = stripPac.matcher(secondary);
          String trIdS = m.replaceAll("");
          Integer orgId = (Integer)rr.get(1);
          Integer geneId = (Integer)rr.get(2);
          try {
            Integer trId = Integer.parseInt(trIdS);
            geneIdMap.put(trId,new GeneInfo(orgId,geneId));
          } catch (NumberFormatException e) {
            throw new BuildException("Trouble converting "+trIdS+" to an integer.");
          }
        }
      }
    } catch (ObjectStoreException e) {
      LOG.error("Problem in query: " + e.getMessage());
      throw new BuildException("Problem in query: " + e.getMessage());
    }
  }
  private void fillFamilyIdHash() throws BuildException {
    try {
      Query q = new Query();

      QueryClass qcFamily = new QueryClass(ProteinFamily.class);

      QueryField qfFamilyId = new QueryField(qcFamily,"id");
      QueryField qfClusterId = new QueryField(qcFamily,"clusterId");
      q.addFrom(qcFamily);
      q.addToSelect(qfFamilyId);
      q.addToSelect(qfClusterId);


      ((ObjectStoreInterMineImpl) osw.getObjectStore()).precompute(q, Constants.PRECOMPUTE_CATEGORY);
      Results res = osw.getObjectStore().execute(q, 1000, true, true, true);
      Iterator<Object> resIter = res.iterator();
      while (resIter.hasNext()) {
        @SuppressWarnings("unchecked")
        ResultsRow<Object> rr = (ResultsRow<Object>) resIter.next();
        Integer id = (Integer)rr.get(0);
        Integer clusterId = (Integer)rr.get(1);
        familyIdMap.put(clusterId,id);
      }
    } catch (ObjectStoreException e) {
      LOG.error("Problem in query: " + e.getMessage());
      throw new BuildException("Problem in query: " + e.getMessage());
    }
  }

  public void setMethodIds(String methods) {
    methodIds = methods;
  }

  private class GeneInfo {
    // this is a silly utility class to capture a pair for organismid and geneid
    Integer orgId;
    Integer geneId;
    GeneInfo(Integer org,Integer gene) {
      orgId = org;
      geneId = gene;
    }
    public Integer orgId() { return orgId; }
    public Integer geneId() { return geneId; }
  }

  private class CopyThread implements Runnable {
    Reader r;
    CopyManager cm;
    CopyThread(CopyManager cm,Reader r) {
      super();
      this.cm = cm;
      this.r = r;
    }
    public void run() {
      try {
        cm.copyIn("COPY Homolog (organism1id,gene1id,organism2id,gene2id,proteinfamilyid,type,relationship,bootscore,tree) from STDIN",r,1024*1024);
      } catch (SQLException e) {
        LOG.error("SQL problem in copy: " + e.getMessage());
        throw new BuildException("SQL problem in copy: " + e.getMessage());
      } catch (IOException e) {
        LOG.error("IO problem in copy: " + e.getMessage());
        throw new BuildException("IO problem in copy: " + e.getMessage());
      }
    }
  }
}
