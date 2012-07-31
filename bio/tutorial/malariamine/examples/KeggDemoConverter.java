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


import java.io.File;
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
 * DataConverter to load Kegg Pathways and link them to Genes
 */
public class KeggDemoConverter extends BioFileConverter
{
    protected static final Logger LOG = Logger.getLogger(KeggDemoConverter.class);

    protected HashMap<String, Item> pathwayMap = new HashMap<String, Item>();
    private String taxonId = null;
    private Item organism = null;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public KeggDemoConverter(ItemWriter writer, Model model) {
        super(writer, model, "GenomeNet", "KEGG PATHWAY");
    }

    /**
     * Set the taxon id to process.
     * @param taxonId the id
     */
    public void setOrganism(String taxonId) {
        this.taxonId = taxonId;
    }
    
    /**
     * Called for each file found by ant.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        // the organism we are processing is set by the project.xml file with an entry like:
        //      <property name="organism" value="36329"/>
        if (taxonId == null || "".equals(taxonId)) {
            throw new IllegalArgumentException("No taxonId provided: " + taxonId);
        }

        // There are two files:
        //      map_title.tab - pathway ids and their names
        //      xxx_gene_map.tab - genes and the pathways they are involved in
        // The following code works out which file we are reading and calls the corresponding method
        File currentFile = getCurrentFile();

        if ("map_title.tab".equals(currentFile.getName())) {
                processMapTitleFile(reader);
        } else if (currentFile.getName().endsWith("gene_map.tab")) {
                processGeneMapFile(reader);
        } else {
                throw new IllegalArgumentException("Unexpected file: " + currentFile.getName());
        }
    }


    /**
     * Process all rows of the map_title.tab file
     * @param reader a reader for the map_title.tab file
     * @throws IOException
     * @throws ObjectStoreException
     */
    private void processMapTitleFile(Reader reader) throws IOException, ObjectStoreException {
        // this file has data of the format:
        // pathway id | pathway name

        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        while (lineIter.hasNext()) {
            // line is an array with one element for each tab separated value in the current line
            String[] line = (String[]) lineIter.next();
            
        }
    }

    /**
     * Process all rows of the xxx_gene_map.tab file
     * @param reader a reader for the xxx_gene_map.tab file
     * @throws IOException
     * @throws ObjectStoreException
     */
    private void processGeneMapFile(Reader reader) throws IOException, ObjectStoreException {
        // this file has data of the format:
        // gene id | pathway ids (space separated)

        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        while (lineIter.hasNext()) {
            // line is an array with one element for each tab separated value in the current line
            String[] line = (String[]) lineIter.next();
            
        }
    }

    
    /**
     * Fetch the organism item - create and store the item on the first time called.
     * @return an Item representing the organism
     */
    private Item getOrganism() throws ObjectStoreException {
        if (organism == null) {
            organism = createItem("Organism");
            organism.setAttribute("taxonId", taxonId);
            store(organism);
        }
        return organism;
    }
}
