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
public class OrganismVersionConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "Organism Version";
    private static final String DATA_SOURCE_NAME = "Phytozome";
    private static final Logger LOG =
        Logger.getLogger(OrganismVersionConverter.class);
    protected String srcDataFile = null;
    private HashMap<String,String> organismMap = new HashMap<String,String>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public OrganismVersionConverter(ItemWriter writer, Model model) {
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

          String pacId = fields[0];
          try {
            Integer.valueOf(pacId);
          } catch (NumberFormatException e) {
            throw new BuildException(pacId+" is not an integer");
          }
          String taxonId = fields[1];
          try {
            Integer.valueOf(taxonId);
          } catch (NumberFormatException e) {
            throw new BuildException(taxonId+" is not an integer");
          }
          String organismVersion = fields[2];
          String assemblyVersion = fields[3];
          String annotationVersion = fields[4];
          String organismShortName = null;
          if (fields.length >= 6 ) {
            organismShortName = fields[5];
          }
       
          if (!organismMap.containsKey(taxonId)) {
            Item o = createItem("Organism");
            o.setAttribute("taxonId", taxonId);
            o.setAttribute("version",organismVersion);
            o.setAttribute("proteomeId", pacId);
            o.setAttribute("assemblyVersion",assemblyVersion);
            o.setAttribute("annotationVersion", annotationVersion);
            if (organismShortName != null && !organismShortName.isEmpty()) {
              o.setAttribute("shortName",organismShortName);
            }
            try {
              store(o);
            } catch (ObjectStoreException e) {
              throw new BuildException("Trouble storing organism: "+e.getMessage());
            }
            organismMap.put(taxonId, o.getIdentifier());
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
