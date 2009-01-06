package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
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
        Reader reader = new InputStreamReader(getClass().getClassLoader()
                                              .getResourceAsStream("1AZE.pdb"));

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        PdbConverter converter = new PdbConverter(itemWriter, Model.getInstanceByName("genomic"));
        File f = new File("1AZE.pdb");
        converter.setCurrentFile(f);
        converter.process(reader);
        converter.close();
        // uncomment to create a new target items files
        //writeItemsFile(itemWriter.getItems(), "PdbConverterTest_tgt.xml");
        Set expected = readItemSet("PdbConverterTest_tgt.xml");
        assertEquals(expected, itemWriter.getItems());

    }

}
