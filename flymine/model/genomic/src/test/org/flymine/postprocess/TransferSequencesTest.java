package org.flymine.postprocess;

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
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.io.InputStream;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.dataloader.IntegrationWriterFactory;
import org.intermine.dataloader.XmlDataLoader;
import org.intermine.dataloader.IntegrationWriter;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.datatracking.Source;
import org.intermine.util.DynamicUtil;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.Item;

import org.flymine.model.genomic.*;
import org.apache.log4j.Logger;

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
    private RepeatRegion [] storedRepeatRegions;
    private Model model;

    private static final Logger LOG = Logger.getLogger(TransferSequencesTest.class);

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.genomic-test");
        model = Model.getInstanceByName("genomic");
        createData();
    }

    public void tearDown() throws Exception {
        LOG.error("in tear down");
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ObjectStore os = osw.getObjectStore();
        SingletonResults res = new SingletonResults(q, osw.getObjectStore(),
                                                    osw.getObjectStore().getSequence());
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

    public void testExonSequence() throws Exception {
        CalculateLocations cl = new CalculateLocations(osw);
        cl.fixPartials();
        cl.createLocations();
        TransferSequences ts = new TransferSequences(osw);
        ts.transferSequences();
        checkSequences();
    }

    public void checkSequences() throws Exception {
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
        assertEquals(expectedChrSequence, resChromosome.getSequence().getSequence());

        String expectedExonSequence0 =
            "tatgactgtaacgttaatagcaaagtgagtgttaataatgataaaatagcagcaaaatct" +
            "cttttccgagtaagacgttttccagtc";
        Exon resExon0 = (Exon) os.getObjectById(storedExons[0].getId());
        assertEquals(expectedExonSequence0, resExon0.getSequence().getSequence());
        String expectedExonSequence4 =
            "caagtagtaagaaagacggatataaaaaaatagcctacagcaccccggattcccatgttg" +
            "tctccaaccatagtactaacgaggccctcagacgcttaactgcagtgatcggacgggaac" +
            "tggtgttttcgcctaggtatggccgtagacat";
        Exon resExon4 = (Exon) os.getObjectById(storedExons[4].getId());
        assertEquals(expectedExonSequence4, resExon4.getSequence().getSequence());
        String expectedExonSequence1 =
            "caaaaatcttctagtttttttttagaaaggatacaccaagta";
        Exon resExon1 = (Exon) os.getObjectById(storedExons[1].getId());
        assertEquals(expectedExonSequence1, resExon1.getSequence().getSequence());
        String expectedExonSequence2 =
            "agacgattgtttactttacacgccggaaatgaaaaccgcaatggtgctcaaggcaaaaga" +
            "cgttatccgccgtggctgtctggaatacgacgtcagcgccaccgacatcaccagctcgtt" +
            "tatggctatccgcaagaccatgaccagcagcggacgcagcgccacctatgaggccagccg" +
            "cagcgaggaagccagccacgccgacctcgcctgggcgaccatgcacgccctgttaaatga" +
            "gccactcaccgccggtatcagcaccccgctgacatccaccattctggagttttac";
        Exon resExon2 = (Exon) os.getObjectById(storedExons[2].getId());
        assertEquals(expectedExonSequence2, resExon2.getSequence().getSequence());
        String expectedExonSequence5 =
            "cagggcgtgcatggtcgcccaggcgaggtcggcgtggctggcttcctcgctgcggctggc" +
            "ctcataggtggcgctgcgtccgctgctggtcatggtcttgcggatagccataaacgagct" +
            "ggtgatgtcggtggcgctgacgtcgtattccagacagccacggcggataacgtcttttgc" +
            "cttgagcaccattgcggttttcatttccggcgtgtaaag";
        Exon resExon5 = (Exon) os.getObjectById(storedExons[5].getId());
        assertEquals(expectedExonSequence5, resExon5.getSequence().getSequence());
        String expectedExonSequence3 =
            "cgccagcggcccgaaaatggaggcattcacctttggtgagccggtgccggtactcgaccg" +
            "ccgtgacattctggattacgtcgagtgcatcagtaacggcagatggtatgagccaccggt" +
            "cagctttaccggtctggcaaaaagcctgcgggctgccgtgcatcacagctcgccgattta" +
            "cgtcaaacgcaatattctggcctcgacatttatcccgcatccatggctttcccagcagga" +
            "tttcagccgctttgtgctggattttctggtgttcggtaatgcgtttctggaaaagcgtta" +
            "cagcaccaccggtaaggtcatcagactggaaacctcaccggcaaaatatacccgccgtgg" +
            "cgtggaagaggatgtttactggtgggtgccgtccttcaacgagccgacagccttcgcgcc" +
            "cggctccgtgtttcacctgctggagccggatattaatcaggagctgtacggcctgccgga" +
            "atatctcagcgcccttaactctgcctggc";
        Exon resExon3 = (Exon) os.getObjectById(storedExons[3].getId());
        assertEquals(expectedExonSequence3, resExon3.getSequence().getSequence());
        String expectedExonSequence6 =
            "aaggggatgcggtgcgcgtccagcaggtcagcggcgctggcttttttgatattaaaaaaa" +
            "tcgtccttcgtcgccacttcactgagggggataattttaatgccgtcggctttcccctgt" +
            "ggggca";
        Exon resExon6 = (Exon) os.getObjectById(storedExons[6].getId());
        assertEquals(expectedExonSequence6, resExon6.getSequence().getSequence());
        String expectedExonSequence7 =
            "cggggaaagcactgcgcgctgacggtggtgctgattgtattttttcagcgtctcagcgcg" +
            "tcgtgacggcacttagtctgcccgttgaggcgttgtgtgtctgcggggtgttttgtgcgg" +
            "tggtgagcgtg";
        Exon resExon7 = (Exon) os.getObjectById(storedExons[7].getId());
        assertEquals(expectedExonSequence7, resExon7.getSequence().getSequence());

        RepeatRegion resRepeatRegion0 =
            (RepeatRegion) os.getObjectById(storedRepeatRegions[0].getId());
        assertEquals("gctatttttttatatccgtctttcttactacttgcc", resRepeatRegion0.getSequence().getSequence());

        RepeatRegion resRepeatRegion1 =
            (RepeatRegion) os.getObjectById(storedRepeatRegions[1].getId());
        assertEquals("atttttgttactgtacttgaaaatacctattataatctgtggctacaa",
                     resRepeatRegion1.getSequence().getSequence());

        RepeatRegion resRepeatRegion2 =
            (RepeatRegion) os.getObjectById(storedRepeatRegions[2].getId());
        assertEquals("gccagaccggtaaagctgaccggtggctcataccatctgccgtta",
                     resRepeatRegion2.getSequence().getSequence());

        RepeatRegion resRepeatRegion3 =
            (RepeatRegion) os.getObjectById(storedRepeatRegions[3].getId());
        assertEquals("tggcgacgaaggacgatttttttaatatcaaaaaagccagcgccg",
                     resRepeatRegion3.getSequence().getSequence());
    }

    private void createData() throws Exception {
        osw.flushObjectById();

        storedChromosome =
            (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        storedChromosome.setLength(new Integer(4000));
        storedChromosome.setId(new Integer(101));

        storedSupercontig =
            (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        storedSupercontig.setId(new Integer(201));

        storedContigs = new Contig[3];
        storedContigSequences = new Sequence[3];
        storedExons = new Exon[3];

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
            "tatatataatttaataaatacattccgacgatactgcctctatggcttagtggtacagca" +
            "tcgcacttgtaatgcgaagatccttggttcgattccgagtggaggcatatacattatatt" +
            "atattctttttcatgcggaaaaaagatttcaaatttttgggtatgatattaatatgactg" +
            "taacgttaatagcaaagtgagtgttaataatgataaaatagcagcaaaatctcttttccg" +
            "agtaagacgttttccagtctaaatttggagtctgcagttgtttcgcaattcttaatgtat" +
            "ggttatactaaatacaaactttaaagctctgatttatgtttgcaataaactaaaataaaa" +
            "gcacaaaaacctttacccattaatttcaaacaacttataaactaccggtaaacttttttt" +
            "ctaacctttataatttataaactagaatgtttaatgtctacggccatacctaggcgaaaa" +
            "caccagttcccgtccgatcactgcagttaagcgtctgagggcctcgttagtactatggtt" +
            "ggagacaacatgggaatccggggtgctgtaggctatttttttatatccgtctttcttact" +
            "acttgcctaacaagtcatgatgtactctcaaaatatgtttgcatgccttgtaatattggt" +
            "tatggatagctccttctggacttgatcttttgtagccaagaacaatgggtatagactctg" +
            "accttgtgatgttgtagccacagattataataggtattttcaagtacagtaacaaaaatc" +
            "ttctagtttttttttagaaaggatacaccaagtataagcaaattcaggaattgttgatta" +
            "aactgtcaacttcggtaaaactttgggcataagtagtgtgggagcaagtttaactaaaat" +
            "tctattcagatgtcgaatccaaaccgctaattttgctcaactagcttttcataaaaacca" +
            "attcatagtttcatactaataaagacgattgtttacttta";
        storedContigSequences[0].setSequence(contigResidues0);
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
        storedContigSequences[1].setSequence(contigResidues1);
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
            "gcagtcaaattattattctacagaggagaatattatagccagcgtggtagaatctggata" +
            "tatatctactgcaaaagtgtaattgcattggtttaaagggtatactatggttaagtaata" +
            "tattcacagctgtacaatttacagtcataactaaaacttccttaagccgtaaagaaatac" +
            "ctggtgttgtaaaatttgttgtatatccacggcatggtcatataatgtgattttgtgctc" +
            "aaataaatataaaatatgcataatttttgtacatttaatttgagaaacccatcttttgtt" +
            "gagaggctgtcaatgaatagcagtttcattgaaaagcagcgggatgaccagaaaagtatt" +
            "ttacaatggcaagggagtagaaagctagcgtaatattcagaaagctaggtaattgagcaa" +
            "tcctttaattcattgctaagcatgctaggtaaacgcagtaaacctttcagttttcattta" +
            "ggtataaggctgtttaatgagtatctccactaaatttaaagatcaaaactcagtatcaat" +
            "tcttaaaagttttattttatttaataatcatatacttctcataatctttcaattttttcc" +
            "ccattttgatgatatttttattaatcctacagtaagctctatgatatcgttattcttcaa" +
            "ataggctggtcagcacgtggacggtgttacttatcgttaaataaatcgtactaaggaggt" +
            "gcgatgtaaatgatatgcttgtcaagtattaactgctctccaccaaccgccggtttaact" +
            "gattattgttgaaaagcgcagacgaagtttagagaattactagcgtattttaaatttaat" +
            "caacggactattttttattcctttgagatccgactttatc";
        storedContigSequences[2].setSequence(contigResidues2);
        storedContigs[2].setSequence(storedContigSequences[2]);

        toStore.add(createLocation(storedSupercontig, storedContigs[2], -1, 1, 1000));

        storedExons = new Exon [8];
        for (int i = 0 ; i < 8 ; i++) {
            storedExons[i] = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
            storedExons[i].setIdentifier("exon" + i);
        }

        storedRepeatRegions = new RepeatRegion [4];
        for (int i = 0 ; i < 4 ; i++) {
            storedRepeatRegions[i] =
                (RepeatRegion) DynamicUtil.createObject(Collections.singleton(RepeatRegion.class));
            storedRepeatRegions[i].setIdentifier("repeat_region" + i);
        }

        toStore.add(createLocation(storedContigs[0], storedExons[0], 1,   173,  259));
        toStore.add(createLocation(storedContigs[0], storedExons[4], -1,  454,  605));
        toStore.add(createLocation(storedContigs[0], storedExons[1], 1,   773,  814));
        toStore.add(createLocation(storedContigs[0], storedExons[2], 1,   983, 1000));
        toStore.add(createLocation(storedContigs[0], storedExons[5], -1,  996, 1000));
        toStore.add(createLocation(storedContigs[1], storedExons[7], 1,     8,  138));
        toStore.add(createLocation(storedContigs[1], storedExons[6], 1,   368,  493));
        toStore.add(createLocation(storedContigs[1], storedExons[3], -1,  663, 1171));
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
