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

import java.io.StringReader;
import java.util.HashMap;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class InparanoidConverterTest extends ItemsTestCase
{
    private String ENDL = System.getProperty("line.separator");

    public InparanoidConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        // the input file format is 5 tab-delimited columns (eg. '14 1217 CE 1.000 O01438')
        // the first two are cluster IDs and are ignored
        // the third is the species (orthologues are cross-species, paralogues are within-species)
        // the fourth is a confidence relative to the closest match (defined as the orthologue and given a confidence of 1.000)
        // note that the confidence for the first member of a group appears to be meaningless (row 1).
        // so...this input should produce one orthologue (rows 1 & 3) and three paralogues (1 & 2, 3 & 4, 3 & 5)
        String input =
          "14\t1217\tmodSACCE.fa\t0.997\tYEAST_2\t100%" + ENDL
        + "14\t1217\tmodSACCE.fa\t1.000\tYEAST_1\t100%" + ENDL
        + "14\t1217\tensANOGA.fa\t1.000\tMOSQUITO_1\t98%" + ENDL
        + "14\t1217\tensANOGA.fa\t1.000\tMOSQUITO_2\t95%" + ENDL
        + "14\t1217\tensANOGA.fa\t0.566\tMOSQUITO_3\t100%" + ENDL;
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        FileConverter converter = new InparanoidConverter(itemWriter,
                                                          Model.getInstanceByName("genomic"));
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "orth-tgt-items.xml");

        assertEquals(readItemSet("InparanoidConverterTest.xml"), itemWriter.getItems());
    }
}
