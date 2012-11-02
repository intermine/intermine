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
        converter.rslv.addResolverEntry("10116", "RGD:2687", Collections.singleton("ENSRNOG00000003858"));  //1060048
        converter.rslv.addResolverEntry("10116", "RGD:1111", Collections.singleton("ENSRNOG00000003611"));  //1060071
        converter.rslv.addResolverEntry("10116", "RGD:2222", Collections.singleton("ENSRNOG00000028887"));  //1060083
        converter.rslv.addResolverEntry("10116", "RGD:3333", Collections.singleton("ENSRNOG00000031952"));               // 1060082
        super.setUp();
    }

    /**
     * Test process
     * @throws Exception e
     */
    public void testProcess() throws Exception {

        File genes = File.createTempFile("genes", "");
        FileOutputStream out = new FileOutputStream(genes);
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("rat"), out);
        out.close();

        ClassLoader loader = getClass().getClassLoader();
        String input = IOUtils.toString(loader.getResourceAsStream("ortholog.txt.table"));

        converter.setTreefamOrganisms("10116");
        converter.setGeneFile(genes);
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        writeItemsFile(itemWriter.getItems(), "treefam-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("TreefamConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());
    }
}
