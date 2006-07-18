package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2005 FlyMine
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

import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Phenotype;

/**
 * LongOligoPostProcess class
 *
 * @author Kim Rutherford
 */

public class PhenotypePostProcess extends PostProcessor
{
    /**
     * Create a new instance of LongOligoPostProcess
     * @param osw 
     *
     * @param  osw object store writer
     */
    public PhenotypePostProcess (ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * {@inheritDoc}
     * <br/>
     * Use the MicroarrayOligo->Transcript and Transcript->Chromosome Locations to create
     * MicroarrayOligo->Chromosome Locations.
     * @throws ObjectStoreException 
     * @throws Exception if anything goes wrong
     */
    public void postProcess() throws ObjectStoreException {
        CreateReferences cr = new CreateReferences(getObjectStoreWriter());

        try {
            cr.insertReferences(Gene.class, Phenotype.class, "phenotypes");
        } catch (Exception e) {
            throw new RuntimeException("exception during phenotype postprocessing", e);
        }
    }
}
