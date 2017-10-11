package org.intermine.bio.dataconversion;

import java.io.File;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;
/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

public class OrphanetConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    OrphanetConverter converter;
    MockItemWriter itemWriter;
    private final String fileName = "en_product1.xml";

    public OrphanetConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new OrphanetConverter(itemWriter, model);
    }


    public void testProcess() throws Exception {

        File testFile = new File(getClass().getClassLoader()
                .getResource(fileName).toURI());

        Reader reader = new InputStreamReader(getClass().getClassLoader()
                .getResourceAsStream(fileName));

        converter.setCurrentFile(testFile);
        converter.process(reader);

        converter.close();
        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "orphanet-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("OrphanetConverterTest_tgt.xml");
        assertEquals(expected, itemWriter.getItems());
    }
}
