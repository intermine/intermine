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

import java.io.StringReader;
import java.util.HashMap;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;

public class FlyBaseIdentifiersConverterTest extends ItemsTestCase
{
    private String ENDL = System.getProperty("line.separator");

    public FlyBaseIdentifiersConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        //
        String input = "ect\tFBgn0000451\tFBgn0010018,FBgn0036073,FBgn0036074\tCG6611\tCG11965" + ENDL
            + "Dpse\\GA10807\tFBgn0070863\t\tGA10807\t" + ENDL;

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        FileConverter converter = new FlyBaseIdentifiersConverter(itemWriter);
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(),"flybase-identifiers-tgt.xml");
        
        assertEquals(readItemSet("FlyBaseIdentifiersConverterTest.xml"), itemWriter.getItems());
    }
}
