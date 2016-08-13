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

import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

import java.io.File;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;

/**
 * Tiffin expression converter functional test.
 * @author Kim Rutherford
 */
public class TiffinExpressionConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");

    public TiffinExpressionConverterTest(String arg) {
        super(arg);
    }

    public void testConstruct() throws Exception {
        MockItemWriter itemWriter =
            new MockItemWriter(new HashMap<String, org.intermine.model.fulldata.Item>());
        TiffinExpressionConverter converter = new TiffinExpressionConverter(itemWriter, model);
        assertNotNull(converter.orgDrosophila);
    }

    public void testProcess() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        final String testDataFileName = "tiffin_ImaGo.test_data";
        String input = IOUtils.toString(loader.getResourceAsStream(testDataFileName));

        MockItemWriter itemWriter =
            new MockItemWriter(new HashMap<String, org.intermine.model.fulldata.Item>());
        TiffinExpressionConverter converter = new TiffinExpressionConverter(itemWriter, model);
        converter.setCurrentFile(new File(testDataFileName));
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to create a new target items files
        //writeItemsFile(itemWriter.getItems(), "TiffinExpressionTestItems_actual.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("TiffinExpressionTestItems.xml");
        assertEquals(expected, itemWriter.getItems());
    }
}
