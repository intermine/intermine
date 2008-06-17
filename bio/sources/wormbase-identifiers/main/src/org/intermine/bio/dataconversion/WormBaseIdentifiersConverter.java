package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
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

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws ObjectStoreException if an error occurs in storing
     */
    public WormBaseIdentifiersConverter(ItemWriter writer, Model model)
        throws ObjectStoreException {
        super(writer, model, "WormBase", "WormBase genes");

        worm = createItem("Organism");
        worm.setAttribute("taxonId", "6239");
        store(worm);
    }


    /**
     * Read each line from flat file, create genes and synonyms.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        // data is in format:
        // primaryIdentifier | identifier | symbol

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line.length != 3 || line[0].startsWith("#")) {
                continue;
            }

//            if (line.length < 3) {
//                throw new RuntimeException("Line does not have enough elements: "
//                                           + Arrays.asList(line));
//            }
            String primaryidentifier = line[0];
            String identifier = line[1];
            String symbol = line[2];
            List<Item> synonyms = new ArrayList<Item>();

            Item gene = createItem("Gene");
            if (primaryidentifier != null && !primaryidentifier.equals("")) {
                gene.setAttribute("primaryIdentifier", primaryidentifier);
                synonyms.add(createSynonym(gene, "identifier", primaryidentifier));
            }
            if (identifier != null && !identifier.equals("")) {
                gene.setAttribute("secondaryIdentifier", identifier);
                synonyms.add(createSynonym(gene, "identifier", identifier));
            }
            if (symbol != null && !symbol.equals("")) {
                gene.setAttribute("symbol", symbol);
                synonyms.add(createSynonym(gene, "symbol", symbol));
            }

            gene.setReference("organism", worm.getIdentifier());

            store(gene);
            store(synonyms);

        }
    }

    private Item createSynonym(Item subject, String type, String value) {
        Item synonym = createItem("Synonym");
        synonym.setAttribute("type", type);
        synonym.setAttribute("value", value);
        synonym.setReference("subject", subject.getIdentifier());
        return synonym;
    }
}
