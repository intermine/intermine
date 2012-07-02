package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
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
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

/**
 * Test class for EnsemblComparaConverter
 * @author InterMine
 */
public class EnsemblComparaConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    EnsemblComparaConverter converter;
    MockItemWriter itemWriter;
    private static final String TEST_FILE = "7227_9606";

    /**
     * Constructor
     * @param arg argument
     */
    public EnsemblComparaConverterTest(String arg) {
        super(arg);
    }

    /**
     * @throws Exception e
     */
    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new EnsemblComparaConverter(itemWriter, model);
        MockIdResolverFactory flyResolverFactory = new MockIdResolverFactory("Gene");
        flyResolverFactory.addResolverEntry("7227", "FBgn0013672",
                Collections.singleton("FBgn0013672"));
        flyResolverFactory.addResolverEntry("7227", "FBgn0010412",
                Collections.singleton("FBgn0010412"));
        converter.flyResolverFactory = flyResolverFactory;
        DoNothingIdResolverFactory humanResolverFactory = new DoNothingIdResolverFactory("Gene");
        converter.humanResolverFactory = humanResolverFactory;
    }

    /**
     * @throws Exception e
     */
    public void testProcess() throws Exception {

        ClassLoader loader = getClass().getClassLoader();
        String input = IOUtils.toString(loader.getResourceAsStream(TEST_FILE));

        File currentFile = new File(getClass().getClassLoader().getResource(TEST_FILE).toURI());
        converter.setCurrentFile(currentFile);
        converter.setEnsemblcomparaOrganisms("10116 6239 7227");
        converter.setEnsemblcomparaHomologues("9606");
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "ensembl-compara-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected =
            readItemSet("EnsemblComparaConverterTest_tgt.xml");
        assertEquals(expected, itemWriter.getItems());
    }
}
