package org.flymine.biojava;

/*
 * Copyright (C) 2002-2004 FlyMine
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
import org.flymine.model.genomic.Chromosome;

/**
 * A factory for creating FlyMineSequence objects.
 *
 * @author Kim Rutherford
 */

public abstract class FlyMineSequenceFactory
{
    /**
     * Create a new FlyMineSequence from a LocatedSequenceFeature
     * @param feature the LocatedSequenceFeature
     * @return a new FlyMineSequence object
     * @throws IllegalSymbolException if any of the residues of the LocatedSequenceFeature can't be
     * turned into DNA symbols.
     */
    public static FlyMineSequence make(LocatedSequenceFeature feature)
        throws IllegalSymbolException {
        return new FlyMineSequence(DNATools.createDNA(feature.getResidues()), feature);
    }

    /**
     * Create a new FlyMineSequence from a Chromosome
     * @param chr the Chromosome
     * @return a new FlyMineSequence object
     * @throws IllegalSymbolException if any of the residues of the Chromosome can't be
     * turned into DNA symbols.
     */
    public static FlyMineSequence make(Chromosome chr)
        throws IllegalSymbolException {
        // XXX FIXME - Chromosome needs a residues field 
//        return new FlyMineSequence(DNATools.createDNA(Chromosome.getResidues()), chr);
        return new FlyMineSequence(DNATools.createDNA("acgactactactattactggactggacgactgac"), chr);
    }

    /**
     * Create a new FlyMineSequence from a Protein
     * @param protein the Protein
     * @return a new FlyMineSequence object
     * @throws IllegalSymbolException if any of the residues of the LocatedSequenceFeature can't be
     * turned into DNA symbols.
     */
    public static FlyMineSequence make(Protein protein)
        throws IllegalSymbolException {
        return new FlyMineSequence(ProteinTools.createProtein(protein.getResidues()), protein);
    }

}
