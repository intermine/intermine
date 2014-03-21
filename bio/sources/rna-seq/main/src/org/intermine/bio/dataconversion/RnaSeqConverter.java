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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.IOException;
import java.util.Iterator;
import java.lang.Class;
import java.lang.reflect.Method;
import java.beans.PropertyDescriptor;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;           
import org.intermine.xml.full.Item;            
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.bio.dataconversion.BioFileConverter;
import org.intermine.bio.util.OrganismData;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;

/**
 * DataConverter to read a directory of files or fpkm's for 1 organism.
 * The header line will have the unique identifier for the experiment name.
 * This will be used for tracking sample and run information.              
 *                                                                         
 * @author J Carlson                                                       
 */                                                                        
public class RnaSeqConverter extends BioFileConverter                      
{                                                                          
  //                                                                     
  private static final String DATASET_TITLE = "RNA-seq Expression Data";              
  private static final String DATA_SOURCE_NAME = "RNA-seq";                           

  private static final Logger LOG =
      Logger.getLogger(RnaSeqConverter.class);


  // experiment records we will refer to
  private Map<String, Item> experimentMap = new HashMap<String,Item>();
  // organism records we will refer to
  private Map<Integer, Item> organismMap = new HashMap<Integer, Item>();
  private Map<Integer,OrganismData> organismsToProcess = new HashMap<Integer,OrganismData>();

  /**
   * Constructor
   * @param writer the ItemWriter used to handle the resultant items
   * @param model the Model                                         
   */                                                               
  public RnaSeqConverter(ItemWriter writer, Model model) throws ObjectStoreException {          
    super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
  }
  
  public void setOrganisms(String organisms) {
    String[] bits = StringUtil.split(organisms, " ");
    //for (int i = 0; i < bits.length; i++) {
    for (String organismIdString: bits) {
        OrganismData od = null;
        Integer taxonId;
        try {
            taxonId = Integer.valueOf(organismIdString);
            od = OrganismRepository.getOrganismRepository().getOrganismDataByTaxon(taxonId);
        } catch (NumberFormatException e) {
            od = OrganismRepository.getOrganismRepository().getOrganismDataByAbbreviation(organismIdString);
            taxonId = 999;
        }
        if (od == null) {
            throw new RuntimeException("Can't find organism for: " + organismIdString);
        }
        if (!organismsToProcess.containsKey(taxonId) ) {
          Item i = createItem("Organism");
          i.setAttribute("taxonId", taxonId.toString());
        }
        organismsToProcess.put(taxonId,od);
    }
}
  /**
   * 
   * 
   * {@inheritDoc}
   */ 
  
  public void process(Reader reader) throws Exception {
    File theFile = getCurrentFile();                 
    LOG.info("Processing file "+theFile.getName()+"...");
    if (theFile.getName().endsWith(".fpkm_tracking")) {
      if (theFile.getName().startsWith("gene")) {
        processFPKM(reader,"Gene");
      } else if (theFile.getName().startsWith("isoform")) {
        processFPKM(reader,"MRNA");
      }
    } else if (theFile.getName().endsWith(".count_tracking")) {
      if (theFile.getName().startsWith("gene")) {
        processCount(reader,"Gene");
      } else if (theFile.getName().startsWith("isoform")) {
        processCount(reader,"MRNA");
      }
    } else {
      LOG.info("Ignoring file "+theFile.getName()+".");
    }
  }

  private void processFPKM(Reader reader, String type)
      throws BuildException, ObjectStoreException {      
    Iterator<?> tsvIter;                             
    try {                                            
      tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
    } catch (Exception e) {                                           
      throw new BuildException("cannot parse file: " + getCurrentFile(), e);
    }                                                                         
    String [] headers = null;                                                 
    int lineNumber = 0;                                               

    while (tsvIter.hasNext()) {
      String[] line = (String[]) tsvIter.next();
      LOG.debug("SCOREg " + line[0]);           

      if (lineNumber == 0) {
        // column headers - strip off any extra columns - FlyAtlas
        // not necessary for FlyExpressionScore, but OK to keep the code
        int end = 0;
        for (int i = 0; i < line.length; i++) {
          if (StringUtils.isEmpty(line[i])) {
            break;
          }
          end++;
        }
        headers = new String[end];
        System.arraycopy(line, 0, headers, 0, end);
      } else {
        String primaryId = line[0];
        if (StringUtils.isEmpty(primaryId)) {
          break;
        }
        //createBioEntity(primaryId, type);

        // Cell line starts from column 6 and ends at 30 which is hearder[5-29]
        for (int i = 5; i < 30; i++) {
          String col = headers[i];
          // if (!cellLines.containsKey(col)) {
          //   Item cellLine = createCellLine(col);
          //  cellLines.put(col, cellLine.getIdentifier());
          //}
          //Item score = createRNASeqScore(line[i]);
          //score.setReference("item", geneItems.get(primaryId));
          //score.setReference("experiment", experiment);
          //score.setReference("organism", organism);
          // store(score);
        }

        lineNumber++;
      }
    }
  }

  private void processCount(Reader reader, String type)
      throws BuildException, ObjectStoreException {      
    Iterator<?> tsvIter;                             
    try {                                            
      tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
    } catch (Exception e) {                                           
      throw new BuildException("cannot parse file: " + getCurrentFile(), e);
    }                                                                         
    String [] headers = null;                                                 
    int lineNumber = 0;                                               

    while (tsvIter.hasNext()) {
      String[] line = (String[]) tsvIter.next();
      LOG.debug("SCOREg " + line[0]);           

      if (lineNumber == 0) {
        // column headers - strip off any extra columns - FlyAtlas
        // not necessary for FlyExpressionScore, but OK to keep the code
        int end = 0;
        for (int i = 0; i < line.length; i++) {
          if (StringUtils.isEmpty(line[i])) {
            break;
          }
          end++;
        }
        headers = new String[end];
        System.arraycopy(line, 0, headers, 0, end);
      } else {
        String primaryId = line[0];
        if (StringUtils.isEmpty(primaryId)) {
          break;
        }
        //createBioEntity(primaryId, type);

        // Cell line starts from column 6 and ends at 30 which is hearder[5-29]
        for (int i = 5; i < 30; i++) {
          String col = headers[i];
          // if (!cellLines.containsKey(col)) {
          //   Item cellLine = createCellLine(col);
          //  cellLines.put(col, cellLine.getIdentifier());
          //}
          //Item score = createRNASeqScore(line[i]);
          //score.setReference("item", geneItems.get(primaryId));
          //score.setReference("experiment", experiment);
          //score.setReference("organism", organism);
          // store(score);
        }

        lineNumber++;
      }
    }
  }

  private Item createExperiment(String name,Integer taxonId) throws ObjectStoreException {
    Item experiment = createItem("Experiment");
    experiment.setAttribute("name",name);
    experiment.setReference("organism",organismMap.get(taxonId));
    experimentMap.put(name,experiment);
    return experiment;
  }

}
