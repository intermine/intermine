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

import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

import org.biojava.bio.symbol.SymbolList;
import org.biojava.bio.symbol.IllegalSymbolException;
import org.biojava.bio.symbol.IllegalAlphabetException;
import org.biojava.bio.seq.DNATools;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreException;

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
        Map seqArrayMap = transferToChromosome();
        transferToLocatedSequenceFeatures(seqArrayMap);
        osw.commitTransaction();
    }

    private Map transferToChromosome()
        throws IllegalSymbolException, IllegalAlphabetException, ObjectStoreException {
        ObjectStore os = osw.getObjectStore();
        Iterator resIter = CalculateLocations.findLocations(os, Chromosome.class, Contig.class);

        // keep the new Chromosome sequences in a char[] for speed - convert to String at the end
        // this is a Map from Chromosome to char[]
        Map seqArrayMap = new HashMap();

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();

            Chromosome chr = (Chromosome) rr.get(0);
            Contig contig = (Contig) rr.get(1);
            Location contigOnChrLocation = (Location) rr.get(2);

            char[] chrSequence;

            if (seqArrayMap.get(chr) == null) {
                chrSequence = new char[chr.getLength().intValue()];
                // fill with '.' so we can see the parts of the Chromosome sequence that haven't
                // been set
                for (int i = 0; i < chrSequence.length; i++) {
                    chrSequence[i] = '.';
                }
                seqArrayMap.put(chr, chrSequence);
            } else {
                chrSequence = (char[]) seqArrayMap.get(chr);
            }

            copySeqArray(chrSequence, contig.getResidues(), contigOnChrLocation);
        }

// XXX - Chromosome needs a residues field
//
//         // set the Chromosome residue fields
//         Iterator iter = seqArrayMap.keySet().iterator();
//         while (iter.hasNext()) {
//             Chromosome chr = (Chromosome) iter.next();
//             chr.setResidues(new String((char[]) seqArrayMap(chr)));
//             osw.store(chr);
//         }

        Iterator iter = seqArrayMap.keySet().iterator();
        while (iter.hasNext()) {
            Chromosome chr = (Chromosome) iter.next();
        }

        return seqArrayMap;
    }

    private void transferToLocatedSequenceFeatures(Map seqArrayMap)
        throws IllegalSymbolException, IllegalAlphabetException, ObjectStoreException {
        ObjectStore os = osw.getObjectStore();
        Iterator resIter =
            CalculateLocations.findLocations(os, Chromosome.class, LocatedSequenceFeature.class);

        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();

            Chromosome chr = (Chromosome) rr.get(0);
            LocatedSequenceFeature feature = (LocatedSequenceFeature) rr.get(1);
            Location locationOnChr = (Location) rr.get(2);

            char[] chrSequence = (char[]) seqArrayMap.get(chr);
            String featureSeq = getSubSequence(chrSequence, locationOnChr);

            feature.setResidues(featureSeq);
            osw.store(feature);
        }
    }

    private String getSubSequence(char[] chrSequence, Location locationOnChr)
        throws IllegalSymbolException, IllegalAlphabetException {
        int charsToCopy =
            locationOnChr.getEnd().intValue() - locationOnChr.getStart().intValue() + 1;
        String subSeqString =
            new String(chrSequence, locationOnChr.getStart().intValue() - 1, charsToCopy);

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
}
