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

import java.util.HashMap;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

import java.io.InputStreamReader;
import java.io.Reader;

public class Drosophila2ProbeConverterTest extends ItemsTestCase
{
    private String ENDL = System.getProperty("line.separator");

    public Drosophila2ProbeConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {

        Reader reader = new InputStreamReader(getClass().getClassLoader()
                                              .getResourceAsStream("Drosophilia2ProbeConverterTest_src.csv"));
        
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        FileConverter converter = new Drosophila2ProbeConverter(itemWriter,
                                                                Model.getInstanceByName("genomic"));
        converter.process(reader);
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "affy-probes-tgt-items.xml");

        assertEquals(readItemSet("test/Drosophila2ProbeConverterTest.xml"), itemWriter.getItems());
    }

}
