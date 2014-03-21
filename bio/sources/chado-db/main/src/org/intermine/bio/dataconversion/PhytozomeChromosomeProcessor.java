package org.intermine.bio.dataconversion;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.tools.ant.BuildException;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.bio.dataconversion.PhytozomeProcessor;

public class PhytozomeChromosomeProcessor extends PhytozomeProcessor {

  PhytozomeProcessor parentProcessor;
  /**
   * Create a new PhytozomeProcessor
   * @param chadoDBConverter the ChadoDBConverter that is controlling
   * this processor
   */ 
  public PhytozomeChromosomeProcessor(PhytozomeProcessor parent) {
    super(parent);
    parentProcessor = parent;
  }
  /**
   * Query the chromosome table and store features as object of the
   * appropriate type in the object store.
   * @param connection
   * @throws SQLException
   * @throws ObjectStoreException
   */
  @Override
  public void process(Connection connection)
      throws SQLException, ObjectStoreException {
    ResultSet res = createChromosomeTempTable(connection);
    int count = 0;
    while (res.next()) {
      Integer featureId = new Integer(res.getInt("feature_id"));
      String name = res.getString("name");
      String uniqueName = res.getString("uniquename");
      String residues = res.getString("residues");
      String checksum = res.getString("md5checksum");
      Integer organismId = new Integer(res.getInt("organism_id"));
      int seqlen = 0;
      if (res.getObject("seqlen") != null) {
        seqlen = res.getInt("seqlen");
      }
      if (storeFeature(featureId, name, uniqueName,
          seqlen, residues, checksum, "chromosome", organismId)) {
        count++;
      }
    }
    LOG.info("created " + count + " chromosomes");
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
  protected ResultSet createChromosomeTempTable(Connection connection)
      throws SQLException {
    String chromosomeTypeIdsString =
        getSQLString(getFeatures("chromosome"));
    String genomeTypeIdsString = getSQLString(getFeatures("genome"));
    String relationTypeIdsString = getSQLString(getRelations("contained"));

    // if dbxref_id is not null, use it. otherwise use the is_obsolete flag.
    String dbxref = getChadoDBConverter().getDbxrefId();
    Integer dbxref_id;
    String genomeSelector = "";
    if (dbxref != null) {
      try {
        dbxref_id = new Integer(dbxref);
        genomeSelector = " AND g.dbxref_id = " + dbxref_id;
      } catch (NumberFormatException e) {
        // we need to look up the id
        // TODO: look up the id.
      }
    } else {
      genomeSelector = " AND g.is_obsolete = 'f'";
    }
    String verifyGenomeQuery =
        "SELECT count(feature_id) FROM feature g where "
            + "  g.type_id " + genomeTypeIdsString
            + "  AND " + getOrganismConstraint("g")
            + genomeSelector;
    Statement verifyStatement = connection.createStatement();
    try {
      ResultSet res = verifyStatement.executeQuery(verifyGenomeQuery);
      while (res.next()) {
        Integer count = new Integer(res.getInt("count"));
        if (count > 1) {
          throw new BuildException("There are " + count + " genomes that satisfy dbxref_id/is_obsolete condition. " );
        } else if (count == 0) {
          throw new BuildException("There are no genomes that satisfy dbxref_id/is_obsolete condition. " );
        }
      }
      res.close();
    } catch (SQLException e) {
      LOG.error("Problem counting the genomes.");
      throw new BuildException("Problem when counting the genomes " + e);
    }
        
                                             
    String query =
        "CREATE TEMPORARY TABLE " + tempChromosomeTableName + " AS"
            + " SELECT c.feature_id, c.name, c.uniquename,"
            + " c.seqlen, c.is_analysis,"
            + " c.residues, c.md5checksum, c.organism_id"
            + " FROM "
            + " feature c, feature g, feature_relationship r"
            + " WHERE c.type_id " + chromosomeTypeIdsString
            + "  AND g.type_id " + genomeTypeIdsString
            + "  AND r.type_id " + relationTypeIdsString
            + "  AND r.object_id=g.feature_id"
            + "  AND r.subject_id=c.feature_id"
            + "  AND c.is_obsolete = 'f'"
            + genomeSelector
            + "  AND g.organism_id=c.organism_id "
            + "  AND " + getOrganismConstraint("g")
            + "  AND " + getOrganismConstraint("c")
            + ((getExtraFeatureConstraint() != null)?
                " AND (" + getExtraFeatureConstraint() + ")" : "");

    Statement stmt = connection.createStatement();
    LOG.info("executing createChromosomeTempTable(): " + query);
    try {
      stmt.execute(query);
      LOG.info("Done with query.");
      String idIndexQuery = "CREATE INDEX " + tempChromosomeTableName +
          "_feature_index ON " + tempChromosomeTableName + "(feature_id)";
      LOG.info("executing: " + idIndexQuery);
      stmt.execute(idIndexQuery);
      String analyze = "ANALYZE " + tempChromosomeTableName;
      LOG.info("executing: " + analyze);
      stmt.execute(analyze);
      LOG.info("Done with analyze.");
      LOG.info("Querying temp chromosome table.");
    } catch (SQLException e) {
      LOG.info("SQL failed. Assuming table exists.");
    }
    String selectQuery = "SELECT * FROM " + tempChromosomeTableName;
    ResultSet res = stmt.executeQuery(selectQuery);
    LOG.info("Have result set.");
    return res;
  }
}