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
 * TsvSynonymsConverter. A quickie to read gene synonyms from a TSV
 * file and add to a loaded annotation
 * 
 * @author JCarlson
 */
public class TsvSynonymsConverter extends BioFileConverter
{
    // //
  private static final String DATASET_TITLE = "Phytozome Gene Synonyms";
  private static final String DATA_SOURCE_NAME = "Phytozome v10";
  private static final Logger LOG =
      Logger.getLogger(TsvSynonymsConverter.class);
  private HashMap<String,String> organismMap = new HashMap<String,String>();
  private HashMap<String,String> geneMap = new HashMap<String,String>();
  protected String srcDataFile = null;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public TsvSynonymsConverter(ItemWriter writer, Model model) {
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
        
        // format: 
        // <proteome id> (ignored)
        // <taxon id>
        // <gene name>
        // <which> (symbol or synonym>
        // <synonym> or <symbol>
        // only 1 symbol is allowed. And it must be first in the list.
        

        while (tsvIter.hasNext()) {
          String[] fields = (String[]) tsvIter.next();
          String proteomeId = fields[0];
          String geneName = fields[2];
          String which = fields[3];
          String synonym = fields[4];
          try {
            //Integer taxon = Integer.parseInt(proteomeId);
            if (!organismMap.containsKey(proteomeId)) {
              Item o = createItem("Organism");
              o.setAttribute("proteomeId", proteomeId);
              try {
                store(o);
              } catch (ObjectStoreException e) {
                throw new BuildException("Trouble storing organism: "+e.getMessage());
              }
              organismMap.put(proteomeId, o.getIdentifier());
            }
            if (StringUtils.isEmpty(geneName)) {
              break;
            }
            if (!geneMap.containsKey(geneName)) {     
              Item i = createItem("Gene");
              i.setAttribute("primaryIdentifier", geneName);
              i.setReference("organism", organismMap.get(proteomeId));
              if (which.equals("symbol") ) {
                i.setAttribute("symbol",synonym);
              }
              try {
                store(i);
              } catch (ObjectStoreException e) {
                throw new BuildException("Trouble storing gene: "+e.getMessage());
              }
              geneMap.put(geneName,i.getIdentifier());
            }
            if (which.equals("synonym")) {
              Item s = createItem("Synonym");
              s.setAttribute("value",synonym);
              s.setReference("subject",geneMap.get(geneName));
              try {
                store(s);
              } catch (ObjectStoreException e) {
                throw new BuildException("Trouble storing synonym: "+e.getMessage());
              }
            }
            
            lineNumber++;
          } catch (NumberFormatException e) {}
        }

        LOG.info("Processed "+lineNumber+" lines");
      }
    }
    public void setSrcDataFile(String file) {
      srcDataFile = file;
    }
  }
