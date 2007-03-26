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

public class FlyBaseGeneNamesConverterTest extends TestCase
{
    private String ENDL = System.getProperty("line.separator");

    public FlyBaseGeneNamesConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        //
        String input = "FBgn0004053\tzen\tzerknullt\tzerknullt 1,zerknullt" + ENDL
            + "FBgn0012699\tDpse\\Gld\tGlucose dehydrogenas" + ENDL;

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        FileConverter converter = new FlyBaseGeneNamesConverter(itemWriter);
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        FileWriter fw = new FileWriter(new File("flybase-gene-names_tgt.xml"));
        fw.write(FullRenderer.render(itemWriter.getItems()));
        fw.close();

        //System.out.println(DataTranslatorTestCase.printCompareItemSets(new HashSet(getExpectedItems()), itemWriter.getItems()));
        //assertEquals(new HashSet(getExpectedItems()), itemWriter.getItems());
    }

    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("FlyBaseGeneNamesConverterTest.xml"));
    }
}
