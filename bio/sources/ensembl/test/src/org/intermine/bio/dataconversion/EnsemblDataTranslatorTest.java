package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 */

import java.util.*;
import java.io.*;


import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.metadata.Model;
//import org.intermine.modelproduction.xml.InterMineModelParser;
import org.intermine.bio.dataconversion.EnsemblDataTranslator;
import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.dataconversion.MockItemReader;
import org.intermine.dataconversion.MockItemWriter;

import org.apache.log4j.Logger;


public class EnsemblDataTranslatorTest extends DataTranslatorTestCase {

    protected static final Logger LOG=Logger.getLogger(EnsemblDataTranslatorTest.class);
    private String tgtNs = "http://www.flymine.org/model/genomic#";
    private ItemFactory ensemblItemFactory;
    private ItemFactory genomicItemFactory;
    private Properties ensemblProperties;

    public EnsemblDataTranslatorTest(String arg) throws Exception {
        super(arg, "osw.bio-fulldata-test");
        ensemblItemFactory = new ItemFactory(Model.getInstanceByName("ensembl"));
        genomicItemFactory = new ItemFactory(Model.getInstanceByName("genomic"));
        ensemblProperties = getEnsemblProperties();
    }

    public void setUp() throws Exception {
        super.setUp();
        //InterMineModelParser parser = new InterMineModelParser();
        srcModel = Model.getInstanceByName("ensembl");
    }

    public void testTranslate() throws Exception {
        Map itemMap = writeItems(getSrcItems());

        System.out.println("itemMap: " + itemMap);
        EnsemblDataTranslator translator = new EnsemblDataTranslator(
                new MockItemReader(itemMap), mapping, srcModel, getTargetModel(tgtNs), ensemblProperties, "AGP");

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        // uncomment to write out a new target items file
        //FileWriter writer = new FileWriter(new File("ensembl_tgt.xml"));
        //writer.write(FullRenderer.render(tgtIw.getItems()));
        //writer.close();

        System.out.println(printCompareItemSets(new HashSet(getExpectedItems()), tgtIw.getItems()));
        assertEquals(new HashSet(getExpectedItems()), tgtIw.getItems());
    }


    public void testSetOrganismDbId() throws Exception {
        String srcNs = "http://www.intermine.org/model/ensembl#";
        Item gene = createSrcItem(srcNs + "gene", "1_1", "");
        gene.addReference(new Reference("seq_region", "1_99"));
        gene.addAttribute(new Attribute("description", "Transcribed locus [Source:UniGene;Acc:Hs.429230]"));
        gene.addReference(new Reference("analysis", "1_100"));


        Item seq =  createSrcItem(srcNs + "seq_region", "1_99", "");
        seq.addReference(new Reference("coord_system", "30_1"));
        seq.addAttribute(new Attribute("name", "1"));
        seq.addAttribute(new Attribute("length", "24612"));

        Item coord = createSrcItem(srcNs + "coord_system", "30_1", "");
        coord.addAttribute(new Attribute("name", "chromosome"));

        Item transcript = createSrcItem(srcNs + "transcript", "2_1", "");
        transcript.addReference(new Reference("gene", "1_1"));
        Item stableId = createSrcItem(srcNs + "gene_stable_id", "3_1", "");
        stableId.addAttribute(new Attribute("stable_id", "ENSG00000193436"));
        stableId.addReference(new Reference("gene", "1_1"));

        Map itemMap = writeItems(new HashSet(Arrays.asList(new Object[] {gene, stableId, transcript, seq, coord})));
        EnsemblDataTranslator translator = new EnsemblDataTranslator(new
            MockItemReader(itemMap), mapping, srcModel, getTargetModel(tgtNs), ensemblProperties, "HS");


        Item exp1 = createTgtItem(tgtNs + "Gene", "1_1", "");
        exp1.addAttribute(new Attribute("organismDbId", "ENSG00000193436"));
        exp1.addAttribute(new Attribute("identifier", "ENSG00000193436"));
        exp1.addReference(new Reference("organism", "-1_1"));
        exp1.addReference(new Reference("comment", "-1_12"));
        exp1.addCollection(new ReferenceList("evidence", new ArrayList(Arrays.asList(new Object[]{"-1_16", "-1_11"}))));
        exp1.addCollection(new ReferenceList("objects", new ArrayList(Collections.singleton("-1_13"))));

        Item exp2 = createTgtItem(tgtNs + "Location", "-1_13", "");
        exp2.addAttribute(new Attribute("endIsPartial", "false"));
        exp2.addAttribute(new Attribute("startIsPartial", "false"));
        exp2.addReference(new Reference("object", "-1_14"));
        exp2.addReference(new Reference("subject", "1_1"));

        Item exp3 = createTgtItem(tgtNs + "ComputationalResult", "-1_16", "");
        exp3.addReference(new Reference("source", "-1_11"));
        exp3.addReference(new Reference("analysis", "1_100"));

        Item exp4 = createTgtItem(tgtNs + "Comment", "-1_12", "");
        exp4.addAttribute(new Attribute("text", "Transcribed locus [Source:UniGene;Acc:Hs.429230]"));

        Set expected = new HashSet(Arrays.asList(new Object[] {exp1, exp2, exp3, exp4}));

        assertEquals(expected, translator.translateItem(gene));
    }

    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("EnsemblDataTranslatorFunctionalTest_tgt.xml"));
    }

    protected Collection getSrcItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("EnsemblDataTranslatorFunctionalTest_src.xml"));
    }

    protected String getModelName() {
        return "genomic";
    }

    protected String getSrcModelName() {
        return "ensembl";
    }

    private Item createSrcItem(String clsName, String identifier, String imps) {
        Item item = ensemblItemFactory.makeItem(identifier, clsName, imps);
        item.setClassName(clsName);
        item.setIdentifier(identifier);
        item.setImplementations(imps);
        return item;
    }

    private Item createTgtItem(String clsName, String identifier, String imps) {
        Item item = genomicItemFactory.makeItem(identifier, clsName, imps);
        item.setClassName(clsName);
        item.setIdentifier(identifier);
        item.setImplementations(imps);
        return item;
    }

    private Properties getEnsemblProperties() throws IOException {
        Properties ensemblProps = new Properties();
        InputStream epis = getClass().getClassLoader().getResourceAsStream("ensembl_config.properties");
        ensemblProps.load(epis);
        return ensemblProps;
    }
}

