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
    private final Map<String, Item> chromosomes = new HashMap<String, Item>();
    private final Map<String, Item> organisms = new HashMap<String, Item>();
    private final Map<String, Item> dataSets = new HashMap<String, Item>();
    private final Map<String, Item> dataSources = new HashMap<String, Item>();
    private String dataSourceName = null;

    /**
     * Create a new BioDBConverter object.  The constructor will automatically create a
     * DataConverterStoreHook for this converter that sets DataSet references and collections.
     * @param database the database to read from
     * @param tgtModel the Model used by the object store we will write to with the ItemWriter
     * @param writer an ItemWriter used to handle the resultant Items
     * @param dataSourceName the DataSource name
     * @param dataSetTitle the DataSet title
     */
    public BioDBConverter(Database database, Model tgtModel, ItemWriter writer,
                          String dataSourceName, String dataSetTitle) {
        super(database, tgtModel, writer);
        Item dataSource = getDataSourceItem(dataSourceName);
        Item dataSet = getDataSetItem(dataSetTitle, dataSource);
        DataSetStoreHook hook = new DataSetStoreHook(tgtModel, dataSet, dataSource);
        setStoreHook(hook);
    }

    /**
     * Create a new BioDBConverter object.  The caller should call setStoreHook() before
     * processing starts.
     * @param database the database to read from
     * @param tgtModel the Model used by the object store we will write to with the ItemWriter
     * @param writer an ItemWriter used to handle the resultant Items
     */
    public BioDBConverter(Database database, Model tgtModel, ItemWriter writer) {
        super(database, tgtModel, writer);
    }

    /**
     * Set the name of the DataSource Item to create for this converter.
     * @param name the name
     */
    public void setDataSourceName(String name) {
        this.dataSourceName = name;
    }

    /**
     * Return the data source name set by setDataSourceName().
     * @return the data source name
     */
    public String getDataSourceName() {
        return dataSourceName;
    }

    /**
     * Return the DataSet Item created from the dataSetTitle.
     * @param taxonId the taxon id of the to use when creating the DataSet
     * @return the DataSet Item
     */
    public Item getDataSetItem(int taxonId) {
        return getDataSetItem(getDataSetTitle(taxonId), getDataSourceItem());
    }

    /**
     * Return the DataSource Item created from the dataSourceName.
     * @return the DataSource Item
     */
    public Item getDataSourceItem() {
        return getDataSourceItem(dataSourceName);
    }

    /**
     * Make a Location Relation between a LocatedSequenceFeature and a Chromosome.
     * @param chromosomeId Chromosome Item identifier
     * @param locatedSequenceFeatureId the Item identifier of the feature
     * @param start the start position
     * @param end the end position
     * @param strand the strand
     * @param taxonId the taxon id to use when finding the Chromosome for the Location
     * @return the new Location object
     */
    protected Item makeLocation(String chromosomeId, String locatedSequenceFeatureId,
                                int start, int end, int strand, int taxonId) {
        Item location = createItem("Location");

        if (start < end) {
            location.setAttribute("start", String.valueOf(start));
            location.setAttribute("end", String.valueOf(end));
        } else {
            location.setAttribute("start", String.valueOf(end));
            location.setAttribute("end", String.valueOf(start));
        }
        location.setAttribute("strand", String.valueOf(strand));
        location.setReference("object", chromosomeId);
        location.setReference("subject", locatedSequenceFeatureId);
        return location;
    }

    /**
     * The Organism item created from the taxon id passed to the constructor.
     * @param taxonId NCBI taxonomy id of organism to create
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
     * @param dataSourceItem the DataSource referenced by the the DataSet
     * @return the DataSet Item
     */
    public Item getDataSetItem(String title, Item dataSourceItem) {
        return getDataSetItem(title, null, null, dataSourceItem);
    }

    /**
     * Return the DataSet title for a given taxon id.
     * @param taxonId the taxon id
     * @return the title
     */
    public abstract String getDataSetTitle(int taxonId);

    /**
     * Return a DataSource item with the given details.
     * @param title the DataSet title
     * @param url the new url field, or null if the url shouldn't be set
     * @param description the new description field, or null if the field shouldn't be set
     * @param dataSourceItem the DataSource referenced by the the DataSet
     * @return the DataSet Item
     */
    public Item getDataSetItem(String title, String url, String description, Item dataSourceItem) {
        Item dataSet = dataSets.get(title);
        if (dataSet == null) {
            dataSet = createItem("DataSet");
            dataSet.setAttribute("title", title);
            dataSet.setReference("dataSource", dataSourceItem);
            if (url != null) {
                dataSet.setAttribute("url", url);
            }
            if (description != null) {
                dataSet.setAttribute("description", description);
            }
            try {
                store(dataSet);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("failed to store DataSet with title: " + title, e);
            }
            dataSets.put(title, dataSet);
        }
        return dataSet;
    }

    /**
     * Make a Chromosome Item, then store and return it.  The Item is held in a Map so this method
     * can be called multiple times.
     * @param identifier the chromsome identifier
     * @param taxonId the id of the organism
     * @throws ObjectStoreException if an Item can't be stored
     * @return the Chromsome Item
     */
    protected Item getChromosome(String identifier, int taxonId) throws ObjectStoreException {
        Item chromosome = chromosomes.get(identifier);
        if (chromosome == null) {
            chromosome = createItem("Chromosome");
            chromosome.setAttribute("primaryIdentifier", identifier);
            chromosome.setReference("organism", getOrganismItem(taxonId));
            chromosomes.put(identifier, chromosome);
            store(chromosome);
        }
        return chromosome;
    }

    /**
     * Create and return a new Synonym, but don't store it.
     * @param subjectId the Synonym subject id
     * @param type the Synonym type
     * @param value the Synonym value
     * @param isPrimary true if this is a primary identifier
     * @param evidence the Synonym evidence
     * @return the new Synonym
     */
    public Item createSynonym(String subjectId, String type, String value, boolean isPrimary,
                              List<Item> evidence) {
        Item synonym = createItem("Synonym");
        synonym.setAttribute("type", type);
        synonym.setAttribute("value", value);
        synonym.setAttribute("isPrimary", String.valueOf(isPrimary));
        synonym.setReference("subject", subjectId);
        if (evidence != null) {
            for (Item evidenceItem: evidence) {
                synonym.addToCollection("evidence", evidenceItem);
            }
        }
        return synonym;
    }
}
