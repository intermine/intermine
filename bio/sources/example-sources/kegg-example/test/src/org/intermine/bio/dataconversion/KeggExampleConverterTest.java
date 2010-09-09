package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class KeggExampleConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    KeggExampleConverter converter;
    MockItemWriter itemWriter;

    public KeggExampleConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap());
        converter = new KeggExampleConverter(itemWriter, model);
    }

    public void testProcess() throws Exception {
        File srcFile = new File(getClass().getClassLoader().getResource("pfa_gene_map.tab").toURI());
        converter.setTaxonId("36329");
        converter.setCurrentFile(srcFile);
        converter.process(new FileReader(srcFile));

        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "kegg-tgt-items.xml");

        Set expected = readItemSet("KeggConverterTest_tgt.xml");
        assertEquals(expected, itemWriter.getItems());
    }
}
