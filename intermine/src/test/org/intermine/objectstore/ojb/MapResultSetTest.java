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
        assertTrue(0.4 == mrs.getDouble("col3"));
    }

    public void testGetInvalidColumn() throws Exception {
        try {
            mrs.getInt("col4");
            fail("Expected: SQLException");
        } catch (SQLException e) {
        }
    }
}
