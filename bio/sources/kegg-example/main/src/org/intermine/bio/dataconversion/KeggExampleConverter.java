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
 *
 * @author Richard Smith
 */
public class KeggExampleConverter extends BioFileConverter
{
    protected static final Logger LOG = Logger.getLogger(KeggExampleConverter.class);

    protected HashMap<String, Item> pathwayMap = new HashMap<String, Item>();
    private String taxonId = null;
    private Item organism = null;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public KeggExampleConverter(ItemWriter writer, Model model) {
        super(writer, model, "GenomeNet", "KEGG PATHWAY");
    }

    /**
     * Set the taxon id to process.
     * @param taxonId the id
     */
    public void setTaxonId(String taxonId) {
        this.taxonId = taxonId;
    }

    /**
     * Called for each file found by ant.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        if (taxonId == null || taxonId.equals("")) {
            throw new IllegalArgumentException("No taxonId provided: " + taxonId);
        }

        // There are two files:
        //              map_title.tab - pathway ids and their names
        //      xxx_gene_map.tab - genes and the pathways they are involved in
        // The following code works out which file we are reading and calls the corresponding method
        File currentFile = getCurrentFile();

        if (currentFile.getName().equals("map_title.tab")) {
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
     * Process all rows of the xxx_gene_map.tab file
     * @param reader a reader for the xxx_gene_map.tab file
     * @throws IOException
     * @throws ObjectStoreException
     */
    private void processGeneMapFile(Reader reader) throws IOException, ObjectStoreException {
        // this file has data of the format:
        // gene id | pathway ids (space separated)

        Iterator lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        while (lineIter.hasNext()) {
            // line is a string array with the one element for each tab separated value
            // on the next line of the file
            String[] line = (String[]) lineIter.next();

            String geneId = line[0];

            // create a gene with this id as primaryIdentifier
            Item gene = createItem("Gene");
            gene.setAttribute("primaryIdentifier", geneId);
            gene.setReference("organism", getOrganism());

            // split the space separated list of pathway ids
            String[] pathwayIds = line[1].split(" ");

            // add each pathway to the Gene.pathways collection
            for (int i = 0; i < pathwayIds.length; i++) {
                String pathwayId = pathwayIds[i];
                // getPathway() will create a new pathway or fetch it from a map if already seen
                Item pathway = getPathway(pathwayId);
                gene.addToCollection("pathways", pathway);
            }

            // we have finished with this gene now so can store it
            store(gene);
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

    /**
     * Get an Item representing an organism, create and store it if called for the first time
     * @return an Item representing the organism
     * @throws ObjectStoreException
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
