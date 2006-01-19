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

import java.io.*;
import java.util.*;

import org.intermine.dataconversion.MockItemWriter;
import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.metadata.Model;


public class GoConverterTest extends TestCase
{
    private File goFile;
    private File goOboFile;

    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";

    public GoConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        goFile = File.createTempFile("go-tiny", ".ontology");
        Reader goReader = new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("resources/test/go-tiny.ontology"));
        writeTempFile(goFile, goReader);

        goOboFile = File.createTempFile("go-tiny", ".obo");
        Reader goOboReader = new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("resources/test/go-tiny.obo"));
        writeTempFile(goOboFile, goOboReader);
    }

    private void writeTempFile(File outFile, Reader srcFileReader) throws Exception{
        FileWriter fileWriter = new FileWriter(outFile);
        int c;
        while ((c = srcFileReader.read()) > 0) {
            fileWriter.write(c);
        }
        fileWriter.close();
    }

    public void tearDown() throws Exception {
        goFile.delete();
        goOboFile.delete();
    }

    public void testTranslate() throws Exception {
        translateCommon(goFile, "/resources/test/GoConverterTest_src.txt",
                "/resources/test/GoConverterTest_tgt.xml", true, false);
    }

    public void testOboTranslate() throws Exception {

        translateCommon(goOboFile, "/resources/test/GoConverterOboTest_src.txt",
                "/resources/test/GoConverterOboTest_tgt.xml", true, false);

    }

    private void translateCommon(File onotologyFile, String srcFile, String tgtFile,
                                 boolean verbose, boolean writeItemFile) throws Exception{

        Reader reader = new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(srcFile));
        MockItemWriter writer = new MockItemWriter(new LinkedHashMap());
        GoConverter converter = new GoConverter(writer);
        converter.setOntology(onotologyFile);
        converter.process(reader);
        converter.close();

        if(verbose){
            System.out.println(DataTranslatorTestCase.printCompareItemSets(
                    new HashSet(getExpectedItems(tgtFile)), writer.getItems()));
        }

        assertEquals(new HashSet(getExpectedItems(tgtFile)), writer.getItems());

        if(writeItemFile){
            writeItemCollectionOutToFile(
                    getExpectedItems(tgtFile), "GoConverterTestItemFile", "xml", true);
        }
    }

    public void testCreateWithObjects() throws Exception {
        MockItemWriter writer = new MockItemWriter(new LinkedHashMap());
        GoConverter converter = new GoConverter(writer);

        List expected = new ArrayList();
        ItemFactory tgtItemFactory = new ItemFactory(Model.getInstanceByName("genomic"));
        Item gene1 = tgtItemFactory.makeItem("0_0", GENOMIC_NS + "Gene", "");
        gene1.setAttribute("organismDbId", "FBgn0026430");
        gene1.setReference("organism", "1_1");
        expected.add(gene1);
        Item gene2 = tgtItemFactory.makeItem("0_2", GENOMIC_NS + "Gene", "");
        gene2.setAttribute("organismDbId", "FBgn0001612");
        gene2.setReference("organism", "1_1");
        expected.add(gene2);
        assertEquals(expected, converter.createWithObjects(
                "FLYBASE:Grip84; FB:FBgn0026430, FLYBASE:l(1)dd4; FB:FBgn0001612", "1_1", "10_10"));
    }

    protected Collection getExpectedItems(String targetFile) throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream(targetFile));
    }

    /*
        Use this to write out a test file for comparing the source data with...
    */
    private void writeItemCollectionOutToFile(
            Collection itemCollection, String fileName, String suffix, boolean includeItemsTag){

        try {
            File itemLogFile =File.createTempFile(fileName, suffix);

            BufferedWriter writer = new BufferedWriter(new FileWriter(itemLogFile, true));

            if(includeItemsTag){
                writer.write("<items>\n");
            }

            for( Iterator icit = itemCollection.iterator(); icit.hasNext(); ){
                writer.write(icit.next().toString() + "\n");
            }

            if(includeItemsTag){
                writer.write("</items>\n\n");
            }

            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
