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

import java.io.StringReader;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import org.intermine.xml.full.FullParser;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.dataconversion.FileConverter;

public class RNAiConverterTest extends TestCase
{
    public void testProcess() throws Exception {
        String ENDL = "\n";
        String input = ",,Taxon ID,Gene ID,Phenotype,,CGC-approved gene name,PubMedId,,,,,,Other gene name 1,Other gene name2" + ENDL
            + ",,6239,AC7.1,Sck,,,pmid:12529635,,,,," + ENDL
            + ",,6239,AC7.1,Ste,,,pmid:12529635,,,,," + ENDL
            + ",,6239,AC7.2a,WT,,soc-2,pmid:12529635,,,,,,sur-8," + ENDL
            + ",,6239,AC7.2a,WT,,soc-2,pmid:12529635,,,,,,sur-8," + ENDL
            + ",,6239,Flibble,WT,,,pmid:876,," + ENDL
            + ",,6239,Flibble,WT,,,pmid:678,," + ENDL;
        input = input.replaceAll(",", "\t"); //just used commas for readibility
        
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        FileConverter converter = new RNAiConverter(itemWriter);
        converter.process(new StringReader(input));
        converter.close();
        Set expected = new HashSet(FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/RNAiConverterTest.xml")));
        assertEquals(expected, itemWriter.getItems());
    }
}
