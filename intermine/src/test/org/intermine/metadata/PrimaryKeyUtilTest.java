package org.intermine.metadata;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;

import servletunit.struts.MockStrutsTestCase;

import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.model.testmodel.*;

/**
 * Tests for PrimaryKeyUtil.
 *
 * @author Kim Rutherford
 */

public class PrimaryKeyUtilTest extends TestCase
{
    public PrimaryKeyUtilTest(String arg) {
        super(arg);
    }

    public void testGetPrimaryKeyFields() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        Class c = Class.forName("org.intermine.model.testmodel.Employee");
        List fields = PrimaryKeyUtil.getPrimaryKeyFields(model, c);
        ArrayList testFieldNames = new ArrayList();
        testFieldNames.add("id");
        testFieldNames.add("name");
        assertEquals(testFieldNames, fields);
    }
}
