package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.bio.chado.ChadoCV;
import org.intermine.bio.chado.ChadoCVFactory;
import org.intermine.bio.chado.ChadoCVTerm;
import org.intermine.bio.chado.config.ConfigAction;
import org.intermine.bio.chado.config.CreateSynonymAction;
import org.intermine.bio.chado.config.SetFieldConfigAction;
import org.intermine.bio.util.OrganismData;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.IntPresentSet;
import org.intermine.util.XmlUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

/**
 * A converter for chado that handles FlyBase specific configuration.
 * @author Kim Rutherford
 */
public class FlyBaseProcessor extends SequenceProcessor
{
    /**
     * The cv.name for the wild type class term.  For chromosome_structure_variations, used to
     * identify the "Feature type" from the "Class of aberration" section of a FlyBase aberation
     * page.
     */
    private static final String WT_CLASS_CVTERM = "wt_class";

    private static final String FLYBASE_DB_NAME = "FlyBase";

    /**
     * The cv.name for the FlyBase miscellaneous CV.
     */
    protected static final String FLYBASE_MISCELLANEOUS_CV = FLYBASE_DB_NAME + " miscellaneous CV";

    /**
     * The cv.name for the FlyBase miscellaneous CV.
     */
    protected static final String FLYBASE_SO_CV_NAME = "SO";

    private static final String FLYBASE_ANATOMY_TERM_PREFIX = "FBbt";

    // a pattern the matches attribute stored in FlyBase properties, eg. "@FBcv0000289:hypomorph@"
    private static final String FLYBASE_PROP_ATTRIBUTE_PATTERN = "@([^@]+)@";

    // interactions use this - UKNOWN
    private static final String RELATIONSHIP_TYPE = "MI:0499";
    private static final String DEFAULT_ROLE = "unspecified";

