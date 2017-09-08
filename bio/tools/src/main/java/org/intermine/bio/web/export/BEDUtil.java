package org.intermine.bio.web.export;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.bio.io.bed.BEDRecord;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.SequenceFeature;

/**
 * Utility methods for UCSC BED format.
 * Refer to GFF3Util.java.
 *
 * @author Fengyuan Hu
 */
public final class BEDUtil
{
    private static final String UCSC_CHR_PREFIX = "chr";

    private BEDUtil() {
       // dont'
    }

    /**
     * Create a BEDRecord from a LocatedSequenceFeature.
     *
     * @param lsf the LocatedSequenceFeature
     * @return the BEDRecord or null if this lsf has no Chromosome or no Chromosome location
     */
    public static BEDRecord makeBEDRecord(SequenceFeature lsf) {
        return makeBEDRecord(lsf, true);
    }

    /**
     * Create a BEDRecord from a LocatedSequenceFeature.
     *
     * @param lsf the LocatedSequenceFeature
     * @param makeUcscCompatible if true prefix 'chr' to chromosome names to work with UCSC genome
     *      browser and Galaxy
     * @return the BEDRecord or null if this lsf has no Chromosome or no Chromosome location
     */
    public static BEDRecord makeBEDRecord(SequenceFeature lsf, boolean makeUcscCompatible) {

        String chrom = null;
        int chromStart = -1;
        int chromEnd = -1;
        String name = null;
        final int score = 0;
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

            if (makeUcscCompatible) {
                chrom = UCSC_CHR_PREFIX + chr.getPrimaryIdentifier();
            } else {
                chrom = chr.getPrimaryIdentifier();
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

        return new BEDRecord(chrom, chromStart, chromEnd, name, score, strand);
    }
}
