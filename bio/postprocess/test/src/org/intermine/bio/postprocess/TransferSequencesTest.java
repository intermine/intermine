package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.flymine.model.genomic.BioEntity;
import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.Contig;
import org.flymine.model.genomic.Exon;
import org.flymine.model.genomic.Location;
import org.flymine.model.genomic.RankedRelation;
import org.flymine.model.genomic.RepeatRegion;
import org.flymine.model.genomic.Sequence;
import org.flymine.model.genomic.Supercontig;
import org.flymine.model.genomic.Transcript;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;

/**
 * Tests for the TransferSequences class
 */
public class TransferSequencesTest extends TestCase
{
    private ObjectStoreWriter osw;
    private Chromosome storedChromosome;
    private Supercontig storedSupercontig;
    private Contig [] storedContigs;
    private Sequence [] storedContigSequences;
    private Exon [] storedExons;
    private Transcript [] storedTranscripts;
    private RepeatRegion [] storedRepeatRegions;

    private String expectedExonSequence0 =
        "tatgactgtaacgttaatagcaaagtgagtgttaataatgataaaatagcagcaaaatct" +
        "cttttccgagtaagacgttttccagtc";

    private String expectedExonSequence4 =
        "caagtagtaagaaagacggatataaaaaaatagcctacagcaccccggattcccatgttg" +
        "tctccaaccatagtactaacgaggccctcagacgcttaactgcagtgatcggacgggaac" +
        "tggtgttttcgcctaggtatggccgtagacat";

    private String expectedExonSequence1 =
        "caaaaatcttctagtttttttttagaaaggatacaccaagta";

    private String expectedExonSequence2 =
        "gacgattgtttactttacacgccggaaatgaaaaccgcaatggtgctcaaggcaaaagac" +
        "gttatccgccgtggctgtctggaatacgacgtcagcgccaccgacatcaccagctcgttt" +
        "atggctatccgcaagaccatgaccagcagcggacgcagcgccacctatgaggccagccgc" +
        "agcgaggaagccagccacgccgacctcgcctgggcgaccatgcacgccctgttaaatgag" +
        "ccactcaccgccggtatcagcaccccgctgacatccaccattctggagttttac";

    private String expectedExonSequence5 =
        "cagggcgtgcatggtcgcccaggcgaggtcggcgtggctggcttcctcgctgcggctggc" +
        "ctcataggtggcgctgcgtccgctgctggtcatggtcttgcggatagccataaacgagct" +
        "ggtgatgtcggtggcgctgacgtcgtattccagacagccacggcggataacgtcttttgc" +
        "cttgagcaccattgcggttttcatttccggcgtgtaaag";

    private String expectedExonSequence3 =
        "accgccagcggcccgaaaatggaggcattcacctttggtgagccggtgccggtactcgac" +
        "cgccgtgacattctggattacgtcgagtgcatcagtaacggcagatggtatgagccaccg" +
        "gtcagctttaccggtctggcaaaaagcctgcgggctgccgtgcatcacagctcgccgatt" +
        "tacgtcaaacgcaatattctggcctcgacatttatcccgcatccatggctttcccagcag" +
        "gatttcagccgctttgtgctggattttctggtgttcggtaatgcgtttctggaaaagcgt" +
        "tacagcaccaccggtaaggtcatcagactggaaacctcaccggcaaaatatacccgccgt" +
        "ggcgtggaagaggatgtttactggtgggtgccgtccttcaacgagccgacagccttcgcg" +
        "cccggctccgtgtttcacctgctggagccggatattaatcaggagctgtacggcctgccg" +
        "gaatatctcagcgcccttaactctgcctggc";

    private String expectedExonSequence6 =
        "aaggggatgcggtgcgcgtccagcaggtcagcggcgctggcttttttgatattaaaaaaa" +
        "tcgtccttcgtcgccacttcactgagggggataattttaatgccgtcggctttcccctgt" +
        "ggggca";

