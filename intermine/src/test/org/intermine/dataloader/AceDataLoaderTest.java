package org.flymine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import org.acedb.*;
import org.acedb.staticobj.*;

import org.flymine.modelproduction.xml.FlyMineModelParser;

import org.flymine.model.acedbtest.*;

public class AceDataLoaderTest extends TestCase
{
    private AceDataLoader loader;

    public AceDataLoaderTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        FlyMineModelParser parser = new FlyMineModelParser();
        loader = new AceDataLoader(parser.process(getClass().getClassLoader()
                                                  .getResourceAsStream("acedbtest_model.xml")));
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

        AceTestObject testObj = new AceTestObject();
        testObj.setIdentifier("AceTestObject1");
        Text text = new Text();
        text.setIdentifier("A string");
        testObj.getStringValues().add(text);

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

        assertEquals(testObj.getIdentifier(), ret.getIdentifier());
        assertEquals(testObj.getStringValues(), ret.getStringValues());
    }

    public void testTwoValuesForCollection() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, "AceTestObject");
        StaticAceNode node1 = new StaticAceNode("stringValues", obj);
        obj.addNode(node1);
        StaticStringValue value1 = new StaticStringValue("A string", node1);
        node1.addNode(value1);

        StaticStringValue value2 = new StaticStringValue("A second string", node1);
        node1.addNode(value2);

        AceTestObject testObj = new AceTestObject();
        testObj.setIdentifier("AceTestObject1");
        Text text1 = new Text();
        text1.setIdentifier("A string");
        testObj.getStringValues().add(text1);
        Text text2 = new Text();
        text2.setIdentifier("A second string");
        testObj.getStringValues().add(text2);

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

        assertEquals(testObj.getIdentifier(), ret.getIdentifier());
        assertEquals(testObj.getStringValues(), ret.getStringValues());
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

//     public void testHashReference() throws Exception {
//         StaticAceObject obj = new StaticAceObject("AceTestObject1", null, "AceTestObject");
//         StaticAceNode node1 = new StaticAceNode("hashValue", obj);
//         obj.addNode(node1);
//         StaticAceNode node2 = new StaticAceNode("stringValue", node1);
//         node1.addNode(node2);
//         StaticStringValue value1 = new StaticStringValue("A string", node2);
//         node2.addNode(value1);

//         AceTestObject testObj1 = new AceTestObject();
//         AceTestObject testObj2 = new AceTestObject();
//         testObj1.setIdentifier("AceTestObject1");
//         testObj2.setIdentifier("");
//         testObj2.setStringValue("A string");
//         testObj1.setHashValue(testObj2);

//         AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

//         assertEquals(testObj1.getIdentifier(), ret.getIdentifier());
//         assertEquals(testObj2.getIdentifier(), ret.getHashValue().getIdentifier());
//         assertEquals(testObj2.getStringValue(), ret.getHashValue().getStringValue());
//     }

//     public void testHashCollection() throws Exception {
//         StaticAceObject obj = new StaticAceObject("AceTestObject1", null, "AceTestObject");
//         StaticAceNode node1 = new StaticAceNode("hashValues", obj);
//         obj.addNode(node1);
//         StaticAceNode node2 = new StaticAceNode("stringValue", node1);
//         node1.addNode(node2);
//         StaticStringValue value1 = new StaticStringValue("A string", node2);
//         node2.addNode(value1);

//         AceTestObject testObj1 = new AceTestObject();
//         AceTestObject testObj2 = new AceTestObject();
//         testObj1.setIdentifier("AceTestObject1");
//         testObj2.setIdentifier("");
//         testObj2.setStringValue("A string");
//         testObj1.getHashValues().add(testObj2);

//         AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

//         assertEquals(testObj1.getIdentifier(), ret.getIdentifier());
//         assertEquals(testObj1.getHashValues().size(), ret.getHashValues().size());
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
