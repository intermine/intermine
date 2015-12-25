package org.intermine.bio.dataconversion;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.dataloader.IntegrationWriterDataTrackingImpl;
import org.intermine.model.bio.Homolog;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.Gene;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.task.FileDirectDataLoaderTask;
import org.intermine.util.FormattedTextParser;

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
public class InparanoidHomologsDirectDataLoaderTask extends FileDirectDataLoaderTask {
  //
  private static final String DATASET_TITLE = "Phytozome Homologs";
  private static final String DATA_SOURCE_NAME = "Phytozome v11";
  private static final Logger LOG =
      Logger.getLogger(InparanoidHomologsDirectDataLoaderTask.class);

  private HashMap<String,ProxyReference> geneMap = null;
  Pattern filePattern;
  int orthoRegistered;
  int paraRegistered;
  HashMap<Integer,ProxyReference> orgProxys = new HashMap<Integer,ProxyReference>();

  /**
   * Called by parent process method for each file found
   *
   * {@inheritDoc}
   */
  public void processFile(File theFile) {
    if (geneMap == null) {
      prefillGeneMap();
    }
    filePattern = Pattern.compile("table\\.(\\d+)\\.fa-(\\d+)\\.fa");
    orthoRegistered = 0;
    paraRegistered = 0;
    String message = "Processing file: " + theFile.getName();
    Matcher fileMatcher = filePattern.matcher(theFile.getName());
    if( !fileMatcher.matches()) {
      LOG.info("Ignoring file " + theFile.getName() + ". Does not match pattern.");
    } else {
      LOG.info("Processing file "+theFile.getName());
      Integer[] proteomeId = new Integer[2];
      try {
        proteomeId[0] = Integer.parseInt(fileMatcher.group(1));
        proteomeId[1] = Integer.parseInt(fileMatcher.group(2));
      } catch (NumberFormatException e) {
        throw new BuildException("Cannot parse proteome ids from string "+theFile.getName());
      }

      // be sure the organism proxy references are loaded
      for( Integer prot : proteomeId ) {
        if (!orgProxys.containsKey(prot)) {
          Organism org;
          try {
            org = getDirectDataLoader().createObject(Organism.class);

            org.setProteomeId(prot);
            getDirectDataLoader().store(org);
            orgProxys.put(prot,new ProxyReference(getIntegrationWriter().getObjectStore(),
                org.getId(), Organism.class));      
          } catch (ObjectStoreException e) {
            throw new BuildException("Problem storing organism for proteome "+prot);
          }
        }
      }

      Iterator<?> tsvIter;
      try {
        FileReader reader = new FileReader(theFile);
        tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
      } catch (Exception e) {
        throw new BuildException("cannot parse file: " + theFile.getName(), e);
      }

      int lineNumber = 0;

      while (tsvIter.hasNext()) {
        String[] fields = (String[]) tsvIter.next();
        String id = fields[0];
        String genes1 = fields[2];
        String genes2 = fields[3];

        // sometimes things are duplicated!
        HashSet<String> uniquedGenes1 = new HashSet<String>();
        HashSet<String> uniquedGenes2 = new HashSet<String>();
        String[] fields1 = genes1.split(" ");
        String[] fields2 = genes2.split(" ");
        for( int i1=0; i1 < fields1.length; i1+=2) {
          uniquedGenes1.add(fields1[i1]);
        }
        for( int i2=0; i2 < fields2.length; i2+=2) {
          uniquedGenes2.add(fields2[i2]);
        }
        String class1 = (uniquedGenes1.size()>1)?"many":"one";
        String class2 = (uniquedGenes2.size()>1)?"many":"one";
        String groupName = proteomeId[0].toString()+"_"+proteomeId[1].toString()+"_"+id;

        orthoRegistered += registerPairs(groupName,uniquedGenes1,proteomeId[0],uniquedGenes2,proteomeId[1],class1+"-to-"+class2);
        orthoRegistered += registerPairs(groupName,uniquedGenes2,proteomeId[1],uniquedGenes1,proteomeId[0],class2+"-to-"+class1);
        paraRegistered += registerPairs(groupName,uniquedGenes1,proteomeId[0],uniquedGenes1,proteomeId[0],class1+"-to-"+class2);
        paraRegistered += registerPairs(groupName,uniquedGenes2,proteomeId[1],uniquedGenes2,proteomeId[1],class2+"-to-"+class1);
        lineNumber++;

        if ( (lineNumber%5000)==0 ) {
          LOG.info("Processed "+lineNumber+" lines and registered "+orthoRegistered+" orthologs and "+paraRegistered+" paralogs...");
        }
      }

      LOG.info("Processed " + lineNumber + " lines.");
      LOG.info("Registered "+orthoRegistered+" orthologs and "+paraRegistered+" paralogs.");
    }

  }
  private int registerPairs(String groupName,HashSet<String> g1,Integer p1,HashSet<String> g2,Integer p2,String relationship) {
    return registerPairs(groupName,g1,p1.toString(),g2,p2.toString(),relationship);
  }
  private int registerPairs(String groupName,HashSet<String> g1,String p1,HashSet<String> g2,String p2,String relationship) {

    int registered = 0;

    for( String gene1 : g1 ) {
      for(String gene2 : g2 ) {
        // register all pairs with genes1 and genes2
        // everything should be an integer. Skip this record if not
        try {
          Integer.parseInt(gene1);
          Integer.parseInt(gene2);

          if (p1.equals(p2) && gene1.equals(gene2)) {
            continue;
          } else {
            try {
              Homolog o = getDirectDataLoader().createSimpleObject(Homolog.class);
              String pacGene1 = "PAC:"+gene1;
              String pacGene2 = "PAC:"+gene2;
              o.proxyGene1(getGene(pacGene1,p1));
              o.proxyGene2(getGene(pacGene2,p2));
              o.proxyOrganism1(orgProxys.get(Integer.parseInt(p1)));
              o.proxyOrganism2(orgProxys.get(Integer.parseInt(p2)));
              o.setGroupName(groupName);
              o.setMethod("inParanoid");
              o.setRelationship(relationship);
              getDirectDataLoader().store(o);
            } catch (ObjectStoreException e) {
              throw new BuildException("There was a problem storing homolog: "+e.getMessage());
            }
            registered++;
          }
        } catch (NumberFormatException e) {}
      }
    }
    return registered;
  }

