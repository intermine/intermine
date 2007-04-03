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
import java.util.Collection;
import java.util.HashMap;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.xml.full.FullParser;

public class WormBaseIdentifiersConverterTest extends ItemsTestCase
{
    private String ENDL = System.getProperty("line.separator");

    public WormBaseIdentifiersConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        String input = "WBGene00007167\tB0393.2\trbg-3" + ENDL
            + "WBGene00007168\tB0393.3\t" + ENDL;

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        FileConverter converter = new WormBaseIdentifiersConverter(itemWriter);
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        writeItemsFile(itemWriter.getItems(), "flybase-ids_tgt.xml");
        
        assertEquals(readItemSet("WormBaseIdentifiersConverterTest.xml"), itemWriter.getItems());
    }

    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("WormBaseIdentifiersConverterTest.xml"));
    }
}
