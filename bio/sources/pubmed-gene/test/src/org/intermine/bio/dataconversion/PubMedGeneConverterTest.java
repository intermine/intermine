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

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
        MockIdResolverFactory resolverFactory = new MockIdResolverFactory("Gene");
        resolverFactory.addResolverEntry("7227", "FBgn003", Collections.singleton("1234"));
        resolverFactory.addResolverEntry("7227", "FBgn002", Collections.singleton("2222"));
        resolverFactory.addResolverEntry("7227", "FBgn001", Collections.singleton("1111"));
        converter.resolverFactory = resolverFactory;
        super.setUp();
    }

    /**
     * Basic test of converter functionality.
     * @throws Exception
     */
    public void testSimpleFiles() throws Exception {
        process("gene2pubmed", "gene_info");

        // 4 organisms, 9 genes, 16 publications, 1 dataset, 1 datasource, 1 so term, 1 ontology
        assertEquals(48, itemWriter.getItems().size());
        // uncomment to write out a new target items file
        // Set<org.intermine.xml.full.Item> expected = readItemSet("PubMedGeneConverterTest_tgt.xml");
//        checkGene("4126706", "WBGene308375", "34", new String[]{"16689796", "17573816", "17581122", "17590236"}, new String[]{DATASET}); // type "other", do not create a gene
        checkGene("171593", "WBGene00022279", "6239", new String[]{"1"}, new String[]{DATASET});
        checkGene("171594", "WBGene00021677", "6239", new String[]{"2"}, new String[]{DATASET});
        checkGene("171595", "WBGene00021678", "6239", new String[]{"3"}, new String[]{DATASET});
        checkGene("171597", "WBGene00021679", "6239", new String[]{"4"}, new String[]{DATASET});
        checkGene("1234", "FBgn003", "7227", new String[]{"4"}, new String[]{DATASET});
        checkGene("2222", "FBgn002", "7227", new String[]{"2", "3"}, new String[]{DATASET});
        checkGene("1111", "FBgn001", "7227", new String[]{"1"}, new String[]{DATASET});
        checkGene("1111", "FBgn001", "7227", new String[]{"1"}, new String[]{DATASET});
        checkGene("944880", "EG30024", "83333", new String[]{"2184240", "6173374"}, new String[]{DATASET});
        checkNcRNA("944909", "EG30027", "83333", new String[]{"10801497", "10834842"}, new String[]{DATASET});
    }

    /**
     * Test that redundant database prefixes of primary identifiers are removed.
     */
    public void testPrefixRemoved() throws Exception {
        process("gene2pubmed", "gene_infoPrefixRemoved");
//        checkGene("4126706", "WbGene308375", "34", new String[]{"16689796", "17573816", "17581122", "17590236"}, new String[]{DATASET});
        checkGene("171593", "WbGene00022279", "6239", new String[]{"1"}, new String[]{DATASET});
        checkGene("171594", "WbGene00021677", "6239", new String[]{"2"}, new String[]{DATASET});
        checkGene("944880", "EG30024", "83333", new String[]{"2184240", "6173374"}, new String[]{DATASET});
    }

    /**
     * Test case when there is gene in gene information file without
     * referenced publications. This gene should be skipped.
     * @throws Exception
     */
    public void testGeneWithoutPublications() throws Exception {
        process("gene2pubmedWithoutPublications", "gene_info");
        // only one gene, others don't  have publications
        assertEquals(1, getGenes().size());
    }

    /**
     * Test case when primary identifier is invalid, for example
     * is  like WbGene308375|WbGene3083343 that denotes that
     * gene with specific ncbi id corresponds more WB genes but
     * for us it is invalid gene and is not processed
     */
    public void  testInvalidIdsRemoved() throws Exception {
        process("gene2pubmed", "gene_infoInvalidIdsRemoved");
        assertEquals(8, getGenes().size());
    }

    /**
     * Test case when there are two genes in gene information file with
     * different NCBI identifiers but same primary identifiers. None
     * of them should be read.
     * @throws Exception
     */
    public void testTwoPrimaryIdentifiers() throws Exception {
        process("gene2pubmedTwoPrimaryIdentifiers", "gene_infoTwoPrimaryIdentifiers");
        assertEquals(4, getGenes().size());
    }

    /**
     * Test case when there is a strain in the file.  Should be changed to be the taxonID of
     * main organism
     * @throws Exception
     */
    public void testStrain() throws Exception {
        process("gene2pubmed_strain", "gene_info_strain");
        System.out.println(getGenes().toString());
        assertEquals(6, getGenes().size());

        checkGene("3111", "FBgn3111", "4932", new String[]{"2"}, new String[]{DATASET});
        checkGene("3222", "FBgn3222", "4932", new String[]{"1"}, new String[]{DATASET});
        checkGene("2222", "FBgn2222", "7237", new String[]{"2"}, new String[]{DATASET});
        checkGene("2111", "FBgn2111", "7237", new String[]{"1"}, new String[]{DATASET});

    }

    /**
     * Test case when there is no information in gene information file
     * for gene that is mentioned in gene2pubmed file. Exception should be thrown.
     * @throws Exception
     */
    public void testGeneNoInformation() throws Exception {
        try {
            process("gene2pubmed", "gene_infoGeneNoInformation");
            fail("Exception should be thrown.");
        } catch (RuntimeException ex) {
            // ok
        }
    }

    /**
     * Test case when references file (gene2pubmed) is not
     * sorted by organism id. Exception should be thrown.
     * @throws Exception
     */
    public void testReferencesFileBadOrder() throws Exception {
        try {
            process("gene2pubmedBadOrder", "gene_info");
            fail("Exception should be thrown.");
        } catch (RuntimeException ex) {
            // ok
        }
    }

    /**
     * Test case when gene info file is not sorted by organism id. Exception should be thrown.
     * @throws Exception
     */
    public void testInfoFileBadOrder() throws Exception {
        try {
            process("gene2pubmed", "gene_infoBadOrder");
            fail("Exception should be thrown.");
        } catch (RuntimeException ex) {
            // ok
        }
    }

    private List<Item> getGenes() {
        List<Item> ret = new  ArrayList<Item>();
        for (Item item : storedItems) {
            if (item.getClassName().contains("Gene")) {
                ret.add(item);
            }
        }
        return ret;
    }

    private void process(String referencesFile, String infoFile) throws Exception {
        File gene2pubmed = new File(getClass().getClassLoader().getResource(referencesFile).toURI());

        File geneInfo = new File(getClass().getClassLoader().getResource(infoFile).toURI());
        converter.setInfoFile(geneInfo);
        converter.setCurrentFile(gene2pubmed);
        converter.setPubmedOrganisms("34 6239 7227 10090 7237 4932 9606 46245 559292 83333");
        converter.process(new FileReader(gene2pubmed));
        storedItems = itemWriter.getItems();
    }

    private void checkGene(String ncbiId, String primId, String orgId, String[] pubs, String[] datasets) {
        Item gene = getGene(ncbiId);
        assertEquals(primId, gene.getAttribute("primaryIdentifier").getValue());
        Item org = getItem(gene.getReference("organism").getRefId());
        assertEquals(orgId, org.getAttribute("taxonId").getValue());
        checkPublications(gene.getCollection("publications").getRefIds(), pubs);
        checkDataSet(gene.getCollection("dataSets").getRefIds(), datasets);
    }

    private void checkNcRNA(String ncbiId, String primId, String orgId, String[] pubs, String[] datasets) {
        Item ncRNA = getNcRNA(ncbiId);
        assertEquals(primId, ncRNA.getAttribute("primaryIdentifier").getValue());
        Item org = getItem(ncRNA.getReference("organism").getRefId());
        assertEquals(orgId, org.getAttribute("taxonId").getValue());
        checkPublications(ncRNA.getCollection("publications").getRefIds(), pubs);
        checkDataSet(ncRNA.getCollection("dataSets").getRefIds(), datasets);
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

    private Item getGene(String ncbiId) {
        return getItem("Gene", "ncbiGeneNumber", ncbiId);
    }

    private Item getNcRNA(String ncbiId) {
        return getItem("NcRNA", "ncbiGeneNumber", ncbiId);
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
