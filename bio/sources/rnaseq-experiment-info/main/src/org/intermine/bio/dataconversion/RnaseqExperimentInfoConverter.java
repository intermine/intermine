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
public class RnaseqExperimentInfoConverter extends BioFileConverter
{
  //
  private static final String DATASET_TITLE = "RNAseq Experiment Info";
  private static final String DATA_SOURCE_NAME = "RNAseq Experiment";
  private static final Logger LOG = Logger.getLogger(RnaseqExperimentInfoConverter.class);
  private Integer proteomeId = null;

  /**
    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public RnaseqExperimentInfoConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
      File theFile = getCurrentFile();
      //TODO if we want to process multiple organisms, makes sure we set
      // the organism variable at this point.
      if( !theFile.getName().endsWith(".info") ) {
        LOG.info("Ignoring file " + theFile.getName() + ". Not a vcf sample info file.");
      } else {
        // one organism (at most) per file
        String organismIdentifier = null;
        if (proteomeId != null) {
          Item org = createItem("Organism");
          org.setAttribute("proteomeId",proteomeId.toString());
          try {
            store(org);
          } catch (ObjectStoreException e) {
            throw new BuildException("Cannot store organism: "+e.getMessage());
          }
          organismIdentifier = org.getIdentifier();
        } else {
          throw new BuildException("Proteome Id must be set for rna-seq experiment.");
        }

        Iterator<?> tsvIter;
        try {
          tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
        } catch (Exception e) {
          throw new BuildException("Cannot parse file: " + getCurrentFile(),e);
        }
        int ctr = 0;
        while (tsvIter.hasNext() ) {
          ctr++;
          String[] fields = (String[]) tsvIter.next();
          if (fields.length == 1 && fields[0].startsWith("#") ) {
            // header field. Is there anything we want to do?
          } else {
            if (fields.length == 4) {
              Item sample = createItem("RNAseqExperiment");
              sample.setAttribute("name",fields[0]);
              setIfNotNull(sample,"experimentGroup",fields[1]);
              setIfNotNull(sample,"description",fields[2]);
              setIfNotNull(sample,"url",fields[3]);
              sample.setReference("organism",organismIdentifier);
              try{
                store(sample);
              } catch (ObjectStoreException e) {
                throw new BuildException("Cannot store rnaseq-experiment: "+e.getMessage());
              }
            }

          }
          if ((ctr%100000) == 0) {
            LOG.info("Processed " + ctr + " lines...");
          }
        }
        LOG.info("Processed " + ctr + " lines.");
      }
    }
    void setIfNotNull(Item s,String field,String value) {
      if (value != null && value.trim().length() > 0) {
        s.setAttribute(field,value);
      }
    }
    public void setProteomeId(String organism) {
      try {
        proteomeId = Integer.valueOf(organism);
      } catch (NumberFormatException e) {
        throw new RuntimeException("can't find integer value for: " + organism);
      }
    }
}