  private ProxyReference getGene(String geneName, String proteomeId) {
    if (!geneMap.containsKey(geneName)) {
      try {
        Gene g = getDirectDataLoader().createObject(Gene.class);
        g.setSecondaryIdentifier(geneName);
        getDirectDataLoader().store(g);
        ProxyReference ref = new ProxyReference(getIntegrationWriter().getObjectStore(),g.getId(),Gene.class);
        geneMap.put(geneName,ref); 
      } catch (ObjectStoreException e) {
        throw new BuildException("There was a problem storing gene: "+e.getMessage());
      }
    }
    return geneMap.get(geneName);
  }

  private void prefillGeneMap() {
    geneMap = new HashMap<String,ProxyReference>();
    Query q = new Query();
    QueryClass qC = new QueryClass(Gene.class);
    q.addFrom(qC);
    QueryField qFName = new QueryField(qC,"secondaryIdentifier");
    QueryField qFId = new QueryField(qC,"id");
    q.addToSelect(qFName);
    q.addToSelect(qFId);

    LOG.info("Prefilling Gene ProxyReferences. Query is "+q);
    try {
      Results res = getIntegrationWriter().getObjectStore().execute(q,100000,false,false,false);
      Iterator<Object> resIter = res.iterator();
      LOG.info("Iterating...");
      while (resIter.hasNext()) {
        @SuppressWarnings("unchecked")
        ResultsRow<Object> rr = (ResultsRow<Object>) resIter.next();
        String name = (String)rr.get(0);
        Integer id = (Integer)rr.get(1);
        geneMap.put(name,new ProxyReference(getIntegrationWriter().getObjectStore(),id,Gene.class));
        ((IntegrationWriterDataTrackingImpl)getIntegrationWriter()).markAsStored(id);
      }
    } catch (Exception e) {
      throw new BuildException("Problem in prefilling ProxyReferences: " + e.getMessage());
    }
    LOG.info("Retrieved "+geneMap.size()+" ProxyReferences.");

  }
}

