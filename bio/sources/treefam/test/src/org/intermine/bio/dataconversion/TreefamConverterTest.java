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
import java.io.FileOutputStream;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

/**
 * Unit test for TreefamConverter
 * @author IM
 *
 */
public class TreefamConverterTest extends ItemsTestCase
{
    private TreefamConverter converter;
    private MockItemWriter itemWriter;

    /**
     * Constructor
     * @param arg argument
     */
    public TreefamConverterTest(String arg) {
        super(arg);
    }

    @Override
    public void setUp() throws Exception {

        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new TreefamConverter(itemWriter, Model.getInstanceByName("genomic"));
        converter.rslv = IdResolverService.getMockIdResolver("Gene");
        converter.rslv.addResolverEntry("7227", "FBgn0004106", Collections.singleton("cdc2-RA"));
        converter.rslv.addResolverEntry("9606", "983", Collections.singleton("ENSG00000170312"));
        super.setUp();
    }

    /**
     * Test process
     * @throws Exception e
     */
    public void testProcess() throws Exception {

        File genes = File.createTempFile("genes", "");
        FileOutputStream out = new FileOutputStream(genes);
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("genes.txt.table"), out);
        out.close();

        ClassLoader loader = getClass().getClassLoader();
        String input = IOUtils.toString(loader.getResourceAsStream("ortholog.txt.table"));

        converter.setTreefamOrganisms("7227 9606");
        converter.setGeneFile(genes);
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        // writeItemsFile(itemWriter.getItems(), "treefam-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("TreefamConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());
    }
}
