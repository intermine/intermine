package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.intermine.bio.dataconversion.IdResolverService;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

/**
 * Tests for the RedFlyGFF3RecordHandler class.
 *
 * @author Julie
 */
public class NcbiGffGFF3RecordHandlerTest extends ItemsTestCase
{
    private Model tgtModel;
    private NcbiGffGFF3RecordHandler handler;
    private String seqClsName = "Chromosome";
    private String taxonId = "9606";
    private String dataSourceName = "NCBI";
    private String dataSetTitle = "NCBI gff data set";
    private GFF3Converter converter;
    private MockItemWriter writer = new MockItemWriter(new LinkedHashMap<String, Item>());

    public NcbiGffGFF3RecordHandlerTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        tgtModel = Model.getInstanceByName("genomic");
        handler = new NcbiGffGFF3RecordHandler(tgtModel);
        // call the GFF3Converter constructor to initialise the handler

        NcbiGffGFF3SeqHandler seqHandler = new NcbiGffGFF3SeqHandler();

        converter = new GFF3Converter(writer, seqClsName, taxonId, dataSourceName,
                          dataSetTitle, tgtModel, handler, seqHandler);
    }

    public void tearDown() throws Exception {
        converter.close();
    }


    public void testHandler() throws Exception {
        File srcFile = new File(getClass().getClassLoader().getResource("test.gff").toURI());

        BufferedReader srcReader = new BufferedReader(new FileReader(srcFile));
        converter.parse(srcReader);
        converter.storeAll();

        // uncomment to write a new tgt items file
        //writeItemsFile(writer.getItems(), "ncbi-gff-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = new HashSet<org.intermine.xml.full.Item>(readItemSet("NcbiGffGFF3RecordHandler-tgt.xml"));
        assertEquals(expected, writer.getItems());
    }
}
