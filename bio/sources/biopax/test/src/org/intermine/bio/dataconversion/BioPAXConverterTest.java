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
import java.io.StringReader;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;


public class BioPAXConverterTest extends ItemsTestCase
{
    private BioPAXConverter converter;
    private MockItemWriter itemWriter;
    
    
    public BioPAXConverterTest(String arg) {
        super(arg);
    }

    public void testProcess() throws Exception {
        
        itemWriter = new MockItemWriter(new HashMap());
        converter = new BioPAXConverter(itemWriter, Model.getInstanceByName("genomic"));
        
        ClassLoader loader = getClass().getClassLoader();
        String input = IOUtils.toString(loader.getResourceAsStream("BioPAXConverterTest_src.owl"));
        
        
        File currentFile = new File(getClass().getClassLoader().getResource("BioPAXConverterTest_src.owl").toURI());
        converter.setCurrentFile(currentFile);
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        writeItemsFile(itemWriter.getItems(), "BioPAX-tgt-items.xml");

        Set expected = readItemSet("BioPAXConverterTest_tgt.xml");

//        assertEquals(expected, itemWriter.getItems());
    }
}
