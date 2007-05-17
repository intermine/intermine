package org.intermine.bio.gbrowse;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.Exon;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Location;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.Sequence;
import org.flymine.model.genomic.Transcript;

import org.intermine.bio.gbrowse.WriteGFFTask;
import org.intermine.bio.postprocess.CalculateLocations;
import org.intermine.bio.postprocess.CreateReferences;
import org.intermine.bio.postprocess.TransferSequences;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;

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
    private ItemFactory itemFactory;

    private static final Logger LOG = Logger.getLogger(WriteGFFTaskTest.class);

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();
        model = Model.getInstanceByName("genomic");
        itemFactory = new ItemFactory(model);
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
        SingletonResults res = osw.getObjectStore().executeSingleton(q);
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
        CalculateLocations cl = new CalculateLocations(osw);
        cl.createLocations();
        cl.setChromosomeLocationsAndLengths();
        CreateReferences cr = new CreateReferences(osw);
        cr.insertReferences();
        TransferSequences ts = new TransferSequences(osw);
        ts.transferToLocatedSequenceFeatures();
        WriteGFFTask task = new WriteGFFTask();
        task.writeGFF(osw.getObjectStore());
    }

    public void testStringifyAttributes() throws Exception {
        Map attMap = new LinkedHashMap();

        attMap.put("empty_key", new ArrayList());

        List oneValue = new ArrayList();
        oneValue.add("v1");
        attMap.put("k1", oneValue);
        
        List twoValues = new ArrayList(); 
        twoValues.add("v2");
        twoValues.add("v3");
        attMap.put("k2", twoValues);

        List threeValues = new ArrayList();
        threeValues.add("v4");
        threeValues.add("");
        threeValues.add("v5");
        attMap.put("k3", threeValues);

        String stringifiedAttributes = WriteGFFTask.stringifyAttributes(attMap);
        String expected =
            "empty_key; k1 \"v1\"; k2 \"v2\"; k2 \"v3\"; k3 \"v4\"; k3 \"\"; k3 \"v5\"";

        assertEquals(expected, stringifiedAttributes);
    }

    private void createData() throws Exception {
        osw.flushObjectById();

        storedOrganism = (Organism) DynamicUtil.createObject(Collections.singleton(Organism.class));
        storedOrganism.setGenus("Drosophila");
        storedOrganism.setSpecies("melanogaster");
        storedOrganism.setAbbreviation("DM");

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
        storedGene.setLength(new Integer(420));
        
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
        storedExon1.setTranscripts(new HashSet(Arrays.asList(new Object[] {storedTranscript1})));
        storedExon2 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        storedExon2.setIdentifier("exon2");
        storedExon2.setTranscripts(new HashSet(Arrays.asList(new Object[] {storedTranscript1,
            storedTranscript2})));
        storedExon3 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        storedExon3.setIdentifier("exon3");
        storedExon3.setTranscripts(new HashSet(Arrays.asList(new Object[] {storedTranscript2})));

        storedTranscript1Location =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedTranscript1Location.setObject(storedChromosome);
        storedTranscript1Location.setSubject(storedTranscript1);
        storedTranscript1Location.setStart(new Integer(201));
        storedTranscript1Location.setEnd(new Integer(400));
        storedTranscript1Location.setStrand("1");

        storedTranscript2Location =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedTranscript2Location.setObject(storedChromosome);
        storedTranscript2Location.setSubject(storedTranscript2);
        storedTranscript2Location.setStart(new Integer(351));
        storedTranscript2Location.setEnd(new Integer(620));
        storedTranscript2Location.setStrand("1");

        storedGeneLocation =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedGeneLocation.setObject(storedChromosome);
        storedGeneLocation.setSubject(storedGene);
        storedGeneLocation.setStart(new Integer(201));
        storedGeneLocation.setEnd(new Integer(620));
        storedGeneLocation.setStrand("1");

        storedExon1Location =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedExon1Location.setObject(storedChromosome);
        storedExon1Location.setSubject(storedExon1);
        storedExon1Location.setStart(new Integer(201));
        storedExon1Location.setEnd(new Integer(300));
        storedExon1Location.setStrand("1");

        storedExon2Location =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedExon2Location.setObject(storedChromosome);
        storedExon2Location.setSubject(storedExon2);
        storedExon2Location.setStart(new Integer(351));
        storedExon2Location.setEnd(new Integer(400));
        storedExon2Location.setStrand("1");

        storedExon3Location =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedExon3Location.setObject(storedChromosome);
        storedExon3Location.setSubject(storedExon3);
        storedExon3Location.setStart(new Integer(591));
        storedExon3Location.setEnd(new Integer(620));
        storedExon3Location.setStrand("1");

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

    private Item makeItem(InterMineObject o) {
        if (o.getId() == null) {
            o.setId(new Integer(0));
        }
        Item item = itemFactory.makeItem(o);
        return item;
    }
}
