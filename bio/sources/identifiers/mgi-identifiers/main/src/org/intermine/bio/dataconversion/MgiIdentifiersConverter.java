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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.util.FormattedTextParser;
import org.intermine.metadata.StringUtil;
import org.intermine.xml.full.Item;

/**
 *
 * @author Julie Sullivan
 */
public class MgiIdentifiersConverter extends BioFileConverter
{
    protected static final Logger LOG = Logger.getLogger(MgiIdentifiersConverter.class);
    private static final String DATASET_TITLE = "MGI identifiers";
    private static final String DATA_SOURCE_NAME = "MGI";
    private static final String NULL_STRING = "null";

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public MgiIdentifiersConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        @SuppressWarnings("rawtypes")
        Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        Item organism = createItem("Organism");
        organism.setAttribute("taxonId", "10090");
        store(organism);

        Set<String> identifiers = new HashSet<String>();

        // Read all lines into gene records
        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            String type = line[9];
            if (!"Gene".equals(type)) {
                continue;
            }

            String identifier = line[0];    // MGI
            String symbol = line[6];
            String name = line[8];
            String synonymsStr = line[11];

            if (StringUtils.isEmpty(identifier)) {
                throw new RuntimeException("null MGI identifier: " + symbol);
            }

            if (!identifier.equals(NULL_STRING) && identifiers.contains(identifier)) {
                throw new RuntimeException("duplicate MGI identifier");
            }

            if (!symbol.equals(NULL_STRING) && identifiers.contains(symbol)) {
                throw new RuntimeException("duplicate symbol " + symbol + " for gene "
                        + identifier);
            } else {
                identifiers.add(identifier);
                identifiers.add(symbol);
            }

            Item gene = createItem("Gene");
            gene.setReference("organism", organism);
            if (!NULL_STRING.equals(identifier)) {
                gene.setAttribute("primaryIdentifier", identifier);
            }
            if (!NULL_STRING.equals(symbol)) {
                gene.setAttribute("symbol", symbol);
            }
            if (!NULL_STRING.equals(name)) {
                gene.setAttribute("name", name);
            }

            if (synonymsStr != null && !synonymsStr.isEmpty()) {
                if (synonymsStr.contains("OTTMUSG") || synonymsStr.contains("ENSMUSG")) {
                    String[] synonyms = StringUtil.split(synonymsStr, "|");
                    for (String s : synonyms) {
                        if (s.startsWith("OTTMUSG")) {
                            createCrossReference(gene.getIdentifier(), s, "Vega", true);
                        }
                        if (s.startsWith("ENSMUSG")) {
                            createCrossReference(gene.getIdentifier(), s, "Ensembl", true);
                        }
                    }
                }

            }
            store(gene);
        }
    }
}
