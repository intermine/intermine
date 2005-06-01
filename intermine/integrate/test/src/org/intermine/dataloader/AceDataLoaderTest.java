package org.intermine.dataloader;

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

import java.util.Date;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import org.acedb.*;
import org.acedb.staticobj.*;

import org.intermine.util.StringUtil;
import org.intermine.modelproduction.xml.InterMineModelParser;
import org.intermine.model.acedbtest.*;

public class AceDataLoaderTest extends TestCase
{
    private AceDataLoader loader;

    public AceDataLoaderTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        InterMineModelParser parser = new InterMineModelParser();
        loader = new AceDataLoader(parser.process(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("acedbtest_model.xml"))));
    }
    
    public void testSimpleTag() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, "AceTestObject");
        StaticAceNode node1 = new StaticAceNode("stringValue", obj);
        obj.addNode(node1);
        StaticStringValue value1 = new StaticStringValue("A string", node1);
        node1.addNode(value1);
        AceTestObject testObj = new AceTestObject();
        testObj.setIdentifier("AceTestObject1");
        testObj.setStringValue("A string");

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

        assertEquals(testObj.getIdentifier(), ret.getIdentifier());
        assertEquals(testObj.getStringValue(), ret.getStringValue());
    }
    
    public void testDateTypeTag() throws Exception {
        Date date = new Date();
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, "AceTestObject");
        StaticAceNode node1 = new StaticAceNode("dateValue", obj);
        obj.addNode(node1);
        StaticDateValue value1 = new StaticDateValue(date, node1);
        node1.addNode(value1);

        AceTestObject testObj = new AceTestObject();
        testObj.setIdentifier("AceTestObject1");
        testObj.setDateValue(date);

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

        assertEquals(testObj.getIdentifier(), ret.getIdentifier());
        assertEquals(testObj.getDateValue(), ret.getDateValue());
    }
    public void testNestedTag() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, "AceTestObject");
        StaticAceNode node1 = new StaticAceNode("baseTag", obj);
        obj.addNode(node1);
        StaticAceNode node2 = new StaticAceNode("stringValue", node1);
        node1.addNode(node2);
        StaticStringValue value1 = new StaticStringValue("A string", node2);
        node2.addNode(value1);
        AceTestObject testObj = new AceTestObject();
        testObj.setIdentifier("AceTestObject1");
        testObj.setStringValue("A string");

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

        assertEquals(testObj.getIdentifier(), ret.getIdentifier());
        assertEquals(testObj.getStringValue(), ret.getStringValue());
    }

    public void testManyValuesForSameTag() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, "AceTestObject");
        StaticAceNode node1 = new StaticAceNode("stringValue", obj);
        obj.addNode(node1);
        StaticStringValue value1 = new StaticStringValue("A string", node1);
        node1.addNode(value1);
        StaticStringValue value2 = new StaticStringValue("A second string", value1);
        value1.addNode(value2);

        AceTestObject testObj = new AceTestObject();
        testObj.setIdentifier("AceTestObject1");
        testObj.setStringValue("A string");
        testObj.setStringValue_2("A second string");

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

        assertEquals(testObj.getIdentifier(), ret.getIdentifier());
        assertEquals(testObj.getStringValue(), ret.getStringValue());
        assertEquals(testObj.getStringValue_2(), ret.getStringValue_2());
    }

    public void testOneValueForCollection() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, "AceTestObject");
        StaticAceNode node1 = new StaticAceNode("stringValues", obj);
        obj.addNode(node1);
        StaticStringValue value1 = new StaticStringValue("A string", node1);
        node1.addNode(value1);

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

        assertEquals("AceTestObject1", ret.getIdentifier());
        assertEquals("A string", ((Text) ret.getStringValues().iterator().next()).getIdentifier());
    }

    public void testTwoValuesForCollection() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, "AceTestObject");
        StaticAceNode node1 = new StaticAceNode("stringValues", obj);
        obj.addNode(node1);
        StaticStringValue value1 = new StaticStringValue("A string", node1);
        node1.addNode(value1);
        StaticStringValue value2 = new StaticStringValue("A second string", node1);
        node1.addNode(value2);

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

        assertEquals("AceTestObject1", ret.getIdentifier());

        Set expected = new HashSet();
        expected.add("A string");
        expected.add("A second string");
        Set retrieved = new HashSet();
        Iterator iter = ret.getStringValues().iterator();
        retrieved.add(((Text) iter.next()).getIdentifier());
        retrieved.add(((Text) iter.next()).getIdentifier());
        assertEquals(expected, retrieved);
    }

    public void testBooleanTag() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, "AceTestObject");
        StaticAceNode node1 = new StaticAceNode("onOrOff", obj);
        obj.addNode(node1);

        AceTestObject testObj = new AceTestObject();
        testObj.setIdentifier("AceTestObject1");
        testObj.setOnOrOff(true);

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

        assertEquals(testObj.getIdentifier(), ret.getIdentifier());
        assertEquals(testObj.getOnOrOff(), ret.getOnOrOff());
    }

    public void testReference() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, "AceTestObject");
        StaticAceNode node1 = new StaticAceNode("reference", obj);
        obj.addNode(node1);
        Reference ref1 = new StaticReference("AceTestObject2", node1,
                                             new AceURL("acedb", "host", 1234, "AceTestObject", "", "", "username", "password"));
        node1.addNode(ref1);

        AceTestObject testObj1 = new AceTestObject();
        AceTestObject testObj2 = new AceTestObject();
        testObj1.setIdentifier("AceTestObject1");
        testObj2.setIdentifier("AceTestObject2");

        testObj1.setReference(testObj2);

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

        assertEquals(testObj1.getIdentifier(), ret.getIdentifier());
        assertEquals(testObj1.getReference().getIdentifier(), ret.getReference().getIdentifier());
    }

    public void testReferences() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, "AceTestObject");
        StaticAceNode node1 = new StaticAceNode("references", obj);
        obj.addNode(node1);
        Reference ref1 = new StaticReference("AceTestObject2", node1,
                                             new AceURL("acedb", "host", 1234, "AceTestObject", "", "", "username", "password"));
        node1.addNode(ref1);
        Reference ref2 = new StaticReference("AceTestObject3", node1,
                                             new AceURL("acedb", "host", 1234, "AceTestObject", "", "", "username", "password"));
        node1.addNode(ref2);

        AceTestObject testObj1 = new AceTestObject();
        AceTestObject testObj2 = new AceTestObject();
        AceTestObject testObj3 = new AceTestObject();
        testObj1.setIdentifier("AceTestObject1");
        testObj2.setIdentifier("AceTestObject2");
        testObj3.setIdentifier("AceTestObject3");

        testObj1.getReferences().add(testObj2);
        testObj1.getReferences().add(testObj3);

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

        assertEquals(testObj1.getIdentifier(), ret.getIdentifier());
        assertEquals(testObj1.getReferences().size(), ret.getReferences().size());
    }

    // obj1 - node(hash obj2) - node - value => obj1 - obj2 - obj3(node - value)
    public void testHashReference() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, "AceTestObject");
        StaticAceNode node1 = new StaticAceNode("hashValue", obj);
        obj.addNode(node1);
        StaticAceNode node2 = new StaticAceNode("intValue", node1);
        node1.addNode(node2);
        StaticIntValue value1 = new StaticIntValue(42, node2);
        node2.addNode(value1);

        AceTestObject testObj1 = new AceTestObject();
        testObj1.setIdentifier("AceTestObject1");
        AceTestObject testObj2 = new AceTestObject();
        testObj2.setIdentifier("0");
        testObj2.setIntValue(new Integer(42));
        testObj1.setHashValue(testObj2);

        StringUtil.setNextUniqueNumber(0);
        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

        assertEquals(testObj1.getIdentifier(), ret.getIdentifier());
        assertEquals(testObj2.getIdentifier(), ret.getHashValue().getIdentifier());
        assertEquals(testObj2.getIntValue(), ret.getHashValue().getIntValue());
    }

    public void testHashCollection() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, "AceTestObject");
        StaticAceNode node1 = new StaticAceNode("hashValues", obj);
        obj.addNode(node1);
        StaticAceNode node2 = new StaticAceNode("intValue", node1);
        node1.addNode(node2);
        StaticIntValue value1 = new StaticIntValue(42, node2);
        node2.addNode(value1);

        AceTestObject testObj1 = new AceTestObject();
        testObj1.setIdentifier("AceTestObject1");
        AceTestObject testObj2 = new AceTestObject();
        testObj2.setIdentifier("0");
        testObj2.setIntValue(new Integer(42));
        testObj1.getHashValues().add(testObj2);

        StringUtil.setNextUniqueNumber(0);
        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);
        AceTestObject ret2 = (AceTestObject) ret.getHashValues().iterator().next();

        assertEquals(testObj1.getIdentifier(), ret.getIdentifier());
        assertEquals(testObj1.getHashValues().size(), ret.getHashValues().size());
        assertEquals(testObj2.getIdentifier(), ret2.getIdentifier());
        assertEquals(testObj2.getIntValue(), ret2.getIntValue());
    }

//     public void testValueMissing() throws Exception {
//         StaticAceObject obj = new StaticAceObject("AceTestObject1", null, "AceTestObject");
//         StaticAceNode node1 = new StaticAceNode("stringValue", obj);
//         obj.addNode(node1);

//         AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

//         assertEquals("", ret.getStringValue());
//     }
    
    public void testFieldMissing() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, "AceTestObject");
        StaticAceNode node1 = new StaticAceNode("nonexistentValue", obj);
        obj.addNode(node1);
        StaticStringValue value1 = new StaticStringValue("A string", node1);
        node1.addNode(value1);

        AceTestObject testObj = new AceTestObject();
        testObj.setIdentifier("AceTestObject1");

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

        assertEquals(testObj.getIdentifier(), ret.getIdentifier());
    }

    public void testProcessObjectNullObject() throws Exception {
        try {
            loader.processAceObject(null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }
}
