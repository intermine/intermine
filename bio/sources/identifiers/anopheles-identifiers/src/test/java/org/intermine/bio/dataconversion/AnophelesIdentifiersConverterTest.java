package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
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

public class AnophelesIdentifiersConverterTest extends ItemsTestCase
{
    public AnophelesIdentifiersConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        BioFileConverter converter = new AnophelesIdentifiersConverter(itemWriter,
                                                                    Model.getInstanceByName("genomic"));

        File srcFile1 = new File(getClass().getClassLoader().getResource("New_IDs_to_Old_IDs-Genes.tsv").toURI());
        converter.setCurrentFile(srcFile1);
        converter.process(new FileReader(srcFile1));

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "anopheles-ids_tgt.xml");

        assertEquals(readItemSet("AnophelesIdentifiersConverterTest.xml"), itemWriter.getItems());
    }
}
