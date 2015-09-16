/**
 * 
 */
package org.intermine.bio.postprocess;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.intermine.bio.util.Constants;
import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.MetaDataException;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.GeneFlankingRegion;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.GOAnnotation;
//import org.intermine.model.bio.GOSummary;
import org.intermine.model.bio.GOTerm;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.proxy.ProxyReference;
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
import org.intermine.util.DynamicUtil;


/**
 * @author jcarlson
 *
 */
public class GOSummarizer {

  private static final Logger LOG = Logger.getLogger(GOSummarizer.class);
  protected ObjectStore os;
  protected ObjectStoreWriter osw = null;
/*
  // a hash map of the GOSummary terms. The First Integer key is the key for the
  // organism id. The second Integer key is referenced GO Term. The value is a HashSet
  // of gene id's.
  protected HashMap<Integer,HashMap<Integer,GOSummary>> goSummaryMap;
  // a second hash map of the organism id, term and a set of genes. This is
  // to guard against double counting.
  protected HashMap<Integer,HashMap<Integer,HashSet<Integer>>> geneSummaryMap;
  protected HashMap<Integer,ProxyReference> orgMap;
  // this is a set of 'roots' of all namespaces.
  protected HashMap<Integer,GOSummary> superRoot;*/
  
  public  GOSummarizer(ObjectStoreWriter osw) {    
    this.osw = osw;
    this.os = osw.getObjectStore();
 /*   geneSummaryMap = new HashMap<Integer,HashMap<Integer,HashSet<Integer>>>();
    goSummaryMap = new HashMap<Integer,HashMap<Integer,GOSummary>>();
    orgMap = new HashMap<Integer,ProxyReference>();
    superRoot = new HashMap<Integer,GOSummary>();*/
  }
  
  /**
   * Copy all GO annotations from the Protein objects to the corresponding Gene(s)
   * @throws ObjectStoreException if anything goes wrong
   */
  public void execute() throws ObjectStoreException {

    Results res = findGOCounts();
    
    ProxyReference orgP = null;
    ProxyReference termP = null;
    Integer lastGeneId = null;
    
    Iterator<?> it = res.iterator();
  /*  
    while(it.hasNext()) {
      ResultsRow<?> rr = (ResultsRow<?>) it.next();
      GOTerm t = (GOTerm) rr.get(0);
      Organism o = (Organism) rr.get(1);
      Gene g = (Gene) rr.get(2);
      if (orgP==null || !o.getId().equals(orgP.getId())) {
        makeSummaryCount(orgP);
        orgP = new ProxyReference(osw,o.getId(),Organism.class);
        termP = new ProxyReference(osw,t.getId(),GOTerm.class);
        orgMap.put(o.getId(),orgP);
        goSummaryMap.put(o.getId(),new HashMap<Integer,GOSummary>());
        geneSummaryMap.put(o.getId(),new HashMap<Integer,HashSet<Integer>>());
        geneSummaryMap.get(o.getId()).put(t.getId(),new HashSet<Integer>());
        geneSummaryMap.get(o.getId()).get(t.getId()).add(g.getId());
      } else if (!t.getId().equals(termP.getId()) ) {
        // moved to the next term or organism
        makeTermCount(termP,orgP);
        termP = new ProxyReference(osw,t.getId(),GOTerm.class);
        geneSummaryMap.get(o.getId()).put(t.getId(),new HashSet<Integer>());
        geneSummaryMap.get(o.getId()).get(t.getId()).add(g.getId());
      } else {
        // same organism, same term. Just add an id.
        geneSummaryMap.get(o.getId()).get(t.getId()).add(g.getId());
      }
    }*/
    // last one
    makeTermCount(termP,orgP);
    makeSummaryCount(orgP);
    storeResults();
  }
  
