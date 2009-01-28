package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.bio.util.OrganismData;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.intermine.util.StringUtil;

/**
 * A processor that loads feature referred to by the modENCODE metadata.  This class is designed
 * to be used by ModEncodeMetaDataProcessor and will be called once for each submission that has
 * metadata.
 * @author Kim Rutherford
 */
public class ModEncodeFeatureProcessor extends SequenceProcessor
{
    private static final Logger LOG = Logger.getLogger(ModEncodeFeatureProcessor.class);

    private final String dataSetIdentifier;
    private final String dataSourceIdentifier;
    private final List<Integer> dataList;

    private static final String SUBFEATUREID_TEMP_TABLE_NAME = "modmine_subfeatureid_temp";


    // feature type to query from the feature table
    private static final List<String> FEATURES = Arrays.asList(
         "gene", "mRNA", "transcript",
         "CDS", "intron", "exon", "EST",
         "five_prime_untranslated_region",
         "five_prime_UTR", "three_prime_untranslated_region",
         "three_prime_UTR", "origin_of_replication",
         "binding_site", "protein_binding_site", "TF_binding_site",
         "transcript_region", "histone_binding_site"
    );

    /**
     * Create a new ModEncodeFeatureProcessor.
     * @param chadoDBConverter     the parent converter
     * @param dataSetIdentifier    the item identifier of the DataSet, 
     *                             i.e. the submissionItemIdentifier
     * @param dataSourceIdentifier the item identifier of the DataSource, 
     *                             i.e. the labItemIdentifier 
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
        /*
         * tried also other queries (using union, without join), this seems better
         */

        return "(cvterm.name = 'chromosome' OR cvterm.name = 'chromosome_arm') AND "
        + " feature_id IN ( SELECT featureloc.srcfeature_id "
        + " FROM featureloc, " + SUBFEATUREID_TEMP_TABLE_NAME
        + " WHERE featureloc.feature_id = " + SUBFEATUREID_TEMP_TABLE_NAME + ".feature_id) "
        + " OR feature_id IN ( SELECT feature_id "
        + " FROM " + SUBFEATUREID_TEMP_TABLE_NAME + " ) ";

/*        return "(cvterm.name = 'chromosome' OR cvterm.name = 'chromosome_arm') AND "
        + " feature_id IN ( SELECT featureloc.srcfeature_id "
        + " FROM featureloc "
        + " WHERE featureloc.feature_id IN ( SELECT feature_id "
        + " FROM " + SUBFEATUREID_TEMP_TABLE_NAME + " )) "
        + " OR feature_id IN ( SELECT feature_id "
        + " FROM " + SUBFEATUREID_TEMP_TABLE_NAME + " ) ";
*/

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
        

        
        LOG.info("EST: before");

