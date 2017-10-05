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

import org.intermine.bio.dataconversion.IdResolverService;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;

/**
 * Tests for the FlyRegGFF3RecordHandler class.
 *
 * @author Kim Rutherford
 */

public class FlyRegGFF3RecordHandlerTest extends ItemsTestCase
{
    private Model tgtModel;
    private FlyRegGFF3RecordHandler handler;
    private String seqClsName = "Chromosome";
    private String taxonId = "7227";
    private String dataSourceName = "FlyReg";
    private String dataSetTitle = "FlyReg data set";
    private GFF3Converter converter;
    private MockItemWriter writer = new MockItemWriter(new LinkedHashMap<String, org.intermine.model.fulldata.Item>());

    public FlyRegGFF3RecordHandlerTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        tgtModel = Model.getInstanceByName("genomic");

        handler = new FlyRegGFF3RecordHandler(tgtModel);

        handler.rslv = IdResolverService.getMockIdResolver("Gene");
        handler.rslv.addResolverEntry("7227", "FBgn001", Collections.singleton("dpp"));
        handler.rslv.addResolverEntry("7227", "FBgn002", Collections.singleton("dl"));

        converter = new GFF3Converter(writer, seqClsName, taxonId, dataSourceName,
                                      dataSetTitle, tgtModel, handler, null);

    }

    public void tearDown() throws Exception {
        converter.close();
    }

    public void testFlyRegHandler() throws Exception {
        String gff =
            "2L\tREDfly\tregulatory_region\t2456365\t2456372\t.\t.\t.\tID=Unspecified_dpp:REDFLY:TF000068; Dbxref=Flybase:FBgn0000490, PMID:8543160, REDfly:644, FlyBase:; Evidence=footprint/binding assay; Factor=Unspecified; Target=dpp\n"
            + "2L\tREDfly\tregulatory_region\t2456352\t2456369\t.\t.\t.\tID=dl_dpp:REDFLY:TF000069; Dbxref=Flybase:FBgn0000490, PMID:8458580, REDfly:645, FlyBase:FBgn0000463; Evidence=footprint/binding assay; Factor=dl; Target=dpp\n"
            + "2L\tREDfly\tregulatory_region\t2456423\t2456433\t.\t.\t.\tID=Unspecified_dpp:REDFLY:TF000067; Dbxref=Flybase:FBgn0000490, PMID:8543160, REDfly:643, FlyBase:; Evidence=footprint/binding assay; Factor=Unspecified; Target=dpp\n";

        BufferedReader srcReader = new BufferedReader(new StringReader(gff));
        converter.parse(srcReader);
        converter.storeAll();

        // uncomment to write a new target items files
        // writeItemsFile(writer.getItems(), "flyreg-tgt-items.xml");

        Set<Item> expected = readItemSet("FlyRegGFF3RecordHandlerTest.xml");

        assertEquals(expected, writer.getItems());
    }

    public void testFlyRegHandlerNewFormat() throws Exception {
        String gff =
            "2L\tREDfly\tregulatory_region\t2456365\t2456372\t.\t.\t.\tID=\"Unspecified_dpp:REDFLY:TF000068\"; Dbxref=\"Flybase:FBgn0000490, PMID:8543160, REDfly:644, FlyBase:\"; Evidence=footprint/binding assay; factor=Unspecified; target=dpp\n"
            + "2L\tREDfly\tregulatory_region\t2456352\t2456369\t.\t.\t.\tID=\"dl_dpp:REDFLY:TF000069\"; Dbxref=\"Flybase:FBgn0000490, PMID:8458580, REDfly:645, FlyBase:FBgn0000463\"; Evidence=footprint/binding assay; factor=dl; target=dpp\n"
            + "2L\tREDfly\tregulatory_region\t2456423\t2456433\t.\t.\t.\tID=\"Unspecified_dpp:REDFLY:TF000067\"; Dbxref=\"Flybase:FBgn0000490, PMID:8543160, REDfly:643, FlyBase:\"; Evidence=footprint/binding assay; factor=Unspecified; target=dpp\n";

        BufferedReader srcReader = new BufferedReader(new StringReader(gff));
        converter.parse(srcReader);
        converter.storeAll();

        // uncomment to write a new target items files
        // writeItemsFile(writer.getItems(), "flyreg-tgt-items-newformat.xml");

        Set<Item> expected = readItemSet("FlyRegGFF3RecordHandlerNewFormatTest.xml");

        assertEquals(expected, writer.getItems());
    }

}
