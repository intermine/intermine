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

public class KeggPathwayConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    KeggPathwayConverter converter;
    MockItemWriter itemWriter;

    public KeggPathwayConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new KeggPathwayConverter(itemWriter, model);
        converter.setKeggOrganisms("7227 10090 9606");
        converter.rslv = IdResolverService.getMockIdResolver("Gene");
        converter.rslv.addResolverEntry("7227", "FBgn001", Collections.singleton("CG1004"));
        converter.rslv.addResolverEntry("10090", "MGI:007", Collections.singleton("100037258"));
        converter.rslv.addResolverEntry("9606", "PPARG", Collections.singleton("10"));
    }

    public void testProcess() throws Exception {
        File srcFile = new File(getClass().getClassLoader().getResource("map_title.tab").toURI());
        converter.setCurrentFile(srcFile);
        converter.process(new FileReader(srcFile));

        srcFile = new File(getClass().getClassLoader().getResource("dme/dme_gene_map.tab").toURI());
        converter.setCurrentFile(srcFile);
        converter.process(new FileReader(srcFile));

        srcFile = new File(getClass().getClassLoader().getResource("hsa/hsa_gene_map.tab").toURI());
        converter.setCurrentFile(srcFile);
        converter.process(new FileReader(srcFile));

        srcFile = new File(getClass().getClassLoader().getResource("mmu/mmu_gene_map.tab").toURI());
        converter.setCurrentFile(srcFile);
        converter.process(new FileReader(srcFile));

        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "kegg-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("KeggConverterTest_tgt.xml");
        assertEquals(expected, itemWriter.getItems());
    }
}