  private void makeSummaryCount(ProxyReference orgP) throws ObjectStoreException
  {
    if (orgP == null ) return;
    // Look every GOSummary record for this organism, then combine the sets of
    // gene id's
   /* 
    // we're also going to make a 'super' root: the parent of every namespace
    GOSummary rootTerm = (GOSummary) DynamicUtil
        .createObject(Collections.singleton(GOSummary.class));

    rootTerm.setCount(0);
    rootTerm.proxyOrganism(orgP);
    superRoot.put(orgP.getId(),rootTerm);
    HashSet<Integer> everyGeneId = new HashSet<Integer>();
    
    // Make a copy of the things to process.
    // We don't want a ConcurrentModificationException
    Set<Integer> ids = new HashSet<Integer>(goSummaryMap.get(orgP.getId()).keySet());

    for(Integer id : ids ) {
      GOSummary gS = goSummaryMap.get(orgP.getId()).get(id);
      GOTerm t = gS.getTerm();
      // find the parents of this
      Query p = new Query();
      QueryClass qcParents = new QueryClass(GOTerm.class);
      p.addToSelect(qcParents);
      p.addFrom(qcParents);
      p.setConstraint(new ContainsConstraint(new QueryCollectionReference(t,"parents"),
          ConstraintOp.CONTAINS,qcParents));

      Results res = os.execute(p, 200, true, true, true);
      Iterator<?> it = res.iterator();
      while (it.hasNext()) {
        ResultsRow<?> rr = (ResultsRow<?>) it.next();
        GOTerm pT = (GOTerm) rr.get(0);
        if ( ! goSummaryMap.get(orgP.getId()).containsKey(pT.getId()) ) {
          GOSummary summary = (GOSummary) DynamicUtil
              .createObject(Collections.singleton(GOSummary.class));      
          summary.setCount(0);
          summary.proxyOrganism(orgP);
          summary.proxyTerm(new ProxyReference(osw,pT.getId(),GOTerm.class));
          goSummaryMap.get(orgP.getId()).put(pT.getId(),summary);
          geneSummaryMap.get(orgP.getId()).put(pT.getId(),new HashSet<Integer>());
        } 
        // appends genes of current term to parent term
        geneSummaryMap.get(orgP.getId()).get(pT.getId()).addAll(geneSummaryMap.get(orgP.getId()).get(t.getId()));
      }
      // and append every gene to the superroot
      everyGeneId.addAll(geneSummaryMap.get(orgP.getId()).get(id));
    }
    rootTerm.setTotal(everyGeneId.size());
    Set<Integer> revisedIds = new HashSet<Integer>(goSummaryMap.get(orgP.getId()).keySet());
    for(Integer id : revisedIds) {
      // update the total counts
      Integer count = geneSummaryMap.get(orgP.getId()).get(id).size();
      goSummaryMap.get(orgP.getId()).get(id).setTotal(count);
    }*/
  }
  private void storeResults() throws ObjectStoreException
  {
    // now store everything
 /*   for( Integer orgId : goSummaryMap.keySet() ) {
      HashMap<Integer,GOSummary> orgGoSummary = goSummaryMap.get(orgId);
      for(GOSummary gS : orgGoSummary.values() ) {
        osw.store(gS);
      }
      osw.store(superRoot.get(orgId));
    }*/
  }
  
  private void makeTermCount(ProxyReference termP,ProxyReference orgP) throws ObjectStoreException
  {
    if (termP == null || orgP == null) return;
  /*  
    GOSummary summary = (GOSummary) DynamicUtil
        .createObject(Collections.singleton(GOSummary.class));
    
    summary.setCount(geneSummaryMap.get(orgP.getId()).get(termP.getId()).size());
    summary.proxyOrganism(orgP);
    summary.proxyTerm(termP);
    goSummaryMap.get(orgP.getId()).put(termP.getId(),summary);*/
    
  }

  private Results findGOCounts()
      throws ObjectStoreException {
      Query q = new Query();

      q.setDistinct(true);

      QueryClass qcGOAnnotation = new QueryClass(GOAnnotation.class);
      QueryClass qcGene = new QueryClass(Gene.class);
      QueryClass qcOrganism = new QueryClass(Organism.class);
      QueryClass qcGOTerm = new QueryClass(GOTerm.class);
      
      q.addFrom(qcGOAnnotation);
      q.addFrom(qcGene);
      q.addFrom(qcOrganism);
      q.addFrom(qcGOTerm);
      
      q.addToSelect(qcGOTerm);
      q.addToSelect(qcOrganism);
      q.addToSelect(qcGene);

      q.addToOrderBy(qcOrganism);
      q.addToOrderBy(qcGOTerm);
      q.addToOrderBy(qcGene);
      
      ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

      cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qcGene, "organism"),ConstraintOp.CONTAINS,qcOrganism));
      cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qcGOAnnotation, "subject"),ConstraintOp.CONTAINS,qcGene));
      cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qcGOAnnotation, "ontologyTerm"),ConstraintOp.CONTAINS,qcGOTerm));

      q.setConstraint(cs);
      
      ((ObjectStoreInterMineImpl) os).precompute(q, Constants.PRECOMPUTE_CATEGORY);
      return os.execute(q, 5000, true, true, true);
  }
}

