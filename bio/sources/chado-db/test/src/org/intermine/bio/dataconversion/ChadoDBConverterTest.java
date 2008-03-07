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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.sql.Database;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;

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

    public void testProcessTaxon() throws Exception {
        String orgId = "7227";
        doTestProcess(orgId);
    }

    public void testProcessAbbreviation() throws Exception {
        String orgId = "Dmel";
        doTestProcess(orgId);
    }

    /**
     * @param orgId
     * @throws Exception
     * @throws IOException
     */
    private void doTestProcess(String orgId) throws Exception, IOException {
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        ChadoDBConverter converter =
            new TestChadoDBConverter(null, Model.getInstanceByName("genomic"), itemWriter);
        converter.setDataSetTitle("test title");
        converter.setDataSourceName("FlyBase");
        converter.setOrganisms(orgId);
        converter.setProcessors("org.intermine.bio.dataconversion.TestFlyBaseModuleProcessor");
        converter.process();
        itemWriter.close();
        FileWriter fw = new FileWriter("/tmp/chado-db-test-items.xml");
        PrintWriter pw = new PrintWriter(fw);
        pw.println("<items>");
        for (Object item: itemWriter.getItems()) {
            pw.println(item);
        }
        pw.println("</items>");
        pw.close();
        fw.close();
        assertEquals(readItemSet("ChadoDBConverterTest.xml"), itemWriter.getItems());
    }

    public void testGetFeatures() throws Exception {

        final List<String> minimalSet = Arrays.asList("gene", "exon");

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        ChadoDBConverter converter =
            new ChadoDBConverter(null, Model.getInstanceByName("genomic"), itemWriter);
        FlyBaseModuleProcessor processor = new TestFlyBaseModuleProcessor(converter);
        List<String> actualSet = processor.getFeatures();
        assertTrue(actualSet.containsAll(minimalSet));
    }

    private class TestChadoDBConverter extends ChadoDBConverter {
        public TestChadoDBConverter(Database database, Model tgtModel, ItemWriter writer) {
            super(database, tgtModel, writer);
        }

        protected Map<String, Integer> getChadoOrganismIds(@SuppressWarnings("unused")
                                                           Connection connection,
                                                           @SuppressWarnings("unused")
                                                           List<String> abbreviations) {
            Map<String, Integer> retMap = new HashMap<String, Integer>();
            retMap.put("Dmel", 7227);
            return retMap;
        }
    }
}
