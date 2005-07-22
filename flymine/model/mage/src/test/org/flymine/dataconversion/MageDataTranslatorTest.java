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
    private File file;

    public MageDataTranslatorTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        srcItemFactory = new ItemFactory(Model.getInstanceByName("mage"));
        tgtItemFactory = new ItemFactory(Model.getInstanceByName("genomic"));

        String ENDL = System.getProperty("line.separator");
        file = new File("build/model/mage/mage_config.properties");
        String propertiesFile="P10005.experimentName=Experiment 1" + ENDL
            + "P10005.primaryCharacteristic=colour" + ENDL;

        FileWriter fw = new FileWriter(file);
        fw.write(propertiesFile);
        fw.close();
    }


    public void tearDown() throws Exception {
        super.tearDown();
        file.delete();
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


    public void testMicroArrayExperiment()throws Exception {
        Item srcItem1 = createSrcItem("Experiment", "61_748", "");
        srcItem1.setAttribute("name", "P10005");
        srcItem1.addCollection(new ReferenceList("descriptions", new ArrayList(Arrays.asList(new Object[]{"12_749", "12_750"}))));
        srcItem1.addCollection(new ReferenceList("bioAssays", new ArrayList(Arrays.asList(new Object[]{"0_1", "1_1"}))));

        Item srcItem2 = createSrcItem("Description", "12_749", "");
        srcItem2.addAttribute(new Attribute("text", "experiment description"));

        Item srcItem3 = createSrcItem("Description", "12_750", "");
        srcItem3.addCollection(new ReferenceList("bibliographicReferences", new ArrayList(Arrays.asList(new Object[]{"62_751"}))));

        Item srcItem4 = createSrcItem("MeasuredBioAssay", "0_1", "");

        Item srcItem5 = createSrcItem("DerivedBioAssay", "1_1", "");


        Set src = new HashSet(Arrays.asList(new Object[]{srcItem1, srcItem2, srcItem3, srcItem4, srcItem5}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs));

        Item expectedItem =createTgtItem("MicroArrayExperiment", "-1_1", "");
        expectedItem.addAttribute(new Attribute("name", "Experiment 1"));
        expectedItem.addAttribute(new Attribute("description", "experiment description"));
        // not linking to broken publications in mage
        //expectedItem.addReference(new Reference("publication", "62_751"));
        HashSet expected=new HashSet(Arrays.asList(new Object[]{expectedItem}));

        assertEquals(expected, translator.translateItem(srcItem1));

        Map expAssayToExpName = new HashMap();
        expAssayToExpName.put("1_1", "P10005");
        assertEquals(expAssayToExpName, translator.assayToExpName);
    }


    public void testTranslateMicroArrayAssay() throws Exception{
        Item srcItem1 = createSrcItem("DerivedBioAssay", "57_709", "");
        srcItem1.addCollection(new ReferenceList("derivedBioAssayData", new ArrayList(Arrays.asList(new Object[]{"58_710"}))));
        srcItem1.addCollection(new ReferenceList("derivedBioAssayMap", new ArrayList(Arrays.asList(new Object[]{"1_1"}))));

        Item srcItem2 = createSrcItem("DerivedBioAssayData", "58_710", "");
        srcItem2.addReference(new Reference("bioDataValues", "58_739"));

        Item srcItem3= createSrcItem("BioDataTuples", "58_739", "");
        srcItem3.addCollection(new ReferenceList("bioAssayTupleData", new ArrayList(Arrays.asList(new Object[]{"58_740", "58_744"}))));

        Item srcItem4 = createSrcItem("Experiment", "61_748", "");
        srcItem4.addCollection(new ReferenceList("bioAssays", new ArrayList(Arrays.asList(new Object[]{"57_709"}))));

        Item srcItem5 = createSrcItem("BioAssayMap", "1_1", "");
        srcItem5.addCollection(new ReferenceList("sourceBioAssays", new ArrayList(Arrays.asList(new Object[]{"2_1"}))));

        Item srcItem6 = createSrcItem("MeasuredBioAssay", "2_1", "");

        Set src = new HashSet(Arrays.asList(new Object[] {srcItem1, srcItem2, srcItem3, srcItem4, srcItem5, srcItem6}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs));

        Item expItem1 = createTgtItem("MicroArrayAssay", "57_709", "");
        expItem1.setReference("experiment", "-1_1");

        Item expItem2 = createTgtItem("MicroArrayExperiment", "-1_1", "");

        HashSet expected=new HashSet(Arrays.asList(new Object[]{expItem1, expItem2}));

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        assertEquals(expected, tgtIw.getItems());

        Set expAssays = new HashSet();
        expAssays.add(expItem1);
        assertEquals(expAssays, translator.assays);

        // mage:BioAssayDatum to genomic:MicroArrayAssay
        Map expBioAssayDataToAssay = new HashMap();
        expBioAssayDataToAssay.put("58_710", "57_709");
        assertEquals(expBioAssayDataToAssay, translator.bioAssayDataToAssay);


        Map expMeasuredBioAssayToMicroArrayAssay = new HashMap();
        expMeasuredBioAssayToMicroArrayAssay.put("2_1", "57_709");
        assertEquals(expMeasuredBioAssayToMicroArrayAssay, translator.measuredBioAssayToMicroArrayAssay);
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
        assertEquals(expExtractToAssay, translator.labeledExtractToMeasuredBioAssay);
    }


    public void testTranslateTreatment() throws Exception {
        Item srcItem1 = createSrcItem("Treatment", "0_1", "");
        srcItem1.setReference("action","1_1");
        srcItem1.addCollection(new ReferenceList("protocolApplications",
                                                 new ArrayList(Collections.singleton("2_1"))));

        Item srcItem2 = createSrcItem("OntologyEntry", "1_1", "");
        srcItem2.setAttribute("category", "Action");
        srcItem2.setAttribute("value", "labeling");

        Item srcItem3 = createSrcItem("ProtocolApplication", "2_1", "");
        srcItem3.setReference("protocol", "3_1");
        srcItem3.addCollection(new ReferenceList("parameterValues",
                                                 new ArrayList(Collections.singleton("4_1"))));

        Item srcItem4 = createSrcItem("Protocol", "3_1", "");
        srcItem4.setAttribute("name", "protocol 1");
        srcItem4.setAttribute("text", "protocol description");

        Item srcItem5 = createSrcItem("ParameterValue", "4_1", "");
        srcItem5.setAttribute("value", "0.1");
        srcItem5.setReference("parameterType", "5_1");

        Item srcItem6 = createSrcItem("Parameter", "5_1", "");
        srcItem6.setAttribute("name", "volume of stuff");
        srcItem6.setReference("defaultValue", "6_1");

        Item srcItem7 = createSrcItem("Measurement", "6_1", "");
        srcItem7.setReference("unit", "7_1");

        Item srcItem8 = createSrcItem("VolumeUnit", "7_1", "");
        srcItem8.setAttribute("unitNameCV", "ml");


        Set src = new HashSet(Arrays.asList(new Object[] {srcItem1, srcItem2, srcItem3, srcItem4, srcItem5, srcItem6, srcItem7, srcItem8}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs));

        Item expItem1 = createTgtItem("Treatment", "0_1", "");
        expItem1.setAttribute("action", "labeling");
        expItem1.setReference("protocol", "3_1");

        Item expItem2 = createTgtItem("Protocol", "3_1", "");
        expItem2.setAttribute("name", "protocol 1");
        expItem2.setAttribute("description", "protocol description");

        Item expItem3 = createTgtItem("TreatmentParameter", "-1_1", "");
        expItem3.setReference("treatment", "0_1");
        expItem3.setAttribute("value", "0.1");
        expItem3.setAttribute("type", "volume of stuff");
        //expItem3.setAttribute("units", "ml");

        HashSet expected = new HashSet(Arrays.asList(new Object[] {expItem1, expItem2, expItem3}));

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

    public void testTranslateReporterControl() throws Exception {
        Item srcItem1 = createSrcItem("Reporter", "0_1", "");
        srcItem1.setReference("controlType", "1_1");

        Item srcItem2 = createSrcItem("OntologyEntry", "1_1", "");
        srcItem2.setAttribute("category", "ControlType");
        srcItem2.setAttribute("value", "control_label");

        Set src = new HashSet(Arrays.asList(new Object[] {srcItem1, srcItem2}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs));

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        Item expItem = createTgtItem("Reporter", "0_1", "");
        expItem.setAttribute("isControl", "true");
        expItem.setAttribute("controlType", "control_label");
        Set expected = new HashSet(Collections.singleton(expItem));

        translator.translate(tgtIw);
        assertEquals(expected, tgtIw.getItems());

        Set expControls = new HashSet(Collections.singleton("0_1"));
        assertEquals(expControls, translator.controls);
    }


    public void testTranslateReporterMaterial() throws Exception {
        Item srcItem1 = createSrcItem("Reporter", "0_1", "");
        srcItem1.setAttribute("name", "GH1234");
        srcItem1.addCollection(new ReferenceList("immobilizedCharacteristics",
                                                 new ArrayList(Arrays.asList(new Object[] {"1_1"}))));

        Item srcItem2 = createSrcItem("BioSequence", "1_1", "");
        srcItem2.setReference("type", "2_1");

        Item srcItem3 = createSrcItem("OntologyEntry", "2_1", "");
        srcItem3.setAttribute("category", "BioSequenceType");
        srcItem3.setAttribute("value", "cDNA_clone");


        Set src = new HashSet(Arrays.asList(new Object[] {srcItem1, srcItem2, srcItem3}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs));

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        Item expItem1 = createTgtItem("Reporter", "0_1", "");
        expItem1.setAttribute("isControl", "false");
        expItem1.setReference("material", "-1_1");

        Item expItem2 = createTgtItem("CDNAClone", "-1_1", "");
        expItem2.setAttribute("identifier", "GH1234");

        Set expected = new HashSet(Arrays.asList(new Object[] {expItem1, expItem2}));

        assertEquals(expected, tgtIw.getItems());

        Map expReporterToMaterial = new HashMap();
        expReporterToMaterial.put("0_1", "-1_1");
        assertEquals(expReporterToMaterial, translator.reporterToMaterial);

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
        // can't use ItemFactory as bioAssayData reference not in model
        Item srcItem1 = new Item("58_762", srcNs + "BioAssayDatum", "");
        srcItem1.setAttribute("value", "1.234");
        srcItem1.setReference("quantitationType","40_620");
        srcItem1.setReference("designElement","3_1");
        srcItem1.setReference("bioAssayData","4_1");

        Item srcItem2= createSrcItem("SpecializedQuantitationType", "40_620", "");
        srcItem2.setAttribute("name", "Log Ratio");
        srcItem2.setReference("scale", "1_611");

        Item srcItem3= createSrcItem("OntologyEntry", "1_611", "");
        srcItem3.setAttribute("value", "log");

        Item srcItem4= createSrcItem("Feature", "3_1", "");

        Item srcItem5 = new Item("58_763", srcNs + "BioAssayDatum", "");
        srcItem5.setAttribute("value", "Error");

        Set src = new HashSet(Arrays.asList(new Object[]{srcItem1, srcItem2, srcItem3, srcItem4, srcItem5}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs));

        Item expItem1 = createTgtItem("MicroArrayResult", "58_762", "");
        expItem1.setAttribute("value","1.234");
        expItem1.setAttribute("scale","log");
        expItem1.setAttribute("type","(Normalised) Log Ratio");

        HashSet expected = new HashSet(Arrays.asList(new Object[] {expItem1}));

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        assertEquals(expected, tgtIw.getItems());

        // map from genomic:MicroArrayResult to mage:Feature
        Map expResultToFeature = new HashMap();
        expResultToFeature.put("58_762", "3_1");
        assertEquals(expResultToFeature, translator.resultToFeature);

        // map from genomic:MicroArrayResult to mage:BioAssayDataData
        Map expResultToBioAssayData = new HashMap();
        expResultToBioAssayData.put("58_762", "4_1");
        assertEquals(expResultToBioAssayData, translator.resultToBioAssayData);
    }


    public void testProcessMicroArrayResults() throws Exception {
        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(new HashMap()),
                                                               mapping, srcModel, getTargetModel(tgtNs));

        Item result = createTgtItem("MicroArrayResult", "0_1", "");
        translator.microArrayResults.add(result);

        translator.resultToBioAssayData.put("0_1", "1_1");
        translator.bioAssayDataToAssay.put("1_1", "5_1");
        translator.assayToSamples.put("5_1", new ArrayList(Collections.singleton("6_1")));
        translator.controls.add("3_1");

        translator.resultToFeature.put("0_1", "2_1");
        translator.featureToReporter.put("2_1", "3_1");
        translator.assayToExperiment.put("5_1", "6_1");
        translator.reporterToMaterial.put("3_1", "7_1");

        Item expResult = createTgtItem("MicroArrayResult", "0_1", "");
        expResult.setAttribute("isControl", "true");
        expResult.setReference("assay", "5_1");
        expResult.setReference("reporter", "3_1");
        expResult.setReference("experiment", "6_1");
        expResult.setReference("material", "7_1");
        expResult.addCollection(new ReferenceList("samples", new ArrayList(Collections.singleton("6_1"))));
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
        translator.labeledExtractToMeasuredBioAssay.put("2_1", "3_1");
        translator.measuredBioAssayToMicroArrayAssay.put("3_1", "4_1");
        translator.assayToExpName.put("4_1", "P10005");
        HashMap charMap = new HashMap();
        charMap.put("colour", "pink");
        translator.sampleToChars.put("0_1", charMap);

        Item expSample = createTgtItem("Sample", "0_1", "");
        expSample.addCollection(new ReferenceList("treatments", new ArrayList(Arrays.asList(new Object[] {"1_1", "1_2"}))));
        expSample.setReference("assay", "4_1");
        expSample.setAttribute("primaryCharacteristicType", "colour");
        expSample.setAttribute("primaryCharacteristic", "pink");
        Set exp = new HashSet(Collections.singleton(expSample));

        translator.processSamples();
        assertEquals(exp, translator.samples);
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

    // just make sure no exception thrown
    public void testGetPrefetchDescriptors() throws Exception {
        MageDataTranslator.getPrefetchDescriptors();

    }

    public void testBioEntity2MAER() throws Exception {

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
