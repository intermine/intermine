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

public class UniprotConverterTest extends ItemsTestCase
{
    private UniprotConverter converter;
    private MockItemWriter itemWriter;

    public UniprotConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new UniprotConverter(itemWriter, Model.getInstanceByName("genomic"));
        converter.rslv = IdResolverService.getMockIdResolver("Gene");
        converter.rslv.addResolverEntry("7227", "FBgn0000001", Collections.singleton("CG1111"));
        converter.rslv.addResolverEntry("7227", "FBgn0000002", Collections.singleton("CG2222"));
        super.setUp();
    }

    public void testProcess() throws Exception {
        // use test file to get /resources directory. there is probably a better
        // way to do this somehow
        File tmp = new File(getClass().getClassLoader()
                .getResource("UniprotConverterTest_tgt.xml").toURI());
        File datadir = tmp.getParentFile();
        converter.setCreatego("true");
        converter.setUniprotOrganisms("7227");
        converter.process(datadir);
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "uniprot-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("UniprotConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());
    }

//    public void testLoadTrembl() throws Exception {
//        String taxonid = "9606";
//        File datadir = new File("");
//        converter.setUniprotOrganisms(taxonid);
//        converter.setLoadtrembl("false");
//        File[] files = converter.parseFileNames(datadir.listFiles()).get(taxonid);
//        assertEquals(2, files.length);
//        assertTrue(files[0].getName().contains("sprot"));
//        assertNull(files[1]);
//    }
}
