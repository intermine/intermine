package org.intermine.bio.web.biojava;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.ProteinTools;
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
     * Create a new BioSequence from a BioEntity
     * @param bioEnt the bio entity
     * @param type the SequenceType
     * @return a new BioSequence object or null if the Protein doesn't have a Sequence
     * @throws IllegalSymbolException if any of the residues of the Protein can't be
     * turned into amino acid symbols.
     */
    public static BioSequence make(BioEntity bioEnt, SequenceType type)
        throws IllegalSymbolException {
        if (bioEnt instanceof Protein) {
            Protein protein = (Protein) bioEnt;
            if (protein.getSequence() == null || protein.getSequence().getResidues() == null) {
                return null;
            } else {
                String residues = protein.getSequence().getResidues().toString();
                return new BioSequence(ProteinTools.createProtein(residues), protein);
            }
        } else if (bioEnt instanceof SequenceFeature) {
            SequenceFeature feature = (SequenceFeature) bioEnt;
            if (feature.getSequence() == null || feature.getSequence().getResidues() == null) {
                return null;
            } else {
                String residues = feature.getSequence().getResidues().toString();
                return new BioSequence(DNATools.createDNA(residues), feature);
            }
        } else {
            throw new RuntimeException("Sequence type not defined.");
        }
    }
}