    /**
     * A ConfigAction that overrides processValue() to change FlyBase attribute tags
     * (like "@FBcv0000289:hypomorph@") to text like: "hypomorph"
     * @author Kim Rutherford
     */
    private class AlleleClassSetFieldAction extends SetFieldConfigAction
    {
        /**
         * Create a new AlleleClassSetFieldAction
         * @param fieldName the fieldName to process with this object.
         */
        AlleleClassSetFieldAction(String fieldName) {
            super(fieldName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String processValue(String value) {
            Pattern p = Pattern.compile(FLYBASE_PROP_ATTRIBUTE_PATTERN);
            Matcher m = p.matcher(value);
            StringBuffer sb = new StringBuffer();

            while (m.find()) {
                String field = m.group(1);
                int colonPos = field.indexOf(':');
                if (colonPos == -1) {
                    m.appendReplacement(sb, field);
                } else {
                    String text = field.substring(colonPos + 1);
                    m.appendReplacement(sb, text);
                }
            }
            m.appendTail(sb);
            return sb.toString();
        }

    }

    private static final Logger LOG = Logger.getLogger(FlyBaseProcessor.class);

    // the configuration for this processor, set when getConfig() is called the first time
    private final Map<Integer, MultiKeyMap> config = new HashMap<Integer, MultiKeyMap>();

    // a set of feature_ids for those genes that have a location in the featureloc table, set by
    // the constructor
    private final IntPresentSet locatedGeneIds;

    // a map from the uniquename of each allele to its item identifier
    private Map<String, String> alleleIdMap = new HashMap<String, String>();

    // a map from the uniquename of each cdna clone to its item identifier
    private Map<String, FeatureData> cdnaCloneMap = new HashMap<String, FeatureData>();

    // an object representing the FlyBase miscellaneous CV
    private ChadoCV flyBaseMiscCv = null;

    // an object representing the sequence ontology, as stored in the FlyBase chado database
    private ChadoCV sequenceOntologyCV = null;

    // a map from mutagen description to Mutagen Item identifier
    private Map<String, String> mutagensMap = new HashMap<String, String>();

    // a map from featureId to seqlen
//    private Map<Integer, Integer> cdnaLengths = null;

    private final Map<Integer, Integer> chromosomeStructureVariationTypes;

    private Map<String, String> interactionExperiments = new HashMap<String, String>();

    private static final String LOCATED_GENES_TEMP_TABLE_NAME = "intermine_located_genes_temp";
    private static final String ALLELE_TEMP_TABLE_NAME = "intermine_flybase_allele_temp";
    private static final String INSERTION_TEMP_TABLE_NAME = "intermine_flybase_insertion_temp";

    // pattern to match the names of Exelixis insertions
    //  - matches "f07705" in "PBac{WH}f07705"
    //  - matches "f07705" in "PBac{WH}tam[f07705]"
    private static final Pattern PB_INSERTION_PATTERN =
        Pattern.compile(".*\\{.*\\}(?:.*\\[)?([def]\\d+)(?:\\])?");

    private static final Map<String, String> CHROMOSOME_STRUCTURE_VARIATION_SO_MAP
        = new HashMap<String, String>();
    private final Map<String, FeatureData> proteinFeatureDataMap
        = new HashMap<String, FeatureData>();

    static {
        CHROMOSOME_STRUCTURE_VARIATION_SO_MAP.put("chromosomal_deletion",
                                                  "ChromosomalDeletion");
        CHROMOSOME_STRUCTURE_VARIATION_SO_MAP.put("chromosomal_duplication",
                                                  "ChromosomalDuplication");
        CHROMOSOME_STRUCTURE_VARIATION_SO_MAP.put("chromosomal_inversion",
                                                  "ChromosomalInversion");
        CHROMOSOME_STRUCTURE_VARIATION_SO_MAP.put("chromosomal_translocation",
                                                  "ChromosomalTranslocation");
        CHROMOSOME_STRUCTURE_VARIATION_SO_MAP.put("transposition",
                                                  "ChromosomalTransposition");
    }

    private static final String CHROMOSOME_STRUCTURE_VARIATION_SO_NAME =
        "chromosome_structure_variation";

    /**
     * Create a new FlyBaseChadoDBConverter.
     * @param chadoDBConverter the converter that created this object
     */
    public FlyBaseProcessor(ChadoDBConverter chadoDBConverter) {
        super(chadoDBConverter);
        Connection connection = getChadoDBConverter().getConnection();

        try {
            flyBaseMiscCv = getFlyBaseMiscCV(connection);
        } catch (SQLException e) {
            throw new RuntimeException("can't execute query for flybase cv terms", e);
        }

        try {
            sequenceOntologyCV = getFlyBaseSequenceOntologyCV(connection);
        } catch (SQLException e) {
            throw new RuntimeException("can't execute query for so cv terms", e);
        }

        try {
            createLocatedGenesTempTable(connection);
        } catch (SQLException e) {
            throw new RuntimeException("can't execute query for located genes", e);
        }

        locatedGeneIds = getLocatedGeneIds(connection);

        chromosomeStructureVariationTypes = getChromosomeStructureVariationTypes(connection);


//        try {
//            cdnaLengths = makeCDNALengthMap(connection);
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
    }

    /**
     * @param connection database connection
     * @return map of feature_id to seqlen
     */
//    protected Map<Integer, Integer> getLengths(Connection connection) {
//        if (cdnaLengths == null) {
//            try {
//                cdnaLengths = makeCDNALengthMap(connection);
//            } catch (SQLException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//        return cdnaLengths;
//    }

    /**
     * Return a map from chromosome_structure_variation feature_ids to the cvterm_id of the
     * associated cvtermprop.  This is needed because the exact type of the
     * chromosome_structure_variation objects is not used as the type_id of the feature, instead
     * it's stored in the cvtermprop table.
     */
    private  Map<Integer, Integer> getChromosomeStructureVariationTypes(Connection connection) {
        Map<Integer, Integer> retVal = new HashMap<Integer, Integer>();
        ResultSet res;
        try {
            res = getChromosomeStructureVariationResultSet(connection);
        } catch (SQLException e) {
            throw new RuntimeException("can't execute query for chromosome_structure_variation "
                                       + "types", e);
        }

        try {
            while (res.next()) {
                int featureId = res.getInt("feature_id");
                int cvtermId = res.getInt("cvterm_id");
                retVal.put(new Integer(featureId), new Integer(cvtermId));
            }
        } catch (SQLException e) {
            throw new RuntimeException("problem while reading chromosome_structure_variation "
                                       + "types", e);
        }

        return retVal;
    }

    /**
     * Return the results of running a query for the chromosome_structure_variation feature types.
     * @param connection the connection
     * @return the results
     * @throws SQLException if there is a database problem
     */
    protected ResultSet getChromosomeStructureVariationResultSet(Connection connection)
        throws SQLException {
        String query =
            "  SELECT feature.feature_id, cvterm.cvterm_id"
            + "  FROM feature, feature_cvterm, cvterm feature_type, cvterm, cv,"
            + "       feature_cvtermprop, cvterm prop_term"
            + " WHERE feature.type_id = feature_type.cvterm_id"
            + "   AND feature_type.name = '" + CHROMOSOME_STRUCTURE_VARIATION_SO_NAME + "' "
            + "   AND feature_cvterm.feature_id = feature.feature_id"
            + "   AND feature_cvterm.cvterm_id = cvterm.cvterm_id AND cvterm.cv_id = cv.cv_id"
            + "   AND cv.name = 'SO' "
            + "   AND feature_cvtermprop.feature_cvterm_id = feature_cvterm.feature_cvterm_id"
            + "   AND feature_cvtermprop.type_id = prop_term.cvterm_id AND prop_term.name = '"
            + WT_CLASS_CVTERM + "'";

        LOG.info("executing getChromosomeStructureVariationResultSet(): " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Return a set of ids of those genes that have a location in the featureloc table.
     */
    private IntPresentSet getLocatedGeneIds(Connection connection) {
        IntPresentSet retVal = new IntPresentSet();
        ResultSet res;
        try {
            res = getLocatedGenesResultSet(connection);
        } catch (SQLException e) {
            throw new RuntimeException("can't execute query for located genes", e);
        }

        try {
            while (res.next()) {
                int featureId = res.getInt("feature_id");
                retVal.set(featureId, true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("problem while reading located genes", e);
        }

        return retVal;
    }


    /**
     * Create a temporary table containing the ids of the located genes.  This is a protected
     * method so that it can be overridden for testing
     * @param connection the Connection
     * @throws SQLException if there is a database problem
     */
    protected void createLocatedGenesTempTable(Connection connection) throws SQLException {
        String organismConstraint = getOrganismConstraint();
        String orgConstraintForQuery = "";
        if (!StringUtils.isEmpty(organismConstraint)) {
            orgConstraintForQuery = " AND " + organismConstraint;
        }

        String query = "CREATE TEMPORARY TABLE " + LOCATED_GENES_TEMP_TABLE_NAME
            + " AS SELECT feature.feature_id FROM feature, cvterm"
            + "     WHERE feature.type_id = cvterm.cvterm_id"
            + "       AND cvterm.name = 'gene' "
            + "       AND NOT feature.is_obsolete "
            + "       AND feature.feature_id IN "
            + "          (SELECT l.feature_id "
            + "           FROM featureloc l, feature c "
            + "           WHERE l.srcfeature_id = c.feature_id and NOT c.is_obsolete)"
            + orgConstraintForQuery;

        Statement stmt = connection.createStatement();
        LOG.info("executing createLocatedGenesTempTable(): " + query);
        stmt.execute(query);
        String idIndexQuery = "CREATE INDEX " + LOCATED_GENES_TEMP_TABLE_NAME + "_feature_index ON "
            + LOCATED_GENES_TEMP_TABLE_NAME + "(feature_id)";
        LOG.info("executing: " + idIndexQuery);
        stmt.execute(idIndexQuery);
        String analyze = "ANALYZE " + LOCATED_GENES_TEMP_TABLE_NAME;
        LOG.info("executing: " + analyze);
        stmt.execute(analyze);
    }

    /**
     * Create a temporary table of allele feature_ids.  The table will only have allele of genes
     * with locations.
     * @param connection the connection
     * @throws SQLException if there is a database problem
     */
    protected void createAllelesTempTable(Connection connection) throws SQLException {
        String organismConstraint = getOrganismConstraint();
        String orgConstraintForQuery = "";
        if (!StringUtils.isEmpty(organismConstraint)) {
            orgConstraintForQuery = " AND " + organismConstraint;
        }

        String query =
            " CREATE TEMPORARY TABLE " + ALLELE_TEMP_TABLE_NAME
            + " AS SELECT feature_id"
            + " FROM feature, cvterm feature_type "
            + " WHERE feature_type.name = 'gene'"
            + " AND type_id = feature_type.cvterm_id"
            + " AND uniquename LIKE 'FBal%'"
            + " AND NOT feature.is_obsolete"
            + " AND feature_id IN (SELECT feature_id FROM feature WHERE "
            + getLocatedGeneAllesSql() + ")"
            + orgConstraintForQuery;

        Statement stmt = connection.createStatement();
        LOG.info("executing createAllelesTempTable(): " + query);
        stmt.execute(query);
        String idIndexQuery = "CREATE INDEX " + ALLELE_TEMP_TABLE_NAME + "_feature_index ON "
            + ALLELE_TEMP_TABLE_NAME + "(feature_id)";
        LOG.info("executing: " + idIndexQuery);
        stmt.execute(idIndexQuery);
        String analyze = "ANALYZE " + ALLELE_TEMP_TABLE_NAME;
        LOG.info("executing: " + analyze);
        stmt.execute(analyze);
    }

    /**
     * Create a temporary table from pairs of insertions (eg. "FBti0027974" => "FBti0023081")
     * containing the feature_ids of the  pair (the object_id, subject_id in the relation table)
     * and the fmin and fmax of the first insertion in the pair (ie. the progenitor / object from
     * the feature_relationship table).
     * The second in the pair is the "Modified descendant of" the first.  The pairs are found using
     * the 'modified_descendant_of' relation type.  All insertions are from DrosDel.
     * @param connection the connection
     * @throws SQLException if there is a database problem
     */
    protected void createInsertionTempTable(Connection connection) throws SQLException {
        String query =
            " CREATE TEMPORARY TABLE " + INSERTION_TEMP_TABLE_NAME
            + " AS SELECT obj.feature_id AS obj_id, sub.feature_id AS sub_id,"
            + "       obj_loc.fmin, obj_loc.fmax,"
            + "       obj_loc.srcfeature_id as chr_feature_id"
            + "  FROM feature sub, cvterm sub_type, feature_relationship rel, cvterm rel_type, "
            + "       feature obj, cvterm obj_type, featureloc obj_loc"
            + " WHERE sub.feature_id = rel.subject_id AND rel.object_id = obj.feature_id"
            + "   AND sub_type.cvterm_id = sub.type_id AND obj_type.cvterm_id = obj.type_id"
            + "   AND sub_type.name = 'transposable_element_insertion_site' "
            + "   AND obj_type.name = 'transposable_element_insertion_site' "
            + "   AND rel.type_id = rel_type.cvterm_id"
            + "   AND rel_type.name = 'modified_descendant_of'"
            + "   AND sub.feature_id in (select feature_id from feature_pub where pub_id ="
            + "      (SELECT pub_id FROM pub"
            + "       WHERE title = "
            + "'The DrosDel collection: a set of P-element insertions for "
            + "generating custom chromosomal aberrations in Drosophila melanogaster.')) "
            + "   AND obj.feature_id = obj_loc.feature_id";

        Statement stmt = connection.createStatement();
        LOG.info("executing createInsertionTempTable(): " + query);
        stmt.execute(query);
        String idIndexQuery = "CREATE INDEX " + INSERTION_TEMP_TABLE_NAME + "index ON "
            + INSERTION_TEMP_TABLE_NAME + "(sub_id)";
        LOG.info("executing: " + idIndexQuery);
        stmt.execute(idIndexQuery);
        String analyze = "ANALYZE " + INSERTION_TEMP_TABLE_NAME;
        LOG.info("executing: " + analyze);
        stmt.execute(analyze);
    }

    /**
     * Get ChadoCV object representing the FlyBase misc cv.
     * This is a protected method so that it can be overriden for testing
     * @param connection the database Connection
     * @return the cv
     * @throws SQLException if there is a database problem
     */
    protected ChadoCV getFlyBaseMiscCV(Connection connection) throws SQLException {
        ChadoCVFactory cvFactory = new ChadoCVFactory(connection);
        return cvFactory.getChadoCV(FLYBASE_MISCELLANEOUS_CV);
    }

    /**
     * Get ChadoCV object representing SO from FlyBase.
     * This is a protected method so that it can be overriden for testing
     * @param connection the database Connection
     * @return the cv
     * @throws SQLException if there is a database problem
     */
    protected ChadoCV getFlyBaseSequenceOntologyCV(Connection connection) throws SQLException {
        ChadoCVFactory cvFactory = new ChadoCVFactory(connection);
        return cvFactory.getChadoCV(FLYBASE_SO_CV_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Integer store(Item feature, int taxonId) throws ObjectStoreException {
        processItem(feature, new Integer(taxonId));
        Integer itemId = super.store(feature, taxonId);
        return itemId;
    }

    /**
     * note: featureId is needed only by modMine
     * {@inheritDoc}
     */
    @Override
    protected Item makeLocation(int start, int end, int strand, FeatureData srcFeatureData,
                              FeatureData featureData, int taxonId, int featureId)
        throws ObjectStoreException {
        Item location =
            super.makeLocation(start, end, strand, srcFeatureData, featureData, taxonId, 0);
        processItem(location, new Integer(taxonId));
        return location;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Item createSynonym(FeatureData fdat, String identifier)
        throws ObjectStoreException {
        Item synonym = super.createSynonym(fdat, identifier);
        /* synonym can be null if it's been created earlier.  this would happen only if
         * the synonym was created when another protein was created in favour of this one.  */
        if (synonym != null) {
            OrganismData od = fdat.getOrganismData();
            processItem(synonym, new Integer(od.getTaxonId()));
        }
        return synonym;
    }

    /**
     * Return from chado the feature_ids of the genes with entries in the featureloc table.
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getLocatedGenesResultSet(Connection connection) throws SQLException {
        String query = getLocatedGenesSql();
        LOG.info("executing getLocatedGenesResultSet(): " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Return a query that gets the feature_ids of genes that have locations.
     */
    private static String getLocatedGenesSql() {
        return "SELECT feature_id FROM " + LOCATED_GENES_TEMP_TABLE_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<MultiKey, List<ConfigAction>> getConfig(int taxonId) {
        MultiKeyMap map = config.get(new Integer(taxonId));
        if (map == null) {
            map = new MultiKeyMap();
            config.put(new Integer(taxonId), map);

            // synomym configuration example: for features of class "Gene", if the type name of
            // the synonym is "fullname" and "is_current" is true, set the "name" attribute of
            // the new Gene to be this synonym and then make a Synonym object
            map.put(new MultiKey("synonym", "Gene", "fullname", Boolean.TRUE),
                    Arrays.asList(new SetFieldConfigAction("name")));
            map.put(new MultiKey("synonym", "Gene", "fullname", Boolean.FALSE),
                    Arrays.asList(CREATE_SYNONYM_ACTION));
            map.put(new MultiKey("synonym", "Gene", "symbol", Boolean.TRUE),
                    Arrays.asList(new SetFieldConfigAction("symbol")));
            map.put(new MultiKey("synonym", "Gene", "symbol", Boolean.FALSE),
                    Arrays.asList(CREATE_SYNONYM_ACTION));


            // dbxref table configuration example: for features of class "Gene", where the
            // db.name is "FlyBase Annotation IDs" and "is_current" is true, set the
            // "secondaryIdentifier" attribute of the new Gene to be this dbxref and then make a
            // Synonym object
            map.put(new MultiKey("dbxref", "Gene", FLYBASE_DB_NAME + " Annotation IDs",
                                 Boolean.TRUE),
                    Arrays.asList(new SetFieldConfigAction("secondaryIdentifier")));
            map.put(new MultiKey("dbxref", "Gene", FLYBASE_DB_NAME + " Annotation IDs",
                                 Boolean.FALSE),
                    Arrays.asList(CREATE_SYNONYM_ACTION));
            // null for the "is_current" means either TRUE or FALSE is OK.

            map.put(new MultiKey("dbxref", "Gene", FLYBASE_DB_NAME, null),
                    Arrays.asList(CREATE_SYNONYM_ACTION));

            map.put(new MultiKey("dbxref", "MRNA", FLYBASE_DB_NAME + " Annotation IDs",
                                 Boolean.TRUE),
                    Arrays.asList(new SetFieldConfigAction("secondaryIdentifier")));
            map.put(new MultiKey("dbxref", "TransposableElementInsertionSite", "drosdel", null),
                    Arrays.asList(new SetFieldConfigAction("symbol")));

            map.put(new MultiKey("synonym", "ChromosomeStructureVariation", "fullname",
                                 Boolean.TRUE),
                    Arrays.asList(new SetFieldConfigAction("name")));
            map.put(new MultiKey("synonym", "ChromosomalDeletion", "fullname", Boolean.TRUE),
                    Arrays.asList(new SetFieldConfigAction("name")));
            map.put(new MultiKey("synonym", "ChromosomalDuplication", "fullname", Boolean.TRUE),
                    Arrays.asList(new SetFieldConfigAction("name")));
            map.put(new MultiKey("synonym", "ChromosomalInversion", "fullname", Boolean.TRUE),
                    Arrays.asList(new SetFieldConfigAction("name")));
            map.put(new MultiKey("synonym", "ChromosomalTranslocation", "fullname", Boolean.TRUE),
                    Arrays.asList(new SetFieldConfigAction("name")));
            map.put(new MultiKey("synonym", "ChromosomalTransposition", "fullname", Boolean.TRUE),
                    Arrays.asList(new SetFieldConfigAction("name")));

            map.put(new MultiKey("synonym", "MRNA", "symbol", Boolean.TRUE),
                    Arrays.asList(new SetFieldConfigAction("symbol")));
            map.put(new MultiKey("synonym", "MRNA", "symbol", Boolean.FALSE),
                    Arrays.asList(CREATE_SYNONYM_ACTION));
            map.put(new MultiKey("dbxref", "MRNA", FLYBASE_DB_NAME + " Annotation IDs", null),
                    Arrays.asList(CREATE_SYNONYM_ACTION));
            map.put(new MultiKey("dbxref", "MRNA", FLYBASE_DB_NAME, null),
                    Arrays.asList(CREATE_SYNONYM_ACTION));

            // set the Allele.gene when there is an alleleof relationship between Allele and Gene
            map.put(new MultiKey("relationship", "Allele", "alleleof", "Gene"),
                    Arrays.asList(new SetFieldConfigAction("gene")));

            // Set the protein reference in the MRNA - "rev_relationship" means that the
            // relationship table actually has Protein, producedby, MRNA.  We configure like
            // this so we can set a reference in MRNA rather than protein
            map.put(new MultiKey("rev_relationship", "MRNA", "producedby", "Protein"),
                    Arrays.asList(new SetFieldConfigAction("protein")));

            map.put(new MultiKey("relationship", "CDNAClone", "derived_assoc_cdna_clone", "Gene"),
                    Arrays.asList(new SetFieldConfigAction("gene")));

            map.put(new MultiKey("relationship", "Gene", "producedby", "Protein"),
                    Arrays.asList(new SetFieldConfigAction("proteins")));

            // featureprop configuration example: for features of class "Gene", if the type name
            // of the prop is "cyto_range", set the "cytoLocation" attribute of the
            // new Gene to be this property
            map.put(new MultiKey("prop", "Gene", "cyto_range"),
                    Arrays.asList(new SetFieldConfigAction("cytoLocation")));
            map.put(new MultiKey("prop", "Gene", "symbol"),
                    Arrays.asList(CREATE_SYNONYM_ACTION));
            map.put(new MultiKey("prop", "TransposableElementInsertionSite",
                                 "curated_cytological_location"),
                                 Arrays.asList(new SetFieldConfigAction("cytoLocation")));
            ConfigAction alleleClassConfigAction = new AlleleClassSetFieldAction("alleleClass");
            map.put(new MultiKey("prop", "Allele", "promoted_allele_class"),
                    Arrays.asList(alleleClassConfigAction));
            // library config example: for features of class "CDNAClone", if the type name
            // of the library is "stage", set the "stage" attribute of the
            // new CDNAClone to be this property
            map.put(new MultiKey("library", "CDNAClone", "stage"),
                    Arrays.asList(new SetFieldConfigAction("stage")));
            // anatomy term config example:  for features of class "CDNAClone" if there is an
            // anatomy term, set a reference in CDNAClone.tissueSource
// See #2173
//            map.put(new MultiKey("anatomyterm", "CDNAClone", null),
//                    Arrays.asList(new SetFieldConfigAction("tissueSource")));

            // feature_cvterm example for Transposition: we create a featureTerms collection in the
            // Transposition objects containing SequenceOntologyTerm objects.  For the current
            // feature we create one SequenceOntologyTerm object for each associated "SO" cvterm.
            // We set the "name" field of the SequenceOntologyTerm to be the name from the cvterm
            // table.
            // TODO fixme
//            List<String> chromosomeStructureVariationClassNames =
//                Arrays.asList("ChromosomeStructureVariation", "ChromosomalDeletion",
//                        "ChromosomalDuplication", "ChromosomalInversion",
//                        "ChromosomalTranslocation", "ChromosomalTransposition");
//            for (String className: chromosomeStructureVariationClassNames) {
//                map.put(new MultiKey("cvterm", className, "SO"),
//                        Arrays.asList(new CreateCollectionAction("SOTerm", "abberationSOTerms",
//                                "name", true)));
//            }
            // feature configuration example: for features of class "Exon", from "FlyBase",
            // set the Gene.symbol to be the "name" field from the chado feature
            map.put(new MultiKey("feature", "Exon", FLYBASE_DB_NAME, "name"),
                    Arrays.asList(new SetFieldConfigAction("symbol")));
            map.put(new MultiKey("feature", "Allele", FLYBASE_DB_NAME, "name"),
                    Arrays.asList(new SetFieldConfigAction("symbol")));

            // DO_NOTHING_ACTION means skip the name from this feature
            map.put(new MultiKey("feature", "Chromosome", FLYBASE_DB_NAME, "name"),
                    Arrays.asList(DO_NOTHING_ACTION));

            map.put(new MultiKey("feature", "ChromosomeBand", FLYBASE_DB_NAME, "name"),
                    Arrays.asList(DO_NOTHING_ACTION));

            map.put(new MultiKey("feature", "TransposableElementInsertionSite", FLYBASE_DB_NAME,
                                 "name"),
                    Arrays.asList(new SetFieldConfigAction("symbol", PB_INSERTION_PATTERN),
                                  new SetFieldConfigAction("secondaryIdentifier")));

            map.put(new MultiKey("feature", "Gene", FLYBASE_DB_NAME, "uniquename"),
                    Arrays.asList(new SetFieldConfigAction("primaryIdentifier")));
            map.put(new MultiKey("feature", "Gene", FLYBASE_DB_NAME, "name"),
                    Arrays.asList(DO_NOTHING_ACTION));

            map.put(new MultiKey("feature", "ChromosomeStructureVariation", FLYBASE_DB_NAME,
                                 "name"),
                    Arrays.asList(new SetFieldConfigAction("secondaryIdentifier")));

            // just make a Synonym because the secondaryIdentifier and the symbol are set from the
            // dbxref and synonym tables
            map.put(new MultiKey("feature", "MRNA", FLYBASE_DB_NAME, "name"),
                    Arrays.asList(new CreateSynonymAction()));

            map.put(new MultiKey("feature", "PointMutation", FLYBASE_DB_NAME, "uniquename"),
                    Arrays.asList(new SetFieldConfigAction("name"),
                                  new SetFieldConfigAction("primaryIdentifier")));
            // name isn't set in flybase:
            map.put(new MultiKey("feature", "PointMutation", FLYBASE_DB_NAME, "name"),
                    Arrays.asList(DO_NOTHING_ACTION));

            map.put(new MultiKey("dbxref", "Protein", FLYBASE_DB_NAME + " Annotation IDs",
                                 Boolean.TRUE),
                                 Arrays.asList(CREATE_SYNONYM_ACTION));
            map.put(new MultiKey("feature", "Protein", FLYBASE_DB_NAME, "name"),
                    Arrays.asList(CREATE_SYNONYM_ACTION));
            map.put(new MultiKey("feature", "Protein", FLYBASE_DB_NAME, "uniquename"),
                    Arrays.asList(new SetFieldConfigAction("secondaryIdentifier")));
            map.put(new MultiKey("dbxref", "Protein", "GB_protein", Boolean.TRUE),
                    Arrays.asList(new SetFieldConfigAction("genbankIdentifier"),
                                  CREATE_SYNONYM_ACTION));

            // transposable_element and natural_transposable_element
            map.put(new MultiKey("feature", "TransposableElement", FLYBASE_DB_NAME, "name"),
                    Arrays.asList(new SetFieldConfigAction("secondaryIdentifier"),
                                  new SetFieldConfigAction("symbol")));
            map.put(new MultiKey("feature", "NaturalTransposableElement", FLYBASE_DB_NAME, "name"),
                    Arrays.asList(new SetFieldConfigAction("secondaryIdentifier"),
                                  new SetFieldConfigAction("symbol")));
            map.put(new MultiKey("relationship", "TransposableElement",
                    "producedby", "NaturalTransposableElement"),
                    Arrays.asList(new SetFieldConfigAction("insertedElement")));
            map.put(new MultiKey("synonym", "NaturalTransposableElement", "fullname",
                    Boolean.TRUE),
                    Arrays.asList(new SetFieldConfigAction("name")));
        }
        return map;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getExtraFeatureConstraint() {
        return "NOT ((cvterm.name = 'golden_path_region'"
            + " OR cvterm.name = 'ultra_scaffold')"
            + " AND (uniquename LIKE 'Unknown_%' OR uniquename LIKE '%_groupMISC'))"
            + " AND " + getLocatedGeneAllesSql();
    }

    /**
     * Query that returns only allele of located genes.
     */
    private static String getLocatedGeneAllesSql() {
        return "(NOT (uniquename LIKE 'FBal%') OR feature_id IN"
            + "   (SELECT subject_id"
            + "      FROM feature_relationship, cvterm"
            + "     WHERE type_id = cvterm.cvterm_id"
            + "       AND cvterm.name = 'alleleof'"
            + "       AND object_id IN (" + getLocatedGenesSql() + ")))";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Item makeFeature(Integer featureId, String chadoFeatureType, String interMineType,
                               String name, String uniqueName,
                               int seqlen, int taxonId) {
        String realInterMineType = interMineType;

        if ("protein".equals(chadoFeatureType) && !uniqueName.startsWith("FBpp")) {
            return null;
        }

        if ("gene".equals(chadoFeatureType)) {
            if (uniqueName.startsWith("FBal")) {
                // fix type of allele "gene" features
                realInterMineType = "Allele";
            } else {
                if (!locatedGeneIds.contains(featureId.intValue())) {
                    // ignore genes with no location
                    return null;
                }
            }
        }

        // ignore unknown chromosome from dpse
        if (uniqueName.startsWith("Unknown_")) {
            return null;
        }

        if (taxonId != 7227 && "chromosome_arm".equals(chadoFeatureType)) {
            // nothing is located on a chromosome_arm
            return null;
        }

        if ("chromosome".equals(chadoFeatureType)
            && !"dmel_mitochondrion_genome".equals(uniqueName)) {
            // ignore Chromosomes from flybase - features are located on ChromosomeArms except
            // for mitochondrial features
            return null;
        }

        if ("chromosome_arm".equals(chadoFeatureType)
                        || "ultra_scaffold".equals(chadoFeatureType)) {
            if ("dmel_mitochondrion_genome".equals(uniqueName)) {
                // ignore - all features are on the Chromosome object with uniqueName
                // "dmel_mitochondrion_genome"
                return null;
            }
            realInterMineType = "Chromosome";
        }

        if ("golden_path_region".equals(chadoFeatureType)) {
            // For organisms other than D. melanogaster sometimes we can convert a
            // golden_path_region to an actual chromosome: if name is 2L, 4, etc
            // 2015 June - most Drosophila are now golden path fragments
            realInterMineType = "Chromosome";
        }

        if (chadoFeatureType.equals(CHROMOSOME_STRUCTURE_VARIATION_SO_NAME)) {
            Integer cvtermId = chromosomeStructureVariationTypes.get(featureId);

            if (cvtermId != null) {
                ChadoCVTerm term = sequenceOntologyCV.getByChadoId(cvtermId);

                for (String soName: CHROMOSOME_STRUCTURE_VARIATION_SO_MAP.keySet()) {
                    if (termOrChildrenNameMatches(term, soName)) {
                        realInterMineType = CHROMOSOME_STRUCTURE_VARIATION_SO_MAP.get(soName);
                        break;
                    }
                }
            }
        }

        if ("transposable_element_insertion_site".equals(chadoFeatureType)
                        && name == null && !uniqueName.startsWith("FBti")) {
            // ignore this feature as it doesn't have an FBti identifier and there will be
            // another feature for the same transposable_element_insertion_site that does have
            // the FBti identifier
            return null;
        }
        if ("mRNA".equals(chadoFeatureType) && seqlen == 0) {
            // flybase has > 7000 mRNA features that have no sequence and don't appear in their
            // webapp so we filter them out
            return null;
        }
        if ("protein".equals(chadoFeatureType) && seqlen == 0) {
            // flybase has ~ 2100 protein features that don't appear in their webapp so we
            // filter them out
            return null;
        }

        Item feature = getChadoDBConverter().createItem(realInterMineType);

        if ("Allele".equals(realInterMineType)) {
            alleleIdMap.put(uniqueName, feature.getIdentifier());
        }

        return feature;
    }

    /**
     * Return true iff the given term or one of its children is named termName.
     */
    private static boolean termOrChildrenNameMatches(ChadoCVTerm term, String termName) {
        if (term.getName().equals(termName)) {
            return true;
        }
        Set<ChadoCVTerm> children = term.getAllChildren();
        for (ChadoCVTerm childTerm: children) {
            if (childTerm.getName().equals(termName)) {
                return true;
            }
        }
        return false;
    }

    private static final List<String> FEATURES = Arrays.asList(
            "gene", "mRNA", "transcript", "protein",
            "intron", "exon", "regulatory_region", "enhancer", "EST", "cDNA_clone",
            "miRNA", "snRNA", "ncRNA", "rRNA", "ncRNA", "snoRNA", "tRNA",
            "chromosome_band", "transposable_element_insertion_site",
            CHROMOSOME_STRUCTURE_VARIATION_SO_NAME,
            "point_mutation", "natural_transposable_element",
            "transposable_element"
    );

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
     * For objects that have primaryIdentifier == null, set the primaryIdentifier to be the
     * uniquename column from chado.
     * {@inheritDoc}
     */
    @Override
    protected void extraProcessing(Connection connection, Map<Integer, FeatureData> features)
        throws ObjectStoreException, SQLException {

        createAllelesTempTable(connection);
        createInsertionTempTable(connection);

        for (FeatureData featureData: features.values()) {
            if (!featureData.getFlag(FeatureData.IDENTIFIER_SET)) {
                setAttribute(featureData.getIntermineObjectId(), "primaryIdentifier",
                             featureData.getChadoFeatureUniqueName());
            }
        }
        if (FEATURES.contains("gene")) {
            processAlleleProps(connection, features);

            Map<Integer, List<String>> mutagenMap = makeMutagenMap(connection);
            for (Integer alleleFeatureId: mutagenMap.keySet()) {
                FeatureData alleleDat = features.get(alleleFeatureId);
                List<String> mutagenRefIds = new ArrayList<String>();
                for (String mutagenDescription: mutagenMap.get(alleleFeatureId)) {
                    String mutagenIdentifier = getMutagen(mutagenDescription);
                    mutagenRefIds.add(mutagenIdentifier);
                }
                ReferenceList referenceList = new ReferenceList();
                referenceList.setName("mutagens");
                referenceList.setRefIds(mutagenRefIds);
                getChadoDBConverter().store(referenceList, alleleDat.getIntermineObjectId());
            }

            createIndelReferences(connection);
            createDeletionLocations(connection);
            copyInsertionLocations(connection);

            createInteractions(connection);
        }
    }

    private Item getInteraction(Map<MultiKey, Item> interactions, String refId,
            String gene2RefId) {
        MultiKey key = new MultiKey(refId, gene2RefId);
        Item item = interactions.get(key);
        if (item == null) {
            item = getChadoDBConverter().createItem("Interaction");
            item.setReference("participant1", refId);
            item.setReference("participant2", gene2RefId);
            interactions.put(key, item);

        }
        return item;
    }

    /**
     * Create Interaction objects.
     */
    private void createInteractions(Connection connection)
        throws SQLException, ObjectStoreException {
        Map<MultiKey, Item> seenInteractions = new HashMap<MultiKey, Item>();
        ResultSet res = getInteractionResultSet(connection);

        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            Integer otherFeatureId = new Integer(res.getInt("other_feature_id"));
            String pubTitle = res.getString("pub_title");
            Integer pubmedId = new Integer(res.getInt("pubmed_id"));
            FeatureData featureData = getFeatureMap().get(featureId);
            FeatureData otherFeatureData = getFeatureMap().get(otherFeatureId);

            OrganismData od = otherFeatureData.getOrganismData();
            Item dataSetItem = getChadoDBConverter().getDataSetItem(od.getTaxonId());
            String publicationItemId = makePublication(pubmedId);
            String name = "FlyBase:" + featureData.getChadoFeatureUniqueName() + "_"
                    + otherFeatureData.getChadoFeatureUniqueName();

            Item interaction = getInteraction(seenInteractions, featureData.getItemIdentifier(),
                    otherFeatureData.getItemIdentifier());
            createDetail(dataSetItem, pubTitle, publicationItemId, interaction, name);

            name = "FlyBase:" + otherFeatureData.getChadoFeatureUniqueName() + "_"
                    + featureData.getChadoFeatureUniqueName();
            interaction = getInteraction(seenInteractions, otherFeatureData.getItemIdentifier(),
                    featureData.getItemIdentifier());
            createDetail(dataSetItem, pubTitle, publicationItemId, interaction, name);
        }
        for (Item item : seenInteractions.values()) {
            getChadoDBConverter().store(item);
        }
    }

    private void createDetail(Item dataSetItem, String pubTitle,
            String publicationItemId, Item interaction, String name)
        throws ObjectStoreException {
        Item detail = getChadoDBConverter().createItem("InteractionDetail");
        detail.setAttribute("name", name);
        detail.setAttribute("type", "genetic");
        detail.setAttribute("role1", DEFAULT_ROLE);
        detail.setAttribute("role2", DEFAULT_ROLE);
        String experimentItemIdentifier =
            makeInteractionExperiment(pubTitle, publicationItemId);
        detail.setReference("experiment", experimentItemIdentifier);
        detail.setReference("interaction", interaction);
        detail.setAttribute("relationshipType", RELATIONSHIP_TYPE);
        detail.addToCollection("dataSets", dataSetItem);
        getChadoDBConverter().store(detail);
    }

    /**
     * Return the item identifier of the Interaction Item for the given pubmed id, creating the
     * Item if necessary.
     * @param experimentTitle the new title
     * @param publicationItemIdentifier the item identifier of the publication for this experiment
     * @return the interaction item identifier
     * @throws ObjectStoreException if the item can't be stored
     */
    protected String makeInteractionExperiment(String experimentTitle,
                                               String publicationItemIdentifier)
        throws ObjectStoreException {
        if (interactionExperiments.containsKey(experimentTitle)) {
            return interactionExperiments.get(experimentTitle);
        }
        Item experiment = getChadoDBConverter().createItem("InteractionExperiment");
        experiment.setAttribute("name", experimentTitle);
        experiment.setReference("publication", publicationItemIdentifier);
        getChadoDBConverter().store(experiment);
        String experimentId = experiment.getIdentifier();
        interactionExperiments.put(experimentTitle, experimentId);
        return experimentId;
    }

    /**
     * Create Location objects for deletions (chromosome_structure_variation) as they don't have
     * locations in the featureloc table.
     * @throws ObjectStoreException
     */
    private void createDeletionLocations(Connection connection)
        throws SQLException, ObjectStoreException {
        ResultSet res = getDeletionLocationResultSet(connection);
        while (res.next()) {
            Integer delId = new Integer(res.getInt("deletion_feature_id"));

            FeatureData delFeatureData = getFeatureMap().get(delId);
            if (delFeatureData == null) {
                LOG.info("can't find deletion " + delId + " in feature map");
                continue;
            }
            String chromosomeName = res.getString("chromosome_name");
            String startString = res.getString("fmin");
            String endString = res.getString("fmax");
            String strandString = res.getString("strand");

            // Df(3L)ZN47/FBab0000006 and some others don't have a strand
            // I don't know why, but for now we'll just give them a default
            if (StringUtils.isEmpty(strandString)) {
                strandString = "1";
            }
            if (StringUtils.isEmpty(startString) || StringUtils.isEmpty(endString)) {
                continue;
            }
            Integer organismId = new Integer(res.getInt("deletion_organism_id"));
            int start = Integer.parseInt(startString);
            int end = Integer.parseInt(endString);
            int strand = Integer.parseInt(strandString);
            if (start > end) {
                int tmp = start;
                start = end;
                end = tmp;
            }
            int taxonId = delFeatureData.getOrganismData().getTaxonId();

            Integer chrFeatureId = getChromosomeFeatureMap(organismId).get(chromosomeName);
            if (chrFeatureId == null) {
                String msg = "Can't find chromosome " + chromosomeName + " in feature map";
                LOG.warn(msg);
                continue;
            }
            FeatureData chrFeatureData = getFeatureMap().get(chrFeatureId);
            if (chrFeatureData == null) {
                String msg = "chrFeatureData is null " + chrFeatureId + " for feature " + delId;
                LOG.warn(msg);
                continue;
            }
            makeAndStoreLocation(chrFeatureId, delFeatureData, start, end, strand, taxonId);
        }
    }

    private void makeAndStoreLocation(Integer chrFeatureId, FeatureData subjectFeatureData,
            int start, int end, int strand, int taxonId)
        throws ObjectStoreException {
        FeatureData chrFeatureData = getFeatureMap().get(chrFeatureId);
        Item location =
            getChadoDBConverter().makeLocation(chrFeatureData.getItemIdentifier(),
                                               subjectFeatureData.getItemIdentifier(),
                                               start, end, strand, taxonId);
        Item dataSetItem = getChadoDBConverter().getDataSetItem(taxonId);

        location.addToCollection("dataSets", dataSetItem);

        Reference chrLocReference = new Reference();
        chrLocReference.setName("chromosomeLocation");
        chrLocReference.setRefId(location.getIdentifier());
        getChadoDBConverter().store(chrLocReference, subjectFeatureData.getIntermineObjectId());

        getChadoDBConverter().store(location);
    }

    /**
     * Create the ChromosomalDeletion.element1 and element2 references (to
     * TransposableElementInsertionSite objects)
     */
    private void createIndelReferences(Connection connection)
        throws ObjectStoreException, SQLException {
        ResultSet res = getIndelResultSet(connection);
        int featureWarnings = 0;
        while (res.next()) {
            Integer delId = new Integer(res.getInt("deletion_feature_id"));
            Integer insId = new Integer(res.getInt("insertion_feature_id"));
            String breakType = res.getString("breakpoint_type");
            Reference reference = new Reference();
            if ("bk1".equals(breakType)) {
                reference.setName("element1");
            } else {
                reference.setName("element2");
            }
            FeatureData insFeatureData = getFeatureMap().get(insId);
            if (insFeatureData == null) {
                if (featureWarnings <= 20) {
                    if (featureWarnings < 20) {
                        LOG.warn("insertion " + insId
                                 + " was not found in the feature table");
                    } else {
                        LOG.warn("further warnings ignored");
                    }
                    featureWarnings++;
                }
                continue;
            }
            reference.setRefId(insFeatureData.getItemIdentifier());
            FeatureData delFeatureData = getFeatureMap().get(delId);
            if (delFeatureData == null) {
                if (featureWarnings <= 20) {
                    if (featureWarnings < 20) {
                        LOG.warn("deletion " + delId
                                 + " was not found in the feature table");
                    } else {
                        LOG.warn("further warnings ignored");
                    }
                    featureWarnings++;
                }
                continue;
            }
            getChadoDBConverter().store(reference, delFeatureData.getIntermineObjectId());
        }
    }

    private String getMutagen(String description) throws ObjectStoreException {
        if (mutagensMap.containsKey(description)) {
            return mutagensMap.get(description);
        }
        Item mutagen = getChadoDBConverter().createItem("Mutagen");
        mutagen.setAttribute("description", description);
        mutagensMap.put(description, mutagen.getIdentifier());
        store(mutagen);
        return mutagen.getIdentifier();
    }

    /**
     * @param connection
     */
    private void copyInsertionLocations(Connection connection)
        throws ObjectStoreException, SQLException {
        ResultSet res = getInsertionLocationsResultSet(connection);
        while (res.next()) {
            int subId = res.getInt("sub_id");
            int chrId = res.getInt("chr_feature_id");
            int fmin = res.getInt("fmin");
            int fmax = res.getInt("fmax");

            int start = fmin + 1;
            int end = fmax;

            FeatureData subFeatureData = getFeatureMap().get(new Integer(subId));
            if (subFeatureData != null) {
                // this is a hack - we should make sure that we only query for features that are in
                // the feature map, ie. those for the current organism
                int taxonId = subFeatureData.getOrganismData().getTaxonId();

                makeAndStoreLocation(new Integer(chrId), subFeatureData, start, end, 1, taxonId);
            }
        }
    }

    private void store(Item item) throws ObjectStoreException {
        getChadoDBConverter().store(item);
    }

    // map from anatomy identifier (eg. "FBbt0001234") to Item identifier
    private Map<String, String> anatomyTermMap = new HashMap<String, String>();
    // map from development term identifier (eg. "FBdv0001234") to Item identifier
    private Map<String, String> developmentTermMap = new HashMap<String, String>();
    // map from FlyBase cv identifier (eg. "FBcv0001234") to Item identifier
    private Map<String, String> cvTermMap = new HashMap<String, String>();

    private void processAlleleProps(Connection connection,
                                    Map<Integer, FeatureData> features)
        throws SQLException, ObjectStoreException {
        Map<Integer, List<String>> annotationPubMap = makeAnnotationPubMap(connection);
        ResultSet res = getAllelePropResultSet(connection);
        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            String value = res.getString("value");
            String propType = res.getString("type_name");
            Integer featurePropId = new Integer(res.getInt("featureprop_id"));

            FeatureData alleleFeatureData = features.get(featureId);
            OrganismData od = alleleFeatureData.getOrganismData();
            Item dataSetItem = getChadoDBConverter().getDataSetItem(od.getTaxonId());

            String alleleItemIdentifier = alleleFeatureData.getItemIdentifier();

            Item phenotypeAnnotation = null;
            if ("derived_pheno_manifest".equals(propType)) {
                phenotypeAnnotation =
                    makePhenotypeAnnotation(alleleItemIdentifier, value,
                                            dataSetItem, annotationPubMap.get(featurePropId));
                phenotypeAnnotation.setAttribute("annotationType", "manifest in");
            } else {
                if ("derived_pheno_class".equals(propType)) {
                    phenotypeAnnotation =
                        makePhenotypeAnnotation(alleleItemIdentifier, value,
                                                dataSetItem, annotationPubMap.get(featurePropId));
                    phenotypeAnnotation.setAttribute("annotationType", "phenotype class");
                }
            }

            if (phenotypeAnnotation != null) {
                getChadoDBConverter().store(phenotypeAnnotation);
            }
        }
    }

    /**
     * Return a Map from allele feature_id to mutagen.  The mutagen is found be looking at cvterms
     * that are associated with each feature and saving those terms that have "origin of mutation"
     * as a parent term.
     */
    private Map<Integer, List<String>> makeMutagenMap(Connection connection)
        throws SQLException {
        Map<Integer, List<String>> retMap = new HashMap<Integer, List<String>>();

        ResultSet res = getAlleleCVTermsResultSet(connection);
    RESULTS:
        while (res.next()) {
            Integer featureId = new Integer(res.getInt("feature_id"));
            Integer cvtermId = new Integer(res.getInt("cvterm_id"));

            ChadoCVTerm cvterm = flyBaseMiscCv.getByChadoId(cvtermId);
            if (cvterm == null) {
                LOG.error("cvterm not found for " + res.getInt("cvterm_id") + " for feature "
                        + res.getInt("feature_id"));
                continue;
            }

            Set<ChadoCVTerm> parents = cvterm.getAllParents();

            for (ChadoCVTerm parent: parents) {
                if ("origin of mutation".equals(parent.getName())) {
                    String fixedName = XmlUtil.fixEntityNames(cvterm.getName());
                    List<String> mutagens;
                    if (retMap.containsKey(featureId)) {
                        mutagens = retMap.get(featureId);

                    } else {
                        mutagens = new ArrayList<String>();
                        retMap.put(featureId, mutagens);
                    }
                    mutagens.add(fixedName);
                    continue RESULTS;
                }
            }
        }

        return retMap;
    }

    /**
     * Get result set of feature_id, cvterm_id pairs for the alleles in flybase chado.
     * @param connection the Connectio
     * @return the cvterms
     * @throws SQLException if there is a database problem
     */
    protected ResultSet getAlleleCVTermsResultSet(Connection connection) throws SQLException {
        String query = "SELECT DISTINCT feature.feature_id, cvterm.cvterm_id"
            + "           FROM feature, feature_cvterm, cvterm"
            + "          WHERE feature.feature_id = feature_cvterm.feature_id"
            + "            AND feature.feature_id IN (" + getAlleleFeaturesSql() + ")"
            + "            AND feature_cvterm.cvterm_id = cvterm.cvterm_id";
        LOG.info("executing getAlleleCVTermsResultSet(): " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Return a map from featureprop_id for alleles to publication item identifier
     */
    private Map<Integer, List<String>> makeAnnotationPubMap(Connection connection)
        throws SQLException, ObjectStoreException {
        Map<Integer, List<String>> retMap = new HashMap<Integer, List<String>>();

        ResultSet res = getAllelePropPubResultSet(connection);
        while (res.next()) {
            Integer featurePropId = new Integer(res.getInt("featureprop_id"));
            String pubDbId = res.getString("pub_db_identifier");

            Integer n = new Integer(Integer.parseInt(pubDbId));
            String pubicationItemIdentifier = makePublication(n);

            if (!retMap.containsKey(featurePropId)) {
                retMap.put(featurePropId, new ArrayList<String>());
            }
            retMap.get(featurePropId).add(pubicationItemIdentifier);
        }

        return retMap;
    }

    /**
     * Return a map from feature_id to seqlen
     * @throws SQLException if somethign goes wrong
     */
//    private Map<Integer, Integer> makeCDNALengthMap(Connection connection)
//    throws SQLException {
//        Map<Integer, Integer> retMap = new HashMap();
//
//        ResultSet res = getCDNALengthResultSet(connection);
//        while (res.next()) {
//            Integer featureId = new Integer(res.getInt("feature_id"));
//            Integer seqlen = new Integer(res.getInt("seqlen"));
//            retMap.put(featureId, seqlen);
//        }
//        return retMap;
//    }

    private Item makePhenotypeAnnotation(String alleleItemIdentifier, String value,
                                         Item dataSetItem, List<String> publicationsItemIdList)
        throws ObjectStoreException {
        Item phenotypeAnnotation = getChadoDBConverter().createItem("PhenotypeAnnotation");
        phenotypeAnnotation.addToCollection("dataSets", dataSetItem);

        Pattern p = Pattern.compile(FLYBASE_PROP_ATTRIBUTE_PATTERN);
        Matcher m = p.matcher(value);
        StringBuffer sb = new StringBuffer();

        List<String> dbAnatomyTermIdentifiers = new ArrayList<String>();
        List<String> dbDevelopmentTermIdentifiers = new ArrayList<String>();
        List<String> dbCVTermIdentifiers = new ArrayList<String>();

        while (m.find()) {
            String field = m.group(1);
            int colonPos = field.indexOf(':');
            if (colonPos == -1) {
                m.appendReplacement(sb, field);
            } else {
                String identifier = field.substring(0, colonPos);
                if (identifier.startsWith(FLYBASE_ANATOMY_TERM_PREFIX)) {
                    dbAnatomyTermIdentifiers.add(addCVTermColon(identifier));
                } else {
                    if (identifier.startsWith("FBdv")) {
                        dbDevelopmentTermIdentifiers.add(addCVTermColon(identifier));
                    } else {
                        if (identifier.startsWith("FBcv")) {
                            dbCVTermIdentifiers.add(addCVTermColon(identifier));
                        }
                    }
                }
                String text = field.substring(colonPos + 1);
                m.appendReplacement(sb, text);
            }
        }
        m.appendTail(sb);

        /*
         * ignore with for now because the with text is wrong in chado - see ticket #889
        List<String> withAlleleIdentifiers = findWithAllele(value);

        if (withAlleleIdentifiers.size() > 0) {
            phenotypeAnnotation.setCollection("with", withAlleleIdentifiers);
        }
        */

        String valueNoRefs = sb.toString();
        String valueNoUps = valueNoRefs.replaceAll("<up>", "[").replaceAll("</up>", "]");
        phenotypeAnnotation.setAttribute("description", valueNoUps);
        phenotypeAnnotation.setReference("allele", alleleItemIdentifier);
        if (publicationsItemIdList != null && publicationsItemIdList.size() > 0) {
            ReferenceList pubReferenceList =
                new ReferenceList("publications", publicationsItemIdList);
            phenotypeAnnotation.addCollection(pubReferenceList);
        }

        if (dbAnatomyTermIdentifiers.size() == 1) {
            String anatomyIdentifier = dbAnatomyTermIdentifiers.get(0);
            String anatomyTermItemId = makeAnatomyTerm(anatomyIdentifier);
            phenotypeAnnotation.setReference("anatomyTerm", anatomyTermItemId);
        } else {
            if (dbAnatomyTermIdentifiers.size() > 1) {
                throw new RuntimeException("more than one anatomy term: "
                                           + dbAnatomyTermIdentifiers);
            }
        }

        if (dbDevelopmentTermIdentifiers.size() == 1) {
            String developmentTermIdentifier = dbDevelopmentTermIdentifiers.get(0);
            String developmentTermItemId = makeDevelopmentTerm(developmentTermIdentifier);
            phenotypeAnnotation.setReference("developmentTerm", developmentTermItemId);
        } else {
            if (dbAnatomyTermIdentifiers.size() > 1) {
                throw new RuntimeException("more than one anatomy term: "
                                           + dbAnatomyTermIdentifiers);
            }
        }

        if (dbCVTermIdentifiers.size() > 0) {
            for (String cvTermIdentifier: dbCVTermIdentifiers) {
                String cvTermItemId = makeCVTerm(cvTermIdentifier);
                phenotypeAnnotation.addToCollection("cvTerms", cvTermItemId);
            }
        }

        return phenotypeAnnotation;
    }

    private static final Pattern FLYBASE_TERM_IDENTIFIER_PATTERN =
        Pattern.compile("^FB[^\\d][^\\d]\\d+");

    /**
     * For a FlyBase cvterm identifier like "FBbt00000001", add a colon in the middle and return:
     * "FBbt:00000001"
     * @param identifier the identifier from chado
     * @return the public identifier
     */
    protected static String addCVTermColon(String identifier) {
        Matcher m = FLYBASE_TERM_IDENTIFIER_PATTERN.matcher(identifier);
        if (m.matches()) {
            return identifier.substring(0, 4) + ":" + identifier.substring(4);
        }
        return identifier;
    }

    /**
     * Return the item identifiers of the alleles metioned in the with clauses of the argument.
     * Currently unused because flybase with clauses are wrong - see ticket #889
     */
//    @SuppressWarnings("unused")
//    private List<String> findWithAllele(String value) {
//        Pattern p = Pattern.compile("with @(FBal\\d+):");
//        Matcher m = p.matcher(value);
//
//        List<String> foundIdentifiers = new ArrayList<String>();
//
//        while (m.find()) {
//            String identifier = m.group(1);
//            if (identifier.startsWith("FBal")) {
//                foundIdentifiers.add(identifier);
//            } else {
//                throw new RuntimeException("identifier in a with must start: \"FBal\" not: "
//                                           + identifier);
//            }
//        }
//
//        List<String> alleleItemIdentifiers = new ArrayList<String>();
//
//        for (String foundIdentifier: foundIdentifiers) {
//            if (alleleIdMap.containsKey(foundIdentifier)) {
//                alleleItemIdentifiers.add(alleleIdMap.get(foundIdentifier));
//            } else {
//                // this allele wasn't stored so probably it didn't have the right organism - some
//                // GAL4 alleles have cerevisiae as organism, eg. FBal0060667:Scer\GAL4[sd-SG29.1]
//                // referenced by FBal0038994 Rac1[N17.Scer\UAS]
//            }
//        }
//
//        return alleleItemIdentifiers;
//    }

    /**
     * phenotype annotation creates and stores anatomy terms.  so does librarycvterm
     * @param identifier identifier for anatomy term
     * @return refId for anatomy term object
     * @throws ObjectStoreException if term can't be stored
     */
    @Override
    protected String makeAnatomyTerm(String identifier) throws ObjectStoreException {
        String newIdentifier = identifier;
        if (!newIdentifier.startsWith(FLYBASE_ANATOMY_TERM_PREFIX)) {
            newIdentifier = FLYBASE_ANATOMY_TERM_PREFIX + identifier;
            newIdentifier = addCVTermColon(newIdentifier);
        }

        if (anatomyTermMap.containsKey(newIdentifier)) {
            return anatomyTermMap.get(newIdentifier);
        }
        Item anatomyTerm = getChadoDBConverter().createItem("AnatomyTerm");
        anatomyTerm.setAttribute("identifier", newIdentifier);
        getChadoDBConverter().store(anatomyTerm);
        anatomyTermMap.put(identifier, anatomyTerm.getIdentifier());
        return anatomyTerm.getIdentifier();
    }

    private String makeDevelopmentTerm(String identifier) throws ObjectStoreException {
        if (developmentTermMap.containsKey(identifier)) {
            return developmentTermMap.get(identifier);
        }
        Item developmentTerm = getChadoDBConverter().createItem("DevelopmentTerm");
        developmentTerm.setAttribute("identifier", identifier);
        getChadoDBConverter().store(developmentTerm);
        developmentTermMap.put(identifier, developmentTerm.getIdentifier());
        return developmentTerm.getIdentifier();
    }

    private String makeCVTerm(String identifier) throws ObjectStoreException {
        if (cvTermMap.containsKey(identifier)) {
            return cvTermMap.get(identifier);
        }
        Item cvTerm = getChadoDBConverter().createItem("CVTerm");
        cvTerm.setAttribute("identifier", identifier);
        getChadoDBConverter().store(cvTerm);
        cvTermMap.put(identifier, cvTerm.getIdentifier());
        return cvTerm.getIdentifier();
    }

    /**
     * Return a result set containing the interaction genes pairs, the title of the publication
     * that reported the interaction and its pubmed id.  The method is protected
     * so that is can be overridden for testing.
     * @param connection the Connection
     * @throws SQLException if there is a database problem
     * @return the ResultSet
     */
    protected ResultSet getInteractionResultSet(Connection connection) throws SQLException {
        String query =
            "      SELECT feature.feature_id as feature_id, "
            + "           other_feature.feature_id as other_feature_id, "
            + "           pub.title as pub_title, dbx.accession as pubmed_id "
            + "      FROM feature, cvterm cvt, feature other_feature, "
            + "           feature_relationship_pub frpb, pub, "
            + "           feature_relationship fr, pub_dbxref pdbx, dbxref dbx, db "
            + "     WHERE feature.feature_id = subject_id "
            + "           AND object_id = other_feature.feature_id "
            + "           AND fr.type_id = cvt.cvterm_id AND cvt.name = 'interacts_genetically' "
            + "           AND fr.feature_relationship_id = frpb.feature_relationship_id "
            + "           AND frpb.pub_id = pub.pub_id AND db.name='pubmed' "
            + "           AND pdbx.is_current=true AND pub.pub_id=pdbx.pub_id "
            + "           AND pdbx.dbxref_id = dbx.dbxref_id AND dbx.db_id=db.db_id "
            + "           AND NOT feature.is_obsolete AND NOT other_feature.is_obsolete "
            + "           AND feature.feature_id IN (" + getLocatedGenesSql() + ")"
            + "           AND other_feature.feature_id IN (" + getLocatedGenesSql() + ")";
        LOG.info("executing getInteractionResultSet(): " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Return a result set containing the alleles and their featureprops.  The method is protected
     * so that is can be overridden for testing.
     * @param connection the Connection
     * @throws SQLException if there is a database problem
     * @return the ResultSet
     */
    protected ResultSet getAllelePropResultSet(Connection connection) throws SQLException {
        String query =
            "SELECT feature_id, value, cvterm.name AS type_name, featureprop_id"
            + "   FROM featureprop, cvterm"
            + "   WHERE featureprop.type_id = cvterm.cvterm_id"
            + "       AND feature_id IN (" + getAlleleFeaturesSql() + ")"
            + "   ORDER BY feature_id";
        LOG.info("executing getAllelePropResultSet(): " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Return a result set containing pairs of chromosome_structure_variation (deletions) and
     * transposable_element_insertion_site (insertions).  The method is protected
     * so that is can be overridden for testing.
     * @param connection the Connection
     * @throws SQLException if there is a database problem
     * @return the ResultSet
     */
    protected ResultSet getIndelResultSet(Connection connection) throws SQLException {
        String query =
              "SELECT del.feature_id as deletion_feature_id,"
            + "       ins.feature_id as insertion_feature_id,"
            + "       substring(break.uniquename FROM ':([^:]+)$') AS breakpoint_type"
            + "  FROM feature del, cvterm del_type, feature_relationship del_rel,"
            + "       cvterm del_rel_type,"
            + "       feature break, cvterm break_type,"
            + "       feature_relationship ins_rel, cvterm ins_rel_type,"
            + "       feature ins, cvterm ins_type"
            + " WHERE del_rel.object_id = del.feature_id"
            + "   AND del_rel.subject_id = break.feature_id"
            + "   AND ins_rel.subject_id = break.feature_id"
            + "   AND ins_rel.object_id = ins.feature_id"
            + "   AND del.type_id = del_type.cvterm_id"
            + "   AND ins.type_id = ins_type.cvterm_id"
            + "   AND del_type.name = 'chromosome_structure_variation'"
            + "   AND ins_type.name = 'transposable_element_insertion_site'"
            + "   AND del_rel.type_id = del_rel_type.cvterm_id"
            + "   AND del_rel_type.name = 'break_of'"
            + "   AND ins_rel.type_id = ins_rel_type.cvterm_id"
            + "   AND ins_rel_type.name = 'progenitor'"
            + "   AND break.type_id = break_type.cvterm_id"
            + "   AND break_type.name = 'breakpoint'"
            // ignore the progenitors so we only set element1 and element2 to be the "descendants"
            + "   AND ins.feature_id NOT IN (SELECT obj_id FROM " + INSERTION_TEMP_TABLE_NAME + ")";
        LOG.info("executing getIndelResultSet(): " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }


    /**
     * Return a result set containing pairs of insertion feature_ids (eg. for "FBti0027974" =>
     * "FBti0023081") and the fmin and fmax of the first insertion in the pair (ie. the progenitor).
     * The second in the pair is the "Modified descendant of" the first.  The pairs are found using
     * the 'modified_descendant_of' relation type.  All insertions are from DrosDel.
     * The method is protected so that is can be overridden for testing.
     * @param connection the Connection
     * @throws SQLException if there is a database problem
     * @return the ResultSet
     */
    protected ResultSet getInsertionLocationsResultSet(Connection connection) throws SQLException  {
        String query = "SELECT * from " + INSERTION_TEMP_TABLE_NAME;
        LOG.info("executing getInsertionLocationsResultSet(): " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Return a result set containing location for deletions (chromosome_structure_variation)
     * objects.  The locations are in the featureprop able in the form:
     *   2R:12716549..12984803 (53D11;53F8)
     * The method is protected so that is can be overridden for testing.
     * @param connection the Connection
     * @throws SQLException if there is a database problem
     * @return the ResultSet
     */
    protected ResultSet getDeletionLocationResultSet(Connection connection) throws SQLException {
        String query =
            "SELECT f.feature_id as deletion_feature_id, f.organism_id as deletion_organism_id, "
            +   "c.name as chromosome_name, fl.fmin, fl.fmax, fl.strand "
            + "FROM feature f, feature b, feature_relationship fr, cvterm cvt1, cvterm cvt2, "
            + "     featureloc fl, feature c "
            + "WHERE f.feature_id = fr.object_id "
            + "     AND fr.type_id = cvt1.cvterm_id "
            + "     AND cvt1.name = 'break_of' "
            + "     AND fr.subject_id = b.feature_id "
            + "     AND b.type_id = cvt2.cvterm_id "
            + "     AND cvt2.name = 'breakpoint' "
            + "     AND b.feature_id = fl.feature_id "
            + "     AND f.name ~ '^Df.+' "
            + "     AND f.uniquename like 'FBab%' "
            + "     AND f.is_obsolete = false "
            + "     AND fl.srcfeature_id = c.feature_id ";
        LOG.info("executing getDeletionLocationResultSet(): " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }
    /**
     * Return a result set containing the featureprop_id and the publication identifier of the
     * featureprops for al alleles.  The method is protected so that is can be overridden for
     * testing.
     * @param connection the Connection
     * @throws SQLException if there is a database problem
     * @return the ResultSet
     */
    protected ResultSet getAllelePropPubResultSet(Connection connection) throws SQLException {
        String query =
            "SELECT DISTINCT featureprop_pub.featureprop_id, dbxref.accession as pub_db_identifier"
            + "    FROM featureprop, featureprop_pub, dbxref, db, pub, pub_dbxref"
            + "    WHERE featureprop_pub.pub_id = pub.pub_id"
            + "        AND featureprop.featureprop_id = featureprop_pub.featureprop_id"
            + "        AND pub.pub_id = pub_dbxref.pub_id"
            + "        AND pub_dbxref.dbxref_id = dbxref.dbxref_id"
            + "        AND dbxref.db_id = db.db_id"
            + "        AND db.name = 'pubmed'"
            + "        AND feature_id IN (" + getAlleleFeaturesSql() + ")"
            + "    ORDER BY featureprop_id";
        LOG.info("executing getAllelePropPubResultSet(): " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Return a result set containing the feature_id and its seqlen
     * The method is protected so that is can be overridden for
     * testing.
     * @param connection the Connection
     * @throws SQLException if there is a database problem
     * @return the ResultSet
     */
    protected ResultSet getCDNALengthResultSet(Connection connection) throws SQLException {
        String query =
            "SELECT cl.feature_id, fls.seqlen "
            + "FROM feature cl, feature fls, feature_relationship fr, cvterm fls_type "
            + "WHERE fls_type.name IN ('cDNA','BAC_cloned_genomic_insert') "
            + "  AND cl.feature_id=fr.object_id "
            + "  AND fr.subject_id=fls.feature_id "
            + "  AND fls.type_id=fls_type.cvterm_id ";

        LOG.info("executing getCDNALengthResultSet(): " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Convert ISO entities from FlyBase to HTML entities.
     * {@inheritDoc}
     */
    @Override
    protected String fixIdentifier(FeatureData fdat, String identifier) {
        if (StringUtils.isBlank(identifier)) {
            return identifier;
        }
        return  XmlUtil.fixEntityNames(identifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected FeatureData makeFeatureData(int featureId, String type, String uniqueName,
                                          String name, String md5checksum, int seqlen,
                                          int organismId) throws ObjectStoreException {

        if ("protein".equals(type)) {
            if (!uniqueName.startsWith("FBpp")) {
                return null;
            }
            FeatureData protein = proteinFeatureDataMap.get(md5checksum);
            // make a synonym for the protein we're about to discard
            if (protein != null) {
                if (StringUtils.isNotEmpty(uniqueName)
                        && !protein.getExistingSynonyms().contains(uniqueName)) {
                    Item synonym = createSynonym(protein, uniqueName);
                    store(synonym);
                }
                if (StringUtils.isNotEmpty(name)
                        && !protein.getExistingSynonyms().contains(name)) {
                    Item synonym = createSynonym(protein, name);
                    store(synonym);
                }
                return protein;
            }
            FeatureData fdat = super.makeFeatureData(featureId, type, uniqueName, name,
                    md5checksum, seqlen, organismId);
            proteinFeatureDataMap.put(md5checksum, fdat);
            return fdat;
        }

        if ("cDNA_clone".equals(type)) {

            // flybase has duplicates.  to merge with BDGP we need to discard duplicates and
            // make a synonym
            FeatureData cdnaClone = cdnaCloneMap.get(name);
            if (cdnaClone != null) {
                if (StringUtils.isNotEmpty(name)) {
                    Item synonym = createSynonym(cdnaClone, name);
                    if (synonym != null) {
                        store(synonym);
                    }
                }
                return cdnaClone;
            }
            FeatureData fdat = super.makeFeatureData(featureId, type, uniqueName, name,
                    md5checksum, seqlen, organismId);
            cdnaCloneMap.put(name, fdat);
            return fdat;
        }
        return super.makeFeatureData(featureId, type, uniqueName, name,
                md5checksum, seqlen, organismId);
    }

    /**
     * Return a query that gets the feature_ids of the allele in the feature table.
     */
    private static String getAlleleFeaturesSql() {
        return "SELECT feature_id FROM " + ALLELE_TEMP_TABLE_NAME;
    }

    /**
     * Method to add dataSets and DataSources to items before storing
     */
    private void processItem(Item item, Integer taxonId) {
        String className = item.getClassName();
        if ("DataSource".equals(className)
            || "DataSet".equals(className)
            || "Organism".equals(className)
            || "Sequence".equals(className)) {
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
        }
        ChadoDBConverter converter = getChadoDBConverter();
        BioStoreHook.setDataSets(getModel(), item,
                converter.getDataSetItem(taxonId.intValue()).getIdentifier(),
                converter.getDataSourceItem().getIdentifier());
    }
}
