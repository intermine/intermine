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

import java.io.File;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 * 
 * @author
 */
public class GeneDeflineConverter extends BioFileConverter
{
  //
  private static final String DATASET_TITLE = "Phytozome Gene Deflines";
  private static final String DATA_SOURCE_NAME = "Phytozome v10";
  private static final Logger LOG =
      Logger.getLogger(GeneDeflineConverter.class);
  private HashMap<String,String> organismMap = new HashMap<String,String>();
  protected String srcDataFile = null;

  /**
   * Constructor
   * @param writer the ItemWriter used to handle the resultant items
   * @param model the Model
   */
  public GeneDeflineConverter(ItemWriter writer, Model model) {
    super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
  }

  /**
   * 
   *
   * {@inheritDoc}
   */
  public void process(Reader reader) throws Exception {
    File theFile = getCurrentFile();
    // if we're only going to parse 1 file. It should match srcDataFile
    if( (srcDataFile != null) && (!theFile.getName().equals(srcDataFile)) ) {
      LOG.info("Ignoring file " + theFile.getName() + ". File name is set and this is not it.");
    } else {
      Iterator<?> tsvIter;                             
      try {                                            
        tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
      } catch (Exception e) {                                           
        throw new BuildException("cannot parse file: " + getCurrentFile(), e);
      }

      int lineNumber = 0;                                        

      while (tsvIter.hasNext()) {
        String[] fields = (String[]) tsvIter.next();
        String geneId = fields[0];
        String organismId = fields[1];
        String defLine = null;
        String description = null;
        if (fields.length >= 3) {
          defLine = fields[2];
          if( fields.length >= 4) {
            description = fields[3];
          }
        }
        if (!organismMap.containsKey(organismId)) {
          Item o = createItem("Organism");
          o.setAttribute("taxonId", organismId);
          try {
            store(o);
          } catch (ObjectStoreException e) {
            throw new BuildException("Trouble storing organism: "+e.getMessage());
          }
          organismMap.put(organismId, o.getIdentifier());
        }
        if (StringUtils.isEmpty(geneId)) {
          break;
        }
        Item i = createItem("Gene");
        i.setAttribute("primaryIdentifier", geneId);
        i.setReference("organism", organismMap.get(organismId));
        if(defLine != null && !defLine.isEmpty()) {
          i.setAttribute("briefDescription",defLine);
        }
        if(description != null &&  !description.isEmpty()){
          i.setAttribute("description",description);
        }
        try {
          store(i);
        } catch (ObjectStoreException e) {
          throw new BuildException("Trouble storing gene defline: "+e.getMessage());
        }
        lineNumber++;
      }
      LOG.info("Processed "+lineNumber+" lines");
    }
  }
  public void setSrcDataFile(String file) {
    srcDataFile = file;
  }
}
