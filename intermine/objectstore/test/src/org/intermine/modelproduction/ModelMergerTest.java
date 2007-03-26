package org.intermine.modelproduction;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.modelproduction.xml.InterMineModelParser;

public class ModelMergerTest extends TestCase
{
    Model testModel;
    InterMineModelParser parser;
    Model emptyModel;

    public void setUp() throws Exception {
        parser = new InterMineModelParser();
        testModel = Model.getInstanceByName("testmodel");
        emptyModel = new Model("testModel", "testmodel#", Collections.EMPTY_SET);
    }

    public void testCloneClassDescriptor() throws Exception {
        ClassDescriptor cld = testModel
                .getClassDescriptorByName("org.intermine.model.testmodel.Employee");
        ClassDescriptor copy = ModelMerger.cloneClassDescriptor(cld);
        assertFalse("clone should not be reference to original", cld == copy);
        assertEquals(cld, copy);
    }

    public void testCloneAttributeDescriptors() throws Exception {
        ClassDescriptor cld = testModel
                .getClassDescriptorByName("org.intermine.model.testmodel.Employee");
        Set attrs = cld.getAttributeDescriptors();
        Set copy = ModelMerger.cloneAttributeDescriptors(attrs);
        assertEquals(attrs, copy);
    }

    public void testCloneCollectionDescriptors() throws Exception {
        ClassDescriptor cld = testModel
                .getClassDescriptorByName("org.intermine.model.testmodel.Company");
        Set colls = cld.getCollectionDescriptors();
        Set copy = ModelMerger.cloneCollectionDescriptors(colls);
        assertEquals(colls, copy);
    }

    public void testCloneReferenceDescriptors() throws Exception {
        ClassDescriptor cld = testModel
                .getClassDescriptorByName("org.intermine.model.testmodel.Company");
        Set refs = cld.getReferenceDescriptors();
        Set copy = ModelMerger.cloneReferenceDescriptors(refs);
        assertEquals(refs, copy);
    }

    public void testMergeClass() throws Exception {
        String modelStr = "<class name=\"org.intermine.model.testmodel.Department\" is-interface=\"false\"></class>";
        String addition = "<class name=\"org.intermine.model.testmodel.Department\" is-interface=\"true\"></class>";
        ClassDescriptor cld1 = parseClass(modelStr);
        ClassDescriptor cld2 = parseClass(addition);
        try {
            ModelMerger.mergeClass(cld1, cld2, emptyModel, Collections.EMPTY_SET);
            fail("Expected ModelMergerException with is-interface mismatch");
        } catch (ModelMergerException e) {
            //e.printStackTrace();
        }
    }

    // set superclass
    public void testMergeClassAddingInheritance() throws Exception {
        String modelStr = "<model name=\"testmodel\" namespace=\"testmodel#\">"
                            + "<class name=\"A\" is-interface=\"false\"></class>"
                            + "<class name=\"B\" is-interface=\"false\"></class>"
                         + "</model>";
        Model model = parser.process(new StringReader(modelStr));
        String addition = "<class name=\"A\" extends=\"B\" is-interface=\"false\"></class>";
        ClassDescriptor cld1 = model.getClassDescriptorByName("A");
        ClassDescriptor cld2 = (ClassDescriptor) parser
                .generateClassDescriptors(new StringReader(addition))
                .iterator().next();

        ClassDescriptor res = ModelMerger.mergeClass(cld1, cld2, model, Collections.singleton(cld2));
        Set supers = new HashSet();
        supers.add("B");

        assertEquals(supers, res.getSuperclassNames());
    }

    // Add another superclass
    public void testMergeClassReplacingInheritance() throws Exception {
        String modelStr = "<model name=\"testmodel\" namespace=\"testmodel#\">"
                            + "<class name=\"A\" extends=\"B\" is-interface=\"false\"></class>"
                            + "<class name=\"B\" is-interface=\"false\"></class>"
                            + "<class name=\"C\" is-interface=\"false\"></class>"
                         + "</model>";
        Model model = parser.process(new StringReader(modelStr));
        String addition = "<class name=\"A\" extends=\"C\" is-interface=\"false\"></class>";
        ClassDescriptor cld1 = model.getClassDescriptorByName("A");
        ClassDescriptor cld2 = parseClass(addition);
        
        ClassDescriptor res = ModelMerger.mergeClass(cld1, cld2, model, Collections.singleton(cld2));
        Set supers = new HashSet();
        supers.add("C");
        
        assertEquals(supers, res.getSuperclassNames());
    }

    public void testMergeClassReferences() throws Exception {
        String modelStr =
            "<class name=\"A\" is-interface=\"false\">"
            + "<reference name=\"company\" referenced-type=\"A\"/>"
            + "</class>";
        String addition =
            "<class name=\"A\" is-interface=\"false\">"
            + "<reference name=\"company\" referenced-type=\"A\"/>"
            + "</class>";
        String expXml =
            "<class name=\"A\" is-interface=\"false\">"
            + "<reference name=\"company\" referenced-type=\"A\"/>"
            + "</class>";
        ClassDescriptor cld1 = parseClass(modelStr);
        ClassDescriptor cld2 = parseClass(addition);
        ClassDescriptor expected = parseClass(expXml);
        
        Set result = ModelMerger.mergeReferences(cld1, cld2);
        assertEquals(expected.getReferenceDescriptors(), result);
    }

    
    private ClassDescriptor parseClass(String xml) throws Exception {
        return (ClassDescriptor) parser
                    .generateClassDescriptors(new StringReader(xml))
                    .iterator().next();
    }
    
