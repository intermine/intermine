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
import java.util.Iterator;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileWriter;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.dataconversion.DataTranslator;
import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.dataconversion.MockItemReader;
import org.intermine.dataconversion.MockItemWriter;

public class EnsemblHumanDataTranslatorTest extends DataTranslatorTestCase {
    private String tgtNs = "http://www.flymine.org/model/genomic#";

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testTranslate() throws Exception {
        Map itemMap = writeItems(getSrcItems());
        DataTranslator translator = new EnsemblHumanDataTranslator(new MockItemReader(itemMap),
                                                              getOwlModel(), tgtNs, "HS");
        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        FileWriter writer = new FileWriter(new File("exptmp"));
        writer.write(FullRenderer.render(tgtIw.getItems()));
        writer.close();

        String expectedNotActual = "in expected, not actual: " + compareItemSets(new HashSet(getExpectedItems()), tgtIw.getItems());
        String actualNotExpected = "in actual, not expected: " + compareItemSets(tgtIw.getItems(), new HashSet(getExpectedItems()));

        if (expectedNotActual.length() > 25) {
            System.out.println(expectedNotActual);
            System.out.println(actualNotExpected);
        }
        assertEquals(new HashSet(getExpectedItems()), tgtIw.getItems());
    }


    private Set compareItemSets(Set a, Set b) {
        Set diff = new HashSet(a);
        Iterator i = a.iterator();
        while (i.hasNext()) {
            Item itemA = (Item) i.next();
            Iterator j = b.iterator();
            while (j.hasNext()) {
                Item itemB = (Item) j.next();
                if (itemA.equals(itemB)) {
                    diff.remove(itemA);
                }
            }
        }
        return diff;
    }

    public void testSetGeneSynonyms() throws Exception {
        String srcNs = "http://www.flymine.org/model/ensembl-human#";
        Item gene = createItem(srcNs + "gene", "4_1", "");
        gene.addReference(new Reference("display_xref", "5_1"));

        Item xref = createItem(srcNs + "xref", "5_1", "");
        xref.addAttribute(new Attribute("dbprimary_acc", "FBgn1001"));
        xref.addReference(new Reference("external_db", "25_1100"));

        Item externalDb = createItem(srcNs + "external_db", "25_1100", "");
        externalDb.addAttribute(new Attribute("db_name", "RefSeq"));

        Map itemMap = writeItems(new HashSet(Arrays.asList(new Object[] {gene, xref, externalDb})));
        EnsemblHumanDataTranslator translator = new EnsemblHumanDataTranslator(
                   new MockItemReader(itemMap), getOwlModel(), tgtNs, "HS");

        Item exp1 = createItem(tgtNs + "Synonym", "-1_1", "");
        exp1.addAttribute(new Attribute("value", "FBgn1001"));
        exp1.addAttribute(new Attribute("type", "accession"));
        exp1.addReference(new Reference("source", "-1_2"));
        exp1.addReference(new Reference("subject", "4_1"));

        Set expected = new HashSet(Collections.singleton(exp1));
        Item tgtItem = createItem(tgtNs + "Gene", "4_1", "");
        assertEquals(expected, translator.setGeneSynonyms(gene, tgtItem, srcNs));
    }

    public void testSetOrganismDbId() throws Exception {
        String srcNs = "http://www.flymine.org/model/ensembl-human#";
        Item gene = createItem(srcNs + "gene", "1_1", "");
        gene.addReference(new Reference("seq_region", "1_99"));

        Item seq =  createItem(srcNs + "seq_region", "1_99", "");
        seq.addReference(new Reference("coord_system", "30_1"));
        seq.addAttribute(new Attribute("name", "1"));
        seq.addAttribute(new Attribute("length", "24612"));

        Item coord = createItem(srcNs + "coord_system", "30_1", "");
        coord.addAttribute(new Attribute("name", "chromosome"));

        Item genedes = createItem(srcNs + "gene_description", "5_1", "");
        genedes.addReference(new Reference("gene", "1_1"));

        Item transcript = createItem(srcNs + "transcript", "2_1", "");
        transcript.addReference(new Reference("gene", "1_1"));
        transcript.addReference(new Reference("translation", "4_1"));
        Item stableId = createItem(srcNs + "gene_stable_id", "3_1", "");
        stableId.addAttribute(new Attribute("stable_id", "FBgn1001"));
        stableId.addReference(new Reference("gene", "1_1"));

        Map itemMap = writeItems(new HashSet(Arrays.asList(new Object[] {gene, genedes, stableId, transcript, seq, coord})));
        EnsemblHumanDataTranslator translator = new EnsemblHumanDataTranslator(
                      new MockItemReader(itemMap), getOwlModel(), tgtNs, "HS");

        Item exp1 = createItem(tgtNs + "Gene", "1_1", "");
        exp1.addAttribute(new Attribute("organismDbId", "FBgn1001"));
        exp1.addAttribute(new Attribute("identifier", "FBgn1001"));
        exp1.addReference(new Reference("organism", "-1_1"));
        exp1.addCollection(new ReferenceList("evidence", new ArrayList(Arrays.asList(new Object[]{"-1_5", "-1_2"}))));
        exp1.addCollection(new ReferenceList("comments", new ArrayList(Collections.singleton("5_1"))));
        exp1.addCollection(new ReferenceList("objects", new ArrayList(Collections.singleton("-1_3"))));
        Item exp2 = createItem(tgtNs + "Location", "-1_3", "");
        exp2.addAttribute(new Attribute("endIsPartial", "false"));
        exp2.addAttribute(new Attribute("startIsPartial", "false"));
        exp2.addReference(new Reference("subject", "1_1"));
        exp2.addReference(new Reference("object", "-1_4"));

        Item exp3 = createItem(tgtNs + "ComputationalResult", "-1_5", "");
        exp3.addReference(new Reference("source", "-1_2"));

        Set expected = new HashSet(Arrays.asList(new Object[] {exp1, exp2, exp3}));
        assertEquals(expected, translator.translateItem(gene));
    }

