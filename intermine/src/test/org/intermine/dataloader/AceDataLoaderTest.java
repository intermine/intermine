package org.flymine.dataloader;

import java.util.Collection;

import junit.framework.TestCase;

import org.acedb.*;
import org.acedb.staticobj.*;

import org.flymine.model.testmodel.*;


public class AceDataLoaderTest extends TestCase {

    public AceDataLoaderTest(String arg) {
        super(arg);
    }

    private AceDataLoader loader  = new AceDataLoader();

    public void setUp() throws Exception {
        super.setUp();
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

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

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

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

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

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

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

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

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

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

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


        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

        assertEquals(testObj.identifier, ret.identifier);
        assertEquals(testObj.onOrOff, ret.onOrOff);

    }

    public void testReference() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, AceTestObject.class.getName());
        StaticAceNode node1 = new StaticAceNode("reference", obj);
        obj.addNode(node1);
        Reference ref1 = new StaticReference("AceTestObject2", node1,
                                             new AceURL("acedb", "host", 1234, AceTestObject.class.getName(), "", "", "username", "password"));
        node1.addNode(ref1);

        AceTestObject testObj1 = new AceTestObject();
        AceTestObject testObj2 = new AceTestObject();
        testObj1.identifier = "AceTestObject1";
        testObj2.identifier = "AceTestObject2";

        testObj1.reference = testObj2;

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

        assertEquals(testObj1.identifier, ret.identifier);
        assertEquals(testObj1.reference.identifier, ret.reference.identifier);
    }


    public void testReferences() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, AceTestObject.class.getName());
        StaticAceNode node1 = new StaticAceNode("references", obj);
        obj.addNode(node1);
        Reference ref1 = new StaticReference("AceTestObject2", node1,
                                             new AceURL("acedb", "host", 1234, AceTestObject.class.getName(), "", "", "username", "password"));
        node1.addNode(ref1);
        Reference ref2 = new StaticReference("AceTestObject3", node1,
                                             new AceURL("acedb", "host", 1234, AceTestObject.class.getName(), "", "", "username", "password"));
        node1.addNode(ref2);

        AceTestObject testObj1 = new AceTestObject();
        AceTestObject testObj2 = new AceTestObject();
        AceTestObject testObj3 = new AceTestObject();
        testObj1.identifier = "AceTestObject1";
        testObj2.identifier = "AceTestObject2";
        testObj3.identifier = "AceTestObject3";

        testObj1.references.add(testObj2);
        testObj1.references.add(testObj3);

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

        assertEquals(testObj1.identifier, ret.identifier);
        assertEquals(testObj1.references.size(), ret.references.size());
    }

    public void testHash() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, AceTestObject.class.getName());
        StaticAceNode node1 = new StaticAceNode("hashValue", obj);
        obj.addNode(node1);
        StaticAceNode node2 = new StaticAceNode("stringValue", node1);
        node1.addNode(node2);
        StaticStringValue value1 = new StaticStringValue("A string", node2);
        node2.addNode(value1);

        AceTestObject testObj1 = new AceTestObject();
        AceTestObject testObj2 = new AceTestObject();
        testObj1.identifier = "AceTestObject1";
        testObj2.identifier = "";
        testObj2.stringValue = "A string";
        testObj1.hashValue = testObj2;

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

        assertEquals(testObj1.identifier, ret.identifier);
        assertEquals(testObj2.identifier, ret.hashValue.identifier);
        assertEquals(testObj2.stringValue, ret.hashValue.stringValue);

    }

    // Expect nothing to be set, but the object to be created
    public void testFieldMissing() throws Exception {
        StaticAceObject obj = new StaticAceObject("AceTestObject1", null, AceTestObject.class.getName());
        StaticAceNode node1 = new StaticAceNode("nonexistentValue", obj);
        obj.addNode(node1);
        StaticStringValue value1 = new StaticStringValue("A string", node1);
        node1.addNode(value1);
        AceTestObject testObj = new AceTestObject();
        testObj.identifier = "AceTestObject1";

        AceTestObject ret = (AceTestObject) loader.processAceObject(obj);

        assertEquals(testObj.identifier, ret.identifier);

    }

    public void testProcessObjectNullObject() throws Exception {
        try {
            loader.processAceObject(null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testProcessObjectsNullSet() throws Exception {
        try {
            loader.processAceObjects(null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testProcessObjects() throws Exception {
        StaticAceSet set = new StaticAceSet(null, null, null);
        StaticAceObject obj1 = new StaticAceObject("AceTestObject1", null, AceTestObject.class.getName());
        StaticAceNode node1 = new StaticAceNode("stringValue", obj1);
        obj1.addNode(node1);
        StaticStringValue value1 = new StaticStringValue("A string", node1);
        node1.addNode(value1);
        StaticAceObject obj2 = new StaticAceObject("AceTestObject2", null, AceTestObject.class.getName());
        StaticAceNode node2 = new StaticAceNode("stringValue", obj2);
        obj2.addNode(node2);
        StaticStringValue value2 = new StaticStringValue("A second string", node2);
        node2.addNode(value2);

        set.add("1", obj1);
        set.add("2", obj2);

        Collection ret = loader.processAceObjects(set);

        assertEquals(2, ret.size());

    }

}
