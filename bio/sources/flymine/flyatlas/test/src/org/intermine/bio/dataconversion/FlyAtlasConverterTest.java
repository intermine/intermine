package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

public class FlyAtlasConverterTest extends ItemsTestCase
{
    public FlyAtlasConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        String ENDL = System.getProperty("line.separator");
        String input = "Oligo\tbrain vs whole fly - T-Test_Change Direction\tBrainMean\tBrainSEM\tBrainCall\tBrain:fly\thead vs whole fly  - T-Test_Change Direction\tHeadMean\tHeadSEM\tHeadCall\tHead:fly\tFlyMean\tFlySEM\tFlyCall" + ENDL
            + "1616608_a_at\tDown\t1016.15\t23.17392572\t4\t0.696947874\tUp\t1874.55\t85.33788237\t4\t1.285699588\t1458\t127.7786302\t4" + ENDL;

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());

        FlyAtlasConverter converter = new FlyAtlasConverter(itemWriter,
                                                        Model.getInstanceByName("genomic"));

        // replace the assays map so we don't store all tissues
        Map<String, Item> mockAssays = new HashMap<String, Item>();
        mockAssays.put("brain", createMockAssay("Brain", converter));
        mockAssays.put("head", createMockAssay("Head", converter));
        mockAssays.put("FlyMean", createMockAssay("Whole Fly", converter));
        converter.assays = mockAssays;


        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "flyatlas-tgt-items.xml");

        assertEquals(readItemSet("FlyAtlasConverterTest.xml"), itemWriter.getItems());
    }


    private Item createMockAssay(String name, FlyAtlasConverter converter) {
        Item mockAssay = converter.createItem("MicroArrayAssay");
        mockAssay.setAttribute("name", name);
        return mockAssay;
    }
}
