package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Iterator;

import org.biomage.BioSequence.BioSequence;
import org.biomage.Description.OntologyEntry;
import org.biomage.Description.DatabaseEntry;
import org.biomage.BioSequence.SeqFeature;
import org.biomage.tools.xmlutils.*;
import org.biomage.DesignElement.Feature;
import org.biomage.BioAssayData.FeatureDimension;
import org.biomage.DesignElement.FeatureLocation;
import org.biomage.QuantitationType.MeasuredSignal;
import org.biomage.BioAssayData.QuantitationTypeDimension;
import org.biomage.BioAssayData.MeasuredBioAssayData;
import org.biomage.BioAssayData.DerivedBioAssayData;
import org.biomage.BioAssayData.BioDataCube;
import org.biomage.BioAssayData.DataExternal;
import org.biomage.Common.MAGEJava;
import org.biomage.BioSequence.BioSequence_package;
import org.biomage.BioSequence.BioSequence;

import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.ItemHelper;
import org.intermine.dataconversion.MockItemWriter;

public class MageConverterTest extends TestCase
{
    MageConverter converter;
    String ns = "http://www.flymine.org/model/mage#";
    File f = null;

    public void setUp() {
        converter = new MageConverter(null);
    }

    public void tearDown() {
        if (f != null) {
            f.delete();
        }
    }

    public void testConvertMageML() throws Exception {
        Reader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("test/mage_ml_example.xml"));
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        new MageConverter(itemWriter).process(reader);

        Set expected = new HashSet(FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/MAGEConverterTest.xml")));
        assertEquals(expected, itemWriter.getItems());
    }

    public void testCreateItemAttribute() throws Exception {
        converter.seenMap = new LinkedHashMap();

        BioSequence bio = new BioSequence();
        bio.setSequence("GATTACA");
        bio.setIdentifier("bio_identifier");

        Item expected = new Item();
        expected.setClassName(ns + "BioSequence");
        expected.setIdentifier("0_0");
        Attribute attr = new Attribute();
        attr.setName("sequence");
        attr.setValue("GATTACA");
        expected.addAttribute(attr);

        assertEquals(expected, ItemHelper.convert(converter.createItem(bio)));
    }

    public void testCreateItemReferenceInnerClass() throws Exception {
        converter.seenMap = new LinkedHashMap();

        SeqFeature s1 = new SeqFeature();
        s1.setNameByValueBasis(2);

        Item expected = new Item();
        expected.setClassName(ns + "SeqFeature");
        expected.setIdentifier("0_0");
        Attribute attr = new Attribute();
        attr.setName("basis");
        attr.setValue("both");
        expected.addAttribute(attr);

        assertEquals(expected, ItemHelper.convert(converter.createItem(s1)));
    }

    public void testCreateItemReference() throws Exception {
        converter.seenMap = new LinkedHashMap();

        BioSequence bio = new BioSequence();
        OntologyEntry o1 = new OntologyEntry();
        o1.setValue("Term");
        bio.setPolymerType(o1);

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

        assertEquals(expected, ItemHelper.convert(converter.createItem(bio)));
    }

    public void testCreateItemCollection() throws Exception {
        converter.seenMap = new LinkedHashMap();

        BioSequence bio = new BioSequence();
        DatabaseEntry d1 = new DatabaseEntry();
        d1.setURI("www.test1.org");
        DatabaseEntry d2 = new DatabaseEntry();
        d2.setURI("www.test2.org");
        bio.addToSequenceDatabases(d1);
        bio.addToSequenceDatabases(d2);

        Item expected = new Item();
        expected.setClassName(ns + "BioSequence");
        expected.setIdentifier("0_0");
        Item item2 = new Item();
        item2.setClassName(ns + "DatabaseEntry");
        item2.setIdentifier("1_1");
        Attribute attr1 = new Attribute();
        attr1.setName("uri");
        attr1.setValue("www.test1.org");
        item2.addAttribute(attr1);

        Item item3 = new Item();
        item3.setClassName(ns + "DatabaseEntry");
        item3.setIdentifier("1_2");
        Attribute attr2 = new Attribute();
        attr2.setName("uri");
        attr2.setValue("www.test1.org");
        item3.addAttribute(attr2);

        ReferenceList r1 = new ReferenceList();
        r1.setName("sequenceDatabases");
        r1.addRefId("1_1");
        r1.addRefId("1_2");
        expected.addCollection(r1);


        assertEquals(expected, ItemHelper.convert(converter.createItem(bio)));
    }

