package org.intermine.bio.dataconversion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.bio.dataconversion.CoexpressionConverter.Pair;
import org.intermine.dataloader.IntegrationWriterDataTrackingImpl;
import org.intermine.metadata.ConstraintOp;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Coexpression;
import org.intermine.model.bio.CoexpressionJSON;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Ontology;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.SOTerm;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.ProxyReference;
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
import org.intermine.task.FileDirectDataLoaderTask;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

/**
 * A DirectDataLoader for Phytozome diversity data. This skips the items step completely
 * and creates/store InterMineObjects directly, providing significant speed increase and
 * removing need for a separate post-processing step.
 * 
 * Skipping the items step means that queries for merging objects in the target database
 * are run individually rather than in batches. This is slower but here the number of
 * objects being merged (organism, genes, mRNAs, etc) is very small compared to the total
 * data size. 
 * 
 * @author Richard Smith
 *
 */
public class CoexpressionDirectDataLoaderTask extends FileDirectDataLoaderTask {
  // NOTE if DataSet and DataSource aren't important in the webapp could disable
  // creating and setting references to save some disk writing.
  private static final String DATASET_TITLE = "RNA-seq Coexpressio Data";
  private static final String DATA_SOURCE_NAME = "Phytozome";
  private static final Logger LOG = Logger.getLogger(CoexpressionDirectDataLoaderTask.class);

  // the one organism we're working on. proteomeId is set by setter.
  private Integer proteomeId = null;
  private ProxyReference organism = null;
  // bioentities we record data about
  private HashMap<String,ProxyReference> geneProxyMap = null;
  private HashMap<String,ReferenceList> collections = new HashMap<String,ReferenceList>();
  private Double bucketSize = new Double(.05);
  private Integer bucketNumber = 3;
  private HashMap<String,ArrayList<Pair>> jsonBuckets = new HashMap<String,ArrayList<Pair>>();


  public void setProteomeId(String proteome) {
    try {
      proteomeId = Integer.valueOf(proteome);
    } catch (NumberFormatException e) {
      throw new RuntimeException(
          "Cannot find numerical proteome id for: " + proteome);
    }
  }

