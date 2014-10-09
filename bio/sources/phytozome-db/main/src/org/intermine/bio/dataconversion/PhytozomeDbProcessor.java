/**
 * 
 */
package org.intermine.bio.dataconversion;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.bio.dataconversion.PhytozomeDbConfig;
import org.intermine.bio.util.OrganismData;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.TypeUtil;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

/**
 * @author jcarlson
 *
 */
public class PhytozomeDbProcessor {

  // PRIVATE CLASS VARIABLES

  protected static final Logger LOG =
      Logger.getLogger(PhytozomeDbProcessor.class);
  
  // the name of the temporary tables we create from the feature table
  // to speed up processing
  protected String tempChromosomeTableName = "intermine_chado_chromosomes";
  protected String tempFeatureTableName = "intermine_chado_features";

  // PRIVATE HASHMAPS
  // these keep track of the interproscan db's and hits in the db's.
  protected Map<String,String> dataSourceMap;
  protected Map<String,HashMap<String,Item>> dataHitMap;
  
  PhytozomeDbConverter converter;  // the caller
  String orgId;       // the intermine object id
  Integer organismId; // the CHADO organism_id
  Integer assemblyDbxrefId;
  Integer annotationDbxrefId;
  PhytozomeDbConfig config;
  
  public PhytozomeDbProcessor(PhytozomeDbConverter conv,String organismIdentifier,
      Integer organism,Integer assembly,Integer annotation) {
    converter = conv;
    orgId = organismIdentifier;
    organismId = organism;
    assemblyDbxrefId = assembly;
    annotationDbxrefId = annotation;
    dataSourceMap = new HashMap<String,String>();
    dataHitMap = new HashMap<String,HashMap<String,Item>>();
    config = new PhytozomeDbConfig(converter);
  }
  
  public void process() throws SQLException, ObjectStoreException {
    // the steps
    fillChromosomeTable();
    fillAnnotationTable();
    fillProperties();
    fillRelationships();
    fillAnalyses();
    converter.getDatabase().getConnection().createStatement().execute(
        "DROP TABLE "+ tempChromosomeTableName);
    converter.getDatabase().getConnection().createStatement().execute(
        "DROP TABLE "+ tempFeatureTableName);
  }
  
  private void fillAnalyses() throws SQLException, ObjectStoreException {
    
    String pQuery = "SELECT p.feature_id as protein_analysis_feature_id," +
        "p.name as match_name, " +
        "f.name as protein_name," +
        "f.uniquename as protein_uniquename," +
        "db.name as db_name," +
        "dbx.accession," +
        "f.feature_id as protein_feature_id," +
        "l.fmin, l.fmax," +
        "afp.value as program_name," +
        "af.rawscore, af.normscore, af.significance," +
        "af.identity " +
        "FROM " + tempFeatureTableName + " f," +
        "feature m, feature p," +
        "feature_relationship fm, feature_relationship mp," +
        "analysisfeature af, analysisfeatureprop afp," +
        "featureloc l, dbxref dbx, db "+
        "WHERE l.srcfeature_id=f.feature_id " +
        "AND l.feature_id=p.feature_id " +
        "AND f.type_id = " + config.cvTerm("sequence","polypeptide") + " " +
        "AND m.type_id = " + config.cvTerm("sequence","match") + " " +
        "AND p.type_id = " + config.cvTerm("sequence","match_part") + " " +
        "AND fm.type_id = " + config.cvTerm("relationship","contained_in") + " " +
        "AND mp.type_id = " + config.cvTerm("relationship","part_of") + " " +
        "AND afp.type_id = " + config.cvTerm("sequence","evidence_for_feature") + " " +
        "AND afp.rank=0 " +
        "AND m.organism_id=" + organismId + " " +
        "AND p.organism_id=" + organismId + " " +
        "AND m.organism_id=p.organism_id " +
        "AND fm.object_id=f.feature_id " +
        "AND fm.subject_id=m.feature_id " +
        "AND mp.object_id=m.feature_id " +
        "AND mp.subject_id=p.feature_id " +
        "AND p.feature_id=af.feature_id " +
        "AND af.analysisfeature_id = afp.analysisfeature_id " +
        "AND p.dbxref_id=dbx.dbxref_id " +
        "AND dbx.db_id=db.db_id";

    LOG.info("executing getProteinFeatureResultSet(): " + pQuery);
    Statement stmt = converter.getDatabase().getConnection().createStatement();
    ResultSet res = stmt.executeQuery(pQuery);
    LOG.info("Got resultset.");

    int count=0;
    while(res.next()) {
      Integer proteinAnalysisFeatureId = res.getInt("protein_analysis_feature_id");
      Integer proteinFeatureId = res.getInt("protein_feature_id");
      String dbName = res.getString("db_name");
      // what the feature hits
      String proteinName = res.getString("protein_name");
      String proteinUniquename = res.getString("protein_uniquename");
      // the primary identifier we give to the feature
      String hitName = res.getString("match_name");
      // the thing the feature hits
      String accession = res.getString("accession");
      // the program that made the assignment
      String programName = res.getString("program_name");
      // min and max. convert to 1-indexed. SIGNALP is
      // a special case since it is a cleavage site.
      Integer fmin = "SIGNALP".equals(dbName)?
          res.getInt("fmin") - 1:
            res.getInt("fmin") + 1;
      Integer fmax = res.getInt("fmax");
      // assuming strand = 1
      Double rawScore = res.getDouble("rawscore");
      if (res.wasNull()) rawScore = null;
      Double normScore = res.getDouble("normscore");
      if (res.wasNull()) normScore = null;
      Double significance = res.getDouble("significance");
      if (res.wasNull()) significance = null;
      
      if( processAndStoreProteinAnalysisFeature(proteinAnalysisFeatureId,proteinFeatureId,
              hitName,proteinName,proteinUniquename,accession,dbName,programName,
              fmin,fmax,rawScore,normScore,significance) ) {
            count++;
          }
    }
    LOG.info("processed " + count + " ProteinAnalysisFeature records");
    res.close();
  }
  
