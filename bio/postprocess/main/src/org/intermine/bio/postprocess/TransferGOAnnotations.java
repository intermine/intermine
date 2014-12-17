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
 * Take any GOAnnotation objects assigned to proteins and copy them to corresponding genes and proteins.
 * The GO terms are stored slightly differently. At the gene level, they are GOAnnotations. The
 * proteins are stored as OntologyAnnotations.
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
    private HashMap<Integer,HashMap<String,HashSet<String>>> knownGO;
    private HashMap<Integer,HashMap<String,HashSet<String>>> knownOntology;
    private HashMap<String,Integer> goTerms;

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
        knownGO = new HashMap<Integer,HashMap<String,HashSet<String>>>();
        fillGOHash();
        knownOntology = new HashMap<Integer,HashMap<String,HashSet<String>>>();
        fillOntologyHash();
        goTerms = new HashMap<String,Integer>();
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


      Gene lastGene = null;
      Protein lastProtein = null;
      GOTerm lastGOTerm = null;
      HashSet<GOAnnotation> geneAnnotationSet = new HashSet<GOAnnotation>();
      HashSet<OntologyAnnotation> proteinAnnotationSet = new HashSet<OntologyAnnotation>();

      int count = 0;

      Iterator<?> resIter = findProteinDomains();
      while (resIter.hasNext()  ) {
        ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
        Gene thisGene = (Gene) rr.get(0);
        Protein thisProtein = (Protein) rr.get(1);
        ProteinDomain thisDomain = (ProteinDomain) rr.get(2);
        GOTerm thisGOTerm = (GOTerm) rr.get(3);

        LOG.debug("store gene " + thisGene.getPrimaryIdentifier() + " with "
            + thisDomain.getPrimaryIdentifier()+" with term "+thisGOTerm.getName());
        // look at the gene if not already processed for this GO term
        if ((lastGene == null) || (lastGOTerm == null) ||
            (lastGene.getId() != thisGene.getId()) ||
            (lastGOTerm.getId() != thisGOTerm.getId()) ) {
          // special case: this is a new gene. Store the annotationSet from the last one
          if (lastGene != null &&
              geneAnnotationSet.size() > 0 &&
              lastGene.getId() != thisGene.getId() ) {
            lastGene.setGoAnnotation(geneAnnotationSet);
            osw.store(lastGene);
            //TODO: do I need to see if there is something in the goAnnotation
            // collection before adding to it? Or will the new ones be added
            // on top? If we need to save the old, then figure out how to get this to work.
            //annotationSet = (HashSet<GOAnnotation>) thisGene.getGoAnnotation();
            //if (annotationSet == null) {
            geneAnnotationSet = new HashSet<GOAnnotation>();
            //}
          }
          lastGene = thisGene;


          prepareHash(knownGO,thisProtein.getOrganism().getId(),thisProtein.getPrimaryIdentifier());
        
          // store the evidence if not known already and add to the gene's annotation list
          if (!knownGO.get(thisGene.getOrganism().getId()).get(thisGene.getPrimaryIdentifier()).contains(thisGOTerm.getIdentifier()) ){

            LOG.debug("There is a new annotation for the gene "+thisGene.getPrimaryIdentifier() + 
                " organism "+thisGene.getOrganism().getId() + " to " + thisGOTerm.getIdentifier());
            GOEvidence thisEvidence = (GOEvidence)DynamicUtil.createObject(Collections.singleton(GOEvidence.class));
            thisEvidence.setCode(gEC);
            thisEvidence.setWithText(thisDomain.getName());
            osw.store(thisEvidence);
            HashSet<GOEvidence> thisEvidenceSet = new HashSet<GOEvidence>();
            thisEvidenceSet.add(thisEvidence);
            GOAnnotation thisAnnotation = (GOAnnotation) DynamicUtil.createObject(Collections.singleton(GOAnnotation.class));
            thisAnnotation.setEvidence(thisEvidenceSet);
            thisAnnotation.setSubject(thisGene);
            thisAnnotation.setOntologyTerm(thisGOTerm);
            osw.store(thisAnnotation);
            geneAnnotationSet.add(thisAnnotation);
            knownGO.get(thisGene.getOrganism().getId()).get(thisGene.getPrimaryIdentifier()).add(thisGOTerm.getIdentifier());
            count++;
          }
        }

        // now process the protein. These are OntologyAnnotations
        
        if ((lastProtein == null) || (lastGOTerm == null) ||
            (lastProtein.getId() != thisProtein.getId()) ||
            (lastGOTerm.getId() != thisGOTerm.getId()) ) {
          // special case: this is a new protein. Store the annotationSet from the last one
          if (lastProtein != null &&
              proteinAnnotationSet.size() > 0 &&
              lastProtein.getId() != thisGene.getId() ) {
            lastProtein.setOntologyAnnotations(proteinAnnotationSet);
            osw.store(lastProtein);
            //TODO: do I need to see if there is something in the goAnnotation
            // collection before adding to it? Or will the new ones be added
            // on top? If we need to save the old, then figure out how to get this to work.
            //annotationSet = (HashSet<GOAnnotation>) thisGene.getGoAnnotation();
            //if (annotationSet == null) {
            proteinAnnotationSet = new HashSet<OntologyAnnotation>();
            //}
          }
          lastProtein = thisProtein;

          prepareHash(knownOntology,thisProtein.getOrganism().getId(),thisProtein.getPrimaryIdentifier());
          if (!knownOntology.get(thisProtein.getOrganism().getId()).get(thisProtein.getPrimaryIdentifier()).contains(thisGOTerm.getIdentifier()) ){
            OntologyAnnotation thisAnnotation =
                (OntologyAnnotation) DynamicUtil.createObject(Collections.singleton(OntologyAnnotation.class));
            thisAnnotation.setSubject(thisProtein);
            thisAnnotation.setOntologyTerm(thisGOTerm);
            proteinAnnotationSet.add(thisAnnotation);
            osw.store(thisAnnotation);
            knownOntology.get(thisProtein.getOrganism().getId()).get(thisProtein.getPrimaryIdentifier()).add(thisGOTerm.getIdentifier());
            count++;
          }

          lastGOTerm = thisGOTerm;

          if ( (count > 0) && (count%1000 == 0) ) {
            LOG.info("Created "+count+" gene/protein records...");
          }

        }
      }
      // clean up
      if (geneAnnotationSet != null && lastGene != null && geneAnnotationSet.size() > 0) {
        lastGene.setGoAnnotation(geneAnnotationSet);
        osw.store(lastGene);
      }
      if (proteinAnnotationSet != null && lastProtein != null && proteinAnnotationSet.size() > 0) {
        lastProtein.setOntologyAnnotations(proteinAnnotationSet);
        osw.store(lastProtein);
      }

      LOG.info("Created " + count + " new GOAnnotation objects for Genes/Proteins"
          + " - took " + (System.currentTimeMillis() - startTime) + " ms.");
      osw.commitTransaction();

    }

    private void fillGOHash() throws ObjectStoreException {
      
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
        if (thisGene.getOrganism() != null ) {
          LOG.debug("There is a known annotation for "+thisGene.getPrimaryIdentifier() + 
              " organism "+thisGene.getOrganism() + " to " + thisTerm.getIdentifier());
          prepareHash(knownGO,thisGene.getOrganism().getId(),thisGene.getPrimaryIdentifier());
          knownGO.get(thisGene.getOrganism().getId()).get(thisGene.getPrimaryIdentifier()).add(thisTerm.getIdentifier());
        }
      }
      return;
      
    }
    
   
    private void fillOntologyHash() throws ObjectStoreException {

      Query q = new Query();

      q.setDistinct(true);

      QueryClass qcBio = new QueryClass(Protein.class);
      q.addFrom(qcBio);
      q.addToSelect(qcBio);
      q.addToOrderBy(qcBio);

      QueryClass qcOAnnotation = new QueryClass(OntologyAnnotation.class);
      q.addFrom(qcOAnnotation);

      QueryClass qcOTerm = new QueryClass(OntologyTerm.class);
      q.addFrom(qcOTerm);
      q.addToSelect(qcOTerm);

      ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

      QueryObjectReference goAnnotRef = new QueryObjectReference(qcOAnnotation, "subject");
      cs.addConstraint(new ContainsConstraint(goAnnotRef, ConstraintOp.CONTAINS, qcBio));

      QueryObjectReference goTermRef = new QueryObjectReference(qcOAnnotation, "ontologyTerm");
      cs.addConstraint(new ContainsConstraint(goTermRef, ConstraintOp.CONTAINS, qcOTerm));

      q.setConstraint(cs);

      ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
      Iterator<?>  res = os.execute(q, 5000, true, true, true).iterator();
      while(res.hasNext()) {
        ResultsRow<?> rr = (ResultsRow<?>) res.next();
        Protein thisBio = (Protein) rr.get(0);
        OntologyTerm thisTerm = (OntologyTerm) rr.get(1);
        if (thisBio.getOrganism() != null ) {
          LOG.debug("There is a known annotation for "+thisBio.getPrimaryIdentifier() + 
              " organism "+thisBio.getOrganism() + " to " + thisTerm.getIdentifier());
          prepareHash(knownGO,thisBio.getOrganism().getId(),thisBio.getPrimaryIdentifier());
          knownOntology.get(thisBio.getOrganism().getId()).get(thisBio.getPrimaryIdentifier()).add(thisTerm.getIdentifier());
        }
      }
      return;
    }
    /**
     * Query Gene->Protein->Annotation->GOTerm and return an iterator over the Gene
     *  and GOTerm.
     *
     */
    private Iterator<?> findProteinDomains()
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
        q.addToOrderBy(qcProtein);
        
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
        q.addToOrderBy(qcGoTerm);

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
    private void prepareHash(HashMap<Integer,HashMap<String,HashSet<String>>> hash, Integer orgId,String featureName) {
      if( ! hash.containsKey(orgId)) {
        hash.put(orgId,new HashMap<String,HashSet<String>>());
      }
      if ( ! hash.get(orgId).containsKey(featureName)) {
        hash.get(orgId).put(featureName,new HashSet<String>());
      }
      return;
    }
}
