package org.intermine.metadata;

/*
 * Copyright (C) 2002-2007 FlyMine
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
        Set expected = new HashSet();
        expected.add("field1");
        expected.add("field2");
        assertEquals(expected, new PrimaryKey("key1", "field1, field2", null).getFieldNames());
    }
}
