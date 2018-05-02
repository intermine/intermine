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

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

public class UberflyConverterTest extends ItemsTestCase
{

    Model model = Model.getInstanceByName("genomic");
    UberflyConverter converter;
    MockItemWriter itemWriter;

    public UberflyConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new UberflyConverter(itemWriter, model);
        converter.rslv = IdResolverService.getMockIdResolver("Gene");
        converter.rslv.addResolverEntry("7227", "FBgn0000003", Collections.singleton("FBgn0000003"));
    }

    public void testProcess() throws Exception {
        File srcFile = new File(getClass().getClassLoader().getResource("HTD_modENCODE_BinData_2010-10-05.txt").toURI());
        File stages = new File(getClass().getClassLoader().getResource("stages.txt").toURI());


        converter.setCurrentFile(srcFile);
        converter.process(new FileReader(srcFile));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "uberfly-tgt-items.xml");

        assertEquals(readItemSet("UberflyConverterTest_tgt.xml"), itemWriter.getItems());
    }
}
