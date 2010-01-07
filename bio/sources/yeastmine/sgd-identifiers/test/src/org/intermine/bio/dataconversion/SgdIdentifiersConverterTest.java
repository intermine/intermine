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

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Set;

import org.intermine.dataconversion.MockItemWriter;
import org.intermine.dataconversion.MockItemsTestCase;
import org.intermine.metadata.Model;

public class SgdIdentifiersConverterTest extends MockItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    SgdIdentifiersConverter converter;
    MockItemWriter itemWriter;

    public SgdIdentifiersConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        itemWriter = new MockItemWriter(new HashMap());
        converter = new SgdIdentifiersConverter(itemWriter, model);
    }

    public void testProcess() throws Exception {

        Reader reader = new InputStreamReader(getClass().getClassLoader()
                                              .getResourceAsStream("SgdConverterTest_src.data"));
        converter.process(reader);
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "sgd-tgt-items.xml");

        Set expected = readItemSet("SgdConverterTest_tgt.xml");

        assertEquals(expected, itemWriter.getItems());
    }
}

