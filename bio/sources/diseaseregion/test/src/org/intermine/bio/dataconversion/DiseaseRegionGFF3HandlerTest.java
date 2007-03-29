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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Set;

import org.intermine.bio.DiseaseRegionGFF3RecordHandler;
import org.intermine.bio.io.gff3.GFF3Parser;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

/**
 * Tests for the DiseaseRegionGFF3Handler class.
 *
 * @author Wenyan Ji
 */

public class DiseaseRegionGFF3HandlerTest extends ItemsTestCase
{
    DiseaseRegionGFF3RecordHandler handler;
    GFF3Converter converter;

    GFF3Parser parser = new GFF3Parser();
    MockItemWriter writer = new MockItemWriter(new LinkedHashMap());
    String seqClsName = "Chromosome";
    String orgAbbrev = "HS";
    String dataSourceName = "T1DBase";
    String dataSetTitle = "T1DBase disease regions";

    public DiseaseRegionGFF3HandlerTest(String arg) {
        super(arg);
    }
    
    public void setUp() throws Exception {
        Model tgtModel = Model.getInstanceByName("genomic");
        handler = new DiseaseRegionGFF3RecordHandler(tgtModel);
        converter = new GFF3Converter(writer, seqClsName, orgAbbrev, dataSourceName, dataSetTitle,
                                      "test", tgtModel, handler);
    }

    public void tearDown() throws Exception {
        converter.close();
    }

    public void testParse() throws Exception {
        BufferedReader srcReader = new BufferedReader(new
                   InputStreamReader(getClass().getClassLoader().getResourceAsStream("test/diseaseregion.gff")));
        converter.parse(srcReader);
        converter.store();

        // uncomment to write a new items xml file
        writeItemsFile(writer.getItems(), "diseaseregion_items.xml");

        Set expected = readItemSet("test/diseaseregiontgt.xml");
        assertEquals(expected, writer.getItems());
    }
}