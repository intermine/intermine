package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileWriter;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.FullParser;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.MockItemWriter;
import org.flymine.io.gff3.GFF3Parser;
import org.flymine.io.gff3.GFF3Record;
import org.flymine.dataconversion.GFF3Converter;

import org.apache.log4j.Logger;

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
    String orgAbbrev = "HS";
    String infoSourceTitle = "UCSC";
    String targetNameSpace = "http://www.flymine.org/model/genomic#";

    public void setUp() {
        converter = new GFF3Converter(parser, writer, seqClsName, orgAbbrev, infoSourceTitle, targetNameSpace);
    }

    public void tearDown() throws Exception {
        converter.close();
        if (f != null) {
            f.delete();
        }
    }

    public void testParse() throws Exception {
        BufferedReader srcReader = new BufferedReader(new
                   InputStreamReader(getClass().getClassLoader().getResourceAsStream("test/test.gff")));
        converter.parse(srcReader);

        FileWriter writerSrc = new FileWriter(new File("gff_items.xml"));
        writerSrc.write(FullRenderer.render(writer.getItems()));
        writerSrc.close();

        Set expected = new HashSet(getExpectedItems());
        assertEquals(expected, writer.getItems());
    }


    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/GFF3ConverterTest.xml"));
    }

}
