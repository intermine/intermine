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
public class MethodNamesConverter extends BioFileConverter
{
    //
  private static final String DATASET_TITLE = "Phytozome Method Names";
  private static final String DATA_SOURCE_NAME = "Phytozome v10";
  private static final Logger LOG =
      Logger.getLogger(MethodNamesConverter.class);
  protected String srcDataFile = null;
    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public MethodNamesConverter(ItemWriter writer, Model model) {
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
        String clusterId = fields[0];
        String methodName = fields[1];
        if (StringUtils.isEmpty(clusterId) || StringUtils.isEmpty(methodName)) {
          break;
        }
        Item i = createItem("ProteinFamily");
        i.setAttribute("clusterId", clusterId);
        i.setAttribute("methodName", methodName);
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
