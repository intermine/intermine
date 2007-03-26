package org.intermine.util;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;
import junit.framework.*;

public class PropertiesUtilTest extends TestCase
{
    Properties props;

    public PropertiesUtilTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        props = new Properties();
        props.put("testprop.name", "myname");
        props.put("testprop.address", "myaddress");
        props.put("anotherprop.address", "anotheraddress");
    }

    public void testStartingWithNullStr() throws Exception {
        try {
            PropertiesUtil.getPropertiesStartingWith(null, new Properties());
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testStartingWithNullProperties() throws Exception {
        try {
            PropertiesUtil.getPropertiesStartingWith("blahblah", null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testStartingWithExists() throws Exception {

        Properties p = PropertiesUtil.getPropertiesStartingWith("testprop", props);

        assertEquals(2, p.size());
        assertEquals("myname", p.get("testprop.name"));
        assertEquals("myaddress", p.get("testprop.address"));

    }

    public void testStartingWithNotExists() throws Exception {

        Properties p = PropertiesUtil.getPropertiesStartingWith("nothing", props);

        assertEquals(0, p.size());
    }

    public void testStripStartNullProps() throws Exception {
        try {
            PropertiesUtil.stripStart(null, new Properties());
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }

    }

    public void testStripStartNullProperties() throws Exception {
        try {
            PropertiesUtil.stripStart("blahblah", null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testStripStartExists() throws Exception {

        Properties p1 = PropertiesUtil.getPropertiesStartingWith("testprop", props);
        Properties p2 = PropertiesUtil.stripStart("testprop", p1);

        assertEquals(2, p2.size());
        assertEquals("myname", p2.get("name"));
        assertEquals("myaddress", p2.get("address"));

    }

    public void testStripStartNotExists() throws Exception {

        Properties p = PropertiesUtil.stripStart("nothing", props);

        assertEquals(0, p.size());
    }

    public void testLoadInvalid() throws Exception {
        try {
            Properties p = PropertiesUtil.loadProperties("invalidTest.properties");
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Cannot override non-overrideable property flibble = flobble with new value flooble", e.getMessage());
        }
    }
}
