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

import java.io.StringReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.lang.StringUtils;

import junit.framework.TestCase;

/**
 * Tests for the TextFileUtil class.
 *
 * @author Kim Rutherford
 */

public class FormattedTextParserTest extends TestCase
{

    public FormattedTextParserTest (String arg) {
        super(arg);
    }

    public void testParseDelimitedReader() throws Exception {
        String inputString =
            "# some comment\n"
            + "1.1|1.2|1.3\n"
            + "2.1|2.2|2.3\n"
            + "# another some comment\n"
            + "3.1|3.2|3.3\n";

        StringReader sr = new StringReader(inputString);

        Iterator iterator = FormattedTextParser.parseDelimitedReader(sr, '|');

        assertTrue(iterator.hasNext());
        String[] line0 = {
            "1.1", "1.2", "1.3"
        };
        String[] actual = (String[]) iterator.next();
        assertEquals(StringUtils.join(line0), StringUtils.join(actual));
        assertTrue(iterator.hasNext());
        String[] line1 = {
            "2.1", "2.2", "2.3"
        };
        actual = (String[]) iterator.next();
        assertEquals(StringUtils.join(line1), StringUtils.join(actual));
        assertTrue(iterator.hasNext());
        String[] line2 = {
            "3.1", "3.2", "3.3"
        };
        actual = (String[]) iterator.next();
        assertEquals(StringUtils.join(line2), StringUtils.join(actual));
        assertFalse(iterator.hasNext());

        try {
            iterator.next();
            fail("expected NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }
    }

    public void testParseTabDelimitedReader() throws Exception {
        String inputString =
            "# some comment\n"
            + "1.1\t1.2\t1.3\n"
            + "2.1\t2.2\t2.3\n"
            + "# another some comment\n"
            + "3.1\t3.2\t3.3\n";

        StringReader sr = new StringReader(inputString);

        Iterator iterator = FormattedTextParser.parseTabDelimitedReader(sr);

        assertTrue(iterator.hasNext());
        String[] line0 = {
            "1.1", "1.2", "1.3"
        };
        String[] actual = (String[]) iterator.next();
        assertTrue(Arrays.equals(line0, actual));
        assertTrue(iterator.hasNext());
        String[] line1 = {
            "2.1", "2.2", "2.3"
        };
        actual = (String[]) iterator.next();
        assertTrue(Arrays.equals(line1, actual));
        assertTrue(iterator.hasNext());
        String[] line2 = {
            "3.1", "3.2", "3.3"
        };
        actual = (String[]) iterator.next();
        assertTrue(Arrays.equals(line2, actual));
        assertFalse(iterator.hasNext());

        try {
            iterator.next();
            fail("expected NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }
    }

    public void testParseCSVDelimitedReader() throws Exception {
        String inputString = "\"one\", \"two\n\", \"three\"" + "\n";

        System.out.println(inputString);

        StringReader sr = new StringReader(inputString);

        Iterator iterator = FormattedTextParser.parseCsvDelimitedReader(sr);

        assertTrue(iterator.hasNext());
        String[] expected = {
            "one", "two\n", "three"
        };

        String[] actual = (String[]) iterator.next();
        assertEquals("bad parser", StringUtils.join(expected), StringUtils.join(actual));
    }
}
