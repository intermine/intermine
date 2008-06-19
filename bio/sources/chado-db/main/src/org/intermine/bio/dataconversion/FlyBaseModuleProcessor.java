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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.intermine.bio.chado.ChadoCV;
import org.intermine.bio.chado.ChadoCVFactory;
import org.intermine.bio.chado.ChadoCVTerm;
import org.intermine.bio.util.OrganismData;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.IntPresentSet;
import org.intermine.util.XmlUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ReferenceList;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * A converter for chado that handles FlyBase specific configuration.
 * @author Kim Rutherford
 */
public class FlyBaseModuleProcessor extends ChadoSequenceProcessor
{
    /**
     * The cv.name for the FlyBase miscellaneous CV.
     */
    static final String FLY_BASE_MISCELLANEOUS_CV = "FlyBase miscellaneous CV";

    /**
     * A ConfigAction that changes FlyBase attribute tags (like "@FBcv0000289:hypomorph") to text
     * like: "hypomorph"
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
            Pattern p = Pattern.compile(FYBASE_PROP_ATTRIBUTE_PATTERN);
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

    private static final String FYBASE_PROP_ATTRIBUTE_PATTERN = "@([^@]+)@";

    private static final Logger LOG = Logger.getLogger(FlyBaseModuleProcessor.class);

    private final Map<Integer, MultiKeyMap> config = new HashMap<Integer, MultiKeyMap>();
    private final IntPresentSet locatedGeneIds = new IntPresentSet();

    private Map<String, Item> alleleIdMap = new HashMap<String, Item>();

    // an object representing the FlyBase miscellaneous CV
    private ChadoCV flyBaseMiscCv = null;

    private Map<String, Item> mutagensMap = new HashMap<String, Item>();

    private static final String ALLELE_TEMP_TABLE_NAME = "intermine_flybase_allele_temp";

    /**
     * Create a new FlyBaseChadoDBConverter.
     * @param chadoDBConverter the converter that created this object
     */
    public FlyBaseModuleProcessor(ChadoDBConverter chadoDBConverter) {
        super(chadoDBConverter);
        Connection connection;
        if (getDatabase() == null) {
            // no Database when testing and no connection needed
            connection = null;
        } else {
            try {
                connection = getDatabase().getConnection();
            } catch (SQLException e) {
                throw new RuntimeException("can't get connection to the database", e);
            }
        }

        try {
            flyBaseMiscCv = getFlyBaseMiscCV(connection);
        } catch (SQLException e) {
            throw new RuntimeException("can't execute query for flybase cv terms", e);
        }

        ResultSet res;
        try {
            res = getLocatedGenesResultSet(connection);
        } catch (SQLException e) {
            throw new RuntimeException("can't execute query", e);
        }

        try {
            while (res.next()) {
                int featureId = res.getInt("feature_id");
                locatedGeneIds.set(featureId, true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("problem while reading located genes", e);
        }
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
        LOG.info("executing: " + query);
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
     * Get ChadoCV object representing the FlyBase misc cv.
     * This is a protected method so that it can be overriden for testing
     * @param connection the database Connection
     * @return the cv
     * @throws SQLException if there is a database problem
     */
    protected ChadoCV getFlyBaseMiscCV(Connection connection) throws SQLException {
        ChadoCVFactory cvFactory = new ChadoCVFactory(connection);
        return cvFactory.getChadoCV(FLY_BASE_MISCELLANEOUS_CV);
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
     * Return from chado the feature_ids of the genes with entries in the featureloc table.
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getLocatedGenesResultSet(Connection connection) throws SQLException {
        String query = getLocatedGenesSql();
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Return a query that gets the feature_ids of gene that have locations.
     */
    private String getLocatedGenesSql() {
        String organismConstraint = getOrganismConstraint();
        String orgConstraintForQuery = "";
        if (!StringUtils.isEmpty(organismConstraint)) {
            orgConstraintForQuery = " AND " + organismConstraint;
        }

        return "SELECT feature.feature_id FROM feature, cvterm, featureloc"
            + "   WHERE feature.type_id = cvterm.cvterm_id"
            + "      AND feature.feature_id = featureloc.feature_id AND cvterm.name = 'gene'"
            + " " + orgConstraintForQuery;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<MultiKey, List<ConfigAction>> getConfig(int taxonId) {
        MultiKeyMap map = config.get(taxonId);
        if (map == null) {
            map = new MultiKeyMap();
            config.put(taxonId, map);

            // synomym configuration example: for features of class "Gene", if the type name of
            // the synonym is "fullname" and "is_current" is true, set the "name" attribute of
            // the new Gene to be this synonym and then make a Synonym object
            map.put(new MultiKey("synonym", "Gene", "fullname", Boolean.TRUE),
                    Arrays.asList(new SetFieldConfigAction("name"),
                                  CREATE_SYNONYM_ACTION));

            map.put(new MultiKey("synonym", "Gene", "fullname", Boolean.FALSE),
                    Arrays.asList(CREATE_SYNONYM_ACTION));
            map.put(new MultiKey("synonym", "Gene", "symbol", Boolean.TRUE),
                    Arrays.asList(new SetFieldConfigAction("symbol"),
                                  CREATE_SYNONYM_ACTION));
            map.put(new MultiKey("synonym", "Gene", "symbol", Boolean.FALSE),
                    Arrays.asList(CREATE_SYNONYM_ACTION));
            map.put(new MultiKey("synonym", "Gene", "symbol", Boolean.FALSE),
                    Arrays.asList(new SetMatchingFieldConfigAction("GLEANRsymbol", ".*GLEANR.*"),
                                  CREATE_SYNONYM_ACTION));


            // dbxref table configuration example: for features of class "Gene", where the
            // db.name is "FlyBase Annotation IDs" and "is_current" is true, set the
            // "secondaryIdentifier" attribute of the new Gene to be this dbxref and then make a
            // Synonym object
            map.put(new MultiKey("dbxref", "Gene", "FlyBase Annotation IDs", Boolean.TRUE),
                    Arrays.asList(new SetFieldConfigAction("secondaryIdentifier"),
                                  CREATE_SYNONYM_ACTION));
            map.put(new MultiKey("dbxref", "Gene", "FlyBase Annotation IDs", Boolean.FALSE),
                    Arrays.asList(CREATE_SYNONYM_ACTION));
            // null for the "is_current" means either TRUE or FALSE is OK.
            map.put(new MultiKey("dbxref", "Gene", "FlyBase", null),
                    Arrays.asList(CREATE_SYNONYM_ACTION));

            map.put(new MultiKey("dbxref", "MRNA", "FlyBase Annotation IDs", Boolean.TRUE),
                    Arrays.asList(new SetFieldConfigAction("secondaryIdentifier"),
                                  CREATE_SYNONYM_ACTION));
            map.put(new MultiKey("dbxref", "TransposableElementInsertionSite", "drosdel", null),
                    Arrays.asList(new SetFieldConfigAction("secondaryIdentifier")));

            map.put(new MultiKey("synonym", "ArtificialDeletion", "fullname", Boolean.TRUE),
                    Arrays.asList(new SetFieldConfigAction("name"),
                                  CREATE_SYNONYM_ACTION));

            map.put(new MultiKey("synonym", "MRNA", "symbol", Boolean.TRUE),
                    Arrays.asList(new SetFieldConfigAction("symbol"),
                                  CREATE_SYNONYM_ACTION));
            map.put(new MultiKey("synonym", "MRNA", "symbol", Boolean.FALSE),
                    Arrays.asList(CREATE_SYNONYM_ACTION));
            map.put(new MultiKey("dbxref", "MRNA", "FlyBase Annotation IDs", null),
                    Arrays.asList(CREATE_SYNONYM_ACTION));
            map.put(new MultiKey("dbxref", "MRNA", "FlyBase", null),
                    Arrays.asList(CREATE_SYNONYM_ACTION));

            map.put(new MultiKey("relationship", "Allele", "alleleof", "Gene"),
                    Arrays.asList(new SetFieldConfigAction("gene")));
            map.put(new MultiKey("relationship", "Translation", "producedby", "MRNA"),
                    Arrays.asList(new SetFieldConfigAction("MRNA")));

            // featureprop configuration example: for features of class "Gene", if the type name
            // of the prop is "cyto_range", set the "cytoLocation" attribute of the
            // new Gene to be this property
            map.put(new MultiKey("prop", "Gene", "cyto_range"),
                    Arrays.asList(new SetFieldConfigAction("cytoLocation")));
            map.put(new MultiKey("prop", "Gene", "symbol"),
                    Arrays.asList(CREATE_SYNONYM_ACTION));
            // the feature type for gene, eg. "rRNA_gene", "protein_coding_gene"
            map.put(new MultiKey("prop", "Gene", "promoted_gene_type"),
                    Arrays.asList(new SetFieldConfigAction("flyBaseFeatureType")));
            map.put(new MultiKey("prop", "TransposableElementInsertionSite",
                                 "curated_cytological_location"),
                                 Arrays.asList(new SetFieldConfigAction("cytoLocation")));
            ConfigAction alleleClassConfigAction = new AlleleClassSetFieldAction("alleleClass");
            map.put(new MultiKey("prop", "Allele", "promoted_allele_class"),
                    Arrays.asList(alleleClassConfigAction));

            // feature configuration example: for features of class "Exon", from "FlyBase",
            // set the Gene.symbol to be the "name" field from the chado feature
            map.put(new MultiKey("feature", "Exon", "FlyBase", "name"),
                    Arrays.asList(new SetFieldConfigAction("symbol"),
                                  CREATE_SYNONYM_ACTION));
            // DO_NOTHING_ACTION means skip the name from this feature
            map.put(new MultiKey("feature", "Chromosome", "FlyBase", "name"),
                    Arrays.asList(DO_NOTHING_ACTION));

            map.put(new MultiKey("feature", "ChromosomeBand", "FlyBase", "name"),
                    Arrays.asList(DO_NOTHING_ACTION));

            map.put(new MultiKey("feature", "TransposableElementInsertionSite", "FlyBase",
                                 "name"),
                    Arrays.asList(new SetFieldConfigAction("symbol"),
                                  new SetFieldConfigAction("name"),
                                  CREATE_SYNONYM_ACTION));

            map.put(new MultiKey("feature", "Gene", "FlyBase", "uniquename"),
                    Arrays.asList(new SetFieldConfigAction("primaryIdentifier")));
            map.put(new MultiKey("feature", "Gene", "FlyBase", "name"),
                    Arrays.asList(DO_NOTHING_ACTION));

            map.put(new MultiKey("feature", "ArtificialDeletion", "FlyBase", "name"),
                    Arrays.asList(new SetFieldConfigAction("secondaryIdentifier"),
                                  CREATE_SYNONYM_ACTION));

            map.put(new MultiKey("feature", "MRNA", "FlyBase", "uniquename"),
                    Arrays.asList(new SetFieldConfigAction("primaryIdentifier")));

            map.put(new MultiKey("feature", "PointMutation", "FlyBase", "uniquename"),
                    Arrays.asList(new SetFieldConfigAction("name"),
                                  new SetFieldConfigAction("primaryIdentifier"),
                                  CREATE_SYNONYM_ACTION));
            // name isn't set in flybase:
            map.put(new MultiKey("feature", "PointMutation", "FlyBase", "name"),
                    Arrays.asList(DO_NOTHING_ACTION));

            if (taxonId == 7227) {
                map.put(new MultiKey("dbxref", "Translation", "FlyBase Annotation IDs",
                                     Boolean.TRUE),
                                     Arrays.asList(new SetFieldConfigAction("secondaryIdentifier"),
                                                   CREATE_SYNONYM_ACTION));
                map.put(new MultiKey("feature", "Translation", "FlyBase", "name"),
                        Arrays.asList(new SetFieldConfigAction("symbol"),
                                      CREATE_SYNONYM_ACTION));
                map.put(new MultiKey("feature", "Translation", "FlyBase", "uniquename"),
                        Arrays.asList(new SetFieldConfigAction("primaryIdentifier")));
            } else {
                map.put(new MultiKey("feature", "Translation", "FlyBase", "uniquename"),
                        Arrays.asList(new SetFieldConfigAction("primaryIdentifier")));
                map.put(new MultiKey("feature", "Translation", "FlyBase", "name"),
                        Arrays.asList(new SetFieldConfigAction("symbol"),
                                      CREATE_SYNONYM_ACTION));
                map.put(new MultiKey("dbxref", "Translation", "GB_protein", null),
                        Arrays.asList(new SetFieldConfigAction("secondaryIdentifier"),
                                      CREATE_SYNONYM_ACTION));
            }
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

    private String getLocatedGeneAllesSql() {
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

        if (chadoFeatureType.equals("gene")) {
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

        if (taxonId != 7227 && chadoFeatureType.equals("chromosome_arm")) {
            // nothing is located on a chromosome_arm
            return null;
        }

        if (chadoFeatureType.equals("chromosome")
            && !uniqueName.equals("dmel_mitochondrion_genome")) {
            // ignore Chromosomes from flybase - features are located on ChromosomeArms except
            // for mitochondrial features
            return null;
        } else {
            if (chadoFeatureType.equals("chromosome_arm")
                || chadoFeatureType.equals("ultra_scaffold")) {
                if (uniqueName.equals("dmel_mitochondrion_genome")) {
                    // ignore - all features are on the Chromosome object with uniqueName
                    // "dmel_mitochondrion_genome"
                    return null;
                } else {
                    realInterMineType = "Chromosome";
                }
            }
        }
        if (chadoFeatureType.equals("golden_path_region")) {
            // For organisms other than D. melanogaster sometimes we can convert a
            // golden_path_region to an actual chromosome: if name is 2L, 4, etc
            if (taxonId == 7237) {
                // chromosomes are stored as golden_path_region
                realInterMineType = "Chromosome";
            } else {
                if (taxonId != 7227 && !uniqueName.contains("_")) {
                    realInterMineType = "Chromosome";
                } else {
                    // golden_path_fragment is the actual SO term
                    realInterMineType = "GoldenPathFragment";
                }
            }
        }
        if (chadoFeatureType.equals("chromosome_structure_variation")) {
            if (uniqueName.startsWith("FBab") && name.matches("Df\\(.*\\)ED\\d+")) {
                realInterMineType = "ArtificialDeletion";
            } else {
                return null;
            }
        }
        if (chadoFeatureType.equals("protein")) {
            if (uniqueName.startsWith("FBpp")) {
                realInterMineType = "Translation";
            } else {
                return null;
            }
        }
        if (chadoFeatureType.equals("transposable_element_insertion_site")
                        && name == null && !uniqueName.startsWith("FBti")) {
            // ignore this feature as it doesn't have an FBti identifier and there will be
            // another feature for the same transposable_element_insertion_site that does have
            // the FBti identifier
            return null;
        }
        if (chadoFeatureType.equals("mRNA") && seqlen == 0) {
            // flybase has > 7000 mRNA features that have no sequence and don't appear in their
            // webapp so we filter them out
            return null;
        }
        if (chadoFeatureType.equals("protein") && seqlen == 0) {
            // flybase has ~ 2100 protein features that don't appear in their webapp so we
            // filter them out
            return null;
        }

        Item feature = getChadoDBConverter().createItem(realInterMineType);

        alleleIdMap.put(uniqueName, feature);

        return feature;
    }

    private static final List<String> FEATURES = Arrays.asList(
            "gene", "mRNA", "transcript",
            "intron", "exon",
            "regulatory_region", "enhancer",
            // ignore for now:        "EST", "cDNA_clone",
            "miRNA", "snRNA", "ncRNA", "rRNA", "ncRNA", "snoRNA", "tRNA",
            "chromosome_band", "transposable_element_insertion_site",
            "chromosome_structure_variation",
            "protein", "point_mutation"
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

        for (FeatureData featureData: features.values()) {
            if ((featureData.flags & FeatureData.IDENTIFIER_SET) == 0) {
                setAttribute(featureData.getIntermineObjectId(), "primaryIdentifier",
                             featureData.getChadoFeatureUniqueName());
            }
        }

        processAlleleProps(connection, features);

        Map<Integer, List<String>> mutagenMap = makeMutagenMap(connection);
        for (Integer alleleFeatureId: mutagenMap.keySet()) {
            FeatureData alleleDat = features.get(alleleFeatureId);
            List<String> mutagenRefIds = new ArrayList<String>();
            for (String mutagenDescription: mutagenMap.get(alleleFeatureId)) {
                Item mutagen = getMutagen(mutagenDescription);
                mutagenRefIds.add(mutagen.getIdentifier());
            }
            ReferenceList referenceList = new ReferenceList();
            referenceList.setName("mutagens");
            referenceList.setRefIds(mutagenRefIds);
            getChadoDBConverter().store(referenceList, alleleDat.getIntermineObjectId());
        }
    }

    private Item getMutagen(String description) throws ObjectStoreException {
        if (mutagensMap.containsKey(description)) {
            return mutagensMap.get(description);
        } else {
            Item mutagen = getChadoDBConverter().createItem("Mutagen");
            mutagen.setAttribute("description", description);
            mutagensMap.put(description, mutagen);
            store(mutagen);
            return mutagen;
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
            if (propType.equals("derived_pheno_manifest")) {
                phenotypeAnnotation =
                    makePhenotypeAnnotation(alleleItemIdentifier, value,
                                            dataSetItem, annotationPubMap.get(featurePropId));
                phenotypeAnnotation.setAttribute("annotationType", "manifest in");
            } else {
                if (propType.equals("derived_pheno_class")) {
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

            Set<ChadoCVTerm> parents = cvterm.getAllParents();

            for (ChadoCVTerm parent: parents) {
                if (parent.getName().equals("origin of mutation")) {
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
        LOG.info("executing: " + query);
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

            String pubicationItemIdentifier = makePublication(Integer.parseInt(pubDbId));

            if (!retMap.containsKey(featurePropId)) {
                retMap.put(featurePropId, new ArrayList<String>());
            }
            retMap.get(featurePropId).add(pubicationItemIdentifier);
        }

        return retMap;
    }

    private Item makePhenotypeAnnotation(String alleleItemIdentifier, String value,
                                         Item dataSetItem, List<String> publicationsItemIdList)
        throws ObjectStoreException {
        Item phenotypeAnnotation = getChadoDBConverter().createItem("PhenotypeAnnotation");
        phenotypeAnnotation.addToCollection("dataSets", dataSetItem);

        Pattern p = Pattern.compile(FYBASE_PROP_ATTRIBUTE_PATTERN);
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
                if (identifier.startsWith("FBbt")) {
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
        phenotypeAnnotation.setReference("subject", alleleItemIdentifier);
        if (publicationsItemIdList != null && publicationsItemIdList.size() > 0) {
            ReferenceList pubReferenceList =
                new ReferenceList("publications", publicationsItemIdList);
            phenotypeAnnotation.addCollection(pubReferenceList);
        }

        if (dbAnatomyTermIdentifiers.size() == 1) {
            String anatomyIdentifier = dbAnatomyTermIdentifiers.get(0);
            String anatomyTermItemId = makeAnatomyTerm(anatomyIdentifier);
            phenotypeAnnotation.setReference("anatomyTerm", anatomyTermItemId);
            phenotypeAnnotation.setReference("property", anatomyTermItemId);
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
            phenotypeAnnotation.setReference("property", developmentTermItemId);
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
        } else {
            return identifier;
        }
    }

    /**
     * Return the item identifiers of the alleles metioned in the with clauses of the argument.
     * Currently unused because flybase with clauses are wrong - see ticket #889
     */
    @SuppressWarnings("unused")
    private List<String> findWithAllele(String value) {
        Pattern p = Pattern.compile("with @(FBal\\d+):");
        Matcher m = p.matcher(value);

        List<String> foundIdentifiers = new ArrayList<String>();

        while (m.find()) {
            String identifier = m.group(1);
            if (identifier.startsWith("FBal")) {
                foundIdentifiers.add(identifier);
            } else {
                throw new RuntimeException("identifier in a with must start: \"FBal\" not: "
                                           + identifier);
            }
        }

        List<String> alleleItemIdentifiers = new ArrayList<String>();

        for (String foundIdentifier: foundIdentifiers) {
            if (alleleIdMap.containsKey(foundIdentifier)) {
                alleleItemIdentifiers.add(alleleIdMap.get(foundIdentifier).getIdentifier());
            } else {
                // this allele wasn't stored so probably it didn't have the right organism - some
                // GAL4 alleles have cerevisiae as organism, eg. FBal0060667:Scer\GAL4[sd-SG29.1]
                // referenced by FBal0038994 Rac1[N17.Scer\UAS]
            }
        }

        return alleleItemIdentifiers;
    }

    private String makeAnatomyTerm(String identifier) throws ObjectStoreException {
        if (anatomyTermMap.containsKey(identifier)) {
            return anatomyTermMap.get(identifier);
        } else {
            Item anatomyTerm = getChadoDBConverter().createItem("AnatomyTerm");
            anatomyTerm.setAttribute("identifier", identifier);
            getChadoDBConverter().store(anatomyTerm);
            anatomyTermMap.put(identifier, anatomyTerm.getIdentifier());
            return anatomyTerm.getIdentifier();
        }
    }

    private String makeDevelopmentTerm(String identifier) throws ObjectStoreException {
        if (developmentTermMap.containsKey(identifier)) {
            return developmentTermMap.get(identifier);
        } else {
            Item developmentTerm = getChadoDBConverter().createItem("DevelopmentTerm");
            developmentTerm.setAttribute("identifier", identifier);
            getChadoDBConverter().store(developmentTerm);
            developmentTermMap.put(identifier, developmentTerm.getIdentifier());
            return developmentTerm.getIdentifier();
        }
    }

    private String makeCVTerm(String identifier) throws ObjectStoreException {
        if (cvTermMap.containsKey(identifier)) {
            return cvTermMap.get(identifier);
        } else {
            Item cvTerm = getChadoDBConverter().createItem("CVTerm");
            cvTerm.setAttribute("identifier", identifier);
            getChadoDBConverter().store(cvTerm);
            cvTermMap.put(identifier, cvTerm.getIdentifier());
            return cvTerm.getIdentifier();
        }
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
        LOG.info("executing: " + query);
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
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
    }

    /**
     * Convert ISO entities from FlyBase to HTML entities.
     * {@inheritDoc}
     */
    @Override
    protected String fixIdentifier(String type, String identifier) {
        return XmlUtil.fixEntityNames(identifier);
    }

    private String getAlleleFeaturesSql() {
        return "SELECT feature_id FROM " + ALLELE_TEMP_TABLE_NAME;
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
            ChadoDBConverter converter = getChadoDBConverter();
            DataSetStoreHook.setDataSets(getModel(), item,
                                         converter.getDataSetItem(taxonId).getIdentifier(),
                                         converter.getDataSourceItem().getIdentifier());
        }
    }
}
