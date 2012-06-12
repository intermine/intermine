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
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.bio.dataconversion.BioFileConverter;
import org.intermine.bio.dataconversion.FlyBaseIdResolverFactory;
import org.intermine.bio.dataconversion.IdResolver;
import org.intermine.bio.dataconversion.IdResolverFactory;
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
    protected IdResolverFactory resolverFactory;
    protected IdResolver resolver;
    private File flybaseExpressionLevelsFile;
    private Item organism;

    private static final String TAXON_ID = "7227";
    private Map<String, String> genes = new HashMap<String, String>();
    private Map<String, String> terms = new HashMap<String, String>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public FlybaseExpressionConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
        resolverFactory = new FlyBaseIdResolverFactory("gene");

        organism = createItem("Organism");
        organism.setAttribute("taxonId", TAXON_ID);
        try {
            store(organism);
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the expression levels input file. This file contains the details for each expression
     * level from modMine and FlyBase, eg. ME_01   No expression
     *
     * The "ME_O1" is used in the scores file to represent the expression level.
     * @param flybaseExpressionLevelsFile data file
     *
     * @param expressionLevelsFile screen input file
     */
    public void setFlybaseExpressionLevelsFile(File flybaseExpressionLevelsFile) {
        this.flybaseExpressionLevelsFile = flybaseExpressionLevelsFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws Exception {
        try {
            processTermFile(new FileReader(flybaseExpressionLevelsFile));
        } catch (IOException err) {
            throw new RuntimeException("error reading expressionLevelsFile", err);
        }
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
                System.out.println("test " + line.length);
                continue;
            }

            String fbgn = line[0];	// FBgn0000003
            String stage = line[3];	// embryo_02-04hr

            Item result = createItem("RNASeqResult");
            result.setAttribute("stage", stage);

            if (line.length > 4) {
                String rpkm = line[4];	// 6825 - OPTIONAL
                if (StringUtils.isNotEmpty(rpkm)) {
                    try {
                        Integer.valueOf(rpkm);
                        result.setAttribute("expressionScore", rpkm);
                    } catch (NumberFormatException e) {
                        LOG.warn("bad score: " + rpkm, e);
                    }
                }
            }
            if (line.length > 5) {
                String levelIdentifier = line[5];	// ME_07 - OPTIONAL
                String levelName = terms.get(levelIdentifier);
                if (StringUtils.isNotEmpty(levelName)) {
                    result.setAttribute("expressionLevel", levelName);
                }
            }

            String gene = getGene(fbgn);
            if (StringUtils.isNotEmpty(gene)) {
                result.setReference("gene", gene);
                store(result);
            }
        }
    }


    private void processTermFile(Reader reader) {
        Iterator<?> tsvIter;
        try {
            tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
        } catch (Exception e) {
            throw new BuildException("cannot parse file: " + getCurrentFile(), e);
        }

        while (tsvIter.hasNext()) {
            String[] line = (String[]) tsvIter.next();

            if (line.length != 6) {
                LOG.error("Couldn't process line.  Expected 8 cols, but was " + line.length);
                continue;
            }

            String identifier = line[1];	// 09
            String name = line[3];	// No expression

            terms.put(identifier, name);
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
        resolver = resolverFactory.getIdResolver();
        boolean currentGene = resolver.isPrimaryIdentifier(TAXON_ID, fbgn);
        if (currentGene) {
            return fbgn;
        }
        return null;
    }
}
