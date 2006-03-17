package org.flymine.postprocess;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.Iterator;

import org.biojava.bio.symbol.SymbolList;
import org.biojava.bio.symbol.IllegalSymbolException;
import org.biojava.bio.symbol.IllegalAlphabetException;
import org.biojava.bio.seq.DNATools;

import org.intermine.objectstore.query.*;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.util.DynamicUtil;

import org.intermine.objectstore.query.ResultsRow;
import org.flymine.model.genomic.*;

import org.apache.log4j.Logger;

/**
 * Transfer sequences from the Assembly objects to the other objects that are located on the
 * Assemblys and to the objects that the Assemblys are located on (eg. Chromosomes).
 *
 * @author Kim Rutherford
 */

public class TransferSequences
{
    protected ObjectStoreWriter osw;

    private static final Logger LOG = Logger.getLogger(TransferSequences.class);

    /**
     * Create a new TransferSequences object from the given ObjectStoreWriter
     * @param osw writer on genomic ObjectStore
     */
    public TransferSequences (ObjectStoreWriter osw) {
        this.osw = osw;
    }

    /**
     * Copy the Assembly sequences to the appropriate place in the chromosome sequences and store a
     * Sequence object for each Chromosome.  Uses the ObjectStoreWriter that was passed to the
     * constructor
     * @throws Exception if there are problems with the transfer
     */
    public void transferToChromosome()
        throws Exception {
        osw.beginTransaction();
        ObjectStore os = osw.getObjectStore();

        Results results = 
            PostProcessUtil.findLocationAndObjects(os, Chromosome.class, Assembly.class, false);
        // could try reducing further if still OutOfMemeory problems
        results.setBatchSize(20);

        Iterator resIter = results.iterator();

        Chromosome currentChr = null;
        char[] currentChrBases = null;

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            Integer chrId = (Integer) rr.get(0);
            Chromosome chr = (Chromosome) os.getObjectById(chrId);
            Assembly assembly = (Assembly) rr.get(1);
            Location assemblyOnChrLocation = (Location) rr.get(2);

            if (currentChr == null || !chr.equals(currentChr)) {
                if (currentChr != null) {
                    storeNewSequence(currentChr, currentChrBases);
                    LOG.info("finished Chromosome: " + currentChr.getIdentifier());
                }
                currentChrBases = new char[chr.getLength().intValue()];
                // fill with '.' so we can see the parts of the Chromosome sequence that haven't
                // been set
                for (int i = 0; i < currentChrBases.length; i++) {
                    currentChrBases[i] = '.';
                }
                currentChr = chr;
            }

            copySeqArray(currentChrBases, assembly.getSequence().getResidues(),
                         assemblyOnChrLocation.getStart().intValue(),
                         assemblyOnChrLocation.getEnd().intValue(),
                         assemblyOnChrLocation.getStrand().intValue());
        }
        if (currentChrBases == null) {
            LOG.error("in transferToChromosome(): no Assembly sequences found");
        } else {
            storeNewSequence(currentChr, currentChrBases);
            LOG.info("finished Chromosome: " + currentChr.getIdentifier());
        }
        osw.commitTransaction();
    }

    private void storeNewSequence(LocatedSequenceFeature feature, char [] featureBases)
        throws ObjectStoreException {
        Sequence sequence =
            (Sequence) DynamicUtil.createObject(Collections.singleton(Sequence.class));
        sequence.setResidues(new String(featureBases));
        sequence.setLength(featureBases.length);
        feature.setSequence(sequence);
        feature.setLength(new Integer(featureBases.length));
        osw.store(feature);
        osw.store(sequence);
    }

    /**
     * Use the Location relations to copy the sequence from the Chromosomes to every
     * LocatedSequenceFeature that is located on a Chromosome and which doesn't already have a
     * sequence (ie. don't copy to Assembly).  Uses the ObjectStoreWriter that was passed to the
     * constructor
     * @throws Exception if there are problems with the transfer
     */
    public void transferToLocatedSequenceFeatures()
        throws Exception {
        ObjectStore os = osw.getObjectStore();
        osw.beginTransaction();

        Results results =
            PostProcessUtil.findLocationAndObjects(os, Chromosome.class,
                                                   LocatedSequenceFeature.class, true);
        results.setBatchSize(500);

        Iterator resIter = results.iterator();

        long start = System.currentTimeMillis();
        int i = 0;
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();

            Integer chrId = (Integer) rr.get(0);
            LocatedSequenceFeature feature = (LocatedSequenceFeature) rr.get(1);
            Location locationOnChr = (Location) rr.get(2);

            if (feature instanceof Assembly) {
                continue;
            }

            if (feature instanceof ChromosomeBand) {
                continue;
            }

            Chromosome chr = (Chromosome) os.getObjectById(chrId);
            Sequence chromosomeSequence = chr.getSequence();

            if (chromosomeSequence == null) {
                throw new Exception("no sequence found for: " + chr.getIdentifier() + "  id: "
                                    + chr.getId());
            }

            String featureSeq = getSubSequence(chromosomeSequence, locationOnChr);

            Sequence sequence =
                (Sequence) DynamicUtil.createObject(Collections.singleton(Sequence.class));
            sequence.setResidues(featureSeq);
            sequence.setLength(featureSeq.length());
            feature.setSequence(sequence);
            osw.store(feature);
            osw.store(sequence);
            i++;
            if (i % 1000 == 0) {
                long now = System.currentTimeMillis();
                LOG.info("Set sequences for " + i + " features"
                         + " (avg = " + ((60000L * i) / (now - start)) + " per minute)");
            }
        }

        osw.commitTransaction();
    }

    private String getSubSequence(Sequence chromosomeSequence, Location locationOnChr)
        throws IllegalSymbolException, IllegalAlphabetException {
        int charsToCopy =
            locationOnChr.getEnd().intValue() - locationOnChr.getStart().intValue() + 1;
        String chromosomeSequenceString = chromosomeSequence.getResidues();
        int startPos = locationOnChr.getStart().intValue() - 1;
        int endPos = startPos + charsToCopy;

        if (startPos < 0 || endPos < 0) {
            return "";

// TODO XXX FIXME - uncomment this
//             throw new RuntimeException("in TransferSequences.getSubSequence(): locationOnChr "
//                                        + locationOnChr
//                                        + "\n  startPos: " + startPos + " endPos " + endPos
//                                        + "\n chromosomeSequence.substr(0,1000) " +
//                                        chromosomeSequenceString.substring(0,1000)
//                                        + "\n location.getObject() " 
//                                        + locationOnChr.getObject().toString()
//                                        + " location.getSubject() " +
//                                        locationOnChr.getSubject().toString() + " "
//                                        + "\n location.getSubject().getId() " +
//                                        locationOnChr.getSubject().getId() +
//                                        "\n location.getObject().getId() ");
        }

        String subSeqString = new String(chromosomeSequenceString.substring(startPos, endPos));

        if (locationOnChr.getStrand().intValue() == -1) {
            SymbolList symbolList = DNATools.createDNA(subSeqString);

            symbolList = DNATools.reverseComplement(symbolList);

            subSeqString = symbolList.seqString();
        }

        return subSeqString;
    }

    private void copySeqArray(char[] destArray, String sourceSequence,
                              int start, int end, int strand)
        throws IllegalSymbolException, IllegalAlphabetException {
        char[] sourceArray;

        if (strand == -1) {
            SymbolList symbolList = DNATools.createDNA(sourceSequence);

            symbolList = DNATools.reverseComplement(symbolList);

            sourceArray = symbolList.seqString().toCharArray();
        } else {
            sourceArray = sourceSequence.toLowerCase().toCharArray();
        }

        int charsToCopy = end - start + 1;

        System.arraycopy(sourceArray, 0, destArray, start - 1, charsToCopy);
    }

    /**
     * For each Transcript, join and transfer the sequences from the child Exons to a new Sequence
     * object for the Transcript.  Uses the ObjectStoreWriter that was passed to the constructor
     * @throws Exception if there are problems with the transfer
     */
    public void transferToTranscripts()
        throws Exception {

        osw.beginTransaction();

        ObjectStore os = osw.getObjectStore();
        Query q = new Query();
        q.setDistinct(false);

        QueryClass qcTranscript = new QueryClass(Transcript.class);
        q.addFrom(qcTranscript);
        q.addToSelect(qcTranscript);
        q.addToOrderBy(qcTranscript);

        QueryClass qcRankedRelation = new QueryClass(RankedRelation.class);
        q.addFrom(qcRankedRelation);
        q.addToSelect(qcRankedRelation);
        QueryField qfObj = new QueryField(qcRankedRelation, "rank");
        q.addToOrderBy(qfObj);

        QueryClass qcExon = new QueryClass(Exon.class);
        q.addFrom(qcExon);
        q.addToSelect(qcExon);

        QueryClass qcExonSequence = new QueryClass(Sequence.class);
        q.addFrom(qcExonSequence);
        q.addToSelect(qcExonSequence);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        QueryObjectReference rankTransRef =
            new QueryObjectReference(qcRankedRelation, "object");
        ContainsConstraint cc1 =
            new ContainsConstraint(rankTransRef, ConstraintOp.CONTAINS, qcTranscript);
        cs.addConstraint(cc1);

        QueryObjectReference rankExonRef =
            new QueryObjectReference(qcRankedRelation, "subject");
        ContainsConstraint cc2 =
            new ContainsConstraint(rankExonRef, ConstraintOp.CONTAINS, qcExon);
        cs.addConstraint(cc2);

        QueryObjectReference sequenceRef = new QueryObjectReference(qcExon, "sequence");
        ContainsConstraint cc3 =
            new ContainsConstraint(sequenceRef, ConstraintOp.CONTAINS, qcExonSequence);
        cs.addConstraint(cc3);

        q.setConstraint(cs);

        ((ObjectStoreInterMineImpl) os).precompute(q);
        Results res = new Results(q, os, os.getSequence());
        res.setBatchSize(200);

        Iterator resIter = res.iterator();

        Transcript currentTranscript = null;
        StringBuffer currentTranscriptBases = new StringBuffer();

        long start = System.currentTimeMillis();
        int i = 0;
        while (resIter.hasNext()) {
           ResultsRow rr = (ResultsRow) resIter.next();

           Transcript transcript =  (Transcript) rr.get(0);

           RankedRelation rankedRelation = (RankedRelation) rr.get(1);
           Exon exon = (Exon) rr.get(2);
           Sequence sequence = (Sequence) rr.get(3);
           String sequenceString = sequence.getResidues();

           if (currentTranscript == null || !transcript.equals(currentTranscript)) {
               if (currentTranscript != null) {
                   storeNewSequence(currentTranscript,
                                    currentTranscriptBases.toString().toCharArray());
                   i++;
               }
               currentTranscriptBases = new StringBuffer();
               currentTranscript = transcript;
           }

           currentTranscriptBases.append(exon.getSequence().getResidues());
           if (i % 100 == 0) {
               long now = System.currentTimeMillis();
               LOG.info("Set sequences for " + i + " Transcripts"
                        + " (avg = " + ((60000L * i) / (now - start)) + " per minute)");
           }
        }
        if (currentTranscript == null) {
            LOG.error("in transferToTranscripts(): no Transcripts found");
        } else {
            storeNewSequence(currentTranscript, currentTranscriptBases.toString().toCharArray());
        }

        osw.commitTransaction();
    }
}
