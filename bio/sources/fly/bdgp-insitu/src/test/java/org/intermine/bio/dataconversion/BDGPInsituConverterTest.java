package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashMap;

import org.intermine.bio.dataconversion.IdResolverService;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;
import org.intermine.objectstore.ObjectStoreException;

import java.io.StringReader;

import org.apache.commons.io.IOUtils;

/**
 * BDGP converter functional test.
 * @author Julie Sullivan
 */
public class BDGPInsituConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    BDGPInsituConverter converter;
    MockItemWriter itemWriter;

    public BDGPInsituConverterTest(String arg) throws ObjectStoreException {
        super(arg);
    }

    public void setUp() throws Exception {
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new BDGPInsituConverter(itemWriter, model);
        converter.rslv = IdResolverService.getMockIdResolver("Gene");
        converter.rslv.addResolverEntry("7227", "FBgn001", Collections.singleton("CG10002"));
        converter.rslv.addResolverEntry("7227", "FBgn002", Collections.singleton("CG100022"));
    }

    public void testConstruct() throws Exception {
        assertNotNull(converter.orgDrosophila);
    }

    public void testProcess() throws Exception {

        String input = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("bdgp-insitu-mysql"));

        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "bdgp_tgt.xml");

        assertEquals(readItemSet("BDGPInsituConverterTest_tgt.xml"), itemWriter.getItems());

    }
}
