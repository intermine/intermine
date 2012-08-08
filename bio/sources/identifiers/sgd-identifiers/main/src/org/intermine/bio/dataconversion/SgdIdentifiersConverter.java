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
 * Data converter to load SGD Identifiers from:
 * /micklem/data/sgd-identifiers/current/SGD_features.tab
 *
 * @author Fengyuan Hu
 */
public class SgdIdentifiersConverter extends BioFileConverter
{
    private static final String DATASET_TITLE = "SGD";
    private static final String DATA_SOURCE_NAME = "SGD genes";

    private static final String GENE_TYPE_ORF = "ORF";
    private static final String GENE_TYPE_TEG = "transposable_element_gene";
    private static final String YEAST_TAXON = "4932";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public SgdIdentifiersConverter(ItemWriter writer, Model model) {
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
//        1.   Primary SGDID (mandatory)
//        2.   Feature type (mandatory)
//        3.   Feature qualifier (optional)
//        4.   Feature name (optional)
//        5.   Standard gene name (optional)
//        6.   Alias (optional, multiples separated by |)
//        7.   Parent feature name (optional)
//        8.   Secondary SGDID (optional, multiples separated by |)
//        9.   Chromosome (optional)
//        10.  Start_coordinate (optional)
//        11.  Stop_coordinate (optional)
//        12.  Strand (optional)
//        13.  Genetic position (optional)
//        14.  Coordinate version (optional)
//        15.  Sequence version (optional)
//        16.  Description (optional)

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            String primaryidentifier = line[0];
            String featureType = line[1];
            String symbol = line[4];

            Item gene = createItem("Gene");
            if (featureType.equals(GENE_TYPE_ORF) || featureType.equals(GENE_TYPE_TEG)) {
                // TODO filter verified genes
                if (!StringUtils.isEmpty(primaryidentifier)) {
                    gene.setAttribute("primaryIdentifier", primaryidentifier);

                    if (!StringUtils.isEmpty(symbol)) {
                        gene.setAttribute("symbol", symbol);
                    }
                }
                gene.setReference("organism", getOrganism(YEAST_TAXON));
                store(gene);
            }
        }
    }
}
