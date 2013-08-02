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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Item;

/**
 * Human gene info converter
 *
 * @author Fengyuan Hu
 */
public class HumanGeneConverter extends BioFileConverter
{
    private static final String DATASET_TITLE = "Human gene information";
    private static final String DATA_SOURCE_NAME = "HGNC";
    private static final String HUMAN_TAXONID = "9606";
    private static final String HGNC_PREFIX = "HGNC:";
    private static final String NCBI_PREFIX = "Entrez Gene:";

    protected static final Logger LOG = Logger.getLogger(HumanGeneConverter.class);

    private List<String> symboldupEnsemblIdList = Arrays.asList("MIR3150A", "MIR4776-1");

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public HumanGeneConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * Read the HGNC human info file and create genes setting identifiers, organism and synonyms.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        @SuppressWarnings("rawtypes")
        Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            // use Ensembl is as pid, if empty, use HGNC id
            String symbol = line[0];
            String hgncid = HGNC_PREFIX + line[1];
            String entrezid = line[2];
            String ensemblid = line[3];
            String name = line[5];
            String prevsymbols = line[6];
            String prevnames = line[7];
            String namealiases = line[8];
            String aliases = line[9];
            String maploc = line[10];

            Item gene = createItem("Gene");
            gene.setReference("organism", getOrganism(HUMAN_TAXONID));
            createCrossReference(gene.getIdentifier(), hgncid, "HGNC", true);

            if (!entrezid.isEmpty()) {
                createCrossReference(gene.getIdentifier(), NCBI_PREFIX + entrezid, "NCBI", true);
            }

            if (!ensemblid.isEmpty()) {
                gene.setAttribute("primaryIdentifier", ensemblid);
            } else {
                gene.setAttribute("primaryIdentifier", hgncid);
            }

            // HACK: in HGNC, MIR3150A and MIR3150B are mapped to the same gene in Ensembl
            // ENSG00000265256, but in Ensembl, MIR3150A is resolved as MIR3150B. And there are more
            if (symboldupEnsemblIdList.contains(symbol)) {
                gene.setAttribute("primaryIdentifier", hgncid);
                createCrossReference(gene.getIdentifier(), ensemblid, "Ensembl", true);
            }
            // END of Hack

            gene.setAttribute("symbol", symbol);

            if (!name.isEmpty()) {
                gene.setAttribute("name", name);
            }

            if (!prevsymbols.isEmpty()) {
                String[] prevsymbolsArr = StringUtil.split(prevsymbols, ",");
                for (String prevsym : prevsymbolsArr) {
                    createSynonym(gene, prevsym.trim().replaceAll("^\"|\"$", ""), true);
                }
            }

            if (!prevnames.isEmpty()) {
                String[] prevnamesArr = StringUtil.split(prevnames, ",");
                for (String prevname : prevnamesArr) {
                    createSynonym(gene, prevname.trim().replaceAll("^\"|\"$", ""), true);
                }
            }

            if (!namealiases.isEmpty()) {
                String[] namealiasesArr = StringUtil.split(namealiases, ",");
                for (String namealiase : namealiasesArr) {
                    createSynonym(gene, namealiase.trim().replaceAll("^\"|\"$", ""), true);
                }
            }

            if (!aliases.isEmpty()) {
                String[] aliasesArr = StringUtil.split(aliases, ",");
                for (String aliase : aliasesArr) {
                    createSynonym(gene, aliase.trim().replaceAll("^\"|\"$", ""), true);
                }
            }

            if (!maploc.isEmpty()) {
                gene.setAttribute("cytoLocation", maploc);
            }

            store(gene);
        }
    }
}
