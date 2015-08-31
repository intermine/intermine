/**
 * 
 */
package org.intermine.bio.postprocess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.io.PrintWriter;
import java.io.File;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.bio.util.Constants;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.CufflinksScore;
import org.intermine.model.bio.DiversitySample;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.RNAseqExperiment;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.metadata.ConstraintOp;
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
import org.intermine.sql.query.Constraint;

/**
 * @author jcarlson
 *
 */
public class CufflinksPostProcess extends PostProcessor {

  ObjectStoreWriter osw;
  private static final Logger LOG = Logger.getLogger(CufflinksPostProcess.class);
  private static final int batchSize = 100000;
  private Integer proteomeId = null;
  
  public CufflinksPostProcess(ObjectStoreWriter osw) {
    super(osw);
    this.osw = osw;
    
  }
  
  public void setProteomeId(String id) {
    try {
      proteomeId = Integer.parseInt(id);
    } catch (NumberFormatException e) {
      throw new BuildException("Cannot parse integer from "+id);
    }
  }

  public void postProcess() throws BuildException {
    
    // look through all scores, ordered first by bioentity, then by library. 
    LOG.info("Processing BioEntity...");
    processSet("BioEntity");
    LOG.info("Processing Experiments...");
    processSet("Experiment");
  }
  
  private void processSet(String setName) throws BuildException {
    
    boolean doMore = true;
    int offset = 0;
    int ctr = 0;

    Integer lastId = null;
    ArrayList<CufflinksScore> cuffGroup = null;
    
    while (doMore) {
      //Look for high and low expression cases and mark them as such.
      List<ResultsRow<Object>> results = getCufflinksIterator(setName,offset);
      doMore = false;
      offset += batchSize;
      for (ResultsRow<Object> rr : results) {
        doMore = true;
        @SuppressWarnings("unchecked")
        CufflinksScore cs = (CufflinksScore)rr.get(0);
        Integer id = (Integer)rr.get(1);

        if (lastId == null) {
          lastId = id;
          cuffGroup = new ArrayList<CufflinksScore>();
          cuffGroup.add(cs);
        } else if (lastId.equals(id)) {
          cuffGroup.add(cs);
        } else if (!lastId.equals(id) ) {
          processCuffGroup(cuffGroup,setName);
          lastId = id;
          cuffGroup = new ArrayList<CufflinksScore>();
          cuffGroup.add(cs);
        }
        ctr++;
        if( (ctr%10000) == 0) {
          LOG.info("Processed "+ctr+" records...");
        }
      }
    }
    // process the final set
    processCuffGroup(cuffGroup,setName);
    LOG.info("Processed "+ctr+" records...");
  }

  private void processCuffGroup(ArrayList<CufflinksScore> group,String groupBy)
      throws BuildException {
    int nSamples = 0;
    double sum = 0.;
    double sum2 = 0;

    try {
      for( CufflinksScore cs : group) {
        double fpkm = cs.getFpkm();
        if (fpkm == 0.) {
          // exactly zero
          LOG.debug("Setting Not Expressed for "+cs.getId());
          cs.setLocusExpressionLevel("Not expressed");
          cs.setLibraryExpressionLevel("Not expressed");
          osw.store(cs);
        } else {
          nSamples++;
          sum += Math.log(fpkm);
          sum2 += Math.log(fpkm)*Math.log(fpkm);
        }
      }

      if (nSamples > 1) {
        double mean = sum/nSamples;
        double stddev = Math.sqrt((sum2-sum*mean)/(nSamples-1));
        LOG.debug("Means and stdev for set containing "+group.get(0).getId()+
            " is " + mean + " and " + stddev);

        // this is a stupid test. Just something to get us going.
        for( CufflinksScore cs : group) {
          double fpkm = cs.getFpkm();
          if (fpkm != 0.) {

            if (Math.log(fpkm) > mean + stddev) {
              if (groupBy.equals("BioEntity") ) {
                cs.setLocusExpressionLevel("High");
              } else {
                cs.setLibraryExpressionLevel("High");
              }
              LOG.debug("Storing high "+groupBy+" cufflinks for "+cs.getId());
              osw.store(cs);
            } else if (Math.log(fpkm) < mean - stddev) {
              if( groupBy.equals("BioEntity") ) {
                cs.setLocusExpressionLevel("Low");
              } else {
                cs.setLibraryExpressionLevel("Low");
              }
              LOG.debug("Storing low "+groupBy+" cufflinks for "+cs.getId());
              osw.store(cs);
            } else {
              if( groupBy.equals("BioEntity") ) {
                if( cs.getLocusExpressionLevel() != null && !cs.getLocusExpressionLevel().isEmpty() ) {
                  LOG.warn("Locus expression level is not right for "+cs.getId());
                }
              } else {
                if( cs.getLibraryExpressionLevel() != null && !cs.getLibraryExpressionLevel().isEmpty() ) {
                  LOG.warn("Library expression level is not right for "+cs.getId());
                }
              }
            }
          }
        }
      }
    } catch (ObjectStoreException e) {
      throw new BuildException("There was a problem storing: "+e.getMessage());
    }


    
  }
  private List<ResultsRow<Object>> getCufflinksIterator(String groupBy,int offset) throws BuildException {
    try {
      Query q = new Query();

      q.setDistinct(true);

      QueryClass qcCufflinks = new QueryClass(CufflinksScore.class);
      q.addFrom(qcCufflinks);
      q.addToSelect(qcCufflinks);

 
      QueryClass qcOrg = null;
      QueryClass qcBase;
      QueryObjectReference qcBaseRef;
      
      if (groupBy.equals("BioEntity")) {
        qcBase = new QueryClass(BioEntity.class);
        qcBaseRef = new QueryObjectReference(qcCufflinks, "bioentity");
      } else if (groupBy.equals("Experiment")) {
        qcBase = new QueryClass(RNAseqExperiment.class);
        qcBaseRef = new QueryObjectReference(qcCufflinks, "experiment");
      } else {
        throw new BuildException("Unknown grouping entry "+groupBy);
      }

      q.addFrom(qcBase);
      QueryField qf = new QueryField(qcBase,"id");
      q.addToSelect(qf);
      ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
      cs.addConstraint(new ContainsConstraint(qcBaseRef, ConstraintOp.CONTAINS, qcBase));
      q.addToOrderBy(qf);
      
      if (proteomeId != null) {
        qcOrg = new QueryClass(Organism.class);
        QueryField qfProt = new QueryField(qcOrg,"proteomeId");
        QueryObjectReference orgRef = new QueryObjectReference(qcCufflinks,"organism");
        QueryValue qvProtId = new QueryValue(proteomeId);
        cs.addConstraint(new SimpleConstraint(qfProt,ConstraintOp.EQUALS,qvProtId));
        cs.addConstraint(new ContainsConstraint(orgRef,ConstraintOp.CONTAINS, qcOrg));
      }
      
      q.setConstraint(cs); 

      ((ObjectStoreInterMineImpl) osw.getObjectStore()).precompute(q, Constants.PRECOMPUTE_CATEGORY);
      List<ResultsRow<Object>> res = osw.getObjectStore().execute(q, offset, batchSize, true, true,ObjectStore.SEQUENCE_IGNORE);
      return res;
    } catch (ObjectStoreException e) {
      LOG.error("Problem in query: " + e.getMessage());
      throw new BuildException("Problem in query: " + e.getMessage());
    }
  }
}