        ResultSet matchESTLocRes = getESTMatchLocResultSet(connection);
        processLocationTable(connection, matchESTLocRes);
        
    }

    
    /**
     * Return the interesting EST matches from the featureloc and feature tables.
     * feature<->featureloc<->match_feature<->featureloc<->feature
     * This is a protected method so that it can be overriden for testing
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getESTMatchLocResultSet(Connection connection) throws SQLException {
        String query =       
            "SELECT -1 AS featureloc_id, est.feature_id, chrloc.fmin, " 
            + " chrloc.srcfeature_id AS srcfeature_id, chrloc.fmax, FALSE AS is_fmin_partial, " 
            + " estloc.strand "
            + " FROM feature est, featureloc estloc, cvterm estcv, feature mf, "
            + " cvterm mfcv, featureloc chrloc, feature chr, cvterm chrcv "
            + " WHERE est.type_id = estcv.cvterm_id "
            + " AND estcv.name = 'EST' " 
            + " AND est.feature_id = estloc.srcfeature_id "
            + " AND estloc.feature_id = mf.feature_id "
            + " AND mf.feature_id = chrloc.feature_id "
            + " AND chrloc.srcfeature_id = chr.feature_id "
            + " AND chr.type_id = chrcv.cvterm_id "
            + " AND chrcv.name = 'chromosome' "
            + " AND mf.type_id = mfcv.cvterm_id "
            + " AND mfcv.name = 'EST_match' "
            + " AND est.feature_id IN " 
            + " (select feature_id from " + SUBFEATUREID_TEMP_TABLE_NAME + " ) ";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
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
     * copied from FlyBaseProcessor
     * {@inheritDoc}
     */
    @Override
    protected Item makeFeature(Integer featureId, String chadoFeatureType, String interMineType,
                               String name, String uniqueName,
                               int seqlen, int taxonId) {
        String realInterMineType = interMineType;

        if (chadoFeatureType.equals("chromosome_arm")
                || chadoFeatureType.equals("ultra_scaffold")) {
                realInterMineType = "Chromosome";
            }

        Item feature = getChadoDBConverter().createItem(realInterMineType);

        return feature;
    }



    /**
     * method to transform dataList (list of integers)
     * in the string for a IN clause in SQL (comma separated)
     * @return String
     */
    protected String forINclause() {

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

    /**
     * Create a temporary table of all feature_ids for a given submission.
     * @param connection the connection
     * @throws SQLException if there is a database problem
     */
    protected void createSubFeatureIdTempTable(Connection connection) throws SQLException {
        String queryList = forINclause();

        String query =
            " CREATE TEMPORARY TABLE " + SUBFEATUREID_TEMP_TABLE_NAME
            + " AS SELECT data_feature.feature_id "
            + " FROM data_feature "
            + " WHERE data_id IN (" + queryList + ")";

        Statement stmt = connection.createStatement();
        LOG.info("executing: " + query);
        stmt.execute(query);
        String idIndexQuery = "CREATE INDEX " + SUBFEATUREID_TEMP_TABLE_NAME + "_feature_index ON "
            + SUBFEATUREID_TEMP_TABLE_NAME + "(feature_id)";
        LOG.info("executing: " + idIndexQuery);
        stmt.execute(idIndexQuery);
        String analyze = "ANALYZE " + SUBFEATUREID_TEMP_TABLE_NAME;
        LOG.info("executing: " + analyze);
        stmt.execute(analyze);
    }




    /**
     * {@inheritDoc}
     */
    @Override
   protected void earlyExtraProcessing(Connection connection) throws  SQLException {
        createSubFeatureIdTempTable(connection);

        // override in subclasses as necessary
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected void finishedProcessing(Connection connection,
            Map<Integer, FeatureData> featureDataMap)
        throws SQLException {
        // override in subclasses as necessary
        String query =
            " DROP TABLE " + SUBFEATUREID_TEMP_TABLE_NAME;

        Statement stmt = connection.createStatement();
        LOG.info("executing: " + query);
        stmt.execute(query);

    }

    /**
     * Process the identifier and return a "cleaned" version.  Implement in sub-classes to fix
     * data problem.
     * @param type the InterMine type of the feature that this identifier came from
     * @param identifier the identifier
     * @return a cleaned identifier
     */
    /**
     * {@inheritDoc}
     */
    @Override
    protected String fixIdentifier(FeatureData fdat, String identifier) {
        
        //do
        String uniqueName = fdat.getChadoFeatureUniqueName();
        String name = fdat.getChadoFeatureName();
//        String type = fdat.getInterMineType();

        if (StringUtil.isEmpty(identifier)) {
            if (StringUtil.isEmpty(name)) {
                String fixedName = uniqueName.substring(uniqueName.lastIndexOf('.') + 1);
                LOG.info("IDI " + fixedName + "|<-" + uniqueName + "|" + name + "|");
                return fixedName;
            } else {
                LOG.info("IDI else " + uniqueName + "|" + name + "|");
                return name;
            }
        } else if (identifier == name) {
            LOG.info("IDI name | " + uniqueName + "|" + name + "|");
            return identifier;
        } else {
            LOG.info("IDI rest | " + uniqueName + "|" + name + "|");
            return identifier;
        }
    }


}
