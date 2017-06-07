package org.intermine.bio.dataconversion;

import java.io.File;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedHashMap;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class HpoConverterTest extends ItemsTestCase
{
    Model model;
    HpoConverter converter;
    MockItemWriter writer;

    public HpoConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        model = Model.getInstanceByName("genomic");
        writer = new MockItemWriter(new LinkedHashMap<String, org.intermine.model.fulldata.Item>());
        converter = new HpoConverter(writer, model);
    }


    public void testProcess() throws Exception {
        File tmp = new File(getClass().getClassLoader().getResource("hp.obo").toURI());
        File datadir = tmp.getParentFile();
        converter.process(datadir);
        converter.close();

        // uncomment to write a new target items file
        writeItemsFile(writer.getItems(), "hpo-tgt-items.xml");

        assertEquals(readItemSet("HpoConverterOboTest_tgt.xml"), writer.getItems());
    }
}
