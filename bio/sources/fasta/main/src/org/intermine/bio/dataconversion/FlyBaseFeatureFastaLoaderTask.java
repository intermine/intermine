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
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Location;
import org.flymine.model.genomic.Organism;

/**
 * Code for loading fasta for flybase, setting feature attribute from the FASTA header.
 * @author Kim Rutherford
 */
public class FlyBaseFeatureFastaLoaderTask extends FastaLoaderTask
{
    Map<String, Chromosome> chrMap = new HashMap<String, Chromosome>();

    protected BioEntity getChromosome(String chromosomeId, Organism organism)
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

    protected Location getLocationFromHeader(String header, LocatedSequenceFeature interMineObject,
                                             Organism organism)
        throws ObjectStoreException {
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
            interMineObject.setChromosomeLocation(loc);
            return loc;
        } else {
            throw new RuntimeException("header doesn't match pattern \"" + regexp + "\": "
                                       + header);
        }
    }

    protected boolean isComplement(String locString) {
        return locString.contains("complement");
    }

    protected int getMin(String locString) {
        int currentMin = Integer.MAX_VALUE;
        String regexp = "\\d+";
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(locString);
        while (m.find()) {
            String posString = m.group();
            int pos = Integer.parseInt(posString);
            if (pos < currentMin) {
                currentMin = pos;
            }
        }
        if (currentMin == Integer.MAX_VALUE) {
            throw new RuntimeException("can't find minimum value from location: " + locString);
        }
        return currentMin;
    }

    protected int getMax(String locString) {
        int currentMax = Integer.MIN_VALUE;
        String regexp = "\\d+";
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(locString);
        while (m.find()) {
            String posString = m.group();
            int pos = Integer.parseInt(posString);
            if (pos > currentMax) {
                currentMax = pos;
            }
        }
        if (currentMax == Integer.MIN_VALUE) {
            throw new RuntimeException("can't find minimum value from location: " + locString);
        }
        return currentMax;
    }

}