  private boolean processAndStoreProteinAnalysisFeature(Integer matchId,
      Integer proteinId,String hitName, String proteinName, String proteinUniquename,
      String accession, String dbName,String programName,
      Integer fmin, Integer fmax,
      Double rawScore,Double normScore, Double significance)
          throws ObjectStoreException {

    if (converter.objectIdentifier(proteinId)==null ||
                 converter.objectIdentifier(matchId)!=null ) {
      return false;
    }
    
    Item feature = converter.createItem("ProteinAnalysisFeature");
    feature.setReference("organism", orgId);
    feature.setAttribute("primaryIdentifier", hitName);
    feature.setAttribute("secondaryIdentifier",proteinUniquename+":"+dbName);
    feature.setReference("protein", converter.objectIdentifier(proteinId));

    if (!dataSourceMap.containsKey(dbName)) {
      Item newItem = converter.createItem("DataSource");
      newItem.setAttribute("name", dbName);
      converter.store(newItem);
      dataSourceMap.put(dbName,newItem.getIdentifier());
      dataHitMap.put(dbName, new HashMap<String,Item> ());
    }
    String dataSourceIdentifier = dataSourceMap.get(dbName);
    feature.setReference("sourceDatabase",dataSourceIdentifier);

    if (!dataHitMap.get(dbName).containsKey(accession)) {
      Item newItem = converter.createItem("CrossReference");
      newItem.setAttribute("identifier",accession);
      newItem.setReference("source", dataSourceIdentifier);
      converter.store(newItem);
      dataHitMap.get(dbName).put(accession,newItem);
    }
    Item dataHitItem = dataHitMap.get(dbName).get(accession);
    feature.setReference("crossReference",dataHitItem);

    feature.setAttribute("programname",programName);
    if(normScore != null) {
      feature.setAttribute("normscore",normScore.toString());
    }
    if(rawScore != null) {
      feature.setAttribute("rawscore",rawScore.toString());
    }
    if(significance != null) {
      feature.setAttribute("significance",significance.toString());
    }
    Item location = converter.makeLocation(converter.objectIdentifier(proteinId),
        feature.getIdentifier(),fmin, fmax, new Integer(1));
    if (location != null) {
      converter.store(location);
    }
    converter.recordObject(matchId,converter.store(feature));
    converter.recordObject(matchId, feature.getIdentifier());    
    return true;
  }
  
