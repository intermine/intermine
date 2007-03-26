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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import org.intermine.bio.io.gff3.GFF3Parser;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.Item;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import junit.framework.TestCase;

/**
 * Class to read a GFF3 source data and produce a data representation
 *
 * @author Wenyan Ji
 * @author Richard Smith
 */

public class GFF3ConverterTest extends TestCase {
    GFF3Converter converter;
    File f = null;

    GFF3Parser parser = new GFF3Parser();
    MockItemWriter writer = new MockItemWriter(new LinkedHashMap());
    String seqClsName = "Chromosome";
    String orgTaxonId = "7227";
    String dataSourceName = "UCSC";
    String seqDataSourceName = "MGI";
    String dataSetTitle = "UCSC data set";

    public void setUp() throws Exception {
        Model tgtModel = Model.getInstanceByName("genomic");
        converter = new GFF3Converter(writer, seqClsName, orgTaxonId, dataSourceName,
                                      dataSetTitle, seqDataSourceName, tgtModel,
                                      new GFF3RecordHandler(tgtModel));
    }

    public void tearDown() throws Exception {
        converter.close();
        if (f != null) {
            f.delete();
        }
    }

    public void testParse() throws Exception {
        BufferedReader srcReader = new BufferedReader(new
                   InputStreamReader(getClass().getClassLoader().getResourceAsStream("test.gff")));
        converter.parse(srcReader);
        converter.store();

        //FileWriter writerSrc = new FileWriter(new File("gff_items.xml"));
        //writerSrc.write(FullRenderer.render(writer.getItems()));
        //writerSrc.close();

        Set expected = new HashSet(getExpectedItems());

        String expectedNotActual = "in expected, not actual: " + compareItemSets(expected, writer.getItems());
        String actualNotExpected = "in actual, not expected: " + compareItemSets(writer.getItems(), expected);
        if (expectedNotActual.length() > 25) {
            System.out.println(expectedNotActual);
            System.out.println(actualNotExpected);
        }

        assertEquals(expected, writer.getItems());
    }


    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("GFF3ConverterTest.xml"));
    }

/**
     * Given two sets of Items (a and b) return a set of Items that are present in a
     * but not b.
     * @param a a set of Items
     * @param b a set of Items
     * @return the set of Items in a but not in b
     */
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

}
