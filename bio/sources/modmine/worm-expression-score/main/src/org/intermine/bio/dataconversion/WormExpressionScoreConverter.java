package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2013 FlyMine
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
 *
 * @author
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

//    private Map<String, String> geneItems = new HashMap<String, String>();
    private Map<String, String> mRNAItems = new HashMap<String, String>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws ObjectStoreException
     */
    public WormExpressionScoreConverter(ItemWriter writer, Model model)
        throws ObjectStoreException {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
        createSubmissionItem();
        createOrganismItem();
    }

    /**
     *
     *
     * {@inheritDoc}
     */
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

     *   note: transript is mRNA in ws220 (~ gene.symbol)
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

            // Developmental stage starts from column 3 till the end
            for (int i = 2; i < headers.length; i++) {
                String col = headers[i];
                //LOG.info("WWW " + i + ": " + col);
                col = correctOfficialName(col, DEVELOPMENTAL_STAGE);

                if (!devStages.containsKey(col)) {
                    Item developmentalStage = createDevelopmentalStage(col);
                    devStages.put(col, developmentalStage.getIdentifier());
                }
                // Item score = createGeneExpressionScore(line[i]);
                // score.setReference("gene", geneItems.get(primaryId));
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
     * TODO, data from marc
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

            String newName = DEVSTAGES_NAME_MAP.get(name);

            if (newName == null) {
                LOG.warn("NOT FOUND in DEVSTAGES_NAME_MAP: " + name);
            } else {
                name = newName;
            }

            name = name.replace("_", " ");

            if (name.matches("^emb.*\\d-\\dh$")) {
                name = name.replaceFirst("emb", "Embryo");
                name = name.replaceFirst("h", " h");
            }

            // TODO : rm?
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
     */
    private Item createMRNAExpressionScore(String score) throws ObjectStoreException {
        Item expressionscore = createItem("MRNAExpressionScore");
        expressionscore.setAttribute("score", score);

        return expressionscore;
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
     * Create and store a BioEntity item on the first time called.
     *
     * @param primaryId the primaryIdentifier
     * @param type mRNA or exon, at the moment only mRNA
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

        //if ("Gene".equals(type)) {
        //if (!geneItems.containsKey(primaryId)) {
        //bioentity = createItem("Gene");
        //bioentity.setAttribute("symbol", primaryId);
        //store(bioentity);
        //geneItems.put(primaryId, bioentity.getIdentifier());
        //}
        //}
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


    /**
     * maps from dev stages names in the file to modENCODE names.
     *
     *
     * a check is performed and fields unaccounted for are logged.
     */
    private static final Map<String, String> DEVSTAGES_NAME_MAP =
            new HashMap<String, String>();
    private static final String NOT_TO_BE_LOADED = "this is ; illegal - anyway";

    static {
        // the translation is provided by marc.
        // TODO: there are duplicates values.

        DEVSTAGES_NAME_MAP.put("N2_EE_50-0", "N2 EE 50-0");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-30", "N2 EE 50-30");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-60", "N2 EE 50-60");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-90", "N2 EE 50-90");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-120", "N2 EE 50-120");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-150", "N2 EE 50-150");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-180", "N2 EE 50-180");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-210", "N2 EE 50-210");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-240", "N2 EE 50-240");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-300", "N2 EE 50-300");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-330", "N2 EE 50-330");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-360", "N2 EE 50-360");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-390", "N2 EE 50-390");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-420", "N2 EE 50-420");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-450", "N2 EE 50-450");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-480", "N2 EE 50-480");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-510", "N2_EE_50-510");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-540", "N2 EE 50-540");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-570", "N2 EE 50-570");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-600", "N2 EE 50-600");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-630", "N2 EE 50-630");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-660", "N2 EE 50-660");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-690", "N2 EE 50-690");
        DEVSTAGES_NAME_MAP.put("N2_EE_50-720", "N2 EE 50-720");
        DEVSTAGES_NAME_MAP.put("EmMalesHIM8_EmMalesHIM8-2", "Embryo him-8(e1489) 20dC");
        DEVSTAGES_NAME_MAP.put("N2_4cell_EE_RZ-56", "4 cell stage embryo");
        DEVSTAGES_NAME_MAP.put("N2_E2-E8_sorted", "E2-E8 cells");
        DEVSTAGES_NAME_MAP.put("N2_EE_DSN-51", "D1 early Embryo");
        DEVSTAGES_NAME_MAP.put("EE", "early Embryo");
        DEVSTAGES_NAME_MAP.put("N2_EE-2", "early Embryo EE-2");
        DEVSTAGES_NAME_MAP.put("EE_N2_EE-2", "D1 early Embryo EE-2");
        DEVSTAGES_NAME_MAP.put("N2_EE_RZ-54", "D2 early Embryo");
        DEVSTAGES_NAME_MAP.put("LE", "late Embryo 20dC 4.5 hours post-early embryo");
        DEVSTAGES_NAME_MAP.put("N2_LE-1", "D1 late Embryo 20dC 4.5 hours post-early embryo");
        DEVSTAGES_NAME_MAP.put("LE_N2_LE-1", "D2 late Embryo 20dC 4.5 hours post-early embryo");
        DEVSTAGES_NAME_MAP.put("L1", "mid-L1 20dC 4hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("N2_L1-1", "D1 mid-L1 20dC 4hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("L1_N2_L1-1", "D2 mid-L1 20dC 4hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("N2_L2_RZ-53", "mid-L2 20dC 14hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("LIN35", "D3 mid-L1 20dC 4hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("L2", "D1 mid-L2 20dC 14hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("N2_L2-4", "D2 mid-L2 20dC 14hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("L2_N2_L2-4", "D3 mid-L2 20dC 14hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("L3", "mid-L3 20dC 25hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("N2_L3-1", "D1 mid-L3 20dC 25hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("L3_N2_L3-1", "D2 mid-L3 20dC 25hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DauerEntryDAF2", "dauer entry daf-2(el370) 25dC 48 hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DauerEntryDAF2-2", "D1 dauer entry daf-2(el370) 25dC 48 hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DauerEntryDAF2-1-1", "D2 dauer entry daf-2(el370) 25dC 48 hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DauerEntryDAF2-4-1", "D3 dauer entry daf-2(el370) 25dC 48 hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DauerEntryDAF2_DauerEntryDAF2-2_DauerEntryDAF2-1-1_DauerEntryDAF2-4-1", "D4 dauer entry daf-2(el370) 25dC 48 hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DauerDAF2", "D5 dauer daf-2(el370) 25dC 91hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DauerDAF2-2", "D6 dauer daf-2(el370) 25dC 91hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DauerDAF2-2-1", "D7 dauer daf-2(el370) 25dC 91hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DauerDAF2-5-1", "D8 dauer daf-2(el370) 25dC 91hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DauerDAF2_DauerDAF2-2_DauerDAF2-2-1_DauerDAF2-5-1", "D9 dauer daf-2(el370) 25dC 91hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DauerExitDAF2-2", "dauer exit daf-2(e1370) 25dC 91hrs 15dC 12hrs");
        DEVSTAGES_NAME_MAP.put("DauerExitDAF2-3-1", "D1 dauer exit daf-2(e1370) 25dC 91hrs 15dC 12hrs");
        DEVSTAGES_NAME_MAP.put("DauerExitDAF2-6-1", "D2 dauer exit daf-2(e1370) 25dC 91hrs 15dC 12hrs");
        DEVSTAGES_NAME_MAP.put("DauerExitDAF2-2_DauerExitDAF2-3-1_DauerExitDAF2-6-1", "D3 dauer exit daf-2(e1370) 25dC 91hrs 15dC 12hrs");
        DEVSTAGES_NAME_MAP.put("L4", "mid-L4 20dC 36hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("L4b", "D1 mid-L4 20dC 36hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("L4_L4b", "D2 mid-L4 20dC 36hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("L4JK1107soma", "L4 soma JK1107 no DNaseI");
        DEVSTAGES_NAME_MAP.put("L4JK1107soma-2", "D1 L4 soma JK1107 no DNaseI");
        DEVSTAGES_NAME_MAP.put("L4JK1107soma_L4JK1107soma-2", "D2 L4 soma JK1107 no DNaseI");
        DEVSTAGES_NAME_MAP.put("L4MALE", "Male larva mid-L4 25dC 30 hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("L4MALE5", "D1 Male larva mid-L4 25dC 30 hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("L4MALE_L4MALE5", "D2 Male larva mid-L4 25dC 30 hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("YA", "Young Adult 20dC 42 hr post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("N2_Yad-1", "D1 Young Adult 20dC 42 hr post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("YA_N2_Yad-1", "D2 Young Adult 20dC 42 hr post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("N2_YA_RZ-1", "D1 Young Adult 20dC 42 hr post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("AdultSPE9", "Adult spe-9(hc88) 23dC 8 days post-L4 molt");
        DEVSTAGES_NAME_MAP.put("PharyngealMuscle", "late Embryo 20dC 4.5 hours post-early embryo");
        DEVSTAGES_NAME_MAP.put("DC-1-5", "Young Adult");
        DEVSTAGES_NAME_MAP.put("DC-2-12", "D1 Young Adult");
        DEVSTAGES_NAME_MAP.put("OPDC-2-12", "D2 Young Adult");
        DEVSTAGES_NAME_MAP.put("EF-1-24", "D3 Young Adult");
        DEVSTAGES_NAME_MAP.put("PL-2-24", "D4 Young Adult");
        DEVSTAGES_NAME_MAP.put("Hsph", "D5 Young Adult");
        DEVSTAGES_NAME_MAP.put("HsphEcoliCntl", "D6 Young Adult");
        DEVSTAGES_NAME_MAP.put("SmacDb10", "D7 Young Adult");
        DEVSTAGES_NAME_MAP.put("SmacDb10EcoliCntl", "D8 Young Adult");
        DEVSTAGES_NAME_MAP.put("Harpo", "D9 Young Adult");
        DEVSTAGES_NAME_MAP.put("HarpoEcoliCntl", "D10 Young Adult");
        DEVSTAGES_NAME_MAP.put("DMM386-NSML_NSMR-nrn_L1", "mid-L1 20dC 4hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DMM401_N2all_L1", "D1 mid-L1 20dC 4hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DMM402_N2all_L1", "D2 mid-L1 20dC 4hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DMM408_Amot_nrn_L2", "mid-L2 20dC 14hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DMM414_Amot_nrn_L2", "D1 mid-L2 20dC 14hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DMM415_Amot_nrn_L2", "D2 mid-L2 20dC 14hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DSN-Negative-Positive", "4 cell stage embryo");
        DEVSTAGES_NAME_MAP.put("DMM387-NSML_NSMR-nrn_L1-V", "mid-L1 20dC 4hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DMM383-all-nrn_L1-V", "D1 mid-L1 20dC 4hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DMM391-all-nrn_L1-V", "D2 mid-L1 20dC 4hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DMM401-N2all_L1-V", "D3 mid-L1 20dC 4hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("DMM402-N2all_L1-V", "D4 mid-L1 20dC 4hrs post-L1 stage larvae");
        DEVSTAGES_NAME_MAP.put("AG1201", "Mixed Larval Stages Grown for 4-5 Days");
    }



}
