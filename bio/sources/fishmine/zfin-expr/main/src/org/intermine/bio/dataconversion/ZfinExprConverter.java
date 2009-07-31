package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 *
 * @author
 */
public class ZfinExprConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(ZfinExprConverter.class);
    protected String orgRefId;
    private static final String DATASET_TITLE = "ZFIN Expression data set";
    private static final String DATA_SOURCE_NAME = "ZFIN";
    private Map<String, String> genes = new HashMap();
    private Map<String, String> terms = new HashMap();
    private Set<String> synonyms = new HashSet();
    private Map<String, String> stages = new LinkedHashMap();
    private File stagesFile;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public ZfinExprConverter(ItemWriter writer, Model model)
    throws ObjectStoreException {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);

        // create and store organism
        Item organism = createItem("Organism");
        organism.setAttribute("taxonId", "7955");
        store(organism);
        orgRefId = organism.getIdentifier();
    }

    /**
     * @param stagesFile list of stages
     */
    public void setStagesFile(File stagesFile) {
        this.stagesFile = stagesFile;
    }

    /**
     *
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

        if (stagesFile == null) {
            throw new NullPointerException("stagesFile property not set");
        }

        try {
            getStages(new FileReader(stagesFile));
        } catch (IOException err) {
            throw new RuntimeException("error reading stagesFile", err);
        }


        BufferedReader br = new BufferedReader(reader);
        String line = null;
        while ((line = br.readLine()) != null) {
            String cols[] = StringUtils.split(line, '\t');

            if (cols.length != 8) {
                LOG.error("ERROR parsing zfin expression data.  Malformed line: " + line);
                break;
            }

            // parse file
            String primaryIdentifier = cols[0];
            String symbol = cols[1];
            String genotype = cols[2];
            String anatomy = cols[4];
            String startStage = cols[5];
            String endStage = cols[6];
            String assay = cols[7];

            // gene
            String geneRefId = getGene(primaryIdentifier);

            // anatomy term
            String termRefId = getExpressionTerm(anatomy);

            // expression result
            Item result = createItem("ExpressionResult");
            result.setReference("gene", geneRefId);
            result.addToCollection("anatomyTerms", termRefId);
            result.setAttribute("expressed", "true");
            setStageRange(result, startStage, endStage);
            store(result);
        }
    }

    /**
     * ZDB-STAGE-050211-1      Unknown 0.0     17520.0
     * ZDB-STAGE-010723-4      Zygote:1-cell   0.0     0.75
     *
     * @param reader reader
     * @throws IOException if the file cannot be found/read
     */
    public void getStages(Reader reader)
    throws IOException, ObjectStoreException {
        BufferedReader br = new BufferedReader(reader);
        String line = null;
        while ((line = br.readLine()) != null) {
            String fields[] = StringUtils.split(line, '\t');
            String identifier = fields[0];
            String name = fields[1];
            String start = fields[2];
            String end = fields[3];

            Item stage = createItem("Stage");
            stage.setAttribute("identifier", identifier);
            stage.setAttribute("name", name);
            stage.setAttribute("start", start);
            stage.setAttribute("end", end);
            store(stage);
            stages.put(name, stage.getIdentifier());
        }
    }

    /**
     * @param result object representing expression result
     * @param start starting stage
     * @param end ending stage
     */
    public void setStageRange(Item result, String start, String end) {
        if (start.equals(end)) {
            result.setAttribute("stageRange", start);
            result.addToCollection("stages", stages.get(start));
            return;
        }
        result.setAttribute("stageRange", start + "-" + end);
        boolean addStage = false;
        for (Map.Entry<String, String> entry: stages.entrySet()) {
            String stage = entry.getKey();
            if (stage.equals(start)) {
                addStage = true;
            }
            if (addStage) {
                result.addToCollection("stages", entry.getValue());
            }
            if (stage.equals(end)) {
                addStage = false;
            }
        }
    }
    /**
     * @param expression
     * @return expression term
     * @throws ObjectStoreException
     */
    private String getExpressionTerm(String expression)
    throws ObjectStoreException {
        if (terms.containsKey(expression)) {
            return terms.get(expression);
        }
        Item term = createItem("AnatomyTerm");
        String refId = term.getIdentifier();
        term.setAttribute("name", expression);
        store(term);
        terms.put(expression, refId);
        return refId;
    }

    private String getGene(String identifier)
    throws ObjectStoreException {
        if (genes.containsKey(identifier)) {
            return genes.get(identifier);
        }
        Item gene = createItem("Gene");
        String refId = gene.getIdentifier();
        gene.setAttribute("primaryIdentifier", identifier);
        gene.setReference("organism", orgRefId);
        store(gene);
        genes.put(identifier, refId);
        createSynonym(refId, "identifier", identifier);
        return refId;
    }

    private void createSynonym(String subjectId, String type, String value)
    throws ObjectStoreException {
        String key = subjectId + type + value;
        if (!synonyms.contains(key)) {
            Item syn = createItem("Synonym");
            syn.setReference("subject", subjectId);
            syn.setAttribute("type", type);
            syn.setAttribute("value", value);
            store(syn);
            synonyms.add(key);
        }
    }
}
