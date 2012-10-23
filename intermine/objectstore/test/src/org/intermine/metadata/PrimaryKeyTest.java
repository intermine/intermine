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

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class PrimaryKeyTest extends TestCase {
    public void testConstructorNull() throws Exception {
        try {
            new PrimaryKey("key", null, null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testConstructor() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        ClassDescriptor cld = model.getClassDescriptorByName("org.intermine.model.testmodel.Company");
        Set expected = new HashSet();
        expected.add("name");
        expected.add("vatNumber");
        assertEquals(expected, new PrimaryKey("key1", "name, vatNumber", cld).getFieldNames());
        try {
            new PrimaryKey("key1", "name, vatNumber", null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            new PrimaryKey("key1", "name, flibble", cld);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            new PrimaryKey("key1", "name, departments", cld);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }
}
