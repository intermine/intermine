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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.intermine.bio.util.BioConverterUtil;
import org.intermine.dataconversion.DirectoryConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * A DirectoryConverter that automatically sets the dataSets collection of
 * objects as they are stored.
 *
 * @author Julie Sullivan
 */

public abstract class BioDirectoryConverter extends DirectoryConverter
{
    private final Map<String, String> dataSets = new HashMap<String, String>();
    private final Map<String, String> dataSources = new HashMap<String, String>();
    private Set<String> synonyms = new HashSet<String>();
    private Set<String> crossReferences = new HashSet<String>();
    private Map<String, String> organisms = new HashMap<String, String>();
    private BioStoreHook hook = null;
    private String sequenceOntologyRefId;

    /**
     * Create a new BioDirectoryConverter.
     * @param writer the Writer used to output the resultant items
     * @param model the data model
     * @param dataSourceName the DataSource name
     * @param dataSetTitle the DataSet title
     */
    public BioDirectoryConverter (ItemWriter writer, Model model, String dataSourceName,
            String dataSetTitle) {
        super(writer, model);
        String dataSourceRefId = null;
        String dataSetRefId = null;
        sequenceOntologyRefId = BioConverterUtil.getOntology(this);
        if (StringUtils.isNotEmpty(dataSourceName)) {
            dataSourceRefId = getDataSource(dataSourceName);
        }
        if (StringUtils.isNotEmpty(dataSetTitle)) {
            dataSetRefId = getDataSet(dataSetTitle, dataSourceRefId);
        }
        hook = new BioStoreHook(model, dataSetRefId, dataSourceRefId, sequenceOntologyRefId);
        setStoreHook(hook);
    }

    /**
     * Create a new BioDirectoryConverter.
     * @param writer the Writer used to output the resultant items
     * @param model the data model
     * @param dataSourceName the DataSource name
     * @param dataSetTitle the DataSet title
     * @param ontology name of Ontology eg. "Sequence Ontology"
     */
    public BioDirectoryConverter (ItemWriter writer, Model model, String dataSourceName,
            String dataSetTitle, String ontology) {
        super(writer, model);
        String dataSourceRefId = null;
        String dataSetRefId = null;
        if (StringUtils.isNotEmpty(dataSourceName)) {
            dataSourceRefId = getDataSource(dataSourceName);
        }
        if (StringUtils.isNotEmpty(dataSetTitle)) {
            dataSetRefId = getDataSet(dataSetTitle, dataSourceRefId);
        }
        hook = new BioStoreHook(model, dataSetRefId, dataSourceRefId, ontology);
        setStoreHook(hook);
    }

    /**
     * Update the dataset reference Id.  Overwrites the one set in the constructor.  Needed for
     * Uniprot which has a few different datasets.
     *
     * @param refId ID representing dataset object
     */
    public void setDataSet(String refId) {
        hook.setDataSet(refId);
    }

    /**
     * Return a DataSource item for the given title
     * @param name the DataSource name
     * @return the DataSource Item
     */
    public String getDataSource(String name) {
        if (name == null) {
            return null;
        }
        String refId = dataSources.get(name);
        if (refId == null) {
            Item item = createItem("DataSource");
            item.setAttribute("name", name);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("failed to store DataSource with name: " + name, e);
            }
            refId = item.getIdentifier();
            dataSources.put(name, refId);
        }
        return refId;
    }

    /**
     * Return a DataSet item with the given details.
     * @param title the DataSet title
     * @param dataSourceRefId the DataSource referenced by the the DataSet
     * @return the DataSet Item
     */
    public String getDataSet(String title, String dataSourceRefId) {
        String refId = dataSets.get(title);
        if (refId == null) {
            Item dataSet = createItem("DataSet");
            dataSet.setAttribute("name", title);
            dataSet.setReference("dataSource", dataSourceRefId);
            try {
                store(dataSet);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("failed to store DataSet with title: " + title, e);
            }
            refId = dataSet.getIdentifier();
            dataSets.put(title, refId);
        }
        return refId;
    }

    /**
     * The Organism item created from the taxon id passed to the constructor.
     * @param taxonId NCBI taxonomy id of organism to create
     * @return the refId representing the Organism Item
     */
    public String getOrganism(String taxonId) {
        String refId = organisms.get(taxonId);
        if (refId == null) {
            Item organism = createItem("Organism");
            organism.setAttribute("taxonId", taxonId);
            try {
                store(organism);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("failed to store organism with taxonId: " + taxonId, e);
            }
            refId = organism.getIdentifier();
            organisms.put(taxonId, refId);
        }
        return refId;
    }

    /**
     * Create a new CrossReference.  Keeps a map of already processed items, ignores duplicates.
     * The "store" param should be true only if the subject has already been stored.  Storing a
     * CrossReference first can signficantly slow down the build process.
     * @param subjectId id representing the object (eg. Gene) this CrossReference describes.
     * @param value identifier
     * @param dataSource external database
     * @param store if true, will store item
     * @throws ObjectStoreException if the synonym can't be stored
     * @return the synonym item or null if this is a duplicate
     */
    public Item createCrossReference(String subjectId, String value, String dataSource,
            boolean store) throws ObjectStoreException {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        String key = subjectId + value;
        if (!crossReferences.contains(key)) {
            Item item = createItem("CrossReference");
            item.setAttribute("identifier", value);
            item.setReference("subject", subjectId);
            item.setReference("source", getDataSource(dataSource));
            crossReferences.add(key);
            if (store) {
                store(item);
            }
            return item;
        }
        return null;
    }

    /**
     * Create a new Synonym.  Keeps a map of already processed synonyms, ignores duplicates.
     * The "store" param should be true only if the subject has already been stored.  Storing a
     * synonym first can signficantly slow down the build process.
     * @param subjectId id representing the object (eg. Gene) this synonym describes.
     * @param value the Synonym value
     * @param store if true, will store item
     * @throws ObjectStoreException if the synonym can't be stored
     * @return the synonym item or null if this is a duplicate
     */
    public Item createSynonym(String subjectId, String value, boolean store)
        throws ObjectStoreException {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        String key = subjectId + value;
        if (!synonyms.contains(key)) {
            Item synonym = createItem("Synonym");
            synonym.setAttribute("value", value);
            synonym.setReference("subject", subjectId);
            synonyms.add(key);
            if (store) {
                store(synonym);
            }
            return synonym;
        }
        return null;
    }

    /**
     * @return ID represening the Ontology object
     */
    public String getSequenceOntologyRefId() {
        if (sequenceOntologyRefId == null) {
            sequenceOntologyRefId = BioConverterUtil.getOntology(this);
        }
        return sequenceOntologyRefId;
    }
}
