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
import java.util.HashMap;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

public class EcogeneIdentifiersConverterTest extends ItemsTestCase
{
    public EcogeneIdentifiersConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        String record1 = "EG10001\t"
                + "alr\t"
                + "ECK4045\t"
                + "P0A6B4\t"
                + "b4053\t"
                + "g1790487\t"
                + "JW4013\t"
                + "aa\t"
                + "Clockwise\t"
                + "4263805\t"
                + "4264884\t"
                + "None";

        String record2 = "EG10012\t"
            + "cydC\t"
            + "ECK0877\t"
            + "P23886\t"
            + "b0886\t"
            + "g1787112\t"
            + "JW0869\t"
            + "aa\t"
            + "Counterclockwise\t"
            + "926697\t"
            + "928418\t"
            + "mdrA, mdrH, surB, ycaB";

        MockItemWriter itemWriter = new MockItemWriter(new HashMap<String, Item>());
        BioFileConverter converter = new EcogeneIdentifiersConverter(itemWriter,
                                                                Model.getInstanceByName("genomic"));
        converter.process(new StringReader(record1));
        converter.process(new StringReader(record2));
        converter.close();

        // uncomment to write out a new target items file
        writeItemsFile(itemWriter.getItems(), "ecogene-identfiers_tgt.xml");

        assertEquals(readItemSet("EcogeneIdentifiersConverterTest.xml"), itemWriter.getItems());
    }
}
