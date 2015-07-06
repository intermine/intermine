/**
 * 
 */
package org.intermine.bio.postprocess;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.bio.util.Constants;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.CrossReference;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.GOAnnotation;
import org.intermine.model.bio.GOEvidence;
import org.intermine.model.bio.GOTerm;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Ontology;
import org.intermine.model.bio.OntologyTerm;
import org.intermine.model.bio.OntologyAnnotation;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.ProteinAnalysisFeature;
import org.intermine.model.bio.ProteinDomain;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.postprocess.PostProcessor;
import org.intermine.util.DynamicUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

/**
 * let's number the genes along each chromosome/scaffold
 * @author jcarlson
 *
 */
public class PhytozomeDbPostProcess extends PostProcessor {

  Integer proteomeId;
 
 
  private static final Logger LOG = Logger.getLogger(PhytozomeDbPostProcess.class);

  public PhytozomeDbPostProcess(ObjectStoreWriter osw) {
    super(osw);
    this.osw = osw;
    proteomeId = null;

  }

  public void postProcess() throws BuildException, ObjectStoreException {
    if (proteomeId==null) {
      LOG.error("Proteome Id is not set.");
      throw new BuildException("Proteome Id is not set.");
    }

    Integer lastChrId = null;
    Integer geneCtr = new Integer(1);
    Iterator<?> geneRefs = findGenesInOrder();
    while (geneRefs.hasNext()  ) {
      ResultsRow<?> rr = (ResultsRow<?>) geneRefs.next();
      Gene thisGene = (Gene) rr.get(0);
      Integer chrId = (Integer) rr.get(1);

      if (lastChrId == null || !lastChrId.equals(chrId)) {
        geneCtr = 1;
      } else {
        geneCtr++;
      }
      thisGene.setGenomicOrder(geneCtr);

      osw.store(thisGene);
    }

  }

  private Iterator<?> findGenesInOrder() {
    // find genes in order
    try {
      Query q = new Query();

      q.setDistinct(false);

      QueryClass qcGene = new QueryClass(Gene.class);
      q.addFrom(qcGene);
      q.addToSelect(qcGene);
      
      QueryClass qcOrg = new QueryClass(Organism.class);
      q.addFrom(qcOrg);
      QueryField qcOrgProt = new QueryField(qcOrg,"proteomeId");
      
      QueryClass qLoc = new QueryClass(Location.class);
      q.addFrom(qLoc); 
      
      QueryField qLocStart = new QueryField(qLoc,"start");
      QueryField qLocEnd = new QueryField(qLoc,"end");
      QueryField qLocStrand = new QueryField(qLoc,"strand");

      q.addToSelect(qLocStart);
      q.addToSelect(qLocEnd);
      q.addToSelect(qLocStrand);
      
      QueryClass qChr = new QueryClass(Chromosome.class);
      q.addFrom(qChr);
      QueryField qChrId = new QueryField(qChr,"id");
      q.addToSelect(qChrId);

      ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

      cs.addConstraint(new SimpleConstraint(qcOrgProt,ConstraintOp.EQUALS,new QueryValue(proteomeId)));
      QueryObjectReference orgRef = new QueryObjectReference(qcGene,"organism");
      cs.addConstraint(new ContainsConstraint(orgRef, ConstraintOp.CONTAINS, qcOrg));
      
      QueryObjectReference geneLocationRef = new QueryObjectReference(qcGene, "chromosomeLocation");
      cs.addConstraint(new ContainsConstraint(geneLocationRef, ConstraintOp.CONTAINS, qLoc));

      QueryObjectReference chromRef = new QueryObjectReference(qLoc, "locatedOn");
      cs.addConstraint(new ContainsConstraint(chromRef, ConstraintOp.CONTAINS, qChr));

      q.setConstraint(cs);
      q.clearOrderBy();
      q.addToOrderBy(qChrId);

      q.addToOrderBy(qLocStart);
      q.addToOrderBy(qLocEnd);
      q.addToOrderBy(qLocStrand);

      ((ObjectStoreInterMineImpl) osw.getObjectStore()).precompute(q, Constants.PRECOMPUTE_CATEGORY);
      Results res = osw.getObjectStore().execute(q, 500000, true, true, true);
      return res.iterator();
    } catch (ObjectStoreException e) {
      throw new BuildException("Problem querying for genese: "+e.getMessage());
    }
  
   }
 
  public void setProteomeId(String proteome) {
    try {
      proteomeId = Integer.valueOf(proteome);
    } catch (NumberFormatException e) {
      LOG.error("Cannot find numerical proteome id for: " + proteome);
      throw new BuildException("Cannot find numerical proteome id for: " + proteome);
    }
  }

}
