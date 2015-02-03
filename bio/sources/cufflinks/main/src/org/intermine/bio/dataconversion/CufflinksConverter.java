package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;           
import org.intermine.xml.full.Item;            
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.bio.dataconversion.BioFileConverter;
import org.intermine.util.FormattedTextParser;


/**
 * 
 * @author
 */
public class CufflinksConverter extends BioFileConverter
{

  //                                                                     
  private static final String DATASET_TITLE = "RNA-seq Expression Data";              
  private static final String DATA_SOURCE_NAME = "RNA-seq";                           

  private static final Logger LOG =
      Logger.getLogger(CufflinksConverter.class);

  // experiment records we will refer to
  private HashMap<String, Item> experimentMap = new HashMap<String,Item>();
  // the 'column group' correspondence with experiment
  private HashMap<Integer,String> experimentColGroupMap = new HashMap<Integer,String>();
  // for now, this can only process files of 1 organism
  private Integer proteomeId = null;
  private Item organism;
  // bioentities we record data about
  private HashMap<String,HashMap<String,Item> > bioentityMap = new HashMap<String, HashMap<String, Item> >();
  // the score data we record data about
  // the keys are bioentity type (gene or isoform), then bioentity identifier, then the experiment
  private HashMap<String,HashMap<String,HashMap<String,Item>>> scoreMap = 
      new HashMap<String, HashMap<String,HashMap<String,Item>>>();

  private final String[] expectedFPKMHeaders = { "tracking_id" , "class_code",
      "nearest_ref_id" , "gene_id" , "gene_short_name",
      "tss_id" , "locus" , "length" , "coverage"};
  private final String[] expectedFPKMSuffices = {"_FPKM", "_conf_lo", "_conf_hi", "_status" };
  private final String[] expectedCountHeaders = { "tracking_id"};
  private final String[] expectedCountSuffices = {"_count","_count_variance","_count_uncertainty_var","_count_dispersion_var","_status"};


  /**
   * Constructor
   * @param writer the ItemWriter used to handle the resultant items
   * @param model the Model
   */
  public CufflinksConverter(ItemWriter writer, Model model) {
    super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    bioentityMap.put("Gene", new HashMap<String,Item>());
    bioentityMap.put("MRNA", new HashMap<String,Item>());
    scoreMap.put("Gene", new HashMap<String,HashMap<String,Item>>());
    scoreMap.put("MRNA", new HashMap<String,HashMap<String,Item>>());
  }

  /**
   * The main even. Read and process a cufflinks file.
   * We read each of the files, then store the results in the close() method.
   * {@inheritDoc}
   */ 

  public void process(Reader reader) throws Exception {
    File theFile = getCurrentFile();                 
    LOG.info("Processing file "+theFile.getName()+"...");

    if (organism==null) {
      // we need to register the organism
      if (proteomeId != null ) {
        organism = createItem("Organism");
        organism.setAttribute("proteomeId", proteomeId.toString());
        try {
          store(organism);
        } catch (ObjectStoreException e) {
          throw new RuntimeException("failed to store organism with proteomeId: "
              + proteomeId, e);
        }
      } else {
        throw new BuildException("No proteomeId specified.");
      }
    }
    
    if (theFile.getName().endsWith("genes.fpkm_tracking")) {
      processCufflinksFile(reader,"FPKM","Gene");
    } else if (theFile.getName().endsWith("isoforms.fpkm_tracking")) {
      processCufflinksFile(reader,"FPKM","MRNA");
    } else if (theFile.getName().endsWith("genes.count_tracking")) {
      processCufflinksFile(reader,"Count","Gene");
    } else if (theFile.getName().endsWith("isoforms.count_tracking")) {
      processCufflinksFile(reader,"Count","MRNA");
    } else {
      LOG.info("Ignoring file "+theFile.getName()+".");
    }
  }

