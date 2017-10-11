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

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;

import org.intermine.bio.dataconversion.IdResolverService;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

public class AffyProbesConverterTest extends ItemsTestCase
{
    @SuppressWarnings("unused")
    private String ENDL = System.getProperty("line.separator");
    Model model = Model.getInstanceByName("genomic");
    AffyProbesConverter converter;
    MockItemWriter itemWriter;

    public AffyProbesConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new AffyProbesConverter(itemWriter, model);
        converter.rslv = IdResolverService.getMockIdResolver("Gene");
        converter.rslv.addResolverEntry("7227", "FBgn001", Collections.singleton("FBgn00158291"));
        converter.rslv.addResolverEntry("7227", "FBgn002", Collections.singleton("FBgn0033159"));
        converter.rslv.addResolverEntry("7227", "FBgn003", Collections.singleton("FBgn0035636"));
        converter.rslv.addResolverEntry("7227", "FBgn004", Collections.singleton("FBgn0050389"));
        converter.rslv.addResolverEntry("7227", "FBgn005", Collections.singleton("FBgn0046113"));
        converter.rslv.addResolverEntry("7227", "FBgn006", Collections.singleton("FBgn0043854"));
        converter.rslv.addResolverEntry("7227", "FBgn007", Collections.singleton("FBgn0037786"));
        converter.rslv.addResolverEntry("7227", "FBgn008", Collections.singleton("FBgn0011829"));
    }

    public void testProcess() throws Exception {

        Reader reader = new InputStreamReader(getClass().getClassLoader()
                                              .getResourceAsStream("AffyProbeConverterTest_src.txt"));
        converter.process(reader);
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "affy-probes-tgt-items.xml");

        assertEquals(readItemSet("AffyProbeConverterTest_tgt.xml"), itemWriter.getItems());
    }

}
