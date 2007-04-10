package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;

public class FlyRNAiScreenConverterTest extends ItemsTestCase
{
    
    
    public FlyRNAiScreenConverterTest(String arg) {
        super(arg);
    }

    public void testProcess() throws Exception {
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        FlyRNAiScreenConverter converter = new FlyRNAiScreenConverter(itemWriter);
        File srcFile = new File(getClass().getClassLoader().getResource("FlyRNAiConverterTest.dataset").toURI());
        converter.setCurrentFile(srcFile);
        converter.taxonId = "7227";
        converter.process(new FileReader(srcFile));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "flyrnai-tgt-items.xml");

        Set expected = readItemSet("FlyRNAiConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());
    }
}
