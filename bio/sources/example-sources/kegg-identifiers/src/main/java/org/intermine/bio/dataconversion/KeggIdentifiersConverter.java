package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 * DataConverter to load Kegg Pathways
 *
 * @author Julie Sullivan
 */
public class KeggIdentifiersConverter extends BioFileConverter
{
    protected static final Logger LOG = Logger.getLogger(KeggIdentifiersConverter.class);

    protected HashMap<String, Item> pathwayMap = new HashMap<String, Item>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public KeggIdentifiersConverter(ItemWriter writer, Model model) {
        super(writer, model, "GenomeNet", "KEGG PATHWAY", null);
    }

    /**
     * Called for each file found by ant.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        processMapTitleFile(reader);
    }

    /**
     * Process all rows of the map_title.tab file
     * @param reader a reader for the map_title.tab file
     * @throws IOException
     * @throws ObjectStoreException
     */
    private void processMapTitleFile(Reader reader) throws IOException, ObjectStoreException {
        Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        // this file has data of the format:
        // pathway id | pathway name
        while (lineIter.hasNext()) {
            // line is a string array with the one element for each tab separated value
            // on the next line of the file
            String[] line = (String[]) lineIter.next();

            String pathwayId = line [0];
            String pathwayName = line[1];

            // getPathway will create an Item or fetch it from a map if seen before
            Item pathway = getPathway(pathwayId);
            pathway.setAttribute("name", pathwayName);

            // once we have set the pathway name that is all the information needed so we can store
            store(pathway);
        }
    }

    /**
     * Create a new pathway Item or fetch from a map if it has been seen before
     * @param pathwayId the id of a KEGG pathway to look up
     * @return an Item representing the pathway
     */
    private Item getPathway(String pathwayId) {
        Item pathway = pathwayMap.get(pathwayId);
        if (pathway == null) {
            pathway = createItem("Pathway");
            pathway.setAttribute("identifier", pathwayId);
            pathwayMap.put(pathwayId, pathway);
        }
        return pathway;
    }
}
