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
import java.util.LinkedHashMap;
import java.util.Map;

import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.dataconversion.MockItemReader;
import org.intermine.dataconversion.MockItemWriter;

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

        // uncomment to write a new target items file
        writeItemsFile(tgtIw.getItems(), "protein-structure_tgt.xml");

        assertEquals(readItemSet("ProteinStructureDataTranslatorFunctionalTest_tgt.xml"), tgtIw.getItems());
    }

    protected Collection getSrcItems() throws Exception {
        return readItemSet("ProteinStructureDataTranslatorFunctionalTest_src.xml");
    }

    protected String getModelName() {
        return "genomic";
    }

    protected String getSrcModelName() {
        return "protein_structure";
    }
}
