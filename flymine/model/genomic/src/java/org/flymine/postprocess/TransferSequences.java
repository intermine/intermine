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

import java.util.Collections;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

import org.biojava.bio.symbol.SymbolList;
import org.biojava.bio.symbol.IllegalSymbolException;
import org.biojava.bio.symbol.IllegalAlphabetException;
import org.biojava.bio.seq.DNATools;

import org.intermine.objectstore.query.*;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.DynamicUtil;

import org.intermine.objectstore.query.ResultsRow;
import org.flymine.model.genomic.*;

import org.apache.log4j.Logger;

/**
 * Transfer sequences from the Contig objects to the other objects that are located on the Contigs
 * and to the objects that the Contigs are located on (eg. Chromosomes).
 *
 * @author Kim Rutherford
 */

public class TransferSequences
{
    private static final Logger LOG = Logger.getLogger(TransferSequences.class);

    protected ObjectStoreWriter osw;

    /**
     * Create a new TransferSequences object from the given ObjectStoreWriter
     * @param osw writer on genomic ObjectStore
     */
    public TransferSequences (ObjectStoreWriter osw) {
        this.osw = osw;
    }

    /**
     * Copy the residues from the Contigs to the Chromosome in the ObjectStoreWriter that was
     * passed to the constructor
     * @throws Exception if there are problems with the transfer
     */
    public void transferSequences()
        throws Exception {
        osw.beginTransaction();
        transferToChromosome();
        transferToLocatedSequenceFeatures();
        transferToTranscripts();
        osw.commitTransaction();
    }

