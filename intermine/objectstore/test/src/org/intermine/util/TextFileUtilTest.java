package org.intermine.util;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.*;
import java.util.*;

import junit.framework.*;

/**
 * Tests for the TextFileUtil class.
 *
 * @author Kim Rutherford
 */

public class TextFileUtilTest extends TestCase
{
    public TextFileUtilTest (String arg) {
        super(arg);
    }

    private List getTestRows() {
        List rows = new ArrayList();
        rows.add(Arrays.asList(new Object [] {
                                   new Integer(101), new Integer(102),
                                   "string 103", "string 104, with comma"
                               }));
        rows.add(Arrays.asList(new Object [] {
                                   new Integer(201), new Integer(202),
                                   "string 203", "string 204\t with tab"
                               }));
        return rows;
    }

    public void testWriteTabDelimitedTable() throws Exception {
        ByteArrayOutputStream baos;
        String expected;
        String results;

        baos = new ByteArrayOutputStream();
        TextFileUtil.writeTabDelimitedTable(baos, getTestRows(),
                                            new int[] {0, 1, 2, 3},
                                            new boolean[] {true, true, true, true},
                                            100);
        expected = "101\t102\t\"string 103\"\t\"string 104, with comma\"\n"
            + "201\t202\t\"string 203\"\t\"string 204\t with tab\"\n";
        results = baos.toString();
        assertEquals(expected, results);


        baos = new ByteArrayOutputStream();
        TextFileUtil.writeTabDelimitedTable(baos, getTestRows(),
                                            new int[] {0, 1, 2, 3},
                                            new boolean[] {false, true, true, true},
                                            100);
        expected = "102\t\"string 103\"\t\"string 104, with comma\"\n"
            + "202\t\"string 203\"\t\"string 204\t with tab\"\n";
        results = baos.toString();
        assertEquals(expected, results);


        baos = new ByteArrayOutputStream();
        TextFileUtil.writeTabDelimitedTable(baos, getTestRows(),
                                            new int[] {3, 1, 2, 0},
                                            new boolean[] {true, true, true, true},
                                            100);
        expected = "\"string 104, with comma\"\t102\t\"string 103\"\t101\n"
            + "\"string 204\t with tab\"\t202\t\"string 203\"\t201\n";
        results = baos.toString();
        assertEquals(expected, results);

        
        baos = new ByteArrayOutputStream();
        TextFileUtil.writeTabDelimitedTable(baos, getTestRows(),
                                            new int[] {3, 2, 1, 0},
                                            new boolean[] {false, true, true, false},
                                            100);
        expected = "\"string 103\"\t102\n\"string 203\"\t202\n";
        results = baos.toString();
        assertEquals(expected, results);

        
        baos = new ByteArrayOutputStream();
        TextFileUtil.writeTabDelimitedTable(baos, getTestRows(),
                                            new int[] {3, 2, 1, 0},
                                            new boolean[] {false, false, true, false},
                                            100);
        expected = "102\n202\n";
        results = baos.toString();
        assertEquals(expected, results);
    }

    public void testWriteCommaDelimitedTable() throws Exception {
        ByteArrayOutputStream baos;
        String expected;
        String results;

        baos = new ByteArrayOutputStream();
        TextFileUtil.writeCSVTable(baos, getTestRows(),
                                   new int[] {0, 1, 2, 3},
                                   new boolean[] {true, true, true, true},
                                   100);
        expected = "101,102,\"string 103\",\"string 104, with comma\"\n"
            + "201,202,\"string 203\",\"string 204\t with tab\"\n";
        results = baos.toString();
        assertEquals(expected, results);


        baos = new ByteArrayOutputStream();
        TextFileUtil.writeCSVTable(baos, getTestRows(),
                                   new int[] {0, 1, 2, 3},
                                   new boolean[] {false, true, true, true},
                                   100);
        expected = "102,\"string 103\",\"string 104, with comma\"\n"
            + "202,\"string 203\",\"string 204\t with tab\"\n";
        results = baos.toString();
        assertEquals(expected, results);


        baos = new ByteArrayOutputStream();
        TextFileUtil.writeCSVTable(baos, getTestRows(),
                                   new int[] {3, 1, 2, 0},
                                   new boolean[] {true, true, true, true},
                                   100);
        expected = "\"string 104, with comma\",102,\"string 103\",101\n"
            + "\"string 204\t with tab\",202,\"string 203\",201\n";
        results = baos.toString();
        assertEquals(expected, results);

        
        baos = new ByteArrayOutputStream();
        TextFileUtil.writeCSVTable(baos, getTestRows(),
                                   new int[] {3, 2, 1, 0},
                                   new boolean[] {false, true, true, false},
                                   100);
        expected = "\"string 103\",102\n\"string 203\",202\n";
        results = baos.toString();
        assertEquals(expected, results);

        
        baos = new ByteArrayOutputStream();
        TextFileUtil.writeCSVTable(baos, getTestRows(),
                                   new int[] {3, 2, 1, 0},
                                   new boolean[] {false, false, true, false},
                                   100);
        expected = "102\n202\n";
        results = baos.toString();
        assertEquals(expected, results);
    }

    /**
     * Test quoting a string with a quote in.
     */
    public void testWriteQuoted() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);

        TextFileUtil.writeQuoted(ps, "quoted 'String' with a quote: \"");

        ps.close();
        baos.close();

        assertEquals("\"quoted 'String' with a quote: \"\"\"", baos.toString());
    }

    /**
     * Test quoting a string that's null.
     */
    public void testWriteQuotedNull() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);

        TextFileUtil.writeQuoted(ps, null);

        ps.close();
        baos.close();

        assertEquals("\"null\"", baos.toString());
    }

    public void testParseTabDelimitedReader() throws Exception {
        String inputString =
            "# some comment\n"
            + "1.1\t1.2\t1.3\n"
            + "2.1\t2.2\t2.3\n"
            + "# another some comment\n"
            + "3.1\t3.2\t3.3\n";

        StringReader sr = new StringReader(inputString);

        Iterator iterator = TextFileUtil.parseTabDelimitedReader(sr);

        assertTrue(iterator.hasNext());
        String[] line0 = {
            "1.1", "1.2", "1.3"
        };
        assertTrue(Arrays.equals(line0, (Object[]) iterator.next()));
        assertTrue(iterator.hasNext());
        String[] line1 = {
            "2.1", "2.2", "2.3"
        };
        assertTrue(Arrays.equals(line1, (Object[]) iterator.next()));
        assertTrue(iterator.hasNext());
        String[] line2 = {
            "3.1", "3.2", "3.3"
        };
        assertTrue(Arrays.equals(line2, (Object[]) iterator.next()));
        assertFalse(iterator.hasNext());

        try {
            iterator.next();
            fail("expected NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }
    }
}
