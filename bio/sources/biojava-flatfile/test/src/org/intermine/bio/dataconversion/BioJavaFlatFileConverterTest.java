package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;

public class BioJavaFlatFileConverterTest extends ItemsTestCase {
    
    
    public BioJavaFlatFileConverterTest(String arg) {
        super(arg);
    }

    public void testProcess1() throws Exception {
        String input = 
            IOUtils.toString(getClass().getClassLoader().getResourceAsStream("embl_test_1.embl"));
        
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        BioJavaFlatFileConverter converter = new BioJavaFlatFileConverter(itemWriter);
        converter.process(new StringReader(input));
        converter.close();
        System.out.println(itemWriter.getItems());
        Set expected = readItemSet("embl_test_1.xml");
        assertEquals(expected, itemWriter.getItems());
    }
    
    public void testProcess2() throws Exception {
        String input = 
            IOUtils.toString(getClass().getClassLoader().getResourceAsStream("embl_test_2.embl"));
        
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        BioJavaFlatFileConverter converter = new BioJavaFlatFileConverter(itemWriter);
        converter.process(new StringReader(input));
        converter.close();
        System.out.println(itemWriter.getItems());
        Set expected = readItemSet("embl_test_2.xml");
        assertEquals(expected, itemWriter.getItems());
    }
}
