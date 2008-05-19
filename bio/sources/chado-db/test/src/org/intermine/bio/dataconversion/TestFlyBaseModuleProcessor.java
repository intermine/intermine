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

import com.mockobjects.sql.MockMultiRowResultSet;

/**
 * An implementation of FlyBaseModuleProcessor for testing.
 * @author Kim Rutherford
 */
public class TestFlyBaseModuleProcessor extends FlyBaseModuleProcessor
{
    /**
     * Create a new TestFlyBaseModuleProcessor object.
     * @param chadoDBConverter the ChadoDBConverter
     */
    public TestFlyBaseModuleProcessor(ChadoDBConverter chadoDBConverter) {
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
    protected ResultSet getFeatureResultSet(@SuppressWarnings("unused") Connection connection) {
        String[] columnNames = new String[] {
            "feature_id", "name", "uniquename", "type", "seqlen", "residues", "organism_id"
        };
        Object[][] resObjects = new Object[][] {
            {
                23269151, "4.5SRNA", "FBgn0000001", "gene", 1001,
                "acgacagatcattccacttttgacagctcactcggcagtaccagaaaatcc", 7227
            },
            {
                10012345, "CG12345", "FBgn012345", "gene", 200,
                "atatagctagctaggaggattattatta", 7227
            },
            {
                3117509, "CG10006", "FBgn0036461", "gene", 5023,
                "gtcatgcactactatccagttcaccaggctaaagtcggctcctat", 7227
            },
            {
                411, "CG10000-RA", "FBtr0085315", "mRNA", 2528,
                "tctgcctcccaactacaatcagatgaactccaaccccaccac", 7227
            },
            {
                412, "CG10000-RB", "FBtr0085316", "mRNA", 3000,
                "cccgcagcatgtgcatcagcagcatgtgtcatcggacgag", 7227
            },
            {
                11494725, "3", "3L", "chromosome_arm", 24543557,
                "ccacgactcgcagagggtgaagcttaagcgatcacggac"
                + "tcaatcctggcatcccagttcccaaagtcaggtccccacgagcatg", 7227
            },
            {
                11494726, "3", "3R", "chromosome_arm", 27905053,
                "ggtggaacttgagaacgagttcaagagcaacatgtact"
                + "gatgaatctgtcgtggggcgagcctgctgccaagtcgagaaagctgag", 7227
            },
            {
                3175412, "CG10000:1", "CG10000:1", "exon", 148,
                "gattgctcagcgcgaacg", 7227
            },
            {
                3175413, "CG10000:2", "CG10000:2", "exon", 161,
                "cttgtccctgtg", 7227
            },
            {
                1000000, "CG10000-RA_prot", "FBpp10000", "protein", 2345,
                "ccaggtg", 7227
            },
            {
                // this is actually an allele
                2340000, "CG10006[GD2461]", "FBal0198867", "gene", null,
                null, 7227
            },
            {
                // this is actually an allele
                2345000, "Scer\\GAL4[sd-SG29.1]", "FBal0060667", "gene", null,
                null, 7227
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
    protected ResultSet getFeatureRelationshipResultSet(@SuppressWarnings("unused")
                                                        Connection connection) {
        String[] columnNames = new String[] {
            "feature_relationship_id", "subject_id", "object_id", "type_name"
        };
        // results must be ordered by subject_id
        Object[][] resObjects = new Object[][] {
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
                800 ,    2340000 ,   3117509 , "alleleof"
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
                3117509,
                "@FBbt00004729:wing@, with @FBal0060667:Scer\\GAL4<up>sd-SG29.1</up>@",
                "derived_pheno_manifest",
                40000001
            },
            {
                3117509,
                "sensory mother cell & dorsal mesothoracic disc | ectopic, with @FBal0060667:Scer\\GAL4<up>sd-SG29.1</up>@",
                "derived_pheno_manifest",
                40000002
            },
            {
                3117509,
                "@FBbt00004729:wing@ | @FBcv0000031:anterior compartment@, with @FBal0060667:Scer\\GAL4<up>sd-SG29.1</up>@",
                "derived_pheno_manifest",
                40000003
            },
            {
                3117509,
                "@FBcv0000351:lethal@ | @FBdv00005289:embryonic stage@ | @FBcv0000323:segment polarity@ | @FBcv0000311:conditional ts@",
                "derived_pheno_manifest",
                40000004
            },
            {
                3117509,
                "@FBcv0000354:visible@, with @FBal0060667:Scer\\GAL4<up>sd-SG29.1</up>@",
                "derived_pheno_class",
                40000005
            },
//            {
//                3117509,
//                "@FBcv0000289:hypomorph@",
//                "promoted_allele_class",
//                40000006
//            }
        };

        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    /* (non-Javadoc)
     * @see org.intermine.bio.dataconversion.FlyBaseModuleProcessor#getAllelePropPubResultSet(java.sql.Connection)
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

}
