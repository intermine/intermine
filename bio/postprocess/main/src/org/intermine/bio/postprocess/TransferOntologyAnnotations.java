/**
 * 
 */
package org.intermine.bio.postprocess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.bio.util.Constants;
import org.intermine.dataloader.IntegrationWriterDataTrackingImpl;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.CrossReference;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.GOAnnotation;
import org.intermine.model.bio.GOEvidence;
import org.intermine.model.bio.GOEvidenceCode;
import org.intermine.model.bio.GOTerm;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Ontology;
import org.intermine.model.bio.OntologyAnnotation;
import org.intermine.model.bio.OntologyTerm;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.ProteinDomain;
import org.intermine.model.bio.ProteinAnalysisFeature;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.DynamicUtil;


/**
 * Take any Crossreferences linked to ontology terms assigned to proteins and copy them to corresponding genes.
 *
 * Modified for phytozome schema from GoPostprocess
 * 
 * @author J Carlson 
 */
public class TransferOntologyAnnotations {
  private static final Logger LOG = Logger.getLogger(TransferOntologyAnnotations.class);
  protected ObjectStore os;
  protected ObjectStoreWriter osw = null;
  private DataSet dataSet;
  private DataSource dataSource;

  /**
   * Create a new UpdateOrthologes object from an ObjectStoreWriter
   * @param osw writer on genomic ObjectStore
   */
  public  TransferOntologyAnnotations(ObjectStoreWriter osw) {    
    this.osw = osw;
    this.os = osw.getObjectStore();
  }


  /**
   * Copy all crossreference annotations from the Protein objects to the corresponding Gene(s)
   * @throws ObjectStoreException if anything goes wrong
   */
  public void execute() throws ObjectStoreException {

    ArrayList<Integer> proteomes = new ArrayList<Integer>();;

    Query q = new Query();
    QueryClass qOrg = new QueryClass(Organism.class);
    q.addFrom(qOrg);
    QueryField qF = new QueryField(qOrg,"proteomeId");
    q.addToSelect(qF);
    ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
    Iterator<?>  res = os.execute(q, 100, true, true, true).iterator();
    while(res.hasNext()) {
      ResultsRow<?> rr = (ResultsRow<?>) res.next();
      proteomes.add((Integer)rr.get(0));
    }
    LOG.info("Going to query "+proteomes.size()+" different proteoms.");

    for(Integer proteomeId : proteomes ) {

      LOG.info("Making query for "+proteomeId);
      
      HashSet<KnownPair> knownTerms = getKnownTerms(os,proteomeId);
      Iterator<?> resIter = findOntologyTerms(proteomeId);

      int geneCount = 0;
      int protCount = 0;
      int knownGCount = 0;
      int knownPCount = 0;
      Gene lastGene = null;
      Protein lastProtein = null;
      HashSet<OntologyAnnotation> geneAnnotationSet = new HashSet<OntologyAnnotation>();
      HashSet<OntologyAnnotation> proteinAnnotationSet = new HashSet<OntologyAnnotation>();
      while (resIter.hasNext()  ) {
        ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
        Gene thisGene = (Gene) rr.get(0);
        Protein thisProtein = (Protein) rr.get(1);
        OntologyTerm thisTerm = (OntologyTerm) rr.get(2);

        if (!thisTerm.getOntology().getName().equals("GO")) {

          // first one
          if (lastGene==null) {
            lastGene = thisGene;
            lastProtein = thisProtein;
          }
          LOG.debug("Processing gene " + thisGene.getPrimaryIdentifier() + ", protein " + thisProtein.getPrimaryIdentifier() +
              " with term "+thisTerm.getId());

          if (!knownTerms.contains(new KnownPair(thisTerm.getId(),thisGene.getId()))) {
            knownTerms.add(new KnownPair(thisTerm.getId(),thisGene.getId()));

            OntologyAnnotation gOA = (OntologyAnnotation)DynamicUtil.createObject(Collections.singleton(OntologyAnnotation.class));
            gOA.setOntologyTerm(thisTerm);
            gOA.setSubject(thisGene);
            osw.store(gOA);
            // look at the gene if not already processed for this term
            if( lastGene.getId() != thisGene.getId() ) {
              // we've moved to a new gene. Store the last.
              if (geneAnnotationSet.size() > 0 ) {
                // I'm no sure if this does anything
                lastGene.setOntologyAnnotations(geneAnnotationSet);
                // so I've commented out the store.
                //osw.store(lastGene);
                geneAnnotationSet = new HashSet<OntologyAnnotation>();
              }
              lastGene = thisGene;
            }
            geneAnnotationSet.add(gOA);
            geneCount++;
          } else {
            knownGCount++;
            LOG.debug("Already know about term "+thisTerm.getId()+" and gene "+thisGene.getId());
          }

          // it's protein time
          if (!knownTerms.contains(new KnownPair(thisTerm.getId(),thisProtein.getId()))) {
            knownTerms.add(new KnownPair(thisTerm.getId(),thisProtein.getId()));
            OntologyAnnotation pOA = (OntologyAnnotation)DynamicUtil.createObject(Collections.singleton(OntologyAnnotation.class));
            pOA.setOntologyTerm(thisTerm);
            pOA.setSubject(thisProtein);
            osw.store(pOA);
            // look at the protein if not already processed for this term
            if ( lastProtein.getId() != thisProtein.getId() ) {
              if (proteinAnnotationSet.size() > 0 ) {
                lastProtein.setOntologyAnnotations(proteinAnnotationSet);
                // as above, commented out.
                //osw.store(lastProtein);
                proteinAnnotationSet = new HashSet<OntologyAnnotation>();
              }
              lastProtein = thisProtein;
            }
            proteinAnnotationSet.add(pOA);
            protCount++;
          } else {
            knownPCount++;
            LOG.debug("Already know about term "+thisTerm.getId()+" and protein "+thisProtein.getId());
          }

          if ( (geneCount + protCount > 0) && ((geneCount+protCount)%50000 == 0) ) {
            LOG.info("Created "+geneCount+" gene records and "+protCount+" protein records...");
          }
        }
      }

      // clean up
      if (geneAnnotationSet.size() > 0) {
        lastGene.setOntologyAnnotations(geneAnnotationSet);
        osw.store(lastGene);
      }
      if (proteinAnnotationSet.size() > 0) {
        lastProtein.setOntologyAnnotations(proteinAnnotationSet);
        osw.store(lastProtein);
      }

      LOG.info("Created "+geneCount+" gene records and "+protCount+" protein records.");
      LOG.info("Knew about "+knownGCount+" genes and "+knownPCount+" proteins.");
    }
  }


