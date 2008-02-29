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
public class BDGPInsituConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");

    public BDGPInsituConverterTest(String arg) {
        super(arg);
    }

    public void testConstruct() throws Exception {
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        BDGPInsituConverter converter = new BDGPInsituConverter(itemWriter, model);
        assertNotNull(converter.orgDrosophila);
    }

    public void testProcess() throws Exception {

        String input = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("BDGPInsituConverterTest_src.csv"));

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        BDGPInsituConverter converter = new BDGPInsituConverter(itemWriter, Model.getInstanceByName("genomic"));
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "bdgp_tgt.xml");

        assertEquals(readItemSet("BDGPInsituConverterTest_tgt.xml"), itemWriter.getItems());

    }
}
