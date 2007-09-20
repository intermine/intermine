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

import org.biojava.bio.seq.impl.SimpleSequence;
import org.biojava.bio.symbol.SymbolList;

import org.flymine.model.genomic.BioEntity;

/**
 * An implementation of the BioJava Sequence interface that uses FlyMine objects underneath.
 *
 * @author Kim Rutherford
 */
public class FlyMineSequence extends SimpleSequence
{
    /**
     * The BioEntity that was passed to the constructor.
     */
    private BioEntity bioEntity = null;

    /**
     * Create a new FlyMineSequence from a BioEntity
     * @param symbols a DNA SymbolList created from the BioEntity
     * @param bioEntity the BioEntity
     */
    FlyMineSequence (SymbolList symbols, BioEntity bioEntity) {
        super(symbols, null, bioEntity.getIdentifier(), null);
        this.bioEntity = bioEntity;
    }
}
