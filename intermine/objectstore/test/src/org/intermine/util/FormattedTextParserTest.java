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
