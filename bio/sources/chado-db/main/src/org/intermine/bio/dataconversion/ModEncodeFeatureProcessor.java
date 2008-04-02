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

import java.util.List;

import org.intermine.bio.util.OrganismData;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * A processor that loads feature referred to by the modENCODE metadata.  This class is designed
 * to be used by ModEncodeFeatureProcessor and will be called once for each submission that has
 * metadata.
 * @author Kim Rutherford
 */
public class ModEncodeFeatureProcessor extends ChadoSequenceProcessor
{
    private final String dataSetIdentifier;
    private final String dataSourceIdentifier;
    private final Integer chadoExperimentId;

    /**
     * Create a new ModEncodeFeatureProcessor.
     * @param chadoDBConverter the parent converter
     * @param dataSetIdentifier the item identifier of the DataSet
     * @param dataSourceIdentifier the item identifier of the DataSource
     * @param chadoExperimentId the chado internal identifier of the experiment (the submission)
     */
    public ModEncodeFeatureProcessor(ChadoDBConverter chadoDBConverter,
                                     String dataSetIdentifier, String dataSourceIdentifier,
                                     Integer chadoExperimentId) {
        super(chadoDBConverter);
        this.dataSetIdentifier = dataSetIdentifier;
        this.dataSourceIdentifier = dataSourceIdentifier;
        this.chadoExperimentId = chadoExperimentId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getExtraFeatureConstraint() {
        return "AND feature_id IN "
            + "(SELECT feature_id "
            + "   FROM some_indirection_table "
            + "   WHERE experiment_id = " + chadoExperimentId + ")";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Integer store(Item feature, int taxonId) throws ObjectStoreException {
        processItem(feature, taxonId);
        Integer itemId = super.store(feature, taxonId);
        return itemId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Item makeLocation(int start, int end, int strand, FeatureData srcFeatureData,
                              FeatureData featureData, int taxonId) throws ObjectStoreException {
        Item location =
            super.makeLocation(start, end, strand, srcFeatureData, featureData, taxonId);
        processItem(location, taxonId);
        getChadoDBConverter().store(location);
        return location;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Item createSynonym(FeatureData fdat, String type, String identifier,
                                 boolean isPrimary, List<Item> otherEvidence)
        throws ObjectStoreException {
        Item synonym = super.createSynonym(fdat, type, identifier, isPrimary, otherEvidence);
        OrganismData od = fdat.getOrganismData();
        processItem(synonym, od.getTaxonId());
        getChadoDBConverter().store(synonym);
        return synonym;
    }


    private Item getDataSourceItem(String dataSourceName) {
        return getChadoDBConverter().getDataSourceItem(dataSourceName);
    }

    private Item getDataSetItem(String dataSourceName, int taxonId) {
        OrganismRepository or = OrganismRepository.getOrganismRepository();
        OrganismData od = or.getOrganismDataByTaxon(taxonId);
        String species = od.getSpecies();
        String genus = od.getGenus();
        String name = "FlyBase " + genus + " " + species + " data set";
        String description = "The FlyBase " + genus + " " + species + " genome";
        return getChadoDBConverter().getDataSetItem(name, "http://www.flybase.org", description,
                                                    getDataSourceItem(dataSourceName));
    }

    /**
     * Method to add dataSets and DataSources to items before storing
     */
    private void processItem(Item item, Integer taxonId) {
        if (item.getClassName().equals("http://www.flymine.org/model/genomic#DataSource")
            || item.getClassName().equals("http://www.flymine.org/model/genomic#DataSet")
            || item.getClassName().equals("http://www.flymine.org/model/genomic#Organism")
            || item.getClassName().equals("http://www.flymine.org/model/genomic#Sequence")) {
            return;
        }

        if (taxonId == null) {
            ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
            ClassLoader classLoader = getClass().getClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            try {
                throw new RuntimeException("getCurrentTaxonId() returned null while processing "
                                           + item);
            } finally {
                Thread.currentThread().setContextClassLoader(currentClassLoader);
            }
        } else {
            DataSetStoreHook.setDataSets(item, dataSetIdentifier, dataSourceIdentifier);
        }
    }
}
