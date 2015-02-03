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
import org.intermine.model.bio.CrossReference;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.GOAnnotation;
import org.intermine.model.bio.GOEvidence;
import org.intermine.model.bio.GOTerm;
import org.intermine.model.bio.Gene;
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
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.postprocess.PostProcessor;
import org.intermine.util.DynamicUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

/**
 * this was originally written as a test to see if adding the SimpleObject table
 * linking the SNPs to the samples could be done faster as a postprocessing step
 * than as an integration step. It can.
 * There is much code duplication between this and the integration processor. tsk tsk tsk.
 * @author jcarlson
 *
 */
public class PhytozomeDbPostProcess extends PostProcessor {

  Integer proteomeId;
  // we'll want to know all registered ontologies
  HashMap<String,Item> ontologyMap;
  // and all registered terms for thos ontologies
  HashMap<String,HashMap<String,Item>> ontologyTermMap;
  // already registered terms for 
  HashMap<String,HashMap<String,Item>> knownOntologyTerms;
  
  // some xref from interproscan are not exactly the same name as
  // what we pick up from the ontology. Anything not in this
  // map is assumed to be unchanged.
  private static final HashMap<String,String> xrefToOntologyMap;
  static
  {
    xrefToOntologyMap = new HashMap<String, String>();
    xrefToOntologyMap.put("TIGRFAMs", "TIGRFAM");
    xrefToOntologyMap.put("PIRSF", "PIR");
    xrefToOntologyMap.put("InterPro", "Interpro");
  }
 
  private static final Logger LOG = Logger.getLogger(PhytozomeDbPostProcess.class);

  public PhytozomeDbPostProcess(ObjectStoreWriter osw) {
    super(osw);
    this.osw = osw;
    proteomeId = null;
    knownOntologyTerms = new HashMap<String,HashMap<String,Item>>();
    ontologyMap = new HashMap<String,Item>();
    ontologyTermMap = new HashMap<String, HashMap<String,Item>>();
  }

  public void postProcess() throws BuildException, ObjectStoreException {
    if (proteomeId==null) {
      LOG.error("Proteome Id is not set.");
      throw new BuildException("Proteome Id is not set.");
    }
    HashSet<CrossReference> geneXref = null;
    HashSet<CrossReference> proteinXref = null;
    Gene lastGene = null;
    Protein lastProtein = null;
    
    findAllOntology();
    findAllOntologyTerms();
    findKnownOntologyTerms();
    
    Iterator<?> crossRefs = findAnalysisCrossReferences();
    while (crossRefs.hasNext()  ) {
      ResultsRow<?> rr = (ResultsRow<?>) crossRefs.next();
      Gene thisGene = (Gene) rr.get(0);
      Protein thisProtein = (Protein) rr.get(1);
      CrossReference xref = (CrossReference) rr.get(2);
      DataSource source = (DataSource) rr.get(3);
      LOG.info("Adding gene "+thisGene.getId()+" and protein "+thisProtein.getId()+" to cross ref "+
      xref.getIdentifier() +":"+ source.getName());
      if (!knownOntologyTerms.containsKey(source.getName())) {
        knownOntologyTerms.put(source.getName(),new HashMap<String,Item>());
      }
   /*   if (!ontologyTerms.get(ontologyName).containsKey(ontologyTerm) {
      }
      if ( (lastGene==null) || (!thisGene.getId().equals(lastGene.getId())) ) {
        storeXRef(lastGene,geneXref);
        lastGene = thisGene;
        geneXref = new HashSet<CrossReference>();
      }
      if ( (lastProtein==null) || (!thisProtein.getId().equals(lastProtein.getId())) ) {
        storeXRef(lastProtein,proteinXref);
        lastProtein = thisProtein;
        proteinXref = new HashSet<CrossReference>();
      }
      proteinXref.add(crossRef);
      geneXref.add(crossRef);*/
    }
    storeXRef(lastGene,geneXref);
    storeXRef(lastProtein,proteinXref);
    
  }

