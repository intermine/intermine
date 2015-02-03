/**
 * 
 */
package org.intermine.bio.dataconversion;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.bio.dataconversion.PhytozomeProcessor;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.model.bio.DataSource;

/**
 * @author jcarlson
 *
 */
public final class PhytozomeLocationProcessor extends PhytozomeProcessor {

  /**
   * @param parent
   */
  public PhytozomeLocationProcessor(PhytozomeProcessor parent) {
    super(parent);
  }
  /**
   * Process a featureloc table and create Location objects.
   * @param connection the Connection
   * @param res a ResultSet that has the columns:
   *    featureloc_id, feature_id, srcfeature_id, fmin, fmax, strand
   * @throws SQLException if there is a problem while querying
   * @throws ObjectStoreException if there is a problem while storing
   */
  @Override
  public void process(Connection connection)
      throws SQLException, ObjectStoreException {
    int count = 0;
    int featureWarnings = 0;
    String query = "SELECT featureloc_id, feature_id, srcfeature_id, fmin, "
        + " is_fmin_partial, fmax, is_fmax_partial, strand"
        + " FROM " + tempFeatureTableName;
    LOG.info("executing getFeatureLocResultSet(): " + query);
    Statement stmt = connection.createStatement();
    ResultSet res = stmt.executeQuery(query);
    LOG.info("got resultset.");
    while (res.next()) {
      Integer featureLocId = new Integer(res.getInt("featureloc_id"));
      Integer featureId = new Integer(res.getInt("feature_id"));
      Integer srcFeatureId = new Integer(res.getInt("srcfeature_id"));
      int start = res.getInt("fmin") + 1;
      int end = res.getInt("fmax");
      if (start < 1 || end < 1) {
        // ignore as this location not legal in flymine
        continue;
      }
      int strand = res.getInt("strand");
      if (featureMap.containsKey(srcFeatureId)) {
        FeatureData srcFeatureData = featureMap.get(srcFeatureId);
        if (featureMap.containsKey(featureId)) {
          FeatureData featureData = featureMap.get(featureId);
          int taxonId = featureData.organismData.getTaxonId();
          Item location =
              storeLocation(start, end, strand, srcFeatureData,
                  featureData, taxonId);
          // location could be null
          if (location != null) {
            getChadoDBConverter().store(location);
          }
          final String featureClassName =
              getModel().getPackageName() + "."
                  + featureData.getInterMineType();
          Class<?> featureClass;
          try {
            featureClass = Class.forName(featureClassName);
          } catch (ClassNotFoundException e) {
            throw new RuntimeException("unable to find class "
                + " object for setting a chromosome reference", e);
          }
          if (SequenceFeature.class.isAssignableFrom(featureClass)) {
            Integer featureIntermineObjectId =
                featureData.getIntermineObjectId();
            if ("Chromosome".equals(srcFeatureData.getInterMineType())) {
              Reference chrReference = new Reference();
              chrReference.setName("chromosome");
              chrReference.setRefId(srcFeatureData.getItemIdentifier());
              getChadoDBConverter().store(chrReference, featureIntermineObjectId);
            }
            if (location != null) {
              Reference locReference = new Reference();
              locReference.setName("chromosomeLocation");
              locReference.setRefId(location.getIdentifier());
              getChadoDBConverter().store(locReference, featureIntermineObjectId);
            }
            if (!featureData.getFlag(FeatureData.LENGTH_SET)) {
              setAttribute(featureData.getIntermineObjectId(), "length",
                  String.valueOf(end - start + 1));
            }
          } else {
            //LOG.warn("featureId (" + featureId + ") from location " + featureLocId
            //    + " was expected to be a SequenceFeature");
          }
          count++;
        } else {
          if (featureWarnings <= 20) {
            if (featureWarnings < 20) {
              LOG.warn("featureId (" + featureId + ") from location " + featureLocId
                  + " was not found in the feature table");
            } else {
              LOG.warn("further location warnings ignored");
            }
            featureWarnings++;
          }
        }
      } else {
        String msg = "srcfeature_id (" + srcFeatureId + ") from location "
            + featureLocId + " was not found in the feature table";
        LOG.error(msg);
        throw new RuntimeException(msg);
      }
    }
    LOG.info("created " + count + " locations");
    res.close();
  }
}
