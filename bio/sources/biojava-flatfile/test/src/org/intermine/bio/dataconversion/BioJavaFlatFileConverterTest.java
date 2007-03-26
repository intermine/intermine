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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.ItemFactory;

import java.io.StringReader;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;

public class BioJavaFlatFileConverterTest extends TestCase {
    public void testProcess1() throws Exception {
        String input = 
            IOUtils.toString(getClass().getClassLoader().getResourceAsStream("embl_test_1.embl"));
        
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        BioJavaFlatFileConverter converter = new BioJavaFlatFileConverter(itemWriter);
        converter.process(new StringReader(input));
        converter.close();
        System.out.println(itemWriter.getItems());
        Set expected = 
            new HashSet(FullParser.parse(getClass().getClassLoader().getResourceAsStream("embl_test_1.xml")));
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
        Set expected = 
            new HashSet(FullParser.parse(getClass().getClassLoader().getResourceAsStream("embl_test_2.xml")));
        assertEquals(expected, itemWriter.getItems());
    }
}
