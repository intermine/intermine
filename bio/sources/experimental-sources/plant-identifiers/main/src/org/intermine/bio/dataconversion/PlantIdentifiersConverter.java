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

import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 * Read file exported from EnsemblGenomes BioMart containing A. thaliana gene identifiers, symbols,
 * types and NCBI gene ids.
 * @author Richard Smith
 */
public class PlantIdentifiersConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "A. thaliana gene identifiers";
    private static final String DATA_SOURCE_NAME = "A. thaliana gene identifiers";
    private static final String PLANT_TAXON = "3702";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public PlantIdentifiersConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

        Map<String, GeneRecord> records = new HashMap<String, GeneRecord>();
        Set<String> seenEntrez = new HashSet<String>();
        Set<String> duplicateEntrez = new HashSet<String>();
        Set<String> seenSymbols = new HashSet<String>();
        Set<String> duplicateSymbols = new HashSet<String>();


        // Read all lines into id pairs, track any ensembl ids or symbols that appear twice
        Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        // remove header line
        lineIter.next();

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            String identifier = line[0];
            String symbol = line[1];
            String entrez = line[2];

            if (entrez != null) {
                if (seenEntrez.contains(entrez)) {
                    duplicateEntrez.add(entrez);
                } else {
                    seenEntrez.add(entrez);
                }
            }
            if (seenSymbols.contains(symbol)) {
                duplicateSymbols.add(symbol);
            } else {
                seenSymbols.add(symbol);
            }

            GeneRecord record = records.get(identifier);
            if (record == null) {
                record = new GeneRecord(identifier, symbol, entrez);
                records.put(identifier, record);
            } else {
                if (!symbol.equals(record.symbol)) {
                    record.synonyms.add(symbol);
                    record.synonyms.add(record.symbol);
                    record.symbol = null;
                }

                // if this entrez is null (maybe both) or they are the same do nothing
                if (entrez != null && !entrez.equals(record.entrez)) {
                    record.synonyms.add(entrez);
                    record.synonyms.add(record.entrez);
                    record.entrez = null;
                }
            }
        }
        for (GeneRecord record : records.values()) {
            Item gene = createItem("Gene");
            gene.setAttribute("primaryIdentifier", record.identifier);
            gene.setReference("organism", getOrganism(PLANT_TAXON));
            if (record.entrez != null) {
                if (duplicateEntrez.contains(record.entrez)) {
                    record.synonyms.add(record.entrez);
                } else {
                    gene.setAttribute("secondaryIdentifier", record.entrez);
                    gene.setAttribute("ncbiGeneNumber", record.entrez);
                }
            }
            if (duplicateSymbols.contains(record.symbol)) {
                record.synonyms.add(record.symbol);
            } else {
                gene.setAttribute("symbol", record.symbol);
            }

            for (String synonym : record.synonyms) {
                createSynonym(gene, synonym, true);
            }
            store(gene);
        }
    }

    private class GeneRecord
    {
        String identifier, symbol, entrez, bioType;
        Set<String> synonyms = new HashSet<String>();

        public GeneRecord(String identifier, String symbol, String entrez) {
            this.identifier = identifier;
            this.symbol = symbol;
            this.entrez = entrez;
        }
    }
}