    public void testMergeReferenceSettingReverseRef() throws Exception {
        String modelStr =
                "<class name=\"org.intermine.model.testmodel.Department\" is-interface=\"false\">"
                + "<reference name=\"company\" referenced-type=\"org.intermine.model.testmodel.Company\"/>"
                + "</class>";

        // add Department.company
        String addition = "<class name=\"org.intermine.model.testmodel.Department\" is-interface=\"false\">"
            + "<reference name=\"company\" referenced-type=\"org.intermine.model.testmodel.Company\" reverse-reference=\"dept\"/>"
                + "</class>";

        String expXml = "<class name=\"org.intermine.model.testmodel.Department\" is-interface=\"false\">"
            + "<reference name=\"company\" referenced-type=\"org.intermine.model.testmodel.Company\" reverse-reference=\"dept\"/>"
            + "</class>";
        
        ClassDescriptor cld1 = parseClass(modelStr);
        ClassDescriptor cld2 = parseClass(addition);
        ClassDescriptor expected = parseClass(expXml);
        
        Set result = ModelMerger.mergeReferences(cld1, cld2);

        assertEquals(expected.getReferenceDescriptors(), result);
    }
    
    public void testMergeCollectionSettingReverseRef() throws Exception {
        String modelStr =
                "<class name=\"org.intermine.model.testmodel.Department\" is-interface=\"false\">"
                + "<collection name=\"company\" referenced-type=\"org.intermine.model.testmodel.Company\"/>"
                + "</class>";

        // add Department.company
        String addition = "<class name=\"org.intermine.model.testmodel.Department\" is-interface=\"false\">"
            + "<collection name=\"company\" referenced-type=\"org.intermine.model.testmodel.Company\" reverse-reference=\"dept\"/>"
                + "</class>";

        String expXml = "<class name=\"org.intermine.model.testmodel.Department\" is-interface=\"false\">"
            + "<collection name=\"company\" referenced-type=\"org.intermine.model.testmodel.Company\" reverse-reference=\"dept\"/>"
            + "</class>";
        
        ClassDescriptor cld1 = parseClass(modelStr);
        ClassDescriptor cld2 = parseClass(addition);
        ClassDescriptor expected = parseClass(expXml);
        
        Set result = ModelMerger.mergeCollections(cld1, cld2);

        assertEquals(expected.getCollectionDescriptors(), result);
    }
    
    public void testMergeClassReferencesWithReverseRefs() throws Exception {
        String modelStr =
                "<class name=\"org.intermine.model.testmodel.Department\" is-interface=\"false\">"
                + "<reference name=\"company\" referenced-type=\"org.intermine.model.testmodel.Company\" reverse-reference=\"department\"/>"
                + "</class>";

        // add Department.company
        String addition = "<class name=\"org.intermine.model.testmodel.Department\" is-interface=\"false\">"
                + "<reference name=\"addition\" referenced-type=\"org.intermine.model.testmodel.Department\"/>"
                + "</class>";

        String expXml = "<class name=\"org.intermine.model.testmodel.Department\" is-interface=\"false\">"
                + "<reference name=\"company\" referenced-type=\"org.intermine.model.testmodel.Company\" reverse-reference=\"department\"/>"
                + "<reference name=\"addition\" referenced-type=\"org.intermine.model.testmodel.Department\"/>"
                + "</class>";

        ClassDescriptor cld1 = parseClass(modelStr);
        ClassDescriptor cld2 = parseClass(addition);
        ClassDescriptor expected = parseClass(expXml);
        
        Set result = ModelMerger.mergeReferences(cld1, cld2);

        assertEquals(expected.getReferenceDescriptors(), result);

        // test bad reverse-reference
        addition = "<class name=\"org.intermine.model.testmodel.Department\" is-interface=\"false\">"
                + "<reference name=\"company\" referenced-type=\"org.intermine.model.testmodel.Company\" reverse-reference=\"incorrect\"/>"
                + "</class>";

        cld2 = parseClass(addition);
        try {
            ModelMerger.mergeReferences(cld1, cld2);
            fail("Expected ModelMergerException with incorrect reverse-reference name");
        } catch (ModelMergerException e) {
            //e.printStackTrace();
        }

        // test bad reference type
        addition = "<class name=\"org.intermine.model.testmodel.Department\" is-interface=\"false\">"
                + "<reference name=\"company\" referenced-type=\"org.intermine.model.testmodel.BadType\" reverse-reference=\"department\"/>"
                + "</class>";

        cld2 = (ClassDescriptor) parser.generateClassDescriptors(
                new StringReader(addition)).iterator().next();
        try {
            ModelMerger.mergeReferences(cld1, cld2);
            fail("Expected ModelMergerException with incorrect reference type");
        } catch (ModelMergerException e) {
            //e.printStackTrace();
        }
    }

