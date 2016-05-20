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
import java.io.StringReader;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class RNAiConverterTest extends ItemsTestCase
{

    public RNAiConverterTest(String arg) {
        super(arg);
    }

    public void testProcess() throws Exception {
        String ENDL = System.getProperty("line.separator");
        String input = "Gene WB ID\tGene Public Name\tPhenotype WB ID\tPhenotype Primary Name\tPhenotype Short Name\tPhenotype Is Observed\tPhenotype Is Not Observed\tPhenotype Penetrance From (%)\tPhenotype Penetrance To (%)\tRNAi WB ID\tRNAi Remark\tPubMed ID" + ENDL
                + "WBGene00000516\tcki-1\tWBPhenotype0000202\talae_abnormal\t\t1\t\t\t\tWBRNAi00064855\tclone does not match to the reported genomic sequence\t9716524" + ENDL
                + "WBGene00006974\tzen-4\tWBPhenotype0000765\tspindle_elongation_integrity_abnormal_early_emb\tEmb\t\t1\t27\t27\tWBRNAi00063891\t\t9693365";

        File file = new File ("wormrnai-final.txt");

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        RNAiConverter converter = new RNAiConverter(itemWriter,
                                                    Model.getInstanceByName("genomic"));
        converter.setCurrentFile(file);
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "RNAiConverterTest.xml");

        Set expected = readItemSet("RNAiConverterTest.xml");

        assertEquals(expected, itemWriter.getItems());
    }
}
