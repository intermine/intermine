package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import org.flymine.xml.full.FullParser;

public class RNAiConverterTest extends TestCase
{
    public void testProcess() throws Exception {
        String input = "\t\t\tGene ID\tPhenotype\n"
            + "\t\t\tAC7.1\tSck\n"
            + "\t\t\tAC7.1\tSte\n"
            + "\t\t\tAC7.2a\tWT\n"
            + "\t\t\tAC7.2a\tWT\n";

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        new RNAiConverter(new BufferedReader(new StringReader(input)), itemWriter).process();
        Set expected = new HashSet(FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/RNAiConverterTest.xml")));

        assertEquals(expected, itemWriter.getItems());
    }
}
