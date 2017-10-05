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

import org.apache.commons.io.IOUtils;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class EnsemblIdentifiersConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    EnsemblIdentifiersConverter converter;
    MockItemWriter itemWriter;
    private String TEST_FILE = "9606";

    public EnsemblIdentifiersConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap());
        converter = new EnsemblIdentifiersConverter(itemWriter, model);
    }

    public void testProcess() throws Exception {

        ClassLoader loader = getClass().getClassLoader();
        String input = IOUtils.toString(loader.getResourceAsStream(TEST_FILE));

        File currentFile = new File(getClass().getClassLoader().getResource(TEST_FILE).toURI());
        converter.setCurrentFile(currentFile);
        converter.setEnsemblOrganisms("10116 6239 7227 9606");
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "ensembl-compara-tgt-items.xml");

        Set expected = readItemSet("EnsemblIdentifiersConverterTest_tgt.xml");
        assertEquals(expected, itemWriter.getItems());
    }
}
