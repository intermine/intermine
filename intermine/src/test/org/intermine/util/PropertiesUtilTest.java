package org.flymine.util;

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

}
