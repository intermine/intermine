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
 *
 * @author Fengyuan Hu
 */
public class ZfinIdentifiersConverter extends BioFileConverter
{
    private static final String DATASET_TITLE = "ZFIN";
    private static final String DATA_SOURCE_NAME = "ZFIN genes";

    private static final String GENE_PATTERN = "ZDB-GENE";
    private static final String FISH_TAXON = "7955";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public ZfinIdentifiersConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     *
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {


        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        // data is in format:
        // ZDBID  SYMBOL  Ensembl(Zv9)

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line.length < 3 || line[0].startsWith("#") || !line[0].startsWith(GENE_PATTERN)) {
                continue;
            }
            String primaryidentifier = line[0];
            String symbol = line[1];
            String secondaryIdentifier = line[2];

            Item gene = createItem("Gene");
            if (!StringUtils.isEmpty(primaryidentifier)) {
                gene.setAttribute("primaryIdentifier", primaryidentifier);

                if (!StringUtils.isEmpty(symbol)) {
                    gene.setAttribute("symbol", symbol);
                }
                if (!StringUtils.isEmpty(secondaryIdentifier)) {
                    gene.setAttribute("secondaryIdentifier", secondaryIdentifier);
                }
            }

            gene.setReference("organism", getOrganism(FISH_TAXON));
            store(gene);
        }
    }
}
