package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.dataconversion.MockItemReader;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.Reference;

/**
 * Test for translating MAGE data in fulldata Item format conforming to a source OWL definition
 * to fulldata Item format conforming to InterMine OWL definition.
 *
 * @author Wenyan Ji
 * @author Richard Smith
 */

public class MageDataTranslatorTest extends DataTranslatorTestCase {
    private String tgtNs = "http://www.flymine.org/model/genomic#";
    private String srcNs = "http://www.intermine.org/model/mage#";
    private ItemFactory srcItemFactory;
    private ItemFactory tgtItemFactory;
    //private File file;

    public MageDataTranslatorTest(String arg) {
        super(arg, "osw.bio-fulldata-test");
    }

    public void setUp() throws Exception {
        super.setUp();
        srcItemFactory = new ItemFactory(Model.getInstanceByName("mage"));
        tgtItemFactory = new ItemFactory(Model.getInstanceByName("genomic"));

        /*String ENDL = System.getProperty("line.separator");
        file = new File("build/model/genomic/mage_config.properties");
        String propertiesFile="P10005.experimentName=Experiment 1" + ENDL
            + "P10005.primaryCharacteristic=colour" + ENDL
            + "P10005.materialIdType=image" + ENDL;

        FileWriter fw = new FileWriter(file);
        fw.write(propertiesFile);
        fw.close();*/
    }


    public void tearDown() throws Exception {
        super.tearDown();
        //file.delete();
    }


   //   public void XtestTranslate() throws Exception {
//          Collection srcItems = getSrcItems();
//          //FileWriter writerSrc = new FileWriter(new File("src_items.xml"));
//          //writerSrc.write(FullRenderer.render(srcItems));
//          //writerSrc.close();


//          Map itemMap = writeItems(srcItems);
//          DataTranslator translator = new MageDataTranslator(new MockItemReader(itemMap),
//                                                             mapping, srcModel, getTargetModel(tgtNs));

//          MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
//          translator.translate(tgtIw);

//          FileWriter writer = new FileWriter(new File("exptmp"));
//          writer.write(FullRenderer.render(tgtIw.getItems()));
//          writer.close();

//          //assertEquals(new HashSet(expectedItems), tgtIw.getItems());

//      }

    public void testMicroArrayExperiment()throws Exception {
        Item srcItem1 = createSrcItem("Experiment", "61_748", "");
        srcItem1.setAttribute("identifier", "E-FLYC-1");
        srcItem1.setCollection("descriptions", new ArrayList(Arrays.asList(new Object[]{"12_749", "12_750"})));
        srcItem1.setCollection("bioAssays", new ArrayList(Arrays.asList(new Object[]{"0_1", "1_1"})));

        Item srcItem2 = createSrcItem("Description", "12_749", "");
        srcItem2.addAttribute(new Attribute("text", "experiment description"));

        Item srcItem3 = createSrcItem("Description", "12_750", "");
        srcItem3.setCollection("bibliographicReferences", new ArrayList(Arrays.asList(new Object[]{"62_751"})));

        Item srcItem4 = createSrcItem("MeasuredBioAssay", "0_1", "");

        Item srcItem5 = createSrcItem("DerivedBioAssay", "1_1", "");


        Set src = new HashSet(Arrays.asList(new Object[]{srcItem1, srcItem2, srcItem3, srcItem4, srcItem5}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs), 
                                                               "mage_config_test.properties");
        translator.propertiesFileName = "mage_config_test.properties";
        Item expectedItem = createTgtItem("MicroArrayExperiment", "-1_1", "");
        expectedItem.setAttribute("identifier", "E-FLYC-1");
        expectedItem.setAttribute("name", "Experiment 1");
        expectedItem.setAttribute("description", "experiment description");
        expectedItem.setReference("publication", "-1_2");

        Item expectedDataSet = createTgtItem("DataSet", "-1_3", "");
        expectedDataSet.setAttribute("title", "E-FLYC-1");
        expectedDataSet.setAttribute("description", "Experiment 1");
        expectedDataSet.setReference("dataSource", "-1_4");

        HashSet expected=new HashSet(Arrays.asList(new Object[]{expectedItem, expectedDataSet}));

        assertEquals(expected, translator.translateItem(srcItem1));

        Map expAssayToExpName = new HashMap();
        expAssayToExpName.put("0_1", "E-FLYC-1");
        assertEquals(expAssayToExpName, translator.assayToExpName);

        Map expExptToDataSet = new HashMap();
        expExptToDataSet.put("-1_1", "-1_3");
        assertEquals(expExptToDataSet, translator.exptToDataSet);
    }


