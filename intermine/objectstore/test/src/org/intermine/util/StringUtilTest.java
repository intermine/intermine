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

import junit.framework.TestCase;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

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

    public void testCapitalise() throws Exception {
        assertEquals("A", StringUtil.capitalise("a"));
        assertEquals("A", StringUtil.capitalise("A"));
        assertEquals("Aaaa", StringUtil.capitalise("aaaa"));
        assertEquals("AaaaBbbb", StringUtil.capitalise("aaaaBbbb"));
        assertEquals("", StringUtil.capitalise(""));
        assertNull(StringUtil.capitalise(null));
    }

    public void testToSameInitialCaseNull() throws Exception {
        try {
            StringUtil.toSameInitialCase(null, "dog");
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testToSameInitialCase() throws Exception {
        assertEquals("dog", StringUtil.toSameInitialCase("dog", null));
        assertEquals("a", StringUtil.toSameInitialCase("a", "dog"));
        assertEquals("A", StringUtil.toSameInitialCase("a", "Dog"));
        assertEquals("Ant", StringUtil.toSameInitialCase("ant", "D"));
        assertEquals("ant", StringUtil.toSameInitialCase("Ant", "d"));
    }

    public void testUniqueString() throws Exception {
        Set set = new HashSet();
        for (int i = 0; i < 100; i++) {
            String n = StringUtil.uniqueString();
            assertFalse(set.contains(n));
            set.add(n);
        }
    }

    public void testSplit() throws Exception {
        {
            String testString = "$_^abc$_^defaaa bbb ccc$_^zzzzz$_^";
            String[] resArray = StringUtil.split(testString, "$_^");
            List expected = Arrays.asList(new String [] {"", "abc", "defaaa bbb ccc", "zzzzz", ""});
            assertEquals(expected,  Arrays.asList(resArray));
        }

        {
            String testString = "";
            String[] resArray = StringUtil.split(testString, "splitter_string");
            assertEquals("", resArray[0]);
        }

        {
            String testString = "abc_def";
            String[] resArray = StringUtil.split(testString, "_");
            List expected = Arrays.asList(new String [] {"abc", "def"});
            assertEquals(expected,  Arrays.asList(resArray));
        }

        {
            String testString = "XXXXXX";
            String[] resArray = StringUtil.split(testString, "XXX");
            List expected = Arrays.asList(new String [] {"", "", ""});
            assertEquals(expected,  Arrays.asList(resArray));
        }

        {
            String testString = "XXXaXXXb";
            String[] resArray = StringUtil.split(testString, "XXX");
            List expected = Arrays.asList(new String [] {"", "a", "b"});
            assertEquals(expected,  Arrays.asList(resArray));
        }

        {
            String testString = " a b c XX d e f XX h i j XX";
            String[] resArray = StringUtil.split(testString, "XX");
            List expected = Arrays.asList(new String [] {" a b c ", " d e f ", " h i j ", ""});
            assertEquals(expected,  Arrays.asList(resArray));
        }

        try {
            String testString = "";
            StringUtil.split(testString, "");
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            StringUtil.split(null, "XXX");
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }

        try {
            StringUtil.split("XXX", null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }

        try {
            StringUtil.split(null, null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testDuplicateQuotes() throws Exception{
        assertEquals("it''s", StringUtil.duplicateQuotes("it's"));
    }

    public void testJoin() throws Exception {
        List list = new ArrayList();
        list.add("one");
        list.add("two");
        list.add("three");
        assertEquals("one, two, three", StringUtil.join(list, ", "));
    }

    public void testTokenize() throws Exception {
        try {
            StringUtil.tokenize(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        assertEquals(StringUtil.tokenize(""), new ArrayList());
        assertEquals(StringUtil.tokenize(" "), new ArrayList());
        assertEquals(StringUtil.tokenize(" one"), Arrays.asList(new Object[] {"one"}));
        assertEquals(StringUtil.tokenize(" one  two"), Arrays.asList(new Object[] {"one", "two"}));
    }


    public void testAllDigits() throws Exception {
        assertTrue(StringUtil.allDigits("123456"));
        assertTrue(StringUtil.allDigits("1.23456"));
        assertTrue(StringUtil.allDigits("-1.23456"));
        assertTrue(StringUtil.allDigits("11.23456"));
        assertFalse(StringUtil.allDigits("text"));
        assertFalse(StringUtil.allDigits("1234text"));
        assertFalse(StringUtil.allDigits(""));
        assertFalse(StringUtil.allDigits(null));
    }
    
    public void testReverseCapitalisation() throws Exception {
        assertEquals("a", StringUtil.reverseCapitalisation("A"));
        assertEquals("A", StringUtil.reverseCapitalisation("a"));
        assertEquals("aa", StringUtil.reverseCapitalisation("Aa"));
        assertEquals("AA", StringUtil.reverseCapitalisation("aA"));
        assertEquals("", StringUtil.reverseCapitalisation(""));
        assertNull(StringUtil.reverseCapitalisation(null));
    }

}
