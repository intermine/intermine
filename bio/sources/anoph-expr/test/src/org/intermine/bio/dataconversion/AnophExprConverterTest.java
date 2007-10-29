package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

import java.io.InputStreamReader;
import java.io.Reader;
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

        Reader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("AnophExprTest_src.txt"));

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        FileConverter converter = new AnophExprConverter(itemWriter, Model.getInstanceByName("genomic"));
        converter.process(reader);
        converter.close();

        // uncomment to write out a new target items file
        writeItemsFile(itemWriter.getItems(), "anoph-expr_tgt.xml");

        assertEquals(readItemSet("AnophExprTest_tgt.xml"), itemWriter.getItems());
        
    }
}
