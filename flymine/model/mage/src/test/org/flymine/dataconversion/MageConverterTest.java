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
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;

import org.biomage.BioSequence.BioSequence;
import org.biomage.Description.OntologyEntry;
import org.biomage.Description.DatabaseEntry;
import org.biomage.BioSequence.SeqFeature;
import org.biomage.tools.xmlutils.*;

import org.flymine.xml.full.Attribute;
import org.flymine.xml.full.Item;
import org.flymine.xml.full.Reference;
import org.flymine.xml.full.ReferenceList;
import org.flymine.xml.full.FullParser;
import org.flymine.xml.full.ItemHelper;

public class MageConverterTest extends TestCase
{
    MageConverter converter;
    String ns = "http://www.biomage.org#";

    public void setUp() {
        converter = new MageConverter(null, null);
    }

    public void testConvertMageML() throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("test/mage_ml_example.xml")));
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        new MageConverter(reader, itemWriter).process();

        Set expected = new HashSet(FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/MAGEConverterTest.xml")));
        assertEquals(expected, itemWriter.getItems());
    }

    public void testCreateItemAttribute() throws Exception {
        converter.seenMap = new LinkedHashMap();

        BioSequence bio = new BioSequence();
        bio.setSequence("GATTACA");

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
}
