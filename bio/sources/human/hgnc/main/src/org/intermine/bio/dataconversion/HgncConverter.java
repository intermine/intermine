package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 * Converter to parse data file from HGNC.
 * ftp://ftp.ebi.ac.uk/pub/databases/genenames/new/tsv/hgnc_complete_set.txt
 *
 * @author Julie Sullivan
 */
public class HgncConverter extends BioFileConverter
{
    private static final String DATASET_TITLE = "HGNC identifiers";
    private static final String DATA_SOURCE_NAME = "HGNC";
    private Map<String, String> genes = new HashMap<String, String>();
    private static final String TAXON_ID = "9606";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public HgncConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * Read HGNC TSV file.
     *
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader) throws Exception {
        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        // skip header
        lineIter.next();

        // each gene is on a new line
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            String entrezId = line[18];

            if (StringUtils.isEmpty(entrezId)) {
                // we are only interested in genes that have an NCBI id.
                continue;
            }

            String symbol = line[1];
            String ensemblGeneId = line[19];

            String geneRefId = getGeneId(entrezId, symbol, ensemblGeneId);

            String hgncId = line[0];
            createSynonym(geneRefId, hgncId);

            String aliasSymbol = line[8];
            createSynonym(geneRefId, aliasSymbol);

            String aliasName = line[9];
            createSynonym(geneRefId, aliasName);

            String prevSymbol = line[10];
            createSynonym(geneRefId, prevSymbol);

            String prevName = line[11];
            createSynonym(geneRefId, prevName);

            String vegaId = line[20];
            createSynonym(geneRefId, vegaId);

            String ucscId = line[21];
            createSynonym(geneRefId, ucscId);

            String ena = line[22];
            createSynonym(geneRefId, ena);

            String refseqAccession = line[23];
            createSynonym(geneRefId, refseqAccession);

        }
    }

    private void createSynonym(String geneRefId, String value) throws ObjectStoreException {
        value = value.replace("\"", "");
        String[] bits = value.split("\\|");
        for (String syn : bits) {
            createSynonym(geneRefId, syn, true);
        }
    }

    private String getGeneId(String entrezId, String symbol, String ensembl)
        throws ObjectStoreException {
        String geneId = genes.get(entrezId);
        if (geneId == null) {
            Item gene = createItem("Gene");
            gene.setAttribute("primaryIdentifier", entrezId);
            if (StringUtils.isNotEmpty(ensembl)) {
                gene.setAttribute("secondaryIdentifier", ensembl);
            }
            if (StringUtils.isNotEmpty(symbol)) {
                gene.setAttribute("symbol", symbol);
            }
            gene.setReference("organism", getOrganism(TAXON_ID));
            store(gene);
            geneId = gene.getIdentifier();
            genes.put(entrezId, geneId);
        }
        return geneId;
    }
}