    public void testTranslateMicroArrayAssay() throws Exception{
        Item srcItem1 = createSrcItem("DerivedBioAssay", "57_709", "");
        srcItem1.setCollection("derivedBioAssayData", new ArrayList(Arrays.asList(new Object[]{"58_710"})));
        srcItem1.setCollection("derivedBioAssayMap", new ArrayList(Arrays.asList(new Object[]{"1_1"})));
        srcItem1.setAttribute("name", "Day0(3) vs Day10(3)");

        Item srcItem2 = createSrcItem("DerivedBioAssayData", "58_710", "");
        srcItem2.addReference(new Reference("bioDataValues", "58_739"));

        Item srcItem3= createSrcItem("BioDataTuples", "58_739", "");
        srcItem3.setCollection("bioAssayTupleData", new ArrayList(Arrays.asList(new Object[]{"58_740", "58_744"})));

        Item srcItem4 = createSrcItem("Experiment", "61_748", "");
        srcItem4.setCollection("bioAssays", new ArrayList(Arrays.asList(new Object[] {"57_709", "2_1", "2_2"})));

        Item srcItem5 = createSrcItem("BioAssayMap", "1_1", "");
        srcItem5.setCollection("sourceBioAssays", new ArrayList(Arrays.asList(new Object[]{"2_1", "2_2"})));

        Item srcItem6 = createSrcItem("MeasuredBioAssay", "2_1", "");
        srcItem6.setAttribute("identifier", "assay-A");
        srcItem6.setAttribute("name", "Day0(3)");
        Item srcItem61 = createSrcItem("MeasuredBioAssay", "2_2", "");
        srcItem61.setAttribute("identifier", "assay-B");
        srcItem61.setAttribute("name", "Day10(3)");

        Set src = new HashSet(Arrays.asList(new Object[] {srcItem1,srcItem2, srcItem3, srcItem4,
            srcItem5, srcItem6, srcItem61}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs),
                                                               "mage_config_test.properties");
        
        Item expItem1 = createTgtItem("MicroArrayAssay", "2_1", "");
        expItem1.setReference("experiment", "-1_1");
        expItem1.setAttribute("name", "Day0(3)");
        expItem1.setAttribute("displayOrder", "0");
        Item expItem2 = createTgtItem("MicroArrayAssay", "2_2", "");
        expItem2.setAttribute("name", "Day10(3)");
        expItem2.setAttribute("displayOrder", "1");
        expItem2.setReference("experiment", "-1_1");
        Item expItem3 = createTgtItem("MicroArrayExperiment", "-1_1", "");
        Item expItem4 = createTgtItem("DataSet", "-1_2", "");
        expItem4.setReference("dataSource", "-1_3");
        Item expItem5 = createTgtItem("DataSource", "-1_3", "");
        expItem5.setAttribute("name", "ArrayExpress");

        HashSet expected=new HashSet(Arrays.asList(new Object[]{expItem1, expItem2, expItem3, expItem4, expItem5}));

         MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
         translator.translate(tgtIw);

        assertEquals(expected, tgtIw.getItems());

        Map expAssays = new HashMap();
        expAssays.put(expItem1.getIdentifier(), expItem1);
        expAssays.put(expItem2.getIdentifier(), expItem2);
        assertEquals(expAssays, translator.assays);

        List mbaList = new ArrayList();
        mbaList = (List) translator.processDBA2MBA("57_709");
        List expectedList = new ArrayList(Arrays.asList(new Object[]{"2_1", "2_2"}));
        assertEquals(expectedList, mbaList);
    }


