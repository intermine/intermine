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
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class PantherConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    PantherConverter converter;
    MockItemWriter itemWriter;

    public PantherConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap());
        converter = new PantherConverter(itemWriter, model);
    }

    public void testProcess() throws Exception {
        File srcFile = new File(getClass().getClassLoader().getResource("RefGenomeOrthologs.txt").toURI());
        converter.setCurrentFile(srcFile);
        converter.process(new FileReader(srcFile));

        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "panther-tgt-items.xml");

        Set expected = readItemSet("PantherConverterTest_tgt.xml");
        assertEquals(expected, itemWriter.getItems());
    }
}
