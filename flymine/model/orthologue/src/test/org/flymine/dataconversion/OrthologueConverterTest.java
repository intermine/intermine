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

public class OrthologueConverterTest extends TestCase
{
    public void testProcess() throws Exception {
        String input = "14\t1217\tCE\t1.000\tO01438\n"
            + "14\t1217\tCE\t0.997\tQ95Q95\n"
            + "14\t1217\tSC\t1.000\tTOR2_YEAST\n"
            + "14\t1217\tSC\t0.566\tTOR1_YEAST\n";

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        new OrthologueConverter(new BufferedReader(new StringReader(input)), itemWriter).process();

        Set expected = new HashSet(FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/OrthologueConverterTest.xml")));

        assertEquals(expected, itemWriter.getItems());
    }
}
