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

import org.intermine.bio.io.gff3.GFF3Parser;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;


/**
 * Tests for the RegionGFF3Handler class.
 *
 * @author Wenyan Ji
 */

public class RegionGFF3HandlerTest extends ItemsTestCase
{
    RegionGFF3RecordHandler handler;
    GFF3Converter converter;

    GFF3Parser parser = new GFF3Parser();
    MockItemWriter writer = new MockItemWriter(new LinkedHashMap());
    String seqClsName = "Chromosome";
    String orgTaxonId= "9606";
    String dataSourceName = "UCSC";
    String dataSetTitle = "UCSC data set";
    
    public RegionGFF3HandlerTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        Model tgtModel = Model.getInstanceByName("genomic");
        handler = new RegionGFF3RecordHandler(tgtModel);
        converter = new GFF3Converter(writer, seqClsName, "9606", dataSourceName, dataSetTitle,
                                      dataSourceName, tgtModel, handler, null);
    }

    public void testParse() throws Exception {
        BufferedReader srcReader = new BufferedReader(new
             InputStreamReader(getClass().getClassLoader().getResourceAsStream("region.gff")));
        converter.parse(srcReader);
        converter.store();

        // uncomment to write a new target items file
        //writeItemsFile(writer.getItems(), "region-tgt-items.xml");

        Set expected = readItemSet("regiontgt.xml");
        assertEquals(expected, writer.getItems());
    }
}
