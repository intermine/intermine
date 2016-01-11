package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

public class LongOligoGFF3RecordHandlerTest extends ItemsTestCase
{
    LongOligoGFF3RecordHandler handler;
    GFF3Converter converter;

    MockItemWriter writer = new MockItemWriter(new LinkedHashMap<String, Item>());
    String seqClsName = "MRNA";
    String taxonId = "7227";
    String dataSetTitle = "INDAC long oligo data set";
    String dataSourceName = "Micklem lab";

    public LongOligoGFF3RecordHandlerTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        Model tgtModel = Model.getInstanceByName("genomic");
        handler = new LongOligoGFF3RecordHandler(tgtModel);
        LongOligoGFF3SeqHandler seqHandler = new LongOligoGFF3SeqHandler();
        seqHandler.rslv = IdResolverService.getMockIdResolver("mRNA");
        seqHandler.rslv.addResolverEntry("7227", "FBtr0075391", Collections.singleton("CG4314-RA"));
        converter = new GFF3Converter(writer, seqClsName, taxonId, dataSourceName, dataSetTitle,
                                      tgtModel, handler, seqHandler);
    }

    public void tearDown() throws Exception {
        converter.close();
    }

    public void testParse() throws Exception {
        String gff = "CG4314-RA\tINDAC_1.0\tmicroarray_oligo\t0\t0\t.\t.\t.\tID=1000044388;olen=14;oaTm=92.25;geneID=CG4314;Alias=12-CG4314-RA_1;sequence=ACACGGGTCAGGAT";
        BufferedReader srcReader = new BufferedReader(new StringReader(gff));
        converter.parse(srcReader);
        converter.storeAll();

        // uncomment to write a new items xml file
        //writeItemsFile(writer.getItems(), "long-oligo_items.xml");

        Set<?> expected = readItemSet("LongOligoGFF3RecordHandlerTest.xml");
        assertEquals(expected, writer.getItems());
    }
}
