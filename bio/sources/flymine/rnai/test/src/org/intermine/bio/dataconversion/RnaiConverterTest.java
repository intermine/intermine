package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.FullParser;

public class RnaiConverterTest extends ItemsTestCase
{
    MockItemWriter itemWriter;
    RnaiConverter converter;
    public RnaiConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap());
        converter = new RnaiConverter(itemWriter, Model.getInstanceByName("genomic"));

        MockIdResolverFactory resolverFactory = new MockIdResolverFactory("Gene");
        resolverFactory.addResolverEntry("7227", "FBgn0015806", Collections.singleton("FBgn001"));
        resolverFactory.addResolverEntry("7227", "FBgn0053207", Collections.singleton("FBgn002"));
        converter.resolverFactory = resolverFactory;
    }

    public void testProcess() throws Exception {

        File srcFile = new File(getClass().getClassLoader().getResource("all_screens_genomeRNAi.txt").toURI());
        converter.setCurrentFile(srcFile);
        converter.process(new FileReader(srcFile));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "rnai-converter-tgt.xml");

        assertEquals(readItemSet("RnaiConverterTest.xml"), itemWriter.getItems());
    }
}