  /**
   * Query Gene->Protein->Annotation->GOTerm and return an iterator over the Gene
   *  and GOTerm.
   *
   */
  private Iterator<?> findOntologyTerms(Integer proteomeId)
      throws ObjectStoreException {
    Query q = new Query();

    q.setDistinct(true);

    QueryClass qcGene = new QueryClass(Gene.class);
    q.addFrom(qcGene);
    q.addToSelect(qcGene);
    q.addToOrderBy(qcGene);


    QueryClass qcProtein = new QueryClass(Protein.class);
    q.addFrom(qcProtein);
    q.addToSelect(qcProtein);

    QueryClass qcPAF = new QueryClass(ProteinAnalysisFeature.class);
    q.addFrom(qcPAF);

    QueryClass qcCrossReference= new QueryClass(CrossReference.class);
    q.addFrom(qcCrossReference);

    QueryClass qcOntologyTerm = new QueryClass(OntologyTerm.class);
    q.addFrom(qcOntologyTerm);
    q.addToSelect(qcOntologyTerm);  
    ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

    QueryCollectionReference geneProtRef = new QueryCollectionReference(qcProtein, "genes");
    cs.addConstraint(new ContainsConstraint(geneProtRef, ConstraintOp.CONTAINS, qcGene));

    QueryObjectReference protAnalysisRef = new QueryObjectReference(qcPAF, "protein");
    cs.addConstraint(new ContainsConstraint(protAnalysisRef, ConstraintOp.CONTAINS, qcProtein));

    QueryObjectReference crossRefRef = new QueryObjectReference(qcPAF, "crossReference");
    cs.addConstraint(new ContainsConstraint(crossRefRef, ConstraintOp.CONTAINS, qcCrossReference));

    QueryCollectionReference proteinDomainRef = new QueryCollectionReference(qcOntologyTerm,"xrefs");
    cs.addConstraint(new ContainsConstraint(proteinDomainRef, ConstraintOp.CONTAINS,qcCrossReference));

    if (proteomeId != null ) {
      QueryClass qcOrg = new QueryClass(Organism.class);
      QueryField qcOrgP = new QueryField(qcOrg,"proteomeId");
      QueryValue qcOrgPV= new QueryValue(proteomeId);
      q.addFrom(qcOrg);
      QueryObjectReference orgRef = new QueryObjectReference(qcGene,"organism");
      cs.addConstraint(new ContainsConstraint(orgRef, ConstraintOp.CONTAINS,qcOrg));
      cs.addConstraint(new SimpleConstraint(qcOrgP,ConstraintOp.EQUALS,qcOrgPV));
    }

    q.setConstraint(cs);
    q.addToOrderBy(qcGene);
    q.addToOrderBy(qcProtein);

    LOG.info("About to execute query: "+q.toString());
    //((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);

    LOG.info("Back from precomute.");
    List<ResultsRow<Object>> res = os.execute(q,0,Integer.MAX_VALUE,true,true,ObjectStore.SEQUENCE_IGNORE);

    LOG.info("Back from execute.");
    return res.iterator();
  }