  /**
   * Called by parent process method for each file found
   *
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  public void processFile(File theFile) {
    String message = "Processing file: " + theFile.getName();
    System.out.println(message);
    LOG.info(message);

    if (!theFile.getName().equals("coexpression.tsv")) {
      return;
    }

    if (organism == null) getOrganism();
    if (geneProxyMap == null) prefillGeneProxy();

    Iterator<?> tsvIter;
    try {
      FileReader reader = new FileReader(theFile);
      tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
    } catch (Exception e) {
      throw new BuildException("Cannot parse file: "
          + theFile, e);
    }
    int lineNumber = 0;
    while (tsvIter.hasNext()) {
      String[] fields = (String[]) tsvIter.next();
      lineNumber++;
      if ( (lineNumber%1000000) == 0 ) {
        LOG.info("Processed "+lineNumber+" lines...");
      }
      if (fields.length>=3 && !fields[0].equals(fields[1]) && 
          (Double.parseDouble(fields[2]) >= 1. - bucketSize*bucketNumber) ) {
        if (!geneProxyMap.containsKey(fields[0]) ) {
          try {
            Gene i = getDirectDataLoader().createObject(Gene.class);
            i.setSecondaryIdentifier(fields[0]);
            i.proxyOrganism(organism);
            getDirectDataLoader().store(i);
            ProxyReference ref = new ProxyReference(getIntegrationWriter().getObjectStore(),i.getId(),Gene.class);
            geneProxyMap.put(fields[0],ref);
          } catch (ObjectStoreException e) {
            throw new BuildException("Problem storing Gene: " + e.getMessage());
          }
        }
        if (!geneProxyMap.containsKey(fields[1]) ) {
          try {
            Gene i = getDirectDataLoader().createObject(Gene.class);
            i.setSecondaryIdentifier(fields[1]);
            i.proxyOrganism(organism);
            getDirectDataLoader().store(i);
            ProxyReference ref = new ProxyReference(getIntegrationWriter().getObjectStore(),i.getId(),Gene.class);
            geneProxyMap.put(fields[1],ref);
          } catch (ObjectStoreException e) {
            throw new BuildException("Problem storing Gene: " + e.getMessage());
          }
        }
        
        if (!jsonBuckets.containsKey(fields[0])) jsonBuckets.put(fields[0],new ArrayList<Pair>());
        jsonBuckets.get(fields[0]).add(new Pair(fields[1], Double.parseDouble(fields[2])));
        
        if (!jsonBuckets.containsKey(fields[1])) jsonBuckets.put(fields[1],new ArrayList<Pair>());
        jsonBuckets.get(fields[1]).add(new Pair(fields[0], Double.parseDouble(fields[2])));

        try {
          Coexpression i = getDirectDataLoader().createSimpleObject(Coexpression.class);
          i.proxyGene(geneProxyMap.get(fields[0]));
          i.proxyCoexpressedGene(geneProxyMap.get(fields[1]));
          i.setCorrelation(Float.parseFloat(fields[2]));
          if (fields.length > 3) {
            // optional group argument
            i.setExperimentGroup(fields[3]);
          }
          getDirectDataLoader().store(i);
        } catch (ObjectStoreException e) {
          throw new BuildException("Problem storing Coexpression: " + e.getMessage());
        }

        try {
          Coexpression j = getDirectDataLoader().createSimpleObject(Coexpression.class);
          j.proxyGene(geneProxyMap.get(fields[1]));
          j.proxyCoexpressedGene(geneProxyMap.get(fields[0]));
          j.setCorrelation(Float.parseFloat(fields[2]));
          getDirectDataLoader().store(j);
        } catch (ObjectStoreException e) {
          throw new BuildException("Problem storing Coexpression: " + e.getMessage());
        }
      }
    }
    
    // load the json
    for(String geneId: jsonBuckets.keySet()) {
      ArrayList<Pair> coexpressionList = jsonBuckets.get(geneId);
      StringBuffer thisJSON = new StringBuffer();
      Collections.sort(coexpressionList);
      Double thisLowRange = new Double(1.-bucketSize.doubleValue());
      for(Pair p : coexpressionList) {

        // The pairs are sorted by value in descending order. We need
        // to find the appropriate value of range.
        // as soon as we see p.getValue < lowRange, we need to store the
        // old (if it is not empty and begin a new one.
        while (p.getValue() < thisLowRange) {
          if (!thisJSON.toString().isEmpty()) {
            try {
              CoexpressionJSON cJ = getDirectDataLoader().createObject(CoexpressionJSON.class);
              cJ.proxyGene(geneProxyMap.get(geneId));
              cJ.setLowRange(new Float(thisLowRange.floatValue()));
              cJ.setHighRange(new Float(thisLowRange.doubleValue() + bucketSize.doubleValue()));
              cJ.setjSON("{" + thisJSON.toString() +"}");
              getDirectDataLoader().store(cJ);
            } catch (ObjectStoreException e) {
              throw new BuildException("Problem storing Coexpression: " + e.getMessage());
            }
          }
          thisJSON = new StringBuffer();
          thisLowRange -= bucketSize;
        }

        // now append the gene name to the JSON
        if (!thisJSON.toString().isEmpty() ) thisJSON.append(",");
        thisJSON.append("\""+p.getKey()+"\":\""+p.getValue()+"\"");
      }
      // done. Entry the final one
      if (thisJSON != null && !thisJSON.toString().isEmpty()) {
        try {
          CoexpressionJSON cJ = getDirectDataLoader().createObject(CoexpressionJSON.class);
          cJ.proxyGene(geneProxyMap.get(geneId));
          cJ.setLowRange(thisLowRange.floatValue());
          cJ.setHighRange(new Float(thisLowRange.doubleValue() + bucketSize.doubleValue()));
          cJ.setjSON("{" + thisJSON.toString() +"}");
          getDirectDataLoader().store(cJ);
        } catch (ObjectStoreException e) {
          throw new BuildException("Problem storing Coexpression: " + e.getMessage());
        }
      }

    }
    LOG.info("Processed " + lineNumber + " lines.");
  }
 
  private void prefillGeneProxy() {
    geneProxyMap = new HashMap<String, ProxyReference>();
    Query q = new Query();
    QueryClass qC = new QueryClass(Gene.class);
    q.addFrom(qC);
    QueryClass qO = new QueryClass(Organism.class);
    q.addFrom(qO);
    QueryField qP = new QueryField(qO,"proteomeId");
    QueryValue qV = new QueryValue(proteomeId);
    QueryObjectReference qRef = new QueryObjectReference(qC,"organism");

    ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

    cs.addConstraint(new ContainsConstraint(qRef, ConstraintOp.CONTAINS, qO));
    cs.addConstraint(new SimpleConstraint(qP,ConstraintOp.EQUALS,qV));
    q.setConstraint(cs);

    QueryField qFName = new QueryField(qC,"secondaryIdentifier");
    QueryField qFId = new QueryField(qC,"id");
    q.addToSelect(qFName);
    q.addToSelect(qFId);

    LOG.info("Prefilling ProxyReferences. Query is "+q);
    try {
      Results res = getIntegrationWriter().getObjectStore().execute(q,100000,false,false,false);
      Iterator<Object> resIter = res.iterator();
      LOG.info("Iterating...");
      while (resIter.hasNext()) {
        @SuppressWarnings("unchecked")
        ResultsRow<Object> rr = (ResultsRow<Object>) resIter.next();
        String name = (String)rr.get(0);
        Integer id = (Integer)rr.get(1);
        geneProxyMap.put(name,new ProxyReference(getIntegrationWriter().getObjectStore(),id,Gene.class));
        ((IntegrationWriterDataTrackingImpl)getIntegrationWriter()).markAsStored(id);
      }
    } catch (Exception e) {
      throw new BuildException("Problem in prefilling ProxyReferences: " + e.getMessage());
    }
    LOG.info("Retrieved "+geneProxyMap.size()+" ProxyReferences.");

  }
  
  private void getOrganism() {
    if (proteomeId == null ) {
      throw new BuildException("ProteomeId must be set.");
    }
    Query q = new Query();
    QueryClass qO = new QueryClass(Organism.class);
    q.addFrom(qO);
    q.addToSelect(qO);
    QueryField qP = new QueryField(qO,"proteomeId");
    QueryValue qV = new QueryValue(proteomeId);
    ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

    cs.addConstraint(new SimpleConstraint(qP,ConstraintOp.EQUALS,qV));
    q.setConstraint(cs);
    try {
      Results res = getIntegrationWriter().getObjectStore().execute(q,10,false,false,false);
      Iterator<Object> resIter = res.iterator();
      LOG.info("Iterating...");
      while (resIter.hasNext()) {
        @SuppressWarnings("unchecked")
        ResultsRow<Object> rr = (ResultsRow<Object>) resIter.next();
        if (organism != null) {
          throw new BuildException("Multiple organisms retrieved.");
        }
        Organism o = (Organism)rr.get(0);
        organism = new ProxyReference(getIntegrationWriter().getObjectStore(),o.getId(),Organism.class);
        ((IntegrationWriterDataTrackingImpl)getIntegrationWriter()).markAsStored(o.getId());
      }
    } catch (Exception e) {
      throw new BuildException("Problem in retrieving organism: " + e.getMessage());
    }
    
    if (organism==null) {
      throw new BuildException("Cannot find organism for proteome id "+proteomeId);
    }

  }

  @SuppressWarnings("rawtypes")
  public class Pair implements Comparable {
    private String left;
    private Double right;
    public Pair(String left,Double right) {
      this.left = left;
      this.right = right;
    }
    public String getKey() { return left; }
    public Double getValue() { return right; }
    public int compareTo(Object b) {
      return -right.compareTo(((Pair)b).getValue());
    }
  }
}
