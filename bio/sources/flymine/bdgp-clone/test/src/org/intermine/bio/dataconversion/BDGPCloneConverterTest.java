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

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

/**
 * BDGP clone converter functional test.
 * @author Richard Smith
 */
public class BDGPCloneConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    BDGPCloneConverter converter;
    MockItemWriter itemWriter;

    public BDGPCloneConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap());
        converter = new BDGPCloneConverter(itemWriter, model);
        MockIdResolverFactory resolverFactory = new MockIdResolverFactory("Gene");
        resolverFactory.addResolverEntry("7227", "FBgn001", Collections.singleton("CG9480"));
        converter.resolverFactory = resolverFactory;
    }

    public void testProcess() throws Exception {
        String ENDL = System.getProperty("line.separator");
        String input = "# comment" + ENDL
            + "CG9480\tGlycogenin\tFBgn0034603\tRE02181;RE21586" + ENDL;

        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "bdgp-clone-tgt-items.xml");

        Set expected = readItemSet("BDGPCloneConverterTest.xml");
        assertEquals(expected, itemWriter.getItems());
    }
}
