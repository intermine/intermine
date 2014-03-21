/**
 * 
 */
package org.intermine.bio.dataconversion;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.bio.dataconversion.PhytozomeProcessor;

/**
 * @author jcarlson
 *
 */
public final class PhytozomeFeatureProcessor extends PhytozomeProcessor {

  /**
   * @param parent
   */
  public PhytozomeFeatureProcessor(PhytozomeProcessor parent) {
    super(parent);
  }
  /**
   * Query the feature table and store features as object of the
   * appropriate type in the object store.
   * @param connection
   * @throws SQLException
   * @throws ObjectStoreException
   */
  @Override
  public void process(Connection connection)
      throws SQLException, ObjectStoreException {

    ResultSet res = createFeatureTempTable(connection);
    int count = 0;
    while (res.next()) {
      Integer featureId = new Integer(res.getInt("feature_id"));
      String name = res.getString("name");
      String uniqueName = res.getString("uniquename");
      String type = cvTermInvMap.get(new Integer(res.getInt("type_id")));
      String residues = res.getString("residues");
      String checksum = res.getString("md5checksum");
      if (residues != null && (checksum == null || checksum.length() == 0)) {
        LOG.warn("No checksum for " + name);
      }
      Integer organismId = new Integer(res.getInt("organism_id"));
      int seqlen = 0;
      if (res.getObject("seqlen") != null) {
        seqlen = res.getInt("seqlen");
      }
      if (storeFeature(featureId, name, uniqueName,
          seqlen, residues, checksum, type, organismId)) {
        count++;
      }
    }
    LOG.info("created " + count + " features");
    res.close();
  }
  /**
   * Create a temporary table containing only the features that interest
   * us. Also create indexes for the type and feature_id columns.
   * The table is used in later queries. This is a protected method
   * so that it can be overridden for testing.

   * @param connection the Connection
   * @throws SQLException if there is a problem
   */
  protected ResultSet createFeatureTempTable(Connection connection)
      throws SQLException {
    String featureTypeIdsString = getSQLString(getFeatures("annotation"));
    String genomeTypeIdsString = getSQLString(getFeatures("genome"));

    String query =
        "CREATE TEMPORARY TABLE " + tempFeatureTableName + " AS"
            + " SELECT f.feature_id, f.name, f.uniquename,"
            + " f.type_id, f.seqlen, f.is_analysis,"
            + " f.residues, f.md5checksum, f.organism_id,"
            + " l.featureloc_id, l.srcfeature_id, l.fmin,"
            + " l.is_fmin_partial, l.fmax, l.is_fmax_partial,"
            + " l.strand"
            + " FROM feature f," 
            + tempChromosomeTableName + " g,"
            + " featureloc l"
            + " WHERE f.type_id IN (" + featureTypeIdsString + ")"
            + " AND NOT f.is_obsolete"
            + " AND l.srcfeature_id = g.feature_id "
            + " AND f.feature_id = l.feature_id"
            + " AND " + getOrganismConstraint("f")
            + " AND g.organism_id = f.organism_id"
            + (getExtraFeatureConstraint() != null
            ? " AND (" + getExtraFeatureConstraint() + ")"
                : "");

    Statement stmt = connection.createStatement();
    LOG.info("executing createFeatureTempTable(): " + query);
    try {
      stmt.execute(query);

      LOG.info("executed query.");
      String idIndexQuery = "CREATE INDEX " + tempFeatureTableName
          + "_feature_index ON "
          + tempFeatureTableName + "(feature_id)";
      LOG.info("executing: " + idIndexQuery);
      stmt.execute(idIndexQuery);
      String typeIndexQuery = "CREATE INDEX " + tempFeatureTableName
          + "_type_index ON "
          + tempFeatureTableName + "(type_id)";
      LOG.info("executing: " + typeIndexQuery);
      stmt.execute(typeIndexQuery);
      String analyze = "ANALYZE " + tempFeatureTableName;
      LOG.info("executing: " + analyze);
      stmt.execute(analyze);
      LOG.info("Done with analyze.");
    } catch ( SQLException e) {
      LOG.info("SQL failed. Assuming table exists.");
    }

    LOG.info("Querying temp feature table.");
    String selectQuery = "SELECT * FROM " + tempFeatureTableName;
    ResultSet res = stmt.executeQuery(selectQuery);
    LOG.info("Have result set.");
    return res;
  }
}


