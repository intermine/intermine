package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Transcript;
import org.intermine.util.DynamicUtil;

/**
 * Tests for the PostProcessUtil class.
 */
public class PostProcessUtilTest extends TestCase {

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testCloneInterMineObject() throws Exception {
        Gene gene = createExampleGene();
        Gene clonedGene = (Gene) PostProcessUtil.cloneInterMineObject(gene);
        assertEquals(gene.getId(), clonedGene.getId());
        compareGenes(gene, clonedGene);
    }

    public void testCopyInterMineObject() throws Exception {
        Gene gene = createExampleGene();
        Gene copiedGene = (Gene) PostProcessUtil.copyInterMineObject(gene);
        assertNull(copiedGene.getId());
        compareGenes(gene, copiedGene);
    }


    private Gene createExampleGene() {
        Gene gene = DynamicUtil.createObject(Gene.class);
        gene.setId(new Integer(101));
        Transcript tran1 = DynamicUtil.createObject(Transcript.class);
        tran1.setId(new Integer(102));
        Transcript tran2 = DynamicUtil.createObject(Transcript.class);
        tran2.setId(new Integer(103));
        Chromosome chr = DynamicUtil.createObject(Chromosome.class);
        chr.setId(new Integer(104));

        gene.setPrimaryIdentifier("gene1");
        gene.setChromosome(chr);
        Set<Transcript> transcripts =
                new HashSet<Transcript>(Arrays.asList(new Transcript[] {tran1, tran2}));
        gene.setTranscripts(transcripts);
        return gene;
    }

    private void compareGenes(Gene gene, Gene copiedGene) {
        assertEquals(gene.getSecondaryIdentifier(), copiedGene.getSecondaryIdentifier());
        assertEquals(gene.getChromosome(), copiedGene.getChromosome());
        assertEquals(gene.getTranscripts(), copiedGene.getTranscripts());
    }
}
