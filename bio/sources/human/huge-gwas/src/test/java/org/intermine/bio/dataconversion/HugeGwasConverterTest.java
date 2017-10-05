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
import java.io.FileReader;
import java.util.HashMap;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

/**
 * @author Richard Smith
 * @author Fengyuan Hu
 *
 */
public class HugeGwasConverterTest extends ItemsTestCase
{
    private HugeGwasConverter converter;
    private MockItemWriter itemWriter;

    public HugeGwasConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new HugeGwasConverter(itemWriter,
                Model.getInstanceByName("genomic"));
    }

    public void testProcess() throws Exception {
        File srcFile = new File(getClass().getClassLoader().getResource("HUGE-GWAS.txt").toURI());
        converter.setCurrentFile(srcFile);
        converter.process(new FileReader(srcFile));

        // uncomment to write out a new target items file
        // writeItemsFile(itemWriter.getItems(), "huge-gwas_tgt.xml");

        assertEquals(readItemSet("HugeGwasConverterTest.xml"), itemWriter.getItems());
    }

    public void testParseSnp() throws Exception {
        assertEquals("rs10048146", converter.parseSnp("rs10048146(16q24.1)"));
        assertEquals("rs10048146", converter.parseSnp("rs10048146"));
    }

    public void testParsePValue() throws Exception {
        assertEquals(0.0, converter.parsePValue("5x10-324"));
        assertEquals(2E-6, converter.parsePValue("2x10-6"));
    }
}
