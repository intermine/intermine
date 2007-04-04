package org.intermine.dataconversion;

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

import org.intermine.metadata.Model;

public class OntologyUtilTest extends TestCase
{
    public void testGenerateClassNamesNull() throws Exception {
        assertNull(OntologyUtil.generateClassNames(null, null));
    }

    public void testGenerateClassNamesEmpty() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        assertEquals("", OntologyUtil.generateClassNames("", model));
    }

    public void testGenerateClassNamesSingle() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        assertEquals("org.intermine.model.testmodel.Company", OntologyUtil.generateClassNames(model.getNameSpace() + "Company", model));
    }

    public void testGenerateClassNamesMultiple() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        String classNames = " " + model.getNameSpace() + "Company " + model.getNameSpace() + "Department ";
        String expected = "org.intermine.model.testmodel.Company org.intermine.model.testmodel.Department";
        assertEquals(expected, OntologyUtil.generateClassNames(classNames, model));
    }
}
