package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
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

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class HugeGwasConverterTest extends ItemsTestCase
{
    private HugeGwasConverter converter;
    private MockItemWriter itemWriter;

    public HugeGwasConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap());
        converter = new HugeGwasConverter(itemWriter,
                Model.getInstanceByName("genomic"));
    }

    public void testProcess() throws Exception {
        File srcFile1 = new File(getClass().getClassLoader().getResource("New_IDs_to_Old_IDs-Genes.tsv").toURI());
        converter.setCurrentFile(srcFile1);
        converter.process(new FileReader(srcFile1));

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "anopheles-ids_tgt.xml");

        assertEquals(readItemSet("AnophelesIdentifiersConverterTest.xml"), itemWriter.getItems());
    }

    public void testParseSnp() throws Exception {
        assertEquals("rs10048146", converter.parseSnp("rs10048146(16q24.1)"));
        assertEquals("rs10048146", converter.parseSnp("rs10048146"));
    }
}
