package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2017 FlyMine
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

import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.CDS;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Exon;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Sequence;
import org.intermine.model.bio.Transcript;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.PendingClob;
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
    private Exon [] storedExons;
    private Transcript [] storedTranscripts;
    private CDS storedCDS;

    private String expectedExonSequence0 =
        "ctctctctctaaagagaggggaggaggaggactctctctct";

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

    private String storedChrSequence =
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

    //private static final Logger LOG = Logger.getLogger(TransferSequencesTest.class);

    private static final String EXPECTED_TRANSCRIPT_0_RESIDUES =
        "tcaaatcaaattgataacttgtcaagtatccctatgcttgtcaagataaacct";

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        createData();
    }

    public void tearDown() throws Exception {
        if (osw.isInTransaction()) {
            osw.abortTransaction();
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        SingletonResults res = osw.getObjectStore().executeSingleton(q);
        Iterator resIter = res.iterator();
        osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            osw.delete(o);
        }
        osw.commitTransaction();
        osw.close();
    }


    public void testTransferToLocatedSequenceFeatures() throws Exception {
        TransferSequences ts = new TransferSequences(osw);
        ts.transferToLocatedSequenceFeatures();
        checkExonSequences();
    }

    public void testTranscriptSequence() throws Exception {
        TransferSequences ts = new TransferSequences(osw);
        ts.transferToLocatedSequenceFeatures();
        ts.transferToTranscripts();
        checkTranscriptSequences();
    }

    public void testIgnoreCDS() throws Exception {
        TransferSequences ts = new TransferSequences(osw);
        ts.transferToLocatedSequenceFeatures();
        ObjectStore os = osw.getObjectStore();
        System.out.println("storedCDS.getId(): " + storedCDS.getId());
        CDS resCDS = (CDS) os.getObjectById(storedCDS.getId());
        assertNotNull("CDS used to be skipped a long time ago but should be here now", resCDS.getSequence());
    }

    public void checkExonSequences() throws Exception {
        osw.flushObjectById();

        ObjectStore os = osw.getObjectStore();

        Exon resExon0 = (Exon) os.getObjectById(storedExons[0].getId());

        Assert.assertEquals(expectedExonSequence0, resExon0.getSequence().getResidues().toString());

        Exon resExon4 = (Exon) os.getObjectById(storedExons[4].getId());
        Assert.assertEquals(expectedExonSequence4, resExon4.getSequence().getResidues().toString());

        Exon resExon1 = (Exon) os.getObjectById(storedExons[1].getId());
        Assert.assertEquals(expectedExonSequence1, resExon1.getSequence().getResidues().toString());

        Exon resExon2 = (Exon) os.getObjectById(storedExons[2].getId());
        Assert.assertEquals(expectedExonSequence2, resExon2.getSequence().getResidues().toString());

        Exon resExon5 = (Exon) os.getObjectById(storedExons[5].getId());
        Assert.assertEquals(expectedExonSequence5, resExon5.getSequence().getResidues().toString());

        Exon resExon3 = (Exon) os.getObjectById(storedExons[3].getId());
        Assert.assertEquals(expectedExonSequence3, resExon3.getSequence().getResidues().toString());

        Exon resExon6 = (Exon) os.getObjectById(storedExons[6].getId());
        Assert.assertEquals(expectedExonSequence6, resExon6.getSequence().getResidues().toString());

        Exon resExon7 = (Exon) os.getObjectById(storedExons[7].getId());
        Assert.assertEquals(expectedExonSequence7, resExon7.getSequence().getResidues().toString());

    }

    public void checkTranscriptSequences() throws Exception {
        osw.flushObjectById();

        ObjectStore os = osw.getObjectStore();

        Transcript resTranscript0 =
            (Transcript) os.getObjectById(storedTranscripts[0].getId());
        assertEquals(EXPECTED_TRANSCRIPT_0_RESIDUES, resTranscript0.getSequence().getResidues().toString());

        Transcript resTranscript1 =
            (Transcript) os.getObjectById(storedTranscripts[1].getId());
        String expectedResidues1 = expectedExonSequence7 + expectedExonSequence6;
        assertEquals(expectedResidues1,
                     resTranscript1.getSequence().getResidues().toString());
    }

    private void createData() throws Exception {
        osw.flushObjectById();

        Set<InterMineObject> toStore = new HashSet<InterMineObject>();

        storedChromosome =
            (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        storedChromosome.setLength(new Integer(4000));
        storedChromosome.setId(new Integer(101));
        storedChromosome.setPrimaryIdentifier("store_chromosome");


        Sequence chrSequence =
            (Sequence) DynamicUtil.createObject(Collections.singleton(Sequence.class));
        PendingClob clob = new PendingClob(storedChrSequence);
        chrSequence.setResidues(clob.subSequence(0, storedChrSequence.length()));
        storedChromosome.setSequence(chrSequence);
        toStore.add(chrSequence);
        toStore.add(storedChromosome);

        storedTranscripts = new Transcript[2];
        for (int i = 0 ; i < storedTranscripts.length ; i++) {
            storedTranscripts[i] =
                (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
            storedTranscripts[i].setPrimaryIdentifier("transcript_" + i);
        }

        Sequence transcriptSequence =
            (Sequence) DynamicUtil.createObject(Collections.singleton(Sequence.class));
        clob = new PendingClob(EXPECTED_TRANSCRIPT_0_RESIDUES);
        transcriptSequence.setResidues(clob.subSequence(0, EXPECTED_TRANSCRIPT_0_RESIDUES.length()));
        storedTranscripts[0].setSequence(transcriptSequence);
        toStore.add(transcriptSequence);

        storedExons = new Exon [8];
        for (int i = 0 ; i < storedExons.length ; i++) {
            storedExons[i] = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
            storedExons[i].setPrimaryIdentifier("exon_" + i);
        }

        Sequence exonSequence =
            (Sequence) DynamicUtil.createObject(Collections.singleton(Sequence.class));
        clob = new PendingClob(expectedExonSequence0);
        exonSequence.setResidues(clob.subSequence(0, expectedExonSequence0.length()));
        storedExons[0].setSequence(exonSequence);
        toStore.add(exonSequence);

        List<Exon> transcript0Exons = Arrays.asList(new Exon[] {storedExons[1], storedExons[2],
            storedExons[3]});
        storedTranscripts[0].setExons(new HashSet<Exon>(transcript0Exons));

        List<Exon> transcript1Exons = Arrays.asList(new Exon[] {storedExons[7], storedExons[6]});
        storedTranscripts[1].setExons(new HashSet<Exon>(transcript1Exons));

        Location loc0 = createLocation(storedChromosome, storedExons[0], "1",   1673,  1759);
        toStore.add(loc0);
        storedExons[0].setChromosomeLocation(loc0);
        Location loc1 = createLocation(storedChromosome, storedExons[1], "1",   2273,  2314);
        toStore.add(loc1);
        storedExons[0].setChromosomeLocation(loc1);
        Location loc2 = createLocation(storedChromosome, storedExons[2], "1",   2484, 2777);
        toStore.add(loc2);
        storedExons[2].setChromosomeLocation(loc2);
        Location loc3 = createLocation(storedChromosome, storedExons[3], "1",  2828, 3338);
        toStore.add(loc3);
        storedExons[3].setChromosomeLocation(loc3);
        Location loc4 = createLocation(storedChromosome, storedExons[4], "-1",  1954,  2105);
        toStore.add(loc4);
        storedExons[4].setChromosomeLocation(loc4);
        Location loc5 = createLocation(storedChromosome, storedExons[5], "-1",  2496, 2714);
        toStore.add(loc5);
        storedExons[5].setChromosomeLocation(loc5);
        Location loc6 = createLocation(storedChromosome, storedExons[6], "-1",   3508,  3633);
        toStore.add(loc6);
        storedExons[6].setChromosomeLocation(loc6);
        Location loc7 = createLocation(storedChromosome, storedExons[7], "-1",     3863, 3993);
        toStore.add(loc7);
        storedExons[7].setChromosomeLocation(loc7);

        storedCDS = (CDS) DynamicUtil.createObject(Collections.singleton(CDS.class));
        storedCDS.setPrimaryIdentifier("cds_1");
        toStore.add(storedCDS);
        Location loc8 = createLocation(storedChromosome, storedCDS, "1", 3863, 3993);
        toStore.add(loc8);

        osw.beginTransaction();
        for (InterMineObject obj : toStore) {
            osw.store(obj);
        }
        for (int i = 0; i<storedTranscripts.length; i++) {
            osw.store(storedTranscripts[i]);
        }
        for (int i = 0; i<storedExons.length; i++) {
            osw.store(storedExons[i]);
        }

        osw.commitTransaction();
    }

    private Location createLocation(BioEntity object, BioEntity subject,
                                    String strand, int start, int end) {
        Location loc = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        loc.setLocatedOn(object);
        loc.setFeature(subject);
        loc.setStrand(strand);
        loc.setStart(new Integer(start));
        loc.setEnd(new Integer(end));
        return loc;
    }
}
