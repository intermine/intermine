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

import org.intermine.modelproduction.xml.InterMineModelParser;
import org.intermine.metadata.Model;

import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;


public class MergeModelsFunctionalTest extends XMLTestCase
{
    InterMineModelParser parser;


    public void setUp() throws Exception {
        XMLUnit.setIgnoreWhitespace(true);
        parser = new InterMineModelParser();
    }

    // functional test

    // test adding a class
    public void testAddClass() throws Exception {
        String modelStr = "<model name=\"testmodel\" namespace=\"testmodel#\"><class name=\"org.intermine.model.testmodel.Secretary\" is-interface=\"false\"><attribute name=\"name\" type=\"java.lang.String\"/></class></model>";
        Model model = parser.process(new StringReader(modelStr));

        String addition = "<class name=\"org.intermine.model.testmodel.Department\" extends=\"org.intermine.model.testmodel.RandomInterface\" is-interface=\"false\"><attribute name=\"name\" type=\"java.lang.String\"/></class>";
        Set additionClds = parser.generateClassDescriptors(new StringReader(addition));

        String expected = "<model name=\"testmodel\" namespace=\"testmodel#\"><class name=\"org.intermine.model.testmodel.Secretary\" is-interface=\"false\"><attribute name=\"name\" type=\"java.lang.String\"/></class><class name=\"org.intermine.model.testmodel.Department\" extends=\"org.intermine.model.testmodel.RandomInterface\" is-interface=\"false\"><attribute name=\"name\" type=\"java.lang.String\"/></class></model>";
        //assertXMLEqual(expected, ModelMerger.mergeModel(model, additionClds).toString());
    }


    // model:  A <-- C
    // addition: A <-- B <-- C
    // target should remove A <-- C
    public void testInheritanceChain() throws Exception {


    }


    // test adding subclass
    // + where subclass relation atlready exists
    public void testSubclass() throws Exception {
        String modelStr = "<model name=\"testmodel\" namespace=\"testmodel#\"><class name=\"org.intermine.model.testmodel.Secretary\" is-interface=\"false\"><attribute name=\"name\" type=\"java.lang.String\"/></class></model>";
        Model model = parser.process(new StringReader(modelStr));

        String addition = "<class name=\"org.intermine.model.testmodel.DepartmentSecretary\" extends=\"org.intermine.model.testmodel.Secretary\" is-interface=\"false\"><attribute name=\"department\" type=\"java.lang.String\"/></class>";
        Set additionClds = parser.generateClassDescriptors(new StringReader(addition));

        String expected = "<model name=\"testmodel\" namespace=\"testmodel#\"><class name=\"org.intermine.model.testmodel.Secretary\" is-interface=\"false\"><attribute name=\"name\" type=\"java.lang.String\"/></class>></model>";
        //assertXMLEqual(expected, ModelMerger.mergeModel(model, additionClds).toString());
    }

    // test adding a attribute to an existing class
    public void testAddAtribute() throws Exception {
        String modelStr = "<model name=\"testmodel\" namespace=\"testmodel#\"><class name=\"org.intermine.model.testmodel.Secretary\" is-interface=\"false\"><attribute name=\"name\" type=\"java.lang.String\"/></class></model>";
        Model model = parser.process(new StringReader(modelStr));

        String addition = "<class name=\"org.intermine.model.testmodel.Secretary\" is-interface=\"false\"><attribute name=\"age\" type=\"java.lang.Integer\"/></class>";
        Set additionClds = parser.generateClassDescriptors(new StringReader(addition));

        String expected = "<model name=\"testmodel\" namespace=\"testmodel#\"><class name=\"org.intermine.model.testmodel.Secretary\" is-interface=\"false\"><attribute name=\"name\" type=\"java.lang.String\"/><attribute name=\"age\" type=\"java.lang.Integer\"/></class></model>";
        //assertXMLEqual(expected, ModelMerger.mergeModel(model, additionClds).toString());
    }

    // test add reference

    // test add collection


    // test conflicting types
    public void testConflictingAttributes() throws Exception {
            String modelStr = "<model name=\"testmodel\" namespace=\"testmodel#\"><class name=\"org.intermine.model.testmodel.Secretary\" is-interface=\"false\"><attribute name=\"name\" type=\"java.lang.String\"/></class></model>";
        Model model = parser.process(new StringReader(modelStr));

        String addition = "<class name=\"org.intermine.model.testmodel.Secretary\" is-interface=\"false\"><attribute name=\"name\" type=\"java.lang.Integer\"/></class>";
        Set additionClds = parser.generateClassDescriptors(new StringReader(addition));

        try {
            ModelMerger.mergeModel(model, additionClds);
            fail("Expected ModelMergerException");
        } catch (ModelMergerException e) {
        }
    }

    // test conflicting references

    // test conflicting collections
}
