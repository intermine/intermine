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

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

/**
 * Test for the PdbConverter
 * @author Xavier Watkins
 *
 */
public class PdbConverterTest extends ItemsTestCase
{

    public PdbConverterTest(String arg) {
        super(arg);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        PdbConverter converter = new PdbConverter(itemWriter, Model.getInstanceByName("genomic"));
        File tmp = new File(getClass().getClassLoader().getResource("PdbConverterTest_tgt.xml").toURI());
        File datadir = tmp.getParentFile();
        converter.process(datadir);
        converter.close();
        // uncomment to create a new target items files
        //writeItemsFile(itemWriter.getItems(), "PdbConverterTest_tgt.xml");
        Set expected = readItemSet("PdbConverterTest_tgt.xml");
        assertEquals(expected, itemWriter.getItems());

    }

}
