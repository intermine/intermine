package org.intermine.bio.postprocess;


import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.bio.util.Constants;
import org.intermine.model.bio.CrossReference;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.GOAnnotation;
import org.intermine.model.bio.GOEvidence;
import org.intermine.model.bio.GOEvidenceCode;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Ontology;
import org.intermine.model.bio.OntologyTerm;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.Publication;
import org.intermine.objectstore.ObjectStore;
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
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.postprocess.PostProcessor;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

public class OntologyTermsPostProcess extends PostProcessor {

  private static final Logger LOG = Logger.getLogger(OntologyTermsPostProcess.class);
  protected ObjectStore os;

  protected String crossreferenceName = null;
  protected HashMap<String,String> xRef2Term = null;
  protected String srcDataFile = null;
  protected String srcMapFile = null;
  protected String srcDataDir = null;
  protected String dataSetTitle = null;
  /**
   * Create a new UpdateOrthologes object from an ObjectStoreWriter
   * @param osw writer on genomic ObjectStore
   */
  public OntologyTermsPostProcess(ObjectStoreWriter osw) {
      super(osw);
      this.os = osw.getObjectStore();
  }
  /**
   * Associate the ontology terms with cross references. This is usually just
   * looking for cases where ontologyterm.identifier=crossreference.identifier
   * and datasource.name=ontology.name. But there can be exceptions. Subclass
   * may use slightly different values for datasource.name and ontology.name,
   * and there may be a map file to that multiple crossreference.identifiers
   * can map one ontologyterm.identifier.
   */
  @Override
  public void postProcess() throws BuildException {
    // default is to set crossreference source name to be the same
    // as ontology name
    if (crossreferenceName==null) {
      crossreferenceName=dataSetTitle;
    }
    if (dataSetTitle == null) {
      throw new BuildException("Data Set Title must be set.");
    }
    // First, get all terms for this ontology
    HashMap<String,OntologyTerm> termMap = new HashMap<String,OntologyTerm>();
    Query q1 = new Query();
    QueryClass qOnt = new QueryClass(Ontology.class);
    q1.addFrom(qOnt);
    QueryField qOntF = new QueryField(qOnt,"name");
    QueryValue qOntV = new QueryValue(dataSetTitle);
    QueryClass qOntT = new QueryClass(OntologyTerm.class);
    q1.addFrom(qOntT);
    q1.addToSelect(qOntT);
    QueryObjectReference qOR = new QueryObjectReference(qOntT,"ontology");
    ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
    cs1.addConstraint(new ContainsConstraint(qOR,ConstraintOp.CONTAINS,qOnt));
    cs1.addConstraint(new SimpleConstraint(qOntF,ConstraintOp.EQUALS,qOntV));
    q1.setConstraint(cs1);
    q1.setDistinct(true);
    Results res1 = os.execute(q1);
    Iterator<Object> res1Iter = res1.iterator();
    LOG.info("Getting Ontology terms...");
    int ctr1 = 0;
    while (res1Iter.hasNext()) {
      @SuppressWarnings("unchecked")
      ResultsRow<OntologyTerm> rr = (ResultsRow<OntologyTerm>) res1Iter.next();
      OntologyTerm r = rr.get(0);
      termMap.put(r.getIdentifier(),r);
      ctr1++;
    }
    LOG.info("Retrieved "+ctr1+" Ontology terms.");

    // Next get all cross references for this source
    HashMap<String,CrossReference> xrefMap = new HashMap<String,CrossReference>();
    Query q2 = new Query();
    QueryClass qDS = new QueryClass(DataSource.class);
    QueryField qDSF = new QueryField(qDS,"name");
    QueryValue qDSV = new QueryValue(crossreferenceName);
    q2.addFrom(qDS);
    QueryClass qCR = new QueryClass(CrossReference.class);
    q2.addFrom(qCR);
    q2.addToSelect(qCR);
    QueryObjectReference qCROR = new QueryObjectReference(qCR,"source");
    ConstraintSet cs2 = new ConstraintSet(ConstraintOp.AND);
    cs2.addConstraint(new ContainsConstraint(qCROR,ConstraintOp.CONTAINS,qDS));
    cs2.addConstraint(new SimpleConstraint(qDSF,ConstraintOp.EQUALS,qDSV));
    q2.setConstraint(cs2);
    q2.setDistinct(true);
    Results res2 = os.execute(q2);
    Iterator<Object> res2Iter = res2.iterator();
    LOG.info("Getting cross references...");
    int ctr2 = 0;
    while (res2Iter.hasNext()) {
      @SuppressWarnings("unchecked")
      ResultsRow<CrossReference> rr = (ResultsRow<CrossReference>) res2Iter.next();
      CrossReference r = rr.get(0);
      xrefMap.put(r.getIdentifier(),r);
      ctr2++;
    }

    LOG.info("Retrieved "+ctr2+" Crossreferences.");
    
    HashMap<String,HashSet<CrossReference>> termToStore = 
        new HashMap<String,HashSet<CrossReference>>();
    for( String xref : xrefMap.keySet()) {
      String term = xref;
      if (xRef2Term != null && xRef2Term.containsKey(xref) ) {
        term = xRef2Term.get(xref);
      }
      if (termMap.containsKey(term) ){
        if( !termToStore.containsKey(term)) {
          termToStore.put(term, new HashSet<CrossReference>());
        }
        termToStore.get(term).add(xrefMap.get(xref));
      }
    }
    // also look for cases where the map is actually term2xref. We
    // need only consider the case of xRef2Term != null
    if (xRef2Term != null) {
      for( String term : termMap.keySet()) {
        if (xRef2Term.containsKey(term) && xrefMap.containsKey(xRef2Term.get(term))) {
          if (!termToStore.containsKey(term)) {
            termToStore.put(term,new HashSet<CrossReference>());
          }
          termToStore.get(term).add(xrefMap.get(xRef2Term.get(term)));
        }
      }
    }

    try {
      osw.beginTransaction();
    } catch (ObjectStoreException e) {
      throw new BuildException("Trouble initiating a transaction: "+e.getMessage());
    }
    LOG.info("Need to store "+termToStore.size()+" Ontology Terms...");
    for( String term : termToStore.keySet()) {
      OntologyTerm t = termMap.get(term);
      t.setFieldValue("xrefs",termToStore.get(term));
      try {
        osw.store(t);
      } catch (ObjectStoreException e) {
        throw new BuildException("Trouble storing Ontology Term: "+e.getMessage());
      }
    }

    try {
      osw.commitTransaction();
    } catch (ObjectStoreException e) {
      throw new BuildException("Trouble ending a transaction: "+e.getMessage());
    }
  }

 
  public void setSrcMapFile(String fileName) {
    srcMapFile = fileName;
    if (srcDataDir != null) readMapFile();
  }