  private HashSet<KnownPair> getKnownTerms(ObjectStore os,Integer proteomeId)
      throws ObjectStoreException {

    HashSet<KnownPair> ret = new HashSet<KnownPair>();
    Query q = new Query();

    q.setDistinct(true);

    QueryClass qcGene = new QueryClass(BioEntity.class);
    q.addFrom(qcGene);
    QueryField qcGId = new QueryField(qcGene,"id");
    q.addToSelect(qcGId);

    QueryClass qcOntologyAnnotation = new QueryClass(OntologyAnnotation.class);
    q.addFrom(qcOntologyAnnotation);
    QueryClass qcOntologyTerm = new QueryClass(OntologyTerm.class);
    q.addFrom(qcOntologyTerm);
    QueryField qcOTId = new QueryField(qcOntologyTerm,"id");
    q.addToSelect(qcOTId); 
    QueryClass qcOntology = new QueryClass(Ontology.class);
    q.addFrom(qcOntology);
    QueryField qcGOName = new QueryField(qcOntology,"name");
    QueryValue qcGO = new QueryValue("GO"); 
    ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

    QueryObjectReference qcGeneRef = new QueryObjectReference(qcOntologyAnnotation, "subject");
    cs.addConstraint(new ContainsConstraint(qcGeneRef,ConstraintOp.CONTAINS,qcGene));
    QueryObjectReference qcTermRef = new QueryObjectReference(qcOntologyAnnotation, "ontologyTerm");
    cs.addConstraint(new ContainsConstraint(qcTermRef,ConstraintOp.CONTAINS,qcOntologyTerm));
    QueryObjectReference qcOntRef = new QueryObjectReference(qcOntologyTerm,"ontology");
    cs.addConstraint(new ContainsConstraint(qcOntRef,ConstraintOp.CONTAINS,qcOntology));
    cs.addConstraint(new SimpleConstraint(qcGOName,ConstraintOp.NOT_EQUALS,qcGO));

    if (proteomeId != null ) {
      QueryClass qcOrg = new QueryClass(Organism.class);
      QueryField qcOrgP = new QueryField(qcOrg,"proteomeId");
      QueryValue qcOrgPV= new QueryValue(proteomeId);
      q.addFrom(qcOrg);
      QueryObjectReference orgRef = new QueryObjectReference(qcGene,"organism");
      cs.addConstraint(new ContainsConstraint(orgRef, ConstraintOp.CONTAINS,qcOrg));
      cs.addConstraint(new SimpleConstraint(qcOrgP,ConstraintOp.EQUALS,qcOrgPV));
    }

    q.setConstraint(cs);

    List<ResultsRow<Object>> res = os.execute(q, 0,Integer.MAX_VALUE, true, true,ObjectStore.SEQUENCE_IGNORE);

    Iterator<?> iter = res.iterator();
    while (iter.hasNext()) {
      ResultsRow<?> rr = (ResultsRow<?>) iter.next();
      Integer thisGeneId = (Integer) rr.get(0);
      Integer thisTermId = (Integer) rr.get(1);
      ret.add(new KnownPair(thisTermId,thisGeneId)); 
    }

    LOG.info("Retrieved records for "+ret.size()+" known annotations.");

    return ret;
  }

  class KnownPair {
    Integer term;
    Integer subject;
    KnownPair(Integer termId, Integer subjectId) {
      this.term = termId;
      this.subject = subjectId;
    }
    public Integer termId() { return term;}
    public Integer subjectId() { return subject; }
    public boolean equals(final Object a) {
      return (a==null)?false:(((KnownPair)a).termId().equals(term) &&
          ((KnownPair)a).subjectId().equals(subject));

    }
    @Override
    public int hashCode() {
      return term.hashCode() + subject.hashCode();

    }
  }
}