    public void testMicroArrayAssayLabeledExtract() throws Exception{
        Item srcItem1 = createSrcItem("MeasuredBioAssay", "0_1", "");
        srcItem1.setReference("featureExtraction", "1_1");

        Item srcItem2 = createSrcItem("FeatureExtraction", "1_1", "");
        srcItem2.setReference("physicalBioAssaySource", "2_1");

        Item srcItem3 = createSrcItem("PhysicalBioAssay", "2_1", "");
        srcItem3.setReference("bioAssayCreation", "3_1");

        Item srcItem4 = createSrcItem("Hybridization", "3_1", "");
        srcItem4.setCollection("sourceBioMaterialMeasurements",
                               new ArrayList(Arrays.asList(new Object[] {"4_1", "4_2"})));

        Item srcItem5 = createSrcItem("BioMaterialMeasurement", "4_1", "");
        srcItem5.setReference("bioMaterial", "5_1");

        Item srcItem6 = createSrcItem("BioMaterialMeasurement", "4_2", "");
        srcItem6.setReference("bioMaterial", "5_2");

        Item srcItem7 = createSrcItem("LabeledExtract", "5_1", "");

        Item srcItem8 = createSrcItem("LabeledExtract", "5_2", "");

        Set srcItems = new HashSet(Arrays.asList(new Object[] {srcItem1, srcItem2, srcItem3, srcItem4, srcItem5, srcItem6, srcItem7}));
        Map srcMap = writeItems(srcItems);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs),
                                                               "mage_config_test.properties");
        translator.translateItem(srcItem1);

        // genomic:MicroArrayAssay to list of mage:LabeledExtract identifiers
        Map expExtractToAssay = new HashMap();
        expExtractToAssay.put("5_1", Collections.singleton("0_1"));
        expExtractToAssay.put("5_2", Collections.singleton("0_1"));
        assertEquals(expExtractToAssay, translator.labeledExtractToMicroArrayAssays);
    }


    public void testTranslateTreatment() throws Exception {
        Item srcItem1 = createSrcItem("Treatment", "0_1", "");
        srcItem1.setReference("action","1_1");
        srcItem1.setCollection("protocolApplications", new ArrayList(Collections.singleton("2_1")));

        Item srcItem2 = createSrcItem("OntologyEntry", "1_1", "");
        srcItem2.setAttribute("category", "Action");
        srcItem2.setAttribute("value", "labeling");

        Item srcItem3 = createSrcItem("ProtocolApplication", "2_1", "");
        srcItem3.setReference("protocol", "3_1");
        srcItem3.setCollection("parameterValues", new ArrayList(Collections.singleton("4_1")));

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
                                                               mapping, srcModel, getTargetModel(tgtNs),
                                                               "mage_config_test.properties");
        Item expItem1 = createTgtItem("Treatment", "0_1", "");
        expItem1.setAttribute("action", "labeling");
        expItem1.setCollection("protocols", new ArrayList(Collections.singleton("3_1")));

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

    public void testTranslateTreatmentSampleLabels() throws Exception {
        Item srcItem0 = createSrcItem("LabeledExtract", "7_1", "");
        srcItem0.setCollection("treatments", new ArrayList(Arrays.asList(new Object[] {"0_1"})));

        Item srcItem1 = createSrcItem("Treatment", "0_1", "");
        srcItem1.setReference("action","1_1");
        srcItem1.setCollection("protocolApplications", new ArrayList(Collections.singleton("2_1")));
        srcItem1.setCollection("sourceBioMaterialMeasurements", new ArrayList(Arrays.asList(new Object[] {"6_1"})));

        Item srcItem2 = createSrcItem("OntologyEntry", "1_1", "");
        srcItem2.setAttribute("category", "Action");
        srcItem2.setAttribute("value", "labeling");

        Item srcItem3 = createSrcItem("ProtocolApplication", "2_1", "");
        srcItem3.setCollection("parameterValues", new ArrayList(Collections.singleton("4_1")));

        Item srcItem4 = createSrcItem("ParameterValue", "4_1", "");
        srcItem4.setAttribute("value", "cy3");
        srcItem4.setReference("parameterType", "5_1");

        Item srcItem5 = createSrcItem("Parameter", "5_1", "");
        srcItem5.setAttribute("name", "Label used");

        Item srcItem6 = createSrcItem("BioMaterialMeasurement", "6_1", "");
        srcItem6.setReference("bioMaterial", "7_2");

        Item srcItem7 = createSrcItem("BioSample", "7_2", "");
        srcItem7.setCollection("treatments", new ArrayList(Arrays.asList(new Object[] {"0_2"})));

        Item srcItem8 = createSrcItem("Treatment", "0_2", "");
        srcItem8.setCollection("sourceBioMaterialMeasurements", new ArrayList(Arrays.asList(new Object[] {"6_2"})));

        Item srcItem9 = createSrcItem("BioMaterialMeasurement", "6_2", "");
        srcItem9.setReference("bioMaterial", "8_1");

        Item srcItem10 = createSrcItem("BioSource", "8_1", "");

        Set src = new HashSet(Arrays.asList(new Object[] {srcItem0, srcItem1, srcItem2, srcItem3, srcItem4, srcItem5, srcItem6, srcItem7, srcItem8, srcItem9, srcItem10}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs),
                                                               "mage_config_test.properties");
        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        Map expSampleToLabel = new HashMap();
        expSampleToLabel.put("8_1", "cy3");
        assertEquals(expSampleToLabel, translator.sampleToLabel);
    }


    public void testTranslateReporter() throws Exception {
        Item srcItem1 = createSrcItem("Reporter", "0_1", "");
        srcItem1.setCollection("featureReporterMaps", new ArrayList(Collections.singleton("1_1")));

        Item srcItem2 = createSrcItem("FeatureReporterMap", "1_1", "");
        srcItem2.setCollection("featureInformationSources", new ArrayList(Collections.singleton("2_1")));

        Item srcItem3 = createSrcItem("FeatureInformation", "2_1", "");
        srcItem3.setReference("feature", "3_1");

        Set src = new HashSet(Arrays.asList(new Object[] {srcItem1, srcItem2, srcItem3}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs),
                                                               "mage_config_test.properties");
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
                                                               mapping, srcModel, getTargetModel(tgtNs),
                                                               "mage_config_test.properties");
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
        srcItem1.setCollection("immobilizedCharacteristics",
                    new ArrayList(Arrays.asList(new Object[] {"1_1"})));

        Item srcItem2 = createSrcItem("BioSequence", "1_1", "");
        srcItem2.setReference("type", "2_1");

        Item srcItem3 = createSrcItem("OntologyEntry", "2_1", "");
        srcItem3.setAttribute("category", "BioSequenceType");
        srcItem3.setAttribute("value", "cDNA_clone");


        Set src = new HashSet(Arrays.asList(new Object[] {srcItem1, srcItem2, srcItem3}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs),
                                                               "mage_config_test.properties");
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


    public void testTranslateReporterBioSequence() throws Exception {
        Item srcItem1 = createSrcItem("Reporter", "0_1", "");
        srcItem1.setAttribute("name", "GH1234");
        srcItem1.setCollection("immobilizedCharacteristics",
                               new ArrayList(Arrays.asList(new Object[] {"1_1"})));

        Item srcItem2 = createSrcItem("BioSequence", "1_1", "");
        srcItem2.setReference("type", "2_1");
        srcItem2.setCollection("sequenceDatabases", new ArrayList(Arrays.asList(new Object[] {"3_1"})));

        Item srcItem3 = createSrcItem("OntologyEntry", "2_1", "");
        srcItem3.setAttribute("category", "BioSequenceType");
        srcItem3.setAttribute("value", "cDNA_clone");

        Item srcItem4 = createSrcItem("DatabaseEntry", "3_1", "");
        srcItem4.setAttribute("accession", "1234");
        srcItem4.setReference("database", "4_1");

        Item srcItem5 = createSrcItem("Database", "4_1", "");
        srcItem5.setAttribute("name", "image");

        Set src = new HashSet(Arrays.asList(new Object[] {srcItem1, srcItem2, srcItem3, srcItem4, srcItem5}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs),
                                                               "mage_config_test.properties");

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        Map expCloneIds = new HashMap();
        Map typeMap = new HashMap();
        typeMap.put("image", "1234");
        expCloneIds.put("-1_1", typeMap);
        assertEquals(expCloneIds, translator.cloneIds);
    }


    public void testSearchTreatments() throws Exception {

        Item srcItem1 = createSrcItem("LabeledExtract", "0_1", "");
        srcItem1.setCollection("treatments", new ArrayList(Arrays.asList(new Object[] {"1_1"})));

        Item srcItem2 = createSrcItem("Treatment", "1_1", "");
        srcItem2.setCollection("sourceBioMaterialMeasurements", new ArrayList(Arrays.asList(new Object[] {"2_1"})));

        Item srcItem3 = createSrcItem("BioMaterialMeasurement", "2_1", "");
        srcItem3.setReference("bioMaterial", "0_2");

        Item srcItem4 = createSrcItem("LabeledExtract", "0_2", "");
        srcItem4.setCollection("treatments", new ArrayList(Arrays.asList(new Object[] {"1_2"})));

        Item srcItem5 = createSrcItem("Treatment", "1_2", "");
        srcItem5.setCollection("sourceBioMaterialMeasurements", new ArrayList(Arrays.asList(new Object[] {"2_2"})));

        Item srcItem6 = createSrcItem("BioMaterialMeasurement", "2_2", "");
        srcItem6.setReference("bioMaterial", "3_1");

        Item srcItem7 = createSrcItem("BioSource", "3_1", "");

        Set srcItems = new HashSet(Arrays.asList(new Object[] {srcItem1, srcItem2, srcItem3, srcItem4, srcItem5, srcItem6, srcItem7}));
        Map srcMap = writeItems(srcItems);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs),
                                                               "mage_config_test.properties");
        // call with top level LabeledExtract and empty list
        List treatments = new ArrayList();
        translator.searchTreatments(srcItem1, treatments, false);

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
        srcItem1.setAttribute("value", "-1.234");
        srcItem1.setReference("quantitationType","40_620");
        srcItem1.setReference("feature","3_1");
        srcItem1.setReference("bioAssay","4_1");

        Item srcItem2= createSrcItem("SpecializedQuantitationType", "40_620", "");
        srcItem2.setAttribute("name", "Log Ratio");
        srcItem2.setReference("scale", "1_611");

        Item srcItem3= createSrcItem("OntologyEntry", "1_611", "");
        srcItem3.setAttribute("value", "log");

        Item srcItem4= createSrcItem("Feature", "3_1", "");
        Item srcItem6= createSrcItem("MeasuredBioAssay", "4_1","");

        Item srcItem5 = new Item("58_763", srcNs + "BioAssayDatum", "");
        srcItem5.setAttribute("value", "Error");

        Set src = new HashSet(Arrays.asList(new Object[]{srcItem1, srcItem2, srcItem3, srcItem4, srcItem6, srcItem5}));

        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs),
                                                               "mage_config_test.properties");
        translator.assayNs = "4_";

        Item expItem1 = createTgtItem("MicroArrayResult", "58_762", "");
        expItem1.setAttribute("value","-1.234");
        expItem1.setAttribute("scale","log");
        expItem1.setAttribute("type","Log Ratio");
        expItem1.setCollection("assays", new ArrayList(Collections.singleton("4_1")));
        Item expItem2 = createTgtItem("MicroArrayAssay", "4_1", "");

        HashSet expected = new HashSet(Arrays.asList(new Object[] {expItem1,expItem2}));

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        assertEquals(expected, tgtIw.getItems());

        // map from genomic:MicroArrayResult to mage:Feature

        Map expResultToFeature = new HashMap();
        expResultToFeature.put("58_762", "3_1");
        //assertEquals(expResultToFeature, translator.resultToFeature);
    }


    public void testProcessMicroArrayResults() throws Exception {
        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(new HashMap()),
                                                               mapping, srcModel, getTargetModel(tgtNs),
                                                               "mage_config_test.properties");

        MageDataTranslator.ResultHolder rh = translator.new ResultHolder(1);
        rh.measuredAssayId = 1;
        translator.resultNs = "0_";
        translator.assayNs = "5_";
        translator.microArrayResults.add(rh);

        translator.assayToSamples.put("5_1", new ArrayList(Collections.singleton("16_1")));
        translator.controls.add("3_1");

        translator.resultToFeature.put("0_1", "2_1");
        translator.featureToReporter.put("2_1", "3_1");
        translator.assayToExperiment.put("5_1", "6_1");
        translator.reporterToMaterial.put("3_1", "7_1");
        translator.exptToDataSet.put("6_1", "-1_1");
        translator.expIdNames.put("6_1", "E-FLYC-1");
        Map typeMap = new HashMap();
        typeMap.put("image", "5678");
        translator.cloneIds.put("7_1", typeMap);

        Item expResult = createTgtItem("MicroArrayResult", "0_1", "");
        expResult.setAttribute("isControl", "true");
        expResult.setCollection("assays", new ArrayList(Collections.singleton("5_1")));
        expResult.setCollection("reporters", new ArrayList(Collections.singleton("3_1")));
        expResult.setReference("analysis", "6_1");
        expResult.setReference("experiment", "6_1");
        expResult.setReference("source", "-1_1");
        expResult.setCollection("samples", new ArrayList(Collections.singleton("16_1")));

        assertEquals(expResult, translator.processMicroArrayResult(rh));

        Map expCloneToResults = new HashMap();
        expCloneToResults.put("7_1", new ArrayList(Collections.singleton("0_1")));
        assertEquals(expCloneToResults, translator.cloneToResults);
    }


    public void testProcessMicroArrayResultsMaterialIds() throws Exception {
        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(new HashMap()),
                                                               mapping, srcModel, getTargetModel(tgtNs),
                                                               "mage_config_test.properties");

        //Item result = createTgtItem("MicroArrayResult", "0_1", "");
        //result.setReference("assay", "5_1");
        MageDataTranslator.ResultHolder rh = translator.new ResultHolder(1);
        rh.measuredAssayId = 1;
        translator.resultNs = "0_";
        translator.assayNs = "5_";
        //translator.microArrayResults.add(result);
        translator.microArrayResults.add(rh);


        //translator.bioAssayDataToAssay.put("1_1", "5_1");
        translator.assayToSamples.put("5_1", new ArrayList(Collections.singleton("6_1")));
        translator.controls.add("3_1");

        translator.resultToFeature.put("0_1", "2_1");
        translator.featureToReporter.put("2_1", "3_1");
        translator.assayToExperiment.put("5_1", "6_1");
        translator.reporterToMaterial.put("3_1", "7_1");
        translator.exptToDataSet.put("6_1", "-1_1");
        translator.expIdNames.put("6_1", "E-FLYC-1");

        Item clone = createTgtItem("Clone", "7_1", "");
        clone.setAttribute("identifier", "1234");
        translator.clones.put("7_1", clone);
        Map typeMap = new HashMap();
        typeMap.put("image", "5678");
        translator.cloneIds.put("7_1", typeMap);

        Item expClone = createTgtItem("Clone", "7_1", "");
        expClone.setAttribute("identifier", "5678");
        Map expClones = new HashMap();
        expClones.put("7_1", expClone);

        translator.processMicroArrayResult(rh);
        assertEquals(expClones, translator.clones);
    }


    public void testProcessSamples() throws Exception {
        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(new HashMap()),
                                                               mapping, srcModel, getTargetModel(tgtNs),
                                                               "mage_config_test.properties");

        Item sample1 = createTgtItem("Sample", "0_1", "");
        translator.samplesById.put("0_1", sample1);

        Item sample2 = createTgtItem("Sample", "0_2", "");
        translator.samplesById.put("0_2", sample2);

        translator.sampleToTreatments.put("0_1", new ArrayList(Arrays.asList(new Object[] {"1_1", "1_2"})));

        translator.sampleToLabeledExtracts.put("0_1", new HashSet(Collections.singleton("2_1")));
        translator.labeledExtractToMicroArrayAssays.put("2_1", Collections.singleton("3_1"));
        translator.sampleToLabeledExtracts.put("0_2", new HashSet(Collections.singleton("2_2")));
        translator.labeledExtractToMicroArrayAssays.put("2_2", Collections.singleton("3_2"));
        translator.assayToExpName.put("3_1", "E-FLYC-1");
        translator.assayToExpName.put("3_2", "E-FLYC-1");
        HashMap charMap1 = new HashMap();
        charMap1.put("colour", "pink");
        translator.sampleToChars.put("0_1", charMap1);
        HashMap charMap2 = new HashMap();
        charMap2.put("size", "medium");
        translator.sampleToChars.put("0_2", charMap2);

        Item expSample1 = createTgtItem("Sample", "0_1", "");
        expSample1.setCollection("treatments", new ArrayList(Arrays.asList(new Object[] {"1_1", "1_2"})));
        expSample1.setAttribute("primaryCharacteristicType", "colour");
        expSample1.setAttribute("primaryCharacteristic", "pink");
        Item expSample2 = createTgtItem("Sample", "0_2", "");
        expSample2.setAttribute("primaryCharacteristicType", "size");
        expSample2.setAttribute("primaryCharacteristic", "medium");
        Map exp = new HashMap();
        exp.put("0_1", expSample1);
        exp.put("0_2", expSample2);

        translator.processSamples();
        assertEquals(exp, translator.samplesById);
    }

    public void testProcessMicroArrayAssays() throws Exception {
        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(new HashMap()),
                                                               mapping, srcModel, getTargetModel(tgtNs),
                                                               "mage_config_test.properties");
        // TODO need to test with numerical ids and mix of characters and numbers
        Item assay1 = createTgtItem("MicroArrayAssay", "0_1", "");
        assay1.setAttribute("name", "assay-B");
        translator.assays.put("0_1", assay1);
        Item assay2 = createTgtItem("MicroArrayAssay", "0_2", "");
        assay2.setAttribute("name", "assay-A");
        translator.assays.put("0_2", assay2);

        translator.assayToSamples.put("0_1", Arrays.asList(new String[] {"1_1", "1_2"}));
        Item sample1 = createTgtItem("Sample", "1_1", "");
        sample1.setAttribute("primaryCharacteristicType", "colour");
        sample1.setAttribute("primaryCharacteristic", "pink");
        translator.samplesById.put(sample1.getIdentifier(), sample1);

        Item sample2 = createTgtItem("Sample", "1_2", "");
        sample2.setAttribute("primaryCharacteristicType", "colour");
        sample2.setAttribute("primaryCharacteristic", "green");
        translator.samplesById.put(sample2.getIdentifier(), sample2);

        translator.assayToExpName.put("0_1", "E-FLYC-1");
        translator.sampleToLabel.put("1_1", "cy3");
        translator.sampleToLabel.put("1_2", "cy5");

        Item expAssay1 = createTgtItem("MicroArrayAssay", "0_1", "");
        expAssay1.setAttribute("name", "assay-B");
        expAssay1.setAttribute("sample1", "colour: pink");
        expAssay1.setAttribute("sample2", "colour: green");
        expAssay1.setAttribute("displayOrder", "1");
        expAssay1.setReference("experiment", "2_2");
        expAssay1.setCollection("samples", new ArrayList(Arrays.asList(new Object[] {"1_1", "1_2"})));
        Item expAssay2 = createTgtItem("MicroArrayAssay", "0_2", "");
        expAssay2.setAttribute("name", "assay-A");
        expAssay2.setAttribute("displayOrder", "0");
        expAssay2.setReference("experiment", "2_2");
        Map expected = new HashMap();
        expected.put("0_1", expAssay1);
        expected.put("0_2", expAssay2);

        // set up maps to test displayOrder
        translator.assayToExperiment.put("0_1", "2_2");
        translator.assayToExperiment.put("0_2", "2_2");

        translator.processMicroArrayAssays();
        assertEquals(expected, translator.assays);
    }


    public void testTranslateSample() throws Exception {
        Item srcItem1 = createSrcItem("BioSource", "0_1", "");
        srcItem1.setAttribute("identifier", "S:BioSource:FLYC:10");
        srcItem1.setAttribute("name", "BioSource name");
        srcItem1.setReference("materialType", "1_3");
        srcItem1.setCollection("characteristics",
                               new ArrayList(Arrays.asList(new Object[] {"1_1", "1_2"})));

        Item srcItem2 = createSrcItem("OntologyEntry", "1_1", "");
        srcItem2.setAttribute("category", "Organism");
        srcItem2.setAttribute("value", "Drosophila melanogaster");

        Item srcItem3 = createSrcItem("OntologyEntry", "1_2", "");
        srcItem3.setAttribute("category", "height");
        srcItem3.setAttribute("value", "30 metres");

        Item srcItem4 = createSrcItem("OntologyEntry", "1_3", "");
        srcItem4.setAttribute("category", "materialType");
        srcItem4.setAttribute("value", "genomic DNA");

        Set src = new HashSet(Arrays.asList(new Object[]{srcItem1, srcItem2, srcItem3, srcItem4}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs),
                                                               "mage_config_test.properties");
        Item expItem1 = createTgtItem("Sample", "0_1", "");
        expItem1.setAttribute("name", "BioSource name");
        expItem1.setAttribute("materialType", "genomic DNA");
        expItem1.setReference("organism", "-1_1");
        expItem1.setCollection("characteristics", new ArrayList(Collections.singleton("-1_2")));

        Item expItem2 = createTgtItem("Organism", "-1_1", "");
        expItem2.setAttribute("taxonId", "7227");

        Item expItem3 = createTgtItem("SampleCharacteristic", "-1_2", "");
        expItem3.setAttribute("type", "height");
        expItem3.setAttribute("value", "30 metres");

        HashSet expected=new HashSet(Arrays.asList(new Object[]{expItem1, expItem2, expItem3}));

        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        assertEquals(expected, tgtIw.getItems());
    }

    public void testSetDerivedBAToMeasuredBA() throws Exception{

        Item srcItem10 = createSrcItem("DerivedBioAssay", "57_1", "");
        srcItem10.setCollection("derivedBioAssayMap", new ArrayList(Arrays.asList(new Object[]{"1_1"})));
        Item srcItem11 = createSrcItem("DerivedBioAssay", "57_2", "");
        srcItem11.setCollection("derivedBioAssayMap", new ArrayList(Arrays.asList(new Object[]{"1_2"})));
        Item srcItem12 = createSrcItem("DerivedBioAssay", "57_3", "");
        srcItem12.setCollection("derivedBioAssayMap", new ArrayList(Arrays.asList(new Object[]{"1_3"})));


        Item srcItem51 = createSrcItem("BioAssayMap", "1_1", "");
        srcItem51.setCollection("sourceBioAssays", new ArrayList(Arrays.asList(new Object[]{"57_2", "57_3"})));
        Item srcItem52 = createSrcItem("BioAssayMap", "1_2", "");
        srcItem52.setCollection("sourceBioAssays", new ArrayList(Arrays.asList(new Object[]{"5_2", "5_3"})));
        Item srcItem53 = createSrcItem("BioAssayMap", "1_3", "");
        srcItem53.setCollection("sourceBioAssays", new ArrayList(Arrays.asList(new Object[]{"5_4", "5_5"})));

        Item srcItem61 = createSrcItem("MeasuredBioAssay", "5_2", "");
        Item srcItem62 = createSrcItem("MeasuredBioAssay", "5_3", "");
        Item srcItem63 = createSrcItem("MeasuredBioAssay", "5_4", "");
        Item srcItem64 = createSrcItem("MeasuredBioAssay", "5_5", "");


        Set src = new HashSet(Arrays.asList(new Object[] {srcItem10, srcItem11, srcItem12, srcItem51,
                              srcItem52, srcItem53, srcItem61, srcItem62, srcItem63, srcItem64}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap),
                                                               mapping, srcModel, getTargetModel(tgtNs),
                                                               "mage_config_test.properties");
        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        Map expMap = new HashMap();
        expMap.put("57_1", new ArrayList(Arrays.asList(new Object[]{"5_2", "5_3", "5_4", "5_5"})));
        expMap.put("57_2", new ArrayList(Arrays.asList(new Object[]{"5_2", "5_3"})));
        expMap.put("57_3", new ArrayList(Arrays.asList(new Object[]{"5_4", "5_5"})));
        assertEquals(expMap, translator.bioAssayMap);

    }

    // just make sure no exception thrown
    public void testGetPrefetchDescriptors() throws Exception {
        MageDataTranslator.getPrefetchDescriptors();

    }

    protected Collection getSrcItems() throws Exception {
        MockItemWriter mockIw = new MockItemWriter(new LinkedHashMap());
        MageConverter converter = new MageConverter(mockIw);

        BufferedReader srcReader = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("MageTestData_adf.xml")));
        converter.process(srcReader);


         //  srcReader = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("MageTestData_exp.xml")));
//          converter.process(srcReader);

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
