package org.intermine.metadata;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import java.util.Map;
import java.util.HashMap;

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
    private Model model;

    public PrimaryKeyUtilTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        model = Model.getInstanceByName("testmodel");
    }


    public void testGetPrimaryKeysCld() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        Map expected = new HashMap();
        expected.put("key1", new PrimaryKey("key1", "name, address", cld));
        expected.put("key2", new PrimaryKey("key2", "vatNumber", cld));
        assertEquals(expected, PrimaryKeyUtil.getPrimaryKeys(cld));
    }

    public void testGetPrimaryKeysCldInherited() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Manager");
        Map expected = new HashMap();
        assertEquals(expected, PrimaryKeyUtil.getPrimaryKeys(cld));
    }


}
