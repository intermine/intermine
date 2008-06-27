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

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class Drosophila2ProbeConverterTest extends ItemsTestCase
{
    private String ENDL = System.getProperty("line.separator");
    Model model = Model.getInstanceByName("genomic");
    Drosophila2ProbeConverter converter;
    MockItemWriter itemWriter;

    public Drosophila2ProbeConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap());
        converter = new Drosophila2ProbeConverter(itemWriter, model);
        MockIdResolverFactory resolverFactory = new MockIdResolverFactory("Gene");
        resolverFactory.addResolverEntry("7227", "FBgn005", Collections.singleton("FBgn001"));
        resolverFactory.addResolverEntry("7227", "FBgn002", Collections.singleton("FBgn002"));
        resolverFactory.addResolverEntry("7227", "FBgn003", Collections.singleton("FBgn003"));
        resolverFactory.addResolverEntry("7227", "FBgn004", Collections.singleton("FBgn004"));
        converter.resolverFactory = resolverFactory;
    }

    public void testProcess() throws Exception {

        Reader reader = new InputStreamReader(getClass().getClassLoader()
                                              .getResourceAsStream("Drosophilia2ProbeConverterTest_src.csv"));
        converter.process(reader);
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "affy-probes-tgt-items.xml");

        assertEquals(readItemSet("test/Drosophila2ProbeConverterTest.xml"), itemWriter.getItems());
    }

}
