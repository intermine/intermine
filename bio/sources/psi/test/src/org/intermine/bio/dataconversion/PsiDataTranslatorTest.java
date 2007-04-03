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
import java.util.LinkedHashMap;

import org.intermine.dataconversion.DataTranslator;
import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.dataconversion.MockItemReader;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.dataconversion.XmlConverter;
import org.intermine.metadata.Model;

public class PsiDataTranslatorTest extends DataTranslatorTestCase {
    private String tgtNs = "http://www.flymine.org/model/genomic#";

    public PsiDataTranslatorTest(String arg) {
        super(arg, "osw.bio-fulldata-test");
    }

    public void testTranslate() throws Exception {
        Collection srcItems = getSrcItems();

        // print out source items XML - result of running XmlConverter on PSI XML
        //writeItemsFile(srcItems, "psi-src-items.xml");
        String organisms = "7227 4932";
        DataTranslator translator = new PsiDataTranslator(new MockItemReader(writeItems(srcItems)),
                                                          mapping, srcModel, getTargetModel(tgtNs));
        ((PsiDataTranslator) translator).setOrganisms(organisms);
        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        // uncomment to write a new target items file
        //writeItemsFile(tgtIw.getItems(), "psi-tgt-items.xml");

        assertEquals(readItemSet("test/PsiDataTranslatorTest_tgt.xml"), tgtIw.getItems());
    }


    protected String getModelName() {
        return "genomic";
    }

    protected String getSrcModelName() {
        return "psi";
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
        return mockIw.getItems();
    }
}
