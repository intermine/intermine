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

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.intermine.dataconversion.DataTranslator;
import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.dataconversion.MockItemReader;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.dataconversion.XmlConverter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.FullParser;

public class PsiDataTranslatorTest extends DataTranslatorTestCase {
    private String tgtNs = "http://www.flymine.org/model/genomic#";

    public PsiDataTranslatorTest(String arg) {
        super(arg, "osw.bio-fulldata-test");
    }

    public void testTranslate() throws Exception {
        Collection srcItems = getSrcItems();

        // print out source items XML - result of running XmlConverter on PSI XML
//        java.io.FileWriter writer = new java.io.FileWriter(new java.io.File("src.xml"));
//        writer.write(org.intermine.xml.full.FullRenderer.render(srcItems));
//        writer.close();
        String organisms = "7227 4932";
        DataTranslator translator = new PsiDataTranslator(new MockItemReader(writeItems(srcItems)),
                                                          mapping, srcModel, getTargetModel(tgtNs));
        ((PsiDataTranslator) translator).setOrganisms(organisms);
        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);


        //Use to write out the translated file if you want to make a new tgts file for testing

//         java.io.FileWriter fw = new java.io.FileWriter(new java.io.File("psi_tgts.xml"));
//         fw.write("<items>");
//         fw.write(tgtIw.getItems().toString());
//         fw.write("</items>");
//         fw.flush();
//         fw.close();

        System.out.println(printCompareItemSets(new HashSet(getExpectedItems()), tgtIw.getItems()));

        assertEquals(new HashSet(getExpectedItems()), tgtIw.getItems());
    }


    protected String getModelName() {
        return "genomic";
    }

    protected String getSrcModelName() {
        return "psi";
    }

    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream(
                "test/PsiDataTranslatorTest_tgt.xml"));
    }

    protected Collection getSrcItems() throws Exception {
        Model psiModel = Model.getInstanceByName("psi");
        Reader srcReader = (new InputStreamReader(getClass().getClassLoader().getResourceAsStream(
                "test/PsiDataTranslatorTest_src.xml")));
        MockItemWriter mockIw = new MockItemWriter(new HashMap());
        Reader xsdReader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(
                "psi.xsd"));

        XmlConverter converter = new XmlConverter(psiModel, xsdReader, mockIw);
        converter.process(srcReader);
//         FileWriter fw = new FileWriter(new File("psi_tmp.xml"));
//         fw.write(mockIw.getItems());
//         fw.flush();
//         fw.close();
        return mockIw.getItems();
    }
}
