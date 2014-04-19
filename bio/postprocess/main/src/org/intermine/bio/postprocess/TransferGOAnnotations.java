package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2013 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.bio.util.Constants;
import org.intermine.model.bio.CrossReference;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.GOAnnotation;
import org.intermine.model.bio.GOEvidence;
import org.intermine.model.bio.GOEvidenceCode;
import org.intermine.model.bio.GOTerm;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.OntologyTerm;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.ProteinDomain;
import org.intermine.model.bio.ProteinAnalysisFeature;
import org.intermine.model.bio.Publication;
import org.intermine.model.bio.Transcript;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.postprocess.PostProcessor;
import org.intermine.util.DynamicUtil;

/**
 * Take any GOAnnotation objects assigned to proteins and copy them to corresponding genes.
 *
 * Modified for phytozome schema from GoPostprocess
 * 
 * @author J Carlson 
 */
public class TransferGOAnnotations {
    private static final Logger LOG = Logger.getLogger(TransferGOAnnotations.class);
    protected ObjectStore os;
    protected ObjectStoreWriter osw = null;
    private DataSet dataSet;
    private DataSource dataSource;
    private GOEvidenceCode gEC = null;
    private HashMap<Integer,HashMap<String,HashSet<String>>> knownGO = new HashMap<Integer,HashMap<String,HashSet<String>>>();
    private HashMap<String,Integer> goTerms = new HashMap<String,Integer>();

    /**
     * Create a new UpdateOrthologes object from an ObjectStoreWriter
     * @param osw writer on genomic ObjectStore
     */
    public  TransferGOAnnotations(ObjectStoreWriter osw) {    
      this.osw = osw;
      this.os = osw.getObjectStore();
      dataSource = (DataSource) DynamicUtil.createObject(Collections.singleton(DataSource.class));
      dataSource.setName("UniProtKB");
      try {
        dataSource = (DataSource) os.getObjectByExample(dataSource,
            Collections.singleton("name"));
      } catch (ObjectStoreException e) {
        throw new RuntimeException(
            "unable to fetch PhytoMine DataSource object", e);
      }
      try {
        gEC = findOrCreateCode("ISS");
        fillGOHashMap();
      } catch (ObjectStoreException e) {
        throw new RuntimeException(
            "unable to store GO code/retrieve existing GO terms.", e);
      }
    }
    
    private GOEvidenceCode findOrCreateCode(String code) throws ObjectStoreException {
        
      Query q = new Query();
        q.setDistinct(true);

        QueryClass qcGEC = new QueryClass(GOEvidenceCode.class);
        q.addFrom(qcGEC);
        q.addToSelect(qcGEC);
        
        ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
        Iterator<?> res = (Iterator<?>)os.execute(q, 5000, true, true, true).iterator();
        while(res.hasNext()) {
          ResultsRow<?> rr = (ResultsRow<?>) res.next();
          GOEvidenceCode gec = (GOEvidenceCode)rr.get(0);
          if (gec.getCode().equals(code)) {
            return gec;
          }
        }
        GOEvidenceCode gec = (GOEvidenceCode) DynamicUtil.createObject(Collections.singleton(GOEvidenceCode.class));
        gec.setCode(code);
        osw.store(gec);
        return gec;
    }

    /**
     * Copy all GO annotations from the Protein objects to the corresponding Gene(s)
     * @throws ObjectStoreException if anything goes wrong
     */
    public void execute() throws ObjectStoreException {

        long startTime = System.currentTimeMillis();

        osw.beginTransaction();

        Iterator<?> resIter = findProteinDomains(false);

        int count = 0;
        Map<OntologyTerm, GOAnnotation> annotations = new HashMap<OntologyTerm, GOAnnotation>();

        while (resIter.hasNext()) {
          ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
          Gene thisGene = (Gene) rr.get(0);
          ProteinDomain thisDomain = (ProteinDomain) rr.get(1);
          GOTerm thisTerm = (GOTerm) rr.get(2);

          LOG.debug("store gene " + thisGene.getPrimaryIdentifier() + " with "
              + thisDomain.getPrimaryIdentifier()+" with term "+thisTerm.getName());
          // store if not known already
          if (!knownGO.containsKey(thisGene.getOrganism().getId()) ||
              !knownGO.get(thisGene.getOrganism().getId()).containsKey(thisGene.getPrimaryIdentifier()) ||
              !knownGO.get(thisGene.getOrganism().getId()).get(thisGene.getPrimaryIdentifier()).contains(thisTerm.getIdentifier()) ){

              LOG.debug("There is a new annotation for "+thisGene.getPrimaryIdentifier() + 
                  " organism "+thisGene.getOrganism().getId() + " to " + thisTerm.getIdentifier());
                  GOEvidence thisEvidence = (GOEvidence)DynamicUtil.createObject(Collections.singleton(GOEvidence.class));
                  thisEvidence.setCode(gEC);
                  thisEvidence.setWithText(thisDomain.getName());
                  osw.store(thisEvidence);
                  HashSet<GOEvidence> thisEvidenceSet = new HashSet<GOEvidence>();
                  thisEvidenceSet.add(thisEvidence);
                  GOAnnotation thisAnnotation = (GOAnnotation) DynamicUtil.createObject(Collections.singleton(GOAnnotation.class));
                  thisAnnotation.setEvidence(thisEvidenceSet);
                  thisAnnotation.setSubject(thisGene);
                  thisAnnotation.setOntologyTerm(thisTerm);
                  osw.store(thisAnnotation);
                  count++;
                }
        }

       

        LOG.info("Created " + count + " new GOAnnotation objects for Genes"
                + " - took " + (System.currentTimeMillis() - startTime) + " ms.");
        osw.commitTransaction();
    }


