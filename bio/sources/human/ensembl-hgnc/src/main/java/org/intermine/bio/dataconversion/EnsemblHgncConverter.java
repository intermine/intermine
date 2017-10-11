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
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

/**
 * Read two identifier files fetched from Ensembl's BioMart. These map from Ensembl id to HGNC
 * official symbol and Ensembl id to Entrez id.  An Entrez id is only set for genes that don't have
 * an HGNC symbol.
 * @author Richard Smith
 */
public class EnsemblHgncConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "Ensembl HGNC mapping";
    private static final String DATA_SOURCE_NAME = "Ensembl";
    private static final String HUMAN_TAXON_ID = "9606";
    private Map<String, Item> genes = new HashMap<String, Item>();
    Map<String, Set<String>> ensemblEntrezIds = new HashMap<String, Set<String>>();

    protected static final Logger LOG = Logger.getLogger(EnsemblHgncConverter.class);

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public EnsemblHgncConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    @Override
    public void close() throws Exception {
        setEntrezGeneIds();

        for (Item gene : genes.values()) {
            store(gene);
        }
        super.close();

    }

    // for any genes that don't have a symbol we need to set the Entrez gene id.  Those with a
    // symbol will get their Entrez id from the NCBI gene_info file
    private void setEntrezGeneIds() {
        for (String ensembl : ensemblEntrezIds.keySet()) {
            if (!genes.containsKey(ensembl)) {
                Set<String> entrezs = ensemblEntrezIds.get(ensembl);
                if (entrezs.size() == 1) {
                    Item gene = getGene("primaryIdentifier", ensembl);
                    String entrez = entrezs.iterator().next();
                    gene.setAttribute("ncbiGeneNumber", entrez);
                    gene.setAttribute("secondaryIdentifier", entrez);
                    genes.put(ensembl, gene);

                }
            }
        }
    }

    /**
     * Read in ensembl ids symbols and entrez ids for genes without symbols.
     *
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws Exception {
        File currentFile = getCurrentFile();
        if (currentFile.getName().startsWith("ensembl_hgnc")) {
            processHgncSymbols(reader);
        } else if (currentFile.getName().startsWith("ensembl_entrez"))  {
            //processEntrezIds(reader);
        }
        else {
            throw new RuntimeException("Don't know how to process file: " + currentFile.getName());
        }
    }

    private void processHgncSymbols(Reader reader) throws Exception {
        Set<String> chrs = getChromosomes();

        Set<IdPair> ids = new HashSet<IdPair>();
        Set<String> symbols = new HashSet<String>();
        Set<String> ensembls = new HashSet<String>();
        Set<String> duplicateSymbols = new HashSet<String>();
        Set<String> duplicateEnsembls = new HashSet<String>();

        // Read all lines into id pairs, track any ensembl ids or symbols that appear twice
        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            String ensembl = line[0];
            String chr = line[1];
            String symbol = line[2];

            if (!StringUtils.isBlank(symbol) && chrs.contains(chr)) {

                IdPair idPair = new IdPair(ensembl, symbol);
                if (ids.contains(idPair)) {
                    // same pair appears again because file isn't unique
                    continue;
                }
                ids.add(idPair);

                if (symbols.contains(symbol)) {
                    duplicateSymbols.add(symbol);
                } else {
                    symbols.add(symbol);
                }

                if (ensembls.contains(ensembl)) {
                    duplicateEnsembls.add(ensembl);
                } else {
                    ensembls.add(ensembl);
                }
            }
        }

        LOG.info("Duplicate symbols: " + duplicateSymbols.size() + ", " + duplicateSymbols);
        LOG.info("Duplicate ensembls: " + duplicateEnsembls.size() + ", " + duplicateEnsembls);
        Map<String, Set<String>> ensemblSynonymsMap = new HashMap<String, Set<String>>();
        Map<String, Set<String>> symbolSynonymsMap = new HashMap<String, Set<String>>();

        for (IdPair idPair : ids) {
            if (!duplicateEnsembls.contains(idPair.ensembl)
                    && !duplicateSymbols.contains(idPair.symbol)) {
                // Single mapping, create gene with ensembl id and symbol
                Item gene = getGene("primaryIdentifier", idPair.ensembl);
                gene.setAttribute("symbol", idPair.symbol);
            } else {
                if (duplicateSymbols.contains(idPair.symbol)) {
                    addToMapOfSets(symbolSynonymsMap, idPair.symbol, idPair.ensembl);
                    addToMapOfSets(ensemblSynonymsMap, idPair.ensembl, idPair.symbol);
                }
                if (duplicateEnsembls.contains(idPair.ensembl)) {
                    addToMapOfSets(ensemblSynonymsMap, idPair.ensembl, idPair.symbol);
                    addToMapOfSets(symbolSynonymsMap, idPair.symbol, idPair.ensembl);
                }
            }
        }

        // store duplicates with either ensembl id or symbol set and create others as synonyms
        storeSynonyms(symbolSynonymsMap, "symbol");
        storeSynonyms(ensemblSynonymsMap, "primaryIdentifier");
    }

    private Item getGene(String keyAttribute, String key) {
        Item gene = genes.get(key);
        if (gene == null) {
            gene = createItem("Gene");
            gene.setReference("organism", getOrganism(HUMAN_TAXON_ID));
            gene.setAttribute(keyAttribute, key);
            genes.put(key, gene);
        }
        return gene;
    }

    private void storeSynonyms(Map<String, Set<String>> synonymMap, String keyAttribute)
        throws ObjectStoreException {
        for (String key : synonymMap.keySet()) {
            Item gene = getGene(keyAttribute, key);
            for (String synonym : synonymMap.get(key)) {
                createSynonym(gene, synonym, true);
            }
        }
    }

    private static void addToMapOfSets(Map<String, Set<String>> map, String key, String value) {
        Set<String> values = map.get(key);
        if (values == null) {
            values = new HashSet<String>();
            map.put(key, values);
        }
        values.add(value);
    }

    private static Set<String> getChromosomes() {
        Set<String> chrs = new HashSet<String>();
        chrs.add("X");
        chrs.add("Y");
        chrs.add("MT");
        for (int i = 1; i <= 22; i++) {
            chrs.add("" + i);
        }
        return chrs;
    }

    private class IdPair
    {
        private String ensembl;
        private String symbol;

        public IdPair(String ensembl, String symbol) {
            this.ensembl = ensembl;
            this.symbol = symbol;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof IdPair) {
                IdPair other = (IdPair) obj;
                return this.ensembl.equals(other.ensembl) && this.symbol.equals(other.symbol);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 3 * ensembl.hashCode() + 7 * symbol.hashCode();
        }

        @Override
        public String toString() {
            return ensembl + ", " + symbol;
        }
    }
}
