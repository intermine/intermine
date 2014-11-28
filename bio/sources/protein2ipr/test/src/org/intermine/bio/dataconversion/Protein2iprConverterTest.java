package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

public class Protein2iprConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    Protein2iprConverter converter;
    MockItemWriter itemWriter;
    private final String currentFile = "protein2ipr.dat";

    public Protein2iprConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new Protein2iprConverter(itemWriter, model);

    }

    public void testProcess() throws Exception {

        Reader reader = new InputStreamReader(getClass().getClassLoader()
                                            .getResourceAsStream(currentFile));
        converter.setCurrentFile(new File(currentFile));
        converter.setOrganisms("10116 4932 10090 7227");
        System.out.println(converter.getCurrentFile());
        converter.process(reader);
        converter.close();

        // uncomment to write out a new target items file
        // writeItemsFile(itemWriter.getItems(), "protein2ipr-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("Protein2iprConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());
    }
}
