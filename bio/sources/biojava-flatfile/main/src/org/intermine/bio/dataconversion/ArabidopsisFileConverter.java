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

import org.intermine.dataconversion.ItemWriter;
import org.intermine.objectstore.ObjectStoreException;

import org.biojava.bio.Annotation;
import org.biojava.bio.seq.Feature;

/**
 * @author kmr
 *
 */
public class ArabidopsisFileConverter extends BioJavaFlatFileConverter
{

    /**
     * @param writer
     * @throws ObjectStoreException 
     */
    public ArabidopsisFileConverter(ItemWriter writer) throws ObjectStoreException {
        super(writer);
    }

    /**
     * Return a gene name for the given feature.  For Arabidopsis we parse the /product line
     * line.
     * @param feature the Feature
     * @return the name or null if the name cannot be determined
     */
    protected String getGeneIdentifierFromCDS(Feature feature) {
        Annotation annotation = feature.getAnnotation();
        if (annotation.containsProperty("gene")) {
            return (String) annotation.getProperty("gene");
        } else {
            if (annotation.containsProperty("protein_id")) {
                return (String) annotation.getProperty("protein_id");
            } else {
                if (annotation.containsProperty("product")) {
                    return (String) annotation.getProperty("product");
                } else {
                    throw new RuntimeException("no /gene, /product or /protein_id in: " + feature);
                }
            }
        }
    }
}
