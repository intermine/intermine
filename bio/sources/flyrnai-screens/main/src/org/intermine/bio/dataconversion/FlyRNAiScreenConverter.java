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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.TextFileUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.ItemFactory;
import org.intermine.metadata.Model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.FileReader;
import java.io.File;

import org.apache.tools.ant.BuildException;

/**
 * DataConverter to create items from DRSC RNAi screen date files.
 *
 * @author Kim Rutherford
 */
public class FlyRNAiScreenConverter extends FileConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";

    protected Item dataSource, organism, hfaSource;
    private Map ids = new HashMap();
    private ItemFactory itemFactory;

    private Map genes = new HashMap();
    private Map synonyms = new HashMap();
    private Map publications = new HashMap();
    private Map amplicons = new HashMap();

    protected String taxonId;
    // access to current file for error messages
    private String fileName;

    private static final String PMID_PREFIX = "PMID:";
    private static final String SCREEN_NAME_PREFIX = "Screen_name:";
    private static final String CELL_LINE_PREFIX = "Cell line:";
    private static final String PHENOTYPE_DESCRIPTION_PREFIX = "Phenotype_description:";

    private static final String[] HEADER_FIELDS = new String[] {
        PMID_PREFIX, SCREEN_NAME_PREFIX, CELL_LINE_PREFIX, PHENOTYPE_DESCRIPTION_PREFIX
    };

    // column headings
    private static final String FBGN_COLUMN = "FBGN";
    private static final String PHENOTYPE_COLUMN = "Phenotype";
    private static final String DRSC_AMPLICON_COLUMN = "DRSC_Amplicon";
    private static final String CURATED_DRSC_AMPLICON_COLUMN = "DRSC Amplicon";
    private static final String HFA_AMPLICON_COLUMN = "HFA_Amplicon";
    private static final String AMPLICON_LENGTH_COLUMN = "Amplicon_Length";
    private static final String NUM_OFF_TARGET_COLUMN = "19bp_Matches";
    private static final String CAR_REPEATS_COLUMN = "CAR";
    private static final String HIT_COLUMN = "Hit";

    private Item dataSet;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     */
    public FlyRNAiScreenConverter(ItemWriter writer) {
        super(writer);
        itemFactory = new ItemFactory(Model.getInstanceByName("genomic"));
    }

    /**
     * @see FileConverter#process(Reader)
     */
    public void process(Reader reader) throws Exception {
        if (taxonId == null) {
            throw new RuntimeException("taxonId attribute not set");
        }

        // Files named *.dataset contain the actual data.  Rows that have a
        // corresponding entry in *_curated.tsv need to have
        // RNAiScreenHit.hasPredictedOffTargetEffect set to false, otherwise
        // the flag should be true.  DRSC Amplicon ids can be used to match
        // entries in the two files.

        fileName = getCurrentFile().getPath();
        if (!fileName.endsWith(".dataset")) {
            return;
        }

        BufferedReader curatedReader = null;
        try {
            File curatedFile = new File(fileName.substring(0, fileName.indexOf("."))
                                   + "_curated.tsv");
            curatedReader = new BufferedReader(new FileReader(curatedFile));
        } catch (Exception e) {
            throw new RuntimeException("Unable to find curated file for: " + fileName, e);
        }

        Set offTargetFalse = readCurated(curatedReader);
        System.out. println("offTargetFalse: " + offTargetFalse);

        BufferedReader br = new BufferedReader(reader);

        if (organism == null) {
            organism = newItem("Organism");
            organism.setAttribute("taxonId", taxonId);
            writer.store(ItemHelper.convert(organism));
        }

        System.err .println("Processing file: " + getCurrentFile().getName());

        Map headerFieldValues = getHeaderFields(br);

        String pubmedId = (String) headerFieldValues.get(PMID_PREFIX);

        Item publication = newPublication(pubmedId);

        dataSet = newItem("DataSet");
        dataSet.setAttribute("title", "DRSC RNAi data set: "
                             + headerFieldValues.get(SCREEN_NAME_PREFIX));
        if (dataSource == null) {
            dataSource = newItem("DataSource");
            dataSource.setAttribute("name", "Drosophila RNAi Screening Center");
            writer.store(ItemHelper.convert(dataSource));
        }
        if (hfaSource == null) {
            hfaSource = newItem("DataSource");
            hfaSource.setAttribute("name", "Renato Paro lab");
            writer.store(ItemHelper.convert(hfaSource));
        }
        dataSet.setReference("dataSource", dataSource);
        writer.store(ItemHelper.convert(dataSet));

        Item rnaiScreen = newItem("RNAiScreen");
        rnaiScreen.setAttribute("name", (String) headerFieldValues.get(SCREEN_NAME_PREFIX));
        rnaiScreen.setAttribute("analysisDescription",
                                (String) headerFieldValues.get(PHENOTYPE_DESCRIPTION_PREFIX));
        rnaiScreen.setAttribute("cellLine", (String) headerFieldValues.get(CELL_LINE_PREFIX));
        rnaiScreen.setReference("organism", organism);
        rnaiScreen.setReference("publication", publication);
        writer.store(ItemHelper.convert(rnaiScreen));

        String[] columnNameRow = null;

        // a Map from column name to column index
        Map columnNameMap = new HashMap();

        Iterator tsvIter;
        try {
            tsvIter = TextFileUtil.parseTabDelimitedReader(br);
        } catch (Exception e) {
            throw new BuildException("cannot parse file: " + getCurrentFile(), e);
        }

        while (tsvIter.hasNext()) {
            String [] thisRow = (String[]) tsvIter.next();
            if (columnNameRow == null) {
                // the first row has the column headings (unless it is blank)
                if (thisRow.length <= 1) {
                    continue;
                }
                columnNameRow = thisRow;
                for (int i = 0; i < columnNameRow.length; i++) {
                    columnNameMap.put(columnNameRow[i], new Integer(i));
                }
                continue;
            }

            String hitsColumn = getColumnValue(columnNameMap, thisRow, HIT_COLUMN);
            String [] hitStrengths = hitsColumn.split("[ \\t]*,[ \\t]*");

            if (hitsColumn.equals("")) {
                continue;
            }


            String ampliconIdentifier =
                getColumnValue(columnNameMap, thisRow, DRSC_AMPLICON_COLUMN);
            // if the amplicon doesn't appear in the curated file then we don't want
            // to create it, probably has off target effects.
            if (!offTargetFalse.contains(ampliconIdentifier)) {
                continue;
            }
            Item amplicon = (Item) amplicons.get(ampliconIdentifier);
            if (amplicon == null) {
                amplicon = newItem("Amplicon");
                String ampliconLength =
                    getColumnValue(columnNameMap, thisRow, AMPLICON_LENGTH_COLUMN);
                String hfaAmpliconIdentifier =
                    getColumnValue(columnNameMap, thisRow, HFA_AMPLICON_COLUMN);
                try {
                    new Integer(ampliconLength);
                } catch (NumberFormatException e) {
                    // ignore hits with invalid amplicon lengths
                    continue;
                }

                amplicon.setAttribute("identifier", ampliconIdentifier);
                amplicon.setAttribute("length", ampliconLength);
                amplicon.setReference("organism", organism);
                amplicons.put(ampliconIdentifier, amplicon);
                writer.store(ItemHelper.convert(amplicon));

                newSynonym(ampliconIdentifier, amplicon, dataSource);
                newSynonym(hfaAmpliconIdentifier, amplicon, hfaSource);
            }

            String numOffTargets =
                getColumnValue(columnNameMap, thisRow, NUM_OFF_TARGET_COLUMN);

            for (int hitStrengthIndex = 0; hitStrengthIndex < hitStrengths.length;
                 hitStrengthIndex++) {
                String hitStrength = hitStrengths[hitStrengthIndex];

                // get columns using the column names from the header line
                String geneNameColumn = getColumnValue(columnNameMap, thisRow, FBGN_COLUMN);
                String [] geneNames = geneNameColumn.split("[ \\t]*,[ \\t]*");

                for (int geneIndex = 0; geneIndex < geneNames.length; geneIndex++) {
                    String geneName = geneNames[geneIndex];
                    int geneNameColonIndex = geneName.lastIndexOf(':');
                    if (geneNameColonIndex != -1) {
                        geneName = geneName.substring(geneNameColonIndex + 1).trim();
                    }
                    if (geneName.equals("")) {
                        continue;
                    }

                    Item gene = newGene(geneName);

                    Item screenHit = newItem("RNAiScreenHit");
                    screenHit.setReference("analysis", rnaiScreen);
                    screenHit.setReference("gene", gene);
                    String result = getColumnValue(columnNameMap, thisRow, PHENOTYPE_COLUMN);
                    screenHit.setAttribute("result", result);
                    String carRepeats = getColumnValue(columnNameMap, thisRow, CAR_REPEATS_COLUMN);
                    screenHit.setAttribute("carRepeats", carRepeats);
                    screenHit.setAttribute("numOffTargets", numOffTargets);
                    if (hitStrength != null && hitStrength.trim().length() > 0) {
                        try {
                            new Float(hitStrength);
                            screenHit.setAttribute("strength", hitStrength);
                        } catch (NumberFormatException e) {
                            // ignore - probably a "Y"
                        }
                    }
                    // we are only creating amplicons without off target effects
                    // so don't really need this attribute
                    screenHit.setReference("amplicon", amplicon);
                    String offTarget = offTargetFalse.contains(ampliconIdentifier)
                        ? "false" : "true";
                    screenHit.setAttribute("hasPredictedOffTargetEffect", offTarget);
                    writer.store(ItemHelper.convert(screenHit));
                }
            }
        }
    }

    private Item newPublication(String pubmedId) {
        Item publication;
        if (publications.containsKey(pubmedId)) {
            publication = (Item) publications.get(pubmedId);
        } else {
            publication = newItem("Publication");
            publication.setAttribute("pubMedId", pubmedId);
            publications.put(pubmedId, publication);
        }
        return publication;
    }

    /**
     * Return a map from column name to column index.
     */
    private Map getHeaderFields(BufferedReader br) throws IOException {
        Map headerFieldValues = new HashMap();

        String line = null;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.equals("----")) {
                // end of header
                break;
            }

            for (int i = 0; i < HEADER_FIELDS.length; i++) {
                String headerFieldName = HEADER_FIELDS[i];
                if (line.startsWith("# " + headerFieldName)) {
                    String headerFieldValue = line.substring(headerFieldName.length() + 3);
                    headerFieldValues.put(headerFieldName, headerFieldValue);
                }
            }
        }


        // check for missing header fields
        for (int i = 0; i < HEADER_FIELDS.length; i++) {
            String headerFieldName = HEADER_FIELDS[i];
            if (!headerFieldValues.containsKey(headerFieldName)) {
                throw new RuntimeException("missing header field: " + headerFieldName + " in "
                                           + getCurrentFile().getName());
            }
        }
        return headerFieldValues;
    }

    private String getColumnValue(Map columnNameMap, String[] row, String columnTag) {
        Integer columnIndex = (Integer) columnNameMap.get(columnTag);
        if (columnIndex == null) {
            throw new RuntimeException("can't find column index for: " + columnTag + " in "
                                       + columnNameMap + " while reading: " + fileName);
        }
        return row[columnIndex.intValue()];
    }

    /**
     * Set the taxon ID of the organism object that we will create
     * @param taxonId the taxon ID
     */
    public void setTaxonId(String taxonId) {
        this.taxonId = taxonId;
    }

    /**
     * Convenience method to create a new gene Item
     * @param geneName the gene name
     * @return a new gene Item
     * @throws ObjectStoreException if an error occurs when storing the Item
     */
    protected Item newGene(String geneName)  throws ObjectStoreException {
        if (geneName == null) {
            throw new RuntimeException("geneName can't be null");
        }
        Item item = (Item) genes.get(geneName);
        if (item == null) {
            item = newItem("Gene");
            item.setAttribute("organismDbId", geneName);
            // identifier needs to be a Synonym for quick search to work
            newSynonym(geneName, item, dataSource);
            item.setReference("organism", organism);
            genes.put(geneName, item);
        }
        return item;
    }

    /**
     * Convenience method to create and store a new synonym Item
     * @param synonymName the actual synonym
     * @param subject the synonym's subject item
     * @param source the source of the Synonym
     * @return a new synonym Item
     */
    protected Item newSynonym(String synonymName, Item subject, Item source) {
        if (synonymName == null) {
            throw new RuntimeException("synonymName can't be null");
        }
        if (synonyms.containsKey(synonymName)) {
            return (Item) synonyms.get(synonymName);
        }
        Item item = newItem("Synonym");
        item.setAttribute("value", synonymName);
        item.setAttribute("type", "identifier");
        item.setReference("subject", subject.getIdentifier());
        item.setReference("source", source.getIdentifier());
        item.addToCollection("evidence", dataSet.getIdentifier());
        synonyms.put(synonymName, item);
        return item;
    }

    /**
     * Convenience method for creating a new Item
     * @param className the name of the class
     * @return a new Item
     */
    protected Item newItem(String className) {
        return itemFactory.makeItem(alias(className) + "_" + newId(className),
                                    GENOMIC_NS + className, "");
    }

    private String newId(String className) {
        Integer id = (Integer) ids.get(className);
        if (id == null) {
            id = new Integer(0);
            ids.put(className, id);
        }
        id = new Integer(id.intValue() + 1);
        ids.put(className, id);
        return id.toString();
    }

    /**
     * @see FileConverter#close()
     */
    public void close() throws ObjectStoreException {
        store(genes.values());
        store(synonyms.values());
        store(publications.values());
    }

    private Set readCurated(BufferedReader br) throws IOException {
        Set offtarget = new HashSet();
        Iterator tsvIter = TextFileUtil.parseTabDelimitedReader(br);
        String[] curatedNameRow = null;

        Map curatedNameMap = new HashMap();
        while (tsvIter.hasNext()) {
            String [] thisRow = (String[]) tsvIter.next();
            // get rid of header and find column headings
            if (curatedNameRow == null) {
                if (thisRow[0].equals("Final Hit")) {
                    curatedNameRow = thisRow;
                    for (int i = 0; i < curatedNameRow.length; i++) {
                        curatedNameMap.put(curatedNameRow[i], new Integer(i));
                    }
                }
                continue;
            }
            // reading actual data
            String amplicon = getColumnValue(curatedNameMap, thisRow, CURATED_DRSC_AMPLICON_COLUMN);
            if (amplicon != null) {
                offtarget.add(amplicon);
            }
        }
        return offtarget;
    }
}

