package org.flymine.objectstore.ojb;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.Map;
import java.util.HashMap;
import java.sql.SQLException;

public class MapResultSetTest extends TestCase
{
    private MapResultSet mrs;

    public MapResultSetTest(String arg) {
        super(arg);
    }

    public void setUp() {
        Map map = new HashMap();
        map.put("col1", new Integer(1));
        map.put("col2", "test");
        map.put("col3", new Double(0.4));
        map.put("col4", new Float(0.6));
        map.put("col5", null);
        mrs = new MapResultSet(map);
    }

    public void testNullConstructor() {
        try {
            new MapResultSet(null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testGet() throws Exception {
        assertEquals(1, mrs.getInt("col1"));
        assertEquals("test", mrs.getString("col2"));
        assertTrue(0.4d == mrs.getDouble("col3"));
        assertTrue(0.6f == mrs.getFloat("col4"));

        // Null column
        assertTrue(0 == mrs.getInt("col5"));
        assertTrue(null == mrs.getString("col5"));
        assertTrue(0.0d == mrs.getDouble("col5"));
        assertTrue(0.0f == mrs.getFloat("col5"));
    }

    public void testGetInvalidColumn() throws Exception {
        try {
            mrs.getInt("col99");
            fail("Expected: SQLException");
        } catch (SQLException e) {
        }
    }
}
