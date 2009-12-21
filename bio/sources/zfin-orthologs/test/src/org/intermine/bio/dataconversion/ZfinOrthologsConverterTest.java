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
import java.io.FileOutputStream;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.io.IOUtils;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class ZfinOrthologsConverterTest extends ItemsTestCase
{
    private ZfinOrthologsConverter converter;
    private MockItemWriter itemWriter;

    public ZfinOrthologsConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {

        itemWriter = new MockItemWriter(new HashMap());
        converter = new ZfinOrthologsConverter(itemWriter, Model.getInstanceByName("genomic"));
        

        super.setUp();
    }

    public void testProcess() throws Exception {

        Reader reader = new InputStreamReader(getClass().getClassLoader()
					      .getResourceAsStream("fly_orthos.txt"));
        converter.process(reader);
        converter.close();
        

        // uncomment to write out a new target items file
        writeItemsFile(itemWriter.getItems(), "zfin-orthologs-tgt-items.xml");

        Set expected = readItemSet("ZfinOrthologsConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());
    }
}
