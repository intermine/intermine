package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

/**
 * DataConverter to parse Anopheles Expression data file into Items.
 *
 * @author Julie Sullivan
 */
public class AnophExprConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(AnophExprConverter.class);
    private Map<String, String> reporterToGene = new HashMap<String, String>();
    private Map<String, Item> genes = new HashMap<String, Item>();
    private Map<String, Item> assays = new HashMap<String, Item>();
    private static final String TYPE = "Geometric mean of ratios";
    Item org;
    private Item pub;
    private Item experiment;
    protected File geneFile;

    /**
     * Construct a new instance of Converter.
     *
     * @param model the Model
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public AnophExprConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model, "ArrayExpress", "Anoph-Expr data set");

        org = createItem("Organism");
        org.setAttribute("taxonId", "7165");
        store(org);

        pub = createItem("Publication");
        pub.setAttribute("pubMedId", "17563388");
        store(pub);

        String experimentName = "Koutos AC:  Life cycle transcriptions of malaria mosquito "
            + "Anopheles gambiae and comparison with the fruitfly Drosophila melanogaster";

        experiment = createItem("MicroArrayExperiment");
        experiment.setAttribute("name", experimentName);
        experiment.setReference("publication", pub.getIdentifier());
        store(experiment);
    }

    /**
     * Read reporter and genes from file.  The sole purpose of this file is to determine which
     * reporters to load from the data file.
     *
     * Col0 = reporter
     * Col3 = gene
     *
     * @param reader reader
     * @throws IOException if the file cannot be found/read
     */
    public void readGenes(Reader reader) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        // don't load header
        String line = br.readLine();
        while ((line = br.readLine()) != null) {
            String[] fields = StringUtils.split(line, '\t');
            String reporter = fields[0];
            String gene = fields[2];
            if (StringUtils.isNotEmpty(gene) && !("0".equals(gene))) {
                reporterToGene.put(reporter, gene);
            }
        }
    }

    /**
     * Set the reporter to gene input file. This file contains lots of columns, but we are only
     * interested in col0 (reporter) and col3 (gene)
     *
     * @param geneFile reporter to gene input file
     */
    public void setGeneFile(File geneFile) {
        this.geneFile = geneFile;
    }
    private class StageName
    {
        String age;
        String stage;
        String sex;

        protected String getStageName() {
            return age + "_" + stage + "_" + sex;
        }
    }

    /**
     * Process the text file from arrayexpress
     * @param reader the Reader
     * @see DataConverter#process
     * @throws Exception if something goes wrong
     */
    @Override
    public void process(Reader reader) throws Exception {

        if (geneFile == null) {
            throw new NullPointerException("geneFile property not set");
        }

        try {
            readGenes(new FileReader(geneFile));
        } catch (IOException err) {
            throw new RuntimeException("error reading geneFile", err);
        }

        BufferedReader br = new BufferedReader(reader);

        String line = br.readLine();
        String[] headerArray = StringUtils.split(line, '\t');
        StageName[] stageNames = new StageName[headerArray.length];

        /**
         *  process header
         *  header is 4 lines
         *  1. title - ignore
         *  2. age
         *  3. stage
         *  4. sex
         *  concatenate 2+3+4 to get the whole stage name
         */
        for (int lineIndex = 0; lineIndex < 3; lineIndex++) {
            line = br.readLine();
            headerArray = StringUtils.split(line, '\t');
            for (int colIndex = 1; colIndex < headerArray.length; colIndex++) {
                if (StringUtils.isNotEmpty(headerArray[lineIndex])) {
                    if (lineIndex == 0) {
                        stageNames[colIndex] = new StageName();
                        String age =  headerArray[colIndex];
                        stageNames[colIndex].age =  age.replace(" ", "_");
                    } else if (lineIndex == 1) {
                        stageNames[colIndex].stage = headerArray[colIndex];
                    } else {
                        stageNames[colIndex].sex = headerArray[colIndex];
                        if (colIndex % 2 == 0) {
                            String stageName = stageNames[colIndex].getStageName();
                            Item assay = createItem("MicroArrayAssay");
                            assay.setAttribute("sample1", "Sample: Reference");
                            assay.setAttribute("sample2", "stage: " + stageName);
                            assay.setAttribute("name", "stage: " + stageName);
                            assay.setReference("experiment", experiment.getIdentifier());
                            assays.put(stageName, assay);
                            store(assay);
                        }
                    }
                }
            }
        }

        // process probes
        while ((line = br.readLine()) != null) {
            String[] lineBits = StringUtils.split(line, '\t');

            String probe = lineBits[0];
            if (reporterToGene.get(probe) != null) {
                HashMap<String, Item> results = new HashMap<String, Item>();

                String geneIdentifier = reporterToGene.get(probe);
                if (geneIdentifier.contains(" (")) {
                    geneIdentifier = geneIdentifier.substring(0, geneIdentifier.indexOf(" ("));
                }
                geneIdentifier.trim();
                Item gene = getGene(geneIdentifier);

                ReferenceList microArrayResults
                    = new ReferenceList("microArrayResults", new ArrayList<String>());

                Item material = createItem("ProbeSet");
                material.setAttribute("name", probe);
                material.setAttribute("primaryIdentifier", probe);
                material.setReference("organism", org.getIdentifier());
                material.addToCollection("genes", gene.getIdentifier());

                int index = 1;
                for (int i = 0; i < lineBits.length; i++) {
                    if (stageNames[index] != null && StringUtil.allDigits(lineBits[i])) {
                        String stageName = stageNames[index++].getStageName();
                        if (i % 2 != 0) {
                            Item result = createItem("AGambiaeLifeCycle");
                            result.setAttribute("type", TYPE);
                            result.setAttribute("value", lineBits[i]);
                            result.setAttribute("isControl", "false");
                            result.setReference("material", material.getIdentifier());
                            results.put(stageName, result);
                        } else {
                            Item result = results.get(stageName);
                            if (result != null) {
                                result.setAttribute("standardError", lineBits[i]);
                                microArrayResults.addRefId(result.getIdentifier());
                                Item assay = assays.get(stageName);
                                ReferenceList assaysColl = new ReferenceList("assays",
                                                                    new ArrayList<String>());
                                assaysColl.addRefId(assay.getIdentifier());
                                result.addCollection(assaysColl);
                                store(result);
                            } else {
                                LOG.error("Couldn't store data for the following "
                                          + "stage: " +  stageName
                                          + " and gene: " + geneIdentifier
                                          + " and reporter: " + probe);
                            }
                        }
                    }
                }
                gene.addCollection(microArrayResults);
                store(material);
            }
        }
        for (Item item : genes.values()) {
            store(item);
        }
    }

    private Item getGene(String geneCG) {
        if (genes.containsKey(geneCG)) {
            return genes.get(geneCG);
        }
        Item gene = createItem("Gene");
        gene.setAttribute("primaryIdentifier", geneCG);
        gene.setReference("organism", org.getIdentifier());
        genes.put(geneCG, gene);
        return gene;
    }
}

