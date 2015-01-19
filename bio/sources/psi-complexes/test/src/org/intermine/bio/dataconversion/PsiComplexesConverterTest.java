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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.model.fulldata.Item;
import org.intermine.xml.full.FullRenderer;

public class PsiComplexesConverterTest extends ItemsTestCase
{
    public PsiComplexesConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        PsiComplexesConverter converter = new PsiComplexesConverter(itemWriter,  Model.getInstanceByName("genomic"));
        converter.setPsiOrganisms("4932");
        //Reader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("PsiComplexesConverterTest_src.xml"));
        Reader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("aas_yeast.xml"));
        converter.process(reader);
        converter.close();

        // uncomment to write out a new target items file
        writeItemsFile(itemWriter.getItems(), "psi-complexes-tgt-items.xml");

        Set expected = readItemSet("PsiComplexesConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());

    }


}
