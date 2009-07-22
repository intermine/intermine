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
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.dataconversion.MockItemsTestCase;
import org.intermine.metadata.Model;

public class ZfinExprConverterTest extends MockItemsTestCase
{

    private ZfinExprConverter converter;
    private MockItemWriter itemWriter;

    public ZfinExprConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        itemWriter = new MockItemWriter(new HashMap());
        converter = new ZfinExprConverter(itemWriter, Model.getInstanceByName("genomic"));
        super.setUp();
    }

    public void testProcess() throws Exception {


        File stages = File.createTempFile("stages", "");
        FileOutputStream out = new FileOutputStream(stages);
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("stage_ontology.txt"), out);
        out.close();

        String input = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("wildtype-expression.txt"));
        converter.setStagesFile(stages);

        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        writeItemsFile(itemWriter.getItems(), "zfin_tgt.xml");

        assertEquals(readItemSet("ZfinExprConverterTest_tgt.xml"), itemWriter.getItems());
    }
}
