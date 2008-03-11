package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStreamReader;
import java.util.HashMap;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class DrosophilaHomologyConverterTest extends ItemsTestCase
{
    public DrosophilaHomologyConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        FileConverter converter = new DrosophilaHomologyConverter(itemWriter,
                                                                    Model.getInstanceByName("genomic"));

        InputStreamReader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("DrosophilaHomologyConverterTest_src.tsv"));
        converter.process(reader);
        
        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "dros-homology-tgt.xml");

        assertEquals(readItemSet("DrosophilaHomologyConverterTest.xml"), itemWriter.getItems());
    }
}