  private void fillProperties() throws ObjectStoreException, SQLException {
    // and for the relationships between features
    StringBuilder hack = new StringBuilder();
    for( String type : PhytozomeDbConfig.getPropertyList("gene")) {
      if (hack.length() > 0 ) hack.append(",");
      hack.append(config.cvTerm("feature_property",type).toString());
    }   
    for( String type : PhytozomeDbConfig.getPropertyList("mrna")) {
      if (hack.length() > 0 ) hack.append(",");
      hack.append(config.cvTerm("feature_property",type).toString());
    }
    String query =
            "SELECT " +
            "f.type_id as feature_type_id," +
            "f.feature_id AS feature_id," +
            "p.type_id as property_type_id, " +
            "p.value as property, " +
            "p.rank as rank " +
            "FROM " +
            tempFeatureTableName + " f," +
            "featureprop p " +
            "WHERE f.feature_id=p.feature_id " +
            "AND p.type_id IN (" + 
            hack.toString() +
            ") ORDER by feature_id, property_type_id, rank";
            
    Statement stmt = converter.getDatabase().getConnection().createStatement();
    ResultSet res = stmt.executeQuery(query);
    int count = 0;
    while (res.next()) {
      String propertyType = config.cvTermInv(res.getInt("property_type_id"));
      String featureType = config.cvTermInv(res.getInt("feature_type_id"));
      Integer featureId = res.getInt("feature_id");
      String property = res.getString("property");
      Attribute a = new Attribute(PhytozomeDbConfig.getInterminePropertyName(propertyType),property);
      converter.store(a,converter.objectId(featureId));
    }
    LOG.info("created " + count + " relationships");
    res.close();
  
  }
  private void fillRelationships() throws ObjectStoreException, SQLException {
    // and for the relationships between features
    String query =
            "SELECT " +
            "s.type_id as subject_type_id," +
            "o.type_id AS object_type_id," +
            "r.subject_id, r.object_id " +
            "FROM feature_relationship r," +
            tempFeatureTableName + " s," +
            tempFeatureTableName + " o " +
            "WHERE o.feature_id=r.object_id " +
            "AND s.feature_id=r.subject_id " +
            "UNION " +
            "SELECT " +
            "s.type_id as subject_type_id, " +
            "o.type_id AS object_type_id, " +
            "r1.subject_id, r2.object_id " +
            "FROM feature_relationship r1, " +
            "feature_relationship r2, " +
            tempFeatureTableName + " s," +
            tempFeatureTableName + " i," +
            tempFeatureTableName + " o " +
            "WHERE o.feature_id=r2.object_id " +
            "AND i.feature_id=r2.subject_id " +
            "AND i.feature_id=r1.object_id " +
            "AND s.feature_id=r1.subject_id ";
            
    Statement stmt = converter.getDatabase().getConnection().createStatement();
    ResultSet res = stmt.executeQuery(query);
    int count = 0;
    // make a hash of things that go into collections. The MultiKey is 
    // objectId (number) and name of the collection.
    HashMap<MultiKey,ReferenceList> collections = 
        new HashMap<MultiKey,ReferenceList>();
    while (res.next()) {
      String objectType = config.cvTermInv(res.getInt("object_type_id"));
      String subjectType = config.cvTermInv(res.getInt("subject_type_id"));
      Integer objectId = res.getInt("object_id");
      Integer subjectId = res.getInt("subject_id");
      if( PhytozomeDbConfig.hasReference(subjectType,objectType)) {
        String refName = PhytozomeDbConfig.referenceName(subjectType, objectType);
        Reference r = new Reference(refName,converter.objectIdentifier(objectId));
        converter.store(r,converter.objectId(subjectId));
        LOG.info("Linked types "+objectType+" and "+subjectType+" ids "+objectId+","+subjectId +
        		" in aSubject-Object reference.");
        count++;
      }
      if( PhytozomeDbConfig.hasReference(objectType,subjectType)) {
        String refName = PhytozomeDbConfig.referenceName(objectType, subjectType);
        Reference r = new Reference(refName,converter.objectIdentifier(subjectId));
        converter.store(r,converter.objectId(objectId));
        LOG.info("Linked types "+objectType+" and "+subjectType+" ids "+objectId+","+subjectId +
            " in a Object-Subject reference.");
        count++;
      }
      
      if (PhytozomeDbConfig.hasCollection(objectType,subjectType)) {
        String refName = PhytozomeDbConfig.collectionName(objectType, subjectType);
        Reference r = new Reference(refName,converter.objectIdentifier(subjectId));
        MultiKey key = new MultiKey(converter.objectId(objectId),refName);
        if (!collections.containsKey(key)) collections.put(key, new ReferenceList(refName));
        collections.get(key).addRefId(r.getRefId());
        count++;
      }
      if (PhytozomeDbConfig.hasCollection(subjectType,objectType)) {
        String refName = PhytozomeDbConfig.collectionName(subjectType, objectType);
        Reference r = new Reference(refName,converter.objectIdentifier(objectId));
        MultiKey key = new MultiKey(converter.objectId(subjectId),refName);
        if (!collections.containsKey(key)) collections.put(key, new ReferenceList(refName));
        collections.get(key).addRefId(r.getRefId());
        count++;
      }
    }
    // Now store all the collections
    for(MultiKey key : collections.keySet()) {
      converter.store(collections.get(key),(Integer)key.getKey(0));
    }
    LOG.info("created " + count + " relationships");
    res.close();

  }
  private void fillAnnotationTable() throws ObjectStoreException, SQLException {

    String query =
        "CREATE TABLE " + tempFeatureTableName + " AS " +
            "SELECT f.feature_id, f.name, f.uniquename," +
            "f.type_id,f.seqlen," +
            "f.residues, md5(f.residues) as md5checksum," +
            "l.featureloc_id, l.srcfeature_id, l.fmin," +
            "l.is_fmin_partial, l.fmax, l.is_fmax_partial," +
            "l.strand " +
            "FROM feature f, " + 
            tempChromosomeTableName + " g, " +
            "featureloc l " +
            "WHERE f.type_id IN (" + 
            config.listString("sequence",converter.getFeatureTypes()) +
            ") " +
            "AND NOT f.is_obsolete " +
            "AND is_analysis = 'f' " +
            "AND l.srcfeature_id = g.feature_id " +
            "AND f.feature_id = l.feature_id " +
            "AND f.organism_id =" + organismId;
    
    Statement stmt = converter.getDatabase().getConnection().createStatement();
    LOG.info("Creating Feature temp table(): " + query);
    try {
      stmt.execute(query);
      LOG.info("Done with feature query.");
      String idIndexQuery = "CREATE INDEX " + tempFeatureTableName +
          "_feature_index ON " + tempFeatureTableName + "(feature_id)";
      LOG.info("executing: " + idIndexQuery);
      stmt.execute(idIndexQuery);
      String analyze = "ANALYZE " + tempFeatureTableName;
      LOG.info("executing: " + analyze);
      stmt.execute(analyze);
      LOG.info("Done with analyze.");
      LOG.info("Querying temp feature table.");
    } catch (SQLException e) {
      throw new BuildException("Trouble making annotation table: "+
          e.getMessage());
    }
    
    String selectQuery = "SELECT * FROM " + tempFeatureTableName;
    ResultSet res = stmt.executeQuery(selectQuery);
    int count = 0;
    while (res.next()) {
      Integer featureId = new Integer(res.getInt("feature_id"));
      String name = res.getString("name");
      String uniqueName = res.getString("uniquename");
      String chadoType = config.cvTermInv(res.getInt("type_id"));
      String checksum = res.getString("md5checksum");
      String residues = res.getString("residues");
      Integer seqlen = res.getInt("seqlen");
      Integer srcFeatureId = res.getInt("srcfeature_id");
      Integer fmin = res.getInt("fmin");
      Integer fmax = res.getInt("fmax");
      Integer strand = res.getInt("strand");
      Boolean fminPartial = res.getBoolean("is_fmin_partial");
      Boolean fmaxPartial = res.getBoolean("is_fmax_partial");
      
      String seqId = null;
      // override db value of sequence length if residues is set.
      if (residues != null && !residues.isEmpty()) {
        seqlen = residues.length();
        // and store
        Item seq = converter.createItem("Sequence");
        seq.setAttribute("residues", residues);
        seq.setAttribute("length",Integer.toString(seqlen));
        seq.setAttribute("md5checksum", checksum);
        seqId = seq.getIdentifier();
        converter.store(seq);
      }
      
      Item feat = converter.createItem(PhytozomeDbConfig.getIntermineType(chadoType));
      feat.setAttribute("primaryIdentifier", name);
      feat.setAttribute("secondaryIdentifier",uniqueName);
      feat.setAttribute("length",Integer.toString(seqlen));
      feat.setReference("organism", orgId);
      if (seqId != null) feat.setReference("sequence", seqId);
      converter.recordObject(featureId,converter.store(feat));
      converter.recordObject(featureId,feat.getIdentifier());
      
      Item location = converter.makeLocation(converter.objectIdentifier(srcFeatureId),
          feat.getIdentifier(),fmin, fmax, strand);
      if (location != null) {
        converter.store(location);
      }
      count++;
    }
    LOG.info("created " + count + " features");
    res.close();
    
  }
  
 
  
