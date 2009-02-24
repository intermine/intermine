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

import org.intermine.metadata.Model;
import org.intermine.xml.full.Item;
import org.intermine.util.XmlUtil;

import org.intermine.bio.io.gff3.GFF3Record;

/**
 * Handle special cases when converting malaria GFF3 files.
 *
 * @author Richard Smith
 */

public class MalariaGFF3RecordHandler extends GFF3RecordHandler
{
    /**
     * Create a new MalariaGFF3RecordHandler object.
     * @param tgtModel the target Model
     */
    public MalariaGFF3RecordHandler(Model tgtModel) {
        super(tgtModel);
    }

    /**
     * {@inheritDoc}
     */
    public void process(GFF3Record record) {
        Item feature = getFeature();

        String clsName = XmlUtil.getFragmentFromURI(feature.getClassName());

        if ("Gene".equals(clsName)) {
            // move Gene.primaryIdentifier to Gene.secondaryIdentifier
            // move Gene.symbol to Gene.primaryIdentifier

            if (feature.getAttribute("primaryIdentifier") != null) {
                String secondary = feature.getAttribute("primaryIdentifier").getValue();
                feature.setAttribute("secondaryIdentifier", secondary);
            }
            if (feature.getAttribute("symbol") != null) {
                String primary = feature.getAttribute("symbol").getValue();
                feature.setAttribute("primaryIdentifier", primary);
                feature.removeAttribute("symbol");
            }
        }

    }
}