  public void close() throws Exception
  {
    // Now store. First an iterator over the types
    Iterator<Map.Entry<String,HashMap<String, HashMap<String,Item> > > > typeIterator = scoreMap.entrySet().iterator();
    while (typeIterator.hasNext() ) {
      // look at each (name,item) for that type
      Iterator<Map.Entry<String,HashMap<String, Item>>> idMapIterator = typeIterator.next().getValue().entrySet().iterator();
      while (idMapIterator.hasNext() ) {
        Iterator<Map.Entry<String,Item>> scoreMapIterator = idMapIterator.next().getValue().entrySet().iterator();
        while(scoreMapIterator.hasNext() ) {
          try {
            store(scoreMapIterator.next().getValue());
          } catch (Exception e) {
            throw new BuildException("Problem when storing item: " +e);
          }
        }
      }
    } 
  }
  
  private void processCufflinksFile(Reader reader, String fileType, String bioentityType)
      throws BuildException, ObjectStoreException {  

    int colGroupSize = fileType.equals("FPKM")?expectedFPKMSuffices.length:
                                               expectedCountSuffices.length;
    String[] expectedHeaders = fileType.equals("FPKM")?
        (String[])expectedFPKMHeaders.clone():(String[])expectedCountHeaders.clone();
        Iterator<?> tsvIter;                             
        try {                                            
          tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
        } catch (Exception e) {                                           
          throw new BuildException("cannot parse file: " + getCurrentFile(), e);
        }

        int lineNumber = 0;                                        

        while (tsvIter.hasNext()) {
          String[] fields = (String[]) tsvIter.next();

          if (lineNumber == 0) {
            processHeaders(fields,fileType);
          } else {
            String primaryId = fields[0];
            if (StringUtils.isEmpty(primaryId)) {
              break;
            }
            // register the gene/isoform is not see already.
            if(!bioentityMap.get(bioentityType).containsKey(primaryId) ) {
              Item i = createItem(bioentityType);
              i.setAttribute("primaryIdentifier", primaryId);
              i.setReference("organism", organism);
              store(i);
              bioentityMap.get(bioentityType).put(primaryId, i);
            }

            // scan and process all scores
            for (int i = expectedHeaders.length; i < fields.length;) {
              // the 'group' of columns in 1 experiment
              Integer colGroup = new Integer( (i-expectedHeaders.length)/colGroupSize);
              if (!experimentColGroupMap.containsKey(colGroup)){
                throw new BuildException("Unexpected number of columns in " + getCurrentFile() +
                    " at line " + lineNumber);
              }
              String experiment = experimentColGroupMap.get(colGroup);
              if (!scoreMap.get(bioentityType).containsKey(primaryId) ) {
                scoreMap.get(bioentityType).put(primaryId,new HashMap<String,Item>());
              }
              if(!scoreMap.get(bioentityType).get(primaryId).containsKey(experiment)){
                Item score = createItem("CufflinksScore");
                scoreMap.get(bioentityType).get(primaryId).put(experiment,score);
              }
              Item score = scoreMap.get(bioentityType).get(primaryId).get(experiment);
              if( fileType.equals("FPKM") ) {
                try {
                  if (!fields[i  ].trim().isEmpty() ) score.setAttribute("fpkm",fields[i].trim());
                  if (!fields[i+1].trim().isEmpty() ) score.setAttribute("conflo",fields[i+1].trim());
                  if (!fields[i+2].trim().isEmpty() ) score.setAttribute("confhi",fields[i+2].trim());
                  if (!fields[i+3].trim().isEmpty() ) score.setAttribute("status",fields[i+3].trim());
                } catch (ArrayIndexOutOfBoundsException e) {
                  throw new BuildException("Incorrect number of fields (" + i + " to " + (i+2) + ") at line " + lineNumber
                      + " in " + getCurrentFile() );
                }
                score.setReference("bioentity", bioentityMap.get(bioentityType).get(primaryId));
                score.setReference("experiment", experimentMap.get(experimentColGroupMap.get(colGroup)));
              } else {
                try {
                  if (!fields[i  ].trim().isEmpty() ) score.setAttribute("count",fields[i].trim());
                  if (!fields[i+1].trim().isEmpty() ) score.setAttribute("countvariance",fields[i+1].trim());
                  if (!fields[i+2].trim().isEmpty() ) score.setAttribute("countuncertaintyvar",fields[i+2].trim());
                  if (!fields[i+3].trim().isEmpty() ) score.setAttribute("countdispersionvar",fields[i+3].trim());
                } catch (ArrayIndexOutOfBoundsException e) {
                  throw new BuildException("Incorrect number of fields (" + i + " to " + (i+3) + ") at line " + lineNumber
                      + " in " + getCurrentFile() );
                }
                score.setReference("bioentity", bioentityMap.get(bioentityType).get(primaryId));
                score.setReference("experiment", experimentMap.get(experimentColGroupMap.get(colGroup)));
              }
              i+=colGroupSize;
            }
          }
          lineNumber++;
        }
  }


