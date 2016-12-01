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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

/**
 * Test of OmimConverter class.
 *
 * @author Julie Sullivan
 *
 */
public class OmimConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    OmimConverter converter;
    MockItemWriter itemWriter;
    private Set<Item> storedItems;

    public OmimConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        itemWriter = new MockItemWriter(new HashMap<String, org.intermine.model.fulldata.Item>());
        converter = new OmimConverter(itemWriter, model);
        super.setUp();
    }

    /**
     * Basic test of converter functionality.
     * @throws Exception
     */
    public void testProcess() throws Exception {
        File tmp = new File(getClass().getClassLoader()
                .getResource("morbidmap.txt").toURI());
        File datadir = tmp.getParentFile();
        converter.process(datadir);
        converter.close();
        storedItems = itemWriter.getItems();
        //writeItemsFile(storedItems, "omim-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("OmimConverterTest_tgt.xml");
        assertEquals(expected, storedItems);
    }


}
