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


import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.FullParser;

public class MgiIdentifiersConverterTest extends ItemsTestCase
{
    private String ENDL = System.getProperty("line.separator");

    public MgiIdentifiersConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        String input = "MGI:12345\t"
                + "Gene\t"
                + "mouse-symbol\t"
                + "mouse-name\t"
                + "OTTMUSG00000015981\t"
                + "154617138\t"
                + "154876748\t"
                + "NCBI Build 37\t"
                + "50518\t"
                + "2\t"
                + "mouse-ncbinumber\t"
                + "154876748\t"
                + "ENSMUSG00000027596\t"
                + "2\tnull\tmouse-ensemblId\t"
                + "154617138";


        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        BioFileConverter converter = new MgiIdentifiersConverter(itemWriter,
                                                                   Model.getInstanceByName("genomic"));
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "mgi-identfiers_tgt.xml");

        assertEquals(readItemSet("MgiIdentifiersConverterTest.xml"), itemWriter.getItems());
    }
}
