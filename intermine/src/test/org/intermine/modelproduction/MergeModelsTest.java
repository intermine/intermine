package org.intermine.modelproduction;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.util.Set;
import java.util.HashSet;
import java.io.StringReader;
import junit.framework.TestCase;
import org.intermine.metadata.ClassDescriptor;

import org.intermine.modelproduction.xml.InterMineModelParser;
import org.intermine.metadata.Model;

import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;


public class MergeModelsTest extends TestCase //XMLTestCase
{
    Model testModel;
    InterMineModelParser parser;


    public void setUp() throws Exception {
        parser = new InterMineModelParser();
        testModel = Model.getInstanceByName("testmodel");
    }
    
    public void testCloneClassDescriptor() throws Exception {
        ClassDescriptor cld = testModel.getClassDescriptorByName("org.intermine.model.testmodel.Employee");
        ClassDescriptor copy = ModelMerger.cloneClassDescriptor(cld);
        assertFalse("clone should not be reference to original", cld == copy);
        assertEquals(cld, copy);
    }
    
    public void testCloneAttributeDescriptors() throws Exception {
        ClassDescriptor cld = testModel.getClassDescriptorByName("org.intermine.model.testmodel.Employee");
        Set attrs = cld.getAttributeDescriptors();
        Set copy = ModelMerger.cloneAttributeDescriptors(attrs);
        assertEquals(attrs, copy);
    }
    
    public void testCloneCollectionDescriptors() throws Exception {
        ClassDescriptor cld = testModel.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        Set colls = cld.getCollectionDescriptors();
        Set copy = ModelMerger.cloneCollectionDescriptors(colls);
        assertEquals(colls, copy);
    }
    
    public void testCloneReferenceDescriptors() throws Exception {
        ClassDescriptor cld = testModel.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        Set refs = cld.getReferenceDescriptors();
        Set copy = ModelMerger.cloneReferenceDescriptors(refs);
        assertEquals(refs, copy);
    }
    
    public void testMergeClass() throws Exception {
        String modelStr = "<class name=\"org.intermine.model.testmodel.Department\" is-interface=\"false\"></class>";
        String addition = "<class name=\"org.intermine.model.testmodel.Department\" is-interface=\"true\"></class>";
        ClassDescriptor cld1 = (ClassDescriptor) parser.generateClassDescriptors(new StringReader(modelStr)).iterator().next();
        ClassDescriptor cld2 = (ClassDescriptor) parser.generateClassDescriptors(new StringReader(addition)).iterator().next();
        try {
            ModelMerger.mergeClass(cld1, cld2);
            fail("Expected ModelMergerException with is-interface mismatch");
        } catch (ModelMergerException e) {
            e.printStackTrace();
        }
    }
    
    public void testMergeClassReferences() throws Exception {
        String modelStr = "<class name=\"org.intermine.model.testmodel.Department\" is-interface=\"false\">" +
                                "<reference name=\"company\" referenced-type=\"org.intermine.model.testmodel.Company\" reverse-reference=\"department\"/>" +
                            "</class>";

        // add Department.company
        String addition =   "<class name=\"org.intermine.model.testmodel.Department\" is-interface=\"false\">" +
                                "<reference name=\"addition\" referenced-type=\"org.intermine.model.testmodel.Department\"/>" +
                            "</class>";

        String expXml =   "<class name=\"org.intermine.model.testmodel.Department\" is-interface=\"false\">" +
                                "<reference name=\"company\" referenced-type=\"org.intermine.model.testmodel.Company\" reverse-reference=\"department\"/>" +
                                "<reference name=\"addition\" referenced-type=\"org.intermine.model.testmodel.Department\"/>" +
                            "</class>";
        
        ClassDescriptor cld1 = (ClassDescriptor) parser.generateClassDescriptors(new StringReader(modelStr)).iterator().next();
        ClassDescriptor cld2 = (ClassDescriptor) parser.generateClassDescriptors(new StringReader(addition)).iterator().next();
        ClassDescriptor expected = (ClassDescriptor) parser.generateClassDescriptors(new StringReader(expXml)).iterator().next();
        Set result = ModelMerger.mergeReferences(cld1, cld2);
        
        assertEquals(expected.getReferenceDescriptors(), result);
        
        // test bad reverse-reference
        addition =  "<class name=\"org.intermine.model.testmodel.Department\" is-interface=\"false\">" +
                        "<reference name=\"company\" referenced-type=\"org.intermine.model.testmodel.Company\" reverse-reference=\"incorrect\"/>" +
                    "</class>";
        
        cld2 = (ClassDescriptor) parser.generateClassDescriptors(new StringReader(addition)).iterator().next();
        try {
            ModelMerger.mergeReferences(cld1, cld2);
            fail("Expected ModelMergerException with incorrect reverse-reference name");
        } catch (ModelMergerException e) {
            e.printStackTrace();
        }
        
        // test bad reference type
        addition =  "<class name=\"org.intermine.model.testmodel.Department\" is-interface=\"false\">" +
                        "<reference name=\"company\" referenced-type=\"org.intermine.model.testmodel.BadType\" reverse-reference=\"department\"/>" +
                    "</class>";
        
        cld2 = (ClassDescriptor) parser.generateClassDescriptors(new StringReader(addition)).iterator().next();
        try {
            ModelMerger.mergeReferences(cld1, cld2);
            fail("Expected ModelMergerException with incorrect reference type");
        } catch (ModelMergerException e) {
            e.printStackTrace();
        }
    }
    
