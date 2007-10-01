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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.dataconversion.DBConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.sql.Database;
import org.intermine.xml.full.Item;

/**
 * A DBConverter with helper methods for bio sources.
 * @author Kim Rutherford
 */
public abstract class BioDBConverter extends DBConverter
{
    private Map<String, Item> chromosomes = new HashMap<String, Item>();
    private Map<String, Item> organisms = new HashMap<String, Item>();
    private Map<String, Item> dataSets = new HashMap<String, Item>();
    private Map<String, Item> dataSources = new HashMap<String, Item>();

    /**
     * Create a new BioDBConverter object.
     * @param database the database to read from
     * @param tgtModel the Model used by the object store we will write to with the ItemWriter
     * @param writer an ItemWriter used to handle the resultant Items
     */
    public BioDBConverter(Database database, Model tgtModel, ItemWriter writer) {
        super(database, tgtModel, writer);
    }

    /**
     * Make a Location Relation between a LocatedSequenceFeature and a Chromosome.
     * @param chromosomeIdentifier Chromosome identifier
     * @param locatedSequenceFeatureId the Item idenitifier of the feature
     * @param start the start position
     * @param end the end position
     * @param strand the strand
     * @param taxonId the taxon id to use when finding the Chromosome for the Location
     * @param dataSet the DataSet to put in the evidence collection of the new Location
     * @return the new Location object
     * @throws ObjectStoreException if an Item can't be stored
     */
    protected Item makeLocation(String chromosomeIdentifier, String locatedSequenceFeatureId,
                                int start, int end, int strand, int taxonId, Item dataSet)
        throws ObjectStoreException {
        Item chromosome = getChromosome(chromosomeIdentifier, taxonId);
        Item location = createItem("Location");

        if (start < end) {
            location.setAttribute("start", String.valueOf(start));
            location.setAttribute("end", String.valueOf(end));
        } else {
            location.setAttribute("start", String.valueOf(end));
            location.setAttribute("end", String.valueOf(start));
        }
        location.setAttribute("strand", String.valueOf(strand));
        location.setReference("object", chromosome);
        location.setReference("subject", locatedSequenceFeatureId);
        location.addToCollection("evidence", dataSet);
        store(location);
        return location;
    }

    /**
     * The Organism item created from the taxon id passed to the constructor.
     * @return the Organism Item
     */
    public Item getOrganismItem(int taxonId) {
        String taxonString = String.valueOf(taxonId);
        Item organism = organisms.get(taxonString);
        if (organism == null) {
            organism = createItem("Organism");
            organism.setAttribute("taxonId", taxonString);
            try {
                store(organism);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("failed to store organism with taxonId: " + taxonId, e);
            }
            organisms.put(taxonString, organism);
        }
        return organism;
    }

    /**
     * Return a DataSet item for the given title
     * @param name the DataSet name
     * @return the DataSet Item
     */
    public Item getDataSourceItem(String name) {
        Item dataSource = dataSources.get(name);
        if (dataSource == null) {
            dataSource = createItem("DataSource");
            dataSource.setAttribute("name", name);
            try {
                store(dataSource);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("failed to store DataSource with name: " + name, e);
            }
            dataSources.put(name, dataSource);
        }
        return dataSource;
    }

    /**
     * Return a DataSource item for the given name
     * @param title the DataSet title
     * @return the DataSet Item
     */
    public Item getDataSetItem(String title) {
        Item dataSet = dataSets.get(title);
        if (dataSet == null) {
            dataSet = createItem("DataSet");
            dataSet.setAttribute("title", title);
            try {
                store(dataSet);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("failed to store DataSet with title: " + title, e);
            }
            dataSets.put(title, dataSet);
        }
        return dataSet;
    }

    private Item getChromosome(String identifier, int taxonId) throws ObjectStoreException {
        Item chromosome = chromosomes.get(identifier);
        if (chromosome == null) {
            chromosome = createItem("Chromosome");
            chromosome.setAttribute("identifier", identifier);
            chromosome.setReference("organism", getOrganismItem(taxonId));
            chromosomes.put(identifier, chromosome);
            store(chromosome);
        }
        return chromosome;
    }

    /**
     * Create and return a new Synonym, but don't store it.
     * @param subject the Synonym subject id
     * @param type the Synonym type
     * @param value the Synonym value
     * @param evidence the Synonym evidence (eg. a DataSet)
     * @param dataSource the source of this synonym
     * @return the new Synonym
     * @throws ObjectStoreException if there is a problem while storing
     */
    public Item createSynonym(String subjectId, String type, String value, boolean isPrimary,
                              List<Item> evidence, Item dataSource) throws ObjectStoreException {
        Item synonym = createItem("Synonym");
        synonym.setAttribute("type", type);
        synonym.setAttribute("value", value);
        synonym.setAttribute("isPrimary", String.valueOf(isPrimary));
        synonym.setReference("subject", subjectId);
        synonym.setReference("source", dataSource);
        for (Item evidenceItem: evidence) {
            synonym.addToCollection("evidence", evidenceItem);
        }
        store(synonym);
        return synonym;
    }
}
