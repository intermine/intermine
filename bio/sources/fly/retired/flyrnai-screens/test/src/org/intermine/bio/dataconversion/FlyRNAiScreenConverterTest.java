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
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

public class FlyRNAiScreenConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    FlyRNAiScreenConverter converter;
    MockItemWriter itemWriter;

    public FlyRNAiScreenConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new FlyRNAiScreenConverter(itemWriter, model);
        converter.rslv = IdResolverService.getMockIdResolver("Gene");
        converter.rslv.addResolverEntry("7227", "FBgn001", Collections.singleton("CG31973"));
        converter.rslv.addResolverEntry("7227", "FBgn002", Collections.singleton("eve"));
    }

    public void testProcess() throws Exception {
        File srcFile = new File(getClass().getClassLoader().getResource("RNAi_screen_details").toURI());
        converter.setScreenDetailsFile(srcFile);


        srcFile = new File(getClass().getClassLoader().getResource("RNAi_all_hits.txt").toURI());
        converter.setCurrentFile(srcFile);
        converter.process(new FileReader(srcFile));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "flyrnai-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("FlyRNAiConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());
    }
}
