package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
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

import org.flymine.model.genomic.BioEntity;
import org.flymine.model.genomic.DataSource;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Location;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.Region;
import org.flymine.model.genomic.Synonym;
import org.intermine.objectstore.ObjectStoreException;

/**
 * Code for loading fasta for flybase, setting feature attribute from the FASTA header.
 * @author Kim Rutherford
 */
public class FlyBaseFeatureFastaLoaderTask extends FastaLoaderTask
{
    private Map<String, Region> chrMap = new HashMap<String, Region>();

    /**
     * Return a Chromosome object for the given item.
     * @param chromosomeId the id
     * @param organism the Organism to reference from the Chromosome
     * @return the Chromosome
     * @throws ObjectStoreException if problem fetching Chromosome
     */
    protected Region getChromosome(String chromosomeId, Organism organism)
    throws ObjectStoreException {
        if (chrMap.containsKey(chromosomeId)) {
            return chrMap.get(chromosomeId);
        }
        Region chr = (Region) getDirectDataLoader().createObject(Region.class);
        chr.setPrimaryIdentifier(chromosomeId);
        chr.setOrganism(organism);
        chr.addDataSets(getDataSet());
        getDirectDataLoader().store(chr);
        chrMap.put(chromosomeId, chr);
        return chr;
    }

    /**
     * Create a Synonym.
     * @param interMineObject the subject InterMineObject of the Synonym
     * @param dataSource the DataSource for the Synonym
     * @param identifier the synonym value
     * @throws ObjectStoreException if there is a problem storing
     */
    public void createSynonym(BioEntity interMineObject, DataSource dataSource, String identifier)
    throws ObjectStoreException {
        Synonym synonym = (Synonym) getDirectDataLoader().createObject(Synonym.class);
        synonym.setValue(identifier);
        synonym.setType("identifier");
        synonym.setSubject(interMineObject);
        getDirectDataLoader().store(synonym);
    }

    /**
     * Return a Location object create by parsing the useful information from a FlyBase fasta
     * header line.
     * @param header the header line
     * @param lsf the LocatedSequenceFeature that is the subject of this Location
     * @param organism the Organism object used when creating Chromosomes
     * @return the Location
     * @throws ObjectStoreException there is a problem while creating the Location
     */
    protected Location getLocationFromHeader(String header, LocatedSequenceFeature lsf,
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
            loc.setSubject(lsf);
            loc.setObject(getChromosome(chromosomeId, organism));
            lsf.setChromosomeLocation(loc);
            return loc;
        }
        throw new RuntimeException("header doesn't match pattern \"" + regexp + "\": " + header);
    }

    /**
     * Return true if and only if the given location string represents a location on the reverse
     * strand.
     * @param locString the string
     * @return true if complement
     */
    protected boolean isComplement(String locString) {
        return locString.contains("complement");
    }

    /**
     * Return the minimum coordinate of the given location
     * @param locString the string
     * @return the minimum coordinate
     */
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

    /**
     * Return the maximum coordinate of the given location
     * @param locString the string
     * @return the maximum coordinate
     */
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
