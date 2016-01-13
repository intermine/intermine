package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
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

import org.intermine.bio.dataconversion.IdResolverService;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

public class RnaiConverterTest extends ItemsTestCase
{
    MockItemWriter itemWriter;
    RnaiConverter converter;
    public RnaiConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new RnaiConverter(itemWriter, Model.getInstanceByName("genomic"));
        converter.rslv = IdResolverService.getMockIdResolver("Gene");
        converter.rslv.addResolverEntry("7227", "FBgn0015806", Collections.singleton("FBgn001"));
        converter.rslv.addResolverEntry("7227", "FBgn0053207", Collections.singleton("FBgn002"));
    }

    public void testProcess() throws Exception {

        File srcFile = new File(getClass().getClassLoader().getResource("all_screens_genomeRNAi.txt").toURI());
        converter.setCurrentFile(srcFile);
        converter.process(new FileReader(srcFile));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "rnai-converter-tgt.xml");

        assertEquals(readItemSet("RnaiConverterTest.xml"), itemWriter.getItems());
    }
}
