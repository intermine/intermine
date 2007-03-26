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

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.intermine.bio.dataconversion.DrosdelDataTranslator;
import org.intermine.dataconversion.DataTranslator;
import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.dataconversion.MockItemReader;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.xml.full.FullParser;


/**
 * Tests for the DrosdelDataTranslator class.
 *
 * @author Kim Rutherford
 */

public class DrosdelDataTranslatorTest extends DataTranslatorTestCase {
    private String tgtNs = "http://www.flymine.org/model/genomic#";

    public DrosdelDataTranslatorTest (String arg) {
        super(arg, "osw.bio-fulldata-test");
    }

    public void testTranslate() throws Exception {
        Collection srcItems = getSrcItems();

        DataTranslator translator =
            new DrosdelDataTranslator(new MockItemReader(writeItems(srcItems)),
                                      mapping, srcModel, getTargetModel(tgtNs));
        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        if (getExpectedItems() == null) {
            throw new RuntimeException();
        }
        assertEquals(new HashSet(getExpectedItems()), tgtIw.getItems());
    }

    protected String getModelName() {
        return "genomic";
    }

    protected String getSrcModelName() {
        return "drosdel";
    }

    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("DrosdelDataTranslatorTest_tgt.xml"));
    }

    protected Collection getSrcItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("DrosdelDataTranslatorTest_src.xml"));
    }
}
