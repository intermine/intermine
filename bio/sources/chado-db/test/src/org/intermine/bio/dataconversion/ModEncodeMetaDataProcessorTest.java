package org.intermine.bio.dataconversion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

public class ModEncodeMetaDataProcessorTest extends TestCase {
    ModEncodeMetaDataProcessor processor;
    
    public void setUp() throws Exception {
        MockItemWriter itemWriter =
            new MockItemWriter(new HashMap<String, org.intermine.model.fulldata.Item>());
        ChadoDBConverter converter =
            new ChadoDBConverter(null, Model.getInstanceByName("genomic"), itemWriter);
        processor = new ModEncodeMetaDataProcessor(converter);
        
    }
    
    public void testCorrectOfficialName() throws Exception {

        // actual examples from metadata
        assertEquals("Adult Female", processor.correctOfficialName("Adult_Female", "developmental stage"));
        assertEquals("Embryo 0-4 h", processor.correctOfficialName("E0-4h", "developmental stage"));
        assertEquals("Embryo 0-12 h", processor.correctOfficialName("Embryo 0-12h", "developmental stage"));
        assertEquals("Embryo 0-4 h", processor.correctOfficialName("E0-4", "developmental stage"));
        assertEquals("Embryo 2-4 h", processor.correctOfficialName("Embryo 2-4 hr", "developmental stage"));
        assertEquals("L1 stage larvae", processor.correctOfficialName("DevStage: L1 stage larvae", "developmental stage"));
        assertEquals("L1 stage larvae", processor.correctOfficialName("L1", "developmental stage"));
        assertEquals("mid-L1 stage larvae", processor.correctOfficialName("mid-L1", "developmental stage"));
        assertEquals("White prepupae (WPP) + 12 h", processor.correctOfficialName("WPP + 12 h", "developmental stage"));
    }
    
    
    public void testInferExperimentType() throws Exception {
        String[] test = new String[] {"chromatin_immunoprecipitation", "dummy"};
        assertEquals("ChIP-seq", processor.inferExperimentType(makeProtocols(test), "anything"));
        
        test = new String[] {"chromatin_immunoprecipitation", "hybridization"};
        assertEquals("ChIP-chip", processor.inferExperimentType(makeProtocols(test), "anything"));
    
        test = new String[] {"nucleic_acid_extraction", "sequencing_protocol", "reverse_transcription"};
        assertEquals("RTPCR", processor.inferExperimentType(makeProtocols(test), "anything"));
        
        test = new String[] {"RNA extraction", "sequencing_protocol"};
        assertEquals("RNA-seq", processor.inferExperimentType(makeProtocols(test), "anything"));
        
        test = new String[] {"RNA extraction", "sequencing"};
        assertEquals("RNA-seq", processor.inferExperimentType(makeProtocols(test), "anything"));
        
        test = new String[] {"reverse_transcription", "PCR"};
        assertEquals("RTPCR", processor.inferExperimentType(makeProtocols(test), "anything"));
        
        test = new String[] {"reverse_transcription", "PCR_amplification", "RACE"};
        assertEquals("RACE", processor.inferExperimentType(makeProtocols(test), "anything"));
        
        test = new String[] {"hybridization"};
        assertEquals("RNA tiling array", processor.inferExperimentType(makeProtocols(test), "Celniker"));

        test = new String[] {"hybridization"};
        assertEquals("Chromatin-chip", processor.inferExperimentType(makeProtocols(test), "Henikoff"));

        test = new String[] {"hybridization"};
        assertEquals("Tiling array", processor.inferExperimentType(makeProtocols(test), "anything"));
        
        test = new String[] {"annotation"};
        assertEquals("Computational annotation", processor.inferExperimentType(makeProtocols(test), "anything"));
    
        test = new String[] {"grow"};
        assertEquals("RNA sample creation", processor.inferExperimentType(makeProtocols(test), "anything"));
    }
    
    private Set<String> makeProtocols(String[] types) {
        Set<String> protocolTypes = new HashSet<String>();
        for (String type : types) {
            protocolTypes.add(type);
        }
        return protocolTypes;
    }
}
