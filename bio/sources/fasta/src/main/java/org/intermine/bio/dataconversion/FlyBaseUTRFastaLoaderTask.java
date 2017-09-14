package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.biojava.bio.Annotation;
import org.biojava.bio.seq.Sequence;
import org.intermine.metadata.Model;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.DynamicUtil;

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
            @SuppressWarnings("unused") org.intermine.model.bio.Sequence flymineSequence,
            BioEntity bioEntity, Organism organism,
            @SuppressWarnings("unused") DataSet dataSet)
        throws ObjectStoreException {
        Annotation annotation = bioJavaSequence.getAnnotation();
        String mrnaIdentifier = bioJavaSequence.getName();

        ObjectStore os = getIntegrationWriter().getObjectStore();
        Model model = os.getModel();
        if (model.hasClassDescriptor(model.getPackageName() + ".UTR")) {
            Class<? extends FastPathObject> cdsCls =
                model.getClassDescriptorByName("UTR").getType();
            if (!DynamicUtil.isInstance(bioEntity, cdsCls)) {
                throw new RuntimeException("the InterMineObject passed to "
                        + "FlyBaseUTRFastaDataLoaderTask.extraProcessing() is not a "
                        + "UTR: " + bioEntity);
            }
            InterMineObject mrna = getMRNA(mrnaIdentifier, organism, model);
            if (mrna != null) {
                Set<? extends InterMineObject> mrnas = new HashSet(Collections.singleton(mrna));
                bioEntity.setFieldValue("transcripts", mrnas);
            }
            String header = (String) annotation.getProperty("description");
            Location loc = getLocationFromHeader(header, (SequenceFeature) bioEntity,
                    organism);
            getDirectDataLoader().store(loc);
        } else {
            throw new RuntimeException("Trying to load UTR sequence but UTR does not exist in the"
                    + " data model");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getIdentifier(Sequence bioJavaSequence) {
        if (getClassName().endsWith(".FivePrimeUTR")) {
            return bioJavaSequence.getName() + "-5-prime-utr";
        }
        return bioJavaSequence.getName() + "-3-prime-utr";
    }
}
