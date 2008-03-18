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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.log4j.Logger;
import org.intermine.bio.util.OrganismData;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.IntPresentSet;
import org.intermine.xml.full.Item;

/**
 * A converter for chado that handles FlyBase specific configuration.
 * @author Kim Rutherford
 */
public class FlyBaseModuleProcessor extends ChadoSequenceProcessor
{
    private static final Logger LOG = Logger.getLogger(FlyBaseModuleProcessor.class);


    private Map<Integer, MultiKeyMap> config = new HashMap<Integer, MultiKeyMap>();
    private IntPresentSet locatedGeneIds = new IntPresentSet();

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
     * Return from chado the feature_ids of the genes with entries in the featureloc table.
     * @param connection the db connection
     * @return the SQL result set
     * @throws SQLException if a database problem occurs
     */
    protected ResultSet getLocatedGenesResultSet(Connection connection) throws SQLException {
        String query =
            "SELECT feature.feature_id FROM feature, cvterm, featureloc"
            + "   WHERE feature.type_id = cvterm.cvterm_id"
            + "      AND feature.feature_id = featureloc.feature_id AND cvterm.name = 'gene'";
        LOG.info("executing: " + query);
        Statement stmt = connection.createStatement();
        ResultSet res = stmt.executeQuery(query);
        return res;
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

            map.put(new MultiKey("synonym", "ChromosomalDeletion", "fullname", Boolean.TRUE),
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

            map.put(new MultiKey("relationship", "Translation", "producedby", "MRNA"),
                    Arrays.asList(new SetFieldConfigAction("MRNA")));

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

            map.put(new MultiKey("feature", "ChromosomalDeletion", "FlyBase", "name"),
                    Arrays.asList(new SetFieldConfigAction("name"),
                                  new SetFieldConfigAction("symbol"),
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
        return "NOT (cvterm.name = 'gene' AND uniquename LIKE 'FBal%') "
            + "AND NOT ((cvterm.name = 'golden_path_region'"
            + "          OR cvterm.name = 'ultra_scaffold')"
            + "         AND (uniquename LIKE 'Unknown_%' OR uniquename LIKE '%_groupMISC'))";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Item makeFeature(Integer featureId, String chadoFeatureType, String interMineType,
                               String name, String uniqueName,
                               int seqlen, int taxonId) {
        String realInterMineType = interMineType;

        if (chadoFeatureType.equals("gene") && !locatedGeneIds.contains(featureId.intValue())) {
            // ignore genes with no location
            return null;
        }

        // avoid allele features that have type 'gene'
        if (uniqueName.startsWith("FBal")) {
            return null;
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
            if (uniqueName.startsWith("FBab")) {
                realInterMineType = "ChromosomalDeletion";
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
     * For objects that don't have identifier == null, set the identifier to be the uniquename
     * column from chado.
     * {@inheritDoc}
     */
    @Override
    protected void extraProcessing(Map<Integer, FeatureData> features)
        throws ObjectStoreException {
        for (FeatureData featureData: features.values()) {
            if ((featureData.flags & FeatureData.IDENTIFIER_SET) == 0) {
                setAttribute(featureData.getIntermineObjectId(), "primaryIdentifier",
                             featureData.getChadoFeatureUniqueName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDataSourceName(@SuppressWarnings("unused") int taxonId) {
        return "FlyBase";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Item getDataSourceItem(int taxonId) {
        return getChadoDBConverter().getDataSourceItem(getDataSourceName(taxonId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Item getDataSetItem(int taxonId) {
        OrganismRepository or = OrganismRepository.getOrganismRepository();
        OrganismData od = or.getOrganismDataByTaxon(taxonId);
        String species = od.getSpecies();
        String genus = od.getGenus();
        String name = "FlyBase " + genus + " " + species + " data set";
        String description = "The FlyBase " + genus + " " + species + " genome";
        Item dataSetItem =
            getChadoDBConverter().getDataSetItem(name, "http://www.flybase.org", description);
        return dataSetItem;
    }
}
