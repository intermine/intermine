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

import java.util.HashMap;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.sql.Database;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.mockobjects.sql.MockMultiRowResultSet;

public class ChadoDBConverterTest extends ItemsTestCase
{
    public ChadoDBConverterTest(String arg) {
        super(arg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        ChadoDBConverter converter =
            new TestChadoDBConverter(null, Model.getInstanceByName("genomic"), itemWriter);
        converter.setDataSetTitle("test title");
        converter.setDataSourceName("test name");
        converter.setGenus("Drosophila");
        converter.setSpecies("melanogaster");
        converter.setTaxonId("7227");
        converter.process();
        itemWriter.close();
        FileWriter fw = new FileWriter("/tmp/item_out");
        PrintWriter pw = new PrintWriter(fw);
        pw.println(itemWriter.getItems());
        pw.close();
        fw.close();
        assertEquals(readItemSet("ChadoDBConverterTest.xml"), itemWriter.getItems());
    }

    private class TestChadoDBConverter extends ChadoDBConverter
    {
        /* (non-Javadoc)
         * @see org.intermine.bio.dataconversion.ChadoDBConverter#getPubResultSet(java.sql.Connection)
         */
        @Override
        protected ResultSet getPubResultSet(Connection connection) throws SQLException {
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
                    3175412, "1988156"
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

        @Override
        protected int getChadoOrganismId(@SuppressWarnings("unused") Connection connection) {
            return 1;
        }

        public TestChadoDBConverter(Database database, Model tgtModel, ItemWriter writer) {
            super(database, tgtModel, writer);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected ResultSet getFeatureResultSet(@SuppressWarnings("unused") Connection connection) {
            String[] columnNames = new String[] {
                "feature_id", "name", "uniquename", "type", "seqlen"
            };
            Object[][] resObjects = new Object[][] {
                {
                    23269151, "4.5SRNA", "FBgn0000001", "gene", null
                 },
                 {
                     3117509, "CG10006", "FBgn0036461", "gene", 5023
                 },
                 {
                     3175411, "CG10000-RA", "FBtr0085315", "mRNA", 2528
                 },
                 {
                     11494726, "3", "3R", "chromosome_arm", 27905053
                 },
                 {
                     3175412, "CG10000:1", "CG10000:1", "exon", 148
                 },
                 {
                     3175413, "CG10000:2", "CG10000:2", "exon", 161
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
        protected ResultSet getFeatureRelationshipResultSet(@SuppressWarnings("unused")
                                                                Connection connection) {
            String[] columnNames = new String[] {
                "feature_relationship_id", "subject_id", "object_id", "type_name"
            };
            Object[][] resObjects = new Object[][] {
                {
                    1753955 ,    3175411 ,   3175410 , "partof"
                },
                {
                    1753948 ,    3175412 ,   3175411 , "partof"
                },
                {
                    1753948 ,    3175413 ,   3175411 , "partof"
                },
            };
            MockMultiRowResultSet res = new MockMultiRowResultSet();
            res.setupRows(resObjects);
            res.setupColumnNames(columnNames);
            return res;
        }

        @Override
        protected ResultSet getDbxrefResultSet(@SuppressWarnings("unused")
                                               Connection connection) {
            String[] columnNames = new String[] {
                "feature_id", "accession", "db_name"
            };
            Object[][] resObjects = new Object[][] {
                {
                    23269151, "FBgn0000001_dbxref1", "FlyBase Annotation IDs"
                },
                {
                    23269151, "FBgn0000001_dbxref2", "FlyBase"
                },
                {
                    3117509, "FBgn0036461_dbxref3", "FlyBase"
                },
            };
            MockMultiRowResultSet res = new MockMultiRowResultSet();
            res.setupRows(resObjects);
            res.setupColumnNames(columnNames);
            return res;
        }

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
                    23269151, "65A-65A; (determined by in situ hybridisation)", "derived_experimental_cyto"
                },
                {
                    23269151, "65A-65A; Left limit from in situ hybridisation (FBrf0042734) Right limit from in situ hybridisation (FBrf0042734)", "derived_computed_cyto"
                },
                {
                    23269151, "A function for the 4.5S RNA is still in the realm of speculation.", "misc"
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

        protected ResultSet getFeatureLocResultSet(Connection connection) throws SQLException {
            String[] columnNames = new String[] {
                "featureloc_id", "feature_id", "srcfeature_id", "fmin", "is_fmin_partial",
                "fmax", "is_fmax_partial", "strand"
            };
            MockMultiRowResultSet res = new MockMultiRowResultSet();
            // no test yet:
            res.setupRows(new Object[][] {});
            res.setupColumnNames(columnNames);
            return res;
        }

        protected ResultSet getSynonymResultSet(Connection connection) throws SQLException {
            String[] columnNames = new String[] {
                "feature_id", "synonym_name", "type_name", "is_current"
            };
            MockMultiRowResultSet res = new MockMultiRowResultSet();
            // no test yet:
            Object[][] resObjects = new Object[][] {
                {
                    23269151, "FBgn0000001_fullname_synonym", "fullname", true
                },
                {
                    23269151, "FBgn0000001_symbol_synonym", "symbol", true
                },
                {
                    3117509, "FBgn0036461_symbol_3", "other", false
                },
            };
            res.setupRows(resObjects);
            res.setupColumnNames(columnNames);
            return res;
        }
    }
}
