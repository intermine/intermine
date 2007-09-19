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
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.sql.Database;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;

import com.mockobjects.sql.MockMultiRowResultSet;

public class AnoESTConverterTest extends ItemsTestCase
{
    public AnoESTConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        AnoESTConverter converter =
            new TestAnoESTConverter(null, Model.getInstanceByName("genomic"), itemWriter);
        converter.process();
        itemWriter.close();
        assertEquals(readItemSet("AnoESTConverterTest.xml"), itemWriter.getItems());
    }

    private class TestAnoESTConverter extends AnoESTConverter
    {
        public TestAnoESTConverter(Database database, Model tgtModel, ItemWriter writer) 
        throws ObjectStoreException {
            super(database, tgtModel, writer);
        }
        protected ResultSet getClusterResultSet(@SuppressWarnings("unused") Connection connection) {
            Object[][] resObjects = new Object[][] { 
                {
                    "NCLAG150001", "2L", 2833, 12833, -1
                },
                {
                    "NCLAG150002", "2L", 552, 2211, 1
                },
                {
                    "NCLAG169971", "mitochondrial", 100, 8552, -1
                },
                {
                    "UCLAG189057", null, 0, 0, 0
                }
            };
            MockMultiRowResultSet res = new MockMultiRowResultSet();
            res.setupRows(resObjects);
            return res;
        }

        protected ResultSet getEstResultSet(Connection connection) {
            Object[][] resObjects = new Object[][] {
                {"BM654701.1","NCLAG150001", "1234567"},
                {"BM654701.1","NCLAG150002", "1234567"},
                {"BX615094.1","NCLAG150003", "1234567"},
                {"AF043434.1","NCLAG169971", "1234567"},
                {"AF043436.1","NCLAG169971", "1234567"},
                {"AF043437.1","NCLAG169971", "1234567"},
                {"AF043439.1","NCLAG169971", "1234567"},
                {"AF043443.1","NCLAG169971", "1234567"},
                {"AA413316.1","UCLAG189057", "1234567"},
                {"BX040896.1","UCLAG189057", "1234567"},
                {"BX040897.1","UCLAG189057", "1234567"},
                {"BX610489.1","UCLAG189057", "1234567"},
                {"BX610490.1","UCLAG189057", "1234567"}
            };
            MockMultiRowResultSet res = new MockMultiRowResultSet();
            res.setupRows(resObjects);
            return res;
        }
    }

}
