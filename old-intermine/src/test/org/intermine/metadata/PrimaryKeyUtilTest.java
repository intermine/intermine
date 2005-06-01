package org.intermine.metadata;

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

import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;

/**
 * Tests for PrimaryKeyUtil
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
        Set fields = PrimaryKeyUtil.getPrimaryKeyFields(model, c);
        Set testFieldNames = new HashSet();
        ClassDescriptor cld = model.getClassDescriptorByName(c.getName());
        FieldDescriptor fd = cld.getFieldDescriptorByName("name");
        testFieldNames.add(fd);
        assertEquals(testFieldNames, fields);
    }
}
