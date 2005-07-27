package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.io.Reader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.intermine.dataconversion.MockItemWriter;
import org.intermine.dataconversion.TargetItemsTestCase;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.xml.full.FullParser;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class GoConverterTest extends TestCase
{
    private File goFile;

    public GoConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        goFile = File.createTempFile("go-tiny", "ontology");
        Reader goReader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("test/go-tiny.ontology"));

        FileWriter fileWriter = new FileWriter(goFile);
        int c;
        while ((c = goReader.read()) > 0) {
            fileWriter.write(c);
        }
        fileWriter.close();
    }

    public void tearDown() throws Exception {
        goFile.delete();
    }

    public void testTranslate() throws Exception {
        Reader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("test/GoConverterTest_src.txt"));
        MockItemWriter writer = new MockItemWriter(new LinkedHashMap());
        GoConverter converter = new GoConverter(writer);
        converter.setOntology(goFile);
        converter.process(reader);
        converter.close();

        //System.out.println(DataTranslatorTestCase.printCompareItemSets(new HashSet(getExpectedItems()), writer.getItems()));
        assertEquals(new HashSet(getExpectedItems()), writer.getItems());
    }

    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/GoConverterTest_tgt.xml"));
    }
}
