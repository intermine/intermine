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

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.FullRenderer;
import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.dataconversion.FileConverter;

public class FlyAtlasConverterTest extends TestCase
{
    private String ENDL = System.getProperty("line.separator");

    public FlyAtlasConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        String ENDL = System.getProperty("line.separator");
        String input = "Oligo\tbrain vs whole fly - T-Test_Change Direction\tBrainMean\tBrainSEM\tBrainCall\tBrain:fly\thead vs whole fly  - T-Test_Change Direction\tHeadMean\tHeadSEM\tHeadCall\tHead:fly\tFlyMean\tFlySEM\tFlyCall" + ENDL
            + "1616608_a_at\tDown\t1016.15\t23.17392572\t4\t0.696947874\tUp\t1874.55\t85.33788237\t4\t1.285699588\t1458\t127.7786302\t4" + ENDL;

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        FileConverter converter = new FlyAtlasConverter(itemWriter);
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //FileWriter fw = new FileWriter(new File("flyatlas_tgt.xml"));
        //fw.write(FullRenderer.render(itemWriter.getItems()));
        //fw.close();

        System.out.println(DataTranslatorTestCase.printCompareItemSets(new HashSet(getExpectedItems()), itemWriter.getItems()));
        assertEquals(new HashSet(getExpectedItems()), itemWriter.getItems());
    }

    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/FlyAtlasConverterTest.xml"));
    }
}
