package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Set;

import org.intermine.bio.dataconversion.GFF3Converter;
import org.intermine.bio.dataconversion.TilingPathGFF3RecordHandler;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

/**
 * Tests for the TilingPathGFF3RecordHandler class.
 *
 * @author Julie Sullivan
 */

public class TilingPathGFF3RecordHandlerTest extends ItemsTestCase
{
    private Model tgtModel;
    private TilingPathGFF3RecordHandler handler;
    private String seqClsName = "Chromosome";
    private String dataSourceName = "Genetics";
    private String dataSetTitle = "Department of Genetics tiling path data set";
    private GFF3Converter converter;
    private String orgAbbrev = "DM";
    private MockItemWriter writer = new MockItemWriter(new LinkedHashMap<String, Item>());

    public TilingPathGFF3RecordHandlerTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        tgtModel = Model.getInstanceByName("genomic");

        handler = new TilingPathGFF3RecordHandler(tgtModel);

        converter = new GFF3Converter(writer, seqClsName, orgAbbrev, dataSourceName,
                                      dataSetTitle, tgtModel, handler, null);

    }

    public void tearDown() throws Exception {
        converter.close();
    }

    public void testTilingPathHandler() throws Exception {
        String gff =
        "chr2L\ttile_spans\ttiling_path_span\t1\t7529\t.\t.\t.\tID=span2L:1-7529;oldID=span2L:1-7529;newID=span2L:0000001\n"
      + "chr2L\ttile_spans\tPCR_product\t102297\t103425\t.\t.\t.\tID=span2L:102383-106718_amplimer_1;promotor=0 ;Parent=span2L:1-7529;oldID=span2L:102383-106718_amplimer_1;newID=span2L:0000013_amplimer_1;newParent=span2L:0000013\n"
      + "chr2L\ttile_spans\tforward_primer\t102297\t102318\t.\t.\t.\tName=ACTAAGAGTGAGCTCCGTGAGG ;Parent=span2L:102383-106718_amplimer_1;ID=span2L:102383-106718_amplimer_1_forward_primer;oldID=span2L:102383-106718_amplimer_1_forward_primer;newID=span2L:0000013_amplimer_1_forward_primer;newParent=span2L:0000013\n";

        BufferedReader srcReader = new BufferedReader(new StringReader(gff));
        converter.parse(srcReader);
        converter.storeAll();

        // uncomment to write a new target items files
        //writeItemsFile(writer.getItems(), "tiling-path-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("TilingPathGFF3RecordHandlerTest.xml");

        assertEquals(expected, writer.getItems());
    }

}
