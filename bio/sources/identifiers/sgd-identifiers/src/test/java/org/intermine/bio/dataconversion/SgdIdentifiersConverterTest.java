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
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

/**
 * Unit test of SgdIdentifiersConverter
 * @author Fengyuan Hu
 */
public class SgdIdentifiersConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    SgdIdentifiersConverter converter;
    MockItemWriter itemWriter;

    public SgdIdentifiersConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new SgdIdentifiersConverter(itemWriter, model);
    }

    public void testProcess() throws Exception {
        File srcFile = new File(getClass().getClassLoader().
                getResource("SGD_features.tab").toURI());
        converter.setCurrentFile(srcFile);
        converter.process(new FileReader(srcFile));

        converter.close();

        // uncomment to write out a new target items file
        // writeItemsFile(itemWriter.getItems(), "sgd-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("SgdIdentifiersConverterTest.xml");
        assertEquals(expected, itemWriter.getItems());
    }
}
