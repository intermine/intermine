package org.intermine.bio.web.biojava;

/*
 * Copyright (C) 2002-2007 FlyMine
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

import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Translation;

/**
 * A factory for creating BioSequence objects.
 *
 * @author Kim Rutherford
 */

public abstract class BioSequenceFactory
{
    /**
     * Create a new BioSequence from a LocatedSequenceFeature
     * @param feature the LocatedSequenceFeature
     * @return a new BioSequence object or null if the LocatedSequenceFeature doesn't have a
     * Sequence 
     * @throws IllegalSymbolException if any of the residues of the LocatedSequenceFeature can't be
     * turned into DNA symbols.
     */
    public static BioSequence make(LocatedSequenceFeature feature)
        throws IllegalSymbolException {
        if (feature.getSequence() == null) {
            return null;
        } else {
            String residues = feature.getSequence().getResidues();
            return new BioSequence(DNATools.createDNA(residues), feature);
        }
    }

    /**
     * Create a new BioSequence from a Translation
     * @param translation the Translation
     * @return a new BioSequence object or null if the Translation doesn't have a Sequence
     * @throws IllegalSymbolException if any of the residues of the Translation can't be
     * turned into amino acid symbols.
     */
    public static BioSequence make(Translation translation)
        throws IllegalSymbolException {
        if (translation.getSequence() == null) {
            return null;
        } else {
            String residues = translation.getSequence().getResidues();
            return new BioSequence(ProteinTools.createProtein(residues), translation);
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
            String residues = protein.getSequence().getResidues();
            return new BioSequence(ProteinTools.createProtein(residues), protein);
        }
    }
}
