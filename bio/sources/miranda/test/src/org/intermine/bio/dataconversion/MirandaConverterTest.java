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

import java.io.File;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

/**
 * Test for data from miRanda
 * 
 * @author "Xavier Watkins"
 *
 */
public class MirandaConverterTest extends ItemsTestCase
{
    MockItemWriter itemWriter;
    MirandaConverter converter;
    
    public MirandaConverterTest(String arg) {
        super(arg);
    }

    protected void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap());
        converter = new MirandaConverter(itemWriter, Model.getInstanceByName("genomic"));
        MockIdResolverFactory resolverFactory = new MockIdResolverFactory("Gene");
        resolverFactory.addResolverEntry("7227", "FBgn001", Collections.singleton("mir-92b"));
        resolverFactory.addResolverEntry("7227", "FBgn002", Collections.singleton("mir-312"));
        resolverFactory.addResolverEntry("7227", "FBgn003", Collections.singleton("mir-313"));
        resolverFactory.addResolverEntry("7227", "FBgn004", Collections.singleton("mir-7"));
        converter.resolverFactory = resolverFactory;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testMirandaHandler() throws Exception {
        String gff =
            "dme-miR-312\tmiRanda\tmiRNA_target\t9403\t9424\t16.9418\t+\t.\ttarget=CG11023-RA;score=3.057390e-02"
                     + ENDL
                     + "dme-miR-92b\tmiRanda\tmiRNA_target\t9403\t9424\t17.7377\t+\t.\ttarget=CG11023-RA;score=1.179130e-02"
                     + ENDL;
        converter.setCurrentFile(new File("v5.gff.drosophila_melanogaster"));
        converter.process(new StringReader(gff));
        converter.close();
        
        // uncomment to write a new tgt items file
        writeItemsFile(itemWriter.getItems(), "/tmp/miranda-tgt-items.xml");

        Set expected = readItemSet("miranda-tgt-items.xml");
//       System.out.println(ItemsTestCase.compareItemSets(expected, allItems));
        assertEquals(expected, itemWriter.getItems());
    }

}
