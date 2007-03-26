package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.postprocess.PostProcessor;

import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.MicroarrayOligo;
import org.flymine.model.genomic.Transcript;

/**
 * LongOligoPostProcess class
 *
 * @author Kim Rutherford
 */

public class LongOligoPostProcess extends PostProcessor
{
    /**
     * Create a new instance of LongOligoPostProcess
     * @param osw 
     *
     * @param  osw object store writer
     */
    public LongOligoPostProcess (ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * {@inheritDoc}
     * <br/>
     * Use the MicroarrayOligo->Transcript and Transcript->Chromosome Locations to create
     * MicroarrayOligo->Chromosome Locations.
     * @throws ObjectStoreException if anything goes wrong
     */
    public void postProcess() throws ObjectStoreException {
        CalculateLocations cl = new CalculateLocations(getObjectStoreWriter());

        cl.createTransformedLocations(Transcript.class, Chromosome.class, MicroarrayOligo.class);
    }
}
