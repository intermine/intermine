package org.flymine.gbrowse;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Collections;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.List;

import java.io.*;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import org.intermine.objectstore.*;
import org.intermine.objectstore.query.*;
import org.intermine.dataloader.IntegrationWriterFactory;
import org.intermine.dataloader.XmlDataLoader;
import org.intermine.dataloader.IntegrationWriter;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.datatracking.Source;
import org.intermine.util.DynamicUtil;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.Item;

import org.flymine.postprocess.*;
import org.flymine.model.genomic.*;
import org.apache.log4j.Logger;

/**
 * Tests for WriteGFFTask.
 */

public class WriteGFFTaskTest extends TestCase
{
    private ObjectStoreWriter osw;
    private Model model;
    private Organism storedOrganism = null;
    private Sequence storedSequence = null;
    private Chromosome storedChromosome = null;
    private Gene storedGene = null;
    private Transcript storedTranscript1 = null;
    private Transcript storedTranscript2 = null;
    private Exon storedExon1 = null;
    private Exon storedExon2 = null;
    private Exon storedExon3 = null;
    private Location storedExon1Location = null;
    private Location storedExon2Location = null;
    private Location storedExon3Location = null;
    private Location storedTranscript1Location = null;
    private Location storedTranscript2Location = null;
    private Location storedGeneLocation = null;

