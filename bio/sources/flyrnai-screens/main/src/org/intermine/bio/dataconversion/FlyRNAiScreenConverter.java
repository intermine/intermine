package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
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

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.TextFileUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.ItemHelper;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.tools.ant.BuildException;

/**
 * DataConverter to create items from DRSC RNAi screen date files.
 * 
 * @author Kim Rutherford
 */
public class FlyRNAiScreenConverter extends FileConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";

    protected Item dataSource, organism;
    protected int id = 0;

    private Map genes = new HashMap();
    private Map synonyms = new HashMap();
    
    private ItemFactory tgtItemFactory;
    private String taxonId;

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
    private static final String DRSC_AMPLICON_COLUMN = "DRSC Amplicon";
    private static final String AMPLICON_LENGTH_COLUMN = "Amp. Length";
    private static final String HIT_COLUMN = "Hit";

    private Item dataSet;
    
    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     */
    public FlyRNAiScreenConverter(ItemWriter writer) {
        super(writer);
        tgtItemFactory = new ItemFactory(Model.getInstanceByName("genomic"), "1_");
    }

    /**
     * @see FileConverter#process(Reader)
     */
    public void process(Reader reader) throws Exception {
        BufferedReader br = new BufferedReader(reader);

        if (organism == null) {
            organism = tgtItemFactory.makeItem();
            organism.setAttribute("taxonId", taxonId);
            writer.store(ItemHelper.convert(organism));
        }
        
        System.err .println("Processing file: " + getCurrentFile().getName());
        
        Map headerFieldValues = getHeaderFields(br);
        
        Item publication = newItem("Publication");
        publication.setAttribute("pubMedId", (String) headerFieldValues.get(PMID_PREFIX));
        writer.store(ItemHelper.convert(publication));

        dataSet = newItem("DataSet");
        dataSet.setAttribute("title", "DRSC RNAi data set: "
                             + headerFieldValues.get(SCREEN_NAME_PREFIX));
        if (dataSource == null) {
            dataSource = newItem("DataSource");
            dataSource.setAttribute("name", "Drosophila RNAi Screening Center");
            writer.store(ItemHelper.convert(dataSource));
        }
        dataSet.setReference("dataSource", dataSource);
        writer.store(ItemHelper.convert(dataSet));
        
        Item rnaiScreen= newItem("RNAiScreen");
        rnaiScreen.setAttribute("name", (String) headerFieldValues.get(SCREEN_NAME_PREFIX));
        rnaiScreen.setAttribute("phenotypeDescription", 
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
                // the first row has the column headings
                columnNameRow = thisRow;
                for (int i = 0; i < columnNameRow.length; i++) {
                    columnNameMap.put(columnNameRow[i], new Integer(i));
                }
                continue;
            }
            
            String hitsColumn = getColumnValue(columnNameMap, thisRow, HIT_COLUMN);
            String [] hitStrengths = hitsColumn.split("[ \\t]*,[ \\t]*");
            
            System.err.println(hitsColumn);
            if (hitsColumn.equals("")) {
                continue;
            }
            
            System.err.println(hitStrengths[0] + "  - l: " + hitStrengths.length);
            
            Item amplicon = null;
            
            for (int hitStrengthIndex = 0; hitStrengthIndex < hitStrengths.length; hitStrengthIndex++) {
                String hitStrength = hitStrengths[hitStrengthIndex];
                
                // get columns using the column names from the header line
                String geneNameColumn = getColumnValue(columnNameMap, thisRow, FBGN_COLUMN);

                String [] geneNames = geneNameColumn.split("[ \\t]*,[ \\t]*");
                
                for (int geneIndex = 0; geneIndex < geneNames.length; geneIndex++) {
                    String geneName = geneNames[geneIndex];
                    int geneNameColonIndex = geneName.indexOf(':');
                    if (geneNameColonIndex != -1) {
                        geneName = geneName.substring(geneNameColonIndex + 1).trim();
                    }
                    Item gene = newGene(geneName);
                
                    Item screenHit = newItem("ScreenHit");
                    screenHit.setReference("screen", rnaiScreen);
                    screenHit.setReference("gene", gene);
                    String phenotype = getColumnValue(columnNameMap, thisRow, PHENOTYPE_COLUMN);
                    screenHit.setAttribute("phenotype", phenotype);
                    if (hitStrength != null && hitStrength.trim().length() > 0) {
                        screenHit.setAttribute("strength", hitStrength);
                    }
    
                    if (amplicon == null) {
                        amplicon = newItem("Amplicon");
                        String ampliconIdentifier =
                            getColumnValue(columnNameMap, thisRow, DRSC_AMPLICON_COLUMN);
                        String ampliconLength = 
                            getColumnValue(columnNameMap, thisRow, AMPLICON_LENGTH_COLUMN);
                        screenHit.setReference("amplicon", amplicon);
                        amplicon.setAttribute("identifier", ampliconIdentifier);
                        amplicon.setAttribute("length", ampliconLength);
                        amplicon.setReference("organism", organism);
                    }
    
                    writer.store(ItemHelper.convert(screenHit));
                }
            }
            
            writer.store(ItemHelper.convert(amplicon));
        }
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

                    System.err.println ("while reading " + getCurrentFile().getName()
                                        + " found: " + headerFieldName + " -> " + headerFieldValue);
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
                                       + row);
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
     * @param taxonId the Organism taxonId
     * @param synonym1 a synonym, or null
     * @param synonym2 another synonym, or null
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
            item.setAttribute("identifier", geneName);
            // identifier needs to be a Synonym for quick search to work
            newSynonym(geneName, item);
            item.setReference("organism", organism);
            genes.put(geneName, item);
        }
        return item;
    }

    /**
     * Convenience method to create and store a new synonym Item
     * @param synonymName the actual synonym
     * @param subject the synonym's subject item
     * @return a new synonym Item
     * @throws ObjectStoreException if an error occurs in storing the Utem
     */
    protected Item newSynonym(String synonymName, Item subject) throws ObjectStoreException {
        if (synonymName == null) {
            throw new RuntimeException("synonymName can't be null");
        }
        if (synonyms.containsKey(synonymName)) {
            return (Item) synonyms.get(synonymName);
        }
        Item item = newItem("Synonym");
        item.setAttribute("value", synonymName);
        item.setAttribute("type", "accession");
        item.setReference("subject", subject.getIdentifier());
        item.setReference("source", dataSource.getIdentifier());
        item.addToCollection("evidence", dataSet.getIdentifier());
        writer.store(ItemHelper.convert(item));
        synonyms.put(synonymName, item);
        return item;
    }

    /**
     * Convenience method for creating a new Item
     * @param className the name of the class
     * @return a new Item
     */
    protected Item newItem(String className) {
        Item item = new Item();
        item.setIdentifier(alias(className) + "_" + (id++));
        item.setClassName(GENOMIC_NS + className);
        item.setImplementations("");
        return item;
    }
    
    /**
     * @see FileConverter#close()
     */
    public void close() throws ObjectStoreException {
        store(genes.values());
        store(synonyms.values());
    }
}