    public void testMergeClassCollections() throws Exception {
        String modelStr = "<class name=\"org.intermine.model.testmodel.Company\" is-interface=\"false\">" +
                                "<collection name=\"departments\" referenced-type=\"org.intermine.model.testmodel.Department\" ordered=\"true\" reverse-reference=\"company\"/>" +
                            "</class>";

        // add Department.company
        String addition =   "<class name=\"org.intermine.model.testmodel.Company\" is-interface=\"false\">" +
                                "<collection name=\"foo\" referenced-type=\"org.intermine.model.testmodel.Department\" ordered=\"true\"/>" +
                            "</class>";

        String expXml =   "<class name=\"org.intermine.model.testmodel.Company\" is-interface=\"false\">" +
                                "<collection name=\"departments\" referenced-type=\"org.intermine.model.testmodel.Department\" ordered=\"true\" reverse-reference=\"company\"/>" +
                                "<collection name=\"foo\" referenced-type=\"org.intermine.model.testmodel.Department\" ordered=\"true\"/>" +
                            "</class>";
        
        ClassDescriptor cld1 = (ClassDescriptor) parser.generateClassDescriptors(new StringReader(modelStr)).iterator().next();
        ClassDescriptor cld2 = (ClassDescriptor) parser.generateClassDescriptors(new StringReader(addition)).iterator().next();
        ClassDescriptor expected = (ClassDescriptor) parser.generateClassDescriptors(new StringReader(expXml)).iterator().next();
        Set result = ModelMerger.mergeCollections(cld1, cld2);
        
        assertEquals(expected.getCollectionDescriptors(), result);
        
        // test bad ordering
        addition =  "<class name=\"org.intermine.model.testmodel.Company\" is-interface=\"false\">" +
                        "<collection name=\"departments\" referenced-type=\"org.intermine.model.testmodel.Department\" ordered=\"false\" reverse-reference=\"company\"/>" +
                    "</class>";
        
        cld2 = (ClassDescriptor) parser.generateClassDescriptors(new StringReader(addition)).iterator().next();
        try {
            ModelMerger.mergeCollections(cld1, cld2);
            fail("Expected ModelMergerException with incorrect ordered attribute");
        } catch (ModelMergerException e) {
            e.printStackTrace();
        }
        
        // test bad reverse reference
        addition =  "<class name=\"org.intermine.model.testmodel.Company\" is-interface=\"false\">" +
                        "<collection name=\"departments\" referenced-type=\"org.intermine.model.testmodel.Department\" ordered=\"true\" reverse-reference=\"incorrect\"/>" +
                    "</class>";
        
        cld2 = (ClassDescriptor) parser.generateClassDescriptors(new StringReader(addition)).iterator().next();
        try {
            ModelMerger.mergeCollections(cld1, cld2);
            fail("Expected ModelMergerException with incorrect reverse reference name");
        } catch (ModelMergerException e) {
            e.printStackTrace();
        }
        
        // test bad type
        addition =  "<class name=\"org.intermine.model.testmodel.Company\" is-interface=\"false\">" +
                        "<collection name=\"departments\" referenced-type=\"org.intermine.model.testmodel.BadType\" ordered=\"true\" reverse-reference=\"company\"/>" +
                    "</class>";
        
        cld2 = (ClassDescriptor) parser.generateClassDescriptors(new StringReader(addition)).iterator().next();
        try {
            ModelMerger.mergeCollections(cld1, cld2);
            fail("Expected ModelMergerException with incorrect type");
        } catch (ModelMergerException e) {
            e.printStackTrace();
        }
    }
}
