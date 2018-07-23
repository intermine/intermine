package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2018 FlyMine
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
        converter.rslv.addResolverEntry("7227", "FBgn0024733", Collections.singleton("FBgn0024733"));
    }

    public void testProcess() throws Exception {
        File metadata = new File(getClass().getClassLoader().getResource("metadata.tsv").toURI());
        File geneFile = new File(getClass().getClassLoader().getResource("data/SRX193500.tsv").toURI());

        converter.setUberflyMetadataFile(metadata);

        File tmp = new File(getClass().getClassLoader()
                .getResource("data/SRX193500.tsv").toURI());

        File datadir = tmp.getParentFile();
        converter.process(datadir);
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "uberfly-tgt-items.xml");

        assertEquals(readItemSet("UberflyConverterTest_tgt.xml"), itemWriter.getItems());
    }
}
