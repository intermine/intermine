package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Set;

//import static org.intermine.metadata.TypeUtil.getClass;

public class IsaConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    IsaConverter converter;
    MockItemWriter itemWriter;

    private final String currentFile = "IsaConverterTest_src.json";
    public IsaConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new IsaConverter(itemWriter, model);
    }

    public void testProcess() throws Exception {
        //final String currentFile = "IsaConverterTest_src.json";
        Reader reader = new InputStreamReader(getClass().getClassLoader()
                .getResourceAsStream(currentFile));
        converter.setCurrentFile(new File(currentFile));
        converter.process(reader);
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "reactome-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("IsaConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());
    }
}
