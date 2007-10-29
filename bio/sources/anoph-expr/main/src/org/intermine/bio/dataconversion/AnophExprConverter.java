package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.intermine.dataconversion.DataConverter;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * DataConverter to parse Anopheles Expression data file into Items.
 *
 * @author Julie Sullivan
 */
public class AnophExprConverter extends FileConverter
{
    private static final Logger LOG = Logger.getLogger(AnophExprConverter.class);
    private Map<String, String> reporterToGene = new HashMap<String, String>();
    private Map<String, Item> genes = new HashMap<String, Item>();
    private static final String TYPE = "Geometric mean of ratios";
    Item org;
    private Item dataSet;
    private Item pub;
    private Item experiment;
    private ReferenceList experiments;
    protected File geneFile;
    
    /**
     * Construct a new instance of Converter.
     *
     * @param model the Model
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     */
    public AnophExprConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model);

        org = createItem("Organism");
        org.addAttribute(new Attribute("taxonId", "180454"));
        store(org);

        dataSet = createItem("DataSet");
        dataSet.addAttribute(new Attribute("title", "Anoph-Expr data set"));
        store(dataSet);

        pub = createItem("Publication");
        pub.addAttribute(new Attribute("pubMedId", "17563388"));
        store(pub);
        
        String experimentName = "Koutos AC:  Life cycle transcriptions of malaria mosquito " 
            + "Anopheles gambiae and comparison with the fruitfly Drosophila melanogaster";
        
        experiment = createItem("MicroArrayExperiment");
        experiment.setAttribute("name", experimentName);
        experiment.setReference("publication", pub);
        store(experiment);
        experiments = new ReferenceList("experiments", new ArrayList<String>());
        experiments.addRefId(experiment.getIdentifier());
        
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
    protected void readGenes(Reader reader) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        // don't load header
        String line = br.readLine();
        while ((line = br.readLine()) != null) {
            String fields[] = StringUtils.split(line, '\t');
            String reporter = fields[0];
            String gene = fields[2];
            if (gene != null && !gene.equals("") && !gene.equals("0")) {               
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
    public void setGenefile(File geneFile) {
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
     * Process the results matrix from Fly-FISH.
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
                if (!headerArray[lineIndex].equals("")) {
                    if (lineIndex == 0) {
                        stageNames[colIndex] = new StageName();              
                        stageNames[colIndex].age = headerArray[colIndex];
                    } else if (lineIndex == 1) { 
                        stageNames[colIndex].stage = headerArray[colIndex];   
                    } else {
                        stageNames[colIndex].sex = headerArray[colIndex];   
                    }
                }
            }
        }
        
        HashMap<String, Item> results = new HashMap<String, Item>();
        
        while ((line = br.readLine()) != null) {
            String lineBits[] = StringUtils.split(line, '\t');
            String probe = lineBits[0];
            if (reporterToGene.get(probe) != null) {  
                String geneIdentifier = reporterToGene.get(probe);
                if (reporterToGene.get(probe).contains("(")) {
                    geneIdentifier = geneIdentifier.substring(0, geneIdentifier.indexOf("("));
                }
                Item gene = getGene(geneIdentifier);
                for (int i = 1; i < lineBits.length; i++) {      

                    if (stageNames[i] != null && StringUtil.allDigits(lineBits[i])) {
                        String stageName = stageNames[i].getStageName();
                        if (results.get(stageName) == null) {
                            
                            Item result = createItem("AGambiaeLifeCycle");
                            results.put(stageNames[i].getStageName(), result);
                            result.setAttribute("type", TYPE);
                            result.setAttribute("value", lineBits[i]);
                            result.setAttribute("isControl", "false");
                            ReferenceList experimentGenes = new ReferenceList("genes", 
                                                                    new ArrayList<String>());
                            experimentGenes.addRefId(gene.getIdentifier());
                            result.addCollection(experimentGenes);
                           
                        // process stderr
                        } else {
                            
                            Item result = results.get(stageName);
                            if (result != null) {
                                result.setAttribute("standardError", lineBits[i]);
                                store(result);
                                
                                Item material = createItem("BioEntity");
                                material.setAttribute("name", probe);
                                material.setAttribute("identifier", probe);
                                
                                Item reporter = createItem("Reporter");
                                reporter.setAttribute("isControl", "false");
                                reporter.setReference("material", material);
                                
                                ReferenceList reporters = new ReferenceList("reporters", 
                                                                          new ArrayList<String>());
//                                reporters.addRefId(reporter.getIdentifier());
//                                result.addCollection(reporters);
                                
                                Item assay = createItem("MicroArrayAssay");
                                assay.setAttribute("sample1", "Sample: Reference"); 
                                assay.setAttribute("sample2", "stage: " + stageName);
                                assay.setAttribute("name", "stage: " + stageName);
                                assay.setReference("experiment", experiment);
                                
                                ReferenceList r = new ReferenceList("results", 
                                                                         new ArrayList<String>());
                                r.addRefId(result.getIdentifier());
                                assay.addCollection(r);
                                store(assay);
                            } else {
                                LOG.error("Couldn't store standard error for the following "
                                          + "stage: " + stageName + " and gene: " + geneIdentifier);
                            }
                        }                       
                    }
                }
            }

        }
    }


    private Item getGene(String geneCG) throws ObjectStoreException {
        if (genes.containsKey(geneCG)) {
            return genes.get(geneCG);
        } else {
            Item gene = createItem("Gene");
            gene.setAttribute("identifier", geneCG);
            gene.setReference("organism", org);
            genes.put(geneCG, gene);
            store(gene);
            return gene;
        }
    }
}

