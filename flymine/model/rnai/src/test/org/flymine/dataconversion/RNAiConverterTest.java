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

import org.intermine.xml.full.FullParser;
import org.intermine.dataconversion.MockItemWriter;

public class RNAiConverterTest extends TestCase
{
    public void testProcess() throws Exception {
        String ENDL = "\n";
        String input = ",,,Gene ID,Phenotype,,CGC-approved gene name,,,,,,,Other gene name 1" + ENDL
            + ",,,AC7.1,Sck,,,,,,,," + ENDL
            + ",,,AC7.1,Ste,,,,,,,," + ENDL
            + ",,,AC7.2a,WT,,soc-2,,,,,,,sur-8," + ENDL
            + ",,,AC7.2a,WT,,soc-2,,,,,,,sur-8," + ENDL;
        input = input.replaceAll(",", "\t"); //just used commas for readibility
        
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        new RNAiConverter(new BufferedReader(new StringReader(input)), itemWriter).process();
        Set expected = new HashSet(FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/RNAiConverterTest.xml")));
        System.out.println(itemWriter.getItems());
        assertEquals(expected, itemWriter.getItems());
    }
}