  private Iterator<?> findAnalysisCrossReferences() throws BuildException {
    
    try {
        Query q = new Query();

        q.setDistinct(true);

        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        //QueryField qcGeneId = new QueryField(qcGene,"id");
        q.addToSelect(qcGene);
        q.addToOrderBy(qcGene);
        
        QueryClass qcProtein = new QueryClass(Protein.class);
        q.addFrom(qcProtein);
        //QueryField qcProteinId = new QueryField(qcProtein,"id");
        q.addToSelect(qcProtein);
        q.addToOrderBy(qcProtein);
        
        QueryClass qcOrg = new QueryClass(Organism.class);
        q.addFrom(qcOrg);
        QueryField qcOrgProt = new QueryField(qcOrg,"proteomeId");
        QueryClass qcPAF = new QueryClass(ProteinAnalysisFeature.class);
        q.addFrom(qcPAF);

        QueryClass qcCrossReference= new QueryClass(CrossReference.class);
        q.addFrom(qcCrossReference);
        q.addToSelect(qcCrossReference);
        
        QueryClass qcSource = new QueryClass(DataSource.class);
        q.addFrom(qcSource);
        q.addToSelect(qcSource);


        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryCollectionReference geneProtRef = new QueryCollectionReference(qcProtein, "genes");
        cs.addConstraint(new ContainsConstraint(geneProtRef, ConstraintOp.CONTAINS, qcGene));

        QueryObjectReference orgRef = new QueryObjectReference(qcGene,"organism");
        cs.addConstraint(new ContainsConstraint(orgRef, ConstraintOp.CONTAINS, qcOrg));
        
        QueryObjectReference protAnalysisRef = new QueryObjectReference(qcPAF, "protein");
        cs.addConstraint(new ContainsConstraint(protAnalysisRef, ConstraintOp.CONTAINS, qcProtein));
        
        cs.addConstraint(new SimpleConstraint(qcOrgProt,ConstraintOp.EQUALS,new QueryValue(proteomeId)));
        
        QueryObjectReference crossRefRef = new QueryObjectReference(qcPAF, "crossReference");
        cs.addConstraint(new ContainsConstraint(crossRefRef, ConstraintOp.CONTAINS, qcCrossReference));
        
        QueryObjectReference sourceRef = new QueryObjectReference(qcCrossReference,"source");
        cs.addConstraint(new ContainsConstraint(sourceRef,ConstraintOp.CONTAINS, qcSource));

        q.setConstraint(cs);

        ((ObjectStoreInterMineImpl) osw.getObjectStore()).precompute(q, Constants.PRECOMPUTE_CATEGORY);
        Results res = osw.getObjectStore().execute(q, 5000, true, true, true);
        return res.iterator();
    
    } catch (ObjectStoreException e) {
      LOG.error("Problem in query: " + e.getMessage());
      throw new BuildException("Problem in query: " + e.getMessage());
    }
  }
 
  private void findAllOntology() {
    try {
      Query q = new Query();

      q.setDistinct(true);

      QueryClass qcOntology = new QueryClass(Ontology.class);
      q.addFrom(qcOntology);
      q.addToSelect(qcOntology);

      ((ObjectStoreInterMineImpl) osw.getObjectStore()).precompute(q, Constants.PRECOMPUTE_CATEGORY);
      Results res = osw.getObjectStore().execute(q, 5000, true, true, true);
      //return res.iterator();
  
  } catch (ObjectStoreException e) {
    LOG.error("Problem in query: " + e.getMessage());
    throw new BuildException("Problem in query: " + e.getMessage());
  }
  }
  