    private static final Logger LOG = Logger.getLogger(WriteGFFTaskTest.class);

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.genomic-test");
        osw.getObjectStore().flushObjectById();
        model = Model.getInstanceByName("genomic");
        createData();
    }

    public void tearDown() throws Exception {
        LOG.info("in tear down");
        if (osw.isInTransaction()) {
            osw.abortTransaction();
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ObjectStore os = osw.getObjectStore();
        SingletonResults res = new SingletonResults(q, osw.getObjectStore(), osw.getObjectStore()
                                                    .getSequence());
        LOG.info("created results");
        Iterator resIter = res.iterator();
        osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            LOG.info("deleting: " +o.getId());
            osw.delete(o);
        }
        osw.commitTransaction();
        LOG.info("committed transaction");
        osw.close();
        LOG.info("closed objectstore");
    }

    public void testWriteGFF() throws Exception {
        TransferSequences ts = new TransferSequences(osw);
        ts.transferToLocatedSequenceFeatures();
        WriteGFFTask task = new WriteGFFTask();
        task.writeGFF(osw.getObjectStore(), new File("build/gbrowse/data"));
    }

    private void createData() throws Exception {
        osw.flushObjectById();

        storedOrganism = (Organism) DynamicUtil.createObject(Collections.singleton(Organism.class));
        storedOrganism.setGenus("Drosophila");
        storedOrganism.setSpecies("melanogaster");

        String residues =
            "ggcgaggcggggaaagcactgcgcgctgacggtggtgctgattgtattttttcagcgtct" +
            "cagcgcgtcgtgacggcacttagtctgcccgttgaggcgttgtgtgtctgcggggtgttt" +
            "tgtgcggtggtgagcgtgtgaggggggatgacggggtgtaaaaaagccgcccgcaggcgg" +
            "cgatgttcagtcgttgtcagtgtccagtgagtagtttttaaagcggatgacctcctgacc" +
            "gagccagccgtttatctcgcggatcctgtcctgtaacgggataagctcattgcggacaaa" +
            "gacctttgccactttctcaatatcacccagcgacccgacgttctccggcttgccacccat" +
            "caactgaaaggggatgcggtgcgcgtccagcaggtcagcggcgctggcttttttgatatt" +
            "aaaaaaatcgtccttcgtcgccacttcactgagggggataattttaatgccgtcggcttt" +
            "cccctgtggggcatagagaaacaggtttttaaagttgttgcggcctttcgacttgaccat" +
            "gttttcgcgaagcatttcgatatcgttgcgatcctgcacggcatcggtgacatacatgat" +
            "gtatccggcatgtgcgccattttcgtaatacttgcggcggaacaacgtggccgactcatt" +
            "cagccaggcagagttaagggcgctgagatattccggcaggccgtacagctcctgattaat" +
            "atccggctccagcaggtgaaacacggagccgggcgcgaaggctgtcggctcgttgaagga" +
            "cggcacccaccagtaaacatcctcttccacgccacggcgggtatattttgccggtgaggt" +
            "ttccagtctgatgaccttaccggtggtgctgtaacgcttttccagaaacgcattaccgaa" +
            "caccagaaaatccagcacaaagcggctgaaatcctgctgggaaagccatggatgcgggat" +
            "aaatgtcgaggccagaatattgcgtttgacgtaaatcggcgagctgtgatgcacggcagc" +
            "ccgcaggctttttgccagaccggtaaagctgaccggtggctcataccatctgccgttact" +
            "gatgcactcgacgtaatccagaatgtcacggcggtcgagtaccggcaccggctcaccaaa" +
            "ggtgaatgcctccattttcgggccgctggcggtcattgtttttgccgcaggttgcggtgt" +
            "tttcccttttttcttgctcatcagtaaaactccagaatggtggatgtcagcggggtgctg" +
            "ataccggcggtgagtggctcatttaacagggcgtgcatggtcgcccaggcgaggtcggcg" +
            "tggctggcttcctcgctgcggctggcctcataggtggcgctgcgtccgctgctggtcatg" +
            "gtcttgcggatagccataaacgagctggtgatgtcggtggcgctgacgtcgtattccaga" +
            "cagccacggcggataacgtcttttgccttgagcaccattgcggttttcatttccggcgtg";

        storedSequence = (Sequence) DynamicUtil.createObject(Collections.singleton(Sequence.class));
        storedSequence.setResidues(residues);

        storedChromosome =
            (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        storedChromosome.setIdentifier("1");
        storedChromosome.setLength(new Integer(residues.length()));
        storedChromosome.setOrganism(storedOrganism);
        storedChromosome.setSequence(storedSequence);

        storedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        storedGene.setIdentifier("gene1");

        storedTranscript1 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        storedTranscript1.setIdentifier("trans1");
        storedTranscript1.setGene(storedGene);

        storedTranscript2 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        storedTranscript2.setIdentifier("trans2");
        storedTranscript2.setGene(storedGene);

        storedExon1 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        storedExon1.setIdentifier("exon1");
        storedExon1.setTranscripts(Arrays.asList(new Object[] {storedTranscript1}));
        storedExon2 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        storedExon2.setIdentifier("exon2");
        storedExon2.setTranscripts(Arrays.asList(new Object[] {storedTranscript1,
                                                               storedTranscript2}));
        storedExon3 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        storedExon3.setIdentifier("exon3");
        storedExon3.setTranscripts(Arrays.asList(new Object[] {storedTranscript2}));

        storedTranscript1Location =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedTranscript1Location.setObject(storedChromosome);
        storedTranscript1Location.setSubject(storedTranscript1);
        storedTranscript1Location.setStart(new Integer(201));
        storedTranscript1Location.setEnd(new Integer(400));
        storedTranscript1Location.setStrand(new Integer(1));

        storedTranscript2Location =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedTranscript2Location.setObject(storedChromosome);
        storedTranscript2Location.setSubject(storedTranscript2);
        storedTranscript2Location.setStart(new Integer(351));
        storedTranscript2Location.setEnd(new Integer(620));
        storedTranscript2Location.setStrand(new Integer(1));

        storedGeneLocation =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedGeneLocation.setObject(storedChromosome);
        storedGeneLocation.setSubject(storedGene);
        storedGeneLocation.setStart(new Integer(201));
        storedGeneLocation.setEnd(new Integer(620));
        storedGeneLocation.setStrand(new Integer(1));

        storedExon1Location =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedExon1Location.setObject(storedChromosome);
        storedExon1Location.setSubject(storedExon1);
        storedExon1Location.setStart(new Integer(201));
        storedExon1Location.setEnd(new Integer(300));
        storedExon1Location.setStrand(new Integer(1));

        storedExon2Location =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedExon2Location.setObject(storedChromosome);
        storedExon2Location.setSubject(storedExon2);
        storedExon2Location.setStart(new Integer(351));
        storedExon2Location.setEnd(new Integer(400));
        storedExon2Location.setStrand(new Integer(1));

        storedExon3Location =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedExon3Location.setObject(storedChromosome);
        storedExon3Location.setSubject(storedExon3);
        storedExon3Location.setStart(new Integer(591));
        storedExon3Location.setEnd(new Integer(620));
        storedExon3Location.setStrand(new Integer(1));

        Set toStore = new HashSet(Arrays.asList(new Object[] {
                                                    storedOrganism, storedChromosome,
                                                    storedGene, storedTranscript1,
                                                    storedTranscript2,
                                                    storedExon1, storedExon2,
                                                    storedExon3,
                                                    storedExon1Location, storedExon2Location,
                                                    storedExon3Location,
                                                    storedTranscript1Location,
                                                    storedTranscript2Location,
                                                    storedGeneLocation, storedSequence
                                                }));
        Iterator i = toStore.iterator();
        osw.beginTransaction();
        LOG.info("begun transaction in createData()");
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }

        osw.commitTransaction();
        LOG.info("committed transaction in createData()");
    }

    private Item toItem(InterMineObject o) {
        if (o.getId() == null) {
            o.setId(new Integer(0));
        }
        Item item = FullRenderer.toItem(o, model);
        return item;
    }
}
