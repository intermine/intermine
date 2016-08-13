package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.intermine.bio.dataconversion.IdResolverService;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

import java.io.File;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;

/**
 * Fly-FISH converter functional test.
 * @author Kim Rutherford
 */
public class FlyFishConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    FlyFishConverter converter;
    MockItemWriter itemWriter;

    public FlyFishConverterTest(String arg) throws Exception {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new FlyFishConverter(itemWriter, model);
        converter.rslv = IdResolverService.getMockIdResolver("Gene");
        converter.rslv.addResolverEntry("7227", "FBgn0037874", Collections.singleton("CG4800"));
        converter.rslv.addResolverEntry("7227", "FBgn0020415", Collections.singleton("CG4475"));
        converter.rslv.addResolverEntry("7227", "FBgn0039830", Collections.singleton("CG1746"));
        converter.rslv.addResolverEntry("7227", "FBgn0019830", Collections.singleton("CG3057"));
    }

    public void testConstruct() throws Exception {
        assertNotNull(converter.orgDrosophila);
    }

    public void testProcess() throws Exception {

        ClassLoader loader = getClass().getClassLoader();
        String input = IOUtils.toString(loader.getResourceAsStream("test-matrix"));

        converter.setCurrentFile(new File("test-matrix"));
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to create a new target items files
        //writeItemsFile(itemWriter.getItems(), "flyfish_tgt.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("FlyFishTestItems.xml");
        assertEquals(expected, itemWriter.getItems());
    }
}
