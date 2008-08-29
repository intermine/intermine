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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.bio.dataconversion.ChadoSequenceProcessor.FeatureData;
import org.intermine.bio.util.OrganismData;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 * A processor that loads feature referred to by the modENCODE metadata.  This class is designed
 * to be used by ModEncodeMetaDataProcessor and will be called once for each submission that has
 * metadata.
 * @author Kim Rutherford
 */
public class ModEncodeFeatureProcessor extends ChadoSequenceProcessor
{
    private static final Logger LOG = Logger.getLogger(ModEncodeFeatureProcessor.class);

    private final String dataSetIdentifier;
    private final String dataSourceIdentifier;
    private final List<Integer> dataList;

    // feature type to query from the feature table
    private static final List<String> FEATURES = Arrays.asList(
         "gene", "mRNA", "transcript",
         "CDS", "intron", "exon", "EST",
         "five_prime_untranslated_region",
         "five_prime_UTR", "three_prime_untranslated_region",
         "three_prime_UTR", "origin_of_replication"
    );

    /**
     * Create a new ModEncodeFeatureProcessor.
     * @param chadoDBConverter     the parent converter
     * @param dataSetIdentifier    the item identifier of the DataSet
     * @param dataSourceIdentifier the item identifier of the DataSource
     * @param dataList             the list of data ids to be used in the subquery
     */

    public ModEncodeFeatureProcessor(ChadoDBConverter chadoDBConverter,
            String dataSetIdentifier, String dataSourceIdentifier,
            List <Integer> dataList) {
        super(chadoDBConverter);
        this.dataSetIdentifier = dataSetIdentifier;
        this.dataSourceIdentifier = dataSourceIdentifier;
        this.dataList = dataList;
    }

    /**
     * Get a list of the chado/so types of the LocatedSequenceFeatures we wish to load.  The list
     * will not include chromosome-like features.
     * @return the list of features
     */
    @Override
    protected List<String> getFeatures() {
        return FEATURES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getExtraFeatureConstraint() {
        String queryList = forINclause();

        return "cvterm.name = 'chromosome' OR cvterm.name = 'chromosome_arm' OR feature_id IN "
            + " (SELECT feature_id "
            + "   FROM data_feature "
            + "   WHERE data_id IN (" + queryList + "))";

        /* TODO: needs information from EO
        return "feature_id IN "
            + "(SELECT feature_id "
            + "   FROM some_indirection_table "
            + "   WHERE experiment_id = " + chadoExperimentId + ")";
            */
        //return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void extraProcessing(Connection connection, Map<Integer, FeatureData> featureDataMap)
                    throws ObjectStoreException, SQLException {

        // process indirect locations via match features and featureloc feature<->match<->feature
        ResultSet matchLocRes = getMatchLocResultSet(connection);
        processLocationTable(connection, matchLocRes);
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
            DataSetStoreHook.setDataSets(getModel(), item, dataSetIdentifier, dataSourceIdentifier);
        }
    }



    /**
     * method to transform dataList (list of integers)
     * in the string for a IN clause in SQL (comma separated)
     * @return String
     */
    private String forINclause() {

        StringBuffer ql = new StringBuffer();

        Iterator<Integer> i = dataList.iterator();
        Integer index = 0;
        while (i.hasNext()) {
          index++;
          if (index > 1) {
              ql = ql.append(", ");
          }
            ql = ql.append(i.next());
        }
        return ql.toString();
    }
}
