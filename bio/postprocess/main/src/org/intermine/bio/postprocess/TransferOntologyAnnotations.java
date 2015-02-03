/**
 * 
 */
package org.intermine.bio.postprocess;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.bio.util.Constants;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.CrossReference;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.GOAnnotation;
import org.intermine.model.bio.GOEvidence;
import org.intermine.model.bio.GOEvidenceCode;
import org.intermine.model.bio.GOTerm;
import org.intermine.model.bio.Gene;
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
import org.intermine.objectstore.query.ConstraintOp;
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

        osw.beginTransaction();

        Iterator<?> resIter = findOntologyTerms();

        HashSet<KnownPair> knownTerms = getKnownTerms(os); 
        int geneCount = 0;
        int protCount = 0;
        Gene lastGene = null;
        Protein lastProtein = null;
        HashSet<OntologyAnnotation> geneAnnotationSet = new HashSet<OntologyAnnotation>();
        HashSet<OntologyAnnotation> proteinAnnotationSet = new HashSet<OntologyAnnotation>();
        while (resIter.hasNext()  ) {
          ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
          Gene thisGene = (Gene) rr.get(0);
          Protein thisProtein = (Protein) rr.get(1);
          OntologyTerm thisTerm = (OntologyTerm) rr.get(2);

          // first one
          if (lastGene==null) {
            lastGene = thisGene;
            lastProtein = thisProtein;
          }
          LOG.debug("Processing gene " + thisGene.getPrimaryIdentifier() + ", protein " + thisProtein.getPrimaryIdentifier() +
              " with term "+thisTerm.getId());
          
          if (!knownTerms.contains(new KnownPair(thisTerm.getId(),thisGene.getId()))) {

            OntologyAnnotation gOA = (OntologyAnnotation)DynamicUtil.createObject(Collections.singleton(OntologyAnnotation.class));
            gOA.setOntologyTerm(thisTerm);
            gOA.setSubject(thisGene);
            osw.store(gOA);
            // look at the gene if not already processed for this term
            if( lastGene.getId() != thisGene.getId() ) {
              // we've moved to a new gene. Store the last.
              if (geneAnnotationSet.size() > 0 ) {
                lastGene.setOntologyAnnotations(geneAnnotationSet);
                osw.store(lastGene);
                geneAnnotationSet = new HashSet<OntologyAnnotation>();
              }
              lastGene = thisGene;
            }
            geneAnnotationSet.add(gOA);
            geneCount++;
          } else {
            LOG.info("Already know about term "+thisTerm.getId()+" and gene "+thisGene.getId());
          }

          // it's protein time
          if (!knownTerms.contains(new KnownPair(thisTerm.getId(),thisProtein.getId()))) {
          OntologyAnnotation pOA = (OntologyAnnotation)DynamicUtil.createObject(Collections.singleton(OntologyAnnotation.class));
          pOA.setOntologyTerm(thisTerm);
          pOA.setSubject(thisProtein);
          osw.store(pOA);
          // look at the gene if not already processed for this term
          if ( lastProtein.getId() != thisProtein.getId() ) {
            if (proteinAnnotationSet.size() > 0 ) {
              lastProtein.setOntologyAnnotations(proteinAnnotationSet);
              osw.store(lastProtein);
              proteinAnnotationSet = new HashSet<OntologyAnnotation>();
            }
            lastProtein = thisProtein;
          }
          proteinAnnotationSet.add(pOA);
          protCount++;
          } else {
            LOG.info("Already know about term "+thisTerm.getId()+" and protein "+thisProtein.getId());
          }

        if ( (geneCount + protCount > 0) && ((geneCount+protCount)%50000 == 0) ) {
          LOG.info("Created "+geneCount+" gene records and "+protCount+" protein records...");
         /* osw.abortTransaction();
          osw.beginTransaction();*/
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
    osw.commitTransaction();
}


    /**
     * Query Gene->Protein->Annotation->GOTerm and return an iterator over the Gene
     *  and GOTerm.
     *
     */
    private Iterator<?> findOntologyTerms()
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
        /*
        QueryClass qcOrganism = new QueryClass(Organism.class);
        QueryField qcOrgField = new QueryField(qcOrganism,"proteomeId");
        QueryValue qcEuc = new QueryValue(201);
        QueryValue qcPop = new QueryValue(210);

        q.addFrom(qcOrganism);*/
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference geneProtRef = new QueryCollectionReference(qcProtein, "genes");
        cs.addConstraint(new ContainsConstraint(geneProtRef, ConstraintOp.CONTAINS, qcGene));

        /*cs.addConstraint(new SimpleConstraint(qcOrgField,ConstraintOp.EQUALS,qcEuc));
        cs.addConstraint(new SimpleConstraint(qcOrgField,ConstraintOp.NOT_EQUALS,qcPop));
        QueryObjectReference qcGeneOrgRef = new QueryObjectReference(qcGene, "organism");
        cs.addConstraint(new ContainsConstraint(qcGeneOrgRef,ConstraintOp.CONTAINS,qcOrganism));*/
        QueryObjectReference protAnalysisRef = new QueryObjectReference(qcPAF, "protein");
        cs.addConstraint(new ContainsConstraint(protAnalysisRef, ConstraintOp.CONTAINS, qcProtein));

        QueryObjectReference crossRefRef = new QueryObjectReference(qcPAF, "crossReference");
        cs.addConstraint(new ContainsConstraint(crossRefRef, ConstraintOp.CONTAINS, qcCrossReference));

        QueryCollectionReference proteinDomainRef = new QueryCollectionReference(qcOntologyTerm,"xrefs");
        cs.addConstraint(new ContainsConstraint(proteinDomainRef, ConstraintOp.CONTAINS,qcCrossReference));

        q.setConstraint(cs);
        q.addToOrderBy(qcGene);
        q.addToOrderBy(qcProtein);

        ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
        // TODO: figure out how not to need this number.
        List<ResultsRow<Object>> res = os.execute(q, 0, 25000000, true, true,ObjectStore.SEQUENCE_IGNORE);
        return res.iterator();
    }
    
    private HashSet<KnownPair> getKnownTerms(ObjectStore os)
        throws ObjectStoreException {

      HashSet<KnownPair> ret = new HashSet<KnownPair>();
      Query q = new Query();

      q.setDistinct(true);

      QueryClass qcGene = new QueryClass(BioEntity.class);
      q.addFrom(qcGene);
      q.addToSelect(qcGene);

      QueryClass qcOntologyAnnotation = new QueryClass(OntologyAnnotation.class);
      q.addFrom(qcOntologyAnnotation);
      QueryClass qcOntologyTerm = new QueryClass(OntologyTerm.class);
      q.addFrom(qcOntologyTerm);
      q.addToSelect(qcOntologyTerm); /* 
      QueryClass qcOrganism = new QueryClass(Organism.class);
      QueryField qcOrgField = new QueryField(qcOrganism,"proteomeId");
      QueryValue qcEuc = new QueryValue(201);
      QueryValue qcPop = new QueryValue(210);

      q.addFrom(qcOrganism);*/
      ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

      /*cs.addConstraint(new SimpleConstraint(qcOrgField,ConstraintOp.EQUALS,qcEuc));
      QueryObjectReference qcGeneOrgRef = new QueryObjectReference(qcGene, "organism");
      cs.addConstraint(new ContainsConstraint(qcGeneOrgRef,ConstraintOp.CONTAINS,qcOrganism));*/

      QueryObjectReference qcGeneRef = new QueryObjectReference(qcOntologyAnnotation, "subject");
      cs.addConstraint(new ContainsConstraint(qcGeneRef,ConstraintOp.CONTAINS,qcGene));
      QueryObjectReference qcTermRef = new QueryObjectReference(qcOntologyAnnotation, "ontologyTerm");
      cs.addConstraint(new ContainsConstraint(qcTermRef,ConstraintOp.CONTAINS,qcOntologyTerm));

      q.setConstraint(cs);

      ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
      Results res = os.execute(q, 50000, true, true,true);

      Iterator<Object> iter = res.iterator();
      while (iter.hasNext()) {
        ResultsRow<?> rr = (ResultsRow<?>) iter.next();
        BioEntity thisGene = (BioEntity) rr.get(0);
        OntologyTerm thisTerm = (OntologyTerm) rr.get(1);
        ret.add(new KnownPair(thisTerm.getId(),thisGene.getId()));
        LOG.debug("Know about "+thisGene.getPrimaryIdentifier()+" and "+thisTerm.getId());
      }
      
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

