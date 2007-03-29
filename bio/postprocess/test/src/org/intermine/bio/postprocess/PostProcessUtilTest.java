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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Transcript;
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
        Assert.assertEquals(gene.getId(), clonedGene.getId());
        compareGenes(gene, clonedGene);
    }

    public void testCopyInterMineObject() throws Exception {
        Gene gene = createExampleGene();
        Gene copiedGene = (Gene) PostProcessUtil.copyInterMineObject(gene);
        Assert.assertNull(copiedGene.getId());
        compareGenes(gene, copiedGene);
    }


    private Gene createExampleGene() {
        Gene gene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        gene.setId(new Integer(101));
        Transcript tran1 = (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        tran1.setId(new Integer(102));
        Transcript tran2 = (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        tran2.setId(new Integer(103));
        Chromosome chr = (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        chr.setId(new Integer(104));

        gene.setIdentifier("gene1");
        gene.setChromosome(chr);
        Set transcripts = new HashSet(Arrays.asList(new Object[] {tran1, tran2}));
        gene.setTranscripts(transcripts);
        return gene;
    }

    private void compareGenes(Gene gene, Gene copiedGene) {
        Assert.assertEquals(gene.getIdentifier(), copiedGene.getIdentifier());
        Assert.assertEquals(gene.getChromosome(), copiedGene.getChromosome());
        Assert.assertEquals(gene.getTranscripts(), copiedGene.getTranscripts());
    }
}