  public void setSrcDataDir(String dir) {
    srcDataDir = dir;
    if (srcMapFile != null) readMapFile();
  }
  public void setDataSetTitle(String title) {
    dataSetTitle = title;
  }
  /**
   * Read a file with the mapping of xref identifiers to term identifier.
   * @param fileName Name of the file in src.data.dir
   * @throws BuildException
   */
  private void readMapFile() throws BuildException {
    if (srcDataDir == null ) {
      throw new BuildException("Source directory for the map file is not set.");
    }
    if (srcMapFile == null ) {
      throw new BuildException("Source map file is not set.");
    }
    try {
      BufferedReader in = new BufferedReader(new FileReader(srcDataDir+"/"+srcMapFile));
      Iterator<?> tsvIter;
      try {
        tsvIter = FormattedTextParser.parseTabDelimitedReader(in);
      } catch (Exception e) {
        in.close();
        throw new BuildException("Cannot parse file: " + srcMapFile + ": "+e.getMessage());
      }
      int ctr = 0;
      xRef2Term = new HashMap<String,String>();
      while (tsvIter.hasNext() ) {
        ctr++;
        String[] fields = (String[]) tsvIter.next();
        if ( fields.length >= 2) {
          xRef2Term.put(fields[0],fields[1]);
        }
      }
      LOG.info("Read "+ctr+" entries from map file.");
      in.close();
      } catch (Exception e) {
        throw new BuildException("Problem opening file: "+e.getMessage());
      }
  }
  
}
