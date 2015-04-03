package org.intermine.bio.dataconversion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Homolog;
import org.intermine.model.bio.Consequence;
import org.intermine.model.bio.ConsequenceType;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.DiversitySample;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.MRNA;
import org.intermine.model.bio.Ontology;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.SNP;
import org.intermine.model.bio.SNPDiversitySample;
import org.intermine.model.bio.SOTerm;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.ProxyReference;
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
  private static final String DATA_SOURCE_NAME = "Phytozome v10";
  private static final Logger LOG =
      Logger.getLogger(InparanoidHomologsDirectDataLoaderTask.class);

  private HashMap<Integer,ProxyReference> organismMap = new HashMap<Integer,ProxyReference>();
  private HashMap<String,ProxyReference> geneMap = new HashMap<String,ProxyReference>();
  Pattern filePattern;
  int orthoRegistered;
  int paraRegistered;

  /**
   * Called by parent process method for each file found
   *
   * {@inheritDoc}
   */
  public void processFile(File theFile) {
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

      // register both as needed.
      for ( Integer proteome: proteomeId ) {
        if (!organismMap.containsKey(proteome)) {
          try {
            Organism org = getDirectDataLoader().createObject(Organism.class);
            org.setProteomeId(proteome);
            // and store the organism.
            getDirectDataLoader().store(org);  
            ProxyReference orgRef = new ProxyReference(getIntegrationWriter().getObjectStore(),
                org.getId(), Organism.class);
            organismMap.put(proteome, orgRef);
          } catch (ObjectStoreException e) {
            throw new BuildException("Problem storing Organism: " + e.getMessage());
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

        int field1Ctr = genes1.split(" ").length;
        int field2Ctr = genes2.split(" ").length;
        String class1 = (field1Ctr>3)?"many":"one";
        String class2 = (field2Ctr>3)?"many":"one";
        String groupName = proteomeId[0].toString()+"_"+proteomeId[1].toString()+"_"+id;

        orthoRegistered += registerPairs(groupName,genes1,proteomeId[0],genes2,proteomeId[1],class1+"-to-"+class2);
        orthoRegistered += registerPairs(groupName,genes2,proteomeId[1],genes1,proteomeId[0],class2+"-to-"+class1);
        paraRegistered += registerPairs(groupName,genes1,proteomeId[0],genes1,proteomeId[0],class1+"-to-"+class2);
        paraRegistered += registerPairs(groupName,genes2,proteomeId[1],genes2,proteomeId[1],class2+"-to-"+class1);
        lineNumber++;

        if ( (lineNumber%5000)==0 ) {
          LOG.info("Processed "+lineNumber+" lines and registered "+orthoRegistered+" orthologs and "+paraRegistered+" paralogs...");
        }
      }

      LOG.info("Processed " + lineNumber + " lines.");
      LOG.info("Registered "+orthoRegistered+" orthologs and "+paraRegistered+" paralogs.");
    }

  }
  private int registerPairs(String groupName,String g1,Integer p1,String g2,Integer p2,String relationship) {
    return registerPairs(groupName,g1,p1.toString(),g2,p2.toString(),relationship);
  }
  private int registerPairs(String groupName,String g1,String p1,String g2,String p2,String relationship) {
    String[] fields1 = g1.split(" ");
    String[] fields2 = g2.split(" ");
    int registered = 0;

    for( int i1=0; i1 < fields1.length; i1+=2) {
      for( int i2=0; i2 < fields2.length; i2+=2 ) {
        // register all pairs with genes1 and genes2
        // everything should be an integer. Skip this record if not
        try {
          Integer.parseInt(fields1[i1]);
          Integer.parseInt(fields2[i2]);

          if (p1.equals(p2) && i1==i2) {
            continue;
          } else {
            try {
              Homolog o = getDirectDataLoader().createSimpleObject(Homolog.class);
              o.proxyOrganism1(organismMap.get(Integer.parseInt(p1)));
              o.proxyOrganism2(organismMap.get(Integer.parseInt(p2)));
              String gene1 = "PAC:"+fields1[i1];
              String gene2 = "PAC:"+fields2[i2];
              o.proxyGene1(getGene(gene1,p1));
              o.proxyGene2(getGene(gene2,p2));
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
        g.proxyOrganism(organismMap.get(Integer.parseInt(proteomeId)));
        getDirectDataLoader().store(g);
        ProxyReference ref = new ProxyReference(getIntegrationWriter().getObjectStore(),g.getId(),Gene.class);
        geneMap.put(geneName,ref); 
      } catch (ObjectStoreException e) {
        throw new BuildException("There was a problem storing gene: "+e.getMessage());
      }
    }
    return geneMap.get(geneName);
  }
}

