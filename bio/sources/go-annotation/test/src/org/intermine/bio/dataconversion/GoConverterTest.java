package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;


public class GoConverterTest extends ItemsTestCase
{
    private File goOboFile;
    Model model;
    GoConverter converter;
    MockItemWriter writer;

    public GoConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        model = Model.getInstanceByName("genomic");
        goOboFile = File.createTempFile("go-tiny", ".obo");
        Reader goOboReader = new InputStreamReader(
            getClass().getClassLoader().getResourceAsStream("go-tiny.obo"));
        writeTempFile(goOboFile, goOboReader);
        writer = new MockItemWriter(new LinkedHashMap<String, org.intermine.model.fulldata.Item>());
        converter = new GoConverter(writer, model);
        converter.setGaff("2.0");

        converter.rslv = IdResolverService.getMockIdResolver("Gene");
        converter.rslv.addResolverEntry("7227", "FBgn0004168", Collections.singleton("FBgn0020002"));
        converter.rslv.addResolverEntry("7227", "FBgn0015567", Collections.singleton("FBgn0015567"));
        converter.rslv.addResolverEntry("7227", "FBgn0026430", Collections.singleton("FBgn0026430"));
        converter.rslv.addResolverEntry("7227", "FBgn0001612", Collections.singleton("FBgn0001612"));

        converter.rslv = IdResolverService.getMockIdResolver("Go");
        converter.rslv.addResolverEntry("0", "GO:1234567", Collections.singleton("GO:9999999"));
        converter.rslv.addResolverEntry("0", "GO:0000011:", Collections.singleton("GO:0000011"));
        converter.rslv.addResolverEntry("0", "GO:0000004", Collections.singleton("GO:0000004"));
        converter.rslv.addResolverEntry("0", "GO:0000005", Collections.singleton("GO:0000005"));
        converter.rslv.addResolverEntry("0", "GO:0000001", Collections.singleton("GO:0000001"));
    }

    private void writeTempFile(File outFile, Reader srcFileReader) throws Exception {
        FileWriter fileWriter = new FileWriter(outFile);
        int c;
        while ((c = srcFileReader.read()) > 0) {
            fileWriter.write(c);
        }
        fileWriter.close();
    }

    public void tearDown() throws Exception {
        goOboFile.delete();
    }

    public void testProcess() throws Exception {
        Reader reader = new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("GoConverterOboTest_src.txt"));
        converter.process(reader);
        //System.out.println("productWrapperMap: " + converter.productMap.keySet());
        converter.close();

        // uncomment to write a new target items file
        // writeItemsFile(writer.getItems(), "go-tgt-items.xml");

        assertEquals(readItemSet("GoConverterOboTest_tgt.xml"), writer.getItems());
    }

    public void testCreateWithObjects() throws Exception {
        ItemFactory tgtItemFactory = new ItemFactory(Model.getInstanceByName("genomic"));
        Item organism = tgtItemFactory.makeItem("3_1", "Organism", "");
        organism.setAttribute("taxonId", "7227");

        Set<String> expected = new HashSet<String>();
        expected.add("1_1");
        expected.add("1_2");
        converter.initialiseMapsForFile();
        assertEquals(expected, new HashSet<String>(converter.createWithObjects(
                "FLYBASE:Grip84; FB:FBgn0026430, FLYBASE:l(1)dd4; FB:FBgn0001612",
                organism, "FlyBase", "FlyBase")));
    }

}