  private void findAllOntologyTerms() {
    
  }
  private void findKnownOntologyTerms() {
    // find every known annotation for this gene
    try {
      Query q = new Query();

      q.setDistinct(true);

      QueryClass qcGene = new QueryClass(Gene.class);
      q.addFrom(qcGene);
      q.addToSelect(qcGene);
      
      QueryClass qcOrg = new QueryClass(Organism.class);
      q.addFrom(qcOrg);
      QueryField qcOrgProt = new QueryField(qcOrg,"proteomeId");
      QueryClass qcOA = new QueryClass(OntologyAnnotation.class);
      q.addFrom(qcOA);
      QueryClass qcOT = new QueryClass(OntologyTerm.class);
      q.addFrom(qcOT);
      q.addToSelect(qcOT);
      QueryClass qcO = new QueryClass(Ontology.class);
      q.addFrom(qcO);
      q.addToSelect(qcO);


      ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

      cs.addConstraint(new SimpleConstraint(qcOrgProt,ConstraintOp.EQUALS,new QueryValue(proteomeId)));
      QueryObjectReference orgRef = new QueryObjectReference(qcGene,"organism");
      cs.addConstraint(new ContainsConstraint(orgRef, ConstraintOp.CONTAINS, qcOrg));
      
      QueryObjectReference ontAnnSubjectRef = new QueryObjectReference(qcOA, "subject");
      cs.addConstraint(new ContainsConstraint(ontAnnSubjectRef, ConstraintOp.CONTAINS, qcGene));
      
      QueryObjectReference ontAnnTermRef = new QueryObjectReference(qcOA, "ontologyTerm");
      cs.addConstraint(new ContainsConstraint(ontAnnTermRef, ConstraintOp.CONTAINS, qcOT));
      
      QueryObjectReference ontRef = new QueryObjectReference(qcOT, "ontology");
      cs.addConstraint(new ContainsConstraint(ontRef, ConstraintOp.CONTAINS, qcO));

      q.setConstraint(cs);

      ((ObjectStoreInterMineImpl) osw.getObjectStore()).precompute(q, Constants.PRECOMPUTE_CATEGORY);
      Results res = osw.getObjectStore().execute(q, 5000, true, true, true);
      Iterator<?> resIter = res.iterator();
      while (resIter.hasNext()) {
        ResultsRow<?> rr = (ResultsRow<?>)resIter.next();
        Gene g = (Gene)rr.get(0);
        OntologyTerm oT = (OntologyTerm)rr.get(1);
        Ontology o = (Ontology)rr.get(2); 
        
      }
    } catch (ObjectStoreException e) {
      LOG.error("Problem in query: " + e.getMessage());
      throw new BuildException("Problem in query: " + e.getMessage());
    }
    }
  
    
  /*private void blah() {
    Iterator<?> resIter = findProteinDomains();

    int count = 0;
    Map<OntologyTerm, GOAnnotation> annotations = new HashMap<OntologyTerm, GOAnnotation>();

    Gene lastGene = null;
    GOTerm lastGOTerm = null;
    HashSet<GOAnnotation> annotationSet = new HashSet<GOAnnotation>();
    while (resIter.hasNext()  ) {
      ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
      Gene thisGene = (Gene) rr.get(0);
      Protein thisProtein = (Protein) rr.get(1);
      ProteinDomain thisDomain = (ProteinDomain) rr.get(2);
      GOTerm thisGOTerm = (GOTerm) rr.get(3);

      LOG.debug("store gene " + thisGene.getPrimaryIdentifier() + " with "
          + thisDomain.getPrimaryIdentifier()+" with term "+thisGOTerm.getName());
      // look at the gene if not already processed for this GO term
      if ((lastGene == null) ||
          (lastGOTerm == null) ||
          (lastGene.getId() != thisGene.getId()) ||
          (lastGOTerm.getId() != thisGOTerm.getId()) ) {
        // special case: this is a new gene. Store the annotationSet from the last one
        if (lastGene != null &&
            annotationSet.size() > 0 &&
            lastGene.getId() != thisGene.getId() ) {
          lastGene.setGoAnnotation(annotationSet);
          osw.store(lastGene);
          //TODO: do I need to see if there is something in the goAnnotation
          // collection before adding to it? Or will the new ones be added
          // on top? If we need to save the old, then figure out how to get this to work.
          //annotationSet = (HashSet<GOAnnotation>) thisGene.getGoAnnotation();
          //if (annotationSet == null) {
            annotationSet = new HashSet<GOAnnotation>();
          //}
        }
        lastGene = thisGene;
        lastGOTerm = thisGOTerm;

        // store if not known already
        if (!knownGO.containsKey(thisGene.getOrganism().getId()) ||
            !knownGO.get(thisGene.getOrganism().getId()).containsKey(thisGene.getPrimaryIdentifier()) ||
            !knownGO.get(thisGene.getOrganism().getId()).get(thisGene.getPrimaryIdentifier()).contains(thisGOTerm.getIdentifier()) ){

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
          annotationSet.add(thisAnnotation);
          count++;
        }
      }
      // now process the protein
      if (!knownGO.containsKey(thisProtein.getOrganism().getId()) ||
          !knownGO.get(thisProtein.getOrganism().getId()).containsKey(thisProtein.getPrimaryIdentifier()) ||
          !knownGO.get(thisProtein.getOrganism().getId()).get(thisProtein.getPrimaryIdentifier()).contains(thisGOTerm.getIdentifier()) ){

        LOG.debug("There is a new annotation for the protein "+thisProtein.getPrimaryIdentifier() + 
            " organism "+thisProtein.getOrganism().getId() + " to " + thisGOTerm.getIdentifier());
        GOEvidence thisEvidence = (GOEvidence)DynamicUtil.createObject(Collections.singleton(GOEvidence.class));
        thisEvidence.setCode(gEC);
        thisEvidence.setWithText(thisDomain.getName());
        osw.store(thisEvidence);
        HashSet<GOEvidence> thisEvidenceSet = new HashSet<GOEvidence>();
        thisEvidenceSet.add(thisEvidence);
        GOAnnotation thisAnnotation = (GOAnnotation) DynamicUtil.createObject(Collections.singleton(GOAnnotation.class));
        thisAnnotation.setEvidence(thisEvidenceSet);
        thisAnnotation.setSubject(thisProtein);
        thisAnnotation.setOntologyTerm(thisGOTerm);
        osw.store(thisAnnotation);
        count++;
      }
      
      if ( (count > 0) && (count%1000 == 0) ) {
        LOG.info("Created "+count+" gene/protein records...");
      }
    }
    
    // clean up goannotation
    if (annotationSet != null && lastGene != null && annotationSet.size() > 0) {
      lastGene.setGoAnnotation(annotationSet);
      osw.store(lastGene);
    }

    LOG.info("Created " + count + " new GOAnnotation objects for Genes/Proteins"
            + " - took " + (System.currentTimeMillis() - startTime) + " ms.");
    osw.commitTransaction();
}*/

  private void storeXRef(BioEntity e, HashSet<CrossReference> xrefs) {
    if(e==null) return;
    if(xrefs==null) return;
    
    //ReferenceList rL = new ReferenceList("crossReferences");
    for( CrossReference xref : xrefs) {
      //rL.addRefId(xref.toString());
      e.addCrossReferences(xref);
      
    }
    //((Item)e).addCollection(rL);
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
