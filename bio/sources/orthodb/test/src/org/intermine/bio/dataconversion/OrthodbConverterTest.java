package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

public class OrthodbConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    OrthodbConverter converter;
    MockItemWriter itemWriter;

    String taxonIds = "9606 10090 10116";

    public OrthodbConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new OrthodbConverter(itemWriter, model);
        converter.setOrthodbOrganisms(taxonIds);
    }

    public void testProcess() throws Exception {
        File srcFile = new File(getClass().getClassLoader().
                getResource("OrthoDB5_ALL_tabtext").toURI());
        converter.setCurrentFile(srcFile);
        converter.process(new FileReader(srcFile));

        converter.close();

        // uncomment to write out a new target items file
//        writeItemsFile(itemWriter.getItems(), "orthodb-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("OrthodbConverterTest_tgt.xml");
        assertEquals(expected, itemWriter.getItems());
    }
}