    private String expectedExonSequence7 =
        "cggggaaagcactgcgcgctgacggtggtgctgattgtattttttcagcgtctcagcgcg" +
        "tcgtgacggcacttagtctgcccgttgaggcgttgtgtgtctgcggggtgttttgtgcgg" +
        "tggtgagcgtg";
    
    private static final Logger LOG = Logger.getLogger(TransferSequencesTest.class);

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        createData();
    }

    public void tearDown() throws Exception {
        LOG.error("in tear down");
        if (osw.isInTransaction()) {
            osw.abortTransaction();
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        SingletonResults res = osw.getObjectStore().executeSingleton(q);
        LOG.error("created results");
        Iterator resIter = res.iterator();
        //osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            LOG.error("deleting: " + o.getId());
            osw.delete(o);
        }
        //osw.commitTransaction();
        LOG.error("committed transaction");
        osw.close();
        LOG.error("closed objectstore");
    }

    public void testChromosomeSequenceTransfer() throws Exception {
        CalculateLocations cl = new CalculateLocations(osw);
        cl.fixPartials();
        cl.createLocations();
        TransferSequences ts = new TransferSequences(osw);
        ts.transferToChromosome();
        checkChromosomeSequences();
    }

    public void testTransferToLocatedSequenceFeatures() throws Exception {
        CalculateLocations cl = new CalculateLocations(osw);
        cl.fixPartials();
        cl.createLocations();
        TransferSequences ts = new TransferSequences(osw);
        ts.transferToChromosome();
        ts.transferToLocatedSequenceFeatures();
        checkExonSequences();
    }

    public void testTranscriptSequence() throws Exception {
        CalculateLocations cl = new CalculateLocations(osw);
        cl.fixPartials();
        cl.createLocations();
        CreateReferences cr = new CreateReferences(osw);
        cr.insertReferences();
        cl.setChromosomeLocationsAndLengths();
        TransferSequences ts = new TransferSequences(osw);
        ts.transferToChromosome();
        ts.transferToLocatedSequenceFeatures();
        ts.transferToTranscripts();
        checkTranscriptSequences();
    }

    public void checkChromosomeSequences() throws Exception {
        osw.flushObjectById();

        ObjectStore os = osw.getObjectStore();

        Chromosome resChromosome = (Chromosome) os.getObjectById(storedChromosome.getId());

        String expectedChrSequence =
            "............................................................"+
            "............................................................"+
            "............................................................"+
            "............................................................"+
            "............................................................"+
            "............................................................"+
            "............................................................"+
            "............................................................"+
            "....................gataaagtcggatctcaaaggaataaaaaatagtccgttg"+
            "attaaatttaaaatacgctagtaattctctaaacttcgtctgcgcttttcaacaataatc"+
            "agttaaaccggcggttggtggagagcagttaatacttgacaagcatatcatttacatcgc"+
            "acctccttagtacgatttatttaacgataagtaacaccgtccacgtgctgaccagcctat"+
            "ttgaagaataacgatatcatagagcttactgtaggattaataaaaatatcatcaaaatgg"+
            "ggaaaaaattgaaagattatgagaagtatatgattattaaataaaataaaacttttaaga"+
            "attgatactgagttttgatctttaaatttagtggagatactcattaaacagccttatacc"+
            "taaatgaaaactgaaaggtttactgcgtttacctagcatgcttagcaatgaattaaagga"+
            "ttgctcaattacctagctttctgaatattacgctagctttctactcccttgccattgtaa"+
            "aatacttttctggtcatcccgctgcttttcaatgaaactgctattcattgacagcctctc"+
            "aacaaaagatgggtttctcaaattaaatgtacaaaaattatgcatattttatatttattt"+
            "gagcacaaaatcacattatatgaccatgccgtggatatacaacaaattttacaacaccag"+
            "gtatttctttacggcttaaggaagttttagttatgactgtaaattgtacagctgtgaata"+
            "tattacttaaccatagtataccctttaaaccaatgcaattacacttttgcagtagatata"+
            "tatccagattctaccacgctggctataatattctcctctgtagaataataatttgactgc"+
            "aacacctaccaacttctgactactgtttgggctttaatttcattgcctactacaatgcgt"+
            "cgttctgggagtaaagaatcaacaattgtaagtaatcgagttatgtttttagctgttaac"+
            "tatatataatttaataaatacattccgacgatactgcctctatggcttagtggtacagca"+
            "tcgcacttgtaatgcgaagatccttggttcgattccgagtggaggcatatacattatatt"+
            "atattctttttcatgcggaaaaaagatttcaaatttttgggtatgatattaatatgactg"+
            "taacgttaatagcaaagtgagtgttaataatgataaaatagcagcaaaatctcttttccg"+
            "agtaagacgttttccagtctaaatttggagtctgcagttgtttcgcaattcttaatgtat"+
            "ggttatactaaatacaaactttaaagctctgatttatgtttgcaataaactaaaataaaa"+
            "gcacaaaaacctttacccattaatttcaaacaacttataaactaccggtaaacttttttt"+
            "ctaacctttataatttataaactagaatgtttaatgtctacggccatacctaggcgaaaa"+
            "caccagttcccgtccgatcactgcagttaagcgtctgagggcctcgttagtactatggtt"+
            "ggagacaacatgggaatccggggtgctgtaggctatttttttatatccgtctttcttact"+
            "acttgcctaacaagtcatgatgtactctcaaaatatgtttgcatgccttgtaatattggt"+
            "tatggatagctccttctggacttgatcttttgtagccaagaacaatgggtatagactctg"+
            "accttgtgatgttgtagccacagattataataggtattttcaagtacagtaacaaaaatc"+
            "ttctagtttttttttagaaaggatacaccaagtataagcaaattcaggaattgttgatta"+
            "aactgtcaacttcggtaaaactttgggcataagtagtgtgggagcaagtttaactaaaat"+
            "tctattcagatgtcgaatccaaaccgctaattttgctcaactagcttttcataaaaacca"+
            "attcatagtttcatactaataaagacgattgtttactttacacgccggaaatgaaaaccg"+
            "caatggtgctcaaggcaaaagacgttatccgccgtggctgtctggaatacgacgtcagcg"+
            "ccaccgacatcaccagctcgtttatggctatccgcaagaccatgaccagcagcggacgca"+
            "gcgccacctatgaggccagccgcagcgaggaagccagccacgccgacctcgcctgggcga"+
            "ccatgcacgccctgttaaatgagccactcaccgccggtatcagcaccccgctgacatcca"+
            "ccattctggagttttactgatgagcaagaaaaaagggaaaacaccgcaacctgcggcaaa"+
            "aacaatgaccgccagcggcccgaaaatggaggcattcacctttggtgagccggtgccggt"+
            "actcgaccgccgtgacattctggattacgtcgagtgcatcagtaacggcagatggtatga"+
            "gccaccggtcagctttaccggtctggcaaaaagcctgcgggctgccgtgcatcacagctc"+
            "gccgatttacgtcaaacgcaatattctggcctcgacatttatcccgcatccatggctttc"+
            "ccagcaggatttcagccgctttgtgctggattttctggtgttcggtaatgcgtttctgga"+
            "aaagcgttacagcaccaccggtaaggtcatcagactggaaacctcaccggcaaaatatac"+
            "ccgccgtggcgtggaagaggatgtttactggtgggtgccgtccttcaacgagccgacagc"+
            "cttcgcgcccggctccgtgtttcacctgctggagccggatattaatcaggagctgtacgg"+
            "cctgccggaatatctcagcgcccttaactctgcctggctgaatgagtcggccacgttgtt"+
            "ccgccgcaagtattacgaaaatggcgcacatgccggatacatcatgtatgtcaccgatgc"+
            "cgtgcaggatcgcaacgatatcgaaatgcttcgcgaaaacatggtcaagtcgaaaggccg"+
            "caacaactttaaaaacctgtttctctatgccccacaggggaaagccgacggcattaaaat"+
            "tatccccctcagtgaagtggcgacgaaggacgatttttttaatatcaaaaaagccagcgc"+
            "cgctgacctgctggacgcgcaccgcatcccctttcagttgatgggtggcaagccggagaa"+
            "cgtcgggtcgctgggtgatattgagaaagtggcaaaggtctttgtccgcaatgagcttat"+
            "cccgttacaggacaggatccgcgagataaacggctggctcggtcaggaggtcatccgctt"+
            "taaaaactactcactggacactgacaacgactgaacatcgccgcctgcgggcggcttttt"+
            "tacaccccgtcatcccccctcacacgctcaccaccgcacaaaacaccccgcagacacaca"+
            "acgcctcaacgggcagactaagtgccgtcacgacgcgctgagacgctgaaaaaatacaat"+
            "cagcaccaccgtcagcgcgcagtgctttccccgcctcgcc";
        Assert.assertEquals(expectedChrSequence, resChromosome.getSequence().getResidues());

    }

    public void checkExonSequences() throws Exception {
        osw.flushObjectById();

        ObjectStore os = osw.getObjectStore();

        Exon resExon0 = (Exon) os.getObjectById(storedExons[0].getId());
        Assert.assertEquals(expectedExonSequence0, resExon0.getSequence().getResidues());

        Exon resExon4 = (Exon) os.getObjectById(storedExons[4].getId());
        Assert.assertEquals(expectedExonSequence4, resExon4.getSequence().getResidues());

        Exon resExon1 = (Exon) os.getObjectById(storedExons[1].getId());
        Assert.assertEquals(expectedExonSequence1, resExon1.getSequence().getResidues());

        Exon resExon2 = (Exon) os.getObjectById(storedExons[2].getId());
        Assert.assertEquals(expectedExonSequence2, resExon2.getSequence().getResidues());

        Exon resExon5 = (Exon) os.getObjectById(storedExons[5].getId());
        Assert.assertEquals(expectedExonSequence5, resExon5.getSequence().getResidues());

        Exon resExon3 = (Exon) os.getObjectById(storedExons[3].getId());
        Assert.assertEquals(expectedExonSequence3, resExon3.getSequence().getResidues());

        Exon resExon6 = (Exon) os.getObjectById(storedExons[6].getId());
        Assert.assertEquals(expectedExonSequence6, resExon6.getSequence().getResidues());

        Exon resExon7 = (Exon) os.getObjectById(storedExons[7].getId());
        Assert.assertEquals(expectedExonSequence7, resExon7.getSequence().getResidues());

        RepeatRegion resRepeatRegion0 =
            (RepeatRegion) os.getObjectById(storedRepeatRegions[0].getId());
        Assert.assertEquals("gctatttttttatatccgtctttcttactacttgcc", resRepeatRegion0.getSequence().getResidues());

        RepeatRegion resRepeatRegion1 =
            (RepeatRegion) os.getObjectById(storedRepeatRegions[1].getId());
        Assert.assertEquals("atttttgttactgtacttgaaaatacctattataatctgtggctacaa",
                     resRepeatRegion1.getSequence().getResidues());

        RepeatRegion resRepeatRegion2 =
            (RepeatRegion) os.getObjectById(storedRepeatRegions[2].getId());
        Assert.assertEquals("gccagaccggtaaagctgaccggtggctcataccatctgccgtta",
                     resRepeatRegion2.getSequence().getResidues());

        RepeatRegion resRepeatRegion3 =
            (RepeatRegion) os.getObjectById(storedRepeatRegions[3].getId());
        Assert.assertEquals("tggcgacgaaggacgatttttttaatatcaaaaaagccagcgccg",
                     resRepeatRegion3.getSequence().getResidues());
    }

    public void checkTranscriptSequences() throws Exception {
        osw.flushObjectById();

        ObjectStore os = osw.getObjectStore();

        Transcript resTranscript0 =
            (Transcript) os.getObjectById(storedTranscripts[0].getId());
        String expectedResidues0 = expectedExonSequence1
            + expectedExonSequence2 + expectedExonSequence3;
        assertEquals(expectedResidues0, resTranscript0.getSequence().getResidues());

        Transcript resTranscript1 =
            (Transcript) os.getObjectById(storedTranscripts[1].getId());
        String expectedResidues1 = expectedExonSequence7 + expectedExonSequence6;
        assertEquals(expectedResidues1,
                     resTranscript1.getSequence().getResidues());
    }

    private void createData() throws Exception {
        osw.flushObjectById();

        storedChromosome =
            (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        storedChromosome.setLength(new Integer(4000));
        storedChromosome.setId(new Integer(101));
        storedChromosome.setIdentifier("store_chromosome");

        storedSupercontig =
            (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        storedSupercontig.setId(new Integer(201));

        storedContigs = new Contig[3];
        storedContigSequences = new Sequence[3];

        Set toStore = new HashSet(Arrays.asList(new Object[] {
                                                    storedChromosome,
                                                    storedSupercontig,
                                                }));
        toStore.add(createLocation(storedChromosome, storedSupercontig, 1, 501, 4000));

        storedContigSequences[0] =
            (Sequence) DynamicUtil.createObject(Collections.singleton(Sequence.class));
        storedContigs[0] =
            (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        storedContigs[0].setIdentifier("contig0");
        storedContigs[0].setId(new Integer(300));
        storedContigs[0].setLength(new Integer(1000));
        String contigResidues0 =
            "TATATATAATTTAATAAATACATTCCGACGATACTGCCTCTATGGCTTAGTGGTACAGCA" +
            "TCGCACTTGTAATGCGAAGATCCTTGGTTCGATTCCGAGTGGAGGCATATACATTATATT" +
            "ATATTCTTTTTCATGCGGAAAAAAGATTTCAAATTTTTGGGTATGATATTAATATGACTG" +
            "TAACGTTAATAGCAAAGTGAGTGTTAATAATGATAAAATAGCAGCAAAATCTCTTTTCCG" +
            "AGTAAGACGTTTTCCAGTCTAAATTTGGAGTCTGCAGTTGTTTCGCAATTCTTAATGTAT" +
            "GGTTATACTAAATACAAACTTTAAAGCTCTGATTTATGTTTGCAATAAACTAAAATAAAA" +
            "GCACAAAAACCTTTACCCATTAATTTCAAACAACTTATAAACTACCGGTAAACTTTTTTT" +
            "CTAACCTTTATAATTTATAAACTAGAATGTTTAATGTCTACGGCCATACCTAGGCGAAAA" +
            "CACCAGTTCCCGTCCGATCACTGCAGTTAAGCGTCTGAGGGCCTCGTTAGTACTATGGTT" +
            "GGAGACAACATGGGAATCCGGGGTGCTGTAGGCTATTTTTTTATATCCGTCTTTCTTACT" +
            "ACTTGCCTAACAAGTCATGATGTACTCTCAAAATATGTTTGCATGCCTTGTAATATTGGT" +
            "TATGGATAGCTCCTTCTGGACTTGATCTTTTGTAGCCAAGAACAATGGGTATAGACTCTG" +
            "ACCTTGTGATGTTGTAGCCACAGATTATAATAGGTATTTTCAAGTACAGTAACAAAAATC" +
            "TTCTAGTTTTTTTTTAGAAAGGATACACCAAGTATAAGCAAATTCAGGAATTGTTGATTA" +
            "AACTGTCAACTTCGGTAAAACTTTGGGCATAAGTAGTGTGGGAGCAAGTTTAACTAAAAT" +
            "TCTATTCAGATGTCGAATCCAAACCGCTAATTTTGCTCAACTAGCTTTTCATAAAAACCA" +
            "ATTCATAGTTTCATACTAATAAAGACGATTGTTTACTTTA";
        storedContigSequences[0].setResidues(contigResidues0);
        storedContigs[0].setSequence(storedContigSequences[0]);

        toStore.add(createLocation(storedSupercontig, storedContigs[0], 1, 1001, 2000));

        storedContigSequences[1] =
            (Sequence) DynamicUtil.createObject(Collections.singleton(Sequence.class));
        storedContigs[1] =
            (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        storedContigs[1].setIdentifier("contig1");
        storedContigs[1].setId(new Integer(301));
        storedContigs[1].setLength(new Integer(1500));
        String contigResidues1 =
            "ggcgaggcggggaaagcactgcgcgctgacggtggtgctgattgtattttttcagcgtct" +
            "cagcgcgtcgtgacggcacttagtctgcccgttgaggcgttgtgtgtctgcggggtgttt" +
            "tgtgcggtggtgagcgtgtgaggggggatgacggggtgtaaaaaagccgcccgcaggcgg" +
            "cgatgttcagtcgttgtcagtgtccagtgagtagtttttaaagcggatgacctcctgacc" +
            "gagccagccgtttatctcgcggatcctgtcctgtaacgggataagctcattgcggacaaa" +
            "gacctttgccactttctcaatatcacccagcgacccgacgttctccggcttgccacccat" +
            "caactgaaaggggatgcggtgcgcgtccagcaggtcagcggcgctggcttttttgatatt" +
            "AAAAAAATCGTCCTTCGTCGCCACTTCACTGAGGGGGATAATTTTAATGCCGTCGGCTTT" +
            "CCCCTGTGGGGCATAGAGAAACAGGTTTTTAAAGTTGTTGCGGCCTTTCGACTTGACCAT" +
            "GTTTTCGCGAAGCATTTCGATATCGTTGCGATCCTGCACGGCATCGGTGACATACATGAT" +
            "GTATCCGGCATGTGCGCCATTTTCGTAATACTTGCGGCGGAACAACGTGGCCGACTCATT" +
            "CAGCCAGGCAGAGTTAAGGGCGCTGAGATATTCCGGCAGGCCGTACAGCTCCTGATTAAT" +
            "ATCCGGCTCCAGCAGGTGAAACACGGAGCCGGGCGCGAAGGCTGTCGGCTCGTTGAAGGA" +
            "CGGCACCCACCAGTAAACATCCTCTTCCACGCCACGGCGGGTATATTTTGCCGGTGAGGT" +
            "TTCCAGTCTGATGACCTTACCGGTGGTGCTGTAACGCTTTTCCAGAAACGCATTACCGAA" +
            "caccagaaaatccAGCACAAAGCGGCTGAAATCCTGCTGGGAAAGCCATGGATGCGGGAT" +
            "aaatgtcgaggccagaatattgcgtttgacgtaaatcggcgagctgtgatgcacggcagc" +
            "ccgcaggctttttgccagaccggtaaagctgaccggtggctcataccatctgccgttact" +
            "gatgcactcgacgtaatccagaatgtcacggcggtcgagtaccggcaccggctcaccaaa" +
            "ggtgaatgcctccattttcgggccgctggcggtcattgtttttgccgcaggttgcggtgt" +
            "tttcccttttttcttgctcatcagtaaaactccagaatggtggatgtcagcggggtgctg" +
            "ataccggcggtgagtggctcatttaacagggcgtgcatggtcgcccaggcgaggtcggcg" +
            "tggctggcttcctcgctgcggctggcctcataggtggcgctgcgtccgctgctggtcatg" +
            "gtcttgcggatagccataaacgagctggtgatgtcggtggcgctgacgtcgtattccaga" +
            "cagccacggcggataacgtcttttgccttgagcaccattgcggttttcatttccggcgtg";
        storedContigSequences[1].setResidues(contigResidues1);
        storedContigs[1].setSequence(storedContigSequences[1]);

        toStore.add(createLocation(storedSupercontig, storedContigs[1], -1, 2001, 3500));

        storedContigSequences[2] =
            (Sequence) DynamicUtil.createObject(Collections.singleton(Sequence.class));
        storedContigs[2] =
            (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        storedContigs[2].setIdentifier("contig2");
        storedContigs[2].setId(new Integer(302));
        storedContigs[2].setLength(new Integer(1000));
        String contigResidues2 =
            "gttaacagctaaaaacataactcgattacttacaattgttgattctttactcccagaacg" +
            "acgcattgtagtaggcaatgaaattaaagcccaaacagtagtcagaagttggtaggtgtt" +
            "GCAGTCAAATTATTATTCTACAGAGGAGAATATTATAGCCAGCGTGGTAGAATCTGGATA" +
            "TATATCTACTGCAAAAGTGTAATTGCATTGGTTTAAAGGGTATACTATGGTTAAGTAATA" +
            "TATTCACAGCTGTACAATTTACAGTCATAACTAAAACTTCCTTAAGCCGTAAAGAAATAC" +
            "CTGGTGTTGTAAAATTTGTTGTATATCCACGGCATGGTCATATAATGTGATTTTGTGCTC" +
            "AAATAAATATAAAATATGCATAATTTTTGTACATTTAATTTGAGAAACCCATCTTTTGTT" +
            "GAGAGGCTGTCAATGAATAGCAGTTTCATTGAAAAGCAGCGGGATGACCAGAAAAGTATT" +
            "TTACAATGGCAAGGGAGTAGAAAGCTAGCGTAATATTCAGAAAGCTAGGTAATTGAGCAA" +
            "TCCTTTAATTCATTGCTAAGCATGCTAGGTAAACGCAGTAAACCTTTCAGTTTTCATTTA" +
            "GGTATAAGGCTGTTTAATGAGTATCTCCACTAAATTTAAAGATCAAAACTCAGTATCAAT" +
            "TCTTAAAAGTTTTATTTTATTTAATAATCATATACTTCTCATAATCTTTCAATTTTTTCC" +
            "CCATTTTGATGATATTTTTATTAATCCTACAGTAAGCTCTATGATATCGTTATTCTTCAA" +
            "ATAGGCTGGTCAGCACGTGGACGGTGTTACTTATCGTTAAATAAATCGTACTAAGGAGGT" +
            "gcgatgtaaatgatatgcttgtcaagtattaactgctctccaccaaccgccggtttaact" +
            "gattattgttgaaaagcgcagacgaagtttagagaattactagcgtattttaaatttaat" +
            "caacggactattttttattcctttgagatccgactttatc";
        storedContigSequences[2].setResidues(contigResidues2);
        storedContigs[2].setSequence(storedContigSequences[2]);

        toStore.add(createLocation(storedSupercontig, storedContigs[2], -1, 1, 1000));

        storedTranscripts = new Transcript[2];
        for (int i = 0 ; i < storedTranscripts.length ; i++) {
            storedTranscripts[i] =
                (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
            storedTranscripts[i].setIdentifier("transcript_" + i);
        }

        storedExons = new Exon [8];
        for (int i = 0 ; i < storedExons.length ; i++) {
            storedExons[i] = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
            storedExons[i].setIdentifier("exon_" + i);
        }

        List transcript0Exons = Arrays.asList(new Object[] {storedExons[1], storedExons[2],
            storedExons[3]});
        storedTranscripts[0].setExons(new HashSet(transcript0Exons));

        List transcript1Exons = Arrays.asList(new Object[] {storedExons[7], storedExons[6]});
        storedTranscripts[1].setExons(new HashSet(transcript1Exons));

        for (int i = 0; i < transcript0Exons.size() ; i++) {
            RankedRelation rankedRelation =
                (RankedRelation) DynamicUtil.createObject(Collections.singleton(RankedRelation.class));
            rankedRelation.setRank(new Integer(i + 1));
            rankedRelation.setSubject((BioEntity) transcript0Exons.get(i));
            rankedRelation.setObject(storedTranscripts[0]);
            toStore.add(rankedRelation);
        }

        for (int i = 0; i < transcript1Exons.size() ; i++) {
            RankedRelation rankedRelation =
                (RankedRelation) DynamicUtil.createObject(Collections.singleton(RankedRelation.class));
            rankedRelation.setRank(new Integer(i + 1));
            rankedRelation.setSubject((BioEntity) transcript1Exons.get(i));
            rankedRelation.setObject(storedTranscripts[1]);
            toStore.add(rankedRelation);
        }

        storedRepeatRegions = new RepeatRegion [4];
        for (int i = 0 ; i < storedRepeatRegions.length ; i++) {
            storedRepeatRegions[i] =
                (RepeatRegion) DynamicUtil.createObject(Collections.singleton(RepeatRegion.class));
            storedRepeatRegions[i].setIdentifier("repeat_region_" + i);
        }

        toStore.add(createLocation(storedContigs[0], storedExons[0], 1,   173,  259));
        toStore.add(createLocation(storedContigs[0], storedExons[4], -1,  454,  605));
        toStore.add(createLocation(storedContigs[0], storedExons[1], 1,   773,  814));
        toStore.add(createLocation(storedContigs[0], storedExons[2], 1,   984, 1000));
        toStore.add(createLocation(storedContigs[0], storedExons[5], -1,  996, 1000));

        toStore.add(createLocation(storedContigs[1], storedExons[7], 1,     8,  138));
        toStore.add(createLocation(storedContigs[1], storedExons[6], 1,   368,  493));
        toStore.add(createLocation(storedContigs[1], storedExons[3], -1,  663, 1173));
        toStore.add(createLocation(storedContigs[1], storedExons[2], -1, 1224, 1500));
        toStore.add(createLocation(storedContigs[1], storedExons[5], 1,  1287, 1500));

        toStore.add(createLocation(storedContigs[0], storedRepeatRegions[0], 1, 572, 607));
        toStore.add(createLocation(storedContigs[0], storedRepeatRegions[1], -1, 732, 779));

        toStore.add(createLocation(storedContigs[1], storedRepeatRegions[2], 1, 1034, 1078));
        toStore.add(createLocation(storedContigs[1], storedRepeatRegions[3], -1, 399, 443));

        Iterator iter = toStore.iterator();
        osw.beginTransaction();
        while (iter.hasNext()) {
            osw.store((InterMineObject) iter.next());
        }
        for (int i = 0; i<storedTranscripts.length; i++) {
            osw.store(storedTranscripts[i]);
        }
        for (int i = 0; i<storedExons.length; i++) {
            osw.store(storedExons[i]);
        }
        for (int i = 0; i<storedRepeatRegions.length; i++) {
            osw.store(storedRepeatRegions[i]);
        }

        osw.store(storedContigs[0]);
        osw.store(storedContigs[1]);
        osw.store(storedContigs[2]);
        osw.store(storedContigSequences[0]);
        osw.store(storedContigSequences[1]);
        osw.store(storedContigSequences[2]);
        osw.commitTransaction();
    }

    private Location createLocation(BioEntity object, BioEntity subject,
                                    int strand, int start, int end) {
        Location loc = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        loc.setObject(object);
        loc.setSubject(subject);
        loc.setStrand(new Integer(strand));
        loc.setStart(new Integer(start));
        loc.setEnd(new Integer(end));
        loc.setStartIsPartial(Boolean.FALSE);
        loc.setEndIsPartial(Boolean.FALSE);
        loc.setStrand(new Integer(strand));
        return loc;
    }
}