    private void fillGOHashMap() throws ObjectStoreException {
      
      Query q = new Query();

      q.setDistinct(true);

      QueryClass qcGene = new QueryClass(Gene.class);
      q.addFrom(qcGene);
      q.addToSelect(qcGene);
      q.addToOrderBy(qcGene);

      QueryClass qcGOAnnotation = new QueryClass(GOAnnotation.class);
      q.addFrom(qcGOAnnotation);

      QueryClass qcGoTerm = new QueryClass(GOTerm.class);
      q.addFrom(qcGoTerm);
      q.addToSelect(qcGoTerm);

      ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

      QueryObjectReference goAnnotRef = new QueryObjectReference(qcGOAnnotation, "subject");
      cs.addConstraint(new ContainsConstraint(goAnnotRef, ConstraintOp.CONTAINS, qcGene));

      QueryObjectReference goTermRef = new QueryObjectReference(qcGOAnnotation, "ontologyTerm");
      cs.addConstraint(new ContainsConstraint(goTermRef, ConstraintOp.CONTAINS, qcGoTerm));

      q.setConstraint(cs);

      ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
      Iterator<?>  res = os.execute(q, 5000, true, true, true).iterator();
      while(res.hasNext()) {
        ResultsRow<?> rr = (ResultsRow<?>) res.next();
        Gene thisGene = (Gene) rr.get(0);
        GOTerm thisTerm = (GOTerm) rr.get(1);
        LOG.info("There is a known annotation for "+thisGene.getPrimaryIdentifier() + 
            " organism "+thisGene.getOrganism() + " to " + thisTerm.getIdentifier());
        if ( ! knownGO.containsKey(thisGene.getOrganism().getId())) {
          knownGO.put(thisGene.getOrganism().getId(),new HashMap<String,HashSet<String>>());
        }
        if ( ! knownGO.get(thisGene.getOrganism().getId()).containsKey(thisGene.getPrimaryIdentifier())) {
          knownGO.get(thisGene.getOrganism().getId()).put(thisGene.getPrimaryIdentifier(),new HashSet<String>());
        }
        knownGO.get(thisGene.getOrganism().getId()).get(thisGene.getPrimaryIdentifier()).add(thisTerm.getIdentifier());
      }
      return;
      
    }
    /**
     * Query Gene->Protein->Annotation->GOTerm and return an iterator over the Gene
     *  and GOTerm.
     *
     * @param restrictToPrimaryGoTermsOnly Only get primary Annotation items linking the gene
     *  and the go term.
     */
    private Iterator<?> findProteinDomains(boolean restrictToPrimaryGoTermsOnly)
        throws ObjectStoreException {
        Query q = new Query();

        q.setDistinct(true);

        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);
        q.addToOrderBy(qcGene);
        
        QueryClass qcProtein = new QueryClass(Protein.class);
        q.addFrom(qcProtein);
        
        QueryClass qcPAF = new QueryClass(ProteinAnalysisFeature.class);
        q.addFrom(qcPAF);

        QueryClass qcCrossReference= new QueryClass(CrossReference.class);
        q.addFrom(qcCrossReference);

        QueryClass qcProteinDomain = new QueryClass(ProteinDomain.class);
        q.addFrom(qcProteinDomain);
        q.addToSelect(qcProteinDomain);
            
        QueryClass qcAnnotation = new QueryClass(GOAnnotation.class);
        q.addFrom(qcAnnotation);
        
        QueryClass qcGoTerm = new QueryClass(GOTerm.class);
        q.addFrom(qcGoTerm);
        q.addToSelect(qcGoTerm);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference geneProtRef = new QueryCollectionReference(qcProtein, "genes");
        cs.addConstraint(new ContainsConstraint(geneProtRef, ConstraintOp.CONTAINS, qcGene));

        QueryObjectReference protAnalysisRef = new QueryObjectReference(qcPAF, "protein");
        cs.addConstraint(new ContainsConstraint(protAnalysisRef, ConstraintOp.CONTAINS, qcProtein));

        QueryObjectReference crossRefRef = new QueryObjectReference(qcPAF, "crossReference");
        cs.addConstraint(new ContainsConstraint(crossRefRef, ConstraintOp.CONTAINS, qcCrossReference));

        QueryObjectReference proteinDomainRef = new QueryObjectReference(qcCrossReference, "subject");
        cs.addConstraint(new ContainsConstraint(proteinDomainRef, ConstraintOp.CONTAINS, qcProteinDomain));

        QueryCollectionReference goAnnotRef = new QueryCollectionReference(qcProteinDomain, "goAnnotation");
        cs.addConstraint(new ContainsConstraint(goAnnotRef, ConstraintOp.CONTAINS, qcAnnotation));

        QueryObjectReference goTermRef = new QueryObjectReference(qcAnnotation, "ontologyTerm");
        cs.addConstraint(new ContainsConstraint(goTermRef, ConstraintOp.CONTAINS, qcGoTerm));

        q.setConstraint(cs);

        ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
        Results res = os.execute(q, 5000, true, true, true);
        return res.iterator();
    }
}
