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

import java.util.HashMap;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;
/**
 * Anoph Expr converter functional test.
 * @author Thomas Riley
 */
public class AnophExprConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");

    public AnophExprConverterTest(String arg) {
        super(arg);
    }

    public void testConstruct() throws Exception {
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        AnophExprConverter converter = new AnophExprConverter(itemWriter, model);
        assertNotNull(converter.org);
    }


    public void testProcess() throws Exception {


        File genes = File.createTempFile("genes", "");
        FileOutputStream out = new FileOutputStream(genes);
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("reporter_gene_mappingFile.txt"), out);
        out.close();

        String input = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("AnophExprConverterTest_src.txt"));

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        AnophExprConverter converter = new AnophExprConverter(itemWriter, Model.getInstanceByName("genomic"));
        converter.setGeneFile(genes);
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "anoph-expr_tgt.xml");

        assertEquals(readItemSet("AnophExprConverterTest_tgt.xml"), itemWriter.getItems());

    }
}
