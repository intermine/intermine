package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.FullRenderer;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Reader;

import junit.framework.TestCase;

public class UniprotKeywordConverterTest extends TestCase
{

    public UniprotKeywordConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
	
	Reader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("UniprotKeywordImporterTest_src.xml"));
	

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        FileConverter converter = new UniprotKeywordConverter(itemWriter);
        converter.process(reader);
        converter.close();

        // uncomment to write out a new target items file
	FileWriter fw = new FileWriter(new File("uniprotKeyword_tgt.xml"));
        fw.write(FullRenderer.render(itemWriter.getItems()));
        fw.close();

        System.out.println(DataTranslatorTestCase.printCompareItemSets(new HashSet(getExpectedItems()), itemWriter.getItems()));
        assertEquals(new HashSet(getExpectedItems()), itemWriter.getItems());
    }

    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("UniprotKeywordImporterTest_tgt.xml"));
    }
}
