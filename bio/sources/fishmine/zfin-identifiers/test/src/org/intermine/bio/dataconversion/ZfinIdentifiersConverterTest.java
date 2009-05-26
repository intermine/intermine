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
import java.util.Collection;
import java.util.HashMap;

import org.intermine.dataconversion.MockItemWriter;
import org.intermine.dataconversion.MockItemsTestCase;
import org.intermine.metadata.Model;
import org.intermine.xml.full.FullParser;

public class ZfinIdentifiersConverterTest extends MockItemsTestCase
{

    private ZfinIdentifiersConverter converter;
    private MockItemWriter itemWriter;

    public ZfinIdentifiersConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        itemWriter = new MockItemWriter(new HashMap());
        converter = new ZfinIdentifiersConverter(itemWriter, Model.getInstanceByName("genomic"));
        super.setUp();
    }

    public void testProcess() throws Exception {
        File tmp = new File(getClass().getClassLoader().getResource("ZfinIdentifiersConverterTest_tgt.xml").toURI());
        File datadir = tmp.getParentFile();
        converter.process(datadir);
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "zfin_tgt.xml");

        assertEquals(readItemSet("ZfinIdentifiersConverterTest_tgt.xml"), itemWriter.getItems());
    }

    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("ZfinIdentifiersConverterTest.xml"));
    }
}
