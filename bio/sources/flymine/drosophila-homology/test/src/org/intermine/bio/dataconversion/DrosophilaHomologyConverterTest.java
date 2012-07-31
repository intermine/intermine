package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStreamReader;
import java.util.HashMap;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class DrosophilaHomologyConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    DrosophilaHomologyConverter converter;
    MockItemWriter itemWriter;

    public DrosophilaHomologyConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap());
        converter = new DrosophilaHomologyConverter(itemWriter, model);
    }

    public void testProcess() throws Exception {

        InputStreamReader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("DrosophilaHomologyConverterTest_src.tsv"));
        converter.process(reader);

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "dros-homology-tgt.xml");

        assertEquals(readItemSet("DrosophilaHomologyConverterTest.xml"), itemWriter.getItems());
    }
}
