package org.flymine.util;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 * 
 */

import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.io.gff3.GFF3Record;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Utility methods for GFF3.
 * @author Kim Rutherford
 */
public abstract class GFF3Util
{
    /**
     * Create a GFF3Record from a LocatedSequenceFeature.
     * @param lsf the LocatedSequenceFeature
     * @return the GFF3Record
     */
    public static GFF3Record makeGFF3Record(LocatedSequenceFeature lsf) {
        return makeGFF3Record(lsf, new HashMap());
    }

    /**
     * Create a GFF3Record from a LocatedSequenceFeature.
     * @param lsf the LocatedSequenceFeature
     * @param extraAttributes name/value pairs to add to the attribute field of the GFF3Record
     * @return the GFF3Record
     */
    public static GFF3Record makeGFF3Record(LocatedSequenceFeature lsf, Map extraAttributes) {
        Set classes = DynamicUtil.decomposeClass(lsf.getClass());
        
        String type = null;
        String sequenceID = null;
        int start = -1;
        int end = -1;
        
        if (lsf instanceof Chromosome) {
            sequenceID = lsf.getIdentifier();
            type = "chromosome";
            start = 1;
            end = lsf.getLength().intValue();
        } else {
            sequenceID = lsf.getChromosome().getIdentifier();

            Iterator iter = classes.iterator();
            while (iter.hasNext()) {                
                Class c = (Class) iter.next();
                if (LocatedSequenceFeature.class.isAssignableFrom(c)) {
                    type = TypeUtil.unqualifiedName(c.getName());                
                    break;
                }
            }
            
            if (type == null) {
                throw new IllegalArgumentException("argument to makeGFF3Record isn't a "
                                                   + "LocatedSequenceFeature");
            }

            start = lsf.getChromosomeLocation().getStart().intValue();
            end = lsf.getChromosomeLocation().getEnd().intValue();
        }

        return new GFF3Record(sequenceID, "FlyMine", type, start, end, null, null, null,
                              extraAttributes);

    }
}
