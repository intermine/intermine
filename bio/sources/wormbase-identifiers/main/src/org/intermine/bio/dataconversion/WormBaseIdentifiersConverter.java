package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.dataconversion.DataConverter;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.TextFileUtil;
import org.intermine.xml.full.Item;


/**
 * DataConverter to load WormBase gene identifiers from genes2molecular_names.txt.
 *
 * @author Richard Smith
 */
public class WormBaseIdentifiersConverter extends FileConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";

    protected Item dataSource, worm, dataSet;
    protected Map ids = new HashMap();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws ObjectStoreException if an error occurs in storing
     * @throws MetaDataException if cannot generate model
     */
    public WormBaseIdentifiersConverter(ItemWriter writer, Model model)
        throws ObjectStoreException, MetaDataException {
        super(writer, model);

        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "WormBase");
        store(dataSource);

        dataSet = createItem("DataSet");
        dataSet.setAttribute("title", "WormBase genes");
        dataSet.setReference("dataSource", dataSource);
        store(dataSet);

        worm = createItem("Organism");
        worm.setAttribute("taxonId", "6239");
        store(worm);
    }


    /**
     * Read each line from flat file, create genes and synonyms.
     *
     * @see DataConverter#process
     */
    public void process(Reader reader) throws Exception {
        Iterator lineIter = TextFileUtil.parseTabDelimitedReader(reader);

        // data is in format:
        // organismDbId | identifier | symbol

        while (lineIter.hasNext()) {
            String[] line = (String[]) lineIter.next();

            if (line.length <= 1 || line[0].startsWith("#")) {
                continue;
            }

            if (line.length < 3) {
                throw new RuntimeException("Line does not have enough elements: "
                                           + Arrays.asList(line));
            }
            String organismdbid = line[0];
            String identifier = line[1];
            String symbol = line[2];
            List synonyms = new ArrayList();
            
            Item gene = createItem("Gene");
            if (organismdbid != null && !organismdbid.equals("")) {
                gene.setAttribute("organismDbId", organismdbid);
                synonyms.add(createSynonym(gene, "identifier", organismdbid));
            }
            if (identifier != null && !identifier.equals("")) {
                gene.setAttribute("identifier", identifier);
                synonyms.add(createSynonym(gene, "identifier", identifier));
            }
            if (symbol != null && !symbol.equals("")) {
                gene.setAttribute("symbol", symbol);
                synonyms.add(createSynonym(gene, "symbol", symbol));
            }

            gene.setReference("organism", worm.getIdentifier());
            gene.setCollection("evidence", 
                               new ArrayList(Collections.singleton(dataSet.getIdentifier())));
            store(gene);
            store(synonyms);

        }
    }

    private Item createSynonym(Item subject, String type, String value)
        throws ObjectStoreException {
        Item synonym = createItem("Synonym");
        synonym.setAttribute("type", type);
        synonym.setAttribute("value", value);
        synonym.setReference("source", dataSource.getIdentifier());
        synonym.setReference("subject", subject.getIdentifier());
        return synonym;
    }
}
