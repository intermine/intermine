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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

/**
 * Test of HumanGeneConverter class.
 *
 * @author Fengyuan Hu
 *
 */
public class HumanGeneConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    HumanGeneConverter converter;
    MockItemWriter itemWriter;
    private Set<Item> storedItems;

    public HumanGeneConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        itemWriter = new MockItemWriter(new HashMap<String, org.intermine.model.fulldata.Item>());
        converter = new HumanGeneConverter(itemWriter, model);
        super.setUp();
    }

    /**
     * Basic test of converter functionality.
     * @throws Exception
     */
    public void testSimpleFiles() throws Exception {
        process("human_gene");
        assertEquals(42, itemWriter.getItems().size());
    }

    /**
     * Test the count of items created from the records that have gene type: protein-coding, etc.
     * @throws Exception
     */
    public void testGeneCount() throws Exception {
        process("human_gene");
        assertEquals(6, getGenes().size());
    }

    private void process(String infoFile) throws Exception {
        File geneInfo = new File(getClass().getClassLoader().getResource(infoFile).toURI());

        converter.setCurrentFile(geneInfo);
        converter.process(new FileReader(geneInfo));
        converter.close();

        storedItems = itemWriter.getItems();
        // writeItemsFile(storedItems, "humangene-tgt-items.xml");
    }

    private List<Item> getGenes() {
        List<Item> ret = new  ArrayList<Item>();
        for (Item item : storedItems) {
            if (item.getClassName().contains("Gene")) {
                ret.add(item);
            }
        }
        return ret;
    }
}
