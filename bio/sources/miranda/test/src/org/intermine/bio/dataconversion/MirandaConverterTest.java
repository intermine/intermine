package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.intermine.bio.io.gff3.GFF3Parser;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.util.TypeUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;

/**
 * Test for data from miRanda
 * 
 * @author "Xavier Watkins"
 *
 */
public class MirandaConverterTest extends ItemsTestCase
{

    private Model tgtModel;
    private MirandaConverter converter;
    private MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
    MockItemWriter itemWriter;
    
    public MirandaConverterTest(String arg) {
        super(arg);
    }

    protected void setUp() throws Exception {
        itemWriter = new MockItemWriter(new HashMap());
        tgtModel = Model.getInstanceByName("genomic");
        converter = new MirandaConverter(itemWriter, tgtModel);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        converter.close();
    }
    
//    public void testMirandaHandler() throws Exception {
//        String gff =
//            "dme-miR-312\tmiRanda\tmiRNA_target\t9403\t9424\t16.9418\t+\t.\ttarget=CG11023-RA;score=3.057390e-02"
//                     + ENDL
//                     + "dme-miR-92b\tmiRanda\tmiRNA_target\t9403\t9424\t17.7377\t+\t.\ttarget=CG11023-RA;score=1.179130e-02"
//                     + ENDL
//                     + "dme-miR-313\tmiRanda\tmiRNA_target\t9404\t9424\t17.3966\t+\t.\ttarget=CG11023-RA;score=1.917020e-02"
//                     + ENDL
//                     + "dme-miR-7\tmiRanda\tmiRNA_target\t9445\t9467\t18.101\t+\t.\ttarget=CG11023-RA;score=1.308960e-02";
//
//        converter.process(new StringReader(gff));
//        converter.close();
//        
//        // uncomment to write a new tgt items file
////        writeItemsFile(itemWriter.getItems(), "/tmp/miranda-tgt-items.xml");
//
//        Set expected = readItemSet("miranda-tgt-items.xml");
////       System.out.println(ItemsTestCase.compareItemSets(expected, allItems));
//        assertEquals(expected, itemWriter.getItems());
//    }

}
