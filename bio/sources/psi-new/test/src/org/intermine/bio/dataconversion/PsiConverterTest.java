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
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;

import java.io.InputStreamReader;
import java.io.Reader;

public class PsiConverterTest extends ItemsTestCase
{
    public PsiConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
     
        Reader reader = new InputStreamReader(getClass().getClassLoader()
                                            .getResourceAsStream("PsiConverterTest_src.xml"));
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        PsiConverter converter = new PsiConverter(itemWriter);
        converter.setOrganisms("7227");
        converter.process(reader);
        converter.close();

        // uncomment to write out a new target items file
        writeItemsFile(itemWriter.getItems(), "psi-tgt-items.xml");

        Set expected = readItemSet("PsiConverterTest_tgt.xml");
        
        assertEquals(expected, itemWriter.getItems());
    }
}
