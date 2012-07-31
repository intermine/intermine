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
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class ReactomeConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    ReactomeConverter converter;
    MockItemWriter itemWriter;

    public ReactomeConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap());
        converter = new ReactomeConverter(itemWriter, model);
        MockIdResolverFactory resolverFactory = new MockIdResolverFactory("Gene");
        resolverFactory.addResolverEntry("7227", "FBgn001", Collections.singleton("FBgn001"));
        resolverFactory.addResolverEntry("7227", "FBgn003", Collections.singleton("FBgn002"));
        converter.resolverFactory = resolverFactory;
    }

    public void testProcess() throws Exception {

        String input = "pathway\tgene" + ENDL + "pathway1\tFBgn001" + ENDL;
        converter.setReactomeOrganisms("7227");
        converter.setCurrentFile(new File("7227.tsv"));
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "reactome-tgt-items.xml");

        Set expected = readItemSet("ReactomeConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());
    }
}
