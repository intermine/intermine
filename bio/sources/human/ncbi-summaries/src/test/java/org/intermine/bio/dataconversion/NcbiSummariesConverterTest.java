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

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

/**
 * NcbiSummariesConverterTest
 * @author rns
 *
 */
public class NcbiSummariesConverterTest extends ItemsTestCase
{
    /**
     *
     * @param arg arg
     */
    public NcbiSummariesConverterTest(String arg) {
        super(arg);
    }

    /**
     * Test Process
     * @throws Exception e
     */
    public void testProcess() throws Exception {
        MockItemWriter itemWriter = new MockItemWriter(new HashMap<String, Item>());
        BioFileConverter converter =
            new NcbiSummariesConverter(itemWriter, Model.getInstanceByName("genomic"));

        File srcFile = new File(getClass().getClassLoader().
                getResource("gene_summaries.txt").toURI());
        converter.setCurrentFile(srcFile);
        converter.process(new FileReader(srcFile));

        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "ncbi-summaries-tgt.xml");

        assertEquals(readItemSet("NcbiSummariesConverterTest.xml"), itemWriter.getItems());
    }

}
