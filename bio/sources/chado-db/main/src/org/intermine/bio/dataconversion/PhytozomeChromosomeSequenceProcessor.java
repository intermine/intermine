package org.intermine.bio.dataconversion;

/*
 * 
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence. This should
 * be distributed with the code. See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.intermine.bio.chado.config.ConfigAction;
import org.intermine.bio.chado.config.CreateSynonymAction;
import org.intermine.bio.chado.config.DoNothingAction;
import org.intermine.bio.chado.config.SetFieldConfigAction;
import org.intermine.bio.util.OrganismData;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.model.bio.DataSource;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;

/**
 * A processor for the chromosomes and features stored in chado.
 * Originally based on the SequenceProcessor code by Kim Rutherford
 * (that code Copyright (C) 2002-2012 FlyMine)
 *
 * This is an after-the-fact patch for filling in chromosome sequences
 * that were skipped in the original processing.
 * 
 * @author Joe Carlson
 */
public class PhytozomeChromosomeSequenceProcessor extends ChadoProcessor
{

  // STATIC CONSTANTS RELATED TO CVTERM

  // default genome-like feature types - i.e. those types of features
  // that are the collections of chromosome-type features
  static final List<String> GENOME_FEATURES =
      Arrays.asList("genome");
  // default chromosome-like feature types - i.e. those types of features
  // that are the assembled objects of a sequencing project. Everything
  // will go into the 'chromosome' table in the mine
  static final List<String> CHROMOSOME_FEATURES =
      Arrays.asList("chromosome");
  // default feature types to query from the feature table
  static final List<String> ANNOTATION_FEATURES =
      Arrays.asList( "gene", "mRNA",
          "transcript", "polypeptide",
  //        "intron",
          "exon","CDS",
          "five_prime_untranslated_region",
          "five_prime_UTR",
          "three_prime_untranslated_region",
          "three_prime_UTR",
          "origin_of_replication");
  static final List<String> ANALYSIS_FEATURES =
      Arrays.asList("match","match_part","evidence_for_feature");
  // a list of the possible names for the relationship between the
  // different sequence types.
  // This is the relationship type(s) between chromosomes-like things
  // and genome-like things.
  //I'm using contained_in. Maybe someone will use part_of
  static final List<String> CONTAINED_IN_RELATIONS =
      Arrays.asList("contained_in");
  // The relationship(s) between an exon and an mrna and 
  // between an mrna and a gene
  static final List<String> PART_OF_RELATIONS =
      Arrays.asList("part_of");
  // The relationship(s) between a polypeptide and a transcript
  static final List<String> DERIVES_FROM_RELATIONS =
      Arrays.asList("derives_from");
  // featureprop's that we will want
  static final List<String> FEATURE_PROPERTIES = 
      Arrays.asList("synonym","old_locus_tag","description");

  // the prefix to use when making a temporary tables, the tempTableCount
  // will be added to make it unique
  static final String TEMP_CHROMOSOME_TABLE_NAME_PREFIX =
      "intermine_chado_chromosome_temp";
  static final String TEMP_FEATURE_TABLE_NAME_PREFIX =
      "intermine_chado_features_temp";

  // PRIVATE CLASS VARIABLES
  // a counter that is incremented each time we make a new processor
  // to make sure we have a unique name for temporary tables
  private static int tempTableCount = 0;

  protected static final Logger LOG =
      Logger.getLogger(PhytozomeProcessor.class);
  // the name of the temporary tables we create from the feature table
  // to speed up processing
  protected static String tempChromosomeTableName = null;

  // PRIVATE HASHMAPS

  // a map from chado feature id to FeatureData objects, populated
  // by processFeatureTable() and used to get object types, Item IDs
  // etc. (see FeatureData)
  protected static Map<Integer, FeatureData> featureMap =
      new HashMap<Integer, FeatureData>();
  // a hash of external db's that are hit by features (i.e. PFAM, Panter,...)
  protected static Map<String, Item> dataSourceMap = 
      new HashMap<String,Item>();
  // a hash of things in external db's. To prevent name collisions,
  // this is also indexed by the db
  protected static Map<String,Map<String,Item> > dataHitMap =
      new HashMap<String,Map<String,Item> >();
  // and the cvTerms. Indexed by CV, then term_name->cvterm_id
  protected static Map<String,Map<String,Integer> > cvTermMap =
      new HashMap<String,Map<String,Integer> >();
  protected static Map<Integer,String> cvTermInvMap =
      new HashMap<Integer,String>();


