package org.flymine.dataloader;

import junit.framework.TestCase;

import org.acedb.*;
import org.acedb.staticobj.*;

import org.flymine.model.testmodel.*;


public class AceDataLoaderTest extends TestCase {

    public AceDataLoaderTest(String arg) {
        super(arg);
    }

    private String model;

    public void setUp() throws Exception {
        super.setUp();
        model = "testmodel";
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }


    public void testSimpleTag() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, AceTestObject.class.getName());
        StaticAceNode node1 = new StaticAceNode("stringValue", obj);
        obj.addNode(node1);
        StaticStringValue value1 = new StaticStringValue("A string", node1);
        node1.addNode(value1);
        AceTestObject testObj = new AceTestObject();
        testObj.identifier = "AceTestObject1";
        testObj.stringValue = "A string";


        AceTestObject ret = (AceTestObject) AceDataLoader.processAceObject(obj, null);

        assertEquals(testObj.identifier, ret.identifier);
        assertEquals(testObj.stringValue, ret.stringValue);

    }

    public void testNestedTag() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, AceTestObject.class.getName());
        StaticAceNode node1 = new StaticAceNode("baseTag", obj);
        obj.addNode(node1);
        StaticAceNode node2 = new StaticAceNode("stringValue", node1);
        node1.addNode(node2);
        StaticStringValue value1 = new StaticStringValue("A string", node2);
        node2.addNode(value1);
        AceTestObject testObj = new AceTestObject();
        testObj.identifier = "AceTestObject1";
        testObj.stringValue = "A string";

        AceTestObject ret = (AceTestObject) AceDataLoader.processAceObject(obj, null);

        assertEquals(testObj.identifier, ret.identifier);
        assertEquals(testObj.stringValue, ret.stringValue);

    }

    public void testManyValuesForSameTag() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, AceTestObject.class.getName());
        StaticAceNode node1 = new StaticAceNode("stringValue", obj);
        obj.addNode(node1);
        StaticStringValue value1 = new StaticStringValue("A string", node1);
        node1.addNode(value1);
        StaticStringValue value2 = new StaticStringValue("A second string", value1);
        value1.addNode(value2);

        AceTestObject testObj = new AceTestObject();
        testObj.identifier = "AceTestObject1";
        testObj.stringValue = "A string";
        testObj.stringValue_2 = "A second string";

        AceTestObject ret = (AceTestObject) AceDataLoader.processAceObject(obj, null);

        assertEquals(testObj.identifier, ret.identifier);
        assertEquals(testObj.stringValue, ret.stringValue);
        assertEquals(testObj.stringValue_2, ret.stringValue_2);

    }

    public void testOneValueForCollection() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, AceTestObject.class.getName());
        StaticAceNode node1 = new StaticAceNode("stringValues", obj);
        obj.addNode(node1);
        StaticStringValue value1 = new StaticStringValue("A string", node1);
        node1.addNode(value1);

        AceTestObject testObj = new AceTestObject();
        testObj.identifier = "AceTestObject1";
        testObj.stringValues.add("A string");

        AceTestObject ret = (AceTestObject) AceDataLoader.processAceObject(obj, null);

        assertEquals(testObj.identifier, ret.identifier);
        assertEquals(testObj.stringValues, ret.stringValues);

    }

    public void testTwoValuesForCollection() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, AceTestObject.class.getName());
        StaticAceNode node1 = new StaticAceNode("stringValues", obj);
        obj.addNode(node1);
        StaticStringValue value1 = new StaticStringValue("A string", node1);
        node1.addNode(value1);

        StaticStringValue value2 = new StaticStringValue("A second string", node1);
        node1.addNode(value2);

        AceTestObject testObj = new AceTestObject();
        testObj.identifier = "AceTestObject1";
        testObj.stringValues.add("A string");
        testObj.stringValues.add("A second string");

        AceTestObject ret = (AceTestObject) AceDataLoader.processAceObject(obj, null);

        assertEquals(testObj.identifier, ret.identifier);
        assertEquals(testObj.stringValues, ret.stringValues);

    }

    public void testBooleanTag() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, AceTestObject.class.getName());
        StaticAceNode node1 = new StaticAceNode("onOrOff", obj);
        obj.addNode(node1);

        AceTestObject testObj = new AceTestObject();
        testObj.identifier = "AceTestObject1";
        testObj.onOrOff = Boolean.TRUE;


        AceTestObject ret = (AceTestObject) AceDataLoader.processAceObject(obj, null);

        assertEquals(testObj.identifier, ret.identifier);
        assertEquals(testObj.onOrOff, ret.onOrOff);

    }


}
