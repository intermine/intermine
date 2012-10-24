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
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

/**
 * Data converter to load FlyBase Identifiers from:
 * /micklem/data/flybase-identifiers/current/fb_synonym_fb_2012_04.tsv
 *
 * This converter has been used in masterMine build
 *
 * @author Fengyuan Hu
 */
public class FlyBaseIdentifiersConverter extends BioFileConverter
{
    private static final String DATASET_TITLE = "FlyBase";
    private static final String DATA_SOURCE_NAME = "FlyBase genes";

    private static final String GENE_PATTERN = "FBgn";
    private static final String FLY_TAXON = "7227";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public FlyBaseIdentifiersConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * Read each line from flat file, create genes and synonyms.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        // data is in format:
        // primary_FBid  current_symbol  current_fullname  fullname_synonym(s)  symbol_synonym(s)

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line.length < 5 || line[0].startsWith("#") || !line[0].startsWith(GENE_PATTERN)) {
                continue;
            }
            String primaryidentifier = line[0];
            String symbol = line[1];
            String name = line[2];

            // TODO create synonyms?

            Item gene = createItem("Gene");
            if (!StringUtils.isEmpty(primaryidentifier)) {
                gene.setAttribute("primaryIdentifier", primaryidentifier);

                if (!StringUtils.isEmpty(symbol)) {
                    gene.setAttribute("symbol", symbol);
                }
                if (!StringUtils.isEmpty(name)) {
                    gene.setAttribute("name", name);
                }
            }

            gene.setReference("organism", getOrganism(FLY_TAXON));
            store(gene);
        }
    }
}
