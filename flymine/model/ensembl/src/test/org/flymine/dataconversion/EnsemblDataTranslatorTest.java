package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;

import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.metadata.Model;
import org.intermine.dataconversion.DataTranslator;
import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.dataconversion.MockItemReader;
import org.intermine.dataconversion.MockItemWriter;

public class EnsemblDataTranslatorTest extends DataTranslatorTestCase {
    private String tgtNs = "http://www.flymine.org/model/genomic#";
    private ItemFactory ensemblItemFactory;
    private ItemFactory genomicItemFactory;

    public EnsemblDataTranslatorTest(String arg) throws Exception {
        super(arg);
        ensemblItemFactory = new ItemFactory(Model.getInstanceByName("ensembl"));
        genomicItemFactory = new ItemFactory(Model.getInstanceByName("genomic"));
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testTranslate() throws Exception {
        Map itemMap = writeItems(getSrcItems());
        DataTranslator translator =
            new EnsemblDataTranslator(new MockItemReader(itemMap),
                                      mapping, srcModel,
                                      getTargetModel(tgtNs), "WB");
        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

//         FileWriter fw = new FileWriter(new File("ensembl_tgt.xml"));
//         fw.write(tgtIw.getItems().toString());
//         fw.flush();
//         fw.close();

        String expectedNotActual = "in expected, not actual: " + compareItemSets(new HashSet(getExpectedItems()), tgtIw.getItems());
        String actualNotExpected = "in actual, not expected: " + compareItemSets(tgtIw.getItems(), new HashSet(getExpectedItems()));

        if (expectedNotActual.length() > 25) {
            System.out.println(expectedNotActual);
            System.out.println(actualNotExpected);
        }

        DataTranslatorTestCase.printCompareItemSets(new HashSet(getExpectedItems()), tgtIw.getItems());
        
        assertEquals(new HashSet(getExpectedItems()), tgtIw.getItems());
    }

    public void testSetGeneSynonyms() throws Exception {
        String srcNs = "http://www.flymine.org/model/ensembl#";

        // transcript - gene, transcript - translation
        Item gene = createSrcItem(srcNs + "gene", "1_1", "");
        Item transcript = createSrcItem(srcNs + "transcript", "1_2", "");
        Item translation = createSrcItem(srcNs + "translation", "1_3", "");
        transcript.addReference(new Reference("gene", "1_1"));
        transcript.addReference(new Reference("translation", "1_3"));

        // objectXref - translation, objectXref - xref(dbprimary_acc) - external_db(flybase_gene)
        Item objectXref = createSrcItem(srcNs + "object_xref", "1_4", "");
        Item xref = createSrcItem(srcNs + "xref", "1_5", "");
        Item externalDb = createSrcItem(srcNs + "external_db", "1_6", "");
        objectXref.addReference(new Reference("ensembl", "1_3"));
        objectXref.addReference(new Reference("xref", "1_5"));
        xref.addAttribute(new Attribute("dbprimary_acc", "FBgn1001"));
        xref.addReference(new Reference("external_db", "1_6"));
        externalDb.addAttribute(new Attribute("db_name", "flybase_gene"));

        Map itemMap = writeItems(new HashSet(Arrays.asList(new Object[] {gene, transcript, translation, objectXref, xref, externalDb})));
        EnsemblDataTranslator translator = new EnsemblDataTranslator(new MockItemReader(itemMap),
                                                                     mapping, srcModel,
                                                                     getTargetModel(tgtNs), "WB");

        Item exp1 = createTgtItem(tgtNs + "Synonym", "-1_6", "");
        exp1.addAttribute(new Attribute("value", "FBgn1001"));
        exp1.addAttribute(new Attribute("type", "identifier"));
        exp1.addReference(new Reference("source", "-1_7"));
        exp1.addReference(new Reference("subject", "1_1"));

        Set expected = new HashSet(Collections.singleton(exp1));
        Item tgtItem = createTgtItem(tgtNs + "Gene", "1_1", "");
        assertEquals(expected, translator.setGeneSynonyms(gene, tgtItem, srcNs));
    }

    // two genes with same flybase_gene (FBgn***) - expect one to get
    // _flymine_1 on the end of organismDbId
    public void testDuplicatedFlyBaseIds() throws Exception {
        String srcNs = "http://www.flymine.org/model/ensembl#";
        Item gene = createSrcItem(srcNs + "gene", "1_1", "");
        Item transcript = createSrcItem(srcNs + "transcript", "1_2", "");
        Item translation = createSrcItem(srcNs + "translation", "1_3", "");
        transcript.addReference(new Reference("gene", "1_1"));
        transcript.addReference(new Reference("translation", "1_3"));

        Item objectXref = createSrcItem(srcNs + "object_xref", "1_4", "");
        Item xref = createSrcItem(srcNs + "xref", "1_5", "");
        Item externalDb = createSrcItem(srcNs + "external_db", "1_6", "");
        objectXref.addReference(new Reference("ensembl", "1_3"));
        objectXref.addReference(new Reference("xref", "1_5"));
        xref.addAttribute(new Attribute("dbprimary_acc", "FBgn1001"));
        xref.addReference(new Reference("external_db", "1_6"));
        externalDb.addAttribute(new Attribute("db_name", "flybase_gene"));

        Item gene2 = createSrcItem(srcNs + "gene", "2_1", "");
        Item transcript2 = createSrcItem(srcNs + "transcript", "2_2", "");
        Item translation2 = createSrcItem(srcNs + "translation", "2_3", "");
        transcript2.addReference(new Reference("gene", "2_1"));
        transcript2.addReference(new Reference("translation", "2_3"));

        Item objectXref2 = createSrcItem(srcNs + "object_xref", "2_4", "");
        Item xref2 = createSrcItem(srcNs + "xref", "2_5", "");
        Item externalDb2 = createSrcItem(srcNs + "external_db", "2_6", "");
        objectXref2.addReference(new Reference("ensembl", "2_3"));
        objectXref2.addReference(new Reference("xref", "2_5"));
        xref2.addAttribute(new Attribute("dbprimary_acc", "FBgn1001"));
        xref2.addReference(new Reference("external_db", "2_6"));
        externalDb2.addAttribute(new Attribute("db_name", "flybase_gene"));

        Map itemMap = writeItems(new HashSet(Arrays.asList(new Object[] {gene, transcript, translation, objectXref, xref, externalDb, gene2, transcript2, translation2, objectXref2, xref2, externalDb2})));
        EnsemblDataTranslator translator = new EnsemblDataTranslator(new MockItemReader(itemMap),
                                                                     mapping, srcModel,
                                                                     getTargetModel(tgtNs), "WB");

        Item exp1 = createTgtItem(tgtNs + "Gene", "1_1", "");
        exp1.addAttribute(new Attribute("organismDbId", "FBgn1001"));
        ReferenceList rl1 = new ReferenceList("synonyms", new ArrayList(Collections.singleton("-1_6")));
        exp1.addCollection(rl1);
        Item tgtItem1 = createTgtItem(tgtNs + "Gene", "1_1", "");
        translator.setGeneSynonyms(gene, tgtItem1, srcNs);
        assertEquals(exp1, tgtItem1);

        Item exp2 = createTgtItem(tgtNs + "Gene", "2_1", "");
        exp2.addAttribute(new Attribute("organismDbId", "FBgn1001_flymine_1"));
        ReferenceList rl2 = new ReferenceList("synonyms", new ArrayList(Collections.singleton("-1_8")));
        exp2.addCollection(rl2);
        Item tgtItem2 = createTgtItem(tgtNs + "Gene", "2_1", "");
        translator.setGeneSynonyms(gene2, tgtItem2, srcNs);
        assertEquals(exp2, tgtItem2);
    }


    public void testSetOrganismDbId() throws Exception {
        String srcNs = "http://www.flymine.org/model/ensembl#";
        Item gene = createSrcItem(srcNs + "gene", "1_1", "");
        Item transcript = createSrcItem(srcNs + "transcript", "2_1", "");
        transcript.addReference(new Reference("gene", "1_1"));
        transcript.addReference(new Reference("translation", "4_1"));
        Item stableId = createSrcItem(srcNs + "gene_stable_id", "3_1", "");
        stableId.addAttribute(new Attribute("stable_id", "FBgn1001"));
        stableId.addReference(new Reference("gene", "1_1"));

        Map itemMap = writeItems(new HashSet(Arrays.asList(new Object[] {gene, stableId, transcript})));
        EnsemblDataTranslator translator = new EnsemblDataTranslator(new MockItemReader(itemMap),
                                                                     mapping, srcModel,
                                                                     getTargetModel(tgtNs), "WB");

        Item exp1 = createTgtItem(tgtNs + "Gene", "1_1", "");
        exp1.addAttribute(new Attribute("organismDbId", "FBgn1001"));
        exp1.addAttribute(new Attribute("identifier", "FBgn1001"));
        exp1.addReference(new Reference("organism", "-1_1"));
        exp1.addCollection(new ReferenceList("evidence", new ArrayList(Collections.singleton("-1_2"))));
        assertEquals(Collections.singleton(exp1), translator.translateItem(gene));
    }


    // TESTS NEED UPDATING DUE TO ADDITION OF TRANSLATION/CDS ITEMS
//     public void testMergeProteins() throws Exception {
//         String srcNs = "http://www.flymine.org/model/ensembl#";
//         Item transcript1 = createSrcItem(srcNs + "transcript", "1_1", "");
//         Item translation1 = createSrcItem(srcNs + "translation", "1_2", "");
//         transcript1.addReference(new Reference("translation", "1_2"));
//         Item stableId1 = createSrcItem(srcNs + "translation_stable_id", "1_6", "");
//         stableId1.addAttribute(new Attribute("stable_id", "TRANSLATION1"));
//         stableId1.addReference(new Reference("translation", "1_1"));

//         Item objectXref1 = createSrcItem(srcNs + "object_xref", "1_3", "");
//         Item xref1 = createSrcItem(srcNs + "xref", "1_4", "");
//         Item externalDb1 = createSrcItem(srcNs + "external_db", "1_5", "");
//         objectXref1.addReference(new Reference("ensembl", "1_2"));
//         objectXref1.addReference(new Reference("xref", "1_4"));
//         xref1.addAttribute(new Attribute("dbprimary_acc", "Q1001"));
//         xref1.addReference(new Reference("external_db", "1_5"));
//         externalDb1.addAttribute(new Attribute("db_name", "SWISSPROT"));


//         Item transcript2 = createSrcItem(srcNs + "transcript", "2_1", "");
//         Item translation2 = createSrcItem(srcNs + "translation", "2_2", "");
//         transcript2.addReference(new Reference("translation", "2_2"));
//         Item stableId2 = createSrcItem(srcNs + "translation_stable_id", "2_6", "");
//         stableId2.addAttribute(new Attribute("stable_id", "TRANSLATION2"));
//         stableId2.addReference(new Reference("translation", "2_2"));

//         Item objectXref2 = createSrcItem(srcNs + "object_xref", "2_3", "");
//         Item xref2 = createSrcItem(srcNs + "xref", "2_4", "");
//         Item externalDb2 = createSrcItem(srcNs + "external_db", "2_5", "");
//         objectXref2.addReference(new Reference("ensembl", "2_2"));
//         objectXref2.addReference(new Reference("xref", "2_4"));
//         xref2.addAttribute(new Attribute("dbprimary_acc", "Q1001"));
//         xref2.addReference(new Reference("external_db", "2_5"));
//         externalDb2.addAttribute(new Attribute("db_name", "SWISSPROT"));


//         Map itemMap = writeItems(new HashSet(Arrays.asList(new Object[] {transcript1, translation1, objectXref1, xref1, stableId1, externalDb1, transcript2, translation2, objectXref2, xref2, externalDb2, stableId2})));
//         EnsemblDataTranslator translator = new EnsemblDataTranslator(new MockItemReader(itemMap),
//                                                                      mapping, srcModel,
//                                                                      getTargetModel(tgtNs), "WB");


//         Item protein = createTgtItem(tgtNs + "Protein", "-1_7", "");
//         //protein.addAttribute(new Attribute("idenitifer", "Q1001"));
//         protein.addAttribute(new Attribute("primaryAccession", "Q1001"));
//         protein.addReference(new Reference("organism", "-1_1"));
//         protein.addCollection(new ReferenceList("synonyms", new ArrayList(Collections.singleton("-1_8"))));
//         protein.addCollection(new ReferenceList("evidence", new ArrayList(Collections.singleton("-1_2"))));
//         Item synonym0 = createTgtItem(tgtNs + "Synonym", "-1_8", "");
//         synonym0.addAttribute(new Attribute("type", "accession"));
//         synonym0.addAttribute(new Attribute("value", "Q1001"));
//         synonym0.addReference(new Reference("subject", "-1_7"));
//         synonym0.addReference(new Reference("source", "-1_5"));

//         Item trans1 = createTgtItem(tgtNs + "Transcript", "1_1", "");
//         trans1.addAttribute(new Attribute("identifier", "1_1"));
//         trans1.addReference(new Reference("protein", "-1_7"));
//         trans1.addReference(new Reference("organism", "-1_1"));
//         trans1.addCollection(new ReferenceList("objects", new ArrayList(Collections.singleton("-1_6"))));
//         trans1.addCollection(new ReferenceList("subjects", new ArrayList(Collections.singleton("-1_9"))));
//         trans1.addCollection(new ReferenceList("evidence", new ArrayList(Collections.singleton("-1_2"))));

//         Item trans2 = createTgtItem(tgtNs + "Transcript", "2_1", "");
//         trans2.addAttribute(new Attribute("identifier", "2_1"));
//         trans2.addReference(new Reference("protein", "-1_7"));
//         trans2.addCollection(new ReferenceList("objects", new ArrayList(Collections.singleton("-1_12"))));
//         trans2.addCollection(new ReferenceList("subjects", new ArrayList(Collections.singleton("-1_13"))));
//         trans2.addReference(new Reference("organism", "-1_1"));
//         trans2.addCollection(new ReferenceList("evidence", new ArrayList(Collections.singleton("-1_2"))));

//         Set expected = new HashSet(Arrays.asList(new Object[] {protein, trans1, trans2, synonym0}));


//         MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
//         translator.translate(tgtIw);
//         Set result = new HashSet();
//         Iterator resIter = tgtIw.getItems().iterator();
//         while (resIter.hasNext()) {
//             Item item = (Item) resIter.next();
//             if (!(item.getClassName().equals(tgtNs + "Database")|| item.getClassName().equals(tgtNs + "SimpleRelation") || item.getClassName().equals(tgtNs + "Organism"))) {
//                 result.add(item);
//             }
//         }
//         assertEquals(expected, result);
//     }

//     public void testMergeProteinEmblOnly() throws Exception {
//         String srcNs = "http://www.flymine.org/model/ensembl#";
//         Item transcript1 = createSrcItem(srcNs + "transcript", "1_1", "");
//         Item translation1 = createSrcItem(srcNs + "translation", "1_2", "");
//         transcript1.addReference(new Reference("translation", "1_2"));
//         Item stableId1 = createSrcItem(srcNs + "translation_stable_id", "1_6", "");
//         stableId1.addAttribute(new Attribute("stable_id", "TRANSLATION1"));
//         stableId1.addReference(new Reference("translation", "1_1"));

//         Item objectXref1 = createSrcItem(srcNs + "object_xref", "1_3", "");
//         Item xref1 = createSrcItem(srcNs + "xref", "1_4", "");
//         Item externalDb1 = createSrcItem(srcNs + "external_db", "1_5", "");
//         objectXref1.addReference(new Reference("ensembl", "1_2"));
//         objectXref1.addReference(new Reference("xref", "1_4"));
//         xref1.addAttribute(new Attribute("dbprimary_acc", "Q1001"));
//         xref1.addReference(new Reference("external_db", "1_5"));
//         externalDb1.addAttribute(new Attribute("db_name", "prediction_SPTREMBL"));


//         Map itemMap = writeItems(new HashSet(Arrays.asList(new Object[] {transcript1, translation1, objectXref1, xref1, stableId1, externalDb1})));
//         EnsemblDataTranslator translator = new EnsemblDataTranslator(new MockItemReader(itemMap),
//                                                                      mapping, srcModel,
//                                                                      getTargetModel(tgtNs), "WB");

//         Item trans1 = createTgtItem(tgtNs + "Transcript", "1_1", "");
//         trans1.addAttribute(new Attribute("identifier", "1_1"));
//         trans1.addReference(new Reference("organism", "-1_1"));
//         trans1.addCollection(new ReferenceList("objects", new ArrayList(Collections.singleton("-1_8"))));
//         trans1.addCollection(new ReferenceList("evidence", new ArrayList(Collections.singleton("-1_2"))));

//         Set expected = new HashSet(Arrays.asList(new Object[] {trans1}));


//         MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
//         translator.translate(tgtIw);
//         Set result = new HashSet();
//         Iterator resIter = tgtIw.getItems().iterator();
//         while (resIter.hasNext()) {
//             Item item = (Item) resIter.next();
//             if (!(item.getClassName().equals(tgtNs + "Database")|| item.getClassName().equals(tgtNs + "SimpleRelation") || item.getClassName().equals(tgtNs + "Organism"))) {
//                 result.add(item);
//             }
//         }
//         assertEquals(expected, result);
//     }


    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/EnsemblDataTranslatorFunctionalTest_tgt.xml"));
    }

    protected Collection getSrcItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/EnsemblDataTranslatorFunctionalTest_src.xml"));
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
}