  // Avoid explosion of log messages by only logging missing collections once
  protected Set<String> loggedMissingCols = new HashSet<String>();

  // the configuration for this processor, set when getConfig()
  // is called the first time
  protected final Map<Integer, MultiKeyMap> config =
      new HashMap<Integer, MultiKeyMap>();

  /**
   * Create a new PhytozomeProcessor
   * @param chadoDBConverter the ChadoDBConverter that is controlling
   * this processor
   */
  public PhytozomeChromosomeSequenceProcessor(ChadoDBConverter chadoDBConverter) {
    super(chadoDBConverter);
    synchronized (this) {
      tempTableCount++;
      tempChromosomeTableName =
          TEMP_CHROMOSOME_TABLE_NAME_PREFIX + "_" + tempTableCount;

    }
    String query = "SELECT cv.name, cvterm.name, cvterm.cvterm_id"
        + " FROM  cv, cvterm "
        + " WHERE cvterm.cv_id=cv.cv_id "
        + " AND (( cv.name='sequence' AND cvterm.name in "
        + quoteList(GENOME_FEATURES,
            CHROMOSOME_FEATURES,
            ANNOTATION_FEATURES,
            ANALYSIS_FEATURES) 
            + " ) "
            + " OR ( cv.name='relationship' AND cvterm.name in "
            + quoteList(CONTAINED_IN_RELATIONS,
                PART_OF_RELATIONS,
                DERIVES_FROM_RELATIONS)
                + " ) "
                + " OR ( cv.name='feature_property' AND cvterm.name in "
                + quoteList(FEATURE_PROPERTIES)
                + " ))";

    LOG.info("executing cvterm query: " + query);
    ResultSet res = null;
    try {
      Statement stmt = chadoDBConverter.getConnection().createStatement();
      res = stmt.executeQuery(query);
    } catch (SQLException e) {
      throw new BuildException("Problem when querying CVTerms " + e);
    }
    LOG.info("got resultset.");
    cvTermMap.put("sequence",new HashMap<String,Integer>());
    cvTermMap.put("relationship",new HashMap<String,Integer>());
    cvTermMap.put("feature_property",new HashMap<String,Integer>());
    int counter = 0;
    try {
      while (res.next()) {
        counter++;
        cvTermMap.get(res.getString(1)).put(res.getString(2),
            new Integer(res.getInt(3)));
        cvTermInvMap.put(new Integer(res.getInt(3)), res.getString(2));
      }
    } catch (SQLException e) {
      throw new BuildException("Problem when querying CVTerms " + e);
    }

    // make sure we have the proper size
    if (counter != GENOME_FEATURES.size() + CHROMOSOME_FEATURES.size() +
        ANNOTATION_FEATURES.size() + CONTAINED_IN_RELATIONS.size() +
        PART_OF_RELATIONS.size() + DERIVES_FROM_RELATIONS.size() +
        FEATURE_PROPERTIES.size()) {
      LOG.warn("Did not get the expected number of terms from the CV table.");
    }
  }


