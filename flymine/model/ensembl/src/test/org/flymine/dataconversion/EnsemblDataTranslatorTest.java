package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.InputStreamReader;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.dataconversion.DataTranslator;
import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.dataconversion.MockItemReader;
import org.intermine.dataconversion.MockItemWriter;

public class EnsemblDataTranslatorTest extends DataTranslatorTestCase {
    private String tgtNs = "http://www.flymine.org/model/genomic#";

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testTranslate() throws Exception {
        Map itemMap = writeItems(getSrcItems());
        DataTranslator translator = new EnsemblDataTranslator(new MockItemReader(itemMap),
                                                              getOwlModel(), tgtNs, "WB");
        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        assertEquals(new HashSet(getExpectedItems()), tgtIw.getItems());
    }


    public void testSetGeneSynonyms() throws Exception {
        String srcNs = "http://www.flymine.org/model/ensembl#";
        Item gene = createItem(srcNs + "gene", "1_1", "");
        Item transcript = createItem(srcNs + "transcript", "1_2", "");
        Item translation = createItem(srcNs + "translation", "1_3", "");
        transcript.addReference(new Reference("gene", "1_1"));
        transcript.addReference(new Reference("translation", "1_3"));

        Item objectXref = createItem(srcNs + "object_xref", "1_4", "");
        Item xref = createItem(srcNs + "xref", "1_5", "");
        Item externalDb = createItem(srcNs + "external_db", "1_6", "");
        objectXref.addReference(new Reference("ensembl", "1_3"));
        objectXref.addReference(new Reference("xref", "1_5"));
        xref.addAttribute(new Attribute("dbprimary_acc", "FBgn1001"));
        xref.addReference(new Reference("external_db", "1_6"));
        externalDb.addAttribute(new Attribute("db_name", "flybase_gene"));

        Map itemMap = writeItems(new HashSet(Arrays.asList(new Object[] {gene, transcript, translation, objectXref, xref, externalDb})));
        EnsemblDataTranslator translator = new EnsemblDataTranslator(new MockItemReader(itemMap),
                                                                      getOwlModel(), tgtNs, "WB");

        Item exp1 = createItem(tgtNs + "Synonym", "-1_1", "");
        exp1.addAttribute(new Attribute("value", "FBgn1001"));
        exp1.addAttribute(new Attribute("type", "accession"));
        exp1.addReference(new Reference("source", "-1_2"));
        exp1.addReference(new Reference("subject", "1_1"));

        Set expected = new HashSet(Collections.singleton(exp1));
        Item tgtItem = createItem(tgtNs + "Gene", "1_1", "");
        assertEquals(expected, translator.setGeneSynonyms(gene, tgtItem, srcNs));
    }

    // two genes with same flybase_gene (FBgn***) - expect one to get
    // _flymine_1 on the end of organismDbId
    public void testDuplicatedFlyBaseIds() throws Exception {
        String srcNs = "http://www.flymine.org/model/ensembl#";
        Item gene = createItem(srcNs + "gene", "1_1", "");
        Item transcript = createItem(srcNs + "transcript", "1_2", "");
        Item translation = createItem(srcNs + "translation", "1_3", "");
        transcript.addReference(new Reference("gene", "1_1"));
        transcript.addReference(new Reference("translation", "1_3"));

        Item objectXref = createItem(srcNs + "object_xref", "1_4", "");
        Item xref = createItem(srcNs + "xref", "1_5", "");
        Item externalDb = createItem(srcNs + "external_db", "1_6", "");
        objectXref.addReference(new Reference("ensembl", "1_3"));
        objectXref.addReference(new Reference("xref", "1_5"));
        xref.addAttribute(new Attribute("dbprimary_acc", "FBgn1001"));
        xref.addReference(new Reference("external_db", "1_6"));
        externalDb.addAttribute(new Attribute("db_name", "flybase_gene"));

        Item gene2 = createItem(srcNs + "gene", "2_1", "");
        Item transcript2 = createItem(srcNs + "transcript", "2_2", "");
        Item translation2 = createItem(srcNs + "translation", "2_3", "");
        transcript2.addReference(new Reference("gene", "2_1"));
        transcript2.addReference(new Reference("translation", "2_3"));

        Item objectXref2 = createItem(srcNs + "object_xref", "2_4", "");
        Item xref2 = createItem(srcNs + "xref", "2_5", "");
        Item externalDb2 = createItem(srcNs + "external_db", "2_6", "");
        objectXref2.addReference(new Reference("ensembl", "2_3"));
        objectXref2.addReference(new Reference("xref", "2_5"));
        xref2.addAttribute(new Attribute("dbprimary_acc", "FBgn1001"));
        xref2.addReference(new Reference("external_db", "2_6"));
        externalDb2.addAttribute(new Attribute("db_name", "flybase_gene"));

        Map itemMap = writeItems(new HashSet(Arrays.asList(new Object[] {gene, transcript, translation, objectXref, xref, externalDb, gene2, transcript2, translation2, objectXref2, xref2, externalDb2})));
        EnsemblDataTranslator translator = new EnsemblDataTranslator(new MockItemReader(itemMap),
                                                                      getOwlModel(), tgtNs, "WB");


        Item exp1 = createItem(tgtNs + "Gene", "1_1", "");
        exp1.addAttribute(new Attribute("organismDbId", "FBgn1001"));
        ReferenceList rl1 = new ReferenceList("synonyms", new ArrayList(Collections.singleton("-1_1")));
        exp1.addCollection(rl1);
        Item tgtItem1 = createItem(tgtNs + "Gene", "1_1", "");
        translator.setGeneSynonyms(gene, tgtItem1, srcNs);
        assertEquals(exp1, tgtItem1);

        Item exp2 = createItem(tgtNs + "Gene", "2_1", "");
        exp2.addAttribute(new Attribute("organismDbId", "FBgn1001_flymine_1"));
        ReferenceList rl2 = new ReferenceList("synonyms", new ArrayList(Collections.singleton("-1_3")));
        exp2.addCollection(rl2);
        Item tgtItem2 = createItem(tgtNs + "Gene", "2_1", "");
        translator.setGeneSynonyms(gene2, tgtItem2, srcNs);
        assertEquals(exp2, tgtItem2);
    }


    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/EnsemblDataTranslatorFunctionalTest_tgt.xml"));
    }

    protected Collection getSrcItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/EnsemblDataTranslatorFunctionalTest_src.xml"));
    }

    protected OntModel getOwlModel() {
        InputStreamReader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("genomic.n3"));

        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(reader, null, "N3");
        return ont;
    }

    protected String getModelName() {
        return "genomic";
    }

    private Item createItem(String clsName, String identifier, String imps) {
        Item item = new Item();
        item.setClassName(clsName);
        item.setIdentifier(identifier);
        item.setImplementations(imps);
        return item;
    }
}
