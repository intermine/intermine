package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import java.util.Arrays;
import java.util.Comparator;

import org.intermine.bio.chado.ChadoCV;
import org.intermine.bio.chado.ChadoCVTerm;

import com.mockobjects.sql.MockMultiRowResultSet;

/**
 * An implementation of FlyBaseProcessor for testing.
 * @author Kim Rutherford
 */
public class TestFlyBaseProcessor extends FlyBaseProcessor
{

    /**
     * Create a new TestFlyBaseModuleProcessor object.
     * @param chadoDBConverter the ChadoDBConverter
     * @throws SQLException not used, we are using dummy results
     */
    public TestFlyBaseProcessor(ChadoDBConverter chadoDBConverter)
    throws SQLException {
        super(chadoDBConverter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createFeatureTempTable(@SuppressWarnings("unused") Connection connection) {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createInsertionTempTable(@SuppressWarnings("unused") Connection connection) {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createLocatedGenesTempTable(Connection connection) throws SQLException {
        // empty
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getFeatureTableResultSet(@SuppressWarnings("unused")
            Connection connection) {
        String[] columnNames = new String[] {
            "feature_id", "name", "uniquename", "type", "seqlen", "residues", "md5checksum",
            "organism_id"
        };
        Object[][] resObjects = new Object[][] {
            {
                23269151, "4.5SRNA", "FBgn0000001", "gene", 1001,
                "acgacagatcattccacttttgacagctcactcggcagtaccagaaaatcc",
                "4946c54eee3ca803caac460c6bc68db4", 1
            },
            {
                10012345, "CG12345", "FBgn012345", "gene", 200,
                "atatagctagctaggaggattattatta",
                "b5cd41ab845e765d3c3a899d3c8079df", 1
            },
            {
                3117509, "CG10006", "FBgn0036461", "gene", 5023,
                "gtcatgcactactatccagttcaccaggctaaagtcggctcctat",
                "b5489168d1ccb0d60af43fa4a86506dd", 1
            },
            {
                411, "CG10000-RA", "FBtr0085315", "mRNA", 2528,
                "tctgcctcccaactacaatcagatgaactccaaccccaccac",
                "ba86b4c716043e4519f60f367a9d6747", 1
            },
            {
                412, "CG10000-RB", "FBtr0085316", "mRNA", 3000,
                "cccgcagcatgtgcatcagcagcatgtgtcatcggacgag",
                "c6a4e0fc5f06d22c721467d7e5e5c466", 1
            },
            {
                413, "CG10000-RC", "FBtr0085317", "mRNA", 2528,
                "tctgcctcccaactacaatcagatgaactccaaccccaccac",
                "ba86b4c716043e4519f60f367a9d6747", 1
            },
            {
                11494725, "3", "3L", "chromosome_arm", 24543557,
                "ccacgactcgcagagggtgaagcttaagcgatcacggac"
                + "tcaatcctggcatcccagttcccaaagtcaggtccccacgagcatg",
                "d98e073e7d7197e7b761b34c2f157902", 1
            },
            {
                11494726, "3", "3R", "chromosome_arm", 27905053,
                "ggtggaacttgagaacgagttcaagagcaacatgtact"
                + "gatgaatctgtcgtggggcgagcctgctgccaagtcgagaaagctgag",
                "1333d3694901401350b87065080e82a7", 1
            },
            {
                3175412, "CG10000:1", "CG10000:1", "exon", 148,
                "gattgctcagcgcgaacg",
                "d858eb017a9da2b5f832910d381cc6c1", 1
            },
            {
                3175413, "CG10000:2", "CG10000:2", "exon", 161,
                "cttgtccctgtg",
                "afff961b72f716220e8e69914dbfbb96", 1
            },
            {
                88888888, "CG88888-RA_prot", "FBpp88888", "protein", 41,
                "MTRYKQTEFTEDDSSSIGGIQLNEATGHTGMQIRYHTARAT",
                "824c7d2cbdc711dd49892c63b71832ac", 1
            },
            {
                1000000, "CG10000-RA_prot", "FBpp10000", "protein", 41,
                "MTRYKQTEFTEDDSSSIGGIQLNEATGHTGMQIRYHTARAT",
                "824c7d2cbdc711dd49892c63b71832ac", 1
            },
            {
                1000001, "CG99999-RB_prot", "FBpp99999", "protein", 63,
                "VSFAQVWCSSTTDETNLLQMEKDPHSPSQFRVIGTLSNMKEFAEVFQCKPGKRMNPTEKCEVW",
                "4bdcdd74743c6326f277da6fe9e07f2a", 1
            },
            {
                1000002, "", "", "protein", null,
                "VSFAQVWCSSTTDETNLLQMEKDPHSPSQFRVIGTLSNMKEFAEVFQCKPGKRMNPTEKCEVW",
                "4bdcdd74743c6326f277da6fe9e07f2a", 1
            },
            {
                // this is actually an allele
                2340000, "CG10006[GD2461]", "FBal0198867", "gene", null,
                null, null, 1
            },
            {
                // this is actually an allele
                2345000, "Scer\\GAL4[sd-SG29.1]", "FBal0060667", "gene", null,
                null, null, 1
            },
            {
                7000000, null, "&bgr;Tub85D[10g]", "point_mutation", null,
                null, null, 1
            },
            {
                8747247, "P{RS3}CB-5069-3", "FBti0028380",
                "transposable_element_insertion_site" , null, null, null, 1
            },
            {
                11488812, "Df(2L)ED482", "FBab0032193",
                "chromosome_structure_variation", null, null, null, 1
            },
            {
                11488720, "Df(2L)ED1454", "FBab0031842",
                "chromosome_structure_variation", null, null, null, 1
            },
            {
                11380181, "T(2;3)V21", "FBab0010281",
                "chromosome_structure_variation", null, null, null, 1
            },
            {
                8747905, "P{RS3}CB-0697-3", "FBti0028225",
                "transposable_element_insertion_site", null, null, null, 1
            },
            {
                11432358, "P{RS5r}5-SZ-4122", "FBti0032815",
                "transposable_element_insertion_site", null, null, null, 1
            },
            {
                11431518, "P{RS5r}5-HA-1496", "FBti0031976",
                "transposable_element_insertion_site", null, null, null, 1
            },
            {
                11430370, "P{RS3r}CB-5069-3", "FBti0030830",
                "transposable_element_insertion_site", null, null, null, 1
            },
            {
                11430215, "P{RS3r}CB-0697-3", "FBti0030675",
                "transposable_element_insertion_site", null, null, null, 1
            },
            {
                8748527, "P{RS5}5-SZ-4122", "FBti0030367",
                "transposable_element_insertion_site", null, null, null, 1
            },
            {
                8747724, "P{RS5}5-HA-1496", "FBti0029528",
                "transposable_element_insertion_site", null, null, null, 1
            },
            {
                8862364, "PBac{WH}f07990", "FBti0068355",
                "transposable_element_insertion_site", null, null, null, 1
            },
            {
                11507367, "UUGC0315", "FBcl0000001",
                "cDNA_clone", null, null, null, 1
            },
            {
                11507368, "UUGC0315", "FBcl0000002",
                "cDNA_clone", null, null, null, 1
            }
        };
        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    private Object[][] featureRelationshipTestHelper(boolean subjectIsFirst) {
        Object[][] data = new Object[][] {
            {
                500 ,    411 ,   3117509 , "partof"
            },
            {
                601 ,    3175412 ,   411 , "partof"
            },
            {
                602 ,    3175412 ,   412 , "partof"
            },
            {
                603 ,    3175413 ,   411 , "partof"
            },
            {
                604 ,    3175413 ,   412 , "partof"
            },
            {
                700 ,    1000000 ,   411 , "producedby"
            },
            {
                701 ,    88888888,   413 , "producedby"
            },
            {
                800 ,    2340000 ,   3117509 , "alleleof"
            }
        };

        Object[][] returnVal;

        if (subjectIsFirst) {
            returnVal = data;
        } else {
            returnVal = new Object[data.length][];

            for (int i = 0; i < data.length; i++) {
                returnVal[i] = data[i];
                int tempVal = (Integer) returnVal[i][1];
                returnVal[i][1] = returnVal[i][2];
                returnVal[i][2] = tempVal;
            }
        }

        // results must be sorted by the feature1_id
        Arrays.sort(returnVal, new Comparator() {
            public int compare(Object o1, Object o2) {
                final Integer i1 = (Integer) ((Object[]) o1)[1];
                final Integer i2 = (Integer) ((Object[]) o2)[1];
                if (i1 < i2) {
                    return -1;
                } else {
                    if (i1 > i2) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            }

        });
        return returnVal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getFeatureRelationshipResultSet(@SuppressWarnings("unused")
                                                        Connection connection,
                                                        boolean subjectIsFirst) {
        String[] columnNames = new String[] {
            "feature_relationship_id", "feature1_id", "feature2_id", "type_name"
        };
        // results must be ordered by subject_id
        Object[][] resObjects = featureRelationshipTestHelper(subjectIsFirst);
        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getDbxrefResultSet(@SuppressWarnings("unused")
                                           Connection connection) {
        String[] columnNames = new String[] {
            "feature_id", "accession", "db_name", "is_current"
        };
        Object[][] resObjects = new Object[][] {
            {
                23269151, "FBgn0000001_dbxref1", "FlyBase Annotation IDs", true
            },
            {
                23269151, "FBgn0000001_dbxref2", "FlyBase Annotation IDs", false
            },
            {
                23269151, "FBgn0000001_dbxref3", "FlyBase", true
            },
            {
                3117509, "FBgn0036461_dbxref4", "FlyBase", false
            },
        };
        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getFeaturePropResultSet(@SuppressWarnings("unused")
                                                Connection connection) {
        String[] columnNames = new String[] {
            "feature_id", "value", "type_name"
        };
        Object[][] resObjects = new Object[][] {
            {
                23269151, "3-[21]", "genetic_location"
            },
            {
                23269151, "3-[21]", "promoted_genetic_location"
            },
            {
                23269151, "A function for the 4.5S RNA is unknown", "misc"
            },
            {
                23269151, "non_protein_coding_gene", "promoted_gene_type"
            },
            {
                3117509, "71B1-71B1", "cyto_range"
            },
            {
                3117509, "AE014296", "gbunit"
            },
            {
                3117509, "CG10006_symbol", "symbol"
            },
            {
                3117509, "CG10006_SYMBOL", "symbol"
            },
            {
                3117509, "Not in SwissProt real (computational)", "sp_status"
            },
            {
                3117509, "protein_coding_gene", "promoted_gene_type"
            },
            {
                2340000, "@FBcv0000289:hypomorph@", "promoted_allele_class"
            }
        };

        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getLibraryFeatureResultSet(@SuppressWarnings("unused")
                                                Connection connection) {
        String[] columnNames = new String[] {
            "feature_id", "value", "type_name"
        };
        Object[][] resObjects = new Object[][] {
            {
                11507367, "adult stage | female", "stage"
            },
            {
                11507367, "bar", "foo"
            }
        };

        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getLibraryCVTermResultSet(@SuppressWarnings("unused")
                                                Connection connection) {
        String[] columnNames = new String[] {
            "feature_id", "term_identifier"
        };
        Object[][] resObjects = new Object[][] {
            {
                11507367, "00004958"
            },
            {
                2340000, "00001234"
            }
        };

        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getCDNALengthResultSet(Connection connection)
                    throws SQLException {
        String[] columnNames = new String[] {
            "feature_id", "seqlen"
        };
        Object[][] resObjects = new Object[][] {
            {
                11507367, 100
            }
        };

        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getFeatureLocResultSet(@SuppressWarnings("unused") Connection connection) {
        String[] columnNames = new String[] {
            "featureloc_id", "feature_id", "srcfeature_id", "fmin", "is_fmin_partial",
            "fmax", "is_fmax_partial", "strand"
        };
        MockMultiRowResultSet res = new MockMultiRowResultSet();

        res.setupRows(new Object[][] {
            {
                23774567, 3117509, 11494725, 14985571, false, 14990594, false, 1
            },
            {
                99123456, 23269151, 11494725, 101000, false, 102000, false, 1
            },
            {
                3201099, 411, 11494726, 24574104, false, 24577313, false, -1
            },
            {
                3201101, 3175413, 11494726, 24576946, false, 24577107, false, -1
            },
            {
                3201100, 3175412, 11494726, 24577165, false, 24577313, false, -1
            }
        });
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getMatchLocResultSet(Connection connection) throws SQLException {
        String[] columnNames = new String[] {
            "featureloc_id", "feature_id", "srcfeature_id", "fmin", "is_fmin_partial",
            "fmax", "is_fmax_partial", "strand"
        };
        MockMultiRowResultSet res = new MockMultiRowResultSet();

        res.setupRows(new Object[][] {

        });
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getSynonymResultSet(@SuppressWarnings("unused") Connection connection) {
        String[] columnNames = new String[] {
            "feature_id", "synonym_name", "type_name", "is_current"
        };
        MockMultiRowResultSet res = new MockMultiRowResultSet();
        Object[][] resObjects = new Object[][] {
            {
                23269151, "FBgn0000001_fullname_synonym", "fullname", true
            },
            {
                23269151, "FBgn0000001_symbol_synonym", "symbol", true
            },
            {
                23269151, "FBgn0000001_symbol_SYNONYM", "symbol", false
            },
            {
                23269151, "dmel_GLEANR_1_synonym", "symbol", false
            },
            {
                3117509, "FBgn0036461_symbol_3", "other", false
            },
            {
                413, "CG10000-RC", "symbol", true
            },
        };
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getPubResultSet(@SuppressWarnings("unused") Connection connection) {
        String[] columnNames = new String[] {
            "feature_id", "pub_db_identifier"
        };
        Object[][] resObjects = new Object[][] {
            {
                23269151, "8344257"
            },
            {
                23269151, "8543160"
            },
            {
                3117509, "2892759"
            },
            {
                3175412, "8344257"
            },
            {
                3175412, "2612903"
            },
            {
                3175412, "7720555"
            },
            {
                2345000, "7772020"
            }
        };
        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getLocatedGenesResultSet(@SuppressWarnings("unused")
                                                     Connection connection) {
        String[] columnNames = new String[] {
            "feature_id"
        };
        Object[][] resObjects = new Object[][] {
            {
                3117509
            },
            {
                23269151
            }
        };

        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getAllelePropResultSet(Connection connection) throws SQLException {
        String[] columnNames = new String[] {
            "feature_id", "value", "type_name", "featureprop_id"
        };
        Object[][] resObjects = new Object[][] {
            {
                2340000,
                "@FBbt00004729:wing@, with @FBal0060667:Scer\\GAL4<up>sd-SG29.1</up>@",
                "derived_pheno_manifest",
                40000001
            },
            {
                2340000,
                "sensory mother cell & dorsal mesothoracic disc | ectopic, with @FBal0060667:Scer\\GAL4<up>sd-SG29.1</up>@",
                "derived_pheno_manifest",
                40000002
            },
            {
                2340000,
                "@FBbt00004729:wing@ | @FBcv0000031:anterior compartment@, with @FBal0060667:Scer\\GAL4<up>sd-SG29.1</up>@",
                "derived_pheno_manifest",
                40000003
            },
            {
                2340000,
                "@FBcv0000351:lethal@ | @FBdv00005289:embryonic stage@ | @FBcv0000323:segment polarity@ | @FBcv0000311:conditional ts@",
                "derived_pheno_manifest",
                40000004
            },
            {
                2340000,
                "@FBcv0000354:visible@, with @FBal0060667:Scer\\GAL4<up>sd-SG29.1</up>@",
                "derived_pheno_class",
                40000005
            }
        };

        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getAllelePropPubResultSet(Connection connection) throws SQLException {
        String[] columnNames = new String[] {
            "featureprop_id", "pub_db_identifier"
        };
        Object[][] resObjects = new Object[][] {
            {
                40000001, "1902784"
            },
            {
                40000001, "2226204"
            },
            {
                40000002, "1902784"
            }
        };

        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ChadoCV getFlyBaseMiscCV(Connection connection) throws SQLException {
        ChadoCV cv = new ChadoCV(FlyBaseProcessor.FLYBASE_MISCELLANEOUS_CV);
        ChadoCVTerm root = new ChadoCVTerm("origin of mutation");
        ChadoCVTerm child = new ChadoCVTerm("&agr; ray");
        child.getDirectParents().add(root);
        root.getDirectChildren().add(child);
        cv.addByChadoId(5000001, root);
        cv.addByChadoId(5000002, child);
        return cv;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ChadoCV getFlyBaseSequenceOntologyCV(Connection connection) throws SQLException {
        ChadoCV cv = new ChadoCV(FlyBaseProcessor.FLYBASE_SO_CV_NAME);
        ChadoCVTerm root = new ChadoCVTerm("chromosome_structure_variation");
        ChadoCVTerm child1 = new ChadoCVTerm("interchromosomal_transposition");
        ChadoCVTerm child2 = new ChadoCVTerm("chromosomal_translocation");
        child1.getDirectParents().add(root);
        root.getDirectChildren().add(child1);
        child2.getDirectParents().add(root);
        root.getDirectChildren().add(child2);
        cv.addByChadoId(33, root);
        cv.addByChadoId(64, child1);
        cv.addByChadoId(58, child2);
        return cv;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getAlleleCVTermsResultSet(Connection connection) throws SQLException {
        String[] columnNames = new String[] {
            "feature_id", "cvterm_id"
        };
        Object[][] resObjects = new Object[][] {
            {
                2345000, 5000002
            }
        };

        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createAllelesTempTable(Connection connection) throws SQLException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getDeletionLocationResultSet(Connection connection) throws SQLException {
        String[] columnNames = new String[] {
            "deletion_feature_id", "deletion_organism_id", "chromosome_name", "fmin", "fmax",
            "strand"
        };

        Object[][] resObjects = new Object[][] {
            {
                11488812, 1, "3L", "7423765", "7576637", "1"
            },
            {
                11488720, 1, "3R", "21629316", "21657677", "1"
            }

        };

        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getIndelResultSet(Connection connection) throws SQLException {
        String[] columnNames = new String[] {
            "deletion_feature_id", "insertion_feature_id", "breakpoint_type"
        };
        Object[][] resObjects = new Object[][] {
            {
                11488812, 11430370, "bk1"
            },
            {
                11488812, 11432358, "bk2"
            },
            {
                11488720, 11431518, "bk1"
            },
            {
                11488720, 11430215, "bk2"
            }
        };

        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getInsertionLocationsResultSet(Connection connection) {
        String[] columnNames = new String[] {
            "sub_id", "fmin", "fmax", "chr_feature_id"
        };
        Object[][] resObjects = new Object[][] {
            {
                11430215, 21657677, 21657677, 11494725
            },
            {
                11432358, 7576637, 7576637, 11494725
            },
            {
                11431518, 21629316, 21629316, 11494725
            }
        };

        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getChromosomeStructureVariationResultSet(Connection connection) {
        String[] columnNames = new String[] {
            "feature_id", "cvterm_id"
        };
        Object[][] resObjects = new Object[][] {

        };

        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getFeatureCVTermResultSet(Connection connection)
                    throws SQLException {
        String[] columnNames = new String[] {
            "feature_id", "cvterm_id", "cvterm_name", "cv_name"
        };
        Object[][] resObjects = new Object[][] {
            {
                11380181, 58, "chromosomal_translocation", "SO"
            },
            {
                11380181, 72, "chromosomal_inversion", "SO"
            },
            {
                11380181, 61021, "Xray", "FlyBasemiscellaneousCV"
            }

        };

        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResultSet getInteractionResultSet(Connection connection) throws SQLException {
        String[] columnNames = new String[] {
            "feature_id", "other_feature_id", "pub_title", "pubmed_id"
        };
        Object[][] resObjects = new Object[][] {
            {
                23269151, 3117509, "An paper about interactions", 8344257
            },
            {
                23269151, 3117509, "Another paper about interactions", 2345671
            }
        };

        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }
}
