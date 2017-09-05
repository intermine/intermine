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
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

/**
 * Class to read a GFF3 source data and produce a data representation
 *
 * @author Wenyan Ji
 * @author Richard Smith
 * @author Fengyuan Hu
 * @author Vivek Krishnakumar
 */

public class GFF3ConverterTest extends ItemsTestCase {
    public GFF3ConverterTest(String arg) {
        super(arg);
    }

    GFF3Converter converter;
    File f = null;

    MockItemWriter writer = new MockItemWriter(new LinkedHashMap<String, Item>());
    String seqClsName = "Chromosome";
    String flyTaxonId = "7227";
    String ratTaxonId = "10116";
    String thaleTaxonId = "3702";
    String dataSourceName = "UCSC";
    String dataSetTitle = "UCSC data set";

    public void setUp() throws Exception {
        Model tgtModel = Model.getInstanceByName("genomic");
        converter = new GFF3Converter(writer, seqClsName, flyTaxonId, dataSourceName,
                                      dataSetTitle, tgtModel,
                                      new GFF3RecordHandler(tgtModel), null);
    }

    public void tearDown() throws Exception {
        converter.close();
        if (f != null) {
            f.delete();
        }
    }

    /**
     * Test creating items with dontCreateLocations flag false.
     */
    public void testParseLocated() throws Exception {
        BufferedReader srcReader = new BufferedReader(new
                                                      InputStreamReader(getClass().getClassLoader().getResourceAsStream("test.gff")));
        converter.parse(srcReader);
        converter.storeAll();

        // uncomment to write out a new target items file
        //writeItemsFile(writer.getItems(), "gff_located_item_test.xml");

        assertEquals(readItemSet("GFF3ConverterTest.xml"), writer.getItems());
    }


    /**
     * Test creating items with dontCreateLocations flag true.
     */
    public void testParseUnLocated() throws Exception {
        BufferedReader srcReader = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("test.gff")));
        converter.setDontCreateLocations(true);
        converter.parse(srcReader);
        converter.storeAll();

        Set<org.intermine.xml.full.Item> expected = new HashSet<org.intermine.xml.full.Item>(readItemSet("GFF3ConverterTestUnLocated.xml"));

        // uncomment to write out a new target items file
        //writeItemsFile(writer.getItems(), "gff_unlocated_item_test.xml");

        assertEquals(expected, writer.getItems());
    }

    /**
     * Test creating items with dontCreateLocations flag true.
     */
    public void testParseDiscontinuous() throws Exception {
        BufferedReader srcReader = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("test-locs.gff")));
        converter.parse(srcReader);
        converter.storeAll();

        Set<org.intermine.xml.full.Item> expected = new HashSet<org.intermine.xml.full.Item>(readItemSet("GFF3ConverterTestDiscontinuous.xml"));

        // uncomment to write out a new target items file
        //writeItemsFile(writer.getItems(), "gff_unlocated_item_test.xml");

        assertEquals(expected, writer.getItems());
    }

//    /**
//     * Test RGD
//     */
//    public void testRGD() throws Exception {
//        /* Add to gff_config.properties:
//            10116.terms=gene, mRNA, Exon, CDS, ThreePrimeUTR, FivePrimeUTR
//            10116.attributes.ID=primaryIdentifier
//            10116.gene.attributes.ID=secondaryIdentifier
//            10116.attributes.Note=description
//            10116.mRNA.attributes.Type=scoreType
//            10116.attributes.Dbxref.EntrezGene=ncbiGeneNumber
//            10116.attributes.Dbxref.EnsemblGenes=synonym
//        */
//        Model tgtModel = Model.getInstanceByName("genomic");
//        converter = new GFF3Converter(writer, seqClsName, ratTaxonId, dataSourceName,
//                dataSetTitle, tgtModel,
//                new GFF3RecordHandler(tgtModel), null);
//        BufferedReader srcReader = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("test_rgd.gff")));
//        converter.parse(srcReader);
//        converter.storeAll();
//
//        Set<org.intermine.xml.full.Item> expected = new HashSet<org.intermine.xml.full.Item>(readItemSet("GFF3ConverterTestRGD.xml"));
//
//        // uncomment to write out a new target items file
//        writeItemsFile(writer.getItems(), "gff_rgd_test.xml");
//
//        assertEquals(expected, writer.getItems());
//    }

    /**
     * Test GFF3 Excludes
     */
    public void testExcludes() throws Exception {
        /* Add to gff_config.properties:
            3702.excludes=CDS
            3702.attributes.symbol=symbol
            3702.attributes.Note=briefDescription
        */
        Model tgtModel = Model.getInstanceByName("genomic");
        converter = new GFF3Converter(writer, seqClsName, thaleTaxonId, dataSourceName,
                dataSetTitle, tgtModel,
                new GFF3RecordHandler(tgtModel), null);
        BufferedReader srcReader = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("test_excludes.gff")));
        converter.parse(srcReader);
        converter.storeAll();

        Set<org.intermine.xml.full.Item> expected = new HashSet<org.intermine.xml.full.Item>(readItemSet("GFF3ConverterTestExcludes.xml"));

        // uncomment to write out a new target items file
        //writeItemsFile(writer.getItems(), "gff_excludes_test.xml");

        assertEquals(expected, writer.getItems());
    }

    /**
     * Test chromosome is not created twice.
     */
    public void testChromosome() throws Exception {
        BufferedReader srcReader = new BufferedReader(new
                                                      InputStreamReader(getClass().getClassLoader().getResourceAsStream("test_worm_chr.gff")));
        converter.parse(srcReader);
        converter.storeAll();

        // uncomment to write out a new target items file
        // writeItemsFile(writer.getItems(), "gff_worm_chromosome_item_test.xml");

        assertEquals(readItemSet("GFF3ConverterTestChromosome.xml"), writer.getItems());
    }
}
