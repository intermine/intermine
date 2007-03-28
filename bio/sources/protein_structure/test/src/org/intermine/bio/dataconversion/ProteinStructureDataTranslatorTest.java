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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.dataconversion.MockItemReader;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.xml.full.FullParser;

public class ProteinStructureDataTranslatorTest extends DataTranslatorTestCase {
    private String tgtNs = "http://www.flymine.org/model/genomic#";

    public ProteinStructureDataTranslatorTest(String arg) {
        super(arg, "osw.bio-fulldata-test");
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testTranslate() throws Exception {
        Map itemMap = writeItems(getSrcItems());
        ProteinStructureDataTranslator translator =
            new ProteinStructureDataTranslator(new MockItemReader(itemMap),
                                               mapping, srcModel, getTargetModel(tgtNs));
        translator.setSrcDataDir("resouces/");
        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);



        System.out.println(DataTranslatorTestCase.printCompareItemSets(new HashSet(getExpectedItems()), tgtIw.getItems()));

        // uncomment to write a new target items file
        //java.io.FileWriter fw = new java.io.FileWriter(new java.io.File("protein-structure_tgt.xml"));
        //fw.write(org.intermine.xml.full.FullRenderer.render(tgtIw.getItems()));
        //fw.close();

        assertEquals(new HashSet(getExpectedItems()), tgtIw.getItems());
    }

    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("ProteinStructureDataTranslatorFunctionalTest_tgt.xml"));
    }

    protected Collection getSrcItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("ProteinStructureDataTranslatorFunctionalTest_src.xml"));
    }

    protected String getModelName() {
        return "genomic";
    }

    protected String getSrcModelName() {
        return "protein_structure";
    }
}
