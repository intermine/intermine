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
import java.util.Collections;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.File;
import java.io.BufferedReader;

import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemFactory;
import org.intermine.dataconversion.DataTranslator;
import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.dataconversion.MockItemReader;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

/**
 * Test for translating MAGE data in fulldata Item format conforming to a source OWL definition
 * to fulldata Item format conforming to InterMine OWL definition.
 *
 * @author Wenyan Ji
 * @author Richard Smith
 */

public class MageDataTranslatorTest extends DataTranslatorTestCase {
    private String tgtNs = "http://www.flymine.org/model/genomic#";
    private String srcNs = "http://www.flymine.org/model/mage#";
    private ItemFactory srcItemFactory;
    private ItemFactory tgtItemFactory;

    public MageDataTranslatorTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        srcItemFactory = new ItemFactory(Model.getInstanceByName("mage"));
        tgtItemFactory = new ItemFactory(Model.getInstanceByName("genomic"));
    }


    public void testTranslate() throws Exception {
        Collection srcItems = getSrcItems();
        //FileWriter writerSrc = new FileWriter(new File("src_items.xml"));
        //writerSrc.write(FullRenderer.render(srcItems));
        //writerSrc.close();


        Map itemMap = writeItems(srcItems);
        DataTranslator translator = new MageDataTranslator(new MockItemReader(itemMap),
                                                           mapping, srcModel, getTargetModel(tgtNs));

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        //FileWriter writer = new FileWriter(new File("exptmp"));
        //writer.write(FullRenderer.render(tgtIw.getItems()));
        //writer.close();

        //assertEquals(new HashSet(expectedItems), tgtIw.getItems());

    }

    public void testCreateAuthors() throws Exception {

        Item srcItem = createSrcItem("BibliographicReference", "0_0", "");
        srcItem.addAttribute(new Attribute("authors", " William Whitfield; FlyChip Facility"));

        Item exp1 = createTgtItem( "Author", "-1_1", "");
        exp1.addAttribute(new Attribute("name", "William Whitfield"));
        Item exp2 = createTgtItem("Author", "-1_2", "");
        exp2.addAttribute(new Attribute("name", "FlyChip Facility"));

        Set expected = new HashSet(Arrays.asList(new Object[] {exp1, exp2}));

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(new HashMap()),
                                                               mapping, srcModel, getTargetModel(tgtNs));
        assertEquals(expected, translator.createAuthors(srcItem));
    }


    public void testMicroArrayExperimentDetails()throws Exception {
        Item srcItem1 = createSrcItem("Experiment", "61_748", "");
        srcItem1.setAttribute("name", "P10005");
        srcItem1.addCollection(new ReferenceList("descriptions", new ArrayList(Arrays.asList(new Object[]{"12_749", "12_750"}))));

        Item srcItem2= createSrcItem("Description", "12_749", "");
        srcItem2.addAttribute(new Attribute("text", "experiment description"));

        Item srcItem3= createSrcItem("Description", "12_750", "");
        srcItem3.addCollection(new ReferenceList("bibliographicReferences", new ArrayList(Arrays.asList(new Object[]{"62_751"}))));

        Set src = new HashSet(Arrays.asList(new Object[]{srcItem1, srcItem2, srcItem3}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs));

        Item expectedItem =createTgtItem("MicroArrayExperiment", "61_748", "");
        expectedItem.addAttribute(new Attribute("name", "P10005"));
        expectedItem.addAttribute(new Attribute("description", "experiment description"));
        expectedItem.addReference(new Reference("publication", "62_751"));
        HashSet expected=new HashSet(Arrays.asList(new Object[]{expectedItem}));

        assertEquals(expected, translator.translateItem(srcItem1));
    }


    public void testTranslateMicroArrayAssay() throws Exception{
        Item srcItem1= createSrcItem("MeasuredBioAssay", "57_709", "");
        srcItem1.addCollection(new ReferenceList("measuredBioAssayData", new ArrayList(Arrays.asList(new Object[]{"58_710"}))));

        Item srcItem2= createSrcItem("MeasuredBioAssayData", "58_710", "");
        srcItem2.addReference(new Reference("bioDataValues", "58_739"));

        Item srcItem3= createSrcItem("BioDataTuples", "58_739", "");
        srcItem3.addCollection(new ReferenceList("bioAssayTupleData", new ArrayList(Arrays.asList(new Object[]{"58_740", "58_744"}))));

        Item srcItem4 = createSrcItem("Experiment", "61_748", "");

        Set src = new HashSet(Arrays.asList(new Object[] {srcItem1, srcItem2, srcItem3, srcItem4}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs));

        Item expItem1 = createTgtItem("MicroArrayAssay", "57_709", "");
        expItem1.setReference("experiment", "61_748");

        Item expItem2 = createTgtItem("MicroArrayExperiment", "61_748", "");

        HashSet expected=new HashSet(Arrays.asList(new Object[]{expItem1, expItem2}));

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        assertEquals(expected, tgtIw.getItems());

        // mage:BioAssayDatum to genomic:MicroArrayAssy
        Map expResultToAssay = new HashMap();
        expResultToAssay.put("58_740", "57_709");
        expResultToAssay.put("58_744", "57_709");
        assertEquals(expResultToAssay, translator.resultToAssay);
    }


    public void testMicroArrayAssayLabeledExtract() throws Exception{
        Item srcItem1 = createSrcItem("MeasuredBioAssay", "0_1", "");
        srcItem1.setReference("featureExtraction", "1_1");

        Item srcItem2 = createSrcItem("FeatureExtraction", "1_1", "");
        srcItem2.setReference("physicalBioAssaySource", "2_1");

        Item srcItem3 = createSrcItem("PhysicalBioAssay", "2_1", "");
        srcItem3.setReference("bioAssayCreation", "3_1");

        Item srcItem4 = createSrcItem("Hybridization", "3_1", "");
        srcItem4.addCollection(new ReferenceList("sourceBioMaterialMeasurements",
                               new ArrayList(Arrays.asList(new Object[] {"4_1", "4_2"}))));

        Item srcItem5 = createSrcItem("BioMaterialMeasurement", "4_1", "");
        srcItem5.setReference("bioMaterial", "5_1");

        Item srcItem6 = createSrcItem("BioMaterialMeasurement", "4_2", "");
        srcItem6.setReference("bioMaterial", "5_2");

        Item srcItem7 = createSrcItem("LabeledExtract", "5_1", "");

        Item srcItem8 = createSrcItem("LabeledExtract", "5_2", "");

        Set srcItems = new HashSet(Arrays.asList(new Object[] {srcItem1, srcItem2, srcItem3, srcItem4, srcItem5, srcItem6, srcItem7}));
        Map srcMap = writeItems(srcItems);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs));
        translator.translateItem(srcItem1);

        // genomic:MicroArrayAssay to list of mage:LabeledExtract identifiers
        Map expExtractToAssay = new HashMap();
        expExtractToAssay.put("5_1", "0_1");
        expExtractToAssay.put("5_2", "0_1");
        assertEquals(expExtractToAssay, translator.labeledExtractToAssay);
    }


    public void testTranslateTreatment() throws Exception {
        Item srcItem1 = createSrcItem("Treatment", "0_1", "");
        srcItem1.setReference("action","1_1");

        Item srcItem2 = createSrcItem("OntologyEntry", "1_1", "");
        srcItem2.setAttribute("category", "Action");
        srcItem2.setAttribute("value", "labeling");

        Set src = new HashSet(Arrays.asList(new Object[] {srcItem1, srcItem2}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs));

        Item expItem1 = createTgtItem("Treatment", "0_1", "");
        expItem1.setAttribute("action", "labeling");

        HashSet expected = new HashSet(Collections.singleton(expItem1));

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        assertEquals(expected, tgtIw.getItems());
    }


    public void testTranslateReporter() throws Exception {
        Item srcItem1 = createSrcItem("Reporter", "0_1", "");
        srcItem1.addCollection(new ReferenceList("featureReporterMaps",
                                                 new ArrayList(Collections.singleton("1_1"))));


        Item srcItem2 = createSrcItem("FeatureReporterMap", "1_1", "");
        srcItem2.addCollection(new ReferenceList("featureInformationSources",
                                                 new ArrayList(Collections.singleton("2_1"))));

        Item srcItem3 = createSrcItem("FeatureInformation", "2_1", "");
        srcItem3.setReference("feature", "3_1");

        Set src = new HashSet(Arrays.asList(new Object[] {srcItem1, srcItem2, srcItem3}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs));

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        // map from mage:Feature identifier to genomic:Reporter
        Map expFeatureToReporter = new HashMap();
        expFeatureToReporter.put("3_1", "0_1");
        assertEquals(expFeatureToReporter, translator.featureToReporter);
    }

    public void testSearchTreatments() throws Exception {

        Item srcItem1 = createSrcItem("LabeledExtract", "0_1", "");
        srcItem1.addCollection(new ReferenceList("treatments",
                                                 new ArrayList(Arrays.asList(new Object[] {"1_1"}))));

        Item srcItem2 = createSrcItem("Treatment", "1_1", "");
        srcItem2.addCollection(new ReferenceList("sourceBioMaterialMeasurements",
                                                 new ArrayList(Arrays.asList(new Object[] {"2_1"}))));

        Item srcItem3 = createSrcItem("BioMaterialMeasurement", "2_1", "");
        srcItem3.setReference("bioMaterial", "0_2");

        Item srcItem4 = createSrcItem("LabeledExtract", "0_2", "");
        srcItem4.addCollection(new ReferenceList("treatments",
                                                 new ArrayList(Arrays.asList(new Object[] {"1_2"}))));

        Item srcItem5 = createSrcItem("Treatment", "1_2", "");
        srcItem5.addCollection(new ReferenceList("sourceBioMaterialMeasurements",
                                                 new ArrayList(Arrays.asList(new Object[] {"2_2"}))));

        Item srcItem6 = createSrcItem("BioMaterialMeasurement", "2_2", "");
        srcItem6.setReference("bioMaterial", "3_1");

        Item srcItem7 = createSrcItem("BioSource", "3_1", "");



        Set srcItems = new HashSet(Arrays.asList(new Object[] {srcItem1, srcItem2, srcItem3, srcItem4, srcItem5, srcItem6, srcItem7}));
        Map srcMap = writeItems(srcItems);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs));
        // call with top level LabeledExtract and empty list
        List treatments = new ArrayList();
        translator.searchTreatments(srcItem1, treatments);

        List expTreatments = new ArrayList(Arrays.asList(new Object[] {"1_1", "1_2"}));
        assertEquals(expTreatments, treatments);

        // genomic:Sample identifier to list of genomic:Treatment identifiers
        Map expSampleToTreatments = new HashMap();
        expSampleToTreatments.put("3_1", new ArrayList(Arrays.asList(new Object[] {"1_1", "1_2"})));
        assertEquals(expSampleToTreatments, translator.sampleToTreatments);
    }


    public void testTranslateMicroArrayResult() throws Exception {
        Item srcItem1= createSrcItem("BioAssayDatum", "58_762", "");
        srcItem1.setReference("quantitationType","40_620");
        srcItem1.setReference("designElement","3_1");

        Item srcItem2= createSrcItem("SpecializedQuantitationType", "40_620", "");
        srcItem2.setAttribute("name", "Log Ratio");
        srcItem2.setReference("scale", "1_611");

        Item srcItem3= createSrcItem("OntologyEntry", "1_611", "");
        srcItem3.setAttribute("value", "log");

        Item srcItem4= createSrcItem("Feature", "3_1", "");

        Set src = new HashSet(Arrays.asList(new Object[]{srcItem1, srcItem2, srcItem3}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs));

        Item expectedItem = createTgtItem("MicroArrayResult", "58_762", "");
        expectedItem.setAttribute("scale","log");
        expectedItem.setAttribute("type","Log Ratio");
        expectedItem.setReference("analysis","-1_1");

        HashSet expected = new HashSet(Collections.singleton(expectedItem));

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        assertEquals(expected, tgtIw.getItems());

        // map from genomic:MicroArrayResult to mage:Feature
        Map expResultToFeature = new HashMap();
        expResultToFeature.put("58_762", "3_1");
        assertEquals(expResultToFeature, translator.resultToFeature);
    }


    public void testProcessMicroArrayResults() throws Exception {
        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(new HashMap()),
                                                               mapping, srcModel, getTargetModel(tgtNs));

        Item result = createTgtItem("MicroArrayResult", "0_1", "");
        translator.microArrayResults.add(result);

        translator.resultToAssay.put("0_1", "1_1");

        translator.resultToFeature.put("0_1", "2_1");
        translator.featureToReporter.put("2_1", "3_1");

        Item expResult = createTgtItem("MicroArrayResult", "0_1", "");
        expResult.setReference("assay", "1_1");
        expResult.setReference("reporter", "3_1");
        Set exp = new HashSet(Collections.singleton(expResult));

        translator.processMicroArrayResults();
        assertEquals(exp, translator.microArrayResults);


    }


    public void testProcessSamples() throws Exception {
        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(new HashMap()),
                                                               mapping, srcModel, getTargetModel(tgtNs));

        Item sample = createTgtItem("Sample", "0_1", "");
        translator.samples.add(sample);

        translator.sampleToTreatments.put("0_1", new ArrayList(Arrays.asList(new Object[] {"1_1", "1_2"})));

        translator.sampleToLabeledExtract.put("0_1", "2_1");
        translator.labeledExtractToAssay.put("2_1", "3_1");


        Item expSample = createTgtItem("Sample", "0_1", "");
        expSample.addCollection(new ReferenceList("treatments", new ArrayList(Arrays.asList(new Object[] {"1_1", "1_2"}))));
        expSample.setReference("assay", "3_1");
        Set exp = new HashSet(Collections.singleton(expSample));

        translator.processSamples();
        assertEquals(exp, translator.samples);
    }

    public void testMicroArrayResultError() throws Exception {
        Item srcItem= createSrcItem("BioAssayDatum", "58_762", "");
        srcItem.setReference("quantitationType","40_620");

        Item srcItem1= createSrcItem("Error", "40_620", "");
        srcItem1.setAttribute("name", "Signal st dev Cy3");
        srcItem1.setReference("targetQuantitationType", "38_608");

        Item srcItem2= createSrcItem("MeasuredSignal", "38_608", "");
        srcItem2.setReference("scale", "1_611");

        Item srcItem3= createSrcItem("OntologyEntry", "1_611", "");
        srcItem3.setAttribute("value", "linear_scale");


        Set src = new HashSet(Arrays.asList(new Object[]{srcItem, srcItem1, srcItem2, srcItem3}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs));

        Item expectedItem = createTgtItem("MicroArrayResult", "58_762", "");
        expectedItem.setAttribute("type","Signal st dev Cy3");
        expectedItem.setAttribute("scale","linear_scale");
        expectedItem.setReference("analysis","-1_1");

        HashSet expected=new HashSet(Collections.singleton(expectedItem));

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        assertEquals(expected, tgtIw.getItems());
    }


    public void testTranslateSample() throws Exception {
        Item srcItem1 = createSrcItem("BioSource", "0_1", "");
        srcItem1.setAttribute("identifier", "S:BioSource:FLYC:10");
        srcItem1.setAttribute("name", "BioSource name");
        srcItem1.setReference("materialType", "1_3");
        srcItem1.addCollection(new ReferenceList("characteristics",
                                                 new ArrayList(Arrays.asList(new Object[] {"1_1", "1_2"}))));

        Item srcItem2 = createSrcItem("OntologyEntry", "1_1", "");
        srcItem2.setAttribute("category", "Organism");
        srcItem2.setAttribute("value", "Giraffe");

        Item srcItem3 = createSrcItem("OntologyEntry", "1_2", "");
        srcItem3.setAttribute("category", "height");
        srcItem3.setAttribute("value", "30 metres");

        Item srcItem4 = createSrcItem("OntologyEntry", "1_3", "");
        srcItem4.setAttribute("category", "materialType");
        srcItem4.setAttribute("value", "genomic DNA");

        Set src = new HashSet(Arrays.asList(new Object[]{srcItem1, srcItem2, srcItem3, srcItem4}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs));


        Item expItem1 = createTgtItem("Sample", "0_1", "");
        expItem1.setAttribute("name", "BioSource name");
        expItem1.setAttribute("materialType", "genomic DNA");
        expItem1.setReference("organism", "-1_1");
        expItem1.addCollection(new ReferenceList("characteristics", new ArrayList(Collections.singleton("-1_2"))));

        Item expItem2 = createTgtItem("Organism", "-1_1", "");
        expItem2.setAttribute("name", "Giraffe");

        Item expItem3 = createTgtItem("SampleCharacteristic", "-1_2", "");
        expItem3.setAttribute("type", "height");
        expItem3.setAttribute("value", "30 metres");

        HashSet expected=new HashSet(Arrays.asList(new Object[]{expItem1, expItem2, expItem3}));

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        assertEquals(expected, tgtIw.getItems());
    }


    public void XtestBioEntity() throws Exception {
        Item srcItem= createSrcItem("BioSequence", "0_11", "");
        srcItem.addReference(new Reference("type","1_13"));
        srcItem.addCollection(new ReferenceList("sequenceDatabases",
                   new ArrayList(Arrays.asList(new Object[]{"2_14", "2_15", "2_16", "2_17"}))));

        Item srcItem1= createSrcItem("OntologyEntry", "1_13", "");
        srcItem1.addAttribute(new Attribute("value", "cDNA_clone"));

        Item srcItem2= createSrcItem("DatabaseEntry", "2_14", "");
        srcItem2.addAttribute(new Attribute("accession", "FBgn0010173"));
        srcItem2.addReference(new Reference("database", "3_7"));

        Item srcItem3= createSrcItem("DatabaseEntry", "2_15", "");
        srcItem3.addAttribute(new Attribute("accession", "AY069331"));
        srcItem3.addReference(new Reference("database", "3_9"));

        Item srcItem4= createSrcItem("DatabaseEntry", "2_16", "");
        srcItem4.addAttribute(new Attribute("accession", "AA201663"));
        srcItem4.addReference(new Reference("database", "3_9"));

        Item srcItem5= createSrcItem("DatabaseEntry", "2_17", "");
        srcItem5.addAttribute(new Attribute("accession", "AW941561"));
        srcItem5.addReference(new Reference("database", "3_9"));

        Item srcItem6= createSrcItem("Database", "3_7", "");
        srcItem6.addAttribute(new Attribute("name", "flybase"));

        Item srcItem7= createSrcItem("Database", "3_9", "");
        srcItem7.addAttribute(new Attribute("name", "embl"));

        Item srcItem10=createSrcItem("Reporter", "12_50", "");
        srcItem10.addAttribute(new Attribute("name","LD04815"));
        srcItem10.addCollection(new ReferenceList("featureReporterMaps",
                   new ArrayList(Arrays.asList(new Object[]{"7_46"}))));
        srcItem10.addCollection(new ReferenceList("immobilizedCharacteristics",
                   new ArrayList(Arrays.asList(new Object[]{"0_11"}))));

        Item srcItem11 = createSrcItem("FeatureReporterMap", "7_46", "");
        srcItem11.addReference(new Reference("reporter", "12_50"));
        srcItem11.addCollection(new ReferenceList("featureInformationSources",
                   new ArrayList(Arrays.asList(new Object[]{"8_47"}))));

        Item srcItem12= createSrcItem("FeatureInformation", "8_47", "");
        srcItem12.addReference(new Reference("feature", "11_49"));

        Item srcItem13= createSrcItem("Feature", "11_49", "");
        srcItem13.addReference(new Reference("featureLocation", "9_480"));
        srcItem13.addReference(new Reference("zone", "10_290"));

        Item srcItem14= createSrcItem("FeatureLocation", "9_480", "");
        srcItem14.addAttribute(new Attribute("column", "1"));
        srcItem14.addAttribute(new Attribute("row", "3"));

        Item srcItem15= createSrcItem("FeatureLocation", "10_290", "");
        srcItem15.addAttribute(new Attribute("column", "2"));
        srcItem15.addAttribute(new Attribute("row", "5"));


        Item srcItem20= createSrcItem("BioAssayDatum", "58_828", "");
        srcItem20.addReference(new Reference("designElement", "11_49"));

        Item srcItem21= createSrcItem("BioAssayDatum", "58_821", "");
        srcItem21.addReference(new Reference("designElement", "11_49"));

        Item srcItem22= createSrcItem("BioAssayDatum", "58_823", "");
        srcItem22.addReference(new Reference("designElement", "11_49"));

        Item srcItem30=createSrcItem("PhysicalArrayDesign", "20_69", "");
        srcItem30.addCollection(new ReferenceList("featureGroups", new ArrayList(Arrays.asList(new Object[]{ "17_63"}))));

        Item srcItem31=createSrcItem("FeatureGroup", "17_63", "");
        srcItem31.addCollection(new ReferenceList("features", new ArrayList(Arrays.asList(new Object[]{"11_49"}))));

        Set src = new HashSet(Arrays.asList(new Object[]{srcItem, srcItem1, srcItem2, srcItem3, srcItem4, srcItem5, srcItem6, srcItem7, srcItem10, srcItem11, srcItem12, srcItem13, srcItem14, srcItem15, srcItem20, srcItem21, srcItem22, srcItem30, srcItem31 }));

        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs));

        Item expectedItem = createTgtItem("CDNAClone", "0_11", "");
        expectedItem.addAttribute(new Attribute("identifier","LD04815"));
        expectedItem.addCollection(new ReferenceList("synonyms", new ArrayList(Arrays.asList(new Object[]{"2_15", "2_16", "2_17"}))));

        Item expectedItem1 = createTgtItem("Gene", "2_14", "");
        expectedItem1.addAttribute(new Attribute("organismDbId", "FBgn0010173"));


        Item expectedItem2 = createTgtItem("Synonym", "2_15", "");
        expectedItem2.addAttribute(new Attribute("type", "accession"));
        expectedItem2.addAttribute(new Attribute("value", "AY069331"));
        expectedItem2.addReference(new Reference("source", "-1_3"));
        expectedItem2.addReference(new Reference("subject", "0_11"));

        Item expectedItem3 = createTgtItem("Synonym", "2_16", "");
        expectedItem3.addAttribute(new Attribute("type", "accession"));
        expectedItem3.addAttribute(new Attribute("value", "AA201663"));
        expectedItem3.addReference(new Reference("source",  "-1_3"));
        expectedItem3.addReference(new Reference("subject", "0_11"));

        Item expectedItem4 = createTgtItem("Synonym", "2_17", "");
        expectedItem4.addAttribute(new Attribute("type", "accession"));
        expectedItem4.addAttribute(new Attribute("value", "AW941561"));
        expectedItem4.addReference(new Reference("source",  "-1_3"));
        expectedItem4.addReference(new Reference("subject", "0_11"));

        Item expectedItem5 = createTgtItem("OntologyTerm", "1_13", "");
        expectedItem5.addAttribute(new Attribute("name", "cDNA_clone"));

        Item expectedItem7 = createTgtItem("Database",  "-1_3", "");
        expectedItem7.addAttribute(new Attribute("title", "embl"));

        Item expectedItem10 = createTgtItem("MicroArrayResult", "58_821", "");
        expectedItem10.addReference(new Reference("analysis","-1_1"));
        expectedItem10.addReference(new Reference("material","0_11"));
        expectedItem10.addReference(new Reference("reporter","12_50"));
        expectedItem10.addCollection(new ReferenceList("genes",
                                new ArrayList(Arrays.asList(new Object[]{"2_14"}))));

        Item expectedItem11 = createTgtItem("MicroArrayResult", "58_823", "");
        expectedItem11.addReference(new Reference("analysis","-1_1"));
        expectedItem11.addReference(new Reference("material","0_11"));
        expectedItem11.addReference(new Reference("reporter","12_50"));
        expectedItem11.addCollection(new ReferenceList("genes",
                                new ArrayList(Arrays.asList(new Object[]{"2_14"}))));

        Item expectedItem12 = createTgtItem("MicroArrayResult", "58_828", "");
        expectedItem12.addReference(new Reference("analysis","-1_1"));
        expectedItem12.addReference(new Reference("material","0_11"));
        expectedItem12.addReference(new Reference("reporter","12_50"));
        expectedItem12.addCollection(new ReferenceList("genes",
                                new ArrayList(Arrays.asList(new Object[]{"2_14"}))));

        Item expectedItem13 = createTgtItem("Reporter", "12_50", "");
        expectedItem13.addReference(new Reference("material", "0_11"));
        expectedItem13.addReference(new Reference("location", "7_46"));

        Item expectedItem14 = createTgtItem("MicroArraySlideDesign", "20_69", "");

        Item expectedItem15 = createTgtItem("ReporterLocation", "7_46", "");
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


    public void testGetPrefetchDescriptors() throws Exception {
        MageDataTranslator.getPrefetchDescriptors();

    }

    public void XtestBioEntity2MAER() throws Exception {

        Item srcItem= createSrcItem("Reporter", "12_45", "");
        srcItem.addCollection(new ReferenceList("featureReporterMaps",
                   new ArrayList(Arrays.asList(new Object[]{"7_41"}))));
        srcItem.addCollection(new ReferenceList("immobilizedCharacteristics",
                   new ArrayList(Arrays.asList(new Object[]{"0_3"}))));
        Item srcItem1= createSrcItem("FeatureReporterMap", "7_41", "");
        srcItem1.addCollection(new ReferenceList("featureInformationSources",
                   new ArrayList(Arrays.asList(new Object[]{"8_42"}))));

        Item srcItem2= createSrcItem("FeatureInformation", "8_42", "");
        srcItem2.addReference(new Reference("feature", "9_43"));

        Item srcItem3= createSrcItem("BioSequence", "0_3", "");
        srcItem3.addReference(new Reference("type", "1_5"));

        Item srcItem4 =createSrcItem("OntologyEntry", "1_5", "");
        srcItem4.addAttribute(new Attribute("value", "cDNA_clone"));

        Set srcItems = new HashSet(Arrays.asList(new Object[]
                       {srcItem, srcItem1, srcItem2, srcItem3, srcItem4}));
        Map itemMap = writeItems(srcItems);
        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(itemMap),
                                                               mapping, srcModel, getTargetModel(tgtNs));

        Item tgtItem = createTgtItem("CDNAClone", "0_3", "");

        Map expected = new HashMap();