  /**
   * {@inheritDoc}
   * We process the chado database by reading each table in turn
   * (feature, pub, featureloc, etc.) Each row of each table is read
   * and stored if appropriate.
   */
  @Override
  public void process(Connection connection) throws SQLException, ObjectStoreException {
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
  protected boolean storeFeature(Integer featureId, String uniqueName,
      String name, int seqlen, String residues,
      String md5checksum, String chadoType,
      Integer organismId)
          throws ObjectStoreException {
    // have we processed this already?
    if (featureMap.containsKey(featureId)) {
      return false;
    }

    // make the Chado object version
    FeatureData fdat =
        makeFeatureData(featureId.intValue(), chadoType, uniqueName,
            name, md5checksum, seqlen, organismId.intValue());

    if (fdat == null) {
      return false;
    }

    if (seqlen > 0) {
      setAttributeIfNotSet(fdat, "length", String.valueOf(seqlen));
    }
    ChadoDBConverter chadoDBConverter = getChadoDBConverter();

    String dataSourceName = chadoDBConverter.getDataSourceName();

    OrganismData orgData = fdat.getOrganismData();

    setAttributeIfNotSet(fdat,"primaryIdentifier", uniqueName);
    setAttributeIfNotSet(fdat,"secondaryIdentifier",name);
    //TODO check
    //if(chadoDBConverter.getVersion() != null) {
     // setAttribute(fdat.getIntermineObjectId(), "version", chadoDBConverter.getVersion());
    //}

    // set the BioEntity sequence if there is one
    if (fdat.checkField("sequence") && residues != null &&
        residues.length() > 0) {
      if (!fdat.getFlag("sequence")) {
        Item sequence = getChadoDBConverter().createItem("Sequence");
        sequence.setAttribute("residues", residues);
        sequence.setAttribute("length", new Integer(residues.length()).toString());
        Reference chrReference = new Reference();
        chrReference.setName("sequence");
        chrReference.setRefId(sequence.getIdentifier());
        getChadoDBConverter().store(chrReference,fdat.getIntermineObjectId());
        getChadoDBConverter().store(sequence);
        fdat.setFlag("sequence", true);
      }
    }
    featureMap.put(featureId, fdat);
    return true;
  }


  /**
   * Make a Location between a SequenceFeature and a Chromosome/Protein.
   * @param start the start position
   * @param end the end position
   * @param strand the strand
   * @param srcFeatureData the FeatureData for the src feature (Chromosome/Protein)
   * @param featureData the FeatureData for the SequenceFeature
   * @param taxonId the taxon id to use when finding the Chromosome/Protein
   * @return the new Location object
   * @throws ObjectStoreException if there is a problem while storing
   */
  protected Item storeLocation(int start, int end, int strand,
      FeatureData srcFeatureData, FeatureData featureData,
      int taxonId) throws ObjectStoreException {
    Item location = getChadoDBConverter().makeLocation(
        srcFeatureData.getItemIdentifier(),
        featureData.getItemIdentifier(),
        start, end, strand, taxonId);
    return location;
  }
  /**
   * Set the given attribute if the FeatureData says it's not set,
   * then set the flag in FeatureData to say it's set.
   */
  protected void setAttributeIfNotSet(FeatureData fdat,
      final String attributeName, final String value)
          throws ObjectStoreException {
    if (!fdat.getFlag(attributeName)) {
      setAttribute(fdat.getIntermineObjectId(), attributeName, value);
      fdat.setFlag(attributeName, true);
    }
  }

  /**
   * Create and store a new Item, returning a FeatureData object for
   * the feature.
   *
   * @param featureId the chado id from the feature table
   * @param chadoType the type of the feature from the feature + cvterm tables
   * @param uniqueName the uniquename from chado
   * @param name the name from chado
   * @param md5checksum the checksum from the chado feature able
   * @param seqlen the length from the feature table
   * @param organismId the organism id of the feature from chado
   * @return a FeatureData object
   * @throws ObjectStoreException if there is a problem while storing
   */
  protected FeatureData makeFeatureData(int featureId, String chadoType,
      String uniqueName, String name, String md5checksum, int seqlen,
      int organismId) throws ObjectStoreException {
    String interMineType = TypeUtil.javaiseClassName(fixFeatureType(chadoType));
    OrganismData organismData =
        getChadoDBConverter().getChadoIdToOrgDataMap().get(
            new Integer(organismId));

    Item feature = getChadoDBConverter().createItem(interMineType);
    if (feature == null) {
      return null;
    }
    int taxonId = organismData.getTaxonId();
    FeatureData fdat = new FeatureData();
    Item organismItem = getChadoDBConverter().getOrganismItem(taxonId);
    feature.setReference("organism", organismItem);
    if (feature.checkAttribute("md5checksum")) {
      feature.setAttribute("md5checksum", md5checksum);
    }
    BioStoreHook.setSOTerm(getChadoDBConverter(), feature, chadoType,
        getChadoDBConverter().getSequenceOntologyRefId());
    fdat.setFieldExistenceFlags(feature);

    fdat.setIntermineObjectId(getChadoDBConverter().store(feature));
    fdat.setItemIdentifier(feature.getIdentifier());
    fdat.setUniqueName(uniqueName);
    fdat.setChadoFeatureName(name);
    fdat.setInterMineType(feature.getClassName());
    fdat.organismData = organismData;
    fdat.setMd5checksum(md5checksum);
    return fdat;
  }

  /**
   * Get a list of the chado/so types of the Chromosome-like objects
   * we wish to load. (eg. "chromosome" and "chromosome_arm").
   * @return the list of features
   */
  protected List<String> getChromosomeFeatureTypes() {
    return CHROMOSOME_FEATURES;
  }

  protected String quoteList(List<String>...lists)
  {
    String quoted = "";
    for (List<String> list : lists) {
      for( String term : list ) {
        if (!quoted.isEmpty()) {
          quoted += ",";
        } else {
          quoted = "(";
        }
        quoted += "'" + term + "'";
      }
    }
    quoted += ")";
    return quoted;
  }

  /**
   * Perform any actions needed after all processing is finished.
   * @param connection the Connection
   * @param featureDataMap a map from chado feature_id to data for feature
   * @throws SQLException if there is a problem
   */
  protected void finishedProcessing(Connection connection,
      Map<Integer, FeatureData> featureDataMap)
          throws SQLException {
    // connection will be null for tests
    if (connection != null) {
      String query = "DROP TABLE " + tempChromosomeTableName;
      Statement stmt = connection.createStatement();
      LOG.info("executing: " + query);
      stmt.execute(query);
    }
  }

  /**
   * Convert the list of numbers to a string to be used in a SQL query.
   * @return the list of words as a string (in SQL list format), either
   * "=number" if only 1 number or "in (number1,number2,...) if multiple.
   */
  protected String getSQLString(List<Integer> numbers) {
    if (numbers.size() == 0) {
      // we don't expect this to happen. return 'is null'
      return " is null";
    } else if (numbers.size() == 1) {
      return "= " + numbers.get(0).toString();
    } else {
      StringBuffer wordString = new StringBuffer();
      Iterator<Integer> i = numbers.iterator();
      while (i.hasNext()) {
        Integer item = i.next();
        wordString.append(item.toString());
        if (i.hasNext()) {
          wordString.append(", ");
        }
      }
      return wordString.toString();
    }
  }

  /**
   * Return a comma separated string containing the organism_ids that
   * with with to query from chado.
   */
  protected String getOrganismIdsString() {
    return StringUtil.join(
        getChadoDBConverter().getChadoIdToOrgDataMap().keySet(), ", ");
  }

  /**
   * Return some SQL that can be included in the WHERE part of query
   * that restricts features by organism in which the "organism_id"
   * must be selected.
   * @return the SQL
   */
  protected String getOrganismConstraint() {
    String organismIdsString = getOrganismIdsString();
    if (StringUtils.isEmpty(organismIdsString)) {
      return "'true'";
    }
    return "organism_id IN (" + organismIdsString + ")";
  }
  /**
   * a variant of the previous in which a table alias is supplied
   * @return the SQL
   */
  protected String getOrganismConstraint(String alias) {
    String organismIdsString = getOrganismIdsString();
    if (StringUtils.isEmpty(organismIdsString)) {
      return "'true'";
    }
    return alias + "." + getOrganismConstraint();
  }

  /**
   * Return an extra constraint to be used when querying the feature
   * table. Any feature table column or cvterm table column can
   * be constrained. The cvterm will match the type_id field in
   * the feature.
   * eg. "uniquename not like 'BAD_ID%'"
   * @return the constraint as SQL or nul if there is no extra constraint.
   */
  protected String getExtraFeatureConstraint() {
    // no default
    return null;
  }

  /**
   * @param string
   * @return
   */
  protected String makeOrConstraints(String fieldName, List<String> values) {
    List<String> bits = new ArrayList<String>();
    for (String partOf: values) {
      bits.add(fieldName + " = '" + partOf + "'");
    }
    return StringUtil.join(bits, " OR ");
  }

  /**
   * Create and store a new Item, returning a FeatureData object for
   * the feature.
   *
   * @param featureId the chado id from the feature table
   * @param chadoType the type of the feature from the feature + cvterm tables
   * @param uniqueName the uniquename from chado
   * @param name the name from chado
   * @param md5checksum the checksum from the chado feature able
   * @param seqlen the length from the feature table
   * @param organismId the organism id of the feature from chado
   * @return a FeatureData object
   * @throws ObjectStoreException if there is a problem while storing
   */
  protected FeatureData makeProteinAnalysisFeatureData(int featureId,String hitName,
      FeatureData proteinData, String accession,String dbName,
      String programName, int organismId, Double rawScore,
      Double normScore, Double significance) throws ObjectStoreException {
    String interMineType = TypeUtil.javaiseClassName("ProteinAnalysisFeature");
    OrganismData organismData =
        getChadoDBConverter().getChadoIdToOrgDataMap().get(
            new Integer(organismId));
    Item feature = getChadoDBConverter().createItem(interMineType);
    if (feature == null) {
      return null;
    }

    int taxonId = organismData.getTaxonId();
    FeatureData fdat = new FeatureData();
    Item organismItem = getChadoDBConverter().getOrganismItem(taxonId);
    feature.setReference("organism", organismItem);
    feature.setAttribute("primaryIdentifier", hitName);
    feature.setAttribute("name",proteinData.getChadoFeatureName() + ":" + dbName);
    feature.setReference("protein", proteinData.getItemIdentifier());

    if (!dataSourceMap.containsKey(dbName)) {
      Item newItem = getChadoDBConverter().createItem("DataSource");
      newItem.setAttribute("name", dbName);
      getChadoDBConverter().store(newItem);
      dataSourceMap.put(dbName,newItem);
      dataHitMap.put(dbName, new HashMap<String,Item> ());
    }
    Item dataSourceItem = dataSourceMap.get(dbName);

    if (!dataHitMap.get(dbName).containsKey(accession)) {
      Item newItem = getChadoDBConverter().createItem("CrossReference");
      newItem.setAttribute("identifier",accession);
      newItem.setReference("source", dataSourceItem);
      getChadoDBConverter().store(newItem);
      dataHitMap.get(dbName).put(accession,newItem);
    }
    Item dataHitItem = dataHitMap.get(dbName).get(accession);
    feature.setReference("crossReference",dataHitItem);

    feature.setReference("sourceDatabase",dataSourceItem);
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
    fdat.setIntermineObjectId(getChadoDBConverter().store(feature));
    fdat.setUniqueName(hitName);
    fdat.setItemIdentifier(feature.getIdentifier());
    fdat.setChadoFeatureName(accession);
    fdat.setInterMineType(feature.getClassName());
    fdat.organismData = organismData;
    return fdat;
  }
  /**
   * Return the config Map.
   * @param taxonId return the configuration for this organism
   * @return the Map from configuration key to a list of actions
   */
  @SuppressWarnings("unchecked")
  protected Map<MultiKey, List<ConfigAction>> getConfig(int taxonId) {
    MultiKeyMap map = config.get(new Integer(taxonId));
    if (map == null) {
      map = new MultiKeyMap();
      config.put(new Integer(taxonId), map);
      map.put(new MultiKey("relationship", "Gene", "derives_from", "Protein"),
          Arrays.asList(new SetFieldConfigAction("proteins")));
      // stat of copied part

      // synomym configuration example: for features of class "Gene", if the type name of
      // the synonym is "fullname" and "is_current" is true, set the "name" attribute of
      // the new Gene to be this synonym and then make a Synonym object
      map.put(new MultiKey("synonym", "Gene", "fullname", Boolean.TRUE),
          Arrays.asList(new SetFieldConfigAction("name")));
      map.put(new MultiKey("synonym", "Gene", "fullname", Boolean.FALSE),
          Arrays.asList(new CreateSynonymAction()));
      map.put(new MultiKey("synonym", "Gene", "symbol", Boolean.TRUE),
          Arrays.asList(new SetFieldConfigAction("symbol")));
      map.put(new MultiKey("synonym", "Gene", "symbol", Boolean.FALSE),
          Arrays.asList(new CreateSynonymAction()));

      // Set the protein reference in the MRNA - "rev_relationship" means that the
      // relationship table actually has Protein, producedby, MRNA.  We configure like
      // this so we can set a reference in MRNA rather than protein
      map.put(new MultiKey("rev_relationship", "MRNA", "producedby", "Protein"),
          Arrays.asList(new SetFieldConfigAction("protein")));

      // featureprop configuration example: for features of class "Gene", if the type name
      // of the prop is "cyto_range", set the "cytoLocation" attribute of the
      // new Gene to be this property
      map.put(new MultiKey("prop", "Gene", "cyto_range"),
          Arrays.asList(new SetFieldConfigAction("cytoLocation")));
      map.put(new MultiKey("prop", "Gene", "symbol"),
          Arrays.asList(new CreateSynonymAction()));

    }
    return map;
  }
  /**
   * Fix types from the feature table, perhaps by changing non-SO type
   * into their SO equivalent. Types that don't need fixing will be
   * returned unchanged.
   * @param type the input type
   * @return the fixed type
   */
  protected String fixFeatureType(String type) {
    if ("five_prime_untranslated_region".equals(type)) {
      return "five_prime_UTR";
    }
    if ("three_prime_untranslated_region".equals(type)) {
      return "three_prime_UTR";
    }
    if ("full_transcript".equals(type)) {
      return "mature_transcript";
    }
    if ("polypeptide".equals(type)) {
      return "protein";
    }
    return type;
  }
  /**
   * Convert the list of features to a string to be used in a SQL query.
   * @return the list of features as a string (in SQL list format)
   */
  protected String getFeaturesString(List<String> featuresList) {
    StringBuffer featureListString = new StringBuffer();
    Iterator<String> i = featuresList.iterator();
    while (i.hasNext()) {
      String item = i.next();
      featureListString.append("'" + item + "'");
      if (i.hasNext()) {
        featureListString.append(", ");
      }
    }
    return featureListString.toString();
  }

  /**
   * Get a list of the chado/so types of the SequenceFeatures we wish
   * to load. The list depends on the 'type' * @return the list of
 features */
  protected List<String> getFeatureList(String type) {
    return "genome".equals(type)?GENOME_FEATURES:
      "chromosome".equals(type)?CHROMOSOME_FEATURES:
        "annotation".equals(type)?ANNOTATION_FEATURES:
          null;
  }
  protected List<Integer> getFeatures(String type) {
    List<String> rStrings = getFeatureList(type);
    List<Integer> rIntegers = new ArrayList<Integer>();
    for( String r : rStrings) {
      if (cvTermMap.get("sequence").containsKey(r)) {
        rIntegers.add(cvTermMap.get("sequence").get(r));
      };
    }
    return rIntegers;
  }

  protected List<String> getRelationList(String type) {
    return "contained".equals(type)?CONTAINED_IN_RELATIONS:
      "part_of".equals(type)?PART_OF_RELATIONS:
        "derives_from".equals(type)?DERIVES_FROM_RELATIONS:
          null;
  }
  protected List<Integer> getRelations(String type) {
    List<String> rStrings = getRelationList(type);
    List<Integer> rIntegers = new ArrayList<Integer>();
    for( String r : rStrings) {
      if(cvTermMap.get("relationship").containsKey(r)) {
        rIntegers.add(cvTermMap.get("relationship").get(r));
      }
    }
    return rIntegers;
  }
}
