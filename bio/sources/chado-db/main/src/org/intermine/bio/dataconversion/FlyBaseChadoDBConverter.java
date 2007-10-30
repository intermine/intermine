package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.sql.Database;
import org.intermine.xml.full.Item;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.collections.map.MultiKeyMap;

/**
 * A converter for chado that handles FlyBase specific configuration.
 * @author Kim Rutherford
 */
public class FlyBaseChadoDBConverter extends ChadoDBConverter
{
    private MultiKeyMap config;

    /**
     * Create a new FlyBaseChadoDBConverter.
     * @param database
     * @param tgtModel
     * @param writer
     */
    public FlyBaseChadoDBConverter(Database database, Model tgtModel, ItemWriter writer) {
        super(database, tgtModel, writer);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<MultiKey, List<ConfigAction>> getConfig() {
       if (config == null) {
           config = new MultiKeyMap();
           if (getTaxonIdInt() == 7227 || getTaxonIdInt() == 7237) {

               // synomym configuration example: for features of class "Gene", if the type name of
               // the synonym is "fullname" and "is_current" is true, set the "name" attribute of
               // the new Gene to be this synonym and then make a Synonym object
               config.put(new MultiKey("synonym", "Gene", "fullname", Boolean.TRUE),
                          Arrays.asList(new SetFieldConfigAction("name"),
                                        CREATE_SYNONYM_ACTION));

               config.put(new MultiKey("synonym", "Gene", "fullname", Boolean.FALSE),
                          Arrays.asList(CREATE_SYNONYM_ACTION));
               config.put(new MultiKey("synonym", "Gene", "symbol", Boolean.TRUE),
                          Arrays.asList(new SetFieldConfigAction("symbol"),
                                        CREATE_SYNONYM_ACTION));
               config.put(new MultiKey("synonym", "Gene", "symbol", Boolean.FALSE),
                          Arrays.asList(CREATE_SYNONYM_ACTION));

               // dbxref table configuration example: for features of class "Gene", where the
               // db.name is "FlyBase Annotation IDs" and "is_current" is true, set the "identifier"
               // attribute of the new Gene to be this dbxref and then make a Synonym object
               config.put(new MultiKey("dbxref", "Gene", "FlyBase Annotation IDs", Boolean.TRUE),
                          Arrays.asList(new SetFieldConfigAction("identifier"),
                                        CREATE_SYNONYM_ACTION));
               config.put(new MultiKey("dbxref", "Gene", "FlyBase Annotation IDs", Boolean.FALSE),
                          Arrays.asList(CREATE_SYNONYM_ACTION));
               // null for the "is_current" means either TRUE or FALSE is OK.
               config.put(new MultiKey("dbxref", "Gene", "FlyBase", null),
                          Arrays.asList(CREATE_SYNONYM_ACTION));

               config.put(new MultiKey("synonym", "ChromosomalDeletion", "fullname", Boolean.TRUE),
                          Arrays.asList(new SetFieldConfigAction("name"),
                                        CREATE_SYNONYM_ACTION));

               config.put(new MultiKey("synonym", "MRNA", "symbol", Boolean.TRUE),
                          Arrays.asList(new SetFieldConfigAction("identifier"),
                                        new SetFieldConfigAction("symbol"),
                                        CREATE_SYNONYM_ACTION));
               config.put(new MultiKey("synonym", "MRNA", "symbol", Boolean.FALSE),
                          Arrays.asList(CREATE_SYNONYM_ACTION));
               config.put(new MultiKey("dbxref", "MRNA", "FlyBase Annotation IDs", null),
                          Arrays.asList(CREATE_SYNONYM_ACTION));
               config.put(new MultiKey("dbxref", "MRNA", "FlyBase", null),
                          Arrays.asList(CREATE_SYNONYM_ACTION));

               config.put(new MultiKey("relationship", "Translation", "producedby", "MRNA"),
                          Arrays.asList(new SetFieldConfigAction("MRNA")));

               // featureprop configuration example: for features of class "Gene", if the type name
               // of the prop is "cyto_range", set the "cytoLocation" attribute of the
               // new Gene to be this property
               config.put(new MultiKey("prop", "Gene", "cyto_range"),
                          Arrays.asList(new SetFieldConfigAction("cytoLocation")));
               config.put(new MultiKey("prop", "Gene", "symbol"),
                          Arrays.asList(CREATE_SYNONYM_ACTION));

               // feature configuration example: for features of class "Exon", from "FlyBase",
               // set the Gene.symbol to be the "name" field from the chado feature
               config.put(new MultiKey("feature", "Exon", "FlyBase", "name"),
                          Arrays.asList(new SetFieldConfigAction("symbol"),
                                        CREATE_SYNONYM_ACTION));
               // DO_NOTHING_ACTION means skip the name from this feature
               config.put(new MultiKey("feature", "Chromosome", "FlyBase", "name"),
                          Arrays.asList(DO_NOTHING_ACTION));

               config.put(new MultiKey("feature", "ChromosomeBand", "FlyBase", "name"),
                          Arrays.asList(DO_NOTHING_ACTION));

               config.put(new MultiKey("feature", "TransposableElementInsertionSite", "FlyBase",
                                       "name"),
                                       Arrays.asList(new SetFieldConfigAction("symbol"),
                                                     new SetFieldConfigAction("identifier"),
                                                     CREATE_SYNONYM_ACTION));
               config.put(new MultiKey("feature", "TransposableElementInsertionSite", "FlyBase",
                                       "uniquename"),
               Arrays.asList(new SetFieldConfigAction("organismDbId")));

               config.put(new MultiKey("feature", "Gene", "FlyBase", "uniquename"),
                          Arrays.asList(new SetFieldConfigAction("organismDbId")));
               config.put(new MultiKey("feature", "Gene", "FlyBase", "name"),
                          Arrays.asList(DO_NOTHING_ACTION));

               config.put(new MultiKey("feature", "ChromosomalDeletion", "FlyBase", "name"),
                          Arrays.asList(new SetFieldConfigAction("symbol"),
                                        CREATE_SYNONYM_ACTION));

               config.put(new MultiKey("feature", "MRNA", "FlyBase", "uniquename"),
                          Arrays.asList(new SetFieldConfigAction("organismDbId")));

               config.put(new MultiKey("feature", "PointMutation", "FlyBase", "uniquename"),
                          Arrays.asList(new SetFieldConfigAction("name"),
                                        new SetFieldConfigAction("identifier"),
                                        CREATE_SYNONYM_ACTION));
               // name isn't set in flybase:
               config.put(new MultiKey("feature", "PointMutation", "FlyBase", "name"),
                          Arrays.asList(DO_NOTHING_ACTION));

               if (getTaxonIdInt() == 7227) {
                   config.put(new MultiKey("feature", "Translation", "FlyBase", "name"),
                              Arrays.asList(new SetFieldConfigAction("identifier"),
                                            new SetFieldConfigAction("symbol"),
                                            CREATE_SYNONYM_ACTION));
                   config.put(new MultiKey("feature", "Translation", "FlyBase", "uniquename"),
                              Arrays.asList(new SetFieldConfigAction("organismDbId")));
               } else {
                   config.put(new MultiKey("feature", "Translation", "FlyBase", "uniquename"),
                              Arrays.asList(new SetFieldConfigAction("organismDbId")));
                   config.put(new MultiKey("feature", "Translation", "FlyBase", "name"),
                              Arrays.asList(new SetFieldConfigAction("symbol"),
                                            CREATE_SYNONYM_ACTION));
                   config.put(new MultiKey("dbxref", "Translation", "GB_protein", null),
                              Arrays.asList(new SetFieldConfigAction("identifier"),
                                            CREATE_SYNONYM_ACTION));
               }
           }
       }

       return config;
    }

    /**
     * Make and store a new feature
     * @param featureId the chado feature id
     * @param chadoFeatureType the chado feature type (a SO term)
     * @param interMineType the InterMine type of the feature
     * @param name the name
     * @param uniqueName the uniquename
     * @param seqlen the sequence length (if known)
     */
    @Override
    protected Item makeFeature(Integer featureId, String chadoFeatureType, String interMineType,
                               String name, String uniqueName,
                               int seqlen) {
        String realInterMineType = interMineType;

        // XXX FIMXE TODO HACK for flybase - this should be configured somewhere
        if (uniqueName.startsWith("FBal")) {
            return null;
        }

        // XXX FIMXE TODO HACK for flybase - this should be configured somewhere
        if (getTaxonIdInt() == 7227 || getTaxonIdInt() == 7237) {
            if (chadoFeatureType.equals("chromosome")
                && !uniqueName.equals("dmel_mitochondrion_genome")) {
                // ignore Chromosomes from flybase - features are located on ChromosomeArms except
                // for mitochondrial features
                return null;
            } else {
                if (chadoFeatureType.equals("chromosome_arm")) {
                    if (uniqueName.equals("dmel_mitochondrion_genome")) {
                        // ignore - all features are on the Chromosome object with uniqueName
                        // "dmel_mitochondrion_genome"
                        return null;
                    } else {
                        realInterMineType = "Chromosome";
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
        }

        Item feature = createItem(realInterMineType);

        return feature;
    }
}
