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
import java.io.StringReader;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.dataconversion.MockItemsTestCase;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

/**
 *
 *
 */
public class BioPAXConverterTest extends MockItemsTestCase
{
    private BioPAXConverter converter;
    private MockItemWriter itemWriter;
    private String TEST_FILE = "83333.owl";
    private String TAXON_ID = "83333";

    /**
     *
     * @param arg
     */
    public BioPAXConverterTest(String arg) {
        super(arg);
    }

    /**
     *
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testProcess() throws Exception {

        itemWriter = new MockItemWriter(new HashMap());
        converter = new BioPAXConverter(itemWriter, Model.getInstanceByName("genomic"));

        ClassLoader loader = getClass().getClassLoader();
        String input = IOUtils.toString(loader.getResourceAsStream(TEST_FILE));

        File currentFile = new File(getClass().getClassLoader().getResource(TEST_FILE).toURI());
        converter.setBiopaxDatasourcename("Reactome");
        converter.setBiopaxDatasetname("Reactome data set");
        converter.setCurrentFile(currentFile);
        converter.setBiopaxOrganisms(TAXON_ID);
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        // writeItemsFile(itemWriter.getItems(), "BioPAX-tgt-items.xml");

        Set<Item> expected = readItemSet("BioPAXConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());
    }
}