    public void testMergeProteins() throws Exception {
        String srcNs = "http://www.flymine.org/model/ensembl-human#";
        Item translation1 = createItem(srcNs + "translation", "1_2", "");
        translation1.addReference(new Reference("transcript", "1_1"));

        Item transcript1 = createItem(srcNs + "transcript", "1_1", "");
        transcript1.addReference(new Reference("display_xref", "1_4"));
        transcript1.addReference(new Reference("seq_region", "1_10"));
        transcript1.addAttribute(new Attribute("seq_region_start", "100"));
        transcript1.addAttribute(new Attribute("seq_region_end", "900"));
        transcript1.addAttribute(new Attribute("seq_region_strand", "1"));

        Item stableId1 = createItem(srcNs + "transcript_stable_id", "1_6", "");
        stableId1.addAttribute(new Attribute("stable_id", "TRANScript1"));
        stableId1.addReference(new Reference("transcript", "1_1"));

        Item xref1 = createItem(srcNs + "xref", "1_4", "");
        xref1.addAttribute(new Attribute("dbprimary_acc", "Q1001"));
        xref1.addReference(new Reference("external_db", "1_5"));

        Item externalDb1 = createItem(srcNs + "external_db", "1_5", "");
        externalDb1.addAttribute(new Attribute("db_name", "Uniprot/SWISSPROT"));

        Item seq1 =  createItem(srcNs + "seq_region", "1_10", "");
        seq1.addReference(new Reference("coord_system", "30_1"));
        seq1.addAttribute(new Attribute("name", "1"));
        seq1.addAttribute(new Attribute("length", "2461200"));

        Item coord1 = createItem(srcNs + "coord_system", "30_1", "");
        coord1.addAttribute(new Attribute("name", "chromosome"));

        Item translation2 = createItem(srcNs + "translation", "2_2", "");
        translation2.addReference(new Reference("transcript", "2_1"));

        Item transcript2 = createItem(srcNs + "transcript", "2_1", "");
        transcript2.addReference(new Reference("display_xref", "2_4"));
        transcript2.addReference(new Reference("seq_region", "1_11"));
        transcript2.addAttribute(new Attribute("seq_region_start", "1001"));
        transcript2.addAttribute(new Attribute("seq_region_end", "9001"));
        transcript2.addAttribute(new Attribute("seq_region_strand", "1"));

        Item stableId2 = createItem(srcNs + "transcript_stable_id", "2_6", "");
        stableId2.addAttribute(new Attribute("stable_id", "TRANScript2"));
        stableId2.addReference(new Reference("transcript", "2_1"));

        Item xref2 = createItem(srcNs + "xref", "2_4", "");
        xref2.addAttribute(new Attribute("dbprimary_acc", "Q1001"));
        xref2.addReference(new Reference("external_db", "2_5"));

        Item externalDb2 = createItem(srcNs + "external_db", "2_5", "");
        externalDb2.addAttribute(new Attribute("db_name", "Uniprot/SWISSPROT"));

        Item seq2 =  createItem(srcNs + "seq_region", "1_11", "");
        seq2.addReference(new Reference("coord_system", "30_1"));
        seq2.addAttribute(new Attribute("name", "1"));
        seq2.addAttribute(new Attribute("length", "1124612"));


        Map itemMap = writeItems(new HashSet(Arrays.asList(new Object[] {transcript1, translation1, xref1, stableId1, externalDb1, seq1, coord1, transcript2, translation2,  xref2, externalDb2, stableId2, seq2})));
        EnsemblHumanDataTranslator translator = new EnsemblHumanDataTranslator(
                    new MockItemReader(itemMap), getOwlModel(), tgtNs, "HS");


        Item protein = createItem(tgtNs + "Protein", "-1_12", "");
        //protein.addAttribute(new Attribute("idenitifer", "Q1001"));
        protein.addAttribute(new Attribute("primaryAccession", "Q1001"));
        protein.addReference(new Reference("organism", "-1_1"));
        protein.addCollection(new ReferenceList("subjects", new ArrayList(Arrays.asList(new Object[] {"-1_14","-1_20"}))));
        protein.addCollection(new ReferenceList("synonyms", new ArrayList(Collections.singleton("-1_13"))));
        Item synonym0 = createItem(tgtNs + "Synonym", "-1_13", "");
        synonym0.addAttribute(new Attribute("type", "accession"));
        synonym0.addAttribute(new Attribute("value", "Q1001"));
        synonym0.addReference(new Reference("subject", "-1_12"));
        synonym0.addReference(new Reference("source", "-1_5"));

        Item synonym1 = createItem(tgtNs + "Synonym", "1_6", "");
        synonym1.addAttribute(new Attribute("type", "accession"));
        synonym1.addAttribute(new Attribute("value", "TRANScript1"));
        synonym1.addReference(new Reference("subject", "1_1"));
        synonym1.addReference(new Reference("source", "-1_2"));

        Item synonym2 = createItem(tgtNs + "Synonym", "2_6", "");
        synonym2.addAttribute(new Attribute("type", "accession"));
        synonym2.addAttribute(new Attribute("value", "TRANScript2"));
        synonym2.addReference(new Reference("subject", "2_1"));
        synonym2.addReference(new Reference("source", "-1_2"));

        Item trans1 = createItem(tgtNs + "Transcript", "1_1", "");
        trans1.addAttribute(new Attribute("identifier", "TRANScript1"));
        trans1.addReference(new Reference("organism", "-1_1"));
        trans1.addCollection(new ReferenceList("objects", new ArrayList(Arrays.asList(new Object[] {"-1_16","-1_17"}))));
        //trans1.addCollection(new ReferenceList("subjects", new ArrayList(Collections.singleton("-1_9"))));

        Item trans2 = createItem(tgtNs + "Transcript", "2_1", "");
        trans2.addAttribute(new Attribute("identifier", "TRANScript2"));
        trans2.addReference(new Reference("organism", "-1_1"));
        trans2.addCollection(new ReferenceList("objects", new ArrayList(Arrays.asList(new Object[] {"-1_21","-1_22"}))));
        //trans2.addCollection(new ReferenceList("subjects", new ArrayList(Collections.singleton("-1_13"))));

        Item loca1 = createItem(tgtNs + "Location", "-1_17", "");
        loca1.addAttribute(new Attribute("start", "100"));
        loca1.addAttribute(new Attribute("end", "900"));
        loca1.addAttribute(new Attribute("strand", "1"));
        loca1.addAttribute(new Attribute("endIsPartial", "false"));
        loca1.addAttribute(new Attribute("startIsPartial", "false"));
        loca1.addReference(new Reference("subject", "1_1"));
        loca1.addReference(new Reference("object", "-1_11"));

        Item loca2 = createItem(tgtNs + "Location", "-1_22", "");
        loca2.addAttribute(new Attribute("start", "1001"));
        loca2.addAttribute(new Attribute("end", "9001"));
        loca2.addAttribute(new Attribute("strand", "1"));
        loca2.addAttribute(new Attribute("endIsPartial", "false"));
        loca2.addAttribute(new Attribute("startIsPartial", "false"));
        loca2.addReference(new Reference("subject", "2_1"));
        loca2.addReference(new Reference("object", "-1_15"));

        Item chr1 = createItem(tgtNs +"Chromosome", "-1_11", "");
        chr1.addAttribute(new Attribute("identifier", "1"));
        chr1.addAttribute(new Attribute("length", "2461200"));

        Item chr2 = createItem(tgtNs +"Chromosome", "-1_15", "");
        chr2.addAttribute(new Attribute("identifier", "1"));
        chr2.addAttribute(new Attribute("length", "1124612"));

        Set expected = new HashSet(Arrays.asList(new Object[] {protein, trans1, trans2, synonym0, synonym1, synonym2, loca1, loca2, chr1, chr2}));


        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);
        Set result = new HashSet();
        Iterator resIter = tgtIw.getItems().iterator();
        while (resIter.hasNext()) {
            Item item = (Item) resIter.next();
            if (!(item.getClassName().equals(tgtNs + "Database")
                 || item.getClassName().equals(tgtNs + "SimpleRelation")
                 || item.getClassName().equals(tgtNs + "Organism"))) {
                result.add(item);
            }
        }
        assertEquals(expected, result);
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
