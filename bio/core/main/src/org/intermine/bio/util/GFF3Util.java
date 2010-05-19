package org.intermine.bio.util;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.intermine.bio.io.gff3.GFF3Record;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.model.bio.Location;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

/**
 * Utility methods for GFF3.
 * @author Kim Rutherford
 */
public abstract class GFF3Util
{
    private static final Logger LOG = Logger.getLogger(GFF3Util.class);

    /**
     * Create a GFF3Record from a LocatedSequenceFeature.
     *
     * @param lsf the LocatedSequenceFeature
     * @param soClassNameMap a Map from FlyMine class names to SO term names
     * @param sourceName the name of the data source encoded in the GFF, so the name of your project
     * @param extraAttributes name/value pairs to add to the attribute field of the GFF3Record
     * @return the GFF3Record or null if this lsf has no Chromosome or no Chromosome location
     */
    public static GFF3Record makeGFF3Record(SequenceFeature lsf,
            Map<String, String> soClassNameMap, String sourceName,
            Map<String, List<String>> extraAttributes) {
        Set<Class> classes = DynamicUtil.decomposeClass(lsf.getClass());

        String type = null;
        String sequenceID = null;
        int start = -1;
        int end = -1;
        String strand = ".";

        if (lsf instanceof Chromosome) {
            return null;
        } else {
            Chromosome chr = lsf.getChromosome();
            if (chr == null) {
                return null;
            }

            Location chrLocation = lsf.getChromosomeLocation();

            if (chrLocation == null) {
                return null;
            }

            sequenceID = chr.getPrimaryIdentifier();
//            LOG.info("mGFFseq: " + sequenceID + "|len: " + lsf.getLength());

            for (Class c : classes) {
                if (SequenceFeature.class.isAssignableFrom(c)) {
                    String className = TypeUtil.unqualifiedName(c.getName());
//                     LOG.info("mGFF: " + className + "|" + lsf.getPrimaryIdentifier());
                    if (soClassNameMap.containsKey(className)) {
                        type = soClassNameMap.get(className);
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
            end = chrLocation.getEnd().intValue();
            if (chrLocation.getStrand() != null) {
                if (chrLocation.getStrand().equals("1")) {
                    strand = "+";
                } else if (chrLocation.getStrand().equals("-1")) {
                    strand = "-";
                }
            }
        }

        Map<String, List<String>> recordAttribute =
//          new LinkedHashMap<String, List<String>>(extraAttributes);
            new TreeMap<String, List<String>>(extraAttributes);

        if (lsf.getPrimaryIdentifier() != null) {
            List<String> idList = new ArrayList<String>();
            idList.add(lsf.getPrimaryIdentifier());
            recordAttribute.put("ID", idList);
        }


        Double score = null;
        try {
            for (Class c : DynamicUtil.decomposeClass(lsf.getClass())) {
                if (TypeUtil.getFieldInfo(c, "score") != null) {
                    score = (Double) TypeUtil.getFieldValue(lsf, "score");
                }
            }
        } catch (IllegalAccessException e) {
            // do nothing, we can't set the score
        }

        return new GFF3Record(sequenceID, sourceName, type, start, end, score, strand, null,
                              recordAttribute);
    }
}