  private void processHeaders(String[] headers,String type) throws BuildException {

    int colGroupSize;
    String[] expectedHeaders;
    String[] expectedSuffices;

    if (type.equals("FPKM") ) {
      colGroupSize = expectedFPKMSuffices.length;
      expectedHeaders = (String[])expectedFPKMHeaders.clone();
      expectedSuffices = (String[])expectedFPKMSuffices.clone();
    } else if (type.equals("Count") ) {
      colGroupSize = expectedCountSuffices.length;
      expectedHeaders = (String[])expectedCountHeaders.clone();
      expectedSuffices = (String[])expectedCountSuffices.clone();
    } else {
      throw new BuildException("Unexpected type of header: " + type);
    }

    String experimentName = null;
    for(int i=0;i<headers.length;i++) {
      if ( i<expectedHeaders.length ) {
        if( !headers[i].equals(expectedHeaders[i]) ) {
          throw new BuildException("Unexpected header "+ headers[i] + " expected " +
              expectedHeaders[i] + " in " + getCurrentFile());
        }
      } else {
        int which = (i - expectedHeaders.length)%colGroupSize;
        int colGroup = (i - expectedHeaders.length)/colGroupSize;
        if ( which == 0) {
          // should be experimentName + suffix. We may need to register experiment
          if (!headers[i].endsWith(expectedSuffices[0]) ) {
            throw new BuildException("Unexpected header " + headers[i] +
                " expected experimentName"+ expectedSuffices[0] +" in "+getCurrentFile());
          }
          experimentName = headers[i].substring(0,headers[i].lastIndexOf(expectedSuffices[0]));
          experimentColGroupMap.put(colGroup,experimentName);
          if (!experimentMap.containsKey(experimentName) ) {
            try {
              experimentMap.put(experimentName, createExperiment(experimentName));
            } catch (ObjectStoreException e) {
              throw new BuildException("Cannot save experiment " + experimentName);
            }
          }
        }
        if (!headers[i].equals(experimentName + expectedSuffices[which])){
          throw new BuildException("Unexpected header " + headers[i] +
              " expected experimentName" + expectedSuffices[which] + " in "+getCurrentFile());
        }
      }
    }
  }
  
  /*
   * Create and store one experiment for the current organism. 
   */

  private Item createExperiment(String name) throws ObjectStoreException {
    Item experiment = createItem("RNAseqExperiment");
    experiment.setAttribute("name",name);
    experiment.setReference("organism",organism);
    experimentMap.put(name,experiment);
    store(experiment);
    return experiment;
  }

  public void setProteomeId(String organism) {
    try {
      proteomeId = Integer.valueOf(organism);
    } catch (NumberFormatException e) {
      throw new RuntimeException("can't find integer proteome id for: " + organism);
    }
  }
}

