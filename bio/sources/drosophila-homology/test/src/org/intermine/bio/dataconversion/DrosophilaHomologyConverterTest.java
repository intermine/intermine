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
import java.util.Collections;
import java.util.HashMap;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class DrosophilaHomologyConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    DrosophilaHomologyConverter converter;
    MockItemWriter itemWriter;

    public DrosophilaHomologyConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap());
        converter = new DrosophilaHomologyConverter(itemWriter, model);
        MockIdResolverFactory resolverFactory = new MockIdResolverFactory("Gene");
        resolverFactory.addResolverEntry("7234", "FBgn001", Collections.singleton("dper_GLEANR_1559"));
        resolverFactory.addResolverEntry("7234", "FBgn002", Collections.singleton("dper_GLEANR_1561"));
        resolverFactory.addResolverEntry("7227", "FBgn003", Collections.singleton("FBgn0031344"));
        resolverFactory.addResolverEntry("7230", "FBgn004", Collections.singleton("dmoj_GLEANR_1887"));
        converter.resolverFactory = resolverFactory;
    }

    public void testProcess() throws Exception {

        InputStreamReader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("DrosophilaHomologyConverterTest_src.tsv"));
        converter.process(reader);

        // uncomment to write out a new target items file
//        writeItemsFile(itemWriter.getItems(), "/tmp/dros-homology-tgt.xml");

        assertEquals(readItemSet("DrosophilaHomologyConverterTest.xml"), itemWriter.getItems());
    }
}
