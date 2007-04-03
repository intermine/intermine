package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;

/**
 * Test for translating MAGE data in fulldata Item format conforming to a source OWL definition
 * to fulldata Item format conforming to InterMine OWL definition.
 *
 * @author Wenyan Ji
 */

public class MageFlatFileConverterTest extends ItemsTestCase {
    MockItemWriter writer = new MockItemWriter(new LinkedHashMap());

    
    
    public MageFlatFileConverterTest(String arg) {
        super(arg);
    }


    public void testProcess() throws Exception {

        BufferedReader srcReader = new BufferedReader(new
            InputStreamReader(getClass().getClassLoader().getResourceAsStream("mageFlat.txt")));

        MageFlatFileConverter converter = new MageFlatFileConverter(writer);

        converter.process(srcReader);
        converter.close();

        // uncomment to write a new target items file
        //writeItemsFile(writer.getItems(), "mage-flat-tgt-items.xml");
        
        Set expected = readItemSet("mageFlatTgt.xml");

        assertEquals(expected, writer.getItems());
    }
}
