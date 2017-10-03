package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 * Converter to parse modENCODE expression data.
 *
 * @author Julie Sullivan
 */
public class FlybaseExpressionConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(FlybaseExpressionConverter.class);
    private static final String DATASET_TITLE = "FlyBase expression data";
    private static final String DATA_SOURCE_NAME = "modENCODE";
    private File stagesFile;
    private Item organism;
    private static final String PREFIX = "me_mRNA_";
    private static final String TAXON_FLY = "7227";
    private Map<String, String> genes = new HashMap<String, String>();
    private Map<String, Stage> stages = new HashMap<String, Stage>();
    protected IdResolver rslv;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public FlybaseExpressionConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);

        organism = createItem("Organism");
        organism.setAttribute("taxonId", TAXON_FLY);
        try {
            store(organism);
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set stages file. This file contains the stage value plus Rachel's human readable
     * translation.  Each stage has a category too.
     *
     * @param stagesFile screen input file
     */
    public void setFlybaseStagesFile(File stagesFile) {
        this.stagesFile = stagesFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws Exception {
        if (rslv == null) {
            rslv = IdResolverService.getFlyIdResolver();
        }
        // we are assigning the label right on the score now.
//        processTermFile(new FileReader(flybaseExpressionLevelsFile));
        processStages(new FileReader(stagesFile));
        processScoreFile(reader);
    }

    private void processScoreFile(Reader reader) throws ObjectStoreException {
        Iterator<?> tsvIter;
        try {
            tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
        } catch (Exception e) {
            throw new BuildException("cannot parse file: " + getCurrentFile(), e);
        }
        while (tsvIter.hasNext()) {
            String[] line = (String[]) tsvIter.next();

            if (line.length < 5) {
                LOG.error("Couldn't process line.  Expected 8 cols, but was " + line.length);
                continue;
            }

            // FBgn0000003
            final String fbgn = line[1];
            // modENCODE_mRNA-Seq_U
            final String source = line[4];
            // embryo_02-04hr
            final String stage = line[6];

            if (!source.startsWith("modENCODE")) {
                continue;
            }
            Item result = createItem("RNASeqResult");
            result.setAttribute("stage", replaceStage(stage));

            if (line.length > 7) {
                // 6825 - OPTIONAL
                String rpkm = line[7];

                if (StringUtils.isNotEmpty(rpkm)) {
                    Integer expressionScore = new Integer(0);
                    try {
                        expressionScore = Integer.valueOf(rpkm);
                        result.setAttribute("expressionScore", rpkm);
                        result.setAttribute("expressionLevel", getLabel(expressionScore));
                    } catch (NumberFormatException e) {
                        LOG.warn("bad score: " + rpkm, e);
                    }
                }
            }
            String gene = getGene(fbgn);
            if (StringUtils.isNotEmpty(gene)) {
                result.setReference("gene", gene);
                store(result);
            }
        }
    }

  //> No/Extremely low expression (0 - 0)
  //> Very low expression (1 - 3)
  //> Low expression (4 - 10)
  //> Moderate expression (11 - 25)
  //> Moderately high expression (26 - 50)
  //> High expression (51 - 100)
  //> Very high expression (101 - 1000)
  //> Extremely high expression (>1000)

    private static String getLabel(Integer score) {
        String label = "No / Extremely low expression";
        if (score > 1000) {
            label = "Extremely high expression";
        } else if (score > 100) {
            label = "Very high expression";
        } else if (score > 50) {
            label = "High expression";
        } else if (score > 25) {
            label = "Moderately high expression";
        } else if (score > 10) {
            label = "Moderate expression";
        } else if (score > 3) {
            label = "Low expression";
        } else if (score > 0) {
            label = "Very low expression";
        }
        return label;
    }

    private String replaceStage(String identifier) {
        Stage stage = stages.get(identifier);
        String name = null;
        if (stage != null) {
            name = stage.name;
        } else {
            // if we have no human readable equivalent, just chop off the prefix
            name = identifier.substring(PREFIX.length(), identifier.length());
        }
        return name;
    }

    private void processStages(Reader reader) {
        Iterator<?> tsvIter;
        try {
            tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
        } catch (Exception e) {
            throw new BuildException("cannot parse file: " + getCurrentFile(), e);
        }

        while (tsvIter.hasNext()) {
            String[] line = (String[]) tsvIter.next();

            if (line.length != 3) {
                LOG.error("Couldn't process line.  Expected 3 cols, but was " + line.length);
                continue;
            }

            String category = line[0].trim();
            String identifier = line[1].trim();
            String name = line[2].trim();

            Stage stage = new Stage(name, category);
            stages.put(identifier, stage);
        }
    }

    private String getGene(String fbgn) throws ObjectStoreException {
        String identifier = resolveGene(fbgn);
        if (StringUtils.isEmpty(identifier)) {
            return null;
        }
        if (genes.containsKey(identifier)) {
            return genes.get(identifier);
        }
        Item gene = createItem("Gene");
        gene.setAttribute("primaryIdentifier", identifier);
        gene.setReference("organism", organism);
        String refId = gene.getIdentifier();
        genes.put(identifier, refId);
        store(gene);
        return refId;
    }

    private String resolveGene(String fbgn) {
        // if resolver not exist, return the original id as primary id
        if (rslv == null || !rslv.hasTaxon(TAXON_FLY)) {
            return fbgn;
        }
        boolean currentGene = rslv.isPrimaryIdentifier(TAXON_FLY, fbgn);
        if (currentGene) {
            return fbgn;
        }
        return null;
    }

    /**
     * Respresents a development stage
     * @author Julie
     */
    protected class Stage
    {
        protected String name;
        protected String category;

        /**
         * @param name identifer
         * @param category category
         */
        public Stage(String name, String category) {
            this.name = name;
            this.category = category;
        }
    }
}
