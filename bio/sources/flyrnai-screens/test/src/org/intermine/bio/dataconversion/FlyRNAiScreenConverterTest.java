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

import junit.framework.TestCase;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.FullRenderer;
import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.dataconversion.MockItemWriter;

public class FlyRNAiScreenConverterTest extends TestCase
{
    public void testProcess() throws Exception {
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        FlyRNAiScreenConverter converter = new FlyRNAiScreenConverter(itemWriter);

        File srcFile = new File("resources/FlyRNAiConverterTest.dataset");
        converter.setCurrentFile(srcFile);
        converter.taxonId = "7227";
        converter.process(new FileReader(srcFile));
        converter.close();

        // uncomment to write out a new target items file
        //FileWriter fw = new FileWriter(new File("fly-rnai_tgt.xml"));
        //fw.write(FullRenderer.render(itemWriter.getItems()));
        //fw.close();

        Set expected = new HashSet(FullParser.parse(getClass().getClassLoader().getResourceAsStream("FlyRNAiConverterTest_tgt.xml")));

        if (!expected.equals(itemWriter.getItems())) {
            System.err.println(DataTranslatorTestCase.printCompareItemSets(new HashSet(expected),
                                                                           itemWriter.getItems()));
        }

        assertEquals(expected, itemWriter.getItems());
    }
}
