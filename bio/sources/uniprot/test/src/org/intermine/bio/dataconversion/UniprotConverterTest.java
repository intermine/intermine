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

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class UniprotConverterTest extends ItemsTestCase
{
    public UniprotConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {

        Reader reader = new InputStreamReader(getClass().getClassLoader()
                                              .getResourceAsStream("UniprotConverterTest_src.xml"));

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        UniprotConverter converter = new UniprotConverter(itemWriter,
                                                          Model.getInstanceByName("genomic"));
        converter.setCreateinterpro("true"); 
        converter.process(reader);
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "uniprot-tgt-items.xml");

        Set expected = readItemSet("UniprotConverterTest_tgt.xml");
        
        assertEquals(expected, itemWriter.getItems());
    }
}
