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
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 *
 * @author
 */
public class RgdIdentifiersConverter extends BioFileConverter
{
    //
    private static final String DATASET_TITLE = "Add DataSet.title here";
    private static final String DATA_SOURCE_NAME = "Add DataSource.name here";

    private static final String RAT_TAXON = "10116";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public RgdIdentifiersConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     *
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        // Read all lines into id pairs, track any ensembl ids or symbols that appear twice
        Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();
            String rgdId = line[0];
            String symbol = line[1];
            String name = line[2];
            String description = line[3];
            String entrez = line[20];
            String ensembl = line[37];

            Item gene = createItem("Gene");
            gene.setReference("organism", getOrganism(RAT_TAXON));
            gene.setAttribute("secondaryIdentifier", rgdId);
            gene.setAttribute("symbol", symbol);
            if (!StringUtils.isBlank(name)) {
                gene.setAttribute("name", name);
            }
            if (!StringUtils.isBlank(description)) {
                gene.setAttribute("summary", description);
            }
            if (!StringUtils.isBlank(entrez)) {
                gene.setAttribute("ncbiGeneNumber", entrez);
            }
            if (!StringUtils.isBlank(ensembl)) {
                gene.setAttribute("primaryIdentifier", ensembl);
            }
            store(gene);
        }
    }
}
