package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.IOException;
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
 * DataConverter to create items from modENCODE fly expression score files.
 *
 * @author Fengyuan Hu
 */
public class FlyExpressionScoreConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(FlyExpressionScoreConverter.class);

    //
    private Item sub;
    private Item org;
    private static final String DATASET_TITLE =
        "Drosophila Cell Line and Developmental Stage Gene and Exon Scores";
    private static final String DATA_SOURCE_NAME = "Peter Cherbas";

    private static final String CELL_LINE = "cell line";
    private static final String DEVELOPMENTAL_STAGE = "developmental stage";
    private static final String DCCID = "modENCODE_3305";
    private static final String FLY_TAX_ID = "7227";

    private static Map<String, String> cellLines = null;
    private static Map<String, String> devStages = null;

    private Map<String, String> geneItems = new HashMap<String, String>();
    private Map<String, String> exonItems = new HashMap<String, String>();

    /**
     * Constructor.
     *
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws ObjectStoreException os
     */
    public FlyExpressionScoreConverter(ItemWriter writer, Model model) throws ObjectStoreException {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
        createSubmissionItem();
        createOrganismItem();
    }

    /**
     * Called for each file found by ant.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        // There are two files:
        // - Drosophila_Cell_Lines_and_Developmental_Stages_Gene_Scores.txt -
        // estimated expression levels for annotated genes
        // - Drosophila_Cell_Lines_and_Developmental_Stages_Exon_Scores.txt -
        // estimated expression levels for annotated exons
        // The following code works out which file we are reading and calls the corresponding method
        File currentFile = getCurrentFile();

        if ("Drosophila_Cell_Lines_and_Developmental_Stages_Gene_Scores.txt"
                .equals(currentFile.getName())) {
            processGeneScoreFile(reader, sub, org);
        } else if ("Drosophila_Cell_Lines_and_Developmental_Stages_Exon_Scores.txt"
                .equals(currentFile.getName())) {
            processExonScoreFile(reader, sub, org);
        } else {
            throw new IllegalArgumentException("Unexpected file: "
                    + currentFile.getName());
        }
    }

    /**
     * Process all rows of the
     * Drosophila_Cell_Lines_and_Developmental_Stages_Gene_Scores.txt file
     *
     * @param reader
     *            a reader for the
     *            Drosophila_Cell_Lines_and_Developmental_Stages_Gene_Scores.txt
     *            file
     * @throws IOException
     * @throws ObjectStoreException
     */
    private void processGeneScoreFile(Reader reader, Item submission, Item organism)
        throws IOException, ObjectStoreException {
        Iterator<?> tsvIter;
        try {
            tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
        } catch (Exception e) {
            throw new BuildException("cannot parse file: " + getCurrentFile(), e);
        }
        String [] headers = null;
        int lineNumber = 0;

        cellLines = new HashMap<String, String>();
        devStages = new HashMap<String, String>();

        while (tsvIter.hasNext()) {
            String[] line = (String[]) tsvIter.next();
            LOG.debug("SCOREg " + line[0]);

            if (lineNumber == 0) {
                // column headers - strip off any extra columns - FlyAtlas
                // not necessary for FlyExpressionScore, but OK to keep the code
                int end = 0;
                for (int i = 0; i < line.length; i++) {
                    if (StringUtils.isEmpty(line[i])) {
                        break;
                    }
                    end++;
                }
                headers = new String[end];
                System.arraycopy(line, 0, headers, 0, end);
            } else {
                String primaryId = line[0]; //Gene_FBgn
                // there seems to be some empty lines at the end of the file - FlyAtlas
                if (StringUtils.isEmpty(primaryId)) {
                    break;
                }
                createBioEntity(primaryId, "Gene");

                // Cell line starts from column 6 and ends at 30 which is hearder[5-29]
                for (int i = 5; i < 30; i++) {
                    String col = headers[i];
                    col = correctOfficialName(col, CELL_LINE);

                    if (!cellLines.containsKey(col)) {
                        Item cellLine = createCellLine(col);
                        cellLines.put(col, cellLine.getIdentifier());
                    }
                    Item score = createGeneExpressionScore(line[i]);
                    score.setReference("gene", geneItems.get(primaryId));
                    score.setReference("cellLine", cellLines.get(col));
                    score.setReference("submission", submission);
                    score.setReference("organism", organism);
                    store(score);
                }

                // Developmental stage starts from column 31 till the end
                for (int i = 30; i < headers.length; i++) {
                    String col = headers[i];
                    col = correctOfficialName(col, DEVELOPMENTAL_STAGE);

                    if (!devStages.containsKey(col)) {
                        Item developmentalStage = createDevelopmentalStage(col);
                        devStages.put(col, developmentalStage.getIdentifier());
                    }
                    Item score = createGeneExpressionScore(line[i]);
                    score.setReference("gene", geneItems.get(primaryId));
                    score.setReference("developmentalStage", devStages.get(col));
                    score.setReference("submission", submission);
                    score.setReference("organism", organism);
                    store(score);
                }
            }
            lineNumber++;
        }
    }

    /**
     * Process all rows of the
     * Drosophila_Cell_Lines_and_Developmental_Stages_Exon_Scores.txt file
     *
     * @param reader
     *            a reader for the
     *            Drosophila_Cell_Lines_and_Developmental_Stages_Exon_Scores.txt
     *            file
     * @throws IOException
     * @throws ObjectStoreException
     */
    private void processExonScoreFile(Reader reader, Item submission, Item organism)
        throws IOException, ObjectStoreException {
        Iterator<?> tsvIter;
        try {
            tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
        } catch (Exception e) {
            throw new BuildException("cannot parse file: " + getCurrentFile(), e);
        }
        String [] headers = null;
        int lineNumber = 0;

        cellLines = new HashMap<String, String>();
        devStages = new HashMap<String, String>();

        while (tsvIter.hasNext()) {
            String[] line = (String[]) tsvIter.next();
            LOG.debug("SCOREe " + line[4]);

            if (lineNumber == 0) {
                // column headers - strip off any extra columns - FlyAtlas
                // not necessary for FlyExpressionScore, but OK to keep the code
                int end = 0;
                for (int i = 0; i < line.length; i++) {
                    if (StringUtils.isEmpty(line[i])) {
                        break;
                    }
                    end++;
                }
                headers = new String[end];
                System.arraycopy(line, 0, headers, 0, end);
            } else {
                String primaryId = line[4]; //Annotation ID
                // there seems to be some empty lines at the end of the file - FlyAtlas
                if (StringUtils.isEmpty(primaryId)) {
                    break;
                }
                createBioEntity(primaryId, "Exon");

                // Cell line starts from column 8 and ends at 32 which is header[7-31]
                for (int i = 7; i < 32; i++) {
                    String col = headers[i];
                    col = correctOfficialName(col, CELL_LINE);

                    if (!cellLines.containsKey(col)) {
                        Item cellLine = createCellLine(col);
                        cellLines.put(col, cellLine.getIdentifier());
                    }

                    Item score = createExonExpressionScore(line[i]);
                    score.setReference("exon", exonItems.get(primaryId));
                    score.setReference("cellLine", cellLines.get(col));
                    score.setReference("submission", submission);
                    score.setReference("organism", organism);
                    store(score);
                }

                // Developmental stage starts from column 33 till the end
                for (int i = 32; i < headers.length; i++) {
                    String col = headers[i];
                    col = correctOfficialName(col, DEVELOPMENTAL_STAGE);

                    if (!devStages.containsKey(col)) {
                        Item developmentalStage = createDevelopmentalStage(col);
                        devStages.put(col, developmentalStage.getIdentifier());
                    }

                    Item score = createExonExpressionScore(line[i]);
                    score.setReference("exon", exonItems.get(primaryId));
                    score.setReference("developmentalStage", devStages.get(col));
                    score.setReference("submission", submission);
                    score.setReference("organism", organism);
                    store(score);
                }
            }
            lineNumber++;
        }
    }

    /**
     * Unify variations on similar official names.
     *
     * @param name the original 'official name' value
     * @param type cell line or developmental stage
     * @return a unified official name
     */
    private String correctOfficialName(String name, String type) {
        if (name == null) {
            return null;
        }

        if (type.equals(DEVELOPMENTAL_STAGE)) {
            name = name.replace("_", " ");

            if (name.matches("^emb.*\\d-\\dh$")) {
                name = name.replaceFirst("emb", "Embryo");
                name = name.replaceFirst("h", " h");
            }
            // Assume string like "L3_larvae_dark_blue" has the offical name
            // "L3 stage larvae dark blue"
            if (name.matches("^L\\d.*larvae.*$")) {
                name = name.replace("larvae", "stage larvae");
            }
            // TODO "WPP_2days" is not in the database
            if (name.matches("^WPP.*$")) {
                if (name.endsWith("hr")) {
                    String[] strs = name.split(" ");
                    StringBuffer sb = new StringBuffer();
                    sb.append(strs[0]).append(" + ").append(strs[1]);
                    name = name.replaceFirst("hr", " h");
                } else if (name.endsWith("days")) {

                }
                name = name.replaceFirst("WPP", "White prepupae (WPP)");
            }
        }
        else if (type.equals(CELL_LINE)) {

            if ("CME_L1".equals(name)) {
                name = name.replace("_", " ");
            }
            if ("CME W1 cl.8+".equals(name)) {
                name = name.replace("c", "C");
            }

            // The rest match the names in the database
        }

        return name;
    }


    /**
     * Create and store a FlyExpressionScore item on the first time called.
     *
     * @param score the expression score
     * @return an Item representing the FlyExpressionScore
     */
    @SuppressWarnings("unused")
    private Item createFlyExpressionScore(String score) throws ObjectStoreException {
        Item flyexpressionscore = createItem("FlyExpressionScore");
        flyexpressionscore.setAttribute("score", score);

        return flyexpressionscore;
    }

    /**
     * Create and store a GeneExpressionScore item on the first time called.
     *
     * @param score the expression score
     * @return an Item representing the GeneExpressionScore
     */
    private Item createGeneExpressionScore(String score) throws ObjectStoreException {
        Item expressionscore = createItem("GeneExpressionScore");
        expressionscore.setAttribute("score", score);

        return expressionscore;
    }

    /**
     * Create and store a ExonExpressionScore item on the first time called.
     *
     * @param score the expression score
     * @return an Item representing the ExonExpressionScore
     */
    private Item createExonExpressionScore(String score) throws ObjectStoreException {
        Item expressionscore = createItem("ExonExpressionScore");
        expressionscore.setAttribute("score", score);

        return expressionscore;
    }

    /**
     * Create and store a BioEntity item on the first time called.
     *
     * @param primaryId the primaryIdentifier
     * @param type gene or exon
     * @throws ObjectStoreException
     */
    private void createBioEntity(String primaryId, String type) throws ObjectStoreException {
        Item bioentity = null;

        if ("Gene".equals(type)) {
            if (!geneItems.containsKey(primaryId)) {
                bioentity = createItem("Gene");
                bioentity.setAttribute("primaryIdentifier", primaryId);
                store(bioentity);
                geneItems.put(primaryId, bioentity.getIdentifier());
            }
        } else if ("Exon".equals(type)) {
            if (!exonItems.containsKey(primaryId)) {
                bioentity = createItem("Exon");
                bioentity.setAttribute("primaryIdentifier", primaryId);
                store(bioentity);
                exonItems.put(primaryId, bioentity.getIdentifier());
            }
        }
    }


    /**
     * Create and store a Submission item on the first time called.
     *
     * @param dccid the submission id
     * @return an Item representing the Submission
     * @throws ObjectStoreException os
     */
    @SuppressWarnings("unused")
    private Item createSubmission(String dCCid) throws ObjectStoreException {
        Item submission = createItem("Submission");
        submission.setAttribute("DCCid", dCCid);
        store(submission);

        return submission;
    }

    /**
     * Create and store a Submission item on the first time called.
     *
     * @throws ObjectStoreException os
     */
    protected void createSubmissionItem() throws ObjectStoreException {
        sub = createItem("Submission");
        sub.setAttribute("DCCid", DCCID);
        store(sub);
    }

    /**
     * Create and store a organism item on the first time called.
     *
     * @throws ObjectStoreException os
     */
    protected void createOrganismItem() throws ObjectStoreException {
        org = createItem("Organism");
        org.setAttribute("taxonId", FLY_TAX_ID);
        store(org);
    }

    /**
     * Create and store a CellLine item on the first time called.
     *
     * @param name the cell line name
     * @return an Item representing the CellLine
     */
    private Item createCellLine(String name) throws ObjectStoreException {
        Item cellline = createItem("CellLine");
        cellline.setAttribute("name", name);
        store(cellline);

        return cellline;
    }

    /**
     * Create and store a DevelopmentalStage item on the first time called.
     *
     * @param name the developmental stage name
     * @return an Item representing the DevelopmentalStage
     * @throws ObjectStoreException
     */
    private Item createDevelopmentalStage(String name) throws ObjectStoreException {
        Item developmentalstage = createItem("DevelopmentalStage");
        developmentalstage.setAttribute("name", name);
        store(developmentalstage);

        return developmentalstage;
    }
}
