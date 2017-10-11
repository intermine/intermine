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

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

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
    private Set<Item> storedItems;
    private static final String DATASET = "PubMed to gene mapping";

    public PubMedGeneConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        itemWriter = new MockItemWriter(new HashMap<String, org.intermine.model.fulldata.Item>());
        converter = new PubMedGeneConverter(itemWriter, model);

        converter.rslv = IdResolverService.getMockIdResolver("Gene");
        converter.rslv.addResolverEntry("7227", "FBgn003", Collections.singleton("1234"));
        converter.rslv.addResolverEntry("7227", "FBgn002", Collections.singleton("2222"));
        converter.rslv.addResolverEntry("7227", "FBgn001", new HashSet<String>(Arrays.asList("1111", "1112", "Steve Jobs")));
        converter.rslv.addResolverEntry("6239", "WBGene00022279", Collections.singleton("171593"));
        converter.rslv.addResolverEntry("6239", "WBGene00021677", Collections.singleton("171594"));
        converter.rslv.addResolverEntry("6239", "WBGene00021678", Collections.singleton("171595"));
        converter.rslv.addResolverEntry("6239", "WBGene00021679", Collections.singleton("171597"));
        converter.rslv.addResolverEntry("83333", "EG30024", Collections.singleton("944880"));
        converter.rslv.addResolverEntry("83333", "EG30027", Collections.singleton("944909"));

        super.setUp();
    }

    /**
     * Basic test of converter functionality.
     * @throws Exception
     */
    public void testSimpleFiles() throws Exception {
        process("gene2pubmed");
        // writeItemsFile(itemWriter.getItems(), "pubmed-tgt-items.xml");

        assertEquals(46, itemWriter.getItems().size());

        // uncomment to write out a new target items file
        // Set<org.intermine.xml.full.Item> expected = readItemSet("PubMedGeneConverterTest_tgt.xml");
//        checkGene("4126706", "WBGene308375", "34", new String[]{"16689796", "17573816", "17581122", "17590236"}, new String[]{DATASET}); // type "other", do not create a gene
        checkGene("WBGene00022279", "6239", new String[]{"1"}, new String[]{DATASET});
        checkGene("WBGene00021677", "6239", new String[]{"2"}, new String[]{DATASET});
        checkGene("WBGene00021678", "6239", new String[]{"3"}, new String[]{DATASET});
        checkGene("WBGene00021679", "6239", new String[]{"4"}, new String[]{DATASET});
        checkGene("FBgn003", "7227", new String[]{"4"}, new String[]{DATASET});
        checkGene("FBgn002", "7227", new String[]{"2", "3"}, new String[]{DATASET});
        checkGene("FBgn001", "7227", new String[]{"1"}, new String[]{DATASET});
        checkGene("EG30024", "83333", new String[]{"2184240", "6173374"}, new String[]{DATASET});
        checkGene("EG30027", "83333", new String[]{"10801497", "10834842"}, new String[]{DATASET});
    }

    /**
     * Test case when there are two genes in gene information file with
     * different NCBI identifiers but same primary identifiers. None
     * of them should be read.
     * @throws Exception
     */
    public void testTwoPrimaryIdentifiers() throws Exception {
        process("gene2pubmedTwoPrimaryIdentifiers");
        // FBgn001 which has two NCBI id 1111 and 1112 will be thrown out
        assertEquals(8, getGenes().size());
    }

    /**
     * Test case when there is a strain in the file.  Should be changed to be the taxonID of
     * main organism
     * @throws Exception
     */
    public void testStrain() throws Exception {
        process("gene2pubmed_strain");
        // System.out.println(getGenes().toString());
        assertEquals(6, getGenes().size());

        checkGene("3111", "4932", new String[]{"2"}, new String[]{DATASET});
        checkGene("3222", "4932", new String[]{"1"}, new String[]{DATASET});
        checkGene("2222", "7237", new String[]{"2"}, new String[]{DATASET});
        checkGene("2111", "7237", new String[]{"1"}, new String[]{DATASET});

    }

    /**
     * Test case when references file (gene2pubmed) is not
     * sorted by organism id. Exception should be thrown.
     * @throws Exception
     */
//    public void testReferencesFileBadOrder() throws Exception {
//        try {
//            process("gene2pubmedBadOrder");
//            fail("Exception should be thrown.");
//        } catch (RuntimeException ex) {
//            // ok
//        }
//    }

    private List<Item> getGenes() {
        List<Item> ret = new  ArrayList<Item>();
        for (Item item : storedItems) {
            if (item.getClassName().contains("Gene")) {
                ret.add(item);
            }
        }
        return ret;
    }

    private void process(String referencesFile) throws Exception {
        File gene2pubmed = new File(getClass().getClassLoader().getResource(referencesFile).toURI());

        converter.setCurrentFile(gene2pubmed);
        converter.setPubmedOrganisms("34 6239 7227 10090 7237 4932 9606 46245 559292 83333");
        converter.process(new FileReader(gene2pubmed));
        storedItems = itemWriter.getItems();
//        writeItemsFile(itemWriter.getItems(), "pubmed-tgt-items.xml");
    }

    private void checkGene(String primId, String orgId, String[] pubs, String[] datasets) {
        Item gene = getGene(primId);
        assertEquals(primId, gene.getAttribute("primaryIdentifier").getValue());
        Item org = getItem(gene.getReference("organism").getRefId());
        assertEquals(orgId, org.getAttribute("taxonId").getValue());
        checkPublications(gene.getCollection("publications").getRefIds(), pubs);
        checkDataSet(gene.getCollection("dataSets").getRefIds(), datasets);
    }

    private void checkDataSet(List<String> refIds, String[] datasets) {
        assertEquals(refIds.size(), datasets.length);
        for (String title : datasets) {
            boolean found = false;
            for (String refId : refIds) {
                Item item = getItem(refId);
                if (item.getAttribute("name").getValue().equals(title)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fail("dataset with id " + title + " not found.");
            }
        }
    }

    private void checkPublications(List<String> refIds, String[] pubsIds) {
        assertEquals(refIds.size(), pubsIds.length);
        for (String pubId : pubsIds) {
            boolean found = false;
            for (String refId : refIds) {
                Item item = getItem(refId);
                if (item.getAttribute("pubMedId").getValue().equals(pubId)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fail("publication with id " + pubId + " not found.");
            }
        }
    }

    private Item getItem(String refId) {
        for (Item item : storedItems) {
            if (item.getIdentifier().equals(refId)) {
                return item;
            }
        }
        return null;
    }

    private Item getGene(String primId) {
        return getItem("Gene", "primaryIdentifier", primId);
    }

    private Item getItem(String className, String attribute, String attValue) {
        for (Item item : storedItems) {
            if (item.getClassName().contains(className))
                if (item.getAttribute(attribute) != null)
                    if (item.getAttribute(attribute).getValue().equals(attValue)) {
                return item;
            }
        }
        return null;
    }

}
