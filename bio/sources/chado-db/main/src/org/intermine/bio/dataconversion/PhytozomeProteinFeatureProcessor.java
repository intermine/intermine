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
import org.intermine.bio.dataconversion.PhytozomeProcessor;

/**
 * @author jcarlson
 *
 */
public final class PhytozomeProteinFeatureProcessor extends PhytozomeProcessor {


  /**
   * @param parent
   */
  public PhytozomeProteinFeatureProcessor(PhytozomeProcessor parent) {
    super(parent);
  }

  /**
   * Retrieve the ProteinFeatures from all analyses of this set of features
   * @param connection the db connection
   * @return the SQL result set
   * @throws SQLException if a database problem occurs
   */
  @Override
  public void process(Connection connection)
      throws SQLException, ObjectStoreException {
    // quick guide to aliases: f = the feature (protein) from the tempFeatureTable
    //             m = the match (all of the hits for 1 ProteinFeature)
    //             p = the match part (the scored and located part of the hit)
    //
    String query = "SELECT m.feature_id as protein_analysis_feature_id,"
        + " p.uniquename as match_uniquename,"
        + " f.uniquename as protein_uniquename,"
        + " db.name as db_name,"
        + " dbx.accession,"
        + " f.feature_id as protein_feature_id,"
        + " f.organism_id,l.fmin, l.fmax,"
        + " afp.value as program_name,"
        + " af.rawscore, af.normscore, af.significance,"
        + " af.identity"
        + " FROM " + tempFeatureTableName + " f,"
        + " feature m, feature p,"
        + " feature_relationship fm, feature_relationship mp,"
        + " analysisfeature af, analysisfeatureprop afp,"
        + " featureloc l, dbxref dbx, db"
        + " WHERE l.srcfeature_id=f.feature_id"
        + " AND l.feature_id=p.feature_id"
        + " AND f.type_id=" + cvTermMap.get("sequence").get("polypeptide")
        + " AND m.type_id=" + cvTermMap.get("sequence").get("match")
        + " AND p.type_id= " + cvTermMap.get("sequence").get("match_part")
        + " AND fm.type_id=" + cvTermMap.get("relationship").get("contained_in")
        + " AND mp.type_id=" + cvTermMap.get("relationship").get("part_of")
        + " AND afp.type_id=" + cvTermMap.get("sequence").get("evidence_for_feature")
        + " AND afp.rank=0"
        + " AND " + getOrganismConstraint("f")
        + " AND " + getOrganismConstraint("m")
        + " AND " + getOrganismConstraint("p")
        + " AND p.organism_id=f.organism_id"
        + " AND m.organism_id=p.organism_id "
        + " AND m.organism_id=f.organism_id"
        + " AND fm.object_id=f.feature_id"
        + " AND fm.subject_id=m.feature_id"
        + " AND mp.object_id=m.feature_id"
        + " AND mp.subject_id=p.feature_id"
        + " AND p.feature_id=af.feature_id"
        + " AND af.analysisfeature_id = afp.analysisfeature_id"
        + " AND p.dbxref_id=dbx.dbxref_id"
        + " AND dbx.db_id=db.db_id";

    LOG.info("executing getProteinFeatureResultSet(): " + query);
    Statement stmt = connection.createStatement();
    ResultSet res = stmt.executeQuery(query);
    LOG.info("Got resultset.");

    int count=0;
    while(res.next()) {
      Integer proteinAnalysisFeatureId = res.getInt("protein_analysis_feature_id");
      Integer proteinFeatureId = res.getInt("protein_feature_id");
      Integer organismId = res.getInt("organism_id");
      String dbName = res.getString("db_name");
      // what the feature hits
      String proteinName = res.getString("protein_uniquename");
      // the primary identifier we give to the feature
      String hitName = res.getString("match_uniquename");
      // the thing the feature hits
      String accession = res.getString("accession");
      // the program that made the assignment
      String programName = res.getString("program_name");
      // min and max. convert to 1-indexed. SIGNALP is
      // a special case since it is a cleavage site.
      Integer proteinAnalysisFeatureMin = "SIGNALP".equals(dbName)?
          res.getInt("fmin") - 1:
            res.getInt("fmin") + 1;
          Integer proteinAnalysisFeatureMax = res.getInt("fmax");
          // assuming strand = 1
          Double rawScore = res.getDouble("rawscore");
          if (res.wasNull()) rawScore = null;
          Double normScore = res.getDouble("normscore");
          if (res.wasNull()) normScore = null;
          Double significance = res.getDouble("significance");
          if (res.wasNull()) significance = null;
          if( processAndStoreProteinAnalysisFeature(proteinAnalysisFeatureId,proteinFeatureId,
              hitName,proteinName,accession,dbName,programName,
              proteinAnalysisFeatureMin,proteinAnalysisFeatureMax,organismId,
              rawScore,normScore,significance) ) {
            count++;
          }
    }
    LOG.info("processed " + count + " ProteinAnalysisFeature records");
    res.close();
  }
  /**
   * Create and store a new InterMineObject given data from a row of
   * the feature table in a Chado database.
   * @param featureId the chado id from the feature table
   * @param uniqueName the uniquename from Chado
   * @param name the name from Chado
   * @param seqlen the sequence length from Chado
   * @param residues the residues from Chado
   * @param md5checksum the MD5 checksum of the residues
   * @param chadoType the type of the feature from the feature + cvterm tables
   * @param organismId the chado organism id
   * @throws ObjectStoreException if there is a problem while storing
   */
  private boolean processAndStoreProteinAnalysisFeature(Integer matchId,
      Integer proteinId,String hitName, String proteinName,
      String accession, String dbName,String programName,
      Integer fmin, Integer fmax,Integer organismId,
      Double rawScore,Double normScore, Double significance)
          throws ObjectStoreException {

    if (!featureMap.containsKey(proteinId)) {
      return false;
    }
    FeatureData fdat;
    fdat = featureMap.get(proteinId);
    //String proteinId = fdat.getPrimaryIdentifier();

    if (featureMap.containsKey(matchId) ) {
      fdat = featureMap.get(matchId);
    } else {
      fdat = makeProteinAnalysisFeatureData(proteinId.intValue(),hitName,featureMap.get(proteinId),
          accession, dbName, programName, organismId.intValue(),
          rawScore, normScore, significance);
      featureMap.put(matchId, fdat);
    }
    if (fdat == null) {
      return false;
    }
    Item location = storeLocation(fmin, fmax, new Integer(1),
        featureMap.get(proteinId),fdat,organismId.intValue());
    if (location != null) {
      getChadoDBConverter().store(location);
    }
    return true;
  }
}
