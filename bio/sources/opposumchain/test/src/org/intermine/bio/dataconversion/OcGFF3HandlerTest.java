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
 * Tests for the OcGFF3Handler class.
 *
 * @author Wenyan Ji
 */

public class OcGFF3HandlerTest extends ItemsTestCase
{
    GFF3RecordHandler handler;
    GFF3Converter converter;

    GFF3Parser parser = new GFF3Parser();
    MockItemWriter writer = new MockItemWriter(new LinkedHashMap());
    String seqClsName = "Chromosome";
    String orgTaxonId = "123";
    String dataSourceName = "UCSC";
    String dataSetTitle = "UCSC opossum chain";

    
    public OcGFF3HandlerTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        Model tgtModel = Model.getInstanceByName("genomic");
        handler = new GFF3RecordHandler(tgtModel);
        converter = new GFF3Converter(writer, seqClsName, orgTaxonId, dataSourceName, dataSetTitle,
                                      dataSourceName, tgtModel, handler);
    }

    public void testParse() throws Exception {
        BufferedReader srcReader = new BufferedReader(new
                   InputStreamReader(getClass().getClassLoader().getResourceAsStream("test/opposumChain.gff")));
        converter.parse(srcReader);
        converter.store();

        // uncomment to write out a new target items file
        //writeItemsFile(writer.getItems(), "opposum-tgt-items.xml");
        
        Set expected = readItemSet("test/opposumTgt.xml");
        assertEquals(expected, writer.getItems());
    }
}
