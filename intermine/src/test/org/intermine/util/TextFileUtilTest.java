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
}
