package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2015 FlyMine
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
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;


/**
 * 
 * @author
 */
public class PlantcycPathwayConverter extends BioFileConverter
{
    //
  private static final String DATASET_TITLE = "KEGGt  Pathways";
  private static final String DATA_SOURCE_NAME = "KEGG";
  private static final Logger LOG =
      Logger.getLogger(PlantcycPathwayConverter.class);
  protected String srcDataFile = null;
  protected String proteomeId = null;
  protected HashMap<String,Integer> geneHash = new HashMap<String,Integer>();
  protected HashMap<Integer,ReferenceList> geneRefList = new HashMap<Integer,ReferenceList>();
    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public PlantcycPathwayConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
      
      if (proteomeId == null ) {
        throw new BuildException("proteomeId must be set.");
      }
      
      File theFile = getCurrentFile();
      // if we're only going to parse 1 file. It should match srcDataFile
      if( (srcDataFile != null) && (!theFile.getName().equals(srcDataFile)) ) {
        LOG.info("Ignoring file " + theFile.getName() + ". File name is set and this is not it.");
      } else {
        Iterator<?> tsvIter;                             
        try {                                            
          tsvIter = FormattedTextParser.parseCsvDelimitedReader(reader);
        } catch (Exception e) {                                           
          throw new BuildException("cannot parse file: " + getCurrentFile(), e);
        }

        int lineNumber = 0;  

        // the source
        Item i = createItem("DataSet");
        i.setAttribute("name", "KEGG");
        store(i);
        String datasetReference = i.getIdentifier();
        // and the organism
        Item o = createItem("Organism");
        o.setAttribute("proteomeId",proteomeId);
        store(o);
        String organismIdentifier = o.getIdentifier();
        while (tsvIter.hasNext()) {
          String[] fields = (String[]) tsvIter.next();
          // if field[0] is integer, it's not a header
          try {
            Integer.parseInt(fields[0]);
            String pathwayId = fields[2];
            String pathwayName = fields[1];
            String genes = fields[3];
            // store the pathway
            Item p = createItem("Pathway");
            p.setAttribute("identifier",pathwayId);
            p.setAttribute("name",pathwayName);
            Integer id = store(p);

            Reference r = new Reference("dataSets",datasetReference);
            ReferenceList rL = new ReferenceList("dataSets");
            rL.addRefId(r.getRefId());
            store(rL,id);

            for(String gene : genes.split(",")) {
              if(gene != null && !gene.isEmpty()) {
                LOG.info("Line is "+lineNumber+" with gene "+gene);
                if (!geneHash.containsKey(gene) ) {
                  Item g = createItem("Gene");
                  g.setAttribute("primaryIdentifier",gene);
                  g.setReference("organism",organismIdentifier);
                  Integer gId = store(g);
                  geneHash.put(gene,gId);
                  geneRefList.put(gId,new ReferenceList("pathways"));
                }
                Reference gRef = new Reference("pathways",p.getIdentifier());
                geneRefList.get(geneHash.get(gene)).addRefId(gRef.getRefId());
              }
            }
          } catch (NumberFormatException e) {
            LOG.info("Skipping header line "+fields[0]+" "+fields[1]+" "+fields[2]+"...");
          }
          lineNumber++;
        }
        // now store the collections
        for( Integer gId : geneRefList.keySet()) {
          store(geneRefList.get(gId),gId);
        }
        LOG.info("Processed "+lineNumber+" lines");
      }
    }
    public void setSrcDataFile(String file) {
      srcDataFile = file;
    }
    public void setProteomeId(String proteomeId) {
      this.proteomeId = proteomeId;
    }
}
