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
            new TestAnoESTConverter(null, Model.getInstanceByName("genomic"), itemWriter);
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

    private class TestAnoESTConverter extends ChadoDBConverter
    {
        public TestAnoESTConverter(Database database, Model tgtModel, ItemWriter writer) {
            super(database, tgtModel, writer);
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        protected ResultSet getFeatureResultSet(@SuppressWarnings("unused") Connection connection) {
            String[] columnNames = new String[] {
                "feature_id", "name", "uniquename", "type", "residues", "seqlen"
            };
            Object[][] resObjects = new Object[][] { 
                {
                    23269151, "4.5SRNA", "FBgn0000001", "gene", null, null
                 },
                 {
                     3117509, "CG10006", "FBgn0036461", "gene", null, 5023
                 },
                 {
                     3158204, "CG10005", "FBgn0037972", "gene", null, 2036
                 },
                 {
                     3175410, "CG10000", "FBgn0039596", "gene", null, 3209
                 },
                 {
                     3175412, "CG10000:1", "CG10000:1", "exon", null, 148
                 },
                 {
                     3175413, "CG10000:2", "CG10000:2", "exon", null, 161
                 },
                 {
                     3175414, "CG10000:3", "CG10000:3", "exon", null, 179
                 },
                 {
                     3175415, "CG10000:4", "CG10000:4", "exon", null, 464
                 },
                 {
                     3175416, "CG10000:5", "CG10000:5", "exon", null, 170
                 }
            };
            MockMultiRowResultSet res = new MockMultiRowResultSet();
            res.setupRows(resObjects);
            res.setupColumnNames(columnNames);
            return res;
        }
        
        protected ResultSet getSynonymResultSet(Connection connection) throws SQLException {
            String[] columnNames = new String[] {
                "feature_id", "accession"
            };
            Object[][] resObjects = new Object[][] { 
                {
                    23269151, "FBgn0000001_sym1"
                },
                {
                    23269151, "FBgn0000001_sym2"
                },
                {
                    3117509, "FBgn0036461_sym1"
                },
            };
            MockMultiRowResultSet res = new MockMultiRowResultSet();
            res.setupRows(resObjects);
            res.setupColumnNames(columnNames);
            return res;
        }
    }
}
