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

        Item tgtItem = createItem(tgtNs + "ReporterLocation", "6_28", "");
        tgtItem.addAttribute(new Attribute("localX", "1"));
        tgtItem.addAttribute(new Attribute("localY", "2"));
        tgtItem.addAttribute(new Attribute("zoneX", "1"));
        tgtItem.addAttribute(new Attribute("zoneY", "1"));

        HashSet expected = new HashSet(Arrays.asList(new Object[]{tgtItem}));

        assertEquals(expected, translator.translateItem(srcItem));
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

        Item tgtItem = createItem(tgtNs+"MicroArraySlideDesign", "3_1", "");

        Map expected = new HashMap();
        expected.put("1_2", "3_1");
        translator.createFeatureMap(srcItem1, tgtItem);
        assertEquals(expected, translator.featureToDesign);
    }


    //test translate from PhysicalArrayDesign to MicroArraySlideDesign
    //which includes 3 methods, createFeatureMap, padDescriptions, and changeRefToAttr
    public void testTranslateMASD() throws Exception {
        // set up items
        // PhysicalArrayDesign 1_1
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
        expectedItem.addCollection(new ReferenceList("assays", new ArrayList(Arrays.asList(new Object[]{"57_709"}))));
        HashSet expected=new HashSet(Arrays.asList(new Object[]{expectedItem}));

        assertEquals(expected, translator.translateItem(srcItem));

    }

    public void testMicroArrayAssay() throws Exception{
        Item srcItem= createItem(ns+"DerivedBioAssay", "57_709", "");
        srcItem.addCollection(new ReferenceList("derivedBioAssayData", new ArrayList(Arrays.asList(new Object[]{"58_710"}))));

        Item srcItem1= createItem(ns+"MeasuredBioAssayData", "58_710", "");
        srcItem1.addReference(new Reference("bioDataValues", "58_739"));

        Item srcItem2= createItem(ns+"BioDataTuples", "58_739", "");
        srcItem2.addCollection(new ReferenceList("bioAssayTupleData", new ArrayList(Arrays.asList(new Object[]{"58_740", "58_744", "58_755"}))));

        Set src = new HashSet(Arrays.asList(new Object[]{srcItem, srcItem1, srcItem2}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap), getOwlModel(), tgtNs);

        Item expectedItem = createItem(tgtNs+"MicroArrayAssay", "57_709", "");
        expectedItem.addCollection(new ReferenceList("results", new ArrayList(Arrays.asList(new Object[]{"58_740", "58_744", "58_755"}))));
        HashSet expected=new HashSet(Arrays.asList(new Object[]{expectedItem}));

        assertEquals(expected, translator.translateItem(srcItem));
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


        Set src = new HashSet(Arrays.asList(new Object[]{srcItem, srcItem1, srcItem2}));
        Map srcMap = writeItems(src);

        MageDataTranslator translator = new MageDataTranslator(new MockItemReader(srcMap), getOwlModel(), tgtNs);

        Item expectedItem = createItem(tgtNs+"MicroArrayExperimentalResult", "58_762", "");
        expectedItem.addAttribute(new Attribute("normalised","true"));
        expectedItem.addAttribute(new Attribute("type","Signal st dev Cy3"));
        expectedItem.addAttribute(new Attribute("scale","1_611"));
        expectedItem.addAttribute(new Attribute("isBackground","false"));

        HashSet expected=new HashSet(Arrays.asList(new Object[]{expectedItem}));

        assertEquals(expected, translator.translateItem(srcItem));
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

        return mockIw.getItems();

    }

    protected Collection getExpectedItems() throws Exception {
        //return FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/MageDataTranslatorFunctionalTest_tgt.xml"));
        return null;
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

    private Item createItem(String className, String itemId, String implementation){
        Item item=new Item();
        item.setIdentifier(itemId);
        item.setClassName(className);
        item.setImplementations(implementation);
        return item;
   }
}
