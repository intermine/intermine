package org.intermine.bio.web.biojava;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import org.biojava.nbio.core.exceptions.CompoundNotFoundException;
import org.biojava.nbio.core.sequence.AccessionID;
import org.biojava.nbio.core.sequence.template.AbstractSequence;
import org.biojava.nbio.core.sequence.template.Compound;
import org.biojava.nbio.ontology.utils.SmallAnnotation;
import org.intermine.model.bio.BioEntity;

/**
 * An implementation of the BioJava Sequence interface that uses InterMine objects underneath.
 *
 * @author Kim Rutherford
 */
public class BioSequence extends AbstractSequence<Compound>
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
    private SmallAnnotation annotation;

    /**
     * Create a new BioSequence from a BioEntity
     * @param seq a biojava sequence
     * @param bioEntity the BioEntity
     * @throws CompoundNotFoundException exception
     */
    public BioSequence (AbstractSequence seq, BioEntity bioEntity)
        throws CompoundNotFoundException {

        super(seq.getSequenceAsString(), seq.getCompoundSet());

        AccessionID seqAccession = new AccessionID(bioEntity.getPrimaryIdentifier());
        seq.setAccession(seqAccession);
        annotation = new SmallAnnotation();
        this.bioEntity = bioEntity;
    }

    /**
     * @return the annotation
     */

    public SmallAnnotation getAnnotation() {
        // TODO Auto-generated method stub
        return annotation;
    }
}
