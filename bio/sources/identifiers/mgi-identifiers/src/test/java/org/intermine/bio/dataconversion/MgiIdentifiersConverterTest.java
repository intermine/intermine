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


import java.io.StringReader;
import java.util.HashMap;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

/**
 * Unit test for MgiIdentifiersConverter
 * @author intermine
 *
 */
public class MgiIdentifiersConverterTest extends ItemsTestCase
{
    @SuppressWarnings("unused")
    private static final String ENDL = System.getProperty("line.separator");

    /**
     * Constructor
     * @param arg a
     */
    public MgiIdentifiersConverterTest(String arg) {
        super(arg);
    }

    /**
     * Setup
     * @throws Exception e
     */
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * To test process
     * @throws Exception e
     */
    public void testProcess() throws Exception {
        String input = "MGI:1916316\t"
                + "15\t"
                + "70.10\t"
                + "107028118\t"
                + "107030438\t"
                + "+\t"
                + "1810010H24Rik\t"
                + "0\t"
                + "RIKEN cDNA 1810010H24 gene\t"
                + "Gene\t"
                + "unclassified gene\t"
                + "OTTMUSG00000003581";


        MockItemWriter itemWriter = new MockItemWriter(new HashMap<String, Item>());
        BioFileConverter converter = new MgiIdentifiersConverter(itemWriter,
                Model.getInstanceByName("genomic"));
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        // writeItemsFile(itemWriter.getItems(), "mgi-identfiers_tgt.xml");

        assertEquals(readItemSet("MgiIdentifiersConverterTest.xml"), itemWriter.getItems());
    }
}
