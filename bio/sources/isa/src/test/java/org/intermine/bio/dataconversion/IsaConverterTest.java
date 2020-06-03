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

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;

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
        Reader reader = new InputStreamReader(getClass().getClassLoader()
                .getResourceAsStream(currentFile));
        converter.process(reader);
        converter.close();

        // very rough test if the number of items is correct.
        // using a simple experiment.
        // TODO add some other test
        assertEquals(271, itemWriter.getItems().size());

    }

}
