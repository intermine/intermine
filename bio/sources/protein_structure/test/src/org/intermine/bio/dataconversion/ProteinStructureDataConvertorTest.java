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

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;

/**
 * @author Xavier Watkins
 *
 */
public class ProteinStructureDataConvertorTest extends ItemsTestCase
{
    public ProteinStructureDataConvertorTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {

        Reader reader = new InputStreamReader(getClass().getClassLoader()
                                              .getResourceAsStream("ProteinStructureConverterTest_src.xml"));
        
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        MockProteinStructureDataConvertor converter = new MockProteinStructureDataConvertor(itemWriter);
        File f = new File("10102.xml");
        converter.setCurrentFile(f);
        converter.setSrcDataDir("resouces/");
        converter.process(reader);
        converter.close();
        Set expected = readItemSet("ProteinStructureConverterTest_tgt.xml");
        assertEquals(expected, itemWriter.getItems());
    }
}