    /**
     * Copy the contig sequences to the appropriate place in the chromosome sequence and store a
     * Sequence object for each Chromosome.
     * Returns a Map from chromosome ID to a char array of the chromosome sequence.
     */
    private void transferToChromosome()
        throws IllegalSymbolException, IllegalAlphabetException, ObjectStoreException {
        ObjectStore os = osw.getObjectStore();
        Iterator resIter =
            CalculateLocationsUtil.findLocations(os, Chromosome.class, Contig.class, true);

        // keep the new Chromosome sequences in a char[] for speed - convert to String at the end
        // this is a Map from Chromosome to char[]
        Map seqArrayMap = new HashMap();

        Integer currentChrId = null;
        char[] currentChrBases = null;

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            Integer chrId = (Integer) rr.get(0);
            Chromosome chr = (Chromosome) os.getObjectById(chrId);
            Contig contig = (Contig) rr.get(1);
            Location contigOnChrLocation = (Location) rr.get(2);

            if (currentChrId == null || !chrId.equals(currentChrId)) {
                if (currentChrId != null) {
                    storeChromosome(chr, currentChrBases);
                }
                currentChrBases = new char[chr.getLength().intValue()];
                // fill with '.' so we can see the parts of the Chromosome sequence that haven't
                // been set
                for (int i = 0; i < currentChrBases.length; i++) {
                    currentChrBases[i] = '.';
                }
                currentChrId = chrId;
            }

            copySeqArray(currentChrBases, contig.getSequence().getSequence(), contigOnChrLocation);
        }
        Chromosome chr = (Chromosome) os.getObjectById(currentChrId);
        storeChromosome(chr, currentChrBases);
    }

    private void storeChromosome(Chromosome chr, char [] chrBases) throws ObjectStoreException {
        Sequence sequence =
            (Sequence) DynamicUtil.createObject(Collections.singleton(Sequence.class));
        sequence.setSequence(new String(chrBases));
        chr.setSequence(sequence);
        osw.store(chr);
        osw.store(sequence);
    }

    /**
     * Use the Location relations to copy the sequence from the Chromosomes to every
     * LocatedSequenceFeature that is located on a Chromosome and which doesn't already have a
     * sequence (ie. don't copy to Contig).
     */
    private void transferToLocatedSequenceFeatures()
        throws IllegalSymbolException, IllegalAlphabetException, ObjectStoreException {
        ObjectStore os = osw.getObjectStore();
        Iterator resIter =
            CalculateLocationsUtil.findLocations(os, Chromosome.class,
                                                 LocatedSequenceFeature.class, true);

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();

            Integer chrId = (Integer) rr.get(0);
            LocatedSequenceFeature feature = (LocatedSequenceFeature) rr.get(1);
            Location locationOnChr = (Location) rr.get(2);

            if (feature.getSequence() != null) {
                // probably a contig
                continue;
            }

            Chromosome chr = (Chromosome) os.getObjectById(chrId);
            Sequence chromosomeSequence = chr.getSequence();

            String featureSeq = getSubSequence(chromosomeSequence, locationOnChr);

            Sequence sequence =
                (Sequence) DynamicUtil.createObject(Collections.singleton(Sequence.class));
            sequence.setSequence(featureSeq);
            feature.setSequence(sequence);
            osw.store(feature);
            osw.store(sequence);
        }
    }

    private String getSubSequence(Sequence chromosomeSequence, Location locationOnChr)
        throws IllegalSymbolException, IllegalAlphabetException {
        int charsToCopy =
            locationOnChr.getEnd().intValue() - locationOnChr.getStart().intValue() + 1;
        String chromosomeSequenceString = chromosomeSequence.getSequence();
        int startPos = locationOnChr.getStart().intValue() - 1;
        int endPos = startPos + charsToCopy;
        String subSeqString = new String(chromosomeSequenceString.substring(startPos, endPos));

        if (locationOnChr.getStrand().intValue() == -1) {
            SymbolList symbolList = DNATools.createDNA(subSeqString);

            symbolList = DNATools.reverseComplement(symbolList);

            subSeqString = symbolList.seqString();
        }

        return subSeqString;
    }

    private void copySeqArray(char[] destArray, String sourceSequence, Location contigOnChrLocation)
        throws IllegalSymbolException, IllegalAlphabetException {
        char[] sourceArray;

        if (contigOnChrLocation.getStrand().intValue() == -1) {
            SymbolList symbolList = DNATools.createDNA(sourceSequence);

            symbolList = DNATools.reverseComplement(symbolList);

            sourceArray = symbolList.seqString().toCharArray();
        } else {
            sourceArray = sourceSequence.toCharArray();
        }

        int charsToCopy =
            contigOnChrLocation.getEnd().intValue() - contigOnChrLocation.getStart().intValue() + 1;

        System.arraycopy(sourceArray, 0,
                         destArray, contigOnChrLocation.getStart().intValue() - 1, charsToCopy);
    }

    /**
     * For each Transcript, join and transfer the sequences to the parent Transcript.
     */
    private void transferToTranscripts() {
        ObjectStore os = osw.getObjectStore();
        Query q = new Query();
        q.setDistinct(false);
        QueryClass qcTranscript = new QueryClass(Transcript.class);
        q.addFrom(qcTranscript);
        q.addToSelect(qcTranscript);
        QueryClass qcExon = new QueryClass(Exon.class);
        q.addFrom(qcExon);
        q.addToSelect(qcExon);
        // add to query so the Exon.getSequence() is (mostly) fast
        QueryClass qcExonSequence = new QueryClass(Sequence.class);
        q.addFrom(qcExonSequence);
        q.addToSelect(qcExonSequence);
        q.addToOrderBy(qcTranscript);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryCollectionReference exonRef = new QueryCollectionReference(qcTranscript, "exons");
        ContainsConstraint cc1 = new ContainsConstraint(exonRef, ConstraintOp.CONTAINS, qcExon);
        cs.addConstraint(cc1);
        QueryObjectReference sequenceRef = new QueryObjectReference(qcExon, "sequence");
        ContainsConstraint cc2 =
            new ContainsConstraint(sequenceRef, ConstraintOp.CONTAINS, qcExonSequence);
        cs.addConstraint(cc2);
        q.setConstraint(cs);

        Results res = new Results(q, os, os.getSequence());
        res.setBatchSize(10000);


        Iterator resIter = res.iterator();

        while (resIter.hasNext()) {
//            org.intermine.web.LogMe.log("i", "next: " + resIter.next());
        }
    }
}
