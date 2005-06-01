package org.intermine.modelproduction.acedb;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;
import java.io.BufferedReader;
import java.io.StringReader;

public class ModelTokenStreamTest extends TestCase
{
    private String testString = "Flibble flobble\twotsit// flooble\n"
        + "     wurble //Wiglip\n"
        + "\t   \t flogblup\n"
        + "// gluplig\n"
        + "flutblog\n";

    public ModelTokenStreamTest(String arg) {
        super(arg);
    }

    public void testTokenStream() throws Exception {
        BufferedReader in = new BufferedReader(new StringReader(testString));
        ModelTokenStream stream = new ModelTokenStream(in);
        ModelNode node = stream.nextToken();
        assertEquals("Flibble", node.getName());
        assertEquals(0, node.getIndent());
        node = stream.nextToken();
        assertEquals("flobble", node.getName());
        assertEquals(8, node.getIndent());
        node = stream.nextToken();
        assertEquals("wotsit", node.getName());
        assertEquals(16, node.getIndent());
        node = stream.nextToken();
        assertEquals("wurble", node.getName());
        assertEquals(5, node.getIndent());
        node = stream.nextToken();
        assertEquals("flogblup", node.getName());
        assertEquals(17, node.getIndent());
        node = stream.nextToken();
        assertEquals("flutblog", node.getName());
        assertEquals(0, node.getIndent());
        node = stream.nextToken();
        assertNull(node);
        node = stream.nextToken();
        assertNull(node);
    }
}
