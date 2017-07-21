package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
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

import org.intermine.bio.util.OrganismData;
import org.intermine.bio.util.OrganismRepository;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.sql.Database;
import org.intermine.xml.full.Item;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

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

    private void doTestProcess(String orgId) throws Exception, IOException {
        MockItemWriter itemWriter =
            new MockItemWriter(new HashMap<String, org.intermine.model.fulldata.Item>());
        ChadoDBConverter converter =
            new TestChadoDBConverter(null, Model.getInstanceByName("genomic"), itemWriter);
        converter.setOrganisms(orgId);
        converter.setProcessors("org.intermine.bio.dataconversion.TestFlyBaseProcessor");
        converter.setDataSourceName("FlyBase");
        converter.process();
        itemWriter.close();
        //writeItemsFile(itemWriter.getItems(), "chado-db-test-items-" + orgId + ".xml");
        assertEquals(readItemSet("ChadoDBConverterTest.xml"), itemWriter.getItems());
    }

    public void testGetFeatures() throws Exception {

        final List<String> minimalSet = Arrays.asList("gene", "exon");

        MockItemWriter itemWriter =
            new MockItemWriter(new HashMap<String, org.intermine.model.fulldata.Item>());
        ChadoDBConverter converter =
            new TestChadoDBConverter(null, Model.getInstanceByName("genomic"), itemWriter);
        FlyBaseProcessor processor = new TestFlyBaseProcessor(converter);
        List<String> actualSet = processor.getFeatures();
        assertTrue(actualSet.containsAll(minimalSet));
    }

    public void testFlyBaseChromosomes() throws Exception {

        MockItemWriter itemWriter =
            new MockItemWriter(new HashMap<String, org.intermine.model.fulldata.Item>());
        ChadoDBConverter converter =
            new TestChadoDBConverter(null, Model.getInstanceByName("genomic"), itemWriter);
        FlyBaseProcessor processor = new TestFlyBaseProcessor(converter);

        // if not Dmel genomic_path_regions without '_' should become chromosomes
        Item item = processor.makeFeature(null, "golden_path_region", "DummyType", "3R", "3R", 0,
                                          7237);
        assertTrue(item.getClassName().endsWith("Chromosome"));

        // If an underscore in name and not dmel or dpse, should be a GoldenPathFragment
        item = processor.makeFeature(null, "golden_path_region", "DummyType", "scaffold_10",
                                     "scaffold_10", 0, 7777);
        assertTrue(item.getClassName().endsWith("GoldenPathFragment"));
    }

    private class TestChadoDBConverter extends GenomeDBConverter {
        public TestChadoDBConverter(Database database, Model tgtModel, ItemWriter writer)
            throws SQLException {
            super(database, tgtModel, writer);
        }

        @Override
        protected Map<OrganismData, Integer> getChadoOrganismIds(@SuppressWarnings("unused")
                                                                    Connection connection) {
            Map<OrganismData, Integer> retMap = new HashMap<OrganismData, Integer>();
            retMap.put(OrganismRepository.getOrganismRepository().getOrganismDataByTaxon(7227), 1);
            return retMap;
        }
    }
}
