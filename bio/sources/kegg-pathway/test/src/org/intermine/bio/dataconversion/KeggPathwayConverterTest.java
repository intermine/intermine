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
        MockIdResolverFactory resolverFactory = new MockIdResolverFactory("Gene");
        resolverFactory.addResolverEntry("7227", "FBgn001", Collections.singleton("CG1004"));
        resolverFactory.addResolverEntry("7227", "FBgn002", Collections.singleton("CG10045"));
        resolverFactory.addResolverEntry("7227", "FBgn003", Collections.singleton("CG1007"));
        converter.resolverFactory = resolverFactory;
    }

    public void testProcess() throws Exception {
        File srcFile = new File(getClass().getClassLoader().getResource("map_title.tab").toURI());
        converter.setCurrentFile(srcFile);
        converter.process(new FileReader(srcFile));

        srcFile = new File(getClass().getClassLoader().getResource("dme/dme_gene_map.tab").toURI());
        converter.setCurrentFile(srcFile);
        converter.process(new FileReader(srcFile));

        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "kegg-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("KeggConverterTest_tgt.xml");
        assertEquals(expected, itemWriter.getItems());
    }
}
