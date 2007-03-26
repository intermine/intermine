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

import java.io.*;
import java.util.*;

import org.intermine.util.TextFileUtil.ObjectFormatter;

import junit.framework.*;

/**
 * Tests for the TextFileUtil class.
 *
 * @author Kim Rutherford
 */

public class TextFileUtilTest extends TestCase
{
    private ObjectFormatter objectFormatter;

    public TextFileUtilTest (String arg) {
        super(arg);

        // example formatter
        objectFormatter = new ObjectFormatter() {
            public String format(Object o) {
                if (o instanceof Map) {
                    return "\t,";
                } else {
                    return null;
                }
            }
        };
    }

    private List getTestRows() {
        List rows = new ArrayList();
        Map map = new HashMap();
        map.put("key1", "value1");
        map.put("key2",  new Integer(2));
        rows.add(Arrays.asList(new Object [] {
                                   new Integer(101), map,
                                   "string 103", "string 104, with comma"
                               }));
        rows.add(Arrays.asList(new Object [] {
                                   new Integer(201), map,
                                   "string 203", "string 204\t with tab", 
                               }));
        return rows;
    }

    public void testWriteTabDelimitedTable() throws Exception {
        ByteArrayOutputStream baos;
        String expected;
        String results;

        // test writing all columns in their natural order
        baos = new ByteArrayOutputStream();
        TextFileUtil.writeTabDelimitedTable(baos, getTestRows(),
                                            new int[] {0, 1, 2, 3},
                                            new boolean[] {true, true, true, true},
                                            100, objectFormatter);
        expected = "101\t\"\t,\"\tstring 103\tstring 104, with comma\n"
            + "201\t\"\t,\"\tstring 203\t\"string 204\t with tab\"\n";
        results = baos.toString();
        assertEquals(expected, results);

        // same as above - the nulls mean show all columns in their natural order
        baos = new ByteArrayOutputStream();
        TextFileUtil.writeTabDelimitedTable(baos, getTestRows(),
                                            null,
                                            null,
                                            100, objectFormatter);
        expected = "101\t\"\t,\"\tstring 103\tstring 104, with comma\n"
            + "201\t\"\t,\"\tstring 203\t\"string 204\t with tab\"\n";
        results = baos.toString();
        assertEquals(expected, results);


        baos = new ByteArrayOutputStream();
        TextFileUtil.writeTabDelimitedTable(baos, getTestRows(),
                                            new int[] {0, 1, 2, 3},
                                            new boolean[] {false, true, true, true},
                                            100, objectFormatter);
        expected = "\"\t,\"\tstring 103\tstring 104, with comma\n"
            + "\"\t,\"\tstring 203\t\"string 204\t with tab\"\n";
        results = baos.toString();
        assertEquals(expected, results);


        baos = new ByteArrayOutputStream();
        TextFileUtil.writeTabDelimitedTable(baos, getTestRows(),
                                            new int[] {3, 1, 2, 0},
                                            new boolean[] {true, true, true, true},
                                            100, objectFormatter);
        expected = "string 104, with comma\t\"\t,\"\tstring 103\t101\n"
            + "\"string 204\t with tab\"\t\"\t,\"\tstring 203\t201\n";
        results = baos.toString();
        assertEquals(expected, results);

        
        baos = new ByteArrayOutputStream();
        TextFileUtil.writeTabDelimitedTable(baos, getTestRows(),
                                            new int[] {3, 2, 1, 0},
                                            new boolean[] {false, true, true, false},
                                            100, objectFormatter);
        expected = "string 103\t\"\t,\"\nstring 203\t\"\t,\"\n";
        results = baos.toString();
        assertEquals(expected, results);

        
        baos = new ByteArrayOutputStream();
        TextFileUtil.writeTabDelimitedTable(baos, getTestRows(),
                                            new int[] {3, 2, 1, 0},
                                            new boolean[] {false, false, true, false},
                                            100, objectFormatter);
        expected = "\"\t,\"\n\"\t,\"\n";
        results = baos.toString();
        assertEquals(expected, results);
        
        // test writing a limited number of rows
        baos = new ByteArrayOutputStream();
        TextFileUtil.writeTabDelimitedTable(baos, getTestRows(),
                                            new int[] {0, 1, 2, 3},
                                            new boolean[] {true, true, true, true},
                                            1, objectFormatter);
        expected = "101\t\"\t,\"\tstring 103\tstring 104, with comma\n";
        results = baos.toString();
        assertEquals(expected, results);        
        
        // test writing all rows (-1 as maxRows)
        baos = new ByteArrayOutputStream();
        TextFileUtil.writeTabDelimitedTable(baos, getTestRows(),
                                            new int[] {0, 1, 2, 3},
                                            new boolean[] {true, true, true, true},
                                            -1, objectFormatter);
        expected = "101\t\"\t,\"\tstring 103\tstring 104, with comma\n"
            + "201\t\"\t,\"\tstring 203\t\"string 204\t with tab\"\n";;
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
                                   100, objectFormatter);
        expected = "101,\"\t,\",\"string 103\",\"string 104, with comma\"\n"
            + "201,\"\t,\",\"string 203\",\"string 204\t with tab\"\n";
        results = baos.toString();
        assertEquals(expected, results);


        baos = new ByteArrayOutputStream();
        TextFileUtil.writeCSVTable(baos, getTestRows(),
                                   new int[] {0, 1, 2, 3},
                                   new boolean[] {false, true, true, true},
                                   100, objectFormatter);
        expected = "\"\t,\",\"string 103\",\"string 104, with comma\"\n"
            + "\"\t,\",\"string 203\",\"string 204\t with tab\"\n";
        results = baos.toString();
        assertEquals(expected, results);


        baos = new ByteArrayOutputStream();
        TextFileUtil.writeCSVTable(baos, getTestRows(),
                                   new int[] {3, 1, 2, 0},
                                   new boolean[] {true, true, true, true},
                                   100, objectFormatter);
        expected = "\"string 104, with comma\",\"\t,\",\"string 103\",101\n"
            + "\"string 204\t with tab\",\"\t,\",\"string 203\",201\n";
        results = baos.toString();
        assertEquals(expected, results);

        
        baos = new ByteArrayOutputStream();
        TextFileUtil.writeCSVTable(baos, getTestRows(),
                                   new int[] {3, 2, 1, 0},
                                   new boolean[] {false, true, true, false},
                                   100, objectFormatter);
        expected = "\"string 103\",\"\t,\"\n\"string 203\",\"\t,\"\n";
        results = baos.toString();
        assertEquals(expected, results);

        
        baos = new ByteArrayOutputStream();
        TextFileUtil.writeCSVTable(baos, getTestRows(),
                                   new int[] {3, 2, 1, 0},
                                   new boolean[] {false, false, true, false},
                                   100, objectFormatter);
        expected = "\"\t,\"\n\"\t,\"\n";
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
