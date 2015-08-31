/**
 * 
 */
package org.intermine.bio.postprocess;

import java.util.Iterator;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.bio.util.Constants;
import org.intermine.metadata.ConstraintOp;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Homolog;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.postprocess.PostProcessor;

/**
 * @author jcarlson
 *
 */
public class PhytozomeHomologPostProcess extends PostProcessor {

  String methodIds;

  private static final Logger LOG = Logger.getLogger(PhytozomeHomologPostProcess.class);


  public PhytozomeHomologPostProcess(ObjectStoreWriter osw) {
    super(osw);
    this.osw = osw;
  }

  public void postProcess() throws BuildException, ObjectStoreException {

    Results res = getHomologData();

    try {
      Iterator<Object> resIter = res.iterator();
      while (resIter.hasNext()) {
        @SuppressWarnings("unchecked")
        ResultsRow<Object> rr = (ResultsRow<Object>) resIter.next();
        Homolog h = (Homolog)rr.get(0);
        Integer orgId1 = (Integer)rr.get(1);
        Integer orgId2 = (Integer)rr.get(2);
        h.proxyOrganism1(new ProxyReference(osw,orgId1,Organism.class));
        
        h.proxyOrganism2(new ProxyReference(osw,orgId2,Organism.class));
        osw.store(h);
      }
    } catch (ObjectStoreException e) {
      throw new BuildException("Problem with query: "+e.getMessage());
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

      QueryField qfOrganism1Id = new QueryField(qcOrganism1,"id");
      QueryField qfOrganism2Id = new QueryField(qcOrganism2,"id");

      q.addFrom(qcHomolog);
      q.addFrom(qcOrganism1);
      q.addFrom(qcOrganism2);
      q.addFrom(qcGene1);
      q.addFrom(qcGene2);

      q.addToSelect(qcHomolog);
      q.addToSelect(qfOrganism1Id);
      q.addToSelect(qfOrganism2Id);
      

      ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
      cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qcGene1, "organism"),
          ConstraintOp.CONTAINS, qcOrganism1));
      cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qcGene2, "organism"),
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
