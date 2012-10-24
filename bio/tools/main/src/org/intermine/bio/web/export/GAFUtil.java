package org.intermine.bio.web.export;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.intermine.bio.io.gaf.GAFRecord;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.SequenceFeature;

/**
 * Utility methods for GAF format.
 * Refer to BEDUtil.java.
 *
 * @author Fengyuan Hu
 */
public final class GAFUtil
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(GAFUtil.class);

    private GAFUtil() {
       // dont'
    }

    /**
     * Create a GAFRecord from a LocatedSequenceFeature.
     *
     * @param lsf the LocatedSequenceFeature
     * @return the GAFRecord or null
     */
    public static GAFRecord makeGAFRecord(SequenceFeature lsf) {

        String chrom = null;
        int chromStart = -1;
        int chromEnd = -1;
        String name = null;
        int score = 0;
        String strand = ".";

        if (lsf instanceof Chromosome) {
            return null;
        } else {
            Chromosome chr = lsf.getChromosome();
            if (chr == null) {
                return null;
            }

            if (lsf.getSymbol() == null) {
                if (lsf.getPrimaryIdentifier() == null) {
                    name = "(Unknown)";
                } else {
                    name = lsf.getPrimaryIdentifier();
                }
            } else {
                name = lsf.getSymbol();
            }

            name = name.replaceAll(" ", "_"); // replace white space in name to under score

            Location chrLocation = lsf.getChromosomeLocation();

            if (chrLocation == null) {
                return null;
            }

            chromStart = chrLocation.getStart().intValue() - 1; // Interbase Coordinate
            chromEnd = chrLocation.getEnd().intValue();
            if (chrLocation.getStrand() != null) {
                if ("1".equals(chrLocation.getStrand())) {
                    strand = "+";
                } else if ("-1".equals(chrLocation.getStrand())) {
                    strand = "-";
                }
            }
        }

        return new GAFRecord(chrom, chromStart, chromEnd, name, score, strand);
    }
}
