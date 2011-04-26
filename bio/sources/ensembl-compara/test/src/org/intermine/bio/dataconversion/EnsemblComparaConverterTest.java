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

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class EnsemblComparaConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    EnsemblComparaConverter converter;
    MockItemWriter itemWriter;

    public EnsemblComparaConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap());
        converter = new EnsemblComparaConverter(itemWriter, model);
        MockIdResolverFactory resolverFactory = new MockIdResolverFactory("Gene");
        resolverFactory.addResolverEntry("7227", "FBgn001", Collections.singleton("FBgn001"));
        resolverFactory.addResolverEntry("7227", "FBgn003", Collections.singleton("FBgn002"));
        converter.resolver = resolverFactory.getIdResolver(false);
    }

    public void testProcess() throws Exception {

        Reader reader = new InputStreamReader(getClass().getClassLoader()
                                            .getResourceAsStream("7227_9606"));
        converter.setEnsemblComparaOrganisms("10116 6239 7227");
        converter.setEnsemblComparaHomologues("9606");
        System.out.println(reader);
        converter.process(reader);
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "ensembl-compara-tgt-items.xml");

        Set expected = readItemSet("EnsemblComparaConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());
    }
}
