package org.intermine.bio.util;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 * 
 */

import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Location;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Utility methods for GFF3.
 * @author Kim Rutherford
 */
public abstract class GFF3Util
{
    private static final Logger LOG = Logger.getLogger(GFF3Util.class);

    /**
     * Create a GFF3Record from a LocatedSequenceFeature.
     * @param lsf the LocatedSequenceFeature
     * @param soClassNameMap a Map from FlyMine class names to SO term names
     * @param extraAttributes name/value pairs to add to the attribute field of the GFF3Record
     * @return the GFF3Record or null if this lsf has no Chromosome or no Chromosome location
     */
    public static GFF3Record makeGFF3Record(LocatedSequenceFeature lsf, Map soClassNameMap,
                                            Map extraAttributes) {
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
            Chromosome chr = lsf.getChromosome();
            if (chr == null) {
                return null;
            }
            
            Location chrLocation = lsf.getChromosomeLocation();
            
            if (chrLocation == null) {
                return null;
            }
            
            sequenceID = chr.getIdentifier();

            Iterator iter = classes.iterator();
            while (iter.hasNext()) {                
                Class c = (Class) iter.next();
                if (LocatedSequenceFeature.class.isAssignableFrom(c)) {
                    String className = TypeUtil.unqualifiedName(c.getName());
                    if (soClassNameMap.containsKey(className)) {
                        type = (String) soClassNameMap.get(className);
                    } else {
                        type = className;
                        LOG.warn("in GFF3Util.makeGFF3Record() - cannot find SO term name for: "
                                 + className);
                    }
                    break;
                }
            }
            
            if (type == null) {
                throw new IllegalArgumentException("argument to makeGFF3Record isn't a "
                                                   + "LocatedSequenceFeature");
            }

            start = chrLocation.getStart().intValue();
            end = lsf.getChromosomeLocation().getEnd().intValue();
        }

        Map recordAttribute = new LinkedHashMap(extraAttributes);

        if (lsf.getIdentifier() != null) {
            recordAttribute.put("ID", lsf.getIdentifier());
        }

        return new GFF3Record(sequenceID, "FlyMine", type, start, end, null, null, null,
                              recordAttribute);

    }
}
