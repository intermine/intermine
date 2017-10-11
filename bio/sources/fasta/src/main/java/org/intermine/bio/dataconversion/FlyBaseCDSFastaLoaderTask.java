package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
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
import org.intermine.metadata.Model;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.DynamicUtil;

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
            BioEntity bioEntity, Organism organism, DataSet dataSet)
        throws ObjectStoreException {
        Annotation annotation = bioJavaSequence.getAnnotation();
        String header = (String) annotation.getProperty("description");
        String mrnaIdentifier = getMRNAIdentifier(header);

        ObjectStore os = getIntegrationWriter().getObjectStore();
        Model model = os.getModel();
        if (model.hasClassDescriptor(model.getPackageName() + ".CDS")) {
            Class<? extends FastPathObject> cdsCls =
                model.getClassDescriptorByName("CDS").getType();
            if (!DynamicUtil.isInstance(bioEntity, cdsCls)) {
                throw new RuntimeException("the InterMineObject passed to "
                        + "FlyBaseCDSFastaLoaderTask.extraProcessing() is not a "
                        + "CDS: " + bioEntity);
            }
            InterMineObject mrna = getMRNA(mrnaIdentifier, organism, model);
            if (mrna != null) {
                bioEntity.setFieldValue("transcript", mrna);
            }
            Location loc = getLocationFromHeader(header, (SequenceFeature) bioEntity,
                    organism);
            getDirectDataLoader().store(loc);
        } else {
            throw new RuntimeException("Trying to load CDS sequence but CDS does not exist in the"
                    + " data model");
        }
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
}