    public void testMergeClassCollections() throws Exception {
        String modelStr = "<class name=\"org.intermine.model.testmodel.Company\" is-interface=\"false\">"
                + "<collection name=\"departments\" referenced-type=\"org.intermine.model.testmodel.Department\" ordered=\"true\" reverse-reference=\"company\"/>"
                + "</class>";

        // add Department.company
        String addition = "<class name=\"org.intermine.model.testmodel.Company\" is-interface=\"false\">"
                + "<collection name=\"foo\" referenced-type=\"org.intermine.model.testmodel.Department\" ordered=\"true\"/>"
                + "</class>";

        String expXml = "<class name=\"org.intermine.model.testmodel.Company\" is-interface=\"false\">"
                + "<collection name=\"departments\" referenced-type=\"org.intermine.model.testmodel.Department\" ordered=\"true\" reverse-reference=\"company\"/>"
                + "<collection name=\"foo\" referenced-type=\"org.intermine.model.testmodel.Department\" ordered=\"true\"/>"
                + "</class>";

        ClassDescriptor cld1 = parseClass(modelStr);
        ClassDescriptor cld2 = parseClass(addition);
        ClassDescriptor expected = parseClass(expXml);
        
        Set result = ModelMerger.mergeCollections(cld1, cld2);
        
        System.out.println(result.toString());
        System.out.println(expected.toString());
        
        assertEquals(expected.getCollectionDescriptors(), result);

        // test bad reverse reference
        addition = "<class name=\"org.intermine.model.testmodel.Company\" is-interface=\"false\">"
                + "<collection name=\"departments\" referenced-type=\"org.intermine.model.testmodel.Department\" ordered=\"true\" reverse-reference=\"incorrect\"/>"
                + "</class>";

        cld2 = (ClassDescriptor) parser.generateClassDescriptors(new StringReader(addition)).iterator().next();
        try {
            ModelMerger.mergeCollections(cld1, cld2);
            fail("Expected ModelMergerException with incorrect reverse reference name");
        } catch (ModelMergerException e) {
            //e.printStackTrace();
        }

        // test bad type
        addition =    "<class name=\"org.intermine.model.testmodel.Company\" is-interface=\"false\">"
                        + "<collection name=\"departments\" referenced-type=\"org.intermine.model.testmodel.BadType\" ordered=\"true\" reverse-reference=\"company\"/>"
                    + "</class>";

        cld2 = (ClassDescriptor) parser.generateClassDescriptors(
                new StringReader(addition)).iterator().next();
        try {
            ModelMerger.mergeCollections(cld1, cld2);
            fail("Expected ModelMergerException with incorrect type");
        } catch (ModelMergerException e) {
            //e.printStackTrace();
        }
    }
    
    public void testCheckInheritance() throws Exception {
        String modelStr = "<model name=\"testmodel\" namespace=\"testmodel#\">"
                            + "<class name=\"A\" extends=\"C Ai\" is-interface=\"false\"></class>"
                            + "<class name=\"C\" is-interface=\"false\"></class>"
                            + "<class name=\"Ai\" is-interface=\"true\"></class>"
                        + "</model>";
        Model model = parser.process(new StringReader(modelStr));
        // model is fine
        //assertEquals(model, ModelMerger.checkInheritance(model));

        String addition = "<model name=\"testmodel\" namespace=\"testmodel#\">"
                           + "<class name=\"A\" extends=\"B\" is-interface=\"false\"></class>"
                           + "<class name=\"B\" extends=\"C\" is-interface=\"false\"></class>"
                        + "</model>";
        Set additionClds = parser.generateClassDescriptors(new StringReader(
                addition));

        String dodgyStr = "<model name=\"testmodel\" namespace=\"testmodel#\">"
                            + "<class name=\"A\" extends=\"B C Ai\" is-interface=\"false\"></class>"
                            + "<class name=\"B\" extends=\"C\" is-interface=\"false\"></class>"
                            + "<class name=\"C\" is-interface=\"false\"></class>"
                            + "<class name=\"Ai\" is-interface=\"true\"></class>"
                        + "</model>";
        Set dodgyClds = parser.generateClassDescriptors(new StringReader(dodgyStr));
        
        String expectedStr = "<model name=\"testmodel\" namespace=\"testmodel#\">"
            + "<class name=\"A\" extends=\"B C Ai\" is-interface=\"false\"></class>"
            + "<class name=\"B\" extends=\"C\" is-interface=\"false\"></class>"
            + "<class name=\"C\" is-interface=\"false\"></class>"
            + "<class name=\"Ai\" is-interface=\"true\"></class>"
        + "</model>";
        Set expectedClds = parser.generateClassDescriptors(new StringReader(dodgyStr));

       // Set fixedClds = ModelMerger.checkInheritance(dodgyClds, addition);
      //  assertEquals(expectedClds, fixedClds);
    }
}
