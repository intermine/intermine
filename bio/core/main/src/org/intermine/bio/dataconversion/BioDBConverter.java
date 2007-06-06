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
import java.util.Map;

import org.intermine.dataconversion.DBConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.sql.Database;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemHelper;

/**
 * A DBConverter with helper methods for bio sources.
 * @author Kim Rutherford
 */
public abstract class BioDBConverter extends DBConverter
{
    private Map<String, Item> chromosomes = new HashMap<String, Item>();
    private Item organism;
    
    // the DataSet Item for the evidence collection of the new Location
    private final Item dataSet;
    // the DataSource for the Synonym objects
    private final Item dataSource;

    /**
     * Create a new BioDBConverter object.
     * @param database the database to read from
     * @param tgtModel the Model used by the object store we will write to with the ItemWriter
     * @param writer an ItemWriter used to handle the resultant Items
     * @param taxonId organism taxon id to use to create the Organism object for BioEntitys
     * @param dataSetTitle the title attribute to user when creating the DataSet item
     * @param dataSourceName the name for the DataSource for this conversion
     * @throws ObjectStoreException thrown if ItemWriter.store() fails
     */
    public BioDBConverter(Database database, Model tgtModel, ItemWriter writer, int taxonId,
                          String dataSetTitle, String dataSourceName) 
        throws ObjectStoreException {
        super(database, tgtModel, writer);
        dataSource = makeItem("DataSource");
        dataSource.setAttribute("name", dataSourceName);
        writer.store(ItemHelper.convert(dataSource));
        organism = makeItem("Organism");
        organism.setAttribute("taxonId", String.valueOf(taxonId));
        writer.store(ItemHelper.convert(organism));
        dataSet = makeItem("DataSet");
        dataSet.setAttribute("title", dataSetTitle);
        writer.store(ItemHelper.convert(dataSet));
    }

    /**
     * Make a Location Relation between a LocatedSequenceFeature and a Chromosome.
     * @param chromosomeIdentifier Chromosome identifier
     * @param locatedSequenceFeature the feature
     * @param start the start position
     * @param end the end position
     * @param strand the strand
     * @return the new Location object
     * @throws ObjectStoreException if an Item can't be stored
     */
    protected Item makeLocation(String chromosomeIdentifier, Item locatedSequenceFeature,
                                int start, int end, int strand) throws ObjectStoreException {
        Item chromosome = getChromosome(chromosomeIdentifier);
        Item location = makeItem("Location");
        
        if (start < end) {
            location.setAttribute("start", String.valueOf(start));
            location.setAttribute("end", String.valueOf(end));
        } else {
            location.setAttribute("start", String.valueOf(end));
            location.setAttribute("end", String.valueOf(start));
        }
        location.addAttribute(new Attribute("strand", String.valueOf(strand)));
        location.setReference("object", chromosome);
        location.setReference("subject", locatedSequenceFeature);
        location.addToCollection("evidence", dataSet);
        getItemWriter().store(ItemHelper.convert(location));
        return location;
    }

    /**
     * The Organism item created from the taxon id passed to the constructor.  
     * @return the Organism Item
     */
    public Item getOrganism() {
        return organism;
    }
    
    /**
     * The DataSet item created from the dataset title passed to the constructor.  
     * @return the DataSet Item
     */
    public Item getDataSet() {
        return dataSet;
    }

    private Item getChromosome(String identifier) throws ObjectStoreException {
        Item chromosome = chromosomes.get(identifier);
        if (chromosome == null) {
            chromosome = makeItem("Chromosome");
            chromosome.setAttribute("identifier", identifier);
            chromosome.setReference("organism", getOrganism());
            chromosomes.put(identifier, chromosome);
            getItemWriter().store(ItemHelper.convert(chromosome));
        }
        return chromosome;
    }

    /**
     * Create and return a new Synonym, but don't store it.
     * @param subject the Synonym subject
     * @param type the Synonym type
     * @param value the Synonym value
     * @param evidence the Synonym evidence (eg. a DataSet)
     * @return the new Synonym
     */
    public Item createSynonym(Item subject, String type, String value, boolean isPrimary,
                              Item evidence) {
        Item synonym = makeItem("Synonym");
        synonym.setAttribute("type", type);
        synonym.setAttribute("value", value);
        synonym.setAttribute("isPrimary", String.valueOf(isPrimary));
        synonym.setReference("subject", subject);
        synonym.setReference("source", dataSource);
        synonym.addToCollection("evidence", evidence);
        return synonym;
    }
}
