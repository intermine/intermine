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
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Arrays;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.File;
import java.io.BufferedReader;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.dataconversion.DataTranslator;
import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.dataconversion.MockItemReader;
import org.intermine.dataconversion.MockItemWriter;

/**
 * Test for translating MAGE data in fulldata Item format conforming to a source OWL definition
 * to fulldata Item format conforming to InterMine OWL definition.
 *
 * @author Wenyan Ji
 * @author Richard Smith
 */

public class MageDataTranslatorTest extends DataTranslatorTestCase {
    private String tgtNs = "http://www.flymine.org/model/genomic#";
    private String ns = "http://www.flymine.org/model/mage#";

    public void setUp() throws Exception {
        super.setUp();
    }


    public void testTranslate() throws Exception {
        Collection srcItems = getSrcItems();
        FileWriter writerSrc = new FileWriter(new File("src_items.xml"));
        writerSrc.write(FullRenderer.render(srcItems));
        writerSrc.close();


        Map itemMap = writeItems(srcItems);
        DataTranslator translator = new MageDataTranslator(new MockItemReader(itemMap),
                                                           getOwlModel(), tgtNs);

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        FileWriter writer = new FileWriter(new File("exptmp"));
        writer.write(FullRenderer.render(tgtIw.getItems()));
        writer.close();

        //assertEquals(new HashSet(expectedItems), tgtIw.getItems());

    }