//         expected.put("0_3", "9_43");
//         translator.setBioEntityMap(srcItem,tgtItem);
//         assertEquals(expected, translator.bioEntity2Feature);

    }

    public void XtestBioEntity2Identifier() throws Exception {

        Item srcItem= createSrcItem("Reporter", "12_45", "");
        srcItem.addAttribute(new Attribute("name", "LD14383"));
        srcItem.addCollection(new ReferenceList("immobilizedCharacteristics",
                   new ArrayList(Arrays.asList(new Object[]{"0_3"}))));
        srcItem.addCollection(new ReferenceList("featureReporterMaps",
                   new ArrayList(Arrays.asList(new Object[]{"7_41"}))));

        Item srcItem1= createSrcItem("FeatureReporterMap", "7_41", "");
        srcItem1.addCollection(new ReferenceList("featureInformationSources",
                    new ArrayList(Arrays.asList(new Object[]{"8_42"}))));

        Item srcItem2= createSrcItem("FeatureInformation", "8_42", "");
        srcItem2.addReference(new Reference("feature","11_44"));

        Item srcItem3= createSrcItem("BioSequence", "0_3", "");
        srcItem3.addReference(new Reference("type", "1_5"));

        Item srcItem4 =createSrcItem("OntologyEntry", "1_5", "");
        srcItem4.addAttribute(new Attribute("value", "CDNA_clone"));

        Set srcItems = new HashSet(Arrays.asList(new Object[] {srcItem, srcItem1,srcItem2, srcItem3, srcItem4}));
        Map itemMap = writeItems(srcItems);
        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(itemMap),
                                                               mapping, srcModel, getTargetModel(tgtNs));
        Item tgtItem = createSrcItem("CDNAClone", "0_3", "");

        Map expected = new HashMap();
//         expected.put("0_3", "LD14383");
//         translator.setBioEntityMap(srcItem,tgtItem);
//         assertEquals(expected, translator.bioEntity2IdentifierMap);

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
        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(itemMap),
                                                               mapping, srcModel, getTargetModel(tgtNs));

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        return tgtIw.getItems();

    }

    protected String getModelName() {
        return "genomic";
    }

    protected String getSrcModelName() {
        return "mage";
    }

    private Item createSrcItem(String className, String itemId, String implementation){
        return srcItemFactory.makeItem(itemId, srcNs + className, implementation);
   }

    private Item createTgtItem(String className, String itemId, String implementation){
        return tgtItemFactory.makeItem(itemId, tgtNs + className, implementation);
   }
}
