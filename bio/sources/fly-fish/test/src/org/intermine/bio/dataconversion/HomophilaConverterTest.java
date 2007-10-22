package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
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
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

/**
 * Homophila converter functional test.
 * @author Thomas Riley
 */
public class HomophilaConverterTest extends ItemsTestCase
{
    Model model = Model.getInstanceByName("genomic");
    
    public HomophilaConverterTest(String arg) {
        super(arg);
    }

    public void testConstruct() throws Exception {
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        HomophilaConverter converter = new HomophilaConverter(itemWriter, model);
        assertNotNull(converter.orgHuman);
        assertNotNull(converter.orgDrosophila);
    }
    
    public void testProcess() throws Exception {
        File diseases = File.createTempFile("diseases", "");
        FileOutputStream out = new FileOutputStream(diseases);
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("test/HomophilaTestDiseaseInput"), out);
        out.close();
        
        File proteinGene = File.createTempFile("diseases", "");
        out = new FileOutputStream(proteinGene);
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("test/HomophilaProteinGeneInput"), out);
        out.close();
        
        String input = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("test/HomophilaConverterTestInput"));
        
        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        HomophilaConverter converter = new HomophilaConverter(itemWriter, model);
        converter.setDiseasefile(diseases);
        converter.setProteingenefile(proteinGene);
        converter.process(new StringReader(input));
        converter.close();
 
        // uncomment to create a new target items files
        //writeItemsFile(itemWriter.getItems(), "homophila-tgt-items.xml");
        
        Set expected = readItemSet("test/HomophilaConverterTest.xml");
        assertEquals(expected, itemWriter.getItems());
    }
}
