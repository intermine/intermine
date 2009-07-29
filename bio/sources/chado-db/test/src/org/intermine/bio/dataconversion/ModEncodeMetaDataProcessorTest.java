package org.intermine.bio.dataconversion;

import java.util.HashMap;

import junit.framework.TestCase;

import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

public class ModEncodeMetaDataProcessorTest extends TestCase {

    
    public void testCorrectOfficialName() throws Exception {
        MockItemWriter itemWriter =
            new MockItemWriter(new HashMap<String, org.intermine.model.fulldata.Item>());
        ChadoDBConverter converter =
            new ChadoDBConverter(null, Model.getInstanceByName("genomic"), itemWriter);
        ModEncodeMetaDataProcessor processor = new ModEncodeMetaDataProcessor(converter);
        
        // actual examples from metadata
        assertEquals("Adult Female", processor.correctOfficialName("Adult_Female", "DevelopmentalStage"));
        assertEquals("Embryo 0-4 h", processor.correctOfficialName("E0-4h", "DevelopmentalStage"));
        assertEquals("Embryo 0-12 h", processor.correctOfficialName("Embryo 0-12h", "DevelopmentalStage"));
        assertEquals("Embryo 0-4 h", processor.correctOfficialName("E0-4", "DevelopmentalStage"));
        assertEquals("Embryo 2-4 h", processor.correctOfficialName("Embryo 2-4 hr", "DevelopmentalStage"));
        assertEquals("L1 stage larvae", processor.correctOfficialName("DevStage: L1 stage larvae", "DevelopmentalStage"));
        assertEquals("L1 stage larvae", processor.correctOfficialName("L1", "DevelopmentalStage"));
    }
}
