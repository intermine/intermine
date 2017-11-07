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
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

public class GtexConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    GtexConverter converter;
    MockItemWriter itemWriter;
    private final String targetFile = "GtexConverterTest_tgt.xml";

    public GtexConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new GtexConverter(itemWriter, model);
        converter.rslv = IdResolverService.getMockIdResolver("Gene");
        converter.rslv.addResolverEntry("9606", "123", Collections.singleton("ENSG00000225880"));
        converter.rslv.addResolverEntry("9606", "3456", Collections.singleton("ENSG00000238009"));
    }

    public void testProcess() throws Exception {

        File tmp = new File(getClass().getClassLoader().getResource(targetFile).toURI());
        File datadir = tmp.getParentFile();
        converter.process(datadir);
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "gtex-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet(targetFile);

        assertEquals(expected, itemWriter.getItems());
    }
}
