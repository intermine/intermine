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
 * @author Fengyuan
 */
public class WormExpressionScoreConverter extends BioFileConverter
{
    //
    private static final Logger LOG = Logger.getLogger(WormExpressionScoreConverter.class);

    //
    private Item sub;
    private Item org;
    private static final String DATASET_TITLE =
            "C. elegans Developmental Stage mRNA Scores";
    private static final String DATA_SOURCE_NAME = "Robert Waterston";

    //private static final String CELL_LINE = "cell line";
    private static final String DEVELOPMENTAL_STAGE = "developmental stage";
    private static final String DCCID = "modENCODE_xxxx";
    private static final String WORM_TAX_ID = "6239";

    private static Map<String, String> devStages = null;

    private Map<String, String> mRNAItems = new HashMap<String, String>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws ObjectStoreException if something goes wrong
     */
    public WormExpressionScoreConverter(ItemWriter writer, Model model)
        throws ObjectStoreException {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
        createSubmissionItem();
        createOrganismItem();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws Exception {
        File currentFile = getCurrentFile();

        if ("C_elegans.dcpm_per_gene_per_stage.ws220".equals(currentFile.getName())) {
            processScoreFile(reader, sub, org);
        } else {
            LOG.info("WWSS skipping file: " + currentFile.getName());
            //            throw new IllegalArgumentException("Unexpected file: "
//          + currentFile.getName());
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
     *
     *       from the README file:
     *
The dcpm is calculated

s/c/1000000/n

where
s = sum of the raw high quality coverage of the bases in that transcript that are rep-value <96
c = number of bases in the transcript that are rep-value < 96
n = normalized total read coverage of the genome


column 1: ws220 transcript
column 2: number of non-repetitive bases (representation value < 96) in this transcript
# the following columns contain the "dcpm" value for the following stages
column 3: N2_EE_50-0
column 4: N2_EE_50-30
column 5: N2_EE_50-60

     *   note: transript is mRNA in ws220, = gene.symbol
     *   note: dev stages given as short names, not official names.
     *         we need look up table (from nlw)
     *
     * @throws IOException
     * @throws ObjectStoreException
     */
    private void processScoreFile(Reader reader, Item submission, Item organism)
        throws IOException, ObjectStoreException {
        Iterator<?> tsvIter;
        try {
            tsvIter = FormattedTextParser.parseTabDelimitedReader(reader);
        } catch (Exception e) {
            throw new BuildException("cannot parse file: " + getCurrentFile(), e);
        }

        String [] headers = null;
        int lineNumber = 0;

        devStages = new HashMap<String, String>();

        while (tsvIter.hasNext()) {
            String[] line = (String[]) tsvIter.next();
//            LOG.info("SCOREg " + line[0]);

            if (lineNumber == 0) {
                //LOG.info("SCOREg " + line[0]);
                // column headers - strip off any extra columns - FlyAtlas
                // not necessary for expressionScore, but OK to keep the code
                int end = 0;
                for (int i = 0; i < line.length; i++) {
                    if (StringUtils.isEmpty(line[i])) {
                        break;
                    }
                    end++;
                }
                headers = new String[end];
                System.arraycopy(line, 0, headers, 0, end);
                LOG.info("WW header lenght " + headers.length);
                lineNumber++;
                continue;
            }

            String primaryId = line[0]; // mRNA id, e.g. 2RSSE.1 == Gene.symbol
            // there seems to be some empty lines at the end of the file - FlyAtlas
            if (StringUtils.isEmpty(primaryId)) {
                break;
            }
            createBioEntity(primaryId, "MRNA");
            //createBioEntity(primaryId, "Gene"); // id=symbol

            if (headers == null) {
                continue;
            }

            // Developmental stage starts from column 3 till the end
            for (int i = 2; i < headers.length; i++) {
                String col = headers[i];
                //LOG.info("WWW " + i + ": " + col);
                col = correctOfficialName(col, DEVELOPMENTAL_STAGE);

                if (!devStages.containsKey(col)) {
                    Item developmentalStage = createDevelopmentalStage(col);
                    devStages.put(col, developmentalStage.getIdentifier());
                }
                //                    Item score = createGeneExpressionScore(line[i]);
                //                    score.setReference("gene", geneItems.get(primaryId));
                Item score = createMRNAExpressionScore(line[i]);
                score.setReference("mRNA", mRNAItems.get(primaryId));
                score.setReference("developmentalStage", devStages.get(col));
                score.setReference("submission", submission);
                score.setReference("organism", organism);
                store(score);
            }

            lineNumber++;
        }
    }

    /**
     * Unify variations on similar official names.
     *
     * @param officialName the original 'official name' value
     * @param type cell line or developmental stage
     * @return a unified official name
     */
    private static String correctOfficialName(String officialName, String type) {
        String name = officialName;
        if (name == null) {
            return null;
        }

        if (type.equals(DEVELOPMENTAL_STAGE)) {
            name = name.replace("_", " ");

            if (name.matches("^emb.*\\d-\\dh$")) {
                name = name.replaceFirst("emb", "Embryo");
                name = name.replaceFirst("h", " h");
            }
            // Assume string like "L3_larvae_dark_blue" has the official name
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
                    // do nothing
                }
                name = name.replaceFirst("WPP", "White prepupae (WPP)");
            }
        }

        return name;
    }


    /**
     * Create and store a MRNAExpressionScore item on the first time called.
     *
     * @param score the expression score
     * @return an Item representing the MRNAExpressionScore
     * @throws ObjectStoreException if problem storing to db
     */
    private Item createMRNAExpressionScore(String score) throws ObjectStoreException {
        Item expressionscore = createItem("MRNAExpressionScore");
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

        if ("MRNA".equals(type)) {
            if (!mRNAItems.containsKey(primaryId)) {
                bioentity = createItem("MRNA");
                bioentity.setAttribute("primaryIdentifier", primaryId);
                store(bioentity);
                mRNAItems.put(primaryId, bioentity.getIdentifier());
            }
        }
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
        org.setAttribute("taxonId", WORM_TAX_ID);
        store(org);
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
