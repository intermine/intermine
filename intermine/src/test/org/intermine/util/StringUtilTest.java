package org.flymine.util;

import junit.framework.*;

public class StringUtilTest extends TestCase
{
    public StringUtilTest(String arg1) {
        super(arg1);
    }

    public void testCountOccurancesNullStr() throws Exception {
        try {
            StringUtil.countOccurances(null, "A test string");
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testCountOccurancesNullTarget() throws Exception {
        try {
            StringUtil.countOccurances("e", null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testCountOccurances() throws Exception {
        assertEquals(0,StringUtil.countOccurances("z", "A sentence without the required letter in it"));
        assertEquals(8,StringUtil.countOccurances("e", "A sentence with the required letter in it"));
        assertEquals(1,StringUtil.countOccurances("e", "effffffff"));
        assertEquals(1,StringUtil.countOccurances("e", "ffffffffffffff  fffe"));
    }
}
