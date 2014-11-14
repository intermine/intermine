package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;

public class PsiComplexesConverterTest extends ItemsTestCase
{

    PsiComplexesConverter converter;
    MockItemWriter itemWriter;

    public PsiComplexesConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        itemWriter = new MockItemWriter(new HashMap<String, Item>());
        converter = new PsiComplexesConverter(itemWriter,  Model.getInstanceByName("genomic"));
        converter.setIntactOrganisms("4932");
        super.setUp();
    }

    public void testProcess() throws Exception {

        Reader reader = new InputStreamReader(getClass().getClassLoader()
                                            .getResourceAsStream("PsiComplexesConverterTest_src.xml"));
        converter.process(reader);
        converter.close();

        // uncomment to write out a new target items file
        writeItemsFile(itemWriter.getItems(), "psi-tgt-items.xml");

        Set<org.intermine.xml.full.Item> expected = readItemSet("PsiComplexesConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());
    }


}
