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
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class UniprotConverterTest extends ItemsTestCase
{
    private UniprotConverter converter;
    private MockItemWriter itemWriter;

    public UniprotConverterTest(String arg) {
        super(arg);

//        resolverFactory = new FlyBaseIdResolverFactory();
//        converter.resolverFactory = resolverFactory;
    }

    public void setUp() throws Exception {
        itemWriter = new MockItemWriter(new HashMap());
        converter = new UniprotConverter(itemWriter, Model.getInstanceByName("genomic"));
        MockIdResolverFactory resolverFactory = new MockIdResolverFactory("Gene");
        resolverFactory.addResolverEntry("7227", "FBgn0037874", Collections.singleton("CG4800"));
        resolverFactory.addResolverEntry("7227", "FBgn0020415", Collections.singleton("CG4475"));
        resolverFactory.addResolverEntry("7227", "FBgn0039830", Collections.singleton("CG1746"));
        resolverFactory.addResolverEntry("7227", "FBgn0019830", Collections.singleton("CG3057"));
        converter.resolverFactory = resolverFactory;
        super.setUp();
    }

    public void testProcess() throws Exception {

        Reader reader = new InputStreamReader(getClass().getClassLoader()
                                              .getResourceAsStream("UniprotConverterTest_src.xml"));
        converter.setCreateinterpro("true");
        converter.process(reader);
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "uniprot-tgt-items.xml");

        Set expected = readItemSet("UniprotConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());
    }
}
