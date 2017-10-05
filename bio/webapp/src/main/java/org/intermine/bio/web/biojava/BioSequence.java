package org.intermine.bio.web.biojava;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.biojava.bio.seq.impl.SimpleSequence;
import org.biojava.bio.symbol.SymbolList;

import org.intermine.model.bio.BioEntity;

/**
 * An implementation of the BioJava Sequence interface that uses InterMine objects underneath.
 *
 * @author Kim Rutherford
 */
public class BioSequence extends SimpleSequence
{
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    /**
     * The BioEntity that was passed to the constructor.
     */
    @SuppressWarnings("unused")
    private BioEntity bioEntity = null;

    /**
     * Create a new BioSequence from a BioEntity
     * @param symbols a DNA SymbolList created from the BioEntity
     * @param bioEntity the BioEntity
     */
    BioSequence (SymbolList symbols, BioEntity bioEntity) {
        super(symbols, null, bioEntity.getPrimaryIdentifier(), null);
        this.bioEntity = bioEntity;
    }
}
