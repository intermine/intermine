package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

/**
 * Test of PubMedGeneConverter class. There are 2 small example files, that are converted
 * by this test: test_gene2pubmed and test_gene_info  
 * @author Jakub Kulaviak
 *
 */
public class PubMedGeneConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    PubMedGeneConverter converter;
    MockItemWriter itemWriter;
    Map<String, Item> storedItems = new HashMap<String, Item>();

    public PubMedGeneConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(storedItems);
        converter = new PubMedGeneConverter(itemWriter, model);
    }

    public void testProcess() throws Exception {
        File gene2pubmed = new File(getClass().getClassLoader().getResource(
                "test_gene2pubmed").toURI());
        File geneInfo = new File(getClass().getClassLoader().getResource(
                "test_gene_info").toURI());
        converter.setInfoFile(geneInfo);
        converter.setCurrentFile(gene2pubmed);
        Set<Integer> orgs = new HashSet<Integer>();
        orgs.add(34);
        orgs.add(6239);
        converter.setOrganismsToProcess(orgs);
        converter.process(new FileReader(gene2pubmed));

        // 2 organisms, 5 genes, 8 publications
        assertEquals(15, itemWriter.getItems().size());
        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "PubMedGeneConverterTest_tgt.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("PubMedGeneConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());
    }
}