  private void fillChromosomeTable() throws ObjectStoreException, SQLException {
    String chromosomeTypeIdsString =
        PhytozomeDbConfig.getSQLString(config.getFeatures("chromosome"));
    String genomeTypeIdsString =
        PhytozomeDbConfig.getSQLString(config.getFeatures("genome"));
    String relationTypeIdsString =
        PhytozomeDbConfig.getSQLString(config.getRelations("contained"));

    String verifyGenomeQuery =
        "SELECT count(feature_id) FROM feature g WHERE "
            + "  g.type_id " + genomeTypeIdsString + " AND "
            + "  organism_id = " + organismId + " AND "
            + " dbxref_id = " + assemblyDbxrefId;
    Statement verifyStatement = converter.getDatabase().getConnection().createStatement();
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
        "CREATE TABLE " + tempChromosomeTableName + " AS"
            + " SELECT c.feature_id, c.name, c.uniquename,"
            + " c.seqlen, c.residues,"
            + " md5(c.residues) as md5checksum, c.organism_id"
            + " FROM "
            + " feature c, feature g, feature_relationship r"
            + " WHERE c.type_id " + chromosomeTypeIdsString
            + "  AND g.type_id " + genomeTypeIdsString
            + "  AND r.type_id " + relationTypeIdsString
            + "  AND r.object_id=g.feature_id"
            + "  AND r.subject_id=c.feature_id"
            + "  AND c.is_obsolete='f'"
            + "  AND c.dbxref_id="+ assemblyDbxrefId 
            + "  AND g.dbxref_id="+ assemblyDbxrefId
            + "  AND g.organism_id="+organismId
            + "  AND c.organism_id="+organismId;         

    Statement stmt = converter.getDatabase().getConnection().createStatement();
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
      throw new BuildException("Trouble making chromosome table: "+
          e.getMessage());
    }
    String selectQuery = "SELECT * FROM " + tempChromosomeTableName;
    ResultSet res = stmt.executeQuery(selectQuery);
    int count = 0;
    while (res.next()) {
      Integer featureId = new Integer(res.getInt("feature_id"));
      String name = res.getString("name");
      String uniqueName = res.getString("uniquename");
      String residues = res.getString("residues");
      String checksum = res.getString("md5checksum");
      int seqlen = 0;
      if (res.getObject("seqlen") != null) {
        seqlen = res.getInt("seqlen");
      }
      String seqId = null;
      if(seqlen > 0) {
        Item seq = converter.createItem("Sequence");
        seq.setAttribute("residues", residues);
        seq.setAttribute("length",Integer.toString(seqlen));
        seq.setAttribute("md5checksum", checksum);
        seqId = seq.getIdentifier();
        converter.store(seq);
      }
      Item chrom = converter.createItem("Chromosome");
      chrom.setAttribute("primaryIdentifier", name);
      chrom.setAttribute("secondaryIdentifier",uniqueName);
      chrom.setAttribute("length",Integer.toString(seqlen));
      chrom.setReference("organism", orgId);
      if (seqId != null) chrom.setReference("sequence", seqId);
      converter.store(chrom);
      converter.recordObject(featureId,chrom.getIdentifier());
      count++;
    }
    LOG.info("created " + count + " chromosomes");
    res.close();
    
  }
}