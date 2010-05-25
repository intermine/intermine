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

import java.io.Reader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 * 
 * @author
 */
public class HgncConverter extends BioFileConverter
{
    protected static final Logger LOG = Logger.getLogger(HgncConverter.class);

    private static final String DATASET_TITLE = "Add DataSet.title here";
    private static final String DATA_SOURCE_NAME = "Add DataSource.name here";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public HgncConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * 
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        Item organism = createItem("Organism");
        organism.setAttribute("taxonId", "9606");
        store(organism);

        Set<String> symbols = new HashSet<String>();
        Set<String> seenEnsembl = new HashSet<String>();
        Set<String> seenEntrez = new HashSet<String>();

        Set<String> duplicateEntrez = new HashSet<String>();
        Set<String> duplicateEnsembl = new HashSet<String>();

        Set<GeneRecord> records = new HashSet<GeneRecord>();

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            String status = line[3];
            if (!status.equals("Approved")) {
                continue;
            }
            String symbol = line[1];
            String name = line[2];
            String entrez = line[9];
            String ensembl = line[10];
            if (symbols.contains(symbol)) {
                LOG.warn("HGNC seen same symbol already: " + symbol);
            } else {
                symbols.add(symbol);

                if (!StringUtils.isBlank(entrez)) {
                    if (seenEntrez.contains(entrez)) {
                        duplicateEntrez.add(entrez);
                    }
                }

                if (!StringUtils.isBlank(ensembl)) {
                    if (seenEnsembl.contains(ensembl)) {
                        duplicateEnsembl.add(ensembl);
                    }
                }
                seenEntrez.add(entrez);
                seenEnsembl.add(ensembl);

                GeneRecord record = new GeneRecord(symbol, name, entrez, ensembl);
                records.add(record);
            }
        }
        if (!duplicateEnsembl.isEmpty()) {
            LOG.warn("Found " + duplicateEnsembl.size() + " duplicate ensembl ids, not using "
                    + " duplicates for primaryIdentifier: " + duplicateEnsembl);
        }

        if (!duplicateEntrez.isEmpty()) {
            LOG.warn("Found " + duplicateEntrez.size() + " duplicate entrez ids, not using "
                    + " duplicates for ncbiGeneNumber: " + duplicateEntrez);
        }

        for (GeneRecord record : records) {
            Item gene = createItem("Gene");
            gene.setReference("organism", organism);
            gene.setAttribute("symbol", record.symbol);
            if (StringUtils.isBlank(record.name)) {
                LOG.warn("HGNC no name for symbol: " + record.name);
            } else {
                gene.setAttribute("name", record.name);
            }

            if (!StringUtils.isBlank(record.entrez)) {
                if (!duplicateEnsembl.contains(record.entrez)) {
                    gene.setAttribute("ncbiGeneNumber", record.entrez);
                }
            }
            if (!StringUtils.isBlank(record.ensembl)) {
                if (!duplicateEnsembl.contains(record.ensembl)) {
                    gene.setAttribute("primaryIdentifier", record.ensembl);
                }
            }
            store(gene);
        }
    }

    private class GeneRecord
    {
        protected String symbol;
        protected String name;
        protected String entrez;
        protected String ensembl;

        public GeneRecord(String symbol, String name, String entrez, String ensembl) {
            this.symbol = symbol;
            this.name = name;
            this.entrez = entrez;
            this.ensembl = ensembl;
        }
    }
}
