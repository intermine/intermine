package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2010 FlyMine
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

import org.apache.commons.io.IOUtils;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.dataconversion.MockItemsTestCase;
import org.intermine.metadata.Model;


public class BioPAXConverterTest extends MockItemsTestCase
{
    private BioPAXConverter converter;
    private MockItemWriter itemWriter;
    //private static final String TEST_FILE = "Bos taurus.owl";
    private static final String TEST_FILE = "Drosophila melanogaster.owl";
    public BioPAXConverterTest(String arg) {
        super(arg);
    }

    public void testProcess() throws Exception {

        itemWriter = new MockItemWriter(new HashMap());
        converter = new BioPAXConverter(itemWriter, Model.getInstanceByName("genomic"));
        MockIdResolverFactory resolverFactory = new MockIdResolverFactory("Gene");
        resolverFactory.addResolverEntry("7227", "FBgn0000001", Collections.singleton("CG10863"));
        resolverFactory.addResolverEntry("7227", "FBgn0000002", Collections.singleton("CG2767"));
        converter.resolverFactory = resolverFactory;

        ClassLoader loader = getClass().getClassLoader();
        String input = IOUtils.toString(loader.getResourceAsStream(TEST_FILE));

        File currentFile = new File(getClass().getClassLoader().getResource(TEST_FILE).toURI());
        converter.setBiopaxDatasourcename("Reactome");
        converter.setBiopaxDatasetname("Reactome data set");
        converter.setCurrentFile(currentFile);
        //converter.setBiopaxOrganisms("9913");
        converter.setBiopaxOrganisms("7227");
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "BioPAX-tgt-items.xml");

        Set expected = readItemSet("BioPAXConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());
    }
}
