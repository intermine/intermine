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
import java.io.FileOutputStream;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class TreefamConverterTest extends ItemsTestCase
{
    private TreefamConverter converter;
    private MockItemWriter itemWriter;

    public TreefamConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {

        itemWriter = new MockItemWriter(new HashMap());
        converter = new TreefamConverter(itemWriter, Model.getInstanceByName("genomic"));
        MockIdResolverFactory resolverFactory = new MockIdResolverFactory("Gene");
        resolverFactory.addResolverEntry("7227", "FBgn001", Collections.singleton("CG1111"));
        resolverFactory.addResolverEntry("7227", "FBgn002", Collections.singleton("CG2222"));
        converter.flyResolverFactory = resolverFactory;


        super.setUp();
    }

    public void testProcess() throws Exception {

        File genes = File.createTempFile("genes", "");
        FileOutputStream out = new FileOutputStream(genes);
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("genes.txt.table"), out);
        out.close();

        ClassLoader loader = getClass().getClassLoader();
        String input = IOUtils.toString(loader.getResourceAsStream("ortholog.txt.table"));

        converter.setTreefamOrganisms("7227");
        converter.setTreefamHomologues("9606");
        converter.setGeneFile(genes);
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "treefam-tgt-items.xml");

        Set expected = readItemSet("TreefamConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());
    }
}
