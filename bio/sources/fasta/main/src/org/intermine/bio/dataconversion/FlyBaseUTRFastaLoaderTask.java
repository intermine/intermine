package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;

import org.intermine.objectstore.ObjectStoreException;

import org.flymine.model.genomic.BioEntity;
import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.DataSource;
import org.flymine.model.genomic.Location;
import org.flymine.model.genomic.MRNA;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.UTR;

import org.biojava.bio.Annotation;
import org.biojava.bio.seq.Sequence;

/**
 * A fasta loader that understand the headers of FlyBase fasta UTR fasta files and can make the
 * appropriate extra objects and references.
 * @author Kim Rutherford
 */
public class FlyBaseUTRFastaLoaderTask extends FlyBaseFeatureFastaLoaderTask
{

    Map<String, Chromosome> chrMap = new HashMap<String, Chromosome>();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void extraProcessing(Sequence bioJavaSequence,
                                   @SuppressWarnings("unused")
                                     org.flymine.model.genomic.Sequence flymineSequence,
                                   BioEntity interMineObject, Organism organism,
                                   DataSource dataSource)
        throws ObjectStoreException {
        Annotation annotation = bioJavaSequence.getAnnotation();
        String mrnaIdentifier = bioJavaSequence.getName();
        UTR utr;
        if (interMineObject instanceof UTR) {
            utr = (UTR) interMineObject;
        } else {
            throw new RuntimeException("the InterMineObject passed to "
                                       + "FlyBaseUTRFastaLoaderTask.extraProcessing() is not a "
                                       + "UTR");
        }

        String utrIdentifier = utr.getPrimaryIdentifier();

        MRNA mrna = getMRNA(mrnaIdentifier, organism);
        utr.setmRNA(mrna);

        createSynonym(interMineObject, dataSource, utrIdentifier);

        String header = (String) annotation.getProperty("description");

        Location loc = getLocationFromHeader(header, utr, organism);
        getDirectDataLoader().store(loc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getIdentifier(Sequence bioJavaSequence) {
        if (getClassName().endsWith(".FivePrimeUTR")) {
            return bioJavaSequence.getName() + "-5-prime-utr";
        } else {
            return bioJavaSequence.getName() + "-3-prime-utr";
        }
    }

    private MRNA getMRNA(String mrnaIdentifier, Organism organism) throws ObjectStoreException {
        MRNA mrna = (MRNA) getDirectDataLoader().createObject(MRNA.class);
        mrna.setPrimaryIdentifier(mrnaIdentifier);
        mrna.setOrganism(organism);
        mrna.addDataSets(getDataSet());
        getDirectDataLoader().store(mrna);
        return mrna;
    }
}
