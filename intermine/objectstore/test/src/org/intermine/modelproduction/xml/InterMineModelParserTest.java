package org.intermine.modelproduction.xml;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collections;
import java.io.StringReader;

import org.intermine.metadata.*;



public class InterMineModelParserTest extends TestCase
{
    private InterMineModelParser parser;
    private String PKG = "org.intermine.model.testmodel.";

    public void setUp() throws Exception {
        parser = new InterMineModelParser();
    }


    public void testProcess() throws Exception {
        Model model = parser.process(new StringReader(getModelXml()));
        Model expected = new Model("testmodel", "http://www.intermine.org/model/testmodel",
                                   getExpectedClds());
        assertEquals(expected, model);
    }

    public void testGenerateClassDescriptors() throws Exception {
        Model model = parser.process(new StringReader(getModelXml()));
        assertEquals(getExpectedClds(), model.getClassDescriptors());
    }


    private String getModelXml() {
        return "<model  name=\"testmodel\" namespace=\"http://www.intermine.org/model/testmodel#\"><class name=\"org.intermine.model.testmodel.Company\" extends=\"org.intermine.model.testmodel.HasAddress\" is-interface=\"true\"><attribute name=\"name\" type=\"java.lang.String\"/><collection name=\"departments\" referenced-type=\"org.intermine.model.testmodel.Department\" reverse-reference=\"company\"/></class><class name=\"org.intermine.model.testmodel.Department\" is-interface=\"false\"><reference name=\"company\" referenced-type=\"org.intermine.model.testmodel.Company\" reverse-reference=\"departments\"/></class><class name=\"org.intermine.model.testmodel.HasAddress\" is-interface=\"true\"></class></model>";
    }

    private Set getExpectedClds() {
        ClassDescriptor hasAddress = new ClassDescriptor(PKG + "HasAddress", null, true, new HashSet(), new HashSet(), new HashSet());
        AttributeDescriptor id = new AttributeDescriptor("id", "java.lang.Integer");
        ClassDescriptor intermineObject = new ClassDescriptor("org.intermine.model.InterMineObject", null, true,
                                                              new HashSet(Collections.singleton(id)), new HashSet(), new HashSet());
        AttributeDescriptor companyName = new AttributeDescriptor("name", "java.lang.String");
        CollectionDescriptor companyDepartments = new CollectionDescriptor("departments", PKG + "Department", "company");
        ClassDescriptor company = new ClassDescriptor(PKG + "Company", PKG + "HasAddress", true,
                                                      new HashSet(Collections.singleton(companyName)),
                                                      new HashSet(),
                                                      new HashSet(Collections.singleton(companyDepartments)));

        ReferenceDescriptor departmentCompany = new ReferenceDescriptor("company", PKG + "Company", "departments");
        ClassDescriptor department = new ClassDescriptor(PKG + "Department", null, false,
                                                         new HashSet(),
                                                         new HashSet(Collections.singleton(departmentCompany)),
                                                         new HashSet());;

        return new HashSet(Arrays.asList(new Object[] {intermineObject, hasAddress, company, department}));
    }

}