    public void testCreateAuthors() {

        Item srcItem = createItem(ns + "BibliographicReference", "0_0", "");
        srcItem.addAttribute(new Attribute("authors", " William Whitfield; FlyChip Facility"));

        Item exp1 = createItem(tgtNs + "Author", "-1_1", "");
        exp1.addAttribute(new Attribute("name", "William Whitfield"));
        Item exp2 = createItem(tgtNs + "Author", "-1_2", "");
        exp2.addAttribute(new Attribute("name", "FlyChip Facility"));

        Set expected = new HashSet(Arrays.asList(new Object[] {exp1, exp2}));

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(new HashMap()),
                                                           getOwlModel(), tgtNs);
        assertEquals(expected, translator.createAuthors(srcItem));
    }

    public void testSetReporterLocationCoords() throws Exception{
        Item srcItem = createItem(ns + "FeatureReporterMap", "6_28", "");
        srcItem.addCollection(new ReferenceList("featureInformationSources", new ArrayList(Arrays.asList(new Object[]{"7_29"}))));

        Item srcItem2=createItem(ns + "FeatureInformation", "7_29", "");
        srcItem2. addReference(new Reference("feature", "8_30"));

        Item srcItem3=createItem(ns + "Feature", "8_30", "");
        srcItem3.addReference(new Reference("featureLocation", "9_31"));
        srcItem3.addReference(new Reference("zone", "10_17"));

        Item srcItem4 = createItem(ns + "FeatureLocation", "9_31", "");
        srcItem4.addAttribute(new Attribute("column", "1"));
        srcItem4.addAttribute(new Attribute("row", "2"));

        Item srcItem5 = createItem(ns + "Zone", "10_17", "");
        srcItem5.addAttribute(new Attribute("column", "1"));
        srcItem5.addAttribute(new Attribute("row", "1"));

        Set srcItems = new HashSet(Arrays.asList(new Object[]{srcItem, srcItem2, srcItem3, srcItem4, srcItem5}));
        Map itemMap = writeItems(srcItems);
        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(itemMap), getOwlModel(), tgtNs);
        //MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        //translator.translate(tgtIw);

        Item tgtItem = createItem(tgtNs + "ReporterLocation", "6_28", "");
        tgtItem.addAttribute(new Attribute("localX", "1"));
        tgtItem.addAttribute(new Attribute("localY", "2"));
        tgtItem.addAttribute(new Attribute("zoneX", "1"));
        tgtItem.addAttribute(new Attribute("zoneY", "1"));
        HashSet expected = new HashSet(Arrays.asList(new Object[]{tgtItem}));

        //assertEquals(expected, tgtIw.getItems());
        Item destItem = createItem(tgtNs + "ReporterLocation", "6_28", "");
        translator.setReporterLocationCoords(srcItem, destItem);
        assertEquals(tgtItem, destItem);
    }

    public void testCreateFeatureMap() throws Exception {
        Item srcItem1=createItem(ns + "PhysicalArrayDesign", "0_0", "");
        ReferenceList rl1=new ReferenceList("featureGroups", new ArrayList(Arrays.asList(new Object[] {"1_1"})));
        srcItem1.addCollection(rl1);

        Item srcItem2=createItem(ns + "FeatureGroup", "1_1", "");
        ReferenceList rl2=new ReferenceList("features", new ArrayList(Arrays.asList(new Object[] {"1_2"})));
        srcItem2.addCollection(rl2);

        Set srcItems = new HashSet(Arrays.asList(new Object[] {srcItem1, srcItem2}));
        Map itemMap = writeItems(srcItems);
        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(itemMap),
                                                              getOwlModel(), tgtNs);

        Item tgtItem = createItem(tgtNs+"MicroArraySlideDesign", "0_0", "");

        Map expected = new HashMap();
        expected.put("1_2", "0_0");
        translator.createFeatureMap(srcItem1);
        assertEquals(expected, translator.featureToDesign);
    }


    //test translate from PhysicalArrayDesign to MicroArraySlideDesign
    //which includes 3 methods, createFeatureMap, padDescriptions, and changeRefToAttr
    public void testTranslateMASD() throws Exception {
        Item srcItem1=createItem(ns + "PhysicalArrayDesign", "1_1", "");
        srcItem1.addCollection(new ReferenceList("descriptions", new ArrayList(Arrays.asList(new Object[] {"1_2", "2_2"} ))));
        srcItem1.addReference(new Reference("surfaceType","1_11"));
        srcItem1.addCollection(new ReferenceList("featureGroups", new ArrayList(Arrays.asList(new Object[] {"1_12"}))));

        Item srcItem2=createItem(ns + "Description", "1_2", "");
        srcItem2.addCollection(new ReferenceList("annotations", new ArrayList(Arrays.asList(new Object[] {"1_3", "1_4"}))));

        Item srcItem2a=createItem(ns + "Description", "2_2", "");
        srcItem2a.addCollection(new ReferenceList("annotations", new ArrayList(Arrays.asList(new Object[] {"2_3", "2_4"}))));

        Item srcItem3=createItem(ns + "OntologyEntry", "1_3", "");
        srcItem3.addAttribute(new Attribute("value", "double"));

        Item srcItem4=createItem(ns + "SurfaceType", "1_11", "");
        srcItem4.addAttribute(new Attribute("value", "polylysine"));

        Item srcItem5=createItem(ns + "FeatureGroup", "1_12", "");
        srcItem5.addCollection(new ReferenceList("features", new ArrayList(Arrays.asList(new Object[] {"1_13"}))));

        Set srcItems = new HashSet(Arrays.asList(new Object[] {srcItem1, srcItem2, srcItem2a, srcItem3, srcItem4, srcItem5}));
        Map itemMap = writeItems(srcItems);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(itemMap),
                                                              getOwlModel(), tgtNs);

        //MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        //translator.translate(tgtIw);


        // expected items
        // MicroArraySlideDesign 1_1

        Item expectedItem=createItem(tgtNs + "MicroArraySlideDesign", "1_1", "");
        expectedItem.addCollection(new ReferenceList("descriptions", new ArrayList(Arrays.asList(new Object[] {"1_3", "1_4", "2_3", "2_4"}))));
        expectedItem.addAttribute(new Attribute("surfaceType", "polylysine"));

        HashSet expected = new HashSet(Arrays.asList(new Object[]{expectedItem}));
        assertEquals(expected, translator.translateItem(srcItem1));

    }

    public void testMicroArrayExperiment()throws Exception {
        Item srcItem = createItem(ns+"Experiment", "61_748", "");
        srcItem.addAttribute(new Attribute("name", "P10005"));
        srcItem.addCollection(new ReferenceList("bioAssays", new ArrayList(Arrays.asList(new Object[]{"33_603", "57_709", "43_654"}))));
        srcItem.addCollection(new ReferenceList("descriptions", new ArrayList(Arrays.asList(new Object[]{"12_749", "12_750"}))));

        Item srcItem1= createItem(ns+"MeasuredBioAssay", "33_603", "");
        srcItem1.addReference(new Reference("featureExtraction", "4_2"));

        Item srcItem2= createItem(ns+"DerivedBioAssay", "57_709", "");
        srcItem2.addReference(new Reference("featureExtraction", "4_2"));

        Item srcItem3= createItem(ns+"PhysicalBioAssay", "43_654", "");
        srcItem3.addReference(new Reference("featureExtraction", "4_2"));

        Item srcItem4= createItem(ns+"Description", "12_749", "");
        srcItem4.addAttribute(new Attribute("text", "experiment description"));

        Item srcItem5= createItem(ns+"Description", "12_750", "");
        srcItem5.addCollection(new ReferenceList("bibliographicReferences", new ArrayList(Arrays.asList(new Object[]{"62_751"}))));

        Set src = new HashSet(Arrays.asList(new Object[]{srcItem, srcItem1, srcItem2, srcItem3, srcItem4, srcItem5}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap), getOwlModel(), tgtNs);

        Item expectedItem =createItem(tgtNs+"MicroArrayExperiment", "61_748", "");
        expectedItem.addAttribute(new Attribute("name", "P10005"));
        expectedItem.addAttribute(new Attribute("description", "experiment description"));
        expectedItem.addReference(new Reference("publication", "62_751"));
        //expectedItem.addCollection(new ReferenceList("assays", new ArrayList(Arrays.asList(new Object[]{"57_709"}))));
        HashSet expected=new HashSet(Arrays.asList(new Object[]{expectedItem}));

        assertEquals(expected, translator.translateItem(srcItem));

    }

    public void testMicroArrayAssay() throws Exception{
        Item srcItem= createItem(ns+"DerivedBioAssay", "57_709", "");
        srcItem.addCollection(new ReferenceList("derivedBioAssayData", new ArrayList(Arrays.asList(new Object[]{"58_710"}))));

        Item srcItem1= createItem(ns+"DerivedBioAssayData", "58_710", "");
        srcItem1.addReference(new Reference("bioDataValues", "58_739"));

        Item srcItem2= createItem(ns+"BioDataTuples", "58_739", "");
        srcItem2.addCollection(new ReferenceList("bioAssayTupleData", new ArrayList(Arrays.asList(new Object[]{"58_740", "58_744", "58_755"}))));

        Item srcItem10 = createItem(ns+"Experiment", "61_748", "");
        srcItem10.addCollection(new ReferenceList("bioAssays", new ArrayList(Arrays.asList(new Object[]{"33_603", "57_709", "43_654"}))));

        Item srcItem11= createItem(ns+"MeasuredBioAssay", "33_603", "");
        srcItem11.addReference(new Reference("featureExtraction", "4_2"));

        Item srcItem13= createItem(ns+"PhysicalBioAssay", "43_654", "");
        srcItem13.addReference(new Reference("bioAssayCreation", "50_735"));

        Item srcItem14= createItem(ns+"Hybridization", "50_735", "");
        srcItem14.addCollection(new ReferenceList("sourceBioMaterialMeasurements", new ArrayList(Arrays.asList(new Object[]{"26_736", "26_737"}))));

        Item srcItem15= createItem(ns+"BioMaterialMeasurement", "26_736", "");
        srcItem15.addReference(new Reference("bioMaterial", "23_78"));

        Item srcItem16= createItem(ns+"BioMaterialMeasurement", "26_737", "");
        srcItem16.addReference(new Reference("bioMaterial", "23_146"));


        Set src = new HashSet(Arrays.asList(new Object[]{srcItem, srcItem1, srcItem2,srcItem10, srcItem11, srcItem13, srcItem14, srcItem15, srcItem16 }));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap), getOwlModel(), tgtNs);

        Item expectedItem = createItem(tgtNs+"MicroArrayAssay", "57_709", "");
        //expectedItem.addCollection(new ReferenceList("results", new ArrayList(Arrays.asList(new Object[]{"58_740", "58_744", "58_755"}))));
        //expectedItem.addCollection(new ReferenceList("tissues", new ArrayList(Arrays.asList(new Object[]{"23_78", "23_146"}))));
        expectedItem.addReference(new Reference("experiment", "61_748"));

        Item expectedItem2 =createItem(tgtNs+"MicroArrayExperiment", "61_748", "");
        //expectedItem2.addCollection(new ReferenceList("assays", new ArrayList(Arrays.asList(new Object[]{"57_709"}))));

        HashSet expected=new HashSet(Arrays.asList(new Object[]{expectedItem, expectedItem2}));

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        assertEquals(expected, tgtIw.getItems());

    }

    public void testMicroArrayExperimentalResult() throws Exception {

        Item srcItem= createItem(ns+"BioAssayDatum", "58_762", "");
        srcItem.addReference(new Reference("quantitationType","40_620"));
        srcItem.addAttribute(new Attribute("normalised","true"));

        Item srcItem1= createItem(ns+"Error", "40_620", "");
        srcItem1.addAttribute(new Attribute("name", "Signal st dev Cy3"));
        srcItem1.addReference(new Reference("targetQuantitationType", "38_608"));

        Item srcItem2= createItem(ns+"MeasuredSignal", "38_608", "");
        srcItem2.addAttribute(new Attribute("isBackground", "false"));
        srcItem2.addReference(new Reference("scale", "1_611"));

        Item srcItem3= createItem(ns+"OntologyEntry", "1_611", "");
        srcItem3.addAttribute(new Attribute("value", "linear_scale"));


        Set src = new HashSet(Arrays.asList(new Object[]{srcItem, srcItem1, srcItem2, srcItem3}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                       getOwlModel(), tgtNs);

        Item expectedItem = createItem(tgtNs+"MicroArrayExperimentalResult", "58_762", "");
        expectedItem.addAttribute(new Attribute("normalised","true"));
        expectedItem.addAttribute(new Attribute("type","Signal st dev Cy3"));
        expectedItem.addAttribute(new Attribute("scale","linear_scale"));
        expectedItem.addAttribute(new Attribute("isBackground","false"));
        expectedItem.addReference(new Reference("analysis","-1_1"));

        Item expectedItem2 = createItem(tgtNs+"OntologyTerm", "1_611", "");
        expectedItem2.addAttribute(new Attribute("name","linear_scale"));

        HashSet expected=new HashSet(Arrays.asList(new Object[]{expectedItem, expectedItem2}));

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        assertEquals(expected, tgtIw.getItems());

    }

    public void testBioEntity() throws Exception {
        Item srcItem= createItem(ns+"BioSequence", "0_11", "");
        srcItem.addReference(new Reference("type","1_13"));
        srcItem.addCollection(new ReferenceList("sequenceDatabases",
                   new ArrayList(Arrays.asList(new Object[]{"2_14", "2_15", "2_16", "2_17"}))));

        Item srcItem1= createItem(ns+"OntologyEntry", "1_13", "");
        srcItem1.addAttribute(new Attribute("value", "cDNA_clone"));

        Item srcItem2= createItem(ns+"DatabaseEntry", "2_14", "");
        srcItem2.addAttribute(new Attribute("accession", "FBgn0010173"));
        srcItem2.addReference(new Reference("database", "3_7"));

        Item srcItem3= createItem(ns+"DatabaseEntry", "2_15", "");
        srcItem3.addAttribute(new Attribute("accession", "AY069331"));
        srcItem3.addReference(new Reference("database", "3_9"));

        Item srcItem4= createItem(ns+"DatabaseEntry", "2_16", "");
        srcItem4.addAttribute(new Attribute("accession", "AA201663"));
        srcItem4.addReference(new Reference("database", "3_9"));

        Item srcItem5= createItem(ns+"DatabaseEntry", "2_17", "");
        srcItem5.addAttribute(new Attribute("accession", "AW941561"));
        srcItem5.addReference(new Reference("database", "3_9"));

        Item srcItem6= createItem(ns+"Database", "3_7", "");
        srcItem6.addAttribute(new Attribute("name", "flybase"));

        Item srcItem7= createItem(ns+"Database", "3_9", "");
        srcItem7.addAttribute(new Attribute("name", "embl"));

        Item srcItem10=createItem(ns+"Reporter", "12_50", "");
        srcItem10.addAttribute(new Attribute("name","LD04815"));
        srcItem10.addCollection(new ReferenceList("featureReporterMaps",
                   new ArrayList(Arrays.asList(new Object[]{"7_46"}))));
        srcItem10.addCollection(new ReferenceList("immobilizedCharacteristics",
                   new ArrayList(Arrays.asList(new Object[]{"0_11"}))));

        Item srcItem11 = createItem(ns+"FeatureReporterMap", "7_46", "");
        srcItem11.addReference(new Reference("reporter", "12_50"));
        srcItem11.addCollection(new ReferenceList("featureInformationSources",
                   new ArrayList(Arrays.asList(new Object[]{"8_47"}))));

        Item srcItem12= createItem(ns+"FeatureInformation", "8_47", "");
        srcItem12.addReference(new Reference("feature", "11_49"));

        Item srcItem13= createItem(ns+"Feature", "11_49", "");
        srcItem13.addReference(new Reference("featureLocation", "9_480"));
        srcItem13.addReference(new Reference("zone", "10_290"));

        Item srcItem14= createItem(ns+"FeatureLocation", "9_480", "");
        srcItem14.addAttribute(new Attribute("column", "1"));
        srcItem14.addAttribute(new Attribute("row", "3"));

        Item srcItem15= createItem(ns+"FeatureLocation", "10_290", "");
        srcItem15.addAttribute(new Attribute("column", "2"));
        srcItem15.addAttribute(new Attribute("row", "5"));


        Item srcItem20= createItem(ns+"BioAssayDatum", "58_828", "");
        srcItem20.addAttribute(new Attribute("normalised", "false"));
        srcItem20.addReference(new Reference("designElement", "11_49"));

        Item srcItem21= createItem(ns+"BioAssayDatum", "58_821", "");
        srcItem21.addAttribute(new Attribute("normalised", "false"));
        srcItem21.addReference(new Reference("designElement", "11_49"));

        Item srcItem22= createItem(ns+"BioAssayDatum", "58_823", "");
        srcItem22.addAttribute(new Attribute("normalised", "false"));
        srcItem22.addReference(new Reference("designElement", "11_49"));

        Item srcItem30=createItem(ns+"PhysicalArrayDesign", "20_69", "");
        srcItem30.addCollection(new ReferenceList("featureGroups", new ArrayList(Arrays.asList(new Object[]{ "17_63"}))));

        Item srcItem31=createItem(ns+"FeatureGroup", "17_63", "");
        srcItem31.addCollection(new ReferenceList("features", new ArrayList(Arrays.asList(new Object[]{"11_49"}))));

        Set src = new HashSet(Arrays.asList(new Object[]{srcItem, srcItem1, srcItem2, srcItem3, srcItem4, srcItem5, srcItem6, srcItem7, srcItem10, srcItem11, srcItem12, srcItem13, srcItem14, srcItem15, srcItem20, srcItem21, srcItem22, srcItem30, srcItem31 }));

        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap), getOwlModel(), tgtNs);

        Item expectedItem = createItem(tgtNs+"CDNAClone", "0_11", "");
        expectedItem.addAttribute(new Attribute("identifier","LD04815"));
        expectedItem.addCollection(new ReferenceList("synonyms", new ArrayList(Arrays.asList(new Object[]{"2_15", "2_16", "2_17"}))));

        Item expectedItem1 = createItem(tgtNs+"Gene", "2_14", "");
        expectedItem1.addAttribute(new Attribute("organismDbId", "FBgn0010173"));


        Item expectedItem2 = createItem(tgtNs+"Synonym", "2_15", "");
        expectedItem2.addAttribute(new Attribute("type", "accession"));
        expectedItem2.addAttribute(new Attribute("value", "AY069331"));
        expectedItem2.addReference(new Reference("source", "-1_3"));
        expectedItem2.addReference(new Reference("subject", "0_11"));

        Item expectedItem3 = createItem(tgtNs+"Synonym", "2_16", "");
        expectedItem3.addAttribute(new Attribute("type", "accession"));
        expectedItem3.addAttribute(new Attribute("value", "AA201663"));
        expectedItem3.addReference(new Reference("source",  "-1_3"));
        expectedItem3.addReference(new Reference("subject", "0_11"));

        Item expectedItem4 = createItem(tgtNs+"Synonym", "2_17", "");
        expectedItem4.addAttribute(new Attribute("type", "accession"));
        expectedItem4.addAttribute(new Attribute("value", "AW941561"));
        expectedItem4.addReference(new Reference("source",  "-1_3"));
        expectedItem4.addReference(new Reference("subject", "0_11"));

        Item expectedItem5 = createItem(tgtNs+"OntologyTerm", "1_13", "");
        expectedItem5.addAttribute(new Attribute("name", "cDNA_clone"));

        Item expectedItem7 = createItem(tgtNs+"Database",  "-1_3", "");
        expectedItem7.addAttribute(new Attribute("title", "embl"));

        Item expectedItem10 = createItem(tgtNs+"MicroArrayExperimentalResult", "58_821", "");
        expectedItem10.addAttribute(new Attribute("normalised", "false"));
        expectedItem10.addReference(new Reference("analysis","-1_1"));
        expectedItem10.addReference(new Reference("material","0_11"));
        expectedItem10.addReference(new Reference("reporter","12_50"));
        expectedItem10.addCollection(new ReferenceList("genes",
                                new ArrayList(Arrays.asList(new Object[]{"2_14"}))));

        Item expectedItem11 = createItem(tgtNs+"MicroArrayExperimentalResult", "58_823", "");
        expectedItem11.addAttribute(new Attribute("normalised", "false"));
        expectedItem11.addReference(new Reference("analysis","-1_1"));
        expectedItem11.addReference(new Reference("material","0_11"));
        expectedItem11.addReference(new Reference("reporter","12_50"));
        expectedItem11.addCollection(new ReferenceList("genes",
                                new ArrayList(Arrays.asList(new Object[]{"2_14"}))));

        Item expectedItem12 = createItem(tgtNs+"MicroArrayExperimentalResult", "58_828", "");
        expectedItem12.addAttribute(new Attribute("normalised", "false"));
        expectedItem12.addReference(new Reference("analysis","-1_1"));
        expectedItem12.addReference(new Reference("material","0_11"));
        expectedItem12.addReference(new Reference("reporter","12_50"));
        expectedItem12.addCollection(new ReferenceList("genes",
                                new ArrayList(Arrays.asList(new Object[]{"2_14"}))));

        Item expectedItem13 = createItem(tgtNs+"Reporter", "12_50", "");
        expectedItem13.addReference(new Reference("material", "0_11"));
        expectedItem13.addReference(new Reference("location", "7_46"));

        Item expectedItem14 = createItem(tgtNs+"MicroArraySlideDesign", "20_69", "");

        Item expectedItem15 = createItem(tgtNs + "ReporterLocation", "7_46", "");
        expectedItem15.addAttribute(new Attribute("localX", "1"));
        expectedItem15.addAttribute(new Attribute("localY", "3"));
        expectedItem15.addAttribute(new Attribute("zoneX", "2"));
        expectedItem15.addAttribute(new Attribute("zoneY", "5"));
        expectedItem15.addReference(new Reference("design", "20_69"));
        expectedItem15.addReference(new Reference("reporter", "12_50"));

        HashSet expected=new HashSet(Arrays.asList(new Object[]{expectedItem, expectedItem1, expectedItem2,expectedItem3, expectedItem4, expectedItem5,  expectedItem7,expectedItem10,  expectedItem11,  expectedItem12, expectedItem13, expectedItem14, expectedItem15 }));

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        assertEquals(expected, tgtIw.getItems());

    }


    public void testBioEntity2MAER() throws Exception {

        Item srcItem= createItem(ns+"Reporter", "12_45", "");
        srcItem.addCollection(new ReferenceList("featureReporterMaps",
                   new ArrayList(Arrays.asList(new Object[]{"7_41"}))));
        srcItem.addCollection(new ReferenceList("immobilizedCharacteristics",
                   new ArrayList(Arrays.asList(new Object[]{"0_3"}))));

        Item srcItem1= createItem(ns+"FeatureReporterMap", "7_41", "");
        srcItem1.addCollection(new ReferenceList("featureInformationSources",
                   new ArrayList(Arrays.asList(new Object[]{"8_42"}))));

        Item srcItem2= createItem(ns+"FeatureInformation", "8_42", "");
        srcItem2.addReference(new Reference("feature", "9_43"));

        Item srcItem3= createItem(ns+"BioSequence", "0_3", "");
        srcItem3.addReference(new Reference("type", "1_5"));

        Item srcItem4 =createItem(ns+"OntologyEntry", "1_5", "");
        srcItem4.addAttribute(new Attribute("value", "cDNA_clone"));

        Set srcItems = new HashSet(Arrays.asList(new Object[]
                       {srcItem, srcItem1, srcItem2, srcItem3, srcItem4}));
        Map itemMap = writeItems(srcItems);
        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(itemMap),
                                                              getOwlModel(), tgtNs);

        Item tgtItem = createItem(tgtNs+"cDNAClone", "0_3", "");

        Map expected = new HashMap();
        expected.put("0_3", "9_43");
        translator.setBioEntityMap(srcItem,tgtItem);
        assertEquals(expected, translator.bioEntity2Feature);

    }

    public void testBioEntity2Identifier() throws Exception {

        Item srcItem= createItem(ns+"Reporter", "12_45", "");
        srcItem.addAttribute(new Attribute("name", "LD14383"));
        srcItem.addCollection(new ReferenceList("immobilizedCharacteristics",
                   new ArrayList(Arrays.asList(new Object[]{"0_3"}))));
        srcItem.addCollection(new ReferenceList("featureReporterMaps",
                   new ArrayList(Arrays.asList(new Object[]{"7_41"}))));

        Item srcItem1= createItem(ns+"FeatureReporterMap", "7_41", "");
        srcItem1.addCollection(new ReferenceList("featureInformationSources",
                    new ArrayList(Arrays.asList(new Object[]{"8_42"}))));

        Item srcItem2= createItem(ns+"FeatureInformation", "8_42", "");
        srcItem2.addReference(new Reference("feature","11_44"));

        Item srcItem3= createItem(ns+"BioSequence", "0_3", "");
        srcItem3.addReference(new Reference("type", "1_5"));

        Item srcItem4 =createItem(ns+"OntologyEntry", "1_5", "");
        srcItem4.addAttribute(new Attribute("value", "cDNA_clone"));

        Set srcItems = new HashSet(Arrays.asList(new Object[] {srcItem, srcItem1,srcItem2, srcItem3, srcItem4}));
        Map itemMap = writeItems(srcItems);
        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(itemMap),
                                                              getOwlModel(), tgtNs);

        Item tgtItem = createItem(tgtNs+"cDNAClone", "0_3", "");

        Map expected = new HashMap();
        expected.put("0_3", "LD14383");
        translator.setBioEntityMap(srcItem,tgtItem);
        assertEquals(expected, translator.bioEntity2IdentifierMap);

    }


    protected Collection getSrcItems() throws Exception {
        BufferedReader srcReader = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("test/MageTestData_adf.xml")));
        MockItemWriter mockIw = new MockItemWriter(new LinkedHashMap());
        MageConverter converter = new MageConverter(mockIw);
        converter.process(srcReader);


        //BufferedReader
        srcReader = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("test/MageTestData_exp.xml")));
        //converter = new MageConverter( mockIw);
        converter.process(srcReader);
        converter.close();

        return mockIw.getItems();

    }

    protected Collection getExpectedItems() throws Exception {
        Collection srcItems = getSrcItems();
        Map itemMap = writeItems(srcItems);
        DataTranslator translator = new MageDataTranslator(new MockItemReader(itemMap),
                                                           getOwlModel(), tgtNs);

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        return tgtIw.getItems();

    }

    protected OntModel getOwlModel() {
        InputStreamReader reader = new InputStreamReader(
            getClass().getClassLoader().getResourceAsStream("genomic.n3"));

        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(reader, null, "N3");

        return ont;
    }

    protected String getModelName() {
        return "genomic";
    }

    private Item createItem(String className, String itemId, String implementation){
        Item item=new Item();
        item.setIdentifier(itemId);
        item.setClassName(className);
        item.setImplementations(implementation);
        return item;
   }
}
