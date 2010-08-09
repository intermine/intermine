package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.biojava.bio.Annotation;
import org.biojava.bio.seq.Sequence;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.CDS;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.MRNA;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStoreException;

/**
 * A fasta loader that understand the headers of FlyBase fasta CDS fasta files and can make the
 * appropriate extra objects and references.
 * @author Kim Rutherford
 */
public class FlyBaseCDSFastaLoaderTask extends FlyBaseFeatureFastaLoaderTask
{
   /**
     * {@inheritDoc}
     */
    @Override
    protected void extraProcessing(Sequence bioJavaSequence,
            org.intermine.model.bio.Sequence flymineSequence,
            BioEntity interMineObject, Organism organism, DataSet dataSet)
        throws ObjectStoreException {
        Annotation annotation = bioJavaSequence.getAnnotation();
        String header = (String) annotation.getProperty("description");
        String mrnaIdentifier = getMRNAIdentifier(header);
        CDS cds;
        if (interMineObject instanceof CDS) {
            cds = (CDS) interMineObject;
        } else {
            throw new RuntimeException("the InterMineObject passed to "
                                       + "FlyBaseCDSFastaLoaderTask.extraProcessing() is not a "
                                       + "CDS: " + interMineObject);
        }
        cds.setmRNA(getMRNA(mrnaIdentifier, organism));
        Location loc = getLocationFromHeader(header, cds, organism);
        getDirectDataLoader().store(loc);
        createSynonym(interMineObject, dataSet, cds.getPrimaryIdentifier());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getIdentifier(Sequence bioJavaSequence) {
        Annotation annotation = bioJavaSequence.getAnnotation();
        String header = (String) annotation.getProperty("description");

        final String regexp = ".*FlyBase_Annotation_IDs:([^, =;]+).*";
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(header);

        if (m.matches()) {
            return m.group(1) + "_CDS";
        }
        // it doesn't matter too much what the CDS identifier is
        return getMRNAIdentifier(header) + "_CDS";

    }

    private String getMRNAIdentifier(String header) {
        final String regexp = ".*parent=FBgn[^,]+,([^, =;]+).*";
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(header);

        if (m.matches()) {
            return m.group(1);
        }
        throw new RuntimeException("can't find FBtr identifier in header: " + header);
    }

    private MRNA getMRNA(String mrnaIdentifier, Organism organism) throws ObjectStoreException {
        MRNA mrna = (MRNA) getDirectDataLoader().createObject(MRNA.class);
        mrna.setPrimaryIdentifier(mrnaIdentifier);
        mrna.setOrganism(organism);
        getDirectDataLoader().store(mrna);
        return mrna;
    }
}
