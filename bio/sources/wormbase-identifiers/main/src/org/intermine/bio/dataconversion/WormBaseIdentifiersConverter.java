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
 * DataConverter to load WormBase gene identifiers from genes2molecular_names.txt.
 *
 * @author Richard Smith
 */
public class WormBaseIdentifiersConverter extends BioFileConverter
{
    protected Item worm;
    private static final String HEADER_LINE = "Gene WB ID";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
      */
    public WormBaseIdentifiersConverter(ItemWriter writer, Model model) {
        super(writer, model, "WormBase", "WormBase genes");
    }

    /**
     * Read each line from flat file, create genes and synonyms.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        Iterator<?> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        // data is in format:
        // primaryIdentifier | identifier | symbol

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line.length < 3 || line[0].startsWith("#") || line[0].startsWith(HEADER_LINE)) {
                continue;
            }
            String primaryidentifier = line[0];
            String symbol = line[1];
            String identifier = line[2];
            Item gene = createItem("Gene");
            if (!StringUtils.isEmpty(primaryidentifier)) {
                gene.setAttribute("primaryIdentifier", primaryidentifier);
            }
            if (!StringUtils.isEmpty(symbol)) {
                gene.setAttribute("symbol", symbol);
                // per Rachel.  We can't seem to get the gene names out of wormmart.
                gene.setAttribute("name", symbol);
            }
            if (!StringUtils.isEmpty(identifier)) {
                gene.setAttribute("secondaryIdentifier", identifier);
            }
            gene.setReference("organism", getOrganism("6239"));
            store(gene);
        }
    }
}
