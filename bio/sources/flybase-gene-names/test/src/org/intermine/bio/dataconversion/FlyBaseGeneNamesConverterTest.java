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

public class FlyBaseGeneNamesConverterTest extends ItemsTestCase
{
    private String ENDL = System.getProperty("line.separator");

    public FlyBaseGeneNamesConverterTest(String arg) {
        super(arg);
    }

    public void testProcess() throws Exception {
        // data is in format:
        // FBgn | symbol | current fullname | fullname synonyms | symbol synonyms
        String input = "FBgn0004053\tzen\tzerknullt\tzerknullt 1,zerknullt" + ENDL
            + "FBgn0012699\tDpse\\Gld\tGlucose dehydrogenas" + ENDL;

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        FileConverter converter = new FlyBaseGeneNamesConverter(itemWriter);
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "flybase-gene-names_tgt.xml");

        assertEquals(readItemSet("FlyBaseGeneNamesConverterTest.xml"),
                     itemWriter.getItems());
    }
}
