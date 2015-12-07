package org.intermine.bio.web.biojava;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.ProteinTools;
import org.biojava.bio.seq.RNATools;
import org.biojava.bio.symbol.IllegalSymbolException;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.model.bio.Protein;

/**
 * A factory for creating BioSequence objects.
 *
 * @author Kim Rutherford
 */

public abstract class BioSequenceFactory
{
    /**
     * Type of sequences.
     *
     * @author Julie Sullivan
     */
    public enum SequenceType {
        /**
         * DNA sequence
         */
        DNA,
        /**
         * PROTEIN sequence
         */
        PROTEIN,
        /**
         * RNA sequence
         */
        RNA
    }

    private BioSequenceFactory() {
        // nothing to do
    }

    /**
     * Create a new BioSequence from a SequenceFeature
     * @param feature the SequenceFeature
     * @return a new BioSequence object or null if the SequenceFeature doesn't have a
     * Sequence
     * @throws IllegalSymbolException if any of the residues of the SequenceFeature can't be
     * turned into DNA symbols.
     */
    public static BioSequence make(SequenceFeature feature)
        throws IllegalSymbolException {
        if (feature.getSequence() == null) {
            return null;
        } else {
            String residues = feature.getSequence().getResidues().toString();
            return new BioSequence(DNATools.createDNA(residues), feature);
        }
    }

    /**
     * Create a new BioSequence from a Protein
     * @param protein the Protein
     * @return a new BioSequence object or null if the Protein doesn't have a Sequence
     * @throws IllegalSymbolException if any of the residues of the Protein can't be
     * turned into amino acid symbols.
     */
    public static BioSequence make(Protein protein)
        throws IllegalSymbolException {
        if (protein.getSequence() == null) {
            return null;
        } else {
            String residues = protein.getSequence().getResidues().toString();
            return new BioSequence(ProteinTools.createProtein(residues), protein);
        }
    }
    /**
     * Create a new BioSequence from a BioEntity, given its SequenceType
     * @param bioEnt the bio entity
     * @param type the SequenceType
     * @return a new BioSequence object or null if the BioEntity doesn't have a Sequence
     * @throws IllegalSymbolException if any of the residues of the BioEntity can't be
     * turned into symbols of the given SequenceType.
     * @author Sam Hokin
     *
     * NOTE: this has been rewritten by Sam Hokin, NCGR. It didn't formerly use the type parameter,
     * and simply polled the input BioEntity as to whether it was SequenceFeature or Protein, and
     * assumed DNA if SequenceFeature. The purpose of this version of make should be to force DNA
     * or Protein residues based on the type parameter, regardless of the type of BioEntity.
     * I added RNA support as well.
     */
    public static BioSequence make(BioEntity bioEnt, SequenceType type)
        throws IllegalSymbolException {
        if (bioEnt instanceof Protein) {
            // it really is a protein, which is not a SequenceFeature
            Protein protein = (Protein) bioEnt;
            if (protein.getSequence() == null || protein.getSequence().getResidues() == null) {
                return null;
            } else {
                String residues = protein.getSequence().getResidues().toString();
                return new BioSequence(ProteinTools.createProtein(residues), protein);
            }
        } else if (type.equals(SequenceType.PROTEIN)) {
            // it's an amino acid SequenceFeature, like a polypeptide from chado
            SequenceFeature feature = (SequenceFeature) bioEnt;
            if (feature.getSequence() == null || feature.getSequence().getResidues() == null) {
                return null;
            } else {
                String residues = feature.getSequence().getResidues().toString();
                return new BioSequence(ProteinTools.createProtein(residues), feature);
            }
        } else if (type.equals(SequenceType.DNA)) {
            // it's a DNA sequence
            SequenceFeature feature = (SequenceFeature) bioEnt;
            if (feature.getSequence() == null || feature.getSequence().getResidues() == null) {
                return null;
            } else {
                String residues = feature.getSequence().getResidues().toString();
                return new BioSequence(DNATools.createDNA(residues), feature);
            }
        } else if (type.equals(SequenceType.RNA)) {
            // we want an RNA sequence
            SequenceFeature feature = (SequenceFeature) bioEnt;
            if (feature.getSequence() == null || feature.getSequence().getResidues() == null) {
                return null;
            } else {
                String residues = feature.getSequence().getResidues().toString();
                return new BioSequence(RNATools.createRNA(residues), feature);
            }
        } else {
            throw new RuntimeException("Sequence type not defined. Choices are PROTEIN, DNA, RNA.");
        }
    }

}
