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

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;

import org.biomage.BioSequence.BioSequence;
import org.biomage.Description.OntologyEntry;
import org.biomage.Description.DatabaseEntry;
import org.biomage.BioSequence.SeqFeature;
import org.biomage.tools.xmlutils.*;
import org.biomage.Common.MAGEJava;

import org.flymine.xml.full.Item;
import org.flymine.xml.full.Field;
import org.flymine.xml.full.ReferenceList;
import org.flymine.xml.full.FullRenderer;
import org.flymine.xml.full.FullParser;


public class MageConvertorTest extends TestCase
{
    MageConvertor convertor;
    String ns = "http://www.biomage.org#";

    public void setUp() {
        convertor = new MageConvertor();
    }

    public void testConvertMageML() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("test/mage_ml_example.xml");
        List items = new ArrayList(convertor.convertMageML(is, ns));

        List expected = FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/MAGEConvertor_test.xml"));
        assertEquals(expected, items);
    }

    public void testCreateItemAttribute() throws Exception {
        convertor.seenMap = new LinkedHashMap();

        BioSequence bio = new BioSequence();
        bio.setSequence("GATTACA");

        Item expected = new Item();
        expected.setClassName(ns + "BioSequence");
        expected.setIdentifier("1");
        Field f1 = new Field();
        f1.setName("sequence");
        f1.setValue("GATTACA");
        expected.addField(f1);

        assertEquals(expected, convertor.createItem(bio, ns));
    }


    public void testCreateItemReferenceInnerClass() throws Exception {
        convertor.seenMap = new LinkedHashMap();

        SeqFeature s1 = new SeqFeature();
        s1.setNameByValueBasis(2);

        Item expected = new Item();
        expected.setClassName(ns + "SeqFeature");
        expected.setIdentifier("1");
        Field f1 = new Field();
        f1.setName("basis");
        f1.setValue("both");
        expected.addField(f1);
        assertEquals(expected, convertor.createItem(s1, ns));
    }



    public void testCreateItemReference() throws Exception {
        convertor.seenMap = new LinkedHashMap();

        BioSequence bio = new BioSequence();
        OntologyEntry o1 = new OntologyEntry();
        o1.setValue("Term");
        bio.setPolymerType(o1);

        Item expected = new Item();
        expected.setClassName(ns + "BioSequence");
        expected.setIdentifier("1");
        Item item2 = new Item();
        item2.setClassName(ns + "OntologyEntry");
        item2.setIdentifier("2");
        Field f2 = new Field();
        f2.setName("value");
        f2.setValue("Term");
        item2.addField(f2);
        Field f1 = new Field();
        f1.setName("polymerType");
        f1.setValue("2");
        expected.addReference(f1);

        assertEquals(expected, convertor.createItem(bio, ns));
    }


    public void testCreateItemCollection() throws Exception {
        convertor.seenMap = new LinkedHashMap();

        BioSequence bio = new BioSequence();
        DatabaseEntry d1 = new DatabaseEntry();
        d1.setURI("www.test1.org");
        DatabaseEntry d2 = new DatabaseEntry();
        d2.setURI("www.test2.org");
        bio.addToSequenceDatabases(d1);
        bio.addToSequenceDatabases(d2);

        Item expected = new Item();
        expected.setClassName(ns + "BioSequence");
        expected.setIdentifier("1");
        Item item2 = new Item();
        item2.setClassName(ns + "DatabaseEntry");
        item2.setIdentifier("2");
        Field f1 = new Field();
        f1.setName("uri");
        f1.setValue("www.test1.org");
        item2.addField(f1);

        Item item3 = new Item();
        item3.setClassName(ns + "DatabaseEntry");
        item3.setIdentifier("3");
        Field f2 = new Field();
        f2.setName("uri");
        f2.setValue("www.test1.org");
        item3.addField(f2);

        ReferenceList r1 = new ReferenceList();
        r1.setName("sequenceDatabases");
        r1.addValue("2");
        r1.addValue("3");
        expected.addCollection(r1);

        assertEquals(expected, convertor.createItem(bio, ns));
    }
}
