package org.intermine.xml;

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

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collections;

import org.intermine.util.DynamicUtil;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.*;

public class XmlHelperTest extends TestCase
{
    Model model;

    public void setUp() throws Exception {
        model = Model.getInstanceByName("testmodel");
    }

    public void testGetClassName() throws Exception {
        Employee e1 = new Employee();
        assertEquals("org.intermine.model.testmodel.Employee", XmlHelper.getClassName(e1, model));
        Company c1 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        assertEquals("", XmlHelper.getClassName(c1, model));
        Set set = new HashSet(Arrays.asList(new Object[] {Employee.class, Broke.class}));
        Object be1 = DynamicUtil.createObject(set);
        assertEquals("org.intermine.model.testmodel.Employee", XmlHelper.getClassName(be1, model));
        assertEquals("", XmlHelper.getClassName(new Object(), model));
    }


}
