package org.flymine.web.biojava;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.biojava.bio.seq.impl.SimpleSequence;
import org.biojava.bio.symbol.SymbolList;

import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.LocatedSequenceFeature;

/**
 * An implementation of the BioJava Sequence interface that uses FlyMine objects underneath.
 *
 * @author Kim Rutherford
 */
public class FlyMineSequence extends SimpleSequence
{
    /**
     * The LocatedSequenceFeature that was passed to the constructor.
     */
    private LocatedSequenceFeature feature = null;

    /**
     * The Protein that was passed to the constructor.
     */
    private Protein protein = null;

    /**
     * Create a new FlyMineSequence from a LocatedSequenceFeature
     * @param symbols a DNA SymbolList created from the LocatedSequenceFeature
     * @param feature the LocatedSequenceFeature
     */
    FlyMineSequence (SymbolList symbols, LocatedSequenceFeature feature) {
        super(symbols, null, feature.getIdentifier(), null);
        this.feature = feature;
    }

    /**
     * Create a new FlyMineSequence from a Protein
     * @param symbols a amino acid SymbolList created from the Protein
     * @param protein the Protein
     */
    FlyMineSequence (SymbolList symbols, Protein protein) {
        super(symbols, null, protein.getIdentifier(), null);
        this.protein = protein;
    }
}
