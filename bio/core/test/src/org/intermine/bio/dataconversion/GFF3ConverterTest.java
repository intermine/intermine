package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.FullParser;

/**
 * Class to read a GFF3 source data and produce a data representation
 *
 * @author Wenyan Ji
 * @author Richard Smith
 */

public class GFF3ConverterTest extends ItemsTestCase {
    public GFF3ConverterTest(String arg) {
        super(arg);
    }

    GFF3Converter converter;
    File f = null;

    MockItemWriter writer = new MockItemWriter(new LinkedHashMap());
    String seqClsName = "Chromosome";
    String orgTaxonId = "7227";
    String dataSourceName = "UCSC";
    String dataSetTitle = "UCSC data set";

    public void setUp() throws Exception {
        Model tgtModel = Model.getInstanceByName("genomic");
        converter = new GFF3Converter(writer, seqClsName, orgTaxonId, dataSourceName,
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


        Set expected = new HashSet(getExpectedLocatedItems());

        // uncomment to write out a new target items file
        //writeItemsFile(writer.getItems(), "gff_located_item_test.xml");

        assertEquals(expected, writer.getItems());
    }

    protected Collection getExpectedLocatedItems() throws Exception {
        InputStream resource =
            getClass().getClassLoader().getResourceAsStream("GFF3ConverterTest.xml");
        return FullParser.parse(resource);
    }

    /**
     * Test creating items with dontCreateLocations flag true.
     */
    public void testParseUnLocated() throws Exception {
        BufferedReader srcReader = new BufferedReader(new
                                                      InputStreamReader(getClass().getClassLoader().getResourceAsStream("test.gff")));
        converter.setDontCreateLocations(true);
        converter.parse(srcReader);
        converter.storeAll();

        Set expected = new HashSet(getExpectedUnLocatedItems());

        // uncomment to write out a new target items file
        //writeItemsFile(writer.getItems(), "gff_unlocated_item_test.xml");

        assertEquals(expected, writer.getItems());
    }


    protected Collection getExpectedUnLocatedItems() throws Exception {
        InputStream resource =
            getClass().getClassLoader().getResourceAsStream("GFF3ConverterTestUnLocated.xml");
        return FullParser.parse(resource);
    }

    /**
     * Given two sets of Items (a and b) return a set of Items that are present in a
     * but not b.
     * @param a a set of Items
     * @param b a set of Items
     * @return the set of Items in a but not in b
     */
    /*
    public Set compareItemSets(Set a, Set b) {
        Set diff = new HashSet(a);
        Iterator i = a.iterator();
        while (i.hasNext()) {
            Item itemA = (Item) i.next();
            Iterator j = b.iterator();
            while (j.hasNext()) {
                Item itemB = (Item) j.next();
                if (itemA.equals(itemB)) {
                    diff.remove(itemA);
                }
            }
        }
        return diff;
    }
     */
}