    public void testBioAssayData() throws Exception{
        converter.seenMap = new LinkedHashMap();
        converter.dataItems = new LinkedHashSet();


        String exampleData = "1.006\t3.456" + System.getProperty("line.separator")
            + "435.223\t1.004" + System.getProperty("line.separator");

        f = new File("build/model/mage/resources/test/mage_example_data");
        FileWriter fw = new FileWriter(f);
        fw.write(exampleData);
        fw.flush();
        fw.close();

        DerivedBioAssayData dbad=new DerivedBioAssayData();
        BioDataCube bdc=new BioDataCube();
        DataExternal df=new DataExternal();
        bdc.setDataExternal(df);
        df.setFilenameURI("test/mage_example_data");
        dbad.setBioDataValues(bdc);


        QuantitationTypeDimension qtd = new QuantitationTypeDimension();
        MeasuredSignal qt1 = new MeasuredSignal();
        OntologyEntry oe1=new OntologyEntry();
        oe1.setValue("col1");
        qt1.setDataType(oe1);
        qtd.addToQuantitationTypes(qt1);
        MeasuredSignal qt2 = new MeasuredSignal();
        OntologyEntry oe2=new OntologyEntry();
        oe2.setValue("col2");
        qt2.setDataType(oe2);
        qtd.addToQuantitationTypes(qt2);
        dbad.setQuantitationTypeDimension(qtd);

        FeatureDimension fd = new FeatureDimension();
        FeatureLocation fl1 = new FeatureLocation();
        fl1.setRow(new Integer (1));
        fl1.setColumn(new Integer(1));
        Feature f1 = new Feature();
        f1.setFeatureLocation(fl1);
        f1.setIdentifier("f1");
        fd.addToContainedFeatures(f1);
        FeatureLocation fl2 = new FeatureLocation();
        fl2.setRow(new Integer(1));
        fl2.setColumn(new Integer(2));
        Feature f2 = new Feature();
        f2.setFeatureLocation(fl2);
        f2.setIdentifier("f2");
        fd.addToContainedFeatures(f2);
        dbad.setDesignElementDimension(fd);

        Item expected = new Item();
        expected.setClassName(ns + "DerivedBioAssayData");
        expected.setIdentifier("0_0");
        expected.addReference(createReference("bioDataValues","0_13"));
        expected.addReference(createReference("quantitationTypeDimension", "6_8"));
        expected.addReference(createReference("designElementDimension", "1_1"));
        assertEquals(expected, ItemHelper.convert(converter.createItem(dbad)));

        Item d=createItems(ns+"BioDataTuples","0_13", "");
        ReferenceList rl=new ReferenceList();
        rl.setName("bioAssayTupleData");

        Item d1=createItems(ns+"BioAssayDatum", "0_14","" );
        d1.addReference(createReference("designElement", "3_3"));
        d1.addReference(createReference("quantitationType", "7_9"));
        d1.addAttribute(createAttribute("value", "1.006"));
        d1.addAttribute(createAttribute("normalised", "true"));
        rl.addRefId(d1.getIdentifier());


        Item d2=createItems(ns+"BioAssayDatum", "0_15","" );
        d2.addReference(createReference("designElement", "3_3"));
        d2.addReference(createReference("quantitationType", "7_11"));
        d2.addAttribute(createAttribute("value", "3.456"));
        d2.addAttribute(createAttribute("normalised", "true"));
        rl.addRefId(d2.getIdentifier());

        Item d3=createItems(ns+"BioAssayDatum", "0_16","" );
        d3.addReference(createReference("designElement", "3_5"));
        d3.addReference(createReference("quantitationType", "7_9"));
        d3.addAttribute(createAttribute("value", "435.223"));
        d3.addAttribute(createAttribute("normalised", "true"));
        rl.addRefId(d3.getIdentifier());

        Item d4=createItems(ns+"BioAssayDatum", "0_17","" );
        d4.addReference(createReference("designElement", "3_5"));
        d4.addReference(createReference("quantitationType", "7_11"));
        d4.addAttribute(createAttribute("value", "1.004"));
        d4.addAttribute(createAttribute("normalised", "true"));
        rl.addRefId(d4.getIdentifier());
        d.addCollection(rl);


        Set expSet = new HashSet(Arrays.asList(new Object[] {d, d1, d2, d3, d4}));
        Set results = new HashSet();

        Iterator i = converter.dataItems.iterator();
        while(i.hasNext()){
            results.add(ItemHelper.convert((org.intermine.model.fulldata.Item)i.next()));
        }
        assertEquals(expSet, results);

    }

    public void testIgnoreClass() throws Exception {
        converter.seenMap=new LinkedHashMap();

        MAGEJava m1=new MAGEJava();
        BioSequence bs=new BioSequence();
        bs.setName("bsName");
        BioSequence_package bsp=new BioSequence_package();
        bsp.addToBioSequence_list(bs);
        m1.setBioSequence_package(bsp);

        OntologyEntry o1 = new OntologyEntry();
        o1.setValue("Term");
        bs.setPolymerType(o1);

        Item expected1=createItems(ns+"BioSequence", "0_0", "");
        expected1.addReference(createReference("polymerType", "1_1"));
        expected1.addAttribute(createAttribute("name","bsName"));
        Item expected2=createItems(ns+"OntologyEntry","1_1", "");
        expected2.addAttribute(createAttribute("value", "Term"));

        Set expSet = new HashSet(Arrays.asList(new Object[] {expected1, expected2}));

        Set results = new HashSet();

        converter.createItem(m1);
        Iterator i = converter.seenMap.values().iterator();
        while(i.hasNext()){
            results.add(ItemHelper.convert((org.intermine.model.fulldata.Item)i.next()));
        }

        assertEquals(expSet, results);

    }


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
