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
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.bio.util.Constants;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Homolog;
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

public class InparanoidHomologsPostProcess extends PostProcessor {


  private static final Logger LOG = Logger.getLogger(InparanoidHomologsPostProcess.class);

  public InparanoidHomologsPostProcess(ObjectStoreWriter osw) {
    super(osw);
    this.osw = osw;
  }

  public void postProcess() throws BuildException, ObjectStoreException {

    Results res = getHomologData();

    String groupName = null;
    String shortNameA = null;
    String shortNameB = null;
    HashSet<Homolog> organismAASet = new HashSet<Homolog>();
    HashSet<Homolog> organismABSet = new HashSet<Homolog>();
    HashSet<Homolog> organismBASet = new HashSet<Homolog>();
    HashSet<Homolog> organismBBSet = new HashSet<Homolog>();
    

    Iterator<Object> resIter = res.iterator();
    int ctr = 0;
    while (resIter.hasNext()) {
      @SuppressWarnings("unchecked")
      ResultsRow<Object> rr = (ResultsRow<Object>) resIter.next();
      Homolog h = (Homolog)rr.get(0);
      if (groupName != null && !groupName.equals(h.getGroupName()) ) {
        processOldGroup(groupName,shortNameA,shortNameB,
            organismAASet,organismABSet,organismBASet,organismBBSet);
        groupName = null;
        shortNameA = null;
        shortNameB = null;
        organismAASet = new HashSet<Homolog>();
        organismABSet = new HashSet<Homolog>();
        organismBASet = new HashSet<Homolog>();
        organismBBSet = new HashSet<Homolog>();     
      }
      groupName = h.getGroupName();
      Integer g1 = (Integer)rr.get(1);
      Integer g2 = (Integer)rr.get(2);
      String o1 = (String)rr.get(3);
      String o2 = (String)rr.get(4);
      if (shortNameA==null) {
        shortNameA = o1;
      } else if (shortNameB==null && !shortNameA.equals(o2)) {
        shortNameB = o2;
      }
      if ( o1.equals(shortNameA) && o2.equals(shortNameA) ) {
        organismAASet.add(h);
      } else if (o1.equals(shortNameA) && o2.equals(shortNameB)) {
        organismABSet.add(h);
      } else if (o1.equals(shortNameB) && o2.equals(shortNameA)) {
        organismBASet.add(h);
      } else if (o1.equals(shortNameB) && o2.equals(shortNameB)) {
        organismBBSet.add(h);
      }
      ctr++;
      if ((ctr%10000) == 0 ) {
        LOG.info("Processed "+ctr+" records...");
      }
    }
    LOG.info("Processed "+ctr+" records.");
  }
  
  private void processOldGroup(String groupName,String shortNameA, String shortNameB,
      HashSet<Homolog> organismAASet, HashSet<Homolog> organismABSet,
      HashSet<Homolog> organismBASet, HashSet<Homolog> organismBBSet) {
    try {
    for(Homolog h: organismAASet ) {
      h.setRelationship(((shortNameB==null)?"":shortNameB + " ")+"mutual ortholog");
      osw.store(h);
    }  
    for(Homolog h: organismBBSet ) {
      h.setRelationship(((shortNameA==null)?"":shortNameA + " ")+"mutual ortholog");
      osw.store(h);
    }
    for(Homolog h: organismABSet ) {
      if (shortNameA.equals(h.getOrganism1().getShortName())) {
        h.setRelationship(category(organismABSet.size(),organismBASet.size()));
      } else {
        h.setRelationship(category(organismBASet.size(),organismABSet.size()));
      }
      osw.store(h);
    }    
    for(Homolog h: organismBASet ) {    
      if (shortNameA.equals(h.getOrganism1().getShortName())) {
        h.setRelationship(category(organismABSet.size(),organismBASet.size()));
      } else {
        h.setRelationship(category(organismBASet.size(),organismABSet.size()));
      }
      osw.store(h);
    }
    } catch (ObjectStoreException e) {
      throw new BuildException("Problem storing homolog: "+e.getMessage());
    }
    
  }

  private String category(int i, int j) {
    if (i==1 && j==1) {
      return "one-to-one";
    } else if (i > 1 && j==1) {
      return "many-to-one";
    } else if (i==1 && j > 1) {
      return "one-to-many";
    } else if (i>1 && j > 1) {
      return "many-to-many";
    } else {
      throw new BuildException("Unexpected category.");
    }
  }
  
  private Results getHomologData() {
    Results res = null;

    try {
      Query q = new Query();

      QueryClass qcHomolog = new QueryClass(Homolog.class);
      QueryClass qcOrganism1 = new QueryClass(Organism.class);
      QueryClass qcOrganism2 = new QueryClass(Organism.class);
      QueryClass qcGene1 = new QueryClass(Gene.class);
      QueryClass qcGene2 = new QueryClass(Gene.class);

      QueryField qfGene1Id = new QueryField(qcGene1,"id");
      QueryField qfGene2Id = new QueryField(qcGene2,"id");
      QueryField qfOrganism1Name = new QueryField(qcOrganism1,"shortName");
      QueryField qfOrganism2Name = new QueryField(qcOrganism2,"shortName");
      
      QueryField qfHomologGroupName = new QueryField(qcHomolog,"groupName");

      q.addFrom(qcHomolog);
      q.addFrom(qcOrganism1);
      q.addFrom(qcOrganism2);
      q.addFrom(qcGene1);
      q.addFrom(qcGene2);

      q.addToSelect(qcHomolog);
      q.addToSelect(qfGene1Id);
      q.addToSelect(qfGene2Id);
      q.addToSelect(qfOrganism1Name);
      q.addToSelect(qfOrganism2Name);
      q.addToSelect(qfHomologGroupName);
      
      q.clearOrderBy();
      q.addToOrderBy(qfHomologGroupName);

      ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
      cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qcHomolog, "organism1"),
          ConstraintOp.CONTAINS, qcOrganism1));
      cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qcHomolog, "organism2"),
          ConstraintOp.CONTAINS, qcOrganism2));
      cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qcHomolog, "gene1"),
          ConstraintOp.CONTAINS, qcGene1));
      cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qcHomolog, "gene2"),
          ConstraintOp.CONTAINS, qcGene2));

      q.setConstraint(cs);

      ((ObjectStoreInterMineImpl) osw.getObjectStore()).precompute(q, Constants.PRECOMPUTE_CATEGORY);
      res = osw.getObjectStore().execute(q, 10000, true, true, true);
    } catch (ObjectStoreException e) {
      LOG.error("Problem in query: " + e.getMessage());
      throw new BuildException("Problem in query: " + e.getMessage());
    }

    return res;
  }

}
