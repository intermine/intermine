package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
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
 * Fly-FISH converter functional test.
 * @author Kim Rutherford
 */
public class FlyFishConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");

    public FlyFishConverterTest(String arg) {
        super(arg);
    }

    public void testConstruct() throws Exception {
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        FlyFishConverter converter = new FlyFishConverter(itemWriter, model);
        assertNotNull(converter.orgDrosophila);
    }

    public void testProcess() throws Exception {

        ClassLoader loader = getClass().getClassLoader();
        String input = IOUtils.toString(loader.getResourceAsStream("test-matrix"));

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        FlyFishConverter converter = new FlyFishConverter(itemWriter, model);
        converter.setCurrentFile(new File("test-matrix"));
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to create a new target items files
        //writeItemsFile(itemWriter.getItems(), "flyfish_tgt.xml");

        Set expected = readItemSet("FlyFishTestItems.xml");
        assertEquals(expected, itemWriter.getItems());
    }
}
