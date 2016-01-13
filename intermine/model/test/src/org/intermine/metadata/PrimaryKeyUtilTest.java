package org.intermine.metadata;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * Tests for PrimaryKeyUtil
 *
 * @author Kim Rutherford
 */
public class PrimaryKeyUtilTest extends TestCase
{
    private Model model;
    private Map<String, PrimaryKey> expected;

    public PrimaryKeyUtilTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        model = Model.getInstanceByName("testmodel");
        expected = new HashMap<String, PrimaryKey>();
    }


    public void testGetPrimaryKeysCld() {
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        expected.put("key1", new PrimaryKey("key1", "name, address", cld));
        expected.put("key2", new PrimaryKey("key2", "vatNumber", cld));
        assertEquals(expected, PrimaryKeyUtil.getPrimaryKeys(cld));
    }

    public void testGetPrimaryKeysCldInherited() {
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Manager");
        assertEquals(expected, PrimaryKeyUtil.getPrimaryKeys(cld));
    }


}
