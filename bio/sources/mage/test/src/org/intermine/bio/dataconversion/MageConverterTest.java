package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

public class MageConverterTest extends ItemsTestCase
{
    MageConverter converter;
    String ns = "http://www.intermine.org/model/mage#";
    File f = null;

    
    
    public MageConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        converter = new MageConverter(new MockItemWriter(new HashMap()));
    }

    public void tearDown() throws Exception {
        converter.close();
        if (f != null) {
            f.delete();
        }
    }

    public void testConvertMageML() throws Exception {
        HashMap map = new HashMap();
        MockItemWriter itemWriter = new MockItemWriter(map);
        MageConverter mc = new MageConverter(itemWriter);
        mc.seenMap = new LinkedHashMap();
        mc.refMap = new LinkedHashMap();

        Reader reader = new InputStreamReader(getClass().getClassLoader().
                 getResourceAsStream("mage_ml_example.xml"));

        mc.process(reader);
        System.err.println("seenMap " + mc.seenMap);
        System.err.println("refMap " + mc.refMap);
        reader = new InputStreamReader(getClass().getClassLoader().
                 getResourceAsStream("mage_ml_example1.xml"));
        mc.process(reader);
        System.err.println("seenMap " + mc.seenMap);
        System.err.println("refMap " + mc.refMap);
        mc.close();

        Set expected = readItemSet("MAGEConverterTest.xml");

        assertEquals(expected, itemWriter.getItems());
    }

    public void testCreateItemAttribute() throws Exception {
        converter.seenMap = new LinkedHashMap();

        HashMap map = new HashMap();
        MockItemWriter itemWriter = new MockItemWriter(map);
        MageConverter mc = new MageConverter(itemWriter);

        String s = "<BioSequence_package><BioSequence_assnlist> <BioSequence identifier=\"bio_identifier\" sequence=\"GATTACA\"></BioSequence></BioSequence_assnlist></BioSequence_package>";
        StringReader sr = new StringReader(s);
        mc.process(sr);
        mc.close();

        Item expected = new Item();
        expected.setClassName(ns + "BioSequence");
        expected.setIdentifier("0_0");
        expected.setAttribute("sequence", "GATTACA");
        expected.setAttribute("identifier", "bio_identifier");
        Set expSet = new HashSet(Arrays.asList(new Object[] {expected}));
        assertEquals(expSet, itemWriter.getItems());

    }


    public void testCreateItemReference() throws Exception {
        converter.seenMap = new LinkedHashMap();

        HashMap map = new HashMap();
        MockItemWriter itemWriter = new MockItemWriter(map);
        MageConverter mc = new MageConverter(itemWriter);
        String s = "<BioSequence_package><BioSequence_assnlist><BioSequence><PolymerType_assn><OntologyEntry value=\"Term\" ></OntologyEntry></PolymerType_assn></BioSequence></BioSequence_assnlist></BioSequence_package>";
        StringReader sr = new StringReader(s);
        mc.process(sr);
        mc.close();

        Item expected = new Item();
        expected.setClassName(ns + "BioSequence");
        expected.setIdentifier("0_0");
        Item item2 = new Item();
        item2.setClassName(ns + "OntologyEntry");
        item2.setIdentifier("1_1");
        Attribute attr = new Attribute();
        attr.setName("value");
        attr.setValue("Term");
        item2.addAttribute(attr);
        Reference ref = new Reference();
        ref.setName("polymerType");
        ref.setRefId("1_1");
        expected.addReference(ref);
        Set expSet = new HashSet(Arrays.asList(new Object[] {expected, item2}));

        assertEquals(expSet, itemWriter.getItems());

    }

    public void testCreateItemCollection() throws Exception {
        converter.seenMap = new LinkedHashMap();

        HashMap map = new HashMap();
        MockItemWriter itemWriter = new MockItemWriter(map);
        MageConverter mc = new MageConverter(itemWriter);
        String s = "<BioSequence_package><BioSequence_assnlist><BioSequence><SequenceDatabases_assnlist><DatabaseEntry URI=\"www.test1.org\"/><DatabaseEntry URI=\"www.test2.org\"/></SequenceDatabases_assnlist></BioSequence></BioSequence_assnlist></BioSequence_package>";

        StringReader sr = new StringReader(s);
        mc.process(sr);
        mc.close();
        Item expected = new Item();
        expected.setClassName(ns + "BioSequence");
        expected.setIdentifier("0_0");
        Item item2 = new Item();
        item2.setClassName(ns + "DatabaseEntry");
        item2.setIdentifier("1_1");
        Attribute attr1 = new Attribute();
        attr1.setName("URI");
        attr1.setValue("www.test1.org");
        item2.addAttribute(attr1);

        Item item3 = new Item();
        item3.setClassName(ns + "DatabaseEntry");
        item3.setIdentifier("1_2");
        Attribute attr2 = new Attribute();
        attr2.setName("URI");
        attr2.setValue("www.test2.org");
        item3.addAttribute(attr2);

        ReferenceList r1 = new ReferenceList();
        r1.setName("sequenceDatabases");
        r1.addRefId("1_1");
        r1.addRefId("1_2");
        expected.addCollection(r1);
        Set expSet = new HashSet(Arrays.asList(new Object[] {expected, item2, item3}));

        assertEquals(expSet, itemWriter.getItems());

    }

    public void testBioAssayData() throws Exception {
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        MageConverter mc = new MageConverter(itemWriter);
        // only take two types should skip middle column of file
        mc.setQuantitationtypes("col1, col3");

        mc.seenMap = new LinkedHashMap();
        String s = "<MAGE-ML>";
        s += "<BioAssayData_package>";

        s += "<BioAssayData_assnlist><DerivedBioAssayData identifier = \"dbad1\">" ;
        s += "<BioDataValues_assn><BioDataCube order=\"DBQ\"><DataExternal_assn><DataExternal  filenameURI=\"mage_example_data\"/></DataExternal_assn></BioDataCube></BioDataValues_assn>" ;
        s += "<BioAssayDimension_assnref><BioAssayDimension_ref identifier =\"bad1\"/></BioAssayDimension_assnref>";

        s += "<QuantitationTypeDimension_assn><QuantitationTypeDimension identifier =\"qtd\">" +
            "<QuantitationTypes_assnreflist>" +
             "<MeasuredSignal_ref identifier=\"ms1\"/><MeasuredSignal_ref identifier=\"ms2\"/><MeasuredSignal_ref identifier=\"ms3\"/>" +
            "</QuantitationTypes_assnreflist>" +
            "</QuantitationTypeDimension></QuantitationTypeDimension_assn>" ;

        s += "<DesignElementDimension_assn><FeatureDimension identifier=\"fd1\"><ContainedFeatures_assnlist>" +
            "<Feature identifier = \"f1\"><FeatureLocation_assn><FeatureLocation row=\"1\"  column=\"1\"></FeatureLocation></FeatureLocation_assn></Feature>" +
            "<Feature identifier = \"f2\"><FeatureLocation_assn><FeatureLocation row=\"1\"  column=\"2\"></FeatureLocation></FeatureLocation_assn></Feature>" +
            "</ContainedFeatures_assnlist></FeatureDimension></DesignElementDimension_assn>" ;

        s += "</DerivedBioAssayData></BioAssayData_assnlist>";

        s += "<BioAssayDimension_assnlist><BioAssayDimension identifier=\"bad1\">" +
            "<BioAssays_assnreflist>" +
            // "<MeasuredBioAssay_ref identifier=\"mba1\"/>" +
            "<DerivedBioAssay_ref identifier=\"dba1\"/>" +
            "</BioAssays_assnreflist>" +
            "</BioAssayDimension>";
        s += "</BioAssayDimension_assnlist></BioAssayData_package>";

        s += "<BioAssay_package><BioAssay_assnlist>"
            // + "<MeasuredBioAssay identifier=\"mba1\"/>"
            + "<DerivedBioAssay identifier=\"dba1\"><DerivedBioAssayData_assnreflist><DerivedBioAssayData_ref identifier=\"dbad1\"/></DerivedBioAssayData_assnreflist></DerivedBioAssay>"
            +"</BioAssay_assnlist></BioAssay_package> ";

        s += "<QuantitationType_package><QuantitationType_assnlist>";
        s += "<MeasuredSignal identifier=\"ms1\" name=\"col1\"><DataType_assn><OntologyEntry category=\"DataType\" value=\"type1\"></OntologyEntry></DataType_assn></MeasuredSignal>";
        s += "<MeasuredSignal identifier=\"ms2\" name=\"col2\"><DataType_assn><OntologyEntry category=\"DataType\" value=\"type2\"></OntologyEntry></DataType_assn></MeasuredSignal>";
        s += "<MeasuredSignal identifier=\"ms3\" name=\"col3\"><DataType_assn><OntologyEntry category=\"DataType\" value=\"type3\"></OntologyEntry></DataType_assn></MeasuredSignal>";
        s += "</QuantitationType_assnlist></QuantitationType_package>";

        s += "</MAGE-ML>";
        StringReader sr = new StringReader(s);
        mc.process(sr);
        mc.close();


        Item expected = new Item();
        expected.setClassName(ns + "DerivedBioAssayData");
        expected.setIdentifier("2_2");
        expected.addAttribute(createAttribute("identifier","dbad1"));
        expected.addReference(createReference("bioDataValues","12_21"));
        expected.addReference(createReference("quantitationTypeDimension", "8_10"));
        expected.addReference(createReference("bioAssayDimension", "0_0"));
        expected.addReference(createReference("designElementDimension", "3_3"));

        Item d=createItems(ns+"BioDataTuples","12_21", "");
        ReferenceList rl=new ReferenceList();
        rl.setName("bioAssayTupleData");

        Item d1=createItems(ns+"BioAssayDatum", "11_17","" );
        d1.addReference(createReference("feature", "4_4"));
        d1.addReference(createReference("quantitationType", "9_11"));
        d1.addReference(createReference("bioAssay", "1_1"));
        d1.addAttribute(createAttribute("value", "1.006"));
        rl.addRefId(d1.getIdentifier());

        Item d2=createItems(ns+"BioAssayDatum", "11_18","" );
        d2.addReference(createReference("feature", "4_4"));
        d2.addReference(createReference("quantitationType", "9_15"));
        d2.addReference(createReference("bioAssay","1_1"));
        d2.addAttribute(createAttribute("value", "234"));
        rl.addRefId(d2.getIdentifier());

        Item d3=createItems(ns+"BioAssayDatum", "11_19","" );
        d3.addReference(createReference("feature", "4_6"));
        d3.addReference(createReference("quantitationType", "9_11"));
        d3.addReference(createReference("bioAssay", "1_1"));
        d3.addAttribute(createAttribute("value", "435.223"));
        rl.addRefId(d3.getIdentifier());

        Item d4=createItems(ns+"BioAssayDatum", "11_20","" );
        d4.addReference(createReference("feature", "4_6"));
        d4.addReference(createReference("quantitationType", "9_15"));
        d4.addReference(createReference("bioAssay","1_1"));
        d4.addAttribute(createAttribute("value", "523"));
        rl.addRefId(d4.getIdentifier());
        d.addCollection(rl);


        Set expSet = new HashSet(Arrays.asList(new Object[] {expected, d, d1, d2, d3, d4}));
        Set results = new HashSet();

        // only interested in BioAssayDatam and BioDataTuples items

        Iterator i = itemWriter.getItems().iterator();
        while(i.hasNext()) {
            Item item = (Item) i.next();
            //System.out.println("item: " + item.getClassName()+ " id "+ item.getIdentifier());
            if (item.getClassName().endsWith("BioAssayDatum")
                || item.getClassName().endsWith("BioDataTuples")
                || item.getClassName().endsWith("DerivedBioAssayData")) {
                //System.out.println("itemclassname: " + item.getClassName());
                results.add(item);
            }
        }
        assertEquals(expSet, results);
    }

    public void testIgnoreClass() throws Exception {
        converter.seenMap=new LinkedHashMap();

        HashMap map = new HashMap();
        MockItemWriter itemWriter = new MockItemWriter(map);
        MageConverter mc = new MageConverter(itemWriter);
        String s = "<MAGE-ML identifier=\"MAGE:EBI\"><BioSequence_package><BioSequence_assnlist><BioSequence name=\"bsName\"><PolymerType_assn><OntologyEntry value=\"Term\" ></OntologyEntry></PolymerType_assn></BioSequence></BioSequence_assnlist></BioSequence_package></MAGE-ML>";
        StringReader sr = new StringReader(s);
        mc.process(sr);
        mc.close();

        Item expected1=createItems(ns+"BioSequence", "0_0", "");
        expected1.addReference(createReference("polymerType", "1_1"));
        expected1.addAttribute(createAttribute("name","bsName"));
        Item expected2=createItems(ns+"OntologyEntry","1_1", "");
        expected2.addAttribute(createAttribute("value", "Term"));

        Set expSet = new HashSet(Arrays.asList(new Object[] {expected1, expected2}));

        assertEquals(expSet, itemWriter.getItems());
    }

    // TODO: No proper test created for this yet
//     public void testAddDerivedBioAssays() throws Exception {
//         Reader reader = new InputStreamReader(getClass().getClassLoader().
//                                               getResourceAsStream("MageConverterDerivedBioAssays_src.xml"));

//         MageConverter.processDerivedBioAssays(reader, new File("mage_tmp.xml"), "_normalised");
//     }


    private Reference createReference(String name, String refId){
        Reference ref=new Reference();
        ref.setRefId(refId);
        ref.setName(name);
        return ref;
    }
    private Attribute createAttribute(String name, String refId){
        Attribute ref=new Attribute();
        ref.setValue(refId);
        ref.setName(name);
        return ref;
    }
    private Item createItems(String className, String itemId, String implementation){
        Item item=new Item();
        item.setIdentifier(itemId);
        item.setClassName(className);
        item.setImplementations(implementation);
        return item;
    }

    public void testDuplicateQuotes() throws Exception {
        String s1 = "something \"quoted\"";
        String s2 = converter.escapeQuotes(s1);
        assertEquals("something \\\"quoted\\\"", s2);
    }
}
