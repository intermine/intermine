package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.intermine.objectstore.ObjectStoreException;

import org.flymine.model.genomic.BioEntity;
import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.DataSource;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Location;
import org.flymine.model.genomic.MRNA;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.Synonym;
import org.flymine.model.genomic.UTR;

import org.biojava.bio.Annotation;
import org.biojava.bio.seq.Sequence;

/**
 * A fasta loader that understand the headers of FlyBase fasta UTR fasta files and can make the
 * appropriate extra objects and references.
 * @author Kim Rutherford
 */
public class FlyBaseUTRFastaLoaderTask extends FastaLoaderTask
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

        String utrIdentifier = utr.getIdentifier();

        MRNA mrna = getMRNA(mrnaIdentifier, organism);
        utr.setmRNA(mrna);

        Synonym synonym = (Synonym) getDirectDataLoader().createObject(Synonym.class);
        synonym.setValue(utrIdentifier);
        synonym.setType("identifier");
        synonym.setSubject(interMineObject);
        synonym.setSource(dataSource);
        getDirectDataLoader().store(synonym);

        String header = (String) annotation.getProperty("description");
        final String regexp = ".*loc=(\\S+):(\\S+).*";
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(header);
        if (m.matches()) {
            String chromosomeId = m.group(1);
            String locationString = m.group(2);
            int min = getMin(locationString);
            int max = getMax(locationString);
            Location loc = (Location) getDirectDataLoader().createObject(Location.class);
            loc.setStart(new Integer(min));
            loc.setEnd(new Integer(max));
            if (isComplement(locationString)) {
                loc.setStrand("-1");
            } else {
                loc.setStrand("1");
            }
            loc.setSubject(interMineObject);
            loc.setObject(getChromosome(chromosomeId, organism));
            if (interMineObject instanceof LocatedSequenceFeature) {
                LocatedSequenceFeature lsf = (LocatedSequenceFeature) interMineObject;
                lsf.setChromosomeLocation(loc);
            }
            getDirectDataLoader().store(loc);
        } else {
            throw new RuntimeException("header doesn't match pattern \"" + regexp + "\": "
                                       + header);
        }
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
        mrna.setIdentifier(mrnaIdentifier);
        mrna.setOrganism(organism);
        getDirectDataLoader().store(mrna);
        return mrna;
    }

    private BioEntity getChromosome(String chromosomeId, Organism organism)
    throws ObjectStoreException {
        if (chrMap.containsKey(chromosomeId)) {
            return chrMap.get(chromosomeId);
        } else {
            Chromosome chr = (Chromosome) getDirectDataLoader().createObject(Chromosome.class);
            chr.setIdentifier(chromosomeId);
            chr.setOrganism(organism);
            getDirectDataLoader().store(chr);
            chrMap.put(chromosomeId, chr);
            return chr;
        }
    }

    private boolean isComplement(String location) {
        return location.contains("complement");
    }

    private int getMin(String location) {
        int currentMin = Integer.MAX_VALUE;
        String regexp = "\\d+";
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(location);
        while (m.find()) {
            String posString = m.group();
            int pos = Integer.parseInt(posString);
            if (pos < currentMin) {
                currentMin = pos;
            }
        }
        if (currentMin == Integer.MAX_VALUE) {
            throw new RuntimeException("can't find minimum value from location: " + location);
        }
        return currentMin;
    }

    private int getMax(String location) {
        int currentMax = Integer.MIN_VALUE;
        String regexp = "\\d+";
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(location);
        while (m.find()) {
            String posString = m.group();
            int pos = Integer.parseInt(posString);
            if (pos > currentMax) {
                currentMax = pos;
            }
        }
        if (currentMax == Integer.MIN_VALUE) {
            throw new RuntimeException("can't find minimum value from location: " + location);
        }
        return currentMax;
    }

